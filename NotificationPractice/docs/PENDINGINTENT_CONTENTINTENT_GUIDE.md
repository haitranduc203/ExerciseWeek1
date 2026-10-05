# Bài tập Compose: PendingIntent và Content Intent

Project mẫu: `C:\Users\cuong.bui1\IdeaProjects\NotificationPractice`. Nội dung notification theo mục 3.4.1–3.4.2 của `android_basic_components.md`; giao diện hiện dùng **Kotlin + Jetpack Compose Material 3**.

## 1. Chạy và quan sát trước

Mở folder bằng Android Studio, sync và chạy module **app**. Cấu hình: AGP 9.1.1, Gradle 9.3.1, JDK 21, Kotlin/Compose compiler 2.2.10, Compose BOM 2026.03.01, minSdk 29, targetSdk 36, compileSdk 36.1. BOM được giữ ở phiên bản tương thích compileSdk của project, không nâng SDK chỉ để đổi UI.

1. Trên Android 13+, bấm **Cấp quyền thông báo**, chọn cho phép.
2. Giữ công tắc thử lỗi tắt; tạo thông báo A rồi B.
3. Nhấn Home, mở bảng thông báo. Chạm A: Detail nhận `A`; chạm B: Detail nhận `B`.
4. Tạo lại A/B, mở rộng thông báo A và bấm **Đánh dấu đã đọc**. A bị hủy, B vẫn còn; danh sách hiển thị A đã đọc và B chưa đọc.
5. Bật công tắc thử lỗi, tạo A rồi B; chạm thông báo A: Detail nhận `B`.
6. Tắt thử lỗi và tạo lại A/B: chạm A lại nhận `A`.
7. Dùng **Đặt lại bài tập** để xóa hai trạng thái đọc và hai notification.

```powershell
Set-Location 'C:\Users\cuong.bui1\IdeaProjects\NotificationPractice'
.\scripts\Check-Project.ps1
android run --device emulator-5554 --apks app/build/outputs/apk/debug/app-debug.apk
```

Thay serial bằng thiết bị của bạn. Script dùng `.tmp` và `jdk.net.unixdomain.tmpdir` để xử lý lỗi socket Java trên máy Windows này, khôi phục biến môi trường sau khi chạy. Đường dẫn JDK trong gradle.properties và SDK trong local.properties cần sửa nếu dùng máy khác.

## 2. Hai luồng cần giải thích

```text
Compose Button → Activity → Intent mở Detail
  → PendingIntent.getActivity()
  → setContentIntent(token)
  → người dùng chạm notification
  → hệ thống mở Detail → Compose hiển thị message_id

Action Đánh dấu đã đọc trên notification
  → PendingIntent.getBroadcast()
  → MarkReadReceiver → lưu cờ đọc và hủy notification
  → listener → MainUiState mới → Compose recompose
```

`Intent` mô tả hành động; `PendingIntent` là token để hệ thống thực hiện hành động thay mặt app. **Content Intent** là tên vai trò khi gắn một PendingIntent bằng `setContentIntent()`, không phải lớp riêng. Compose tạo giao diện trong app; giao diện notification do hệ thống Android hiển thị bằng NotificationCompat.

Chạm notification chỉ mở Detail; nút action mới đánh dấu đã đọc. Bạn dễ đối chiếu hai loại token hơn khi hai thao tác có kết quả riêng.

## 3. Tự code lại từng bước

Tạo project **Empty Activity (Jetpack Compose)** mới, ví dụ `NotificationPracticeRedo`. Nếu dùng cùng package để đối chiếu, đặt applicationId khác (`com.example.notificationpractice.redo`) để cài song song với bản mẫu. Khi đổi package, sửa namespace, imports và action constants tương ứng.

Làm từng bước, chạy thử và dự đoán kết quả trước khi mở code mẫu. Các snippets bên dưới thể hiện trọng tâm; code trong project có đầy đủ imports và xử lý UI.

### Bước 1 — Cấu hình Compose

Với cấu hình AGP 9.1.1 của bản mẫu, Kotlin tích hợp đang dùng 2.2.10. Compose compiler plugin phải khớp Kotlin. Trong version catalog:

```toml
[versions]
kotlin = "2.2.10"
composeBom = "2026.03.01"

[plugins]
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

Root build.gradle.kts thêm alias với `apply false`. Module app:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

Đối chiếu catalog đầy đủ trong project để khai báo các alias. Với AGP 9, không thêm plugin `org.jetbrains.kotlin.android`; vẫn cần plugin `org.jetbrains.kotlin.plugin.compose`. Khi dùng template có phiên bản khác, dùng compiler cùng Kotlin của template và BOM phù hợp compileSdk của nó.

Giao diện không dùng `res/layout` hoặc ViewBinding. `AndroidManifest.xml`, `strings.xml`, icon vector và theme cửa sổ Android vẫn là XML resources bình thường.

**Kiểm tra:** Sync thành công, gọi `setContent { Text("Hello Compose") }` trong ComponentActivity và chạy được.

### Bước 2 — Tạo dữ liệu A/B

Tạo `DemoMessage.kt` với `id`, `notificationId`, `titleRes`, `bodyRes`. Dữ liệu mẫu A dùng notification ID 101; B dùng 102. Hàm `DemoMessages.find(id)` trả về tin nhắn hoặc null. Đặt chữ trong strings.xml và dùng `stringResource(...)` trong composable.

**Kiểm tra:** Hiển thị được title/body hai tin nhắn bằng `Text`.

### Bước 3 — Dựng hai màn hình Compose

Tạo `PracticeScreens.kt`:

- `PracticeTheme`: MaterialTheme, light/dark color scheme và Surface.
- `MessageListScreen`: trạng thái quyền, hai Button tạo thông báo, Switch thử lỗi, hai Card tin nhắn, nút đặt lại.
- `MessageDetailScreen`: ID nhận được, title/body, nút về danh sách.
- `ScreenColumn`: Column có `safeDrawingPadding()`, `verticalScroll(rememberScrollState())`, padding 24.dp; cả hai màn hình có thể cuộn.

Dữ liệu trạng thái danh sách:

```kotlin
data class MainUiState(
    val notificationsEnabled: Boolean = false,
    val canRequestPermission: Boolean = true,
    val isARead: Boolean = false,
    val isBRead: Boolean = false,
)
```

Composable nhận state và callback thay vì tự gọi NotificationManager:

```kotlin
@Composable
fun MessageListScreen(
    state: MainUiState,
    brokenMode: Boolean,
    onBrokenModeChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPostMessage: (DemoMessage) -> Unit,
    onReset: () -> Unit,
) { /* Column, Text, Card, Button, Switch */ }
```

Ví dụ nút:

```kotlin
Button(onClick = { onPostMessage(DemoMessages.a) }) {
    Text(stringResource(R.string.notify_a))
}
```

Không gửi notification trong thân composable: recomposition có thể chạy lại nhiều lần. Chỉ thực hiện side effect khi callback của thao tác người dùng được gọi. Dùng `@Preview` và callback rỗng để xem giao diện mà không gửi notification.

**Kiểm tra:** Preview hiển thị được; màn hình nhỏ cuộn tới B và nút đặt lại; system bars không che nội dung.

### Bước 4 — Khai báo component, channel và quyền

Manifest khai báo POST_NOTIFICATIONS, MainActivity launcher exported=true, Detail singleTop/exported=false và MarkReadReceiver exported=false:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- Bên trong application: -->
<activity android:name=".MessageDetailActivity"
    android:exported="false" android:launchMode="singleTop"
    android:parentActivityName=".MainActivity" />
<receiver android:name=".MarkReadReceiver" android:exported="false" />
```

Tạo channel trước notify, với `IMPORTANCE_DEFAULT`. minSdk 29 nên luôn có NotificationChannel (API 26+).

MainActivity dùng Activity Result API `RequestPermission`; khi bấm nút cấp quyền, chỉ launch POST_NOTIFICATIONS trên API 33+. Callback cập nhật state quyền. Trước gửi, kiểm tra quyền, `areNotificationsEnabled()` và channel có bị đặt `IMPORTANCE_NONE` không.

Nút Cài đặt mở `Settings.ACTION_APP_NOTIFICATION_SETTINGS` với `Settings.EXTRA_APP_PACKAGE`. Kiểm tra quyền lại trong `onResume()` khi trở về.

**Kiểm tra:** Từ chối quyền rồi bấm tạo A: không crash, không notification. Cấp quyền rồi bấm tạo A lại: hiện notification.

### Bước 5 — Content PendingIntent mở Detail

Tạo helper `MessageNotifications.kt` và hàm tạo token:

```kotlin
val detailIntent = Intent(context, MessageDetailActivity::class.java).apply {
    action = ACTION_OPEN
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    putExtra(EXTRA_MESSAGE_ID, message.id)
}
val contentToken = PendingIntent.getActivity(
    context, message.notificationId, detailIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
```

Gắn vào notification:

```kotlin
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_message)
    .setContentTitle(context.getString(message.titleRes))
    .setContentText(context.getString(message.bodyRes))
    .setContentIntent(contentToken)
    .setAutoCancel(true)
    .build()
// Chỉ thực hiện sau kiểm tra quyền/channel.
NotificationManagerCompat.from(context).notify(message.notificationId, notification)
```

- `getActivity()` tạo token mở Activity.
- Request code riêng phân biệt A/B.
- `UPDATE_CURRENT` cập nhật extras của token đã có.
- `IMMUTABLE` ngăn bên sử dụng token sửa Intent bằng fill-in, nhưng app tạo token vẫn có thể cập nhật extras bằng UPDATE_CURRENT.
- Notification ID dùng để đăng/cập nhật/hủy notification; requestCode tham gia nhận diện token. Dùng cùng số cho tiện không làm hai khái niệm thành một.

**Kiểm tra:** Tạo A/B, nhấn Home rồi chạm từng notification: ID nhận được đúng.

### Bước 6 — Activity nhận Intent, Compose hiển thị state

Trong Detail:

```kotlin
class MessageDetailActivity : ComponentActivity() {
    private var messageId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent, "onCreate")
        setContent {
            PracticeTheme {
                MessageDetailScreen(messageId = messageId, onBackToList = { /* mở Main */ })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent, "onNewIntent")
    }

    private fun readIntent(intent: Intent, callback: String) {
        messageId = intent.getStringExtra(MessageNotifications.EXTRA_MESSAGE_ID)
        Log.d(MessageNotifications.TAG, "$callback OPEN message_id=$messageId")
    }
}
```

Khi messageId thay đổi, Compose recompose để hiển thị dữ liệu mới. ID thiếu/không hợp lệ phải có fallback thay vì crash. Nút Về danh sách mở Main với CLEAR_TOP | SINGLE_TOP rồi finish Detail; hoạt động cả khi Main chưa có trong task.

Bài dùng getActivity nên không tự dựng parent back stack. `parentActivityName` không tự tạo stack; TaskStackBuilder là phần mở rộng sau khi hiểu hai nội dung chính.

**Kiểm tra:** Khi Detail A đang ở trên cùng, chạm B; log onNewIntent và UI đổi sang B.

### Bước 7 — Action đã đọc và state trong Compose

Tạo explicit Intent tới MarkReadReceiver, action MARK_READ, extra message_id. Tạo bằng `PendingIntent.getBroadcast()` với requestCode riêng theo tin nhắn. Gắn bằng `.addAction(...)` trước build().

Receiver kiểm tra action/ID, lưu Boolean bằng `ReadStateStore`, hủy đúng notification ID và ghi log MARK_READ. Không mở Activity và không làm tác vụ dài trong onReceive.

MainActivity giữ:

```kotlin
private var uiState by mutableStateOf(MainUiState())
private val readStateListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
    refreshStatus()
}
```

Trong refreshStatus tạo MainUiState mới từ quyền và hai cờ đọc. Đăng ký listener ở onStart, hủy ở onStop; refresh thêm ở onResume. Bảng notification có thể không làm Activity pause/resume, nên chỉ đọc cờ ở onResume chưa đủ. Listener cập nhật mutableStateOf; Compose tự render lại.

Trong setContent của Main:

```kotlin
var brokenMode by rememberSaveable { mutableStateOf(false) }
```

Truyền brokenMode vào screen và callback đăng thông báo. rememberSaveable giữ công tắc qua Activity recreation. Hai cờ đã đọc lưu SharedPreferences để tồn tại sau mở lại app; state Compose phản ánh chúng chứ không thay thế lưu trữ.

**Kiểm tra:** Đánh dấu A từ notification khi Main đang hiển thị phía sau; UI cập nhật ngay, B chưa đọc. Tái tạo Activity vẫn giữ công tắc thử lỗi.

### Bước 8 — Thử lỗi dùng chung token

Trong hàm contentPendingIntent:

```kotlin
val requestCode = if (brokenMode) 0 else message.notificationId
```

Ở chế độ thử lỗi, A/B có cùng loại token, requestCode, component, action và data(null); chỉ extras khác nhau. Extras không phân biệt token, nên B cập nhật token A thành dữ liệu B. Tạo A rồi B, chạm A nhận B; tắt thử lỗi để dùng requestCode riêng và sửa lỗi.

Công tắc chỉ tác động Content PendingIntent; action đã đọc luôn dùng token riêng. Khi đổi chế độ app hủy notification cũ để bắt đầu lượt mới. Nút Đặt lại xóa trạng thái và notification, không hủy token; UPDATE_CURRENT cập nhật token ở lượt tạo tiếp.

Thử cách sửa khác: cùng requestCode nhưng Intent có `data` riêng, ví dụ `training://message/A` và `training://message/B`.

## 4. Thứ tự đọc code

Các file Kotlin ở `app/src/main/java/com/example/notificationpractice/`:

| File | Vai trò |
|---|---|
| DemoMessage.kt | Dữ liệu A/B và lookup |
| PracticeScreens.kt | Composables, MainUiState, theme, Preview |
| MessageNotifications.kt | Channel, quyền, hai loại token, build notification |
| MessageDetailActivity.kt | Intent → Compose state |
| MarkReadReceiver.kt | Action đã đọc, hủy notification |
| ReadStateStore.kt | Hai cờ đọc và listener |
| MainActivity.kt | Callback UI, quyền, rememberSaveable, cập nhật state |

## 5. Tự nghiệm thu

| Kịch bản | Dự đoán | Thực tế của bạn |
|---|---|---|
| Từ chối quyền rồi tạo A | Không crash, không notification | |
| Cách đúng, tạo A/B, chạm A | Detail A | |
| Cách đúng, tạo A/B, chạm B | Detail B | |
| Chạm notification khi app ở Home | Mở Detail đúng ID | |
| Action đã đọc A | Chỉ A đã đọc và bị hủy, B vẫn còn | |
| Thử lỗi, tạo A rồi B, chạm A | Detail B | |
| Tắt thử lỗi và tạo lại | Chạm A nhận A | |
| Tái tạo Activity | Công tắc giữ lựa chọn | |
| Cỡ chữ lớn/màn hình nhỏ | Cuộn được tới mọi nút | |
| Đặt lại bài tập | Hai tin nhắn chưa đọc, notification biến mất | |

Logcat lọc tag NotificationPractice. Chạy Compose instrumentation tests:

```powershell
.\scripts\Check-Project.ps1 -ConnectedTests -Serial emulator-5554
adb -s emulator-5554 logcat -d -s NotificationPractice:D '*:S'
```

Tests dùng `createEmptyComposeRule()` với ActivityScenario, tìm UI bằng testTag/semantics. Chúng kiểm tra token thật, dữ liệu qua Intent, receiver cập nhật Compose state và rememberSaveable qua Activity recreation. Xem docs/VERIFICATION.md cho kết quả đã chạy của bản mẫu.

## 6. Câu hỏi ôn tập

1. Content Intent có phải lớp riêng không?
2. Tại sao notification ID khác nhau vẫn có thể dùng chung token?
3. Vì sao extras không phân biệt PendingIntent?
4. Vì sao IMMUTABLE vẫn cho app tạo token cập nhật extras?
5. Intent flags khác PendingIntent flags thế nào?
6. Vì sao không gửi notification trực tiếp trong thân composable?
7. mutableStateOf, rememberSaveable và SharedPreferences có vai trò khác nhau thế nào?
8. Tại sao cần listener dù đã có onResume?

## Tài liệu chính thức

- [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent)
- [Create a notification](https://developer.android.com/develop/ui/compose/notifications/create-notification)
- [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [Compose testing](https://developer.android.com/develop/ui/compose/testing)