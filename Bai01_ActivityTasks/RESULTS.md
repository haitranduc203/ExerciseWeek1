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
- B standard tạo hai instance; B singleTop gọi onNewIntent khi ở đỉnh

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.

## Bảng dự đoán/kết quả Activity
| Kịch bản | Dự đoán | Thực đo API34 |
|---|---|---|
| A → B standard → B standard | Hai instance B | Đạt, hai onCreate khác instance trong runtime.log |
| A → B singleTop → B singleTop | B tái sử dụng | Đạt, onNewIntent BTopActivity |
| B singleTop không ở đỉnh | Tạo B mới | Có nút chạy; chưa kiểm chứng runtime |
| CLEAR_TOP B standard | Finish X/B cũ, tạo B mới | Có nút chạy; chưa kiểm chứng runtime |
| CLEAR_TOP + SINGLE_TOP | Finish X, giữ B | Có nút chạy; chưa kiểm chứng runtime |
| D mở A/X | A/X task khác D | Có nút chạy; chưa kiểm chứng runtime |
| E mở A/X, NEW_DOCUMENT | E là root, có thể nhiều task | Có nút chạy; chưa kiểm chứng runtime |
| NEW_TASK + CLEAR_TASK Login | Task chọn chỉ còn Login | Có nút chạy; chưa kiểm chứng runtime |
| Back root launcher API31+ | Task vào background | Chưa kiểm chứng runtime |
| Notification Detail + Back | Stack parent nhờ TaskStackBuilder | Có code và nút; chưa kiểm chứng runtime |
