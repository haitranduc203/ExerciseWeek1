### Bài 4: AIDL giữa hai app

**Mục tiêu:** Client gọi remote Service và nhận callback mà không chặn UI.

1. Tạo server/client và contract chung theo mục 3.2, dùng ví dụ `Book` để tránh đổi schema giữa bài giảng và bài tập.
2. Build, cài cả hai app cùng certificate; kiểm tra signature permission và explicit ComponentName.
3. Client thêm sách, lấy danh sách, lấy số lượng trên IO thread; UI disable thao tác khi chưa kết nối.
4. Thêm callback `onBookAdded`, chuyển cập nhật UI về main thread; đăng ký/hủy listener theo phiên kết nối.
5. Mở hai client hoặc tạo hai phiên gọi đồng thời; thử dừng process server rồi kết nối lại.

**Nghiệm thu:** Log PID khác nhau; dữ liệu và callback đúng; không chặn UI, không đăng ký listener trùng; bind thất bại/server chết không crash. Giải thích `in/out/inout`, đồng bộ, `oneway` và vì sao RAM không phải nơi lưu bền vững.

**Mở rộng:** Thêm `removeBook(id)` vào contract và cả hai phía. Không chỉ thêm method ở server rồi giữ client theo interface cũ.
