# Bài 6 — Hướng dẫn Broadcast với Jetpack Compose

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

1. Thay TextView bằng state `battery`, `airplane`, `custom`, `network`; `setContent { DemoScreen { Text(...); ActionButton(...) { ... } } }`.
2. BroadcastReceiver hệ thống cập nhật battery/airplane state. Receiver nội bộ tăng count và gán custom state để Compose hiển thị ngay.
3. Giữ đăng ký receiver và NetworkCallback trong onStart, hủy trong onStop; không đăng ký trực tiếp trong thân composable.
4. NetworkCallback cập nhật state qua runOnUiThread và kiểm tra active; giữ phân biệt INTERNET/VALIDATED.
5. count lưu qua Bundle khi Activity recreate. Nút nội bộ gửi Intent giới hạn package; receiver nội bộ NOT_EXPORTED và receiver hệ thống EXPORTED như trước.
6. Kiểm tra gửi một lần tăng đúng một, Home/quay lại không đăng ký trùng; dữ liệu pin và mạng vẫn cập nhật.

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
& $adbPath -s $serial shell am start -W -n "vn.training.bai06/.MainActivity"
& $adbPath -s $serial logcat -s "BroadcastDemo"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Các test template cũ không kiểm chứng giao diện Compose mới; đối chiếu kết quả build/lint và runtime trong RESULTS.md.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai06
& $adbPath -s $serial shell am start -W -n "vn.training.bai06/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

## 5. Test chức năng

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Mở app | Có log registered; pin được cập nhật; trạng thái mạng hiển thị |
| TC02 | Bấm gửi nội bộ một lần | count tăng đúng 1, một log custom |
| TC03 | Bấm tiếp ba lần | count tăng thêm 3, không có callback trùng |
| TC04 | Home → quay lại **task cũ bằng Recents** → bấm gửi | onStop hủy, onStart đăng ký lại; count tăng 1 |
| TC05 | Lặp TC04 nhiều lần | Không nhân receiver/callback, không lỗi Receiver not registered |
| TC06 | Xoay màn hình | count được restore; receiver cũ hủy, mới đăng ký |
| TC07 | Tắt/bật mạng, quay lại app | Có mạng/INTERNET/VALIDATED cập nhật theo mạng thực tế |
| TC08 | Đổi airplane mode khi Activity vẫn started (ví dụ Quick Settings) | Khi có broadcast, UI nhận state mới |
| TC09 | Đổi airplane mode lúc app đã stopped rồi quay lại | Ghi trạng thái thực tế; receiver không nhận sự kiện khi stopped |
| TC10 | Thay đổi mức pin trên emulator | Phần trăm cập nhật đúng level/scale |
| TC11 | Gửi action CUSTOM từ ADB bên ngoài app | Internal receiver không tăng count do NOT_EXPORTED |
| TC12 | Mở/đóng app, xoay, Home liên tục | Không leak/crash, đăng ký và hủy đối xứng |

**Chú ý TC09:** mã hiện tại chỉ đọc airplane state ban đầu trong onCreate và cập nhật qua broadcast. Nếu đổi khi Activity đã onStop rồi quay lại cùng instance, UI có thể giữ giá trị cũ. Ghi lại hạn chế này; nếu muốn hoàn thiện, đọc lại Settings.Global ở onStart. Không ghi PASS cho việc tự refresh airplane mà code hiện tại chưa làm.

Không mở MainActivity bằng am start thông thường để kiểm tra giữ counter ở TC04: Activity standard có thể tạo instance khác. Dùng Recents hoặc thao tác launcher quay lại task hiện có.

Mô phỏng pin trên emulator và khôi phục ngay sau test:

~~~powershell
& $adbPath -s $serial shell dumpsys battery set level 25
# Quan sát UI pin 25%
& $adbPath -s $serial shell dumpsys battery reset
~~~

Kiểm tra receiver nội bộ không nhận từ shell:

~~~powershell
& $adbPath -s $serial shell am broadcast -a vn.training.bai06.CUSTOM -p vn.training.bai06 --es message "outside-app"
~~~

Kết quả lệnh shell tự nó không chứng minh receiver chạy; đối chiếu counter và log. Dùng nút trong app để kiểm tra chiều được phép.

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- Broadcast tăng count hai lần: có thể đăng ký trùng hoặc còn receiver cũ.
- Receiver not registered: hủy không đối xứng hoặc cờ registered sai.
- Callback mạng sửa UI trực tiếp: NetworkCallback cần chuyển cập nhật UI về main.
- Có mạng=true nhưng VALIDATED=false: có thể chưa xác nhận truy cập Internet/captive portal.
- Counter reset khi mở lại: phân biệt instance mới, force-stop và recreate; Bundle không phải lưu bền vững.
- Máy bay bị cũ sau Settings: xem lưu ý TC09; không nhầm với lỗi broadcast đang nhận.

[RESULTS.md](RESULTS.md) có bằng chứng API 34 cho gửi nội bộ một callback/lần và Home/quay lại không đăng ký trùng. Các tình huống pin/mạng/máy bay cần đo bổ sung; runtime_error.txt là bằng chứng thử cũ được giải thích trong RESULTS, không dùng làm kết quả của lượt test mới.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "BroadcastDemo" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 6 sau khi cập nhật cấu hình script.
