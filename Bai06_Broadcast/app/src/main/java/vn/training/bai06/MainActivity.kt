package vn.training.bai06

import android.content.*
import android.net.*
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
class MainActivity : DemoActivity() {
    private lateinit var battery: TextView
    private lateinit var airplane: TextView
    private lateinit var custom: TextView
    private lateinit var network: TextView
    private var registered=false
    private var active=false
    private var count=0
    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }
    private val receiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            when(intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> { val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1); val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100); battery.text="Pin: ${if(level>=0 && scale>0) level*100/scale else -1}%" }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> airplane.text="Máy bay: ${intent.getBooleanExtra("state",false)}"
            }; Log.i("BroadcastDemo","system ${intent.action}")
        }
    }
    private val internal=object : BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) { count++; custom.text="Custom event #$count: ${intent.getStringExtra("message")}"; Log.i("BroadcastDemo","custom count=$count") }
    }
    private val callback=object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(value: Network) { updateNetwork() }
        override fun onLost(value: Network) { updateNetwork() }
        override fun onCapabilitiesChanged(value: Network,caps: NetworkCapabilities) { updateNetwork() }
    }
    private fun updateNetwork() { runOnUiThread { if(active) {
        val caps=connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        network.text="Có mạng: ${caps!=null}\nINTERNET: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true}\nVALIDATED: ${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true}"
    } } }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        count=state?.getInt("count") ?: 0
        battery=text("Pin")
        airplane=text("Máy bay: ${android.provider.Settings.Global.getInt(contentResolver,android.provider.Settings.Global.AIRPLANE_MODE_ON,0)==1}")
        custom=text("Custom count=$count"); network=text("")
        button("Gửi broadcast nội bộ") { sendBroadcast(Intent("$packageName.CUSTOM").setPackage(packageName).putExtra("message","Hello Android")) }
        button("Mở cài đặt mạng/máy bay") { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }
        text("Receiver được đăng ký onStart, hủy onStop. onReceive chỉ cập nhật UI, không làm việc nặng. goAsync vẫn có timeout, dùng Worker cho công việc dài.")
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
