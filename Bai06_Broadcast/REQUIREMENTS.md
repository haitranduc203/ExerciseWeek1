### Bài 6: Broadcast Receiver

**Mục tiêu:** Đăng ký đúng phạm vi và không để receiver sống ngoài vòng đời cần thiết.

1. Nhận pin bằng `ACTION_BATTERY_CHANGED`, chế độ máy bay bằng `ACTION_AIRPLANE_MODE_CHANGED` qua receiver động.
2. Đăng ký trong `onStart()`, hủy trong `onStop()`, chọn exported flag đúng nguồn gửi.
3. Gửi custom broadcast giới hạn package; receiver nội bộ dùng `RECEIVER_NOT_EXPORTED`.
4. Theo dõi kết nối bằng `ConnectivityManager.NetworkCallback`; phân biệt có mạng với mạng được xác thực internet (`NET_CAPABILITY_VALIDATED`).
5. Nếu làm boot receiver, khai báo `RECEIVE_BOOT_COMPLETED` và lên lịch WorkManager, không tự start FGS bị cấm.

**Nghiệm thu:** UI cập nhật đúng; ra/vào màn hình nhiều lần không nhận event trùng và không lỗi unregister. `onReceive()` không làm network/database nặng; giải thích `goAsync()` vẫn có giới hạn thời gian.
