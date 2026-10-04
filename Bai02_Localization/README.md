# Bài 2: Localization
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Nhập tên dài; lần lượt đổi English/Việt/日本語/System; xoay, force-stop rồi mở lại. Kiểm tra tên còn nguyên và số 0/1/2/5.
Fallback đã được tạo chủ ý: chuỗi fallback chỉ có trong values mặc định. AppCompat autoStoreLocales cho <=32, hệ thống localeConfig cho >=33. Theo hệ thống dùng locale list rỗng.
## Log
`adb logcat -s ActivityTaskManager`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.
