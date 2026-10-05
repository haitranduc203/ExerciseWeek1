# Kết quả kiểm chứng
## Bản Jetpack Compose — 05/10/2026

- `:app:assembleDebug :app:lintDebug`: **BUILD SUCCESSFUL**. Lint: 0 lỗi, 33 cảnh báo; còn cảnh báo phiên bản dependency/SDK, tài nguyên mẫu chưa dùng và gợi ý Kotlin/catalog.
- Pixel_6 AVD Android 15/API 35: launcher hiển thị phần trăm pin và INTERNET/VALIDATED.
- Gửi nội bộ một lần hiển thị `Custom event #1`; Home rồi mở lại, gửi một lần tiếp hiển thị `Custom event #2`. Không tăng trùng; log có đăng ký/hủy theo vòng đời.
- Bằng chứng: [UI](evidence/compose_broadcast.xml), [ảnh](evidence/compose_broadcast.png), [log](evidence/compose_runtime.log).
- Chưa thử lại thay đổi máy bay/mạng, xoay màn hình hoặc API 31/32 trên bản Compose.

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
- Custom broadcast mỗi lần đúng một callback, Home/quay lại không đăng ký trùng

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.

Lần kiểm tra ban đầu dùng startActivity tạo instance mới nên counter trở về 1; kiểm tra lại bằng Intent launcher quay lại task cũ đạt counter #2, không event trùng. runtime_error.txt lưu lần thử cũ, không phải lỗi của bản nghiệm thu.

## Đã đo trên Pixel 7 Pro Android 14 (API34)
- Cài APK và mở launcher không crash (Android 14/API34)
- Custom broadcast mỗi lần đúng một callback, Home/quay lại không đăng ký trùng

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.
