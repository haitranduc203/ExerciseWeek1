# Bài 1 — Hướng dẫn viết lại Activity, Task và kiểm thử

## 1. Mục tiêu và file cần tạo

Project: **Bai01_ActivityTasks**, applicationId **vn.training.bai01**. Bạn cần quan sát được Activity nào được tạo mới, Activity nào được tái sử dụng, task nào chứa Activity và extras có được cập nhật không.

| File | Vai trò |
|---|---|
| app/src/main/java/vn/training/bai01/DemoActivity.kt | Khung giao diện chung |
| [MainActivity.kt](app/src/main/java/vn/training/bai01/MainActivity.kt) | StackActivity và các lớp A/B/C/D/E/Login/Detail |
| [AndroidManifest.xml](app/src/main/AndroidManifest.xml) | Launcher, launchMode, parentActivityName và quyền notification |

## 2. Chuẩn bị project để viết lại

1. Trong Android Studio, tạo **Empty Views Activity**, chọn Kotlin. Mỗi bài là một project độc lập.
2. Để tái tạo đúng bản hiện tại, dùng cấu hình trong [build.gradle.kts](build.gradle.kts), [settings.gradle.kts](settings.gradle.kts), [app/build.gradle.kts](app/build.gradle.kts), [gradle.properties](gradle.properties) và bộ Gradle Wrapper của bài này. Có thể sao chép các file cấu hình sang thư mục thực hành mới rồi tự viết lại thư mục app/src/main.
3. Giữ compileSdk/targetSdk **36**, minSdk **31**, AGP **9.1.1**, Gradle **9.3.1**, Java source/target **17**. Chọn Gradle JDK tương thích, có thể dùng JBR 21 của Android Studio. Bản AGP 9 này hỗ trợ Kotlin trực tiếp; không thêm plugin kotlin-android vào cấu hình hiện tại.
4. Đặt namespace và applicationId đúng như phần mục tiêu. Nếu cài bản tự viết song song, đổi applicationId; khi đó sửa package trong các lệnh ADB tương ứng.
5. Cài SDK Platform 36 trong SDK Manager. Android Studio tạo local.properties với sdk.dir của máy; không sao chép đường dẫn SDK của máy khác.
6. Các dependency thực tế được khai báo trực tiếp ở app/build.gradle.kts: Core KTX 1.15.0, AppCompat 1.7.1, Activity KTX 1.13.0, Lifecycle Runtime KTX 2.8.7, Coroutines Android 1.8.1, RecyclerView 1.4.0 và Coil 2.7.0. Giữ cấu hình hiện có trước khi rút gọn dependency.

**Nền giao diện dùng chung:** tự viết [DemoActivity.kt](app/src/main/java/vn/training/bai01/DemoActivity.kt): kế thừa AppCompatActivity; tạo LinearLayout dọc trong ScrollView; gọi setContentView; thêm các hàm text(value), button(label, action), message(value). Áp dụng system bar insets bằng ViewCompat để nội dung không bị che. Các bài Views hiện tại dựng giao diện bằng Kotlin, không cần activity_main.xml.

Tạo app/src/main/res/values/styles.xml:

~~~xml
<resources>
    <style name="DemoTheme" parent="Theme.AppCompat.DayNight.NoActionBar" />
</resources>
~~~

Trong application của manifest đặt android:theme="@style/DemoTheme". MainActivity là launcher với android:exported="true", action MAIN và category LAUNCHER; các Activity nội bộ dùng exported="false". Những cấu hình riêng được trình bày bên dưới.

**Cách học:** viết theo thứ tự ở phần 3, build sau mỗi nhóm chức năng. Các đoạn code là phần cốt lõi để ghép vào lớp tương ứng; mở file nguồn được dẫn để đối chiếu imports, thuộc tính và xử lý lỗi đầy đủ.

## 3. Viết code theo từng bước

### Bước 1 — Tạo lớp quan sát StackActivity

Kế thừa DemoActivity. Mỗi instance có UUID riêng; hiển thị javaClass.simpleName, taskId, instance và payload. UUID là dấu hiệu nhận biết instance trong lượt chạy; không lưu UUID để làm mất sự khác biệt khi Activity được tạo lại.

~~~kotlin
private val instance = UUID.randomUUID().toString().take(8)
private lateinit var status: TextView

private fun show(value: Intent) {
    status.text = "${javaClass.simpleName}\ntaskId=$taskId instance=$instance\n" +
        (value.getStringExtra("payload") ?: "Launcher")
}
~~~

Trong onCreate gọi super, tạo status bằng text(""), gọi show(intent), ghi log tag StackDemo. Override onDestroy để log trước khi gọi super. Override onNewIntent như sau:

~~~kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    show(intent)
    Log.i("StackDemo", "onNewIntent ${javaClass.simpleName} task=$taskId instance=$instance")
}
~~~

setIntent giúp những lần đọc thuộc tính intent sau đó thấy dữ liệu mới.

### Bước 2 — Tạo các Activity con và manifest

Các lớp con chỉ cần kế thừa StackActivity:

~~~kotlin
class MainActivity : StackActivity()
class BStandardActivity : StackActivity()
class BTopActivity : StackActivity()
class CActivity : StackActivity()
class DActivity : StackActivity()
class EActivity : StackActivity()
class LoginActivity : StackActivity()
class DetailActivity : StackActivity()
~~~

Trong manifest khai báo:

| Activity | launchMode/cấu hình |
|---|---|
| MainActivity | standard, launcher; đây là A |
| BStandardActivity | standard |
| BTopActivity | singleTop |
| CActivity | singleTask |
| DActivity | singleInstance |
| EActivity | singleInstancePerTask |
| LoginActivity | standard |
| DetailActivity | standard, parentActivityName=".MainActivity" |

Giữ taskAffinity mặc định như bản hiện tại. D có task riêng chỉ chứa D; E là root của task nhưng có thể có Activity phía trên. Kết quả NEW_TASK còn phụ thuộc task/affinity hiện có. Quy tắc này được mô tả trong [Android Tasks and back stack](https://developer.android.com/guide/components/activities/tasks-and-back-stack).

### Bước 3 — Thêm nút mở màn hình và flags

Tạo hàm open(target, flags) trong StackActivity rồi thêm nút cho từng Activity:

~~~kotlin
private fun open(target: Class<*>, flags: Int) {
    startActivity(
        Intent(this, target)
            .addFlags(flags)
            .putExtra("payload", "Từ ${javaClass.simpleName} lúc ${System.currentTimeMillis()}")
    )
}
~~~

Tạo thêm nút gọi open với CLEAR_TOP; CLEAR_TOP or SINGLE_TOP; NEW_DOCUMENT or MULTIPLE_TASK đến E; NEW_TASK or CLEAR_TASK đến Login. Trong Kotlin ghép bit bằng **or**, không dùng phép cộng.

### Bước 4 — Notification mở Detail với parent stack

Khai báo POST_NOTIFICATIONS. Trên API 33+, nút gửi notification xin quyền khi chưa được cấp; **bấm lại nút sau khi cấp quyền** vì nhánh hiện tại return sau requestPermissions.

Tạo channel detail với IMPORTANCE_DEFAULT. Dùng TaskStackBuilder tạo PendingIntent, rồi gắn vào content intent của notification:

~~~kotlin
val token = TaskStackBuilder.create(this)
    .addNextIntentWithParentStack(
        Intent(this, DetailActivity::class.java)
            .putExtra("payload", "Detail từ notification")
    )
    .getPendingIntent(
        11, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
~~~

Đối chiếu phần Notification trong MainActivity.kt để hoàn thiện Notification.Builder, small icon, notify(11, ...), autoCancel.

## 4. Build, cài đặt và lấy log

Các lệnh dưới chạy trong **PowerShell tại thư mục project của bài**, sau khi mở terminal ở Android Studio. Nếu chưa có JAVA_HOME, có thể cấu hình trong phiên terminal:

~~~powershell
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
./gradlew.bat --version
./gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "Build/lint thất bại" }

$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath) { $sdkPath = "$env:LOCALAPPDATA/Android/Sdk" }
$adbPath = Join-Path $sdkPath "platform-tools/adb.exe"
& $adbPath devices
$serial = "emulator-5554" # Thay bằng serial có trạng thái device
& $adbPath -s $serial install -r "./app/build/outputs/apk/debug/app-debug.apk"
& $adbPath -s $serial shell am start -W -n "vn.training.bai01/.MainActivity"
& $adbPath -s $serial logcat -s "StackDemo"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Bảy bài Views chưa có bộ unit/instrumentation test riêng cho lời giải; task test không có test không chứng minh tính năng đã đúng.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai01
& $adbPath -s $serial shell am start -W -n "vn.training.bai01/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

## 5. Test chức năng

Trước mỗi case, force-stop và **xóa các task của bài trong Recents** để tránh task D/E cũ ảnh hưởng. Ký hiệu stack viết từ root đến top; X có thể dùng MainActivity mở từ B.

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | A → BStandard → BStandard | Hai B có instance khác nhau, hai log onCreate; Back về B trước |
| TC02 | A → BTop → BTop | Instance giữ nguyên, có onNewIntent, payload mới |
| TC03 | A → BTop → A → BTop | BTop thứ hai có instance mới vì B cũ không ở top |
| TC04 | A → BStandard → A; bấm CLEAR_TOP → B standard | A phía trên và B cũ bị loại; B mới onCreate |
| TC05 | Lặp stack TC04; bấm CLEAR_TOP + SINGLE_TOP | B cũ nhận onNewIntent; A phía trên bị loại |
| TC06 | A → C → BStandard → C | C được tái sử dụng, các Activity phía trên C bị loại |
| TC07 | A → D → A | taskId của D khác taskId của A mở từ D |
| TC08 | A → E → BStandard | E là root; BStandard có thể cùng task với E |
| TC09 | Bấm NEW_DOCUMENT + MULTIPLE_TASK → E hai lần | So taskId, instance và Recents để xác nhận nhiều task E |
| TC10 | Từ stack có nhiều màn hình mở Login bằng NEW_TASK + CLEAR_TASK | Task được chọn chỉ còn Login; Back không về stack cũ trong task đó |
| TC11 | Ở A root, bấm Back trên API 31+ | Task vào nền theo hành vi mặc định |
| TC12 | Gửi notification, chạm mở Detail rồi Back | Detail có payload đúng; Back về A nhờ parent stack |

Quan sát đồng thời UI và log, không suy ra stack chỉ từ tên Activity. Có thể kiểm tra thêm:

~~~powershell
& $adbPath -s $serial shell dumpsys activity activities |
    Select-String "vn.training.bai01|taskId|Hist"
~~~

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- BTop luôn tạo mới: kiểm tra launchMode trong manifest và xem B có thật sự ở top không.
- UI giữ payload cũ: xử lý extras ở cả onCreate và onNewIntent, gọi setIntent.
- Detail Back về Home: kiểm tra parentActivityName và addNextIntentWithParentStack.
- Không có notification: kiểm tra quyền và channel, rồi bấm gửi lại.
- NEW_TASK có cùng taskId: kiểm tra affinity và task đang có; đây không tự động là lỗi.

[RESULTS.md](RESULTS.md) hiện ghi đã đo TC01/TC02 trên API 34. Các case khác trong bảng là hướng dẫn để bạn chạy tiếp, chưa được tài liệu này xác nhận PASS. Bản hướng dẫn được đối chiếu mã nguồn, không chạy lại ứng dụng khi viết tài liệu.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "StackDemo" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 1 sau khi cập nhật cấu hình script.
