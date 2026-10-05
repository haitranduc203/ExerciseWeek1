# Biên bản kiểm chứng MiniGallery Compose

Ngày nghiệm thu: **04/10/2026**. Project: **D:\MiniGallery**. Package: `com.example.minigallery`.

## Kiểm tra tự động

Lệnh đã chạy trên mã cuối cùng:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest
```

Kết quả **BUILD SUCCESSFUL**, exit code 0.

| Kiểm tra | Kết quả |
|---|---|
| Unit: MiniGalleryViewModelTest | 11/11 PASS |
| Unit: MediaRepositoryBehaviorTest | 3/3 PASS |
| Unit: MediaStoreQueryTest | 1/1 PASS |
| Compose UI: GalleryScreenTest | 4/4 PASS |
| MediaStoreRepositoryInstrumentedTest | 3/3 PASS |
| Android Lint | 0 error, 17 warning |
| Debug APK | 32.193.546 bytes, build/cài/chạy thành công |

Compose UI test kiểm tra: thao tác cấp quyền/picker khi denied, nhập search, mở detail và batch đã hoàn tất không thể lưu lại. Repository test chạy với ContentResolver thật: sao chép bảo toàn bytes và publish, nguồn bị xóa không để lại row rác, cancellation khi copy dọn row chưa hoàn tất.

Lint warning gồm: 11 thông báo có version plugin/dependency mới hơn, 1 target API, 1 ConfigurationScreenWidthHeight, 2 ObsoleteSdkInt và 2 UnusedResources. Giữ các version đã build/test và targetSdk 36 của bài thực hành; không suppress warning. Báo cáo đầy đủ nằm trong `app/build/reports/lint-results-debug.html`.

## Nghiệm thu thực tế

Thiết bị: **Pixel_7_Pro**, Android 14 / API 34, `emulator-5554`.

1. Cấp Selected Photos Access cho hai ảnh: banner quyền một phần và grid 2/2 đúng. Nút Chọn thêm ảnh mở lại hộp thoại hệ thống.
2. Hủy Photo Picker: quay lại màn hình, không mở batch rỗng.
3. Photo Picker chọn 10 ảnh demo đã có trên thiết bị; preview có 10 ảnh và metadata. Hệ thống picker cung cấp display name dạng ID (`1000000488.png`...), ứng dụng sử dụng đúng metadata nhận được.
4. Xoay ngang khi preview: vẫn có 10 ảnh, danh sách cuộn và nút lưu/đóng nằm trong màn hình.
5. Bấm lưu ở chế độ ngang rồi xoay dọc trong lúc lưu: tiến độ tiếp tục, nút đóng/lưu bị khóa; kết quả **10 thành công, 0 thất bại**. Sau hoàn tất, nút lưu bị vô hiệu.
6. Khi quyền đọc vẫn chỉ một phần, grid cập nhật thêm 10 ảnh do app tạo, hiển thị 12/12.
7. MediaStore có 10 bản sao do `com.example.minigallery` sở hữu trong `Pictures/MiniGallery/`, IDs `1000000531`–`1000000540`, tất cả `is_pending=0`. SHA-256 của từng bản sao trùng nguồn; nguồn vẫn đọc được. Chi tiết trong [media-copy-checksums.json](media-copy-checksums.json).
8. Cấp Allow all qua hộp thoại hệ thống: banner biến mất, thư viện đầy đủ được tải.
9. Kiểm tra dark mode ở 750×1334 px, density 320 (**375×667 dp**): search, sort, grid và FAB hiển thị rõ. Grid vẫn cuộn tới ảnh cuối.
10. Khôi phục chế độ sáng, 780×1710 px, tự động xoay và rotation ban đầu. Tìm `MiniGallery_20261004_2117`: hiện đúng **10/119 ảnh**; 119 là dữ liệu dùng chung trên máy ảo, không phải số cố định của ứng dụng.
11. Logcat AndroidRuntime không có lỗi trong lượt kiểm tra cuối.

Các ảnh demo và bản sao được giữ trên máy ảo để có thể demo tiếp; không xóa dữ liệu Gallery có sẵn. Script bàn giao tạo bộ ảnh tại Pictures/MiniGalleryDemo; lượt nghiệm thu cũng nạp một bộ fixture riêng tại Pictures/MiniGalleryComposeDemo.

## Ảnh chụp

| File | Nội dung |
|---|---|
| [01-partial.png](01-partial.png) | Quyền một phần, chỉ hai ảnh được cấp quyền |
| [02-preview.png](02-preview.png) | Preview batch 10 ảnh |
| [03-landscape-preview.png](03-landscape-preview.png) | Preview ngang, nút lưu/đóng không bị che |
| [04-import-result.png](04-import-result.png) | Đang lưu 3/10, khóa thao tác |
| [05-complete.png](05-complete.png) | Hoàn tất 10 thành công, 0 thất bại |
| [06-small-dark.png](06-small-dark.png) | Quyền đầy đủ, màn hình nhỏ, dark mode |
| [07-filter-copies.png](07-filter-copies.png) | Tìm đúng 10 bản sao mới |

## Lỗi đã xử lý trong quá trình tạo bản Compose

- Vector icon dùng thuộc tính AppCompat `colorControlNormal` làm resource linking fail khi bỏ dependency Views: chuyển sang thuộc tính framework hợp lệ; Compose dùng tint theo MaterialTheme.
- Nút FAB cần semantic description rõ ràng: thêm nhãn “Thêm ảnh”, xác minh bằng Compose UI test. Các test cuối đều pass.

## Giới hạn phạm vi kiểm chứng

Đã chạy thực tế trên API 34; chưa nghiệm thu runtime trên API 29–33 hoặc API 35+. Chưa thử tablet/foldable thật, cloud provider, mất mạng, ổ đĩa đầy hoặc font scale lớn. Không phục hồi batch sau process death/force-stop; cancellation cleanup được kiểm tra nhưng kill process đột ngột có thể bỏ qua cleanup. Không xem các mục chưa kiểm tra là PASS.

## Kiểm tra trước khi push — 05/10/2026

Chạy lại `testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` trên checkout chuẩn bị đưa lên GitHub, với cấu hình Gradle đã bỏ đường dẫn JDK riêng của máy. Kết quả BUILD SUCCESSFUL; 15 unit test và 7 instrumentation test PASS trên Pixel_7_Pro API 34. Lint vẫn 0 error, 17 warning. Bản Git không chứa local.properties, cache, thư mục build hay APK; SDK được cấu hình cục bộ khi mở project.
