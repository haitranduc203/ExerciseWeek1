package vn.training.bai05

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
object ImageSaver {
    fun cameraUri(context: Context): Uri {
        val directory=java.io.File(context.filesDir,"camera").apply { mkdirs() }
        val file=java.io.File.createTempFile("capture_",".jpg",directory)
        return androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".camera",file)
    }
    fun importCamera(resolver: ContentResolver,source: Uri): Uri {
        val destination=create(resolver)
        try {
            checkNotNull(resolver.openInputStream(source)).use { input ->
                checkNotNull(resolver.openOutputStream(destination)).use { output ->
                    check(input.copyTo(output)>0) { "Camera output empty" }
                }
            }
            publish(resolver,destination); return destination
        } catch(error: Exception) { resolver.delete(destination,null,null); throw error }
    }
    fun create(resolver: ContentResolver): Uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME,"Demo_${System.nanoTime()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/BasicComponents")
        put(MediaStore.Images.Media.IS_PENDING,1)
    })) { "insert returned null" }
    fun publish(resolver: ContentResolver,uri: Uri) {
        check(resolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)==1) { "publish failed" }
    }
    fun save(resolver: ContentResolver,bitmap: Bitmap,failForTest: Boolean=false): Uri {
        val uri=create(resolver)
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { stream ->
                check(!failForTest) { "Lỗi lưu mô phỏng trước publish" }
                check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,stream)) { "compress failed" }
            }
            publish(resolver,uri); return uri
        } catch(error: Exception) { resolver.delete(uri,null,null); throw error }
    }
}
