package vn.training.books.server

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import vn.training.books.*
class BookService : Service() {
    private val books=linkedMapOf<Int,Book>()
    private val listeners=RemoteCallbackList<IBookListener>()
    private val callbackLock=Any()
    private val binder=object : IBookService.Stub() {
        override fun addBook(book: Book) {
            require(book.title.length in 1..200) { "Title length must be 1..200" }
            synchronized(this@BookService.books) { this@BookService.books[book.id]=book }
            Log.i("BookServer","add=${book.id} serverPID=${Process.myPid()} callerPID=${Binder.getCallingPid()}")
            synchronized(callbackLock) {
                val count=listeners.beginBroadcast()
                try { for(i in 0 until count) { try { listeners.getBroadcastItem(i).onBookAdded(book) } catch(_: RemoteException) {} } }
                finally { listeners.finishBroadcast() }
            }
        }
        override fun getBooks(): MutableList<Book> = synchronized(this@BookService.books) { this@BookService.books.values.toMutableList() }
        override fun getBookCount(): Int = synchronized(this@BookService.books) { this@BookService.books.size }
        override fun removeBook(id: Int): Boolean = synchronized(this@BookService.books) { this@BookService.books.remove(id)!=null }
        override fun registerListener(listener: IBookListener) { listeners.register(listener) }
        override fun unregisterListener(listener: IBookListener) { listeners.unregister(listener) }
    }
    override fun onBind(intent: Intent): IBinder { Log.i("BookServer","bind PID=${Process.myPid()}"); return binder }
    override fun onDestroy() { listeners.kill(); super.onDestroy() }
}
