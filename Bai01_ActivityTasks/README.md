# Bài 1: Activity / Task
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
A → BStandard → BStandard; A → BTop → BTop; A → BTop → A → BTop. So sánh instance ID và onNewIntent.
CLEAR_TOP với BStandard, rồi CLEAR_TOP|SINGLE_TOP. D → A so taskId; E → A; NEW_DOCUMENT mở nhiều E. Login xóa task; notification Detail rồi Back.
NEW_TASK chọn task theo affinity, không luôn tạo task mới; D có task riêng chỉ chứa D, E có thể chứa Activity khác phía trên. BStandard/BTop được tách để so sánh mà không cần sửa manifest.
## Log
`adb logcat -s StackDemo`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
