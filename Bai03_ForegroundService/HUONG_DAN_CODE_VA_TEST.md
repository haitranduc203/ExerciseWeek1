# Bài 3 — Hướng dẫn Foreground Service với Jetpack Compose

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

1. Trong `MainActivity`, tạo `status by mutableStateOf("")`, gọi `setContent { DemoScreen { Text(status); ActionButton(...) { ... } } }`.
2. Giữ các nút cấp POST_NOTIFICATIONS, Play/Start, Pause, Stop và start/stop dataSync. `Intent` trong lambda giao diện dùng `this@MainActivity` làm Context.
3. Coroutine trong `lifecycleScope` đọc trạng thái `PlayerService` và gán `status`; Compose cập nhật Text khi state đổi.
4. Giữ `PlayerService`, `SyncService`, notification actions, foregroundServiceType và xử lý timeout trong Service. Giao diện chuyển Compose không thay đổi vòng đời Service.
5. Kiểm tra Play/Pause/Stop cả trong app và notification; bộ đếm mô phỏng không phát audio.

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
& $adbPath -s $serial shell am start -W -n "vn.training.bai03/.MainActivity"
& $adbPath -s $serial logcat -s "PlayerDemo"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Các test template cũ không kiểm chứng giao diện Compose mới; đối chiếu kết quả build/lint và runtime trong RESULTS.md.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai03
& $adbPath -s $serial shell am start -W -n "vn.training.bai03/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

Để theo dõi cả hai service: & $adbPath -s $serial logcat -s PlayerDemo SyncDemo.

## 5. Test chức năng

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Play, đợi 3–5 giây | Service=true, playing=true, elapsed tăng, có notification |
| TC02 | Bấm Play liên tiếp 5 lần | Không nhân tốc độ đếm, chỉ một ticker |
| TC03 | Pause, đợi 3 giây | playing=false, elapsed giữ nguyên sau khi trạng thái ổn định |
| TC04 | PLAY từ notification | Bộ đếm tiếp tục; app không cần ở foreground |
| TC05 | PAUSE từ notification | Bộ đếm dừng tăng; notification đổi trạng thái |
| TC06 | STOP từ notification | Notification bị gỡ; service ngừng và state reset |
| TC07 | Play → Home → đợi → quay lại task cũ | Service vẫn chạy; elapsed đã tăng |
| TC08 | Stop từ Activity | running=false, seconds=0; log stopped; không tick tiếp |
| TC09 | Log lúc đang chạy | thread là worker, không phải main |
| TC10 | API 33+: từ chối notification rồi Play | Kiểm tra service còn hoạt động; cách hiển thị của hệ thống được ghi riêng |
| TC11 | Start dataSync rồi Stop dataSync | Notification ID 2 xuất hiện rồi biến mất; không log tiếp |
| TC12 | API 35+: rút ngắn timeout, start Sync rồi đưa app vào nền | onTimeout được log, service tự stop, không crash |

Có thể xem service bằng:

~~~powershell
& $adbPath -s $serial shell dumpsys activity services vn.training.bai03
~~~

### Test timeout trên emulator API 35+

Chỉ làm trên thiết bị thử nghiệm riêng. Theo [Foreground service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout), có thể rút ngắn thời gian bằng device_config. Lưu giá trị cũ để khôi phục chính xác:

~~~powershell
$oldSyncTimeout = (& $adbPath -s $serial shell device_config get activity_manager data_sync_fgs_timeout_duration).Trim()
& $adbPath -s $serial shell device_config put activity_manager data_sync_fgs_timeout_duration 10000
~~~

Bấm Start dataSync khi app đang hiển thị, bấm Home, đợi ít nhất 10 giây và quan sát SyncDemo. Sau test:

~~~powershell
if ($oldSyncTimeout -eq "null") {
    & $adbPath -s $serial shell device_config delete activity_manager data_sync_fgs_timeout_duration
} else {
    & $adbPath -s $serial shell device_config put activity_manager data_sync_fgs_timeout_duration $oldSyncTimeout
}
~~~

Không dùng giới hạn dataSync để kiểm thử PlayerService vì player có type mediaPlayback. Nếu emulator không cho sửa cấu hình, ghi case chưa chạy.

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- Không gọi startForeground kịp: tạo channel/notification trước, không chờ xử lý dài.
- SecurityException: đối chiếu type service và quyền foreground tương ứng.
- Tick tăng nhanh sau nhiều lần Play: tạo ticker trùng; kiểm tra guard.
- Stop xong còn log: quên hủy scope hoặc quên stopSelf.
- Không có âm thanh: bản này chỉ mô phỏng bộ đếm.
- State companion không tồn tại sau process death: đây không phải cơ chế lưu bền vững; bản demo reset.

[RESULTS.md](RESULTS.md) ghi Play/Pause/Stop và worker thread đã đo trên API 34. Timeout API 35+ và các nhánh chưa ghi kết quả phải chạy thêm; tài liệu này không chạy lại runtime.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "PlayerDemo" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 3 sau khi cập nhật cấu hình script.
