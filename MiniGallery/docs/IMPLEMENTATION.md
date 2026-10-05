# Thiết kế và kế hoạch MiniGallery Compose

Ngày: 04/10/2026. Đích: D:\MiniGallery. Bản XML gốc giữ nguyên tại D:\AndroidTrainingExample.

Tạo ứng dụng độc lập com.example.minigallery với minSdk 29, compileSdk 36.1, targetSdk 36. Tái sử dụng model, MediaStore Repository, ViewModel và PermissionHelper; thay Activity/AppCompat, RecyclerView, XML dialog bằng ComponentActivity, Material 3, LazyVerticalGrid và Compose Dialog. Grid thích ứng chiều rộng, dialog giới hạn chiều cao và cuộn để dùng được khi xoay ngang. Detail chọn theo id saveable; batch ở ViewModel giữ qua thay đổi cấu hình.

Kế hoạch kiểm chứng:

1. Tạo bằng Android CLI, cấu hình compiler và chuyển logic cùng regression test sang namespace mới.
2. Viết và chạy Compose UI test trên screen tối thiểu, xác nhận các luồng chưa được triển khai.
3. Triển khai screen, banner quyền, search/sort, detail và import preview theo state.
4. Chạy unit test, lint, build và instrumentation gồm UI test và MediaStore thật.
5. Cài cùng bản XML, thử lưu ảnh nguồn từ thiết bị, kiểm tra hash/IS_PENDING và screenshot; ghi README tiếng Việt với phạm vi đã chạy.

Không cần Room, Retrofit, DI framework hay Navigation cho ứng dụng một màn hình. Không hứa phục hồi batch sau process death. Không sao chép bài học DayOne/DayTwo vào bản độc lập này.
