package vn.training.bai03

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
class PlayerService : Service() {
    companion object { @Volatile var running=false; @Volatile var playing=false; @Volatile var seconds=0 }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private var ticker: Job?=null
    override fun onBind(intent: Intent?): IBinder?=null
    private fun notification(): Notification {
        val open=PendingIntent.getActivity(this,1,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=Notification.Builder(this,"player").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Player mô phỏng").setContentText("${if(playing) "Playing" else "Paused"} — $seconds s").setContentIntent(open).setOngoing(true)
        listOf("PLAY","PAUSE","STOP").forEachIndexed { index, action ->
            val token=PendingIntent.getService(this,index+10,Intent(this,PlayerService::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null,action,token).build())
        }
        return builder.build()
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("player","Playback",NotificationManager.IMPORTANCE_LOW))
        if(intent?.action=="STOP") { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        running=true; playing=intent?.action!="PAUSE"
        startForeground(1,notification())
        if(ticker==null) ticker=scope.launch { while(isActive) { delay(1000); if(playing) seconds++; getSystemService(NotificationManager::class.java).notify(1,notification()); Log.i("PlayerDemo","tick=$seconds playing=$playing thread=${Thread.currentThread().name}") } }
        return START_NOT_STICKY
    }
    override fun onDestroy() { scope.cancel(); ticker=null; running=false; playing=false; seconds=0; stopForeground(STOP_FOREGROUND_REMOVE); Log.i("PlayerDemo","stopped"); super.onDestroy() }
}
class SyncService : Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    override fun onBind(intent: Intent?): IBinder?=null
    override fun onStartCommand(intent: Intent?, flags: Int,startId: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("sync","Sync timeout demo",NotificationManager.IMPORTANCE_LOW))
        startForeground(2,Notification.Builder(this,"sync").setSmallIcon(android.R.drawable.ic_popup_sync).setContentTitle("dataSync timeout demo").build())
        scope.launch { while(isActive) { delay(1000); Log.i("SyncDemo","running on ${Thread.currentThread().name}") } }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int,fgsType: Int) { Log.i("SyncDemo","onTimeout id=$startId type=$fgsType"); scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { scope.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
}
