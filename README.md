# 7 bài thực hành Android Basic
Mỗi thư mục là một project Android Studio độc lập; Kotlin, Views, minSdk 31, target/compileSdk 36, AGP 9.1.1, Gradle 9.3.1, Java 17+.
Bài 4 có 2 module server/client dùng contract AIDL chung và cùng debug certificate.
Được tạo bằng Android CLI empty-activity, chuyển sang Views phù hợp tài liệu.

## Build và chạy
Mở từng folder trong Android Studio và Gradle Sync. local.properties đang trỏ SDK máy này; khi chuyển máy cần cập nhật.
PowerShell: đặt JAVA_HOME vào JDK 17+ (ví dụ JBR Android Studio), rồi chạy .\gradlew.bat assembleDebug lintDebug.
Với bài 4: .\gradlew.bat :server:assembleDebug :client:assembleDebug.
APK trong <module>/build/outputs/apk/debug. Cài bằng android run --apks=<đường dẫn APK> --activity=<package>.MainActivity.
Xem README.md, REQUIREMENTS.md, RESULTS.md và evidence/ trong từng bài.
Không coi build thành công là đã xác nhận mọi hành vi runtime; bảng RESULTS đánh dấu kịch bản chưa chạy.

## Kết quả
Đã build/lint cả 7 project và 8 APK. Chạy launcher cả 7 trên Pixel 7 Pro Android14/API34.
Đã đo launchMode standard/singleTop, đổi locale/persistence, FGS Play/Pause/Stop và worker thread, AIDL hai phiên/callback/server death/reconnect, Gallery FULL/PARTIAL/DENIED/bitmap/camera/cleanup, receiver lifecycle, notification ID riêng. Chi tiết và những nhánh chưa kiểm chứng nằm trong RESULTS.md từng bài.
Các API31/32/33/35/36, dataSync timeout API35, full-screen khi khóa và channel giảm importance chưa kiểm chứng runtime trên các cấu hình đó.
Chạy `.\BUILD_ALL.ps1` để build/lint cả bộ; script tìm JBR Android Studio khi JAVA_HOME chưa được đặt. Không có unit-test suite riêng.

## MiniGallery Jetpack Compose

Bổ sung project độc lập [MiniGallery](MiniGallery/README.md): đọc ảnh qua MediaStore, tìm kiếm/sắp xếp, xem chi tiết, chọn tối đa 10 ảnh bằng Photo Picker và lưu bản sao. UI dùng Jetpack Compose Material 3; minSdk 29, targetSdk 36, compileSdk 36.1, JBR/JDK 21.

Mở riêng thư mục `MiniGallery/` trong Android Studio. Project có 15 unit test và 7 instrumentation test; [biên bản kiểm chứng và ảnh nghiệm thu](MiniGallery/docs/verification/RESULTS.md). Script BUILD_ALL.ps1 hiện dành cho bảy bài Views phía trên; build bản Compose theo README riêng.
