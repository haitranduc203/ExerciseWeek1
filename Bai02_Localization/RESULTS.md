# Kết quả kiểm chứng

## Bản Jetpack Compose — 05/10/2026

- `:app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. Lint: 0 lỗi, 32 cảnh báo (phiên bản dependency/SDK, tài nguyên mẫu chưa dùng, localeConfig chỉ áp dụng API 33+, gợi ý KTX/catalog…).
- `:app:connectedDebugAndroidTest`: **3/3 PASS**, không lỗi/skip, trên Pixel_6 AVD Android 15/API 35. Báo cáo: [evidence/compose_tests.xml](evidence/compose_tests.xml).
- Test đổi English/Việt/日本語: title/lời chào, số 0/1/2/5, fallback tiếng Anh và tên Unicode đều đúng; chọn System trả về locale list rỗng.
- Test tên dài và tên rỗng: giữ nguyên qua Activity recreate; SharedPreferences chứa đúng giá trị.
- Cài APK Compose, nhập `AndroidComposeStudent`, bấm Tiếng Việt; force-stop rồi mở lại: locale `[vi]`, tên giữ nguyên, lời chào `Xin chào, AndroidComposeStudent!`. Bằng chứng: [ảnh](evidence/compose_after_restart.png), [cây UI](evidence/compose_after_restart.xml). Launcher tiếng Anh: [evidence/compose_launcher.xml](evidence/compose_launcher.xml).
- Chưa chạy bản Compose trên API 31/32 để kiểm chứng autoStoreLocales; chưa thử xoay thiết bị thật, đổi ngôn ngữ thiết bị, theme tối hay thay đổi App language trong Settings. Test recreate không thay thế toàn bộ các case này.

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
- Đổi Việt/Nhật và giữ locale sau force-stop

Ảnh: evidence/01_launcher.png, 02_after_actions.png; log: runtime.log; layout: *.xml. Các kịch bản còn lại vẫn chưa kiểm chứng runtime.

- Nhập CodexStudent rồi đổi locale Nhật→Việt (Activity recreate): giữ tên, lời chào đúng; force-stop/mở lại vẫn giữ tên và locale. input_persistence.txt.
