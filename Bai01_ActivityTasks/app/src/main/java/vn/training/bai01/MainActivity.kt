package vn.training.bai01

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.core.app.TaskStackBuilder
import java.util.UUID

open class StackActivity : DemoActivity() {
    private val instance = UUID.randomUUID().toString().take(8)
    private lateinit var status: TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        status = text("")
        show(intent); record("onCreate")
        listOf(MainActivity::class.java, BStandardActivity::class.java, BTopActivity::class.java,
            CActivity::class.java, DActivity::class.java, EActivity::class.java).forEach { target ->
            button("Mở ${target.simpleName}") { open(target,0) }
        }
        button("CLEAR_TOP → B standard") { open(BStandardActivity::class.java,Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        button("CLEAR_TOP | SINGLE_TOP → B standard") { open(BStandardActivity::class.java,Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        button("NEW_DOCUMENT | MULTIPLE_TASK → E") { open(EActivity::class.java,Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK) }
        button("NEW_TASK | CLEAR_TASK → Login") { open(LoginActivity::class.java,Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        button("Notification → Detail với TaskStackBuilder") {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),91); return@button
            }
            val manager=getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("detail","Detail",NotificationManager.IMPORTANCE_DEFAULT))
            val token=TaskStackBuilder.create(this).addNextIntentWithParentStack(Intent(this,DetailActivity::class.java).putExtra("payload","Detail từ notification"))
                .getPendingIntent(11,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(11,Notification.Builder(this,"detail").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Mở Detail").setContentIntent(token).setAutoCancel(true).build())
        }
        text("X trong bảng đề bài có thể dùng A. D/E mở A để so sánh taskId. Reset từng kịch bản bằng adb shell am force-stop "+packageName)
    }
    private fun open(target: Class<*>, flags: Int) { startActivity(Intent(this,target).addFlags(flags).putExtra("payload","Từ ${javaClass.simpleName} lúc ${System.currentTimeMillis()}")) }
    private fun show(value: Intent) { status.text="${javaClass.simpleName}\ntaskId=$taskId instance=$instance\n${value.getStringExtra("payload") ?: "Launcher"}" }
    private fun record(event: String) { Log.i("StackDemo","$event ${javaClass.simpleName} task=$taskId instance=$instance extras=${intent.getStringExtra("payload")}") }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); show(intent); record("onNewIntent") }
    override fun onDestroy() { record("onDestroy"); super.onDestroy() }
}
class MainActivity : StackActivity()
class BStandardActivity : StackActivity()
class BTopActivity : StackActivity()
class CActivity : StackActivity()
class DActivity : StackActivity()
class EActivity : StackActivity()
class LoginActivity : StackActivity()
class DetailActivity : StackActivity()
