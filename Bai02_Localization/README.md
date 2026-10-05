# Bài 2: Localization
## Cấu hình
Kotlin + Jetpack Compose Material 3; minSdk=31, targetSdk=36, compileSdk=36; AGP=9.1.1, Gradle=9.3.1; Compose compiler=2.3.20, Compose BOM=2026.03.01.

## Giao diện Compose
- `MainActivity.kt`: `setContent`, lưu tên bằng `rememberSaveable` và SharedPreferences, đổi locale bằng AppCompat.
- `LocalizationScreen.kt`: `OutlinedTextField`, lời chào, plurals 0/1/2/5, nút chọn ngôn ngữ, fallback; có preview English/Việt/日本語, theme sáng/tối và màn hình cuộn với system/IME insets.
- Không dùng layout XML, ViewBinding hay `DemoActivity`. Giữ `strings.xml`, theme AppCompat và `locales_config.xml` để Android quản lý tài nguyên và ngôn ngữ ứng dụng.
## Build
PowerShell: `.\gradlew.bat :app:assembleDebug :app:lintDebug`.

Có emulator/thiết bị: `.\gradlew.bat :app:connectedDebugAndroidTest`. Test Compose kiểm tra đổi ba ngôn ngữ, plurals/fallback, về locale hệ thống và giữ tên Unicode/dài/rỗng khi Activity recreate. Test nhập tên mẫu và đặt lại locale về hệ thống sau mỗi case.
## Chạy và nghiệm thu
Nhập tên dài; lần lượt đổi English/Việt/日本語/System; xoay, force-stop rồi mở lại. Kiểm tra tên còn nguyên và số 0/1/2/5.
Fallback đã được tạo chủ ý: chuỗi fallback chỉ có trong values mặc định. AppCompat autoStoreLocales cho <=32, hệ thống localeConfig cho >=33. Theo hệ thống dùng locale list rỗng.
## Log
`adb logcat -s ActivityTaskManager`
Chạy từng kịch bản từ trạng thái ban đầu; dùng `adb shell am force-stop <applicationId>` và mở lại. Bài 1 có thể xóa task từ Recents để đảm bảo không giữ task cũ.
## Bằng chứng
RESULTS.md và evidence/ ghi kết quả đã đo; ô chưa chạy phải được giữ là chưa kiểm chứng runtime.

## APK đã build
- `app/build/outputs/apk/debug/app-debug.apk`

Test Compose nằm trong `app/src/androidTest/java/vn/training/bai02/LocalizationTest.kt` và dùng source set Android mặc định.
