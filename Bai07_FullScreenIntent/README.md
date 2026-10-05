# Bài 7: Full-Screen Intent
## Cấu hình
Kotlin + Jetpack Compose Material 3; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Cấp notification và full-screen special access; gửi cuộc gọi sau 10s rồi khóa; thử mở khóa, tắt FSI, từ chối notification, giảm importance channel.
Gửi hai sự kiện; mở notification cũ kiểm tra ID/label không bị đổi. PendingIntent có requestCode và URI riêng. Khi không có FSI vẫn có content intent nếu notification được phép. Không tự mở settings; hệ thống có thể chỉ heads-up.
Tham khảo: https://developer.android.com/about/versions/14/behavior-changes-14 và https://developer.android.com/guide/topics/resources/app-languages
## Log
`adb logcat -s FullScreenDemo`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Giao diện dùng `setContent`, `DemoScreen`, `Text` và `ActionButton` trong Kotlin Compose; không dùng layout XML, ViewBinding hay `DemoActivity`. Giữ XML tài nguyên/cấu hình Android. Template CLI và test mẫu cũ giữ riêng, không coi các test này là kiểm chứng bản Compose. Bài 4 chỉ build hai module server/client.

Cả MainActivity và AlarmActivity dùng Compose; onNewIntent cập nhật state để hiển thị đúng sự kiện mới.
