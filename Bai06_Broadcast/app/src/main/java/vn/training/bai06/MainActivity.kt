package vn.training.bai06

import android.content.*
import android.net.*
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
class MainActivity : ComposeActivity() {
    private var battery by mutableStateOf("")
    private var airplane by mutableStateOf("")
    private var custom by mutableStateOf("")
    private var network by mutableStateOf("")
    private var registered=false
    private var active=false
    private var count=0
    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }
    private val receiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            when(intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> { val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1); val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100); battery="Pin: ${if(level>=0 && scale>0) level*100/scale else -1}%" }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> airplane="Máy bay: ${intent.getBooleanExtra("state",false)}"
            }; Log.i("BroadcastDemo","system ${intent.action}")
        }
    }
    private val internal=object : BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) { count++; custom="Custom event #$count: ${intent.getStringExtra("message")}"; Log.i("BroadcastDemo","custom count=$count") }
    }
    private val callback=object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(value: Network) { updateNetwork() }
        override fun onLost(value: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(value: Network,caps: NetworkCapabilities) { updateNetwork() }
    }
    private fun updateNetwork() { runOnUiThread { if(active) {
        val caps=connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        network="Có mạng: ${caps!=null}\nINTERNET: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}\nVALIDATED: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true}"
    } } }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        count=state?.getInt("count") ?: 0
        battery="Pin"
        airplane="Máy bay: ${android.provider.Settings.Global.getInt(contentResolver,android.provider.Settings.Global.AIRPLANE_MODE_ON,0)==1}"
        custom="Custom count=$count"
        setContent {
            DemoScreen {
                Text(battery)
                Text(airplane)
                Text(custom)
                Text(network)
                ActionButton("Gửi broadcast nội bộ") { sendBroadcast(Intent("$packageName.CUSTOM").setPackage(packageName).putExtra("message","Hello Android")) }
                ActionButton("Mở cài đặt mạng/máy bay") { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }
                Text("Receiver được đăng ký onStart, hủy onStop. onReceive chỉ cập nhật UI, không làm việc nặng. goAsync vẫn có timeout, dùng Worker cho công việc dài.")
            }
        }
    }
    override fun onStart() {
        super.onStart(); active=true
        if(!registered) {
            ContextCompat.registerReceiver(this,receiver,IntentFilter().apply { addAction(Intent.ACTION_BATTERY_CHANGED); addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED) },ContextCompat.RECEIVER_EXPORTED)
            ContextCompat.registerReceiver(this,internal,IntentFilter("$packageName.CUSTOM"),ContextCompat.RECEIVER_NOT_EXPORTED)
            connectivity.registerDefaultNetworkCallback(callback); registered=true
            Log.i("BroadcastDemo","registered")
        }; updateNetwork()
    }
    override fun onStop() {
        active=false
        if(registered) { unregisterReceiver(receiver); unregisterReceiver(internal); connectivity.unregisterNetworkCallback(callback); registered=false; Log.i("BroadcastDemo","unregistered") }
        super.onStop()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("count",count); super.onSaveInstanceState(out) }
}
