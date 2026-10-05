package vn.training.books.server
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Bundle
class MainActivity : ComposeActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { DemoScreen { Text("AIDL Server — cài client cùng debug certificate. Service chạy process :books, dữ liệu chỉ giữ trong RAM."); Text("Activity PID=${android.os.Process.myPid()}") } } }
}
