package vn.training.books.server
import android.os.Bundle
class MainActivity : DemoActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); text("AIDL Server — cài client cùng debug certificate. Service chạy process :books, dữ liệu chỉ giữ trong RAM."); text("Activity PID=${android.os.Process.myPid()}") }
}
