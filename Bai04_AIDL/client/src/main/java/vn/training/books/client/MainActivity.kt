package vn.training.books.client

import android.content.*
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.*
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import vn.training.books.*
class MainActivity : ComposeActivity() {
    private var status by mutableStateOf("")
    private var output by mutableStateOf("")
    private var input by mutableStateOf("")
    private val sessions=mutableListOf<Session>()
    private var operationsEnabled by mutableStateOf(false)
    private var generation=0
    inner class Session(val number: Int) : ServiceConnection {
        @Volatile var api: IBookService?=null
        var bound=false
        var registered=false
        val listener=object : IBookListener.Stub() {
            override fun onBookAdded(book: Book) {
                runOnUiThread { if(sessions.contains(this@Session)) { output += "\nCallback session $number: ${book.title}"; Log.i("BookClient","callback $number ${book.id} PID=${Process.myPid()}") } }
            }
        }
        override fun onServiceConnected(name: ComponentName,binder: IBinder) {
            api=IBookService.Stub.asInterface(binder)
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { synchronized(this@Session) { if(api!=null && !registered) { api!!.registerListener(listener); registered=true } } }; refresh() }
                catch(e: Exception) { lost(e.message.orEmpty()) }
            }
        }
        fun lost(reason: String) { synchronized(this) { api=null; registered=false }; status="Mất kết nối: $reason. Nhấn Kết nối lại."; refresh() }
        override fun onServiceDisconnected(name: ComponentName) { lost("server process chết") }
        override fun onBindingDied(name: ComponentName) { lost("binding died") }
        override fun onNullBinding(name: ComponentName) { lost("null binding") }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        status="Client PID=${Process.myPid()}"
        input=state?.getString("input").orEmpty()
        setContent {
            DemoScreen {
                Text(status)
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Tên sách / ID cần xóa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ActionButton("Kết nối lại / tạo 2 phiên client") { disconnect(); connect() }
                ActionButton("Thêm sách từ cả 2 phiên đồng thời", enabled = operationsEnabled) {
                    val title=input.ifBlank { "Android Book" }.take(180)
                    lifecycleScope.launch {
                        val failures=withContext(Dispatchers.IO) { sessions.toList().map { s -> async { runCatching { s.api?.addBook(Book((System.nanoTime() and 0x7fffffff).toInt(),"$title / ${s.number}")) }.exceptionOrNull() } }.awaitAll().filterNotNull() }
                        if(failures.isNotEmpty()) message("Server không sẵn sàng: ${failures.first().message}")
            }
        }
                ActionButton("Lấy danh sách và số lượng (IO)", enabled = operationsEnabled) { call { remote -> "count=${remote.getBookCount()}\n"+remote.getBooks().joinToString("\n") { "${it.id}: ${it.title}" } } }
                ActionButton("removeBook(id)", enabled = operationsEnabled) { val id=input.toIntOrNull(); if(id==null) message("Nhập ID số") else call { "removed=${it.removeBook(id)}" } }
                Text(output)
            }
        }
        refresh()
    }
    private fun refresh() { operationsEnabled=sessions.any { s -> s.api!=null && s.registered }; if(sessions.any { it.api!=null }) status="Client PID=${Process.myPid()} — connected=${sessions.count { it.api!=null }}" }
    private fun connect() {
        repeat(2) { i -> val s=Session(i+1); sessions+=s
            try { s.bound=bindService(Intent().setComponent(ComponentName("vn.training.books.server","vn.training.books.server.BookService")),s,BIND_AUTO_CREATE); if(!s.bound) status="Bind thất bại: cài server trước" }
            catch(e: SecurityException) { status="Signature permission: ${e.message}" }
        }; refresh()
    }
    private fun call(block: (IBookService)->String) {
        val remote=sessions.firstOrNull { it.api!=null }?.api ?: return
        lifecycleScope.launch { try { output=withContext(Dispatchers.IO) { block(remote) } } catch(e: Exception) { message("Remote call thất bại: ${e.message}"); refresh() } }
    }
    private fun disconnect() {
        generation++
        sessions.toList().forEach { s ->
            val remote=s.api
            synchronized(s) { s.api=null }
            if(remote!=null) CoroutineScope(Dispatchers.IO).launch { synchronized(s) { try { if(s.registered) remote.unregisterListener(s.listener) } catch(_: RemoteException) {} finally { s.registered=false } } }
            if(s.bound) { unbindService(s); s.bound=false }
        }; sessions.clear(); refresh()
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("input", input); super.onSaveInstanceState(out) }
    override fun onStart() { super.onStart(); connect() }
    override fun onStop() { disconnect(); super.onStop() }
}
