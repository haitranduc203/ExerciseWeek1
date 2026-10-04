### Bài 2: Đa ngôn ngữ

**Mục tiêu:** App English/Vietnamese/Japanese đổi ngôn ngữ đúng và giữ lựa chọn.

1. Tạo đầy đủ `values/strings.xml`, `values-vi/strings.xml`, `values-ja/strings.xml`.
2. Thêm settings chọn 3 ngôn ngữ và “Theo hệ thống”; dùng `AppCompatDelegate.setApplicationLocales()`.
3. Cấu hình `localeConfig` và lưu locale trên Android 12 trở xuống.
4. Hiển thị lời chào có tên và số tin nhắn; kiểm tra số 0, 1, 2, 5. Số 0 dùng string riêng nếu muốn câu “Không có tin nhắn”.
5. Xóa thử một bản dịch để quan sát fallback; kiểm tra chuỗi dài và màn hình recreate.

**Nghiệm thu:** Đổi locale không mất dữ liệu người dùng đang nhập; mở lại app vẫn giữ lựa chọn; “Theo hệ thống” cập nhật theo hệ thống. Trên Android 13+ lựa chọn trong settings app và system settings đồng bộ. Có kết quả kiểm tra trên API 31/32 và 33+ nếu có thiết bị.
