package vn.training.bai05

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
data class Photo(val uri: Uri,val name: String)
class MainActivity : ComposeActivity() {
    private var status by mutableStateOf("")
    private var result by mutableStateOf("")
    private var photos by mutableStateOf<List<Photo>>(emptyList())
    private var pending by mutableStateOf<Uri?>(null)
    private var creatingCapture by mutableStateOf(false)
    private var last by mutableStateOf<Uri?>(null)
    private var loadJob: Job?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    private val picker=registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) { last=uri; result="Photo Picker URI grant: $uri" } }
    private val camera=registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri=pending; pending=null; getSharedPreferences("camera_state",MODE_PRIVATE).edit().remove("pending").apply(); android.util.Log.i("GalleryDemo","camera result=$success uri=$uri")
        if(uri!=null) lifecycleScope.launch {
            try {
                val saved=withContext(NonCancellable+Dispatchers.IO) {
                    try { if(success) ImageSaver.importCamera(contentResolver,uri) else null }
                    finally { contentResolver.delete(uri,null,null) }
                }
                if(saved!=null) { last=saved; result="Camera: $saved" } else result="Đã hủy camera, đã dọn ảnh tạm"
                refresh()
            } catch(e: Exception) { if(e is CancellationException) throw e; message("Camera failed: ${e.message}") }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        pending=(state?.getString("pending") ?: getSharedPreferences("camera_state",MODE_PRIVATE).getString("pending",null))?.let(Uri::parse); last=state?.getString("last")?.let(Uri::parse)
        result=last?.toString().orEmpty()
        setContent {
            DemoTheme {
                Scaffold { insets ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().padding(insets).imePadding(),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(status)
                                ActionButton("Cấp quyền / chọn lại ảnh (API 34+)") {
                                    permission.launch(when { Build.VERSION.SDK_INT>=34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                                        Build.VERSION.SDK_INT>=33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE) })
                                }
                                ActionButton("Photo Picker") { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                ActionButton("Tạo và lưu bitmap JPEG") { save(false) }
                                ActionButton("Mô phỏng lỗi lưu — kiểm tra cleanup") { save(true) }
                                ActionButton("Chụp ảnh TakePicture", enabled = pending == null && !creatingCapture) {
                                    if(pending!=null || creatingCapture) return@ActionButton
                                    creatingCapture=true
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
                                        } finally { creatingCapture=false }
                                    }
                                }
                                ActionButton("Mở ảnh vừa lưu trong Gallery hệ thống") { last?.let { uri -> try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch(_: ActivityNotFoundException) { message("Thiết bị không có app xem ảnh") } } ?: message("Chưa chọn/lưu ảnh") }
                                Text(result)
                                last?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Ảnh vừa chọn hoặc lưu",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                    )
                                }
                            }
                        }
                        items(photos, key = { it.uri.toString() }) { photo ->
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
                                    last=photo.uri
                                    result=photo.uri.toString()
                                }
                            )
                        }
                    }
                }
            }
        }
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
            status=access()
            try {
                val photos=withContext(Dispatchers.IO) {
                    val rows=mutableListOf<Photo>()
                    contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME),null,null,"${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
                        val id=cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID); val name=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        while(cursor.moveToNext()) rows+=Photo(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(id)),cursor.getString(name))
                    }; rows
                }; this@MainActivity.photos=photos; status += "\n${photos.size} ảnh được phép đọc"
            } catch(e: SecurityException) { photos=emptyList(); status="Quyền đã đổi: ${e.message}" }
        }
    }
    private fun save(fail: Boolean) { lifecycleScope.launch {
        try {
            val uri=withContext(Dispatchers.IO) {
                val bitmap=Bitmap.createBitmap(600,600,Bitmap.Config.ARGB_8888)
                try { Canvas(bitmap).apply { drawColor(Color.rgb(35,110,160)); drawText("Android Gallery",35f,300f,Paint().apply { color=Color.WHITE; textSize=52f }) }; ImageSaver.save(contentResolver,bitmap,fail) }
                finally { bitmap.recycle() }
            }; last=uri; result="Saved: $uri"; refresh()
        } catch(e: Exception) { if(e is CancellationException) throw e; result="Lưu thất bại, đã cleanup: ${e.message}" }
    } }
    override fun onResume() { super.onResume(); refresh() }
    override fun onSaveInstanceState(out: Bundle) { out.putString("pending",pending?.toString()); out.putString("last",last?.toString()); super.onSaveInstanceState(out) }
}
