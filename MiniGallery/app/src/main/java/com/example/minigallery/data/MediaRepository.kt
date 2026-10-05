package com.example.minigallery.data

import android.net.Uri
import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.MediaImage
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository quản lý truy vấn MediaStore, quan sát thay đổi,
 * và lưu trữ bản sao ảnh an toàn với Scoped Storage.
 */
interface MediaRepository {

    /**
     * Luồng Flow quan sát sự thay đổi dữ liệu ảnh trên thiết bị qua ContentObserver.
     * Tự động unregister khi kết thúc hoặc huỷ bỏ.
     */
    fun observeImages(): Flow<List<MediaImage>>

    /**
     * Truy vấn trực tiếp danh sách ảnh hiện có từ MediaStore trên Dispatchers.IO.
     */
    suspend fun queryImages(): List<MediaImage>

    /**
     * Lấy thông tin metadata cơ bản (tên, kích thước) của một Uri được chọn từ Photo Picker.
     */
    suspend fun getMediaInfo(uri: Uri): ImportItem

    /**
     * Sao chép một ảnh từ sourceUri vào thư mục Pictures/MiniGallery của thiết bị.
     * Sử dụng IS_PENDING = 1 khi đang ghi và dọn dẹp nếu thất bại/bị huỷ.
     */
    suspend fun saveImageCopy(sourceUri: Uri): Result<Uri>
}
