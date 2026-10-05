# Bài 3: Foreground Service
## Cấu hình
Kotlin + Jetpack Compose Material 3; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
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

Giao diện dùng `setContent`, `DemoScreen`, `Text` và `ActionButton` trong Kotlin Compose; không dùng layout XML, ViewBinding hay `DemoActivity`. Giữ XML tài nguyên/cấu hình Android. Template CLI và test mẫu cũ giữ riêng, không coi các test này là kiểm chứng bản Compose. Bài 4 chỉ build hai module server/client.
