# Runtime verification
Python 3 scripts dùng Android CLI layout, screen capture và adb; cập nhật đường dẫn CLI/ADB/ROOT nếu chuyển máy.
Khởi động emulator: android emulator start Pixel_7_Pro --headless.
python runtime_checks.py 1 2 3 4 5 6 7
python extra_checks.py gallery fsi
Không chạy hai script cùng lúc trên cùng thiết bị. Script thay đổi quyền của app bài học và cài APK. Kiểm thử FSI khôi phục quyền sau khi thành công; nếu dừng giữa chừng dùng pm grant POST_NOTIFICATIONS và appops set USE_FULL_SCREEN_INTENT default.
Ảnh và layout là bằng chứng phụ; RESULTS.md chỉ ghi hành vi đã được assert/đối chiếu log.
