# Bài 7 — Hướng dẫn Full-Screen Intent với Jetpack Compose

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

1. MainActivity dùng status state và `setContent { DemoScreen { ... } }` cho các nút xin quyền, mở settings, gửi ngay/trễ và gửi hai sự kiện.
2. Giữ NotificationChannel, kiểm tra POST_NOTIFICATIONS/canUseFullScreenIntent và PendingIntent có requestCode/URI riêng từng event ID.
3. AlarmActivity cũng kế thừa ComposeActivity, dùng event state để hiển thị label/ID và nút đóng.
4. onCreate giữ setShowWhenLocked/setTurnScreenOn. onNewIntent gán intent mới rồi gọi render để state cập nhật đúng event.
5. Nút đóng đọc event ID hiện tại, hủy notification tương ứng và finish. Không tự mở AlarmActivity khi notification bị từ chối.
6. Kiểm tra cả Content Intent, cập nhật event mới và các trạng thái khóa/mở khóa; hành vi full-screen vẫn do permission/channel và hệ thống quyết định.

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
& $adbPath -s $serial shell am start -W -n "vn.training.bai07/.MainActivity"
& $adbPath -s $serial logcat -s "FullScreenDemo"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Các test template cũ không kiểm chứng giao diện Compose mới; đối chiếu kết quả build/lint và runtime trong RESULTS.md.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai07
& $adbPath -s $serial shell am start -W -n "vn.training.bai07/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

## 5. Ma trận test quyền và trạng thái màn hình

Dùng thiết bị API 34+ để có special access. Trước mỗi case ghi cả notification permission, channel importance, FSI access, trạng thái khóa và DND.

| Case | Thiết lập và thao tác | Kết quả cần kiểm tra |
|---|---|---|
| TC01 | Notification cho phép, HIGH, FSI cho phép; gửi khi mở khóa | Có notification; có thể heads-up; chạm mở đúng label/ID |
| TC02 | Như TC01; bấm cuộc gọi sau 10s rồi khóa màn hình | Ghi có/không full-screen thực tế; mở đúng event, không crash |
| TC03 | Tắt FSI access; giữ notification cho phép; gửi event | Không gắn FSI; content notification vẫn mở đúng event |
| TC04 | Từ chối POST_NOTIFICATIONS; bấm gửi | App thông báo quyền bị tắt, không crash; không tạo event notification mới |
| TC05 | Giảm importance channel trong Settings rồi gửi | Ghi cách hiển thị thực tế; không yêu cầu heads-up/full-screen |
| TC06 | Tắt toàn bộ notification của app trong Settings | UI Notifications=false, nút gửi phản hồi rõ |
| TC07 | Gửi A rồi B; chạm notification A | Mở label A và ID A, không bị ghi đè bởi B |
| TC08 | Sau TC07 chạm notification B | Mở đúng label/ID B |
| TC09 | Đóng event đang mở | Notification đúng ID được hủy; event khác còn theo trạng thái thực tế |
| TC10 | Quay về app từ cài đặt FSI/channel | Trạng thái UI đọc lại, không giữ giá trị cũ |
| TC11 | API 31/32 | Không xin POST runtime; không gọi API canUseFullScreenIntent |
| TC12 | API 33 | Có POST runtime; không mở trang special access API 34 |

Bài kiểm tra TC02 cần đối chiếu quyền và OS: việc hệ thống không bật full-screen không tự động là lỗi code. Nếu có notification nhưng channel bị tắt hoặc DND tác động, ghi điều kiện đó. Chạm nội dung là đường kiểm tra dữ liệu rõ nhất.

### Test từ chối quyền bằng ADB (API 33+)

Ưu tiên Settings để mô phỏng thao tác người dùng. Nếu dùng ADB, ghi lại quyền ban đầu và khôi phục về trạng thái đó sau test:

~~~powershell
& $adbPath -s $serial shell pm revoke vn.training.bai07 android.permission.POST_NOTIFICATIONS
# Quay lại app, quan sát trạng thái và bấm gửi
# Chỉ grant nếu trước test quyền vốn được cấp:
& $adbPath -s $serial shell pm grant vn.training.bai07 android.permission.POST_NOTIFICATIONS
~~~

Có thể kiểm tra token/notification bằng:

~~~powershell
& $adbPath -s $serial shell dumpsys notification |
    Select-String "vn.training.bai07|demo://event|alarms"
~~~

Không dùng số event cụ thể 101/102 làm điều kiện cố định: ID được lưu và tăng sau mỗi lượt gửi.

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- Chạm A nhưng ra B: PendingIntent không phân biệt; kiểm tra requestCode/data URI.
- HIGH trong code nhưng UI importance thấp: người dùng đã đổi channel; tạo lại cùng ID không tự nâng lựa chọn đó.
- FSI true mà không full-screen: ghi lock state, channel, DND và OS; quyền chỉ là một điều kiện.
- Chờ 10 giây không gửi sau force-stop: job thuộc lifecycleScope, chưa phải báo thức được lên lịch bền vững.
- Crash trên API cũ: guard cho POST và API 34 special access.
- UI event cũ sau Intent mới: gọi setIntent trong onNewIntent.

[RESULTS.md](RESULTS.md) có bằng chứng API 34 cho hai ID riêng, FSI bị tắt vẫn mở đúng A sau B, từ chối POST và heads-up khi mở khóa. **Khóa màn hình có FSI và channel giảm importance chưa được biên bản xác nhận runtime**. Tài liệu này không chạy lại những case đó.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "FullScreenDemo" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 7 và python ./verification/extra_checks.py fsi sau khi sửa cấu hình script. Kiểm tra việc khôi phục quyền nếu script bị dừng giữa chừng.
