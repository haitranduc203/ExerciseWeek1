package vn.training.bai03

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
class MainActivity : ComposeActivity() {
    private var status by mutableStateOf("")
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            DemoScreen {
                Text("Music Player — bộ đếm mô phỏng, không phát audio")
                Text(status)
                ActionButton("Cấp quyền notification (API 33+)") { if(android.os.Build.VERSION.SDK_INT>=33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),1) }
                ActionButton("Play / Start") { startForegroundService(Intent(this@MainActivity,PlayerService::class.java).setAction("PLAY")) }
                ActionButton("Pause") { if(PlayerService.running) startService(Intent(this@MainActivity,PlayerService::class.java).setAction("PAUSE")) }
                ActionButton("Stop") { stopService(Intent(this@MainActivity,PlayerService::class.java)) }
                ActionButton("Start dataSync timeout demo") { startForegroundService(Intent(this@MainActivity,SyncService::class.java)) }
                ActionButton("Stop dataSync") { stopService(Intent(this@MainActivity,SyncService::class.java)) }
            }
        }
        lifecycleScope.launch { while(isActive) { status="Service=${PlayerService.running} playing=${PlayerService.playing} elapsed=${PlayerService.seconds}s"; delay(500) } }
    }
}
