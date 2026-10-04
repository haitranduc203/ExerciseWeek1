### Bài 5: Content Provider — Gallery và thêm ảnh

**Mục tiêu:** Tự query metadata và ghi bytes qua ContentResolver.

1. Chuẩn bị ít nhất 5 ảnh trên emulator/thiết bị, gồm ảnh do app khác tạo.
2. Implement Gallery dùng `MediaStore`, RecyclerView grid và Glide/Coil; query trên IO, đóng cursor.
3. Xử lý FULL/PARTIAL/DENIED và nút chọn lại ảnh trên Android 14+; không yêu cầu quyền toàn bộ để dùng chế độ partial.
4. Tạo bitmap đơn giản (ví dụ hình vuông có chữ), lưu JPEG/PNG qua `ImageSaver`; hiển thị URI, query lại và mở ảnh trong Gallery hệ thống.
5. Thêm chụp ảnh bằng `TakePicture()`; lưu URI qua recreate và dọn entry khi hủy.
6. Thêm Photo Picker để so sánh URI grant với quyền Gallery riêng. Xóa ảnh là phần mở rộng.

| Trường hợp | Kết quả cần đạt |
|---|---|
| Cấp toàn bộ quyền ảnh | Hiện các ảnh app được phép đọc trong collection |
| Chỉ chọn 2 ảnh trên Android 14+ | Hiện các ảnh được phép, báo partial; không báo lỗi quyền |
| Từ chối quyền | UI giải thích và cho chọn Photo Picker/cấp quyền; không crash |
| Đổi quyền trong settings rồi quay lại | Gallery kiểm tra quyền và query lại |
| Lưu bitmap thành công | MIME/đuôi file đúng; ảnh mở được; `IS_PENDING` về 0 |
| Stream/nén thất bại trước publish | Không báo thành công; xóa entry đã tạo nếu có |
| Hủy camera | Không để lại entry rỗng |
| Xóa ảnh app khác trên Android 10 | Sau cấp quyền, app thử delete lại |
| Xóa ảnh app khác trên Android 11+ | Hệ thống xử lý delete request, app refresh |

**Nghiệm thu:** Code có cả `query()` và `insert()`/`openOutputStream()`; chỉ dùng Photo Picker không đủ chứng minh đã biết query MediaStore. Có ảnh kết quả trước/sau và thử các trạng thái quyền trên thiết bị hỗ trợ.
