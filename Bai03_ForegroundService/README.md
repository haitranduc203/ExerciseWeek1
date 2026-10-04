# Bài 3: Foreground Service
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Play → Home → notification Pause/Play → mở player → Stop; xoay Activity; thử từ chối POST_NOTIFICATIONS. Bộ đếm mô phỏng chạy trên Dispatchers.Default.
Từ chối notification không tự cấm FGS; hệ thống có thể chỉ hiển thị trong task manager. Service chạy cùng process nhưng coroutine chạy thread khác.
Mở rộng dataSync: trên emulator API35+, adb shell device_config put activity_manager data_sync_fgs_timeout_duration 10000; start dataSync rồi Home. Đối chiếu SyncDemo onTimeout, sau đó adb shell device_config delete activity_manager data_sync_fgs_timeout_duration. Không đổi timeout trên máy cá nhân ngoài kiểm thử; mediaPlayback không dùng timeout này.
## Log
`adb logcat -s PlayerDemo SyncDemo`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
