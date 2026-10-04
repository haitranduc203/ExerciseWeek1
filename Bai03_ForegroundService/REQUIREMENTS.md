### Bài 3: Foreground Service và notification actions

**Mục tiêu:** Hiểu foreground không đồng nghĩa thread riêng và điều khiển Service qua notification.

1. Tạo Music Player demo, start từ nút trong Activity đang hiển thị; dùng audio sample hoặc bộ đếm mô phỏng và ghi rõ lựa chọn.
2. Khai báo Service `mediaPlayback`, permission và channel; gọi `startForeground()` trước khi thực hiện công việc.
3. Implement Play/Pause/Stop bằng explicit PendingIntent action; Content Intent mở màn hình player.
4. Hủy công việc, giải phóng tài nguyên, gọi `stopForeground()` và `stopSelf()` khi Stop.
5. Kiểm tra app về Home, Activity recreate, click notification và từ chối `POST_NOTIFICATIONS`.

**Nghiệm thu:** Không chạy công việc nặng trên main thread; action dùng đúng token; Stop dừng Service và notification. Giải thích được tại sao từ chối quyền notification không tự cấm start FGS.

**Mở rộng về phiên bản:** Tạo `dataSync` demo target 35+ và xử lý `onTimeout(startId, fgsType)`. Trên emulator Android 15 riêng cho bài học, có thể rút ngắn timeout bằng công cụ sau, rồi đưa app về background:

```text
adb shell device_config put activity_manager data_sync_fgs_timeout_duration 10000
```

Sau bài, khôi phục mặc định:

```text
adb shell device_config delete activity_manager data_sync_fgs_timeout_duration
```

Ghi rõ đây là cấu hình thiết bị thử nghiệm; không cần chờ 6 giờ, không áp dụng timeout này cho `mediaPlayback`. [Hướng dẫn kiểm tra timeout](https://developer.android.com/develop/background-work/services/fgs/timeout#testing).
