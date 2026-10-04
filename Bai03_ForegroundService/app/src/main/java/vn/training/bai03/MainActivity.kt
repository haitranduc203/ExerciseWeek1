package vn.training.bai03

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
class MainActivity : DemoActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        text("Music Player — bộ đếm mô phỏng, không phát audio")
        val status=text("")
        button("Cấp quyền notification (API 33+)") { if(android.os.Build.VERSION.SDK_INT>=33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),1) }
        button("Play / Start") { startForegroundService(Intent(this,PlayerService::class.java).setAction("PLAY")) }
        button("Pause") { if(PlayerService.running) startService(Intent(this,PlayerService::class.java).setAction("PAUSE")) }
        button("Stop") { stopService(Intent(this,PlayerService::class.java)) }
        button("Start dataSync timeout demo") { startForegroundService(Intent(this,SyncService::class.java)) }
        button("Stop dataSync") { stopService(Intent(this,SyncService::class.java)) }
        lifecycleScope.launch { while(isActive) { status.text="Service=${PlayerService.running} playing=${PlayerService.playing} elapsed=${PlayerService.seconds}s"; delay(500) } }
    }
}
