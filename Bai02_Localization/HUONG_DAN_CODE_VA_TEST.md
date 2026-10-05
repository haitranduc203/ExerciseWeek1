# Bài 2 — Hướng dẫn viết lại ứng dụng đa ngôn ngữ và kiểm thử

## 1. Mục tiêu và file cần tạo

Project: **Bai02_Localization**, applicationId **vn.training.bai02**. App có English, Tiếng Việt, 日本語 và Theo hệ thống; tên người dùng giữ nguyên khi đổi locale, xoay hoặc mở lại.

| File | Vai trò |
|---|---|
| [MainActivity.kt](app/src/main/java/vn/training/bai02/MainActivity.kt) | Nhập tên, lời chào, chọn locale và lưu dữ liệu |
| [LocalizationScreen.kt](app/src/main/java/vn/training/bai02/LocalizationScreen.kt) | Giao diện Compose Material 3, theme và preview ba ngôn ngữ |
| [values/strings.xml](app/src/main/res/values/strings.xml) | Bộ chuỗi mặc định tiếng Anh |
| [values-vi/strings.xml](app/src/main/res/values-vi/strings.xml) | Tiếng Việt |
| [values-ja/strings.xml](app/src/main/res/values-ja/strings.xml) | Tiếng Nhật |
| [locales_config.xml](app/src/main/res/xml/locales_config.xml) | Danh sách locale en, vi, ja |
| [LocalizationTest.kt](app/src/androidTest/java/vn/training/bai02/LocalizationTest.kt) | Test giao diện Compose, đổi locale và giữ dữ liệu |
| [AndroidManifest.xml](app/src/main/AndroidManifest.xml) | localeConfig và autoStoreLocales |

## 2. Chuẩn bị project để viết lại

1. Trong Android Studio, tạo **Empty Activity** dùng Jetpack Compose, chọn Kotlin. Mỗi bài là một project độc lập.
2. Để tái tạo đúng bản hiện tại, dùng cấu hình trong [build.gradle.kts](build.gradle.kts), [settings.gradle.kts](settings.gradle.kts), [app/build.gradle.kts](app/build.gradle.kts), [gradle.properties](gradle.properties) và bộ Gradle Wrapper của bài này. Có thể sao chép các file cấu hình sang thư mục thực hành mới rồi tự viết lại thư mục app/src/main.
3. Giữ compileSdk/targetSdk **36**, minSdk **31**, AGP **9.1.1**, Gradle **9.3.1**, Java source/target **17**. Chọn Gradle JDK tương thích, có thể dùng JBR 21 của Android Studio. Bản AGP 9 này hỗ trợ Kotlin trực tiếp; không thêm plugin kotlin-android vào cấu hình hiện tại.
4. Đặt namespace và applicationId đúng như phần mục tiêu. Nếu cài bản tự viết song song, đổi applicationId; khi đó sửa package trong các lệnh ADB tương ứng.
5. Cài SDK Platform 36 trong SDK Manager. Android Studio tạo local.properties với sdk.dir của máy; không sao chép đường dẫn SDK của máy khác.
6. Bật `buildFeatures { compose = true }`; dùng plugin `org.jetbrains.kotlin.plugin.compose` 2.3.20 và Compose BOM 2026.03.01 như cấu hình hiện tại. Dependency gồm Core KTX, AppCompat, Activity Compose, Compose UI/Material 3/preview/tooling và Compose UI test. Không cần ViewBinding, RecyclerView hay Coil cho bài này.

**Giao diện Compose:** `MainActivity` kế thừa `AppCompatActivity` để AppCompat quản lý locale trên API 31/32, gọi `enableEdgeToEdge()` và `setContent { LocalizationTheme { ... } }`. Viết `LocalizationScreen` nhận `name`, `onNameChange`, `onLanguageChange`. Dùng `Scaffold`, `Column.verticalScroll`, `OutlinedTextField`, `Text` và `Button`; áp dụng padding của Scaffold và `imePadding()` để tránh system bars/bàn phím. Theme Material 3 chọn sáng/tối theo hệ thống. Xem preview en/vi/ja trong Android Studio.

Tạo app/src/main/res/values/styles.xml:

~~~xml
<resources>
    <style name="DemoTheme" parent="Theme.AppCompat.DayNight.NoActionBar" />
</resources>
~~~

Trong application của manifest đặt android:theme="@style/DemoTheme". MainActivity là launcher với android:exported="true", action MAIN và category LAUNCHER; các Activity nội bộ dùng exported="false". Những cấu hình riêng được trình bày bên dưới.

**Cách học:** viết theo thứ tự ở phần 3, build sau mỗi nhóm chức năng. Các đoạn code là phần cốt lõi để ghép vào lớp tương ứng; mở file nguồn được dẫn để đối chiếu imports, thuộc tính và xử lý lỗi đầy đủ.

## 3. Viết code theo từng bước

### Bước 1 — Tạo tài nguyên ba ngôn ngữ

Mỗi bộ có title, name_hint, greeting, zero, system và plurals messages. Giữ cùng key và placeholder; ví dụ bộ mặc định:

~~~xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="title">Language settings</string>
    <string name="name_hint">Your name</string>
    <string name="greeting">Hello, %1$s!</string>
    <string name="zero">No messages</string>
    <string name="system">System default</string>
    <plurals name="messages">
        <item quantity="one">%d message</item>
        <item quantity="other">%d messages</item>
    </plurals>
    <string name="fallback" tools:ignore="MissingTranslation">English fallback example.</string>
</resources>
~~~

Ở values-vi dùng greeting “Xin chào, %1$s!” và zero “Không có tin nhắn”. Ở values-ja dùng “こんにちは、%1$sさん！” và “メッセージはありません”. Tự dịch những key còn lại theo file nguồn. **Chủ ý không khai báo fallback trong hai bộ dịch** để quan sát Android dùng giá trị mặc định.

Không cần `ids.xml`. Compose giữ trạng thái nhập bằng `rememberSaveable`; ô nhập có `testTag("name_input")` để test truy cập ổn định.

### Bước 2 — Cấu hình locale

Tạo res/xml/locales_config.xml:

~~~xml
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="vi" />
    <locale android:name="ja" />
</locale-config>
~~~

Trong application đặt android:localeConfig="@xml/locales_config" và thêm:

~~~xml
<service
    android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
    android:enabled="false"
    android:exported="false">
    <meta-data android:name="autoStoreLocales" android:value="true" />
</service>
~~~

AppCompat lưu locale trên Android 12 trở xuống; Android 13+ tích hợp cài đặt ngôn ngữ ứng dụng của hệ thống. Đối chiếu [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages).

### Bước 3 — Giữ tên và cập nhật lời chào

Trong MainActivity tạo prefs tên input. `rememberSaveable` giữ tên qua Activity recreate; giá trị khởi tạo đọc SharedPreferences, mặc định “Android”.

~~~kotlin
val preferences = getSharedPreferences("input", MODE_PRIVATE)
// Bên trong setContent:
var name by rememberSaveable {
    mutableStateOf(preferences.getString("name", "Android").orEmpty())
}
LocalizationScreen(
    name = name,
    onNameChange = { value ->
        name = value
        preferences.edit().putString("name", value).apply()
    },
    onLanguageChange = { tag ->
        preferences.edit().putString("name", name).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
)
~~~

Trong `LocalizationScreen`, dùng `OutlinedTextField(value = name, onValueChange = onNameChange, ...)` và `Text(stringResource(R.string.greeting, name))`. Compose cập nhật lời chào khi tên thay đổi. SharedPreferences giữ tên qua force-stop; `rememberSaveable` giữ qua recreate. Không cần override `onSaveInstanceState`.

### Bước 4 — Hiển thị số lượng và nút chọn ngôn ngữ

~~~kotlin
listOf(0, 1, 2, 5).forEach { count ->
    Text(
        if (count == 0) stringResource(R.string.zero)
        else pluralStringResource(R.plurals.messages, count, count)
    )
}
~~~

Không giả định quantity zero luôn được chọn: câu không có tin nhắn dùng string riêng.

Thêm bốn nút với language tag en, vi, ja và chuỗi rỗng. Lưu tên trước khi đổi locale:

~~~kotlin
preferences.edit().putString("name", name).apply()
AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
~~~

Chuỗi rỗng nghĩa là theo hệ thống. Dùng `stringResource` cho title/hint/system/fallback để Compose đọc tài nguyên theo cấu hình hiện tại; không sửa Locale.setDefault thủ công. Giữ `strings.xml` và cấu hình Android, toàn bộ bố cục màn hình nằm trong Kotlin Compose.

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
& $adbPath -s $serial shell am start -W -n "vn.training.bai02/.MainActivity"
& $adbPath -s $serial logcat -s "ActivityTaskManager"
~~~

Nếu SDK được đặt nơi khác, lấy đường dẫn từ SDK Manager/local.properties rồi gán $sdkPath. Nếu JBR nằm nơi khác, đổi JAVA_HOME. Có thể dùng nút **Run** của Android Studio thay cho cài APK bằng ADB. Ctrl+C dừng xem log.

Build đạt khi có BUILD SUCCESSFUL và APK được tạo. Lint report nằm ở app/build/reports/lint-results-debug.html; đọc cả cảnh báo. Chạy `.\gradlew.bat :app:connectedDebugAndroidTest` khi có thiết bị/emulator để kiểm tra Compose: ba locale, plurals/fallback, quay về hệ thống, tên Unicode/dài/rỗng và Activity recreate. Test ghi tên mẫu vào dữ liệu ứng dụng. Các case xoay thật, force-stop và đổi App language trong Settings vẫn cần nghiệm thu riêng.

Trước mỗi kịch bản độc lập, force-stop ứng dụng rồi mở lại:

~~~powershell
& $adbPath -s $serial shell am force-stop vn.training.bai02
& $adbPath -s $serial shell am start -W -n "vn.training.bai02/.MainActivity"
~~~

Force-stop không xóa dữ liệu lưu bằng SharedPreferences. Chỉ dùng thao tác xóa dữ liệu trong Settings nếu kịch bản yêu cầu reset dữ liệu, và ghi lại việc reset trong biên bản.

## 5. Test chức năng

Các case giữ dữ liệu được chạy liên tiếp theo mô tả, không xóa dữ liệu app giữa các bước.

| Case | Thao tác | Kết quả mong đợi |
|---|---|---|
| TC01 | Mở app mới; bấm English | Title tiếng Anh, Hello và 0/1/2/5 đúng |
| TC02 | Nhập “Nguyễn Văn Android”; chọn Tiếng Việt | Lời chào tiếng Việt, giữ nguyên tên |
| TC03 | Chọn 日本語 | Tiêu đề/lời chào tiếng Nhật; giữ tên và Unicode |
| TC04 | Xoay màn hình sau TC03 | UI không crash; tên và locale còn đúng |
| TC05 | Force-stop rồi mở lại | SharedPreferences giữ tên; locale vẫn là lựa chọn trước |
| TC06 | Chọn Theo hệ thống, đổi ngôn ngữ thiết bị rồi quay lại | Resource theo locale hệ thống; với locale không hỗ trợ kiểm tra fallback |
| TC07 | Chọn Việt/Nhật, đọc dòng fallback | Dòng này vẫn tiếng Anh vì thiếu bản dịch chủ ý |
| TC08 | Xem số 0, 1, 2, 5 trong từng ngôn ngữ | 0 dùng string zero; số còn lại hiển thị đúng lượng/định dạng |
| TC09 | Nhập tên rất dài, đổi locale, xoay ngang | Không mất nội dung; màn hình cuộn được |
| TC10 | API 33+: đổi App language trong Settings của app | App cập nhật theo lựa chọn hệ thống |
| TC11 | API 31/32: chọn locale rồi force-stop | autoStoreLocales giữ lựa chọn sau mở lại |

API 31/32 và API 33+ cần hai cấu hình thiết bị để xác nhận hai cơ chế lưu locale. Locale vẫn còn sau force-stop khác với sau xóa dữ liệu; xóa dữ liệu sẽ reset lựa chọn.

## 6. Lỗi thường gặp và phạm vi đã kiểm chứng

- Không đổi UI: dùng AppCompatActivity và AppCompatDelegate; đừng đặt configChanges để vô tình chặn recreate.
- Mất tên: kiểm tra `rememberSaveable` và SharedPreferences; không gán tên mặc định sau khi restore.
- Crash khi format: giữ placeholder %1$s cho tên, %d cho số ở mọi bản dịch.
- Locale hệ thống không đồng bộ: kiểm tra localeConfig và thử trên API 33+.
- Lint báo MissingTranslation cho fallback: chỉ bỏ qua cảnh báo ở key minh họa này, không bỏ qua toàn bộ tài nguyên.

[RESULTS.md](RESULTS.md) phân biệt kết quả bản Views trước đây và kết quả kiểm chứng bản Compose. Các cấu hình chưa chạy cần giữ trạng thái chưa kiểm chứng.

## 7. Ghi kết quả kiểm thử

Mỗi case ghi: ngày chạy, model thiết bị, API, trạng thái quyền ban đầu, thao tác, kết quả thực tế, PASS/FAIL và bằng chứng. Có thể ghi theo mẫu:

| Case | Thiết bị/API | Kết quả mong đợi | Kết quả thực tế | PASS/FAIL/Chưa chạy |
|---|---|---|---|---|
| TC01 | ... | ... | ... | ... |

Lưu log sau thao tác bằng lệnh dưới; đổi tên file cho từng case:

~~~powershell
New-Item -ItemType Directory -Force -Path "./test-results" | Out-Null
& $adbPath -s $serial logcat -d -s "ActivityTaskManager" |
    Set-Content -Encoding utf8 "./test-results/TC01.log"
~~~

Chụp màn hình bằng Android Studio hoặc emulator và lưu cùng log. Khi app crash, xem thêm logcat -d -s AndroidRuntime. **Không coi build thành công là tất cả case runtime đều PASS.**

Nếu muốn dùng script có sẵn, đọc [verification/README.md](../verification/README.md). Trước khi chạy phải sửa ROOT, ADB, CLI trong runtime_checks.py theo máy: hiện còn đường dẫn D:/Exercie và user ADMIN; script còn cố định thiết bị emulator-5554. Script cần Python 3 và Android CLI, có thể cài APK/đổi quyền và ghi đè evidence; không chạy đồng thời trên cùng thiết bị.

Tùy chọn từ thư mục gốc: python ./verification/runtime_checks.py 2; kiểm tra thêm nhập liệu bằng python ./verification/extra_checks.py locale_input sau khi sửa cấu hình script.
