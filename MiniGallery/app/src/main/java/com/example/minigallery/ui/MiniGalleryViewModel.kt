package com.example.minigallery.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minigallery.data.MediaRepository
import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.ImportStatus
import com.example.minigallery.model.MediaImage
import com.example.minigallery.model.PermissionState
import com.example.minigallery.model.SortOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel cho MiniGallery:
 * - Quản lý UiState thông qua StateFlow.
 * - Sử dụng viewModelScope cho tất cả tác vụ coroutine.
 * - Áp dụng các toán tử Collections: filter, sortedBy, sortedByDescending, map, take...
 * - Không nuốt CancellationException.
 */
class MiniGalleryViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiniGalleryUiState())
    val uiState: StateFlow<MiniGalleryUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    /**
     * Cập nhật trạng thái quyền (đầy đủ, một phần, hoặc từ chối).
     * Khi có quyền đọc, tự động bắt đầu lắng nghe thay đổi từ MediaStore.
     */
    fun updatePermissionState(newState: PermissionState) {
        // Cùng trạng thái quyền vẫn có thể có tập ảnh khác sau khi chọn lại hoặc từ Settings.
        _uiState.update { it.copy(permissionState = newState) }

        when (newState) {
            is PermissionState.GrantedFull,
            is PermissionState.GrantedPartial -> {
                startObservingMedia()
            }
            PermissionState.Denied -> {
                observeJob?.cancel()
                observeJob = null
                _uiState.update {
                    it.copy(
                        allImages = emptyList(),
                        displayedImages = emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Bắt đầu theo dõi MediaStore bằng ContentObserver Flow từ Repository.
     */
    private fun startObservingMedia() {
        val previousJob = observeJob
        previousJob?.cancel()
        observeJob = viewModelScope.launch {
            previousJob?.join()
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.observeImages().collect { images ->
                    _uiState.update { current ->
                        val filteredAndSorted = applyFilterAndSort(
                            list = images,
                            query = current.searchQuery,
                            order = current.sortOrder
                        )

                        val updatedPermission = if (current.permissionState is PermissionState.GrantedPartial) {
                            PermissionState.GrantedPartial(selectedCount = images.size)
                        } else {
                            current.permissionState
                        }

                        current.copy(
                            allImages = images,
                            displayedImages = filteredAndSorted,
                            isLoading = false,
                            permissionState = updatedPermission
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Lỗi khi đọc thư viện ảnh: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Tìm kiếm ảnh theo tên (áp dụng Kotlin Collections filter).
     */
    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val updated = applyFilterAndSort(current.allImages, query, current.sortOrder)
            current.copy(searchQuery = query, displayedImages = updated)
        }
    }

    /**
     * Đổi cách sắp xếp (áp dụng Kotlin Collections sortedBy/sortedByDescending).
     */
    fun setSortOrder(order: SortOrder) {
        _uiState.update { current ->
            val updated = applyFilterAndSort(current.allImages, current.searchQuery, order)
            current.copy(sortOrder = order, displayedImages = updated)
        }
    }

    /**
     * Người dùng chọn ảnh từ Photo Picker.
     * Kiểm tra và giới hạn tối đa 10 ảnh cả ở callback.
     */
    fun onPhotosSelected(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isImporting) return

        // Áp dụng take(10) để đảm bảo không vượt quá 10 ảnh
        val cappedUris = uris.take(10)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val items = cappedUris.map { uri -> repository.getMediaInfo(uri) }
                _uiState.update {
                    it.copy(
                        selectedImportList = items,
                        importProgressText = null,
                        userMessage = if (uris.size > 10) "Đã giới hạn chọn 10 ảnh đầu tiên." else null
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(userMessage = "Không thể đọc ảnh đã chọn: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Bắt đầu lưu bản sao các ảnh đã chọn vào thư viện (Pictures/MiniGallery).
     * Chặn bấm nhiều lần khi đang xử lý (isImporting).
     */
    fun startSavingCopies() {
        if (_uiState.value.isImporting) return
        val itemsToSave = _uiState.value.selectedImportList
        if (itemsToSave.isEmpty() || itemsToSave.none { it.status is ImportStatus.Pending }) return

        // Đặt cờ ngay trước khi launch để hai lần bấm trong cùng lượt main thread không tạo hai job.
        _uiState.update { it.copy(isImporting = true) }

        viewModelScope.launch {
            val workingList = itemsToSave.toMutableList()
            var successCount = 0
            var failCount = 0

            try {
                for (index in workingList.indices) {
                    val currentItem = workingList[index]
                    if (currentItem.status !is ImportStatus.Pending) continue
                    workingList[index] = currentItem.copy(status = ImportStatus.InProgress)
                    _uiState.update {
                        it.copy(
                            selectedImportList = workingList.toList(),
                            importProgressText = "Đang lưu ảnh ${index + 1}/${workingList.size}…"
                        )
                    }

                    val result = try {
                        repository.saveImageCopy(currentItem.sourceUri)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Result.failure(e)
                    }

                    if (result.isSuccess) {
                        successCount++
                        workingList[index] = currentItem.copy(
                            status = ImportStatus.Success(result.getOrThrow())
                        )
                    } else {
                        failCount++
                        val errorReason = result.exceptionOrNull()?.localizedMessage ?: "Lỗi ghi stream"
                        workingList[index] = currentItem.copy(
                            status = ImportStatus.Error(errorReason)
                        )
                    }

                    _uiState.update {
                        it.copy(selectedImportList = workingList.toList())
                    }
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgressText = "Hoàn tất: $successCount thành công, $failCount thất bại."
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { state ->
                    state.copy(
                        selectedImportList = state.selectedImportList.map { item ->
                            if (item.status is ImportStatus.InProgress) {
                                item.copy(status = ImportStatus.Error("Đã hủy lưu ảnh"))
                            } else item
                        },
                        importProgressText = "Đã hủy lưu ảnh."
                    )
                }
                throw e
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    /**
     * Đóng hộp thoại xem trước và xoá danh sách ảnh đã chọn.
     */
    fun dismissImportPreview() {
        if (_uiState.value.isImporting) return
        _uiState.update {
            it.copy(
                selectedImportList = emptyList(),
                importProgressText = null
            )
        }
    }

    /**
     * Xóa thông báo tạm thời sau khi hiển thị.
     */
    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    /**
     * Hàm thuần túy kết hợp filter và sortedBy/sortedByDescending trên Kotlin Collections.
     */
    private fun applyFilterAndSort(
        list: List<MediaImage>,
        query: String,
        order: SortOrder
    ): List<MediaImage> {
        val trimmedQuery = query.trim()
        val filtered = if (trimmedQuery.isEmpty()) {
            list
        } else {
            list.filter { it.displayName.contains(trimmedQuery, ignoreCase = true) }
        }

        return when (order) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAddedSeconds }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.dateAddedSeconds }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase(Locale.getDefault()) }
        }
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MiniGalleryViewModel::class.java)) {
                return MiniGalleryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
