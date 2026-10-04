package vn.training.bai07

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
class MainActivity : DemoActivity() {
    private lateinit var status: TextView
    private val manager by lazy { getSystemService(NotificationManager::class.java) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        manager.createNotificationChannel(NotificationChannel("alarms","Demo cuộc gọi / báo thức",NotificationManager.IMPORTANCE_HIGH))
        status=text("")
        button("Xin quyền notification") { if(Build.VERSION.SDK_INT>=33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),7) else message("API này không cần runtime permission") }
        button("Full-Screen special access settings") { if(Build.VERSION.SDK_INT>=34) startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:$packageName"))) else message("Chỉ áp dụng API 34+") }
        button("Cài đặt channel / importance") { startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName).putExtra(Settings.EXTRA_CHANNEL_ID,"alarms")) }
        button("Gửi báo thức ngay") { send("Báo thức") }
        button("Gửi cuộc gọi sau 10s — khóa màn hình để thử") { lifecycleScope.launch { message("Khóa màn hình trong 10 giây"); delay(10000); send("Cuộc gọi") } }
        button("Gửi 2 sự kiện ID khác nhau") { send("Sự kiện A"); send("Sự kiện B") }
        text("Khi đang mở khóa, hệ thống có thể chỉ hiện heads-up. Click nội dung notification luôn dùng Content Intent riêng cho từng ID. Hành vi còn phụ thuộc permission và importance của channel.")
    }
    private fun allowed()=Build.VERSION.SDK_INT<34 || manager.canUseFullScreenIntent()
    private fun send(label: String) {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED || !manager.areNotificationsEnabled()) { message("Notification bị tắt. Cấp quyền bằng nút hoặc cài đặt ứng dụng."); return }
        val prefs=getSharedPreferences("events",MODE_PRIVATE)
        val id=prefs.getInt("last",100)+1; prefs.edit().putInt("last",id).apply()
        val intent=Intent(this,AlarmActivity::class.java).setData(Uri.parse("demo://event/$id")).putExtra("event_id",id).putExtra("label",label)
        val content=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val full=PendingIntent.getActivity(this,id+100000,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=Notification.Builder(this,"alarms").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("$label #$id").setContentText("Nhấn để mở sự kiện $id").setCategory(Notification.CATEGORY_ALARM).setContentIntent(content).setAutoCancel(true)
        if(allowed()) builder.setFullScreenIntent(full,true)
        manager.notify(id,builder.build()); android.util.Log.i("FullScreenDemo","event=$id label=$label fullScreen=${allowed()} importance=${manager.getNotificationChannel("alarms").importance}"); refresh()
    }
    private fun refresh() { status.text="API=${Build.VERSION.SDK_INT}\nNotifications=${manager.areNotificationsEnabled()}\ncanUseFullScreenIntent=${allowed()}\nChannel importance=${manager.getNotificationChannel("alarms").importance}" }
    override fun onResume() { super.onResume(); refresh() }
}
class AlarmActivity : DemoActivity() {
    private lateinit var event: TextView
    override fun onCreate(state: Bundle?) { super.onCreate(state); setShowWhenLocked(true); setTurnScreenOn(true); event=text(""); render(); button("Đóng sự kiện") { getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra("event_id",0)); finish() } }
    private fun render() { event.text="${intent.getStringExtra("label")}\nEvent ID=${intent.getIntExtra("event_id",0)}"; android.util.Log.i("FullScreenDemo","opened ${event.text}") }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); render() }
}
