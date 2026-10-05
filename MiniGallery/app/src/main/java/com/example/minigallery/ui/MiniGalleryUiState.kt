package com.example.minigallery.ui

import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.MediaImage
import com.example.minigallery.model.PermissionState
import com.example.minigallery.model.SortOrder

/**
 * State duy nhất cho màn hình MiniGallery tuân thủ mô hình UDF (Unidirectional Data Flow).
 */
data class MiniGalleryUiState(
    val allImages: List<MediaImage> = emptyList(),
    val displayedImages: List<MediaImage> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val isLoading: Boolean = false,
    val permissionState: PermissionState = PermissionState.Denied,
    val selectedImportList: List<ImportItem> = emptyList(),
    val isImporting: Boolean = false,
    val importProgressText: String? = null,
    val userMessage: String? = null
) {
    /**
     * Tổng số ảnh đang có quyền truy cập.
     */
    val totalCount: Int get() = allImages.size

    /**
     * Số ảnh hiện đang hiển thị sau khi lọc tìm kiếm.
     */
    val displayedCount: Int get() = displayedImages.size
}
