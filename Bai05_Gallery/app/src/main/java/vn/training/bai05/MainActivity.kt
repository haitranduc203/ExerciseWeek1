package vn.training.bai05

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import coil.load
import kotlinx.coroutines.*
data class Photo(val uri: Uri,val name: String)
class MainActivity : DemoActivity() {
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var adapter: Photos
    private var pending: Uri?=null
    private var last: Uri?=null
    private var loadJob: Job?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    private val picker=registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) { last=uri; result.text="Photo Picker URI grant: $uri"; column.addView(ImageView(this).apply { layoutParams=android.view.ViewGroup.LayoutParams(-1,400); load(uri) }) } }
    private val camera=registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri=pending; pending=null; getSharedPreferences("camera_state",MODE_PRIVATE).edit().remove("pending").apply(); android.util.Log.i("GalleryDemo","camera result=$success uri=$uri")
        if(uri!=null) lifecycleScope.launch {
            try {
                val saved=withContext(NonCancellable+Dispatchers.IO) {
                    try { if(success) ImageSaver.importCamera(contentResolver,uri) else null }
                    finally { contentResolver.delete(uri,null,null) }
                }
                if(saved!=null) { last=saved; result.text="Camera: $saved" } else result.text="Đã hủy camera, đã dọn ảnh tạm"
                refresh()
            } catch(e: Exception) { if(e is CancellationException) throw e; message("Camera failed: ${e.message}") }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        pending=(state?.getString("pending") ?: getSharedPreferences("camera_state",MODE_PRIVATE).getString("pending",null))?.let(Uri::parse); last=state?.getString("last")?.let(Uri::parse)
        status=text("")
        button("Cấp quyền / chọn lại ảnh (API 34+)") {
            permission.launch(when { Build.VERSION.SDK_INT>=34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                Build.VERSION.SDK_INT>=33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) })
        }
        button("Photo Picker") { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        button("Tạo và lưu bitmap JPEG") { save(false) }
        button("Mô phỏng lỗi lưu — kiểm tra cleanup") { save(true) }
        button("Chụp ảnh TakePicture") {
            if(pending!=null) return@button
            lifecycleScope.launch {
                var created: Uri?=null
                try {
                    val uri=withContext(Dispatchers.IO) { ImageSaver.cameraUri(this@MainActivity).also { created=it } }
                    pending=uri; getSharedPreferences("camera_state",MODE_PRIVATE).edit().putString("pending",uri.toString()).apply(); android.util.Log.i("GalleryDemo","launch camera uri=$uri"); camera.launch(uri)
                } catch(e: Exception) {
                    pending=null
                    withContext(NonCancellable+Dispatchers.IO) { created?.let { contentResolver.delete(it,null,null) } }
                    if(e is CancellationException) throw e
                    message("Không mở được camera: ${e.message}")
                }
            }
        }
        button("Mở ảnh vừa lưu trong Gallery hệ thống") { last?.let { uri -> try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch(_: ActivityNotFoundException) { message("Thiết bị không có app xem ảnh") } } ?: message("Chưa chọn/lưu ảnh") }
        result=text(last?.toString().orEmpty())
        adapter=Photos { uri -> last=uri; result.text=uri.toString() }
        val grid=RecyclerView(this).apply { layoutManager=GridLayoutManager(this@MainActivity,3); adapter=this@MainActivity.adapter; layoutParams=android.view.ViewGroup.LayoutParams(-1,800) }; column.addView(grid)
    }
    private fun granted(p: String)=checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED
    private fun access(): String = when {
        Build.VERSION.SDK_INT>=33 && granted(Manifest.permission.READ_MEDIA_IMAGES) -> "FULL"
        Build.VERSION.SDK_INT>=34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> "PARTIAL"
        Build.VERSION.SDK_INT<=32 && granted(Manifest.permission.READ_EXTERNAL_STORAGE) -> "FULL"
        else -> "DENIED — có thể chọn Photo Picker; vẫn query được ảnh do app tạo"
    }
    private fun refresh() {
        loadJob?.cancel()
        loadJob=lifecycleScope.launch {
            status.text=access()
            try {
                val photos=withContext(Dispatchers.IO) {
                    val rows=mutableListOf<Photo>()
                    contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME),null,null,"${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
                        val id=cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID); val name=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        while(cursor.moveToNext()) rows+=Photo(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(id)),cursor.getString(name))
                    }; rows
                }; adapter.rows=photos; adapter.notifyDataSetChanged(); status.append("\n${photos.size} ảnh được phép đọc")
            } catch(e: SecurityException) { adapter.rows=emptyList(); adapter.notifyDataSetChanged(); status.text="Quyền đã đổi: ${e.message}" }
        }
    }
    private fun save(fail: Boolean) { lifecycleScope.launch {
        try {
            val uri=withContext(Dispatchers.IO) {
                val bitmap=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888)
                try { Canvas(bitmap).apply { drawColor(Color.rgb(35,110,160)); drawText("Android Gallery",35f,300f,Paint().apply { color=Color.WHITE; textSize=52f }) }; ImageSaver.save(contentResolver,bitmap,fail) }
                finally { bitmap.recycle() }
            }; last=uri; result.text="Saved: $uri"; refresh()
        } catch(e: Exception) { if(e is CancellationException) throw e; result.text="Lưu thất bại, đã cleanup: ${e.message}" }
    } }
    override fun onResume() { super.onResume(); if(::adapter.isInitialized) refresh() }
    override fun onSaveInstanceState(out: Bundle) { out.putString("pending",pending?.toString()); out.putString("last",last?.toString()); super.onSaveInstanceState(out) }
}
class Photos(private val click: (Uri)->Unit) : RecyclerView.Adapter<Photos.Holder>() {
    var rows: List<Photo> = emptyList()
    class Holder(val view: ImageView) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: android.view.ViewGroup,type: Int)=Holder(ImageView(parent.context).apply { layoutParams=android.view.ViewGroup.LayoutParams(-1,240); scaleType=ImageView.ScaleType.CENTER_CROP })
    override fun getItemCount()=rows.size
    override fun onBindViewHolder(holder: Holder,position: Int) { val photo=rows[position]; holder.view.contentDescription=photo.name; holder.view.load(photo.uri); holder.view.setOnClickListener { click(photo.uri) } }
}
