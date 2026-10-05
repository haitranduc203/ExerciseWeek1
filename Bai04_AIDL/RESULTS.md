# Kết quả kiểm chứng
## Bản Jetpack Compose — 05/10/2026

- `:client:assembleDebug :server:assembleDebug :client:lintDebug :server:lintDebug`: **BUILD SUCCESSFUL**. Lint: client: 0 lỗi/12 cảnh báo; server: 0 lỗi/24 cảnh báo; còn cảnh báo phiên bản dependency/SDK, tài nguyên mẫu chưa dùng và gợi ý Kotlin/catalog.
- Pixel_6 AVD Android 15/API 35: cài/mở cả server và client cùng debug certificate.
- Client hiển thị `connected=2`; thêm sách đồng thời nhận callback session 1 và 2; lấy danh sách hiển thị `count=2` và hai sách.
- Bằng chứng: [server](evidence/compose_server.xml), [client](evidence/compose_client.xml), [ảnh client](evidence/compose_client.png), [log](evidence/compose_runtime.log).
- Chưa thử lại removeBook, server death/reconnect, signature khác, xoay với input hoặc API 31/32 trên bản Compose.

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
- Hai phiên bind, hai callback, count=2, xử lý server chết và reconnect

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.
