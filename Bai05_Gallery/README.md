# Bài 5: MediaStore Gallery
## Cấu hình
Kotlin + AndroidX Views; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1.
## Build
`./gradlew.bat assembleDebug lintDebug` (PowerShell: `.\gradlew.bat ...`). Bài 4 build cả `server` và `client`.
## Chạy và nghiệm thu
Chuẩn bị >=5 ảnh, có ảnh app khác: dùng camera hệ thống chụp, hoặc adb push ảnh JPEG vào /sdcard/Pictures rồi scan media.
FULL/PARTIAL chọn đúng 2 ảnh/DENIED → refresh. Đổi permission ở Settings rồi quay lại. Bitmap save → xem URI → mở Gallery. Photo Picker chỉ cấp URI grant, không thay thế quyền query collection.
Camera: chụp thành công, hủy, xoay khi camera mở; kiểm tra không entry rỗng. Nút lỗi lưu mô phỏng kiểm chứng cleanup trước publish. Ảnh app tự tạo đọc được dù DENIED. Xóa ảnh app khác là phần mở rộng tùy chọn chưa triển khai.
## Log
`adb logcat -s ActivityTaskManager`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Template CLI cũ được giữ trên disk làm tham chiếu; các test Compose ở src/test và src/androidTest được loại khỏi source set của bài Views. Với bài 4, module app mẫu không nằm trong settings.gradle.kts; chỉ server/client là bài nộp.

## Lưu ý camera đã kiểm chứng
TakePicture ghi vào FileProvider files/camera; sau success app chép JPEG vào MediaStore qua IS_PENDING rồi publish. Không cấp URI của entry IS_PENDING cho camera ngoài app vì provider trên emulator API34 từ chối non-owner. URI lưu cả Bundle và trạng thái bền vững; file tạm dọn khi success/cancel/launch failed.
Máy thử đã có 5 ảnh IMG_20261004_115540..115546 do com.android.camera2 tạo (owner_package_name trong evidence/media_after_cancel.txt).
