# Kết quả kiểm chứng
Build APK và lintDebug thành công (exit 0). Xem evidence/build.log. Lint còn cảnh báo học tập như chuỗi UI hardcode; không có lỗi lint. Không có unit tests riêng; kiểm chứng chính bằng build/lint và thao tác thiết bị thực tế.

| Kịch bản | Dự đoán | Kết quả thực đo |
|---|---|---|
| Cài APK và mở launcher | UI mở không crash | Đạt trên Android 14/API34; evidence/01_launcher.png |
| Đường chạy chính trong README | Hoạt động theo yêu cầu | Các trường hợp đã đo được liệt kê bên dưới; phần còn lại chưa kiểm chứng |
| Tình huống lỗi/quyền trong REQUIREMENTS | Không crash, phản hồi rõ | Xem chi tiết đã đo bên dưới; các nhánh khác chưa kiểm chứng |
| API31/32/33/34/35/36 khác thiết bị hiện có | Tương thích theo guard API | Chưa kiểm chứng runtime |

## Đã đo trên Pixel 7 Pro Android 14 (API34)
- Cài APK và mở launcher không crash (Android 14/API34)
- DENIED không crash, lưu bitmap JPEG thành công, lỗi lưu mô phỏng cleanup

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.

- TakePicture qua FileProvider thành công sau khi kill process nền, khôi phục URI và dọn file tạm; hủy camera cũng cleanup. Xem camera_recreation.log.

## Ma trận quyền ảnh thực đo API34
| Trường hợp | Dự đoán | Kết quả thực tế |
|---|---|---|
| FULL | Query ảnh cả app khác | 97 ảnh trước TakePicture, gồm 5 ảnh camera hệ thống; gallery_full_layout.json |
| PARTIAL chọn 2 ảnh | Hai ảnh được chọn cộng ảnh do app tạo | 3 ảnh = 2 camera + 1 bitmap của app; 03_partial_two.png |
| DENIED | Không crash; vẫn đọc ảnh do app tạo | 1 bitmap của app; 02_after_actions.png |
| Lưu bitmap | JPEG MIME, IS_PENDING=0 | URI 1000000497, metadata media_after_cancel.txt |
| Lỗi trước publish | Cleanup, không thông báo thành công | UI báo lỗi mô phỏng và số ảnh không tăng |
| TakePicture success | Import rồi publish | URI 1000000506; 04_camera_saved.png |
| TakePicture cancel | Cleanup file tạm | camera_cancel_final.txt |
| Camera + process recreation | Khôi phục URI và không crash | camera_recreation.log |
| Photo Picker / xóa ảnh app khác | URI grant / consent | Photo Picker có code; chưa kiểm chứng runtime. Xóa ảnh là mở rộng chưa triển khai |
