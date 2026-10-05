package com.example.minigallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.ImportStatus
import com.example.minigallery.model.MediaImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Triển khai MediaRepository tương tác trực tiếp với ContentResolver và MediaStore API.
 *
 * Tuân thủ nghiêm ngặt các nguyên tắc Scoped Storage:
 * 1. Chỉ dùng ContentResolver.query và ContentUris.withAppendedId, không dùng _data file path.
 * 2. Luôn đóng Cursor bằng use.
 * 3. Chạy mọi tác vụ I/O trên Dispatchers.IO.
 * 4. Dùng ContentObserver trong callbackFlow và awaitClose để unregister chống rò rỉ bộ nhớ.
 * 5. Khi ghi ảnh: dùng IS_PENDING = 1, sao chép stream, gỡ IS_PENDING khi thành công,
 *    xóa dọn file rác nếu gặp sự cố hoặc coroutine bị cancelled.
 */
class MediaStoreRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val context: Context
) : MediaRepository {

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    override fun observeImages(): Flow<List<MediaImage>> {
        return callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    // Nhận thông báo thay đổi từ MediaStore, phát tín hiệu vào Flow
                    trySend(Unit)
                }
            }

            // Đăng ký theo dõi toàn bộ collection ảnh trên bộ nhớ ngoài
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )

            // Phát tín hiệu ban đầu để tải dữ liệu ngay khi vừa collect
            trySend(Unit)

            // Hủy đăng ký khi flow bị đóng / scope bị cancel
            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
            .conflate() // Bỏ qua các tín hiệu dồn dập
            .debounce(300L) // Gộp các thông báo thay đổi liên tiếp để tránh query MediaStore quá nhiều
            .map { queryImages() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun queryImages(): List<MediaImage> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<MediaImage>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Sắp xếp mặc định: ảnh mới thêm vào trước
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.IS_PENDING} = 0",
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "IMG_$id.jpg"
                val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn)

                // Tạo Uri theo quy chuẩn MediaStore Content Provider
                val itemUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                resultList.add(
                    MediaImage(
                        id = id,
                        uri = itemUri,
                        displayName = name,
                        mimeType = mime,
                        sizeBytes = size,
                        dateAddedSeconds = dateAdded
                    )
                )
            }
        }
        // Để lỗi quyền/I/O tới ViewModel, không báo sai thành thư viện trống.

        resultList
    }

    override suspend fun getMediaInfo(uri: Uri): ImportItem = withContext(Dispatchers.IO) {
        var displayName = "selected_photo.jpg"
        var sizeBytes = 0L

        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Dùng tên mặc định nếu không đọc được OpenableColumns
        }

        ImportItem(
            sourceUri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            status = ImportStatus.Pending
        )
    }

    override suspend fun saveImageCopy(sourceUri: Uri): Result<Uri> = withContext(Dispatchers.IO) {
        var createdTargetUri: Uri? = null

        try {
            currentCoroutineContext().ensureActive()
            // 1. Xác định MIME type và đuôi mở rộng phù hợp
            val mimeType = contentResolver.getType(sourceUri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            // 2. Lấy tên gốc của ảnh nguồn
            val originalInfo = getMediaInfo(sourceUri)
            val baseName = originalInfo.displayName
                .substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .ifEmpty { "IMG" }

            // Tạo tên file mới tránh trùng lặp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val newFileName = "MiniGallery_${timestamp}_$baseName.$extension"

            // 3. Chuẩn bị ContentValues theo Scoped Storage (Pictures/MiniGallery)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/MiniGallery"
                )
                // Đặt cờ IS_PENDING = 1: Báo hiệu tệp đang được ghi, các ứng dụng khác chưa nhìn thấy
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            // Chọn collection volume phù hợp (VOLUME_EXTERNAL_PRIMARY cho API 29+)
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            // 4. Chèn bản ghi trống vào MediaStore
            val targetUri = contentResolver.insert(collectionUri, contentValues)
                ?: throw IOException("ContentResolver không thể tạo bản ghi MediaStore cho $newFileName")
            createdTargetUri = targetUri

            // 5. Đọc stream từ sourceUri và ghi sang targetUri
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: throw IOException("Không thể mở InputStream từ ảnh nguồn: $sourceUri")

            inputStream.use { input ->
                // Mở đích bên trong use của nguồn: nếu mở đích lỗi, nguồn vẫn được đóng.
                val outputStream = contentResolver.openOutputStream(targetUri)
                    ?: throw IOException("Không thể mở OutputStream tới ảnh đích: $targetUri")
                outputStream.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        // copyTo là blocking và không tự kiểm tra coroutine cancellation.
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        currentCoroutineContext().ensureActive()
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            // 6. Ghi thành công -> Gỡ bỏ IS_PENDING (đặt lại bằng 0) để hiển thị trong Gallery
            val completedValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            currentCoroutineContext().ensureActive()
            if (contentResolver.update(targetUri, completedValues, null, null) != 1) {
                throw IOException("Không thể hoàn tất bản ghi MediaStore")
            }

            Result.success(targetUri)
        } catch (e: Exception) {
            // 7. Nếu có lỗi hoặc coroutine bị huỷ (cancellation): dọn dẹp entry pending chưa hoàn thành
            createdTargetUri?.let { uriToDelete ->
                withContext(NonCancellable) {
                    try {
                        contentResolver.delete(uriToDelete, null, null)
                    } catch (cleanupError: Exception) {
                        e.addSuppressed(cleanupError)
                    }
                }
            }

            // QUAN TRỌNG: Không bao giờ nuốt CancellationException!
            if (e is CancellationException) {
                throw e
            }

            Result.failure(e)
        }
    }
}
