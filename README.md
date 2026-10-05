# 7 bài thực hành Android Basic

## Hướng dẫn tự viết lại code và kiểm thử

Mỗi project có một file Markdown riêng, gồm cấu hình, thứ tự viết code, đoạn code cốt lõi, lệnh build/chạy, bảng test với kết quả mong đợi và cách ghi bằng chứng:

| Project | Hướng dẫn |
|---|---|
| Bài 1 — Activity / Task | [HUONG_DAN_CODE_VA_TEST.md](Bai01_ActivityTasks/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 2 — Localization | [HUONG_DAN_CODE_VA_TEST.md](Bai02_Localization/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 3 — Foreground Service | [HUONG_DAN_CODE_VA_TEST.md](Bai03_ForegroundService/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 4 — AIDL server/client | [HUONG_DAN_CODE_VA_TEST.md](Bai04_AIDL/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 5 — Gallery / Camera | [HUONG_DAN_CODE_VA_TEST.md](Bai05_Gallery/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 6 — BroadcastReceiver | [HUONG_DAN_CODE_VA_TEST.md](Bai06_Broadcast/HUONG_DAN_CODE_VA_TEST.md) |
| Bài 7 — Full-Screen Intent | [HUONG_DAN_CODE_VA_TEST.md](Bai07_FullScreenIntent/HUONG_DAN_CODE_VA_TEST.md) |
| MiniGallery — Jetpack Compose | [HUONG_DAN_CODE_VA_TEST.md](MiniGallery/HUONG_DAN_CODE_VA_TEST.md) |

Các hướng dẫn được đối chiếu mã nguồn và biên bản hiện có; những case chưa có bằng chứng runtime được ghi rõ để tiếp tục kiểm thử.

Mỗi thư mục là một project Android Studio độc lập; Kotlin, minSdk 31, target/compileSdk 36, AGP 9.1.1, Gradle 9.3.1, Java 17+. Bài 2–7 dùng Jetpack Compose Material 3; bài 1 hiện dùng Views.
Bài 4 có 2 module server/client dùng contract AIDL chung và cùng debug certificate.
Các bài 2–7 đã chuyển giao diện sang Compose, gồm client/server của AIDL và AlarmActivity của bài 7. XML tài nguyên và cấu hình Android vẫn được giữ; không dùng layout XML hay ViewBinding trong các bài này.

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

Mở riêng thư mục `MiniGallery/` trong Android Studio. Project có 15 unit test và 7 instrumentation test; [biên bản kiểm chứng và ảnh nghiệm thu](MiniGallery/docs/verification/RESULTS.md). Script BUILD_ALL.ps1 dành cho bảy bài thực hành phía trên; build MiniGallery theo README riêng.
