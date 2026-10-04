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
- Hai notification có ID riêng; xem log FullScreenDemo

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.

- FSI special access bị tắt: UI false; click notification cũ Sự kiện A mở đúng ID/label dù đã tạo B. POST_NOTIFICATIONS bị từ chối: UI Notifications=false, không crash khi gửi; khôi phục quyền sau test.

## Ma trận notification/FSI trên API34
| Trạng thái | Dự đoán | Thực đo |
|---|---|---|
| Notification cho phép, HIGH, màn hình mở khóa | Có thể heads-up | Có heads-up trong 02_after_actions.png |
| FSI bị tắt, notification cho phép | Không full-screen, click content vẫn hoạt động | canUseFullScreenIntent=false; Sự kiện A mở đúng ID cũ, evidence/content_intent.txt |
| POST_NOTIFICATIONS từ chối | Không gửi; UI giải thích | Notifications=false; nhấn gửi không crash, evidence/denied.txt |
| Gửi A rồi B | Token độc lập | ID 101/102 trong runtime.log; thêm test click A sau B đạt |
| Khóa màn hình, FSI cho phép | Phụ thuộc chính sách OS | Chưa kiểm chứng runtime |
| Người dùng giảm importance channel | Có thể không heads-up/full-screen | Chưa kiểm chứng runtime |
