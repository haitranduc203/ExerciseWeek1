# Bài 7: Full-Screen Intent
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
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

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
