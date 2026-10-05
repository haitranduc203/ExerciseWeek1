package com.example.minigallery.model

import android.net.Uri

/**
 * Trạng thái xử lý của từng ảnh khi người dùng chọn từ Photo Picker để lưu vào thư viện.
 */
sealed interface ImportStatus {
    data object Pending : ImportStatus
    data object InProgress : ImportStatus
    data class Success(val targetUri: Uri) : ImportStatus
    data class Error(val message: String) : ImportStatus
}

/**
 * Mục dữ liệu hiển thị trong màn hình xem trước và theo dõi tiến độ lưu bản sao.
 */
data class ImportItem(
    val sourceUri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val status: ImportStatus = ImportStatus.Pending
)
