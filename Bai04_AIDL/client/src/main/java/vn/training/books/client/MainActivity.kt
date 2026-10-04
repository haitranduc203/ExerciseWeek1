package vn.training.books.client

import android.content.*
import android.os.*
import android.util.Log
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import vn.training.books.*
class MainActivity : DemoActivity() {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var input: EditText
    private val sessions=mutableListOf<Session>()
    private val operations=mutableListOf<Button>()
    private var generation=0
    inner class Session(val number: Int) : ServiceConnection {
        @Volatile var api: IBookService?=null
        var bound=false
        var registered=false
        val listener=object : IBookListener.Stub() {
            override fun onBookAdded(book: Book) {
                runOnUiThread { if(sessions.contains(this@Session)) { output.append("\nCallback session $number: ${book.title}"); Log.i("BookClient","callback $number ${book.id} PID=${Process.myPid()}") } }
            }
        }
        override fun onServiceConnected(name: ComponentName,binder: IBinder) {
            api=IBookService.Stub.asInterface(binder)
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { synchronized(this@Session) { if(api!=null && !registered) { api!!.registerListener(listener); registered=true } } }; refresh() }
                catch(e: Exception) { lost(e.message.orEmpty()) }
            }
        }
        fun lost(reason: String) { synchronized(this) { api=null; registered=false }; status.text="Mất kết nối: $reason. Nhấn Kết nối lại."; refresh() }
        override fun onServiceDisconnected(name: ComponentName) { lost("server process chết") }
        override fun onBindingDied(name: ComponentName) { lost("binding died") }
        override fun onNullBinding(name: ComponentName) { lost("null binding") }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        status=text("Client PID=${Process.myPid()}")
        input=EditText(this).apply { id=R.id.name_input; hint="Tên sách / ID cần xóa"; column.addView(this) }
        button("Kết nối lại / tạo 2 phiên client") { disconnect(); connect() }
        operations+=button("Thêm sách từ cả 2 phiên đồng thời") {
            val title=input.text.toString().ifBlank { "Android Book" }.take(180)
            lifecycleScope.launch {
                val failures=withContext(Dispatchers.IO) { sessions.toList().map { s -> async { runCatching { s.api?.addBook(Book((System.nanoTime() and 0x7fffffff).toInt(),"$title / ${s.number}")) }.exceptionOrNull() } }.awaitAll().filterNotNull() }
                if(failures.isNotEmpty()) message("Server không sẵn sàng: ${failures.first().message}")
            }
        }
        operations+=button("Lấy danh sách và số lượng (IO)") { call { remote -> "count=${remote.getBookCount()}\n"+remote.getBooks().joinToString("\n") { "${it.id}: ${it.title}" } } }
        operations+=button("removeBook(id)") { val id=input.text.toString().toIntOrNull(); if(id==null) message("Nhập ID số") else call { "removed=${it.removeBook(id)}" } }
        output=text("")
        refresh()
    }
    private fun refresh() { operations.forEach { it.isEnabled=sessions.any { s -> s.api!=null && s.registered } }; if(sessions.any { it.api!=null }) status.text="Client PID=${Process.myPid()} — connected=${sessions.count { it.api!=null }}" }
    private fun connect() {
        repeat(2) { i -> val s=Session(i+1); sessions+=s
            try { s.bound=bindService(Intent().setComponent(ComponentName("vn.training.books.server","vn.training.books.server.BookService")),s,BIND_AUTO_CREATE); if(!s.bound) status.text="Bind thất bại: cài server trước" }
            catch(e: SecurityException) { status.text="Signature permission: ${e.message}" }
        }; refresh()
    }
    private fun call(block: (IBookService)->String) {
        val remote=sessions.firstOrNull { it.api!=null }?.api ?: return
        lifecycleScope.launch { try { output.text=withContext(Dispatchers.IO) { block(remote) } } catch(e: Exception) { message("Remote call thất bại: ${e.message}"); refresh() } }
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
    override fun onStart() { super.onStart(); connect() }
    override fun onStop() { disconnect(); super.onStop() }
}
