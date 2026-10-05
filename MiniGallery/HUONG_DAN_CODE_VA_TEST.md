# MiniGallery — Hướng dẫn viết lại project Compose và kiểm thử

## 1. Mục tiêu và kiến trúc

Project: **MiniGallery**, applicationId **com.example.minigallery**. App đọc ảnh qua MediaStore, tìm kiếm/sắp xếp, xem metadata, chọn tối đa 10 ảnh và lưu **bản sao** vào Pictures/MiniGallery.

Luồng xử lý: **GalleryScreen → ViewModel → Repository → ContentResolver/MediaStore → StateFlow → UI**. UI không query MediaStore hoặc mở stream trực tiếp.

| Nhóm file | Cần viết gì |
|---|---|
| model/MediaImage.kt, PermissionState.kt, SortOrder.kt, ImportItem.kt | Dữ liệu ảnh, quyền, kiểu sắp xếp, trạng thái import |
| [MediaRepository.kt](app/src/main/java/com/example/minigallery/data/MediaRepository.kt) | Hợp đồng dữ liệu |
| [MediaStoreRepositoryImpl.kt](app/src/main/java/com/example/minigallery/data/MediaStoreRepositoryImpl.kt) | Query, observer, metadata, copy và cleanup |
| [PermissionHelper.kt](app/src/main/java/com/example/minigallery/util/PermissionHelper.kt) | Quyền theo API |
| [MiniGalleryUiState.kt](app/src/main/java/com/example/minigallery/ui/MiniGalleryUiState.kt) | State duy nhất của UI |
| [MiniGalleryViewModel.kt](app/src/main/java/com/example/minigallery/ui/MiniGalleryViewModel.kt) | Tìm kiếm, sort, quyền và batch import |
| [GalleryScreen.kt](app/src/main/java/com/example/minigallery/ui/screen/GalleryScreen.kt) | Màn hình, grid, banner, search, FAB |
| [GalleryDialogs.kt](app/src/main/java/com/example/minigallery/ui/screen/GalleryDialogs.kt) | Preview import và detail ảnh |
| theme/Color.kt, Theme.kt, Type.kt, GalleryDimens.kt | Material 3, màu, chữ và kích thước |
| [MainActivity.kt](app/src/main/java/com/example/minigallery/MainActivity.kt) | Quyền, picker, ViewModel, setContent |

Những đường dẫn rút gọn model/, data/, ui/, theme/ đều nằm dưới app/src/main/java/com/example/minigallery.

## 2. Tạo project và cấu hình

1. Tạo Android Studio **Empty Activity với Jetpack Compose**, Kotlin, package com.example.minigallery.
2. Dùng [settings.gradle.kts](settings.gradle.kts), [build.gradle.kts](build.gradle.kts), [app/build.gradle.kts](app/build.gradle.kts), [libs.versions.toml](gradle/libs.versions.toml), gradle.properties và bộ Gradle Wrapper của project này để giữ đúng cấu hình.
3. Project dùng **minSdk 29, targetSdk 36, compileSdk 36.1, AGP 9.1.1, Gradle 9.3.1, JBR/JDK JetBrains 21**, Compose compiler plugin 2.3.20, Compose BOM 2026.03.01.
4. Chọn Gradle JDK là Android Studio JBR 21; toolchain yêu cầu vendor JetBrains. Cài SDK Platform tương ứng compileSdk 36.1 qua SDK Manager; để Android Studio tạo local.properties.
5. app/build.gradle.kts bật buildFeatures.compose=true, dùng plugin android.application và compose.compiler. Kotlin do AGP 9 hỗ trợ trực tiếp; không thêm kotlin-android.
6. Giữ dependency từ version catalog: Activity Compose, Material 3, Lifecycle runtime/Compose/ViewModel, Coroutines, Coil Compose. Giữ cả JUnit/Mockito/Coroutines test và Compose UI test/AndroidX test cho bộ test có sẵn.
7. Giữ resource icon/theme/string/backup hiện có trong app/src/main/res để manifest không trỏ đến resource thiếu. Không cần layout XML hoặc RecyclerView.

Trong [AndroidManifest.xml](app/src/main/AndroidManifest.xml) khai báo READ_EXTERNAL_STORAGE với maxSdkVersion=32, READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED; MainActivity là launcher exported=true. Không cần CAMERA vì luồng này chỉ chọn và copy ảnh.

Các README cũ có nhắc D:/MiniGallery; **thư mục thực tế ở bộ bài này là ExerciseWeek1/MiniGallery**. Thực hành và lệnh dưới dùng thư mục hiện tại.

## 3. Viết code theo từng bước

### Bước 1 — Viết model và state trước

- MediaImage chứa id, uri, displayName, mimeType, sizeBytes, dateAddedSeconds; có hàm formatSize/formatDate để hiển thị.
- PermissionState gồm Denied, GrantedFull, GrantedPartial(selectedCount).
- SortOrder gồm DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC và nhãn hiển thị.
- ImportItem chứa sourceUri, displayName, sizeBytes, status; ImportStatus gồm Pending, InProgress, Success(uri), Error(reason).
- MiniGalleryUiState chứa allImages/displayedImages, searchQuery, sortOrder, isLoading, permissionState, selectedImportList, isImporting, importProgressText, userMessage; totalCount/displayedCount là thuộc tính tính toán.

Dùng immutable data class và copy để phát state mới; không sửa list đang được UI đọc.

### Bước 2 — Hợp đồng Repository

~~~kotlin
interface MediaRepository {
    fun observeImages(): Flow<List<MediaImage>>
    suspend fun queryImages(): List<MediaImage>
    suspend fun getMediaInfo(uri: Uri): ImportItem
    suspend fun saveImageCopy(sourceUri: Uri): Result<Uri>
}
~~~

Viết MediaStoreRepositoryImpl nhận ContentResolver và application Context, không giữ Activity.

### Bước 3 — Query và quan sát MediaStore

queryImages chạy withContext(Dispatchers.IO); query projection gồm _ID, DISPLAY_NAME, MIME_TYPE, SIZE, DATE_ADDED; Cursor.use đóng tài nguyên; tạo content URI bằng ContentUris.withAppendedId.

observeImages dùng callbackFlow: đăng ký ContentObserver, trySend(Unit) ngay lần đầu và mỗi onChange; awaitClose hủy observer. Gộp tín hiệu bằng conflate/debounce(300), map thành queryImages, flowOn(IO).

Đừng bắt lỗi quyền rồi trả emptyList: ViewModel cần biết query lỗi để hiện thông báo. MediaStore phải truy cập theo content URI, không theo _data/file path. Đối chiếu [MediaStore shared media](https://developer.android.com/training/data-storage/shared/media).

getMediaInfo đọc DISPLAY_NAME/SIZE qua OpenableColumns; dùng fallback hợp lý khi metadata thiếu.

### Bước 4 — Lưu bản sao an toàn

Trong saveImageCopy:

1. ensureActive, lấy MIME từ source URI và phần mở rộng; tạo tên MiniGallery_<timestamp>_<source>.<ext>.
2. insert target vào MediaStore volume external primary; RELATIVE_PATH Pictures/MiniGallery; IS_PENDING=1.
3. Mở input của source, mở output target **bên trong use của input** để input luôn được đóng khi mở output thất bại.
4. Copy từng block; ensureActive trong vòng lặp; flush/close stream.
5. update IS_PENDING=0; kiểm tra kết quả update=1; trả Result.success(target).
6. Nếu lỗi/cancel, delete target đã tạo trong NonCancellable; giữ lỗi cleanup bằng addSuppressed.
7. CancellationException phải ném lại, lỗi khác trả Result.failure.

Không decode/compress ảnh nguồn trong luồng copy: bản sao giữ byte và MIME nguồn. Không xóa hoặc sửa source. Copy blocking với copyTo không tự kiểm tra cancellation giữa từng block; bản hiện tại dùng vòng lặp riêng.

### Bước 5 — PermissionHelper

| API | Request/kiểm tra |
|---|---|
| 29–32 | READ_EXTERNAL_STORAGE |
| 33 | READ_MEDIA_IMAGES |
| 34+ | READ_MEDIA_IMAGES và READ_MEDIA_VISUAL_USER_SELECTED |

Kiểm tra FULL trước PARTIAL. Callback quyền và onResume đều cập nhật ViewModel; cùng loại quyền vẫn có thể có tập ảnh được chọn khác nên phải tải lại. Xem [Selected Photos Access](https://developer.android.com/about/versions/14/changes/partial-photo-video-access).

### Bước 6 — ViewModel quản lý state

Tạo MutableStateFlow nội bộ, public StateFlow bằng asStateFlow; dùng viewModelScope.

- updatePermissionState: nếu có quyền thì khởi động lại observeImages; cancel/join job cũ để tránh observer chồng. Khi Denied, cancel observer và xóa allImages/displayedImages.
- Tìm tên bằng contains(query.trim(), ignoreCase=true). Sort ngày theo dateAddedSeconds, tên theo lowercase và thứ tự đã chọn.
- onPhotosSelected: hủy picker không tạo preview; take(10); lấy metadata; tạo ImportItem Pending; xử lý loading/lỗi.
- startSavingCopies: return nếu isImporting, list trống hoặc không còn Pending. **Đặt isImporting=true trước launch** để hai lần bấm nhanh không tạo hai job.
- Lưu tuần tự từng Pending, cập nhật InProgress rồi Success/Error; tiếp tục qua lỗi từng ảnh và hiển thị tổng kết.
- Khi cancellation, cập nhật item đang InProgress thành Error, ném lại CancellationException; finally đặt isImporting=false.
- dismissImportPreview chỉ xóa list khi không import. Batch đã hoàn tất không tự chuyển về Pending nên không lưu lại cùng batch.

Đối chiếu MiniGalleryViewModel.kt để hoàn thiện Factory và thứ tự update state.

### Bước 7 — Compose screen và dialogs

GalleryScreen chỉ nhận state/callback. Dùng Scaffold, OutlinedTextField, DropdownMenu, LazyVerticalGrid(GridCells.Adaptive), AsyncImage và FAB “Thêm ảnh”. items dùng stable key theo id; grid chừa khoảng dưới cho FAB.

Detail dialog có ảnh lớn, tên, MIME, dung lượng, ngày, URI; ID đang xem lưu rememberSaveable. Import dialog có list cuộn, trạng thái từng ảnh và nút lưu/đóng luôn nhìn được khi xoay ngang.

Khóa thao tác trong lúc lưu; sau hoàn tất không cho lưu lại batch. Hiển thị banner Denied/Partial và empty/loading phù hợp. UI state được hoist lên ViewModel theo [Compose state](https://developer.android.com/develop/ui/compose/state).

Để giữ UI tests, giữ testTag search và photo_<id>, cùng semantics “Thêm ảnh” như source. Theme hỗ trợ sáng/tối, dynamic color và insets.

### Bước 8 — MainActivity nối lifecycle và Activity Result

Dùng ComponentActivity và ViewModel Factory với MediaStoreRepositoryImpl(contentResolver, applicationContext). Đăng ký RequestMultiplePermissions và PickMultipleVisualMedia(10) ở thuộc tính Activity.

Trong setContent:

~~~kotlin
val state by gallery.uiState.collectAsStateWithLifecycle()
~~~

Truyền state/callback vào GalleryScreen. onAdd gọi picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); callback picker gọi gallery.onPhotosSelected.

LaunchedEffect(state.userMessage) hiện Toast rồi clearUserMessage. onResume gọi PermissionHelper.checkPermissionState và gallery.updatePermissionState. Model batch ở ViewModel giữ qua xoay màn hình.

## 4. Build và chạy các test có sẵn

PowerShell tại **MiniGallery**:

~~~powershell
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
./gradlew.bat --version
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "Unit test/build/lint thất bại" }

$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) { $sdkPath = "$env:LOCALAPPDATA/Android/Sdk" }
$adbPath = Join-Path $sdkPath "platform-tools/adb.exe"
& $adbPath devices
$serial = "emulator-5554"
& $adbPath -s $serial install -r "./app/build/outputs/apk/debug/app-debug.apk"
& $adbPath -s $serial shell am start -W -n "com.example.minigallery/.MainActivity"
~~~

Đổi JBR/SDK/serial theo máy. API 34 phù hợp để kiểm tra partial permission. Chạy instrumentation khi emulator đã online; biến ANDROID_SERIAL chọn thiết bị cho lần chạy:

~~~powershell
$previousSerial = $env:ANDROID_SERIAL
try {
    $env:ANDROID_SERIAL = $serial
    ./gradlew.bat :app:connectedDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Instrumentation test thất bại" }
} finally {
    $env:ANDROID_SERIAL = $previousSerial
}
~~~

### Bộ test và report

| Nhóm | File và điều cần kiểm tra |
|---|---|
| Unit — ViewModel | MiniGalleryViewModelTest: giới hạn 10, filter/sort, quyền đổi, tiến độ/lỗi/cancel, chặn bấm liên tiếp và lưu lại batch |
| Unit — model | MediaRepositoryBehaviorTest: định dạng dung lượng/ngày, nhãn sort |
| Unit — query | MediaStoreQueryTest: lỗi quyền phải được truyền lên |
| Instrumentation — UI | GalleryScreenTest: denied vẫn có picker, search callback, detail metadata, batch hoàn tất không lưu lại |
| Instrumentation — MediaStore thật | MediaStoreRepositoryInstrumentedTest: byte nguồn/đích, publish, nguồn đã xóa, cleanup khi cancel |

Unit source ở app/src/test/java/com/example/minigallery; instrumentation source ở app/src/androidTest/java/com/example/minigallery. Khi viết lại, dùng fake repository để test ViewModel độc lập; bộ test hiện tại là mẫu đối chiếu, không xóa assertion chỉ để làm test xanh.

Report thường ở app/build/reports/tests/testDebugUnitTest/index.html, app/build/reports/androidTests/connected và app/build/reports/lint-results-debug.html. Xác nhận có test thực sự được chạy, không chỉ BUILD SUCCESSFUL hoặc NO-SOURCE. Biên bản hiện tại ghi **15 unit tests và 7 instrumentation tests**.

## 5. Chuẩn bị dữ liệu và test thủ công

Chạy script nạp 10 PNG demo (đọc ảnh từ app khác cần quyền phù hợp):

~~~powershell
./scripts/Prepare-DemoImages.ps1 -AdbPath $adbPath -Serial $serial
~~~

Ảnh nguồn nằm trong Pictures/MiniGalleryDemo; file mẫu ở docs/demo-images. Script chạy lại ghi đè đúng tên demo, không xóa ảnh khác. Nếu Media Scanner chậm, đợi scan và mở lại picker.

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | DENIED, mở app | Banner quyền, grid không đọc toàn thư viện; nút Thêm ảnh vẫn dùng được |
| TC02 | Cấp FULL, tìm demo_ | Thấy 10 ảnh demo nguồn nếu scan xong và dữ liệu không có tên demo_ khác |
| TC03 | Sort ngày mới/cũ, tên A–Z/Z–A | Đổi thứ tự đúng; ngày bằng nhau không dùng để kết luận sort sai |
| TC04 | Mở một ảnh | Metadata và URI đúng ảnh, xem ảnh lớn được |
| TC05 | PARTIAL chọn 2 ảnh nguồn | Grid giới hạn theo quyền, banner Partial, không hiện toàn thư viện |
| TC06 | Chọn lại tập ảnh nhưng vẫn PARTIAL | Danh sách refresh đúng tập mới khi quay lại |
| TC07 | Picker chọn 10 ảnh | Preview 10 mục; chưa có bản sao trước khi bấm lưu |
| TC08 | Mở picker rồi hủy | Không tạo batch hoặc bản sao mới |
| TC09 | Preview → xoay ngang | Batch giữ nguyên, list cuộn, nút cuối dialog không bị che |
| TC10 | Lưu 10 ảnh | Có tiến độ từng ảnh, tổng kết; bản sao ở Pictures/MiniGallery, nguồn còn |
| TC11 | Bấm lưu liên tiếp nhanh | Chỉ một lượt lưu batch |
| TC12 | Batch hoàn tất, thử lưu lại | Nút không cho lưu lại; đóng preview để bắt đầu batch mới |
| TC13 | Xoay khi đang lưu | ViewModel giữ batch, không tạo import thứ hai |
| TC14 | Thu hồi quyền trong Settings, quay lại | Grid/state theo quyền mới; không crash |
| TC15 | Dark mode, màn hình nhỏ, landscape | Không che nút/list/FAB, chữ và thumbnail đọc được |
| TC16 | Nguồn mất hoặc copy lỗi | Item có Error, batch kết thúc, không khóa UI và không để đích pending rác |
| TC17 | Force-stop khi có preview/batch | Không yêu cầu khôi phục batch; bản hiện tại chưa hỗ trợ process death |

TC16 được bộ instrumentation kiểm thử với fixture có kiểm soát; không cần xóa ảnh cá nhân để tạo lỗi. Copy đủ byte và cleanup được xác minh tốt hơn bằng test repository thay vì chỉ nhìn thumbnail.

## 6. Lỗi thường gặp và giới hạn

- Không thấy grid nhưng picker hoạt động: kiểm tra quyền đọc; picker có URI grant riêng.
- Tìm ảnh mới không thấy: tên bản sao có tiền tố MiniGallery_, chờ observer, kiểm tra filter hiện tại.
- MediaStore query lỗi nhưng UI báo thư viện rỗng: đừng nuốt SecurityException trong repository.
- Hai lần bấm lưu ra hai bản: cờ isImporting phải được đặt trước coroutine launch.
- Hủy lưu xong UI còn khóa: reset isImporting ở finally và ném lại CancellationException.
- Toolchain fail: dùng JetBrains JDK 21, đúng SDK 36.1, đọc Gradle error; không hạ SDK tùy ý.
- State còn qua xoay không có nghĩa còn sau kill/force-stop. Batch chưa có cơ chế phục hồi process death; cleanup cũng không bảo đảm khi process bị kill đột ngột.

[Biên bản kiểm chứng hiện có](docs/verification/RESULTS.md) ghi 15 unit/7 instrumentation PASS trên API 34. API 29–33 và 35+, cloud provider/mất mạng/đầy bộ nhớ/font scale lớn chưa được nghiệm thu đầy đủ. Tài liệu này đối chiếu code và bằng chứng, **không phải lượt chạy lại test mới**.

## 7. Ghi kết quả test

| Case/test suite | Thiết bị/API | Kết quả thực tế | PASS/FAIL/Chưa chạy | Log/report/ảnh |
|---|---|---|---|---|
| testDebugUnitTest | JVM/JDK ... | ... tests | ... | ... |
| connectedDebugAndroidTest | ... | ... tests | ... | ... |
| TC01 | ... | ... | ... | ... |

Tạo thư mục test-results cho ảnh và log của lượt chạy riêng. Xem crash nếu có bằng:

~~~powershell
& $adbPath -s $serial logcat -d -s AndroidRuntime
~~~

BUILD_ALL.ps1 ở thư mục gốc chỉ build bảy bài Views; chạy lệnh MiniGallery riêng như phần 4. Không dùng script verification của bảy bài để thay cho các suite của MiniGallery.
