# Bài 6: Broadcast
## Cấu hình
Kotlin + Jetpack Compose Material 3; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
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

Giao diện dùng `setContent`, `DemoScreen`, `Text` và `ActionButton` trong Kotlin Compose; không dùng layout XML, ViewBinding hay `DemoActivity`. Giữ XML tài nguyên/cấu hình Android. Template CLI và test mẫu cũ giữ riêng, không coi các test này là kiểm chứng bản Compose. Bài 4 chỉ build hai module server/client.
