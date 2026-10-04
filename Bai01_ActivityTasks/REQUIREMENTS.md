### Bài 1: Activity, Task, LaunchMode và Flags

**Mục tiêu:** Dự đoán được back stack và phân biệt tạo mới với tái sử dụng instance.

1. Tạo A/B/C/D/E. A là launcher; các màn hình có nút mở Activity khác và hiển thị `taskId`, instance ID.
2. Dùng A làm root với `standard`; thử B là `standard`, sau đó `singleTop`. Dùng C `singleTask`, D `singleInstance`, E `singleInstancePerTask`.
3. Log `onCreate()`, `onNewIntent()`, `onDestroy()`; xử lý extras ở cả `onCreate()` và `onNewIntent()`.
4. Chạy từng kịch bản từ cùng trạng thái ban đầu. Không giữ task nền từ kịch bản trước khi đo kịch bản sau.

| Kịch bản | Kết quả mong đợi |
|---|---|
| B standard, `[A, B]` → mở B | `[A, B₁, B₂]`, B₂ có instance ID mới |
| B singleTop, `[A, B]` → mở B | Stack không đổi, B nhận `onNewIntent()` |
| B singleTop, `[A, B, X]` → mở B | Tạo B₂ vì B không ở top |
| `[A, B, X]` → B bằng `CLEAR_TOP`, B standard | X và B cũ finish, B mới được tạo |
| Cùng stack trên → `CLEAR_TOP | SINGLE_TOP` | X finish, B cũ nhận `onNewIntent()` |
| Mở D singleInstance rồi D mở X | X nằm ở task khác D; kiểm tra task ID |
| E singleInstancePerTask mở X | E là root, X có thể ở cùng task; thử thêm `NEW_DOCUMENT` để tạo task khác |
| `NEW_TASK | CLEAR_TASK` mở Login | Task được chọn chỉ còn Login |
| Back tại A root launcher trên Android 12+ | Task vào background theo hành vi mặc định |

**Nghiệm thu:** Có bảng dự đoán/kết quả thực đo; giải thích được ảnh hưởng affinity và vì sao `NEW_TASK` không luôn tạo task mới. Thử notification mở Detail rồi Back với `TaskStackBuilder`.
