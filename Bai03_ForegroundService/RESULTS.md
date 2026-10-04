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
- Play/Pause/Stop hoạt động, coroutine worker thread, Service dừng

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.
