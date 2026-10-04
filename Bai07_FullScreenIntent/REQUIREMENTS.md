### Bài 7: Full-Screen Intent cho demo cuộc gọi/báo thức

**Mục tiêu:** Gắn PendingIntent đúng vai trò và xử lý quyền/notification channel.

1. Tạo channel HIGH, Activity hiển thị trên lock screen và nút mô phỏng cuộc gọi/báo thức.
2. Xin notification permission theo thao tác người dùng; trên Android 14+ hiển thị trạng thái `canUseFullScreenIntent()` và nút mở settings.
3. Content Intent luôn mở được màn hình khi người dùng click notification; Full-Screen Intent được gắn khi quyền cho phép.
4. Thử: khóa màn hình; đang mở khóa; quyền Full-Screen bị tắt; notification permission bị từ chối; channel bị người dùng giảm importance.
5. Gửi hai sự kiện khác ID; xác nhận notification cũ không mở dữ liệu sự kiện mới do dùng chung PendingIntent.

**Nghiệm thu:** Có bảng quyền/trạng thái/kết quả thực tế; giải thích được vì sao hệ thống có thể chỉ hiện heads-up. Khi không đủ quyền, UI hướng dẫn và Content Intent hoạt động nếu notification được phép; không mở settings bất ngờ hoặc giả định luôn mở được full-screen.
