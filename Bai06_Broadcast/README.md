# Bài 6: Broadcast
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Gửi custom 1 lần → count tăng đúng 1. Home/mở lại nhiều lần; xoay; bật/tắt máy bay, mạng; log registered/unregistered không trùng.
Receiver hệ thống EXPORTED (action được hệ thống bảo vệ); custom NOT_EXPORTED và Intent giới hạn package. NetworkCallback phân biệt INTERNET với VALIDATED.
Không có boot receiver vì bài yêu cầu có điều kiện nếu làm boot; công việc dài từ boot nên WorkManager. goAsync không bỏ giới hạn thời gian broadcast.
## Log
`adb logcat -s BroadcastDemo`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
