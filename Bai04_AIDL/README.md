# Bài 4: AIDL
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Build/cài server trước client (cùng debug keystore mặc định). Client bind hai phiên đồng thời; thêm sách, đọc count/list, removeBook(id).
Log PID client và server :books khác nhau; callback về main thread; get/add/remove trên IO. Thử force-stop server rồi nhấn Kết nối lại.
Signature permission ngăn app ký khác bind; ComponentName tường minh; RemoteCallbackList xử lý binder death. in = gửi client→server, out = server→client, inout = cả hai. Các method service là đồng bộ, callback oneway không đợi phản hồi; giữ dữ liệu RAM chỉ dùng demo, server bị giết sẽ mất sách.
## Log
`adb logcat -s BookServer BookClient`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `client/build/outputs/apk/debug/client-debug.apk`
- `server/build/outputs/apk/debug/server-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
