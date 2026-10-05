# Bài 5 — Hướng dẫn MediaStore Gallery với Jetpack Compose

## 1. Mục tiêu và cấu trúc

Giao diện hiện tại dùng **Jetpack Compose Material 3**, không dùng layout XML, ViewBinding hoặc các View như TextView/EditText/RecyclerView. Mỗi project độc lập; bài AIDL gồm server/client. Mở MainActivity.kt và ComposeActivity.kt trong src/main/java của module tương ứng để đối chiếu code đầy đủ.

Các file Service, contract AIDL, ImageSaver và manifest giữ chức năng nền của bài. XML cho tài nguyên, theme hệ thống, quyền và FileProvider vẫn cần cho Android.

## 2. Chuẩn bị project để viết lại

1. Tạo Empty Activity dùng Compose; sao chép cấu hình project, version catalog và Gradle Wrapper của bài.
2. Giữ compileSdk/targetSdk 36, minSdk 31, AGP 9.1.1, Gradle 9.3.1, Java source/target 17. AGP hỗ trợ Kotlin trực tiếp.
3. Áp dụng plugin Compose compiler 2.3.20, bật compose trong buildFeatures, dùng Compose BOM 2026.03.01, Activity Compose và Material 3/UI/tooling. Gallery dùng Coil Compose 2.7.0.
4. ComposeActivity kế thừa ComponentActivity, bật edge-to-edge và cung cấp Toast cho thông báo ngắn. DemoTheme chọn sáng/tối theo hệ thống; DemoScreen dùng Scaffold, Column cuộn và IME padding. ActionButton nhận label/enabled/action.
5. MainActivity gọi setContent sau khi khởi tạo trạng thái. Callback cập nhật mutableStateOf trên main thread; Compose tự cập nhật giao diện. Giữ lifecycle callbacks cho các tài nguyên cần đăng ký/hủy.
6. Giữ applicationId, permission, Service và cấu hình đặc thù trong AndroidManifest.xml.

## 3. Viết code theo từng bước

1. `MainActivity` kế thừa `ComposeActivity`; tạo state `status`, `result`, `photos`, `last`, `pending` và `creatingCapture`.
2. Dùng `setContent { DemoTheme { Scaffold { ... } } }` và `LazyVerticalGrid(GridCells.Fixed(3))`. Phần nút/status/preview là một item phủ ba cột; các ảnh là item riêng có key URI.
3. Coil Compose `AsyncImage` hiển thị preview và thumbnail; `ContentScale.Crop` cho ô vuông. Bấm ảnh cập nhật last/result state.
4. Giữ Activity Result API cho xin quyền, Photo Picker và TakePicture. Picker cập nhật state thay vì thêm ImageView vào bố cục.
5. MediaStore query chạy trên IO, gán danh sách photos trên main thread. onResume query lại để phản ánh thay đổi quyền.
6. Giữ ImageSaver, IS_PENDING và FileProvider camera_paths.xml. pending/last vẫn restore qua Bundle; pending còn được lưu SharedPreferences. Nút camera vô hiệu khi đang tạo URI hoặc đang chờ kết quả.
7. Giữ cleanup file tạm khi camera success/cancel và cleanup MediaStore khi save/import lỗi. Kiểm tra lưu JPEG, lỗi mô phỏng, preview, picker và camera.

## 4. Build, cài đặt và lấy log

Các lệnh dưới chạy trong **PowerShell tại thư mục project của bài**, sau khi mở terminal ở Android Studio. Nếu chưa có JAVA_HOME, có thể cấu hình trong phiên terminal:

~~~powershell
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
./gradlew.bat --version
./gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "Build/lint thất bại" }

$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) { $sdkPath = "$env:LOCALAPPDATA/Android/Sdk" }
$adbPath = Join-Path $sdkPath "platform-tools/adb.exe"
& $adbPath devices
$serial = "emulator-5554" # Thay bằng serial có trạng thái device
& $adbPath -s $serial install -r "./app/build/outputs/apk/debug/app-debug.apk"
& $adbPath -s $serial shell am start -W -n "vn.training.bai05/.MainActivity"
& $adbPath -s $serial logcat -s "GalleryDemo"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Các test template cũ không kiểm chứng giao diện Compose mới; đối chiếu kết quả build/lint và runtime trong RESULTS.md.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai05
& $adbPath -s $serial shell am start -W -n "vn.training.bai05/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

## 5. Test chức năng

Chuẩn bị vài ảnh **do app khác tạo** trên thiết bị (camera hệ thống hoặc ảnh demo), rồi ghi số ảnh app tạo trước mỗi case. Album đích là Pictures/BasicComponents.

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Từ chối quyền, mở app | DENIED, không crash; có thể thấy ảnh app đã tạo |
| TC02 | Cấp toàn bộ quyền đọc | FULL, grid đọc ảnh được phép, có ảnh của app khác |
| TC03 | API 34+: chọn đúng 2 ảnh từ app khác | PARTIAL, có hai ảnh đã chọn cộng ảnh app tạo nếu có |
| TC04 | Chọn lại tập ảnh được cấp quyền | Grid cập nhật đúng tập mới sau callback/onResume |
| TC05 | Thu hồi quyền trong Settings, quay lại | Trạng thái đổi, không giữ grid trái quyền |
| TC06 | DENIED → Photo Picker → chọn ảnh | Preview URI hoạt động mà không cần FULL |
| TC07 | Mở picker rồi hủy | Không thêm ảnh, không crash, last trước đó giữ nguyên |
| TC08 | Tạo và lưu bitmap JPEG | Có URI Saved, ảnh đọc được, MIME JPEG, IS_PENDING=0 |
| TC09 | Bấm mô phỏng lỗi lưu | Báo thất bại; không có ảnh đích mới hoặc pending rác |
| TC10 | Chụp TakePicture, xác nhận | Ảnh được import/publish; file tạm được dọn |
| TC11 | Mở camera rồi Back/hủy | Không có ảnh mới trong album đích; file tạm được dọn |
| TC12 | Khi camera mở, xoay hoặc làm app bị recreate rồi xác nhận | pending được khôi phục; import đúng, không crash |
| TC13 | Chọn ảnh và bấm mở Gallery hệ thống | Viewer đọc được URI; không có viewer thì thông báo rõ |

So số ảnh trước/sau theo album đích, không theo tổng ảnh toàn thiết bị có thể thay đổi. Để xác nhận metadata ảnh lưu:

~~~powershell
& $adbPath -s $serial shell content query --uri content://media/external/images/media --projection "_id:_display_name:mime_type:relative_path:is_pending" |
    Select-String "Demo_|BasicComponents"
~~~

Một số máy hạn chế query từ shell; khi đó dùng Device Explorer/debugger hoặc viewer, ghi giới hạn kiểm tra. Không biến lỗi quyền shell thành kết luận app lỗi.

Kiểm tra file tạm của bản debug:

~~~powershell
& $adbPath -s $serial shell run-as vn.training.bai05 ls -l files/camera
~~~

Sau callback, không còn file capture_* của lượt đó. Khi camera đang mở có thể thử process recreation bằng lệnh dưới, rồi xác nhận ảnh:

~~~powershell
& $adbPath -s $serial shell am kill vn.training.bai05
~~~

am kill chỉ giết process đủ điều kiện đang nền. Kiểm tra PID trước/sau để chắc có recreation. **Không thay bằng force-stop** cho case này vì force-stop có thể hủy luồng nhận kết quả.

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- FileProvider báo đường dẫn không hợp lệ: authority và files-path camera/ phải khớp.
- Saved nhưng Gallery không có ảnh: kiểm tra IS_PENDING, publish và album.
- Từ chối quyền vẫn thấy bitmap app: hành vi dự kiến, không phải quyền bị bỏ qua.
- Callback camera không biết URI: lưu pending trước launch và restore trong onCreate.
- Ảnh rác sau cancel: delete file tạm ở finally, không chỉ ở nhánh thành công.
- Hủy coroutine khác process bị kill đột ngột; cleanup không được bảo đảm khi process không còn chạy.

[RESULTS.md](RESULTS.md) có bằng chứng API 34 cho FULL/PARTIAL/DENIED, JPEG, lỗi cleanup, camera thành công/hủy/recreation. Photo Picker ở bài này chưa có bằng chứng runtime trong biên bản; chức năng xóa ảnh app khác là mở rộng chưa triển khai, không nằm trong checklist nghiệm thu bản hiện tại.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "GalleryDemo" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 5 và python ./verification/extra_checks.py gallery sau khi cập nhật cấu hình script.
