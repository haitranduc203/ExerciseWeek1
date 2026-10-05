# Kết quả kiểm chứng
## Bản Jetpack Compose — 05/10/2026

- `:app:assembleDebug :app:lintDebug`: **BUILD SUCCESSFUL**. Lint: 0 lỗi, 36 cảnh báo; còn cảnh báo phiên bản dependency/SDK, tài nguyên mẫu chưa dùng và gợi ý Kotlin/catalog.
- Pixel_6 AVD Android 15/API 35: launcher mở; sau cấp POST_NOTIFICATIONS hiển thị `Notifications=true`.
- Gửi hai sự kiện tạo A #101 và B #102. Bấm từng notification trong shade: AlarmActivity Compose hiển thị lần lượt `Event ID=101` và `Event ID=102`; nút đóng hoạt động.
- Log: `fullScreen=true importance=4`; phép đo này là mở Content Intent khi máy đang mở khóa, không chứng minh full-screen khi khóa.
- Bằng chứng: [Main UI](evidence/compose_main.xml), [Alarm UI](evidence/compose_alarm.xml), [notification](evidence/compose_notifications.xml), [log](evidence/compose_runtime.log).
- Chưa thử lại FSI khi khóa, cuộc gọi trễ 10 giây, onNewIntent cùng Activity, từ chối quyền/special access, giảm channel importance hoặc API 31/32 trên bản Compose.

## Bản Views trước khi chuyển Compose

Các kết quả và bằng chứng bên dưới thuộc bản Views cũ.


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
