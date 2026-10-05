# Notification Practice

Một bài tập Kotlin/Jetpack Compose Material 3 luyện PendingIntent và Content Intent: tạo thông báo A/B, chạm để mở Detail, đánh dấu đã đọc bằng notification action và tái hiện lỗi chỉ đổi extras nhưng dùng chung token.

- Hướng dẫn tự code lại: [docs/PENDINGINTENT_CONTENTINTENT_GUIDE.md](docs/PENDINGINTENT_CONTENTINTENT_GUIDE.md).
- Kết quả kiểm chứng: [docs/VERIFICATION.md](docs/VERIFICATION.md).
- Android Studio: mở thư mục project, sync, chạy module **app**.
- CLI: chạy `scripts/Check-Project.ps1`, rồi `android run --device SERIAL --apks app/build/outputs/apk/debug/app-debug.apk`.
- Android 13+: bấm Cấp quyền thông báo trước khi tạo A/B.

Project dùng minSdk 29, targetSdk 36, compileSdk 36.1 và JDK 21. Đường dẫn SDK/JDK trong local.properties và gradle.properties theo máy phát triển; sửa nếu chạy trên máy khác.

Trong thử lỗi, tạo A rồi B → chạm notification A nhận ID B. Tắt thử lỗi để dùng requestCode riêng. Notification action luôn có token riêng để việc đánh dấu đã đọc hoạt động đúng ở cả hai chế độ.

Giao diện ở PracticeScreens.kt, Activity dùng setContent và Compose state. Dependencies dùng Compose BOM 2026.03.01 và compiler 2.2.10 tương thích Kotlin tích hợp của AGP 9.1.1. Tests dùng Compose UI testing.
