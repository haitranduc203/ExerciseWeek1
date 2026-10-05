# Bài 4 — Hướng dẫn AIDL với Jetpack Compose

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

1. Project chỉ gồm `server` và `client`; giữ `contract/java`, `contract/aidl` và `buildFeatures { aidl = true; compose = true }` ở cả hai module.
2. Trong mỗi module, `ComposeActivity.kt` cung cấp nền giao diện; server `MainActivity` dùng `setContent` để hiển thị mô tả và PID.
3. Client dùng state `status`, `output`, `input`, `operationsEnabled`; hiển thị `Text`, `OutlinedTextField` và các `ActionButton`. Lưu input trong Bundle qua `onSaveInstanceState`.
4. Callback AIDL chuyển về main thread, cập nhật output state. `refresh()` đặt trạng thái enabled theo các session đã đăng ký listener.
5. Giữ bind hai session trong onStart và disconnect trong onStop. Các lời gọi đồng bộ get/add/remove vẫn chạy trên Dispatchers.IO; contract và BookService không đổi.
6. Giữ signature permission, ComponentName tường minh và process `:books`. Cài server trước client với cùng debug certificate.

## 4. Build và cài hai APK

PowerShell chạy tại Bai04_AIDL:

~~~powershell
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
./gradlew.bat :server:assembleDebug :client:assembleDebug :server:lintDebug :client:lintDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "Build/lint thất bại" }

$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) { $sdkPath = "$env:LOCALAPPDATA/Android/Sdk" }
$adbPath = Join-Path $sdkPath "platform-tools/adb.exe"
& $adbPath devices
$serial = "emulator-5554"
& $adbPath -s $serial install -r "./server/build/outputs/apk/debug/server-debug.apk"
& $adbPath -s $serial install -r "./client/build/outputs/apk/debug/client-debug.apk"
& $adbPath -s $serial shell am start -W -n "vn.training.books.client/.MainActivity"
& $adbPath -s $serial logcat -s BookServer BookClient
~~~

Đổi JBR/SDK/serial theo máy. Cài server trước client, đợi UI connected=2. Lint reports nằm trong server/build/reports và client/build/reports. Bài này chưa có unit/instrumentation suite riêng.

## 5. Test chức năng

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Cài đủ hai APK, mở client | connected=2, các nút RPC được bật |
| TC02 | Nhập “Android IPC”, bấm thêm một lần | Hai sách ID khác nhau, mỗi Session nhận callback cho hai sách |
| TC03 | Bấm lấy danh sách/count | Có hai sách mới; count tăng 2 so với trước, không luôn bằng 2 nếu có dữ liệu cũ |
| TC04 | Nhập ID đang có, bấm removeBook | removed=true; lấy danh sách lại thấy count giảm 1 |
| TC05 | Xóa lại cùng ID | removed=false; app không crash |
| TC06 | Bấm thêm nhiều lần | Count tăng theo số ID mới; UI không bị chặn bởi RPC |
| TC07 | Home, quay lại bằng Recents, thêm một lần | Hai phiên mới được kết nối, không bị nhân callback do đăng ký cũ |
| TC08 | Force-stop server lúc client còn mở | UI báo mất kết nối, nút RPC bị vô hiệu, không crash |
| TC09 | Sau TC08 bấm Kết nối lại | connected=2; thao tác RPC chạy lại; kho sách có thể trống |
| TC10 | Chạy client trên thiết bị chưa cài server | Bind thất bại được xử lý, nút RPC không dùng được |
| TC11 | Kiểm tra PID trong log | Server :books khác PID client |
| TC12 | Dùng APK client thử nghiệm ký certificate khác | Bind bị từ chối bởi signature permission |

TC12 cần một APK thử nghiệm riêng; đừng thay keystore của hai module đang dùng. Không giả định đã đạt chỉ vì manifest có permission.

Lệnh làm chết server cho TC08:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.books.server
~~~

Nếu thiết bị giữ package server ở trạng thái stopped và không bind lại được, mở launcher server rồi bấm Kết nối lại:

~~~powershell
& $adbPath -s $serial shell am start -W -n "vn.training.books.server/.MainActivity"
~~~

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- Không sinh IBookService: bật aidl, đúng sourceSet, package AIDL và đường dẫn contract.
- Parcelable đọc sai: giữ thứ tự id/title giống khi ghi.
- SecurityException khi bind: so certificate và permission signature.
- Callback crash khi sửa TextView: callback Binder không bảo đảm ở main thread.
- Count sai/lỗi concurrent modification: đọc ghi cùng kho phải dùng cùng khóa.
- Mất sách sau server chết hoặc unbind hết: bản này lưu RAM, chưa dùng database.
- onBindingDied khác onServiceDisconnected; thao tác reconnect phải giải phóng binding cũ.

[RESULTS.md](RESULTS.md) ghi API 34 đã đo hai phiên bind, callback, count=2 từ kho mới, server death/reconnect. Các case khác cần đo bổ sung; không coi hai Session là bằng chứng kiểm thử hai process client độc lập.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "BookClient" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Khi thu thập bằng chứng cho IPC, lấy thêm tag BookServer. Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 4 sau khi cập nhật cấu hình script.
