package com.example.minigallery

import android.net.Uri
import com.example.minigallery.data.MediaRepository
import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.ImportStatus
import com.example.minigallery.model.MediaImage
import com.example.minigallery.model.PermissionState
import com.example.minigallery.model.SortOrder
import com.example.minigallery.ui.MiniGalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Fake Repository hỗ trợ kiểm thử đơn vị độc lập và nhanh chóng cho ViewModel.
 */
class FakeMediaRepository : MediaRepository {
    val imagesFlow = MutableSharedFlow<List<MediaImage>>(replay = 1)
    var shouldFailSave = false
    var shouldFailMetadata = false
    var cancelSave = false
    var observeCount = 0
    val savedCopies = mutableListOf<Uri>()

    override fun observeImages(): Flow<List<MediaImage>> {
        observeCount++
        return imagesFlow
    }

    override suspend fun queryImages(): List<MediaImage> {
        return imagesFlow.replayCache.firstOrNull() ?: emptyList()
    }

    override suspend fun getMediaInfo(uri: Uri): ImportItem {
        if (shouldFailMetadata) throw SecurityException("Quyền ảnh đã bị thu hồi")
        return ImportItem(
            sourceUri = uri,
            displayName = "mock_${uri.lastPathSegment}.jpg",
            sizeBytes = 1024L * 1024L,
            status = ImportStatus.Pending
        )
    }

    override suspend fun saveImageCopy(sourceUri: Uri): Result<Uri> {
        if (cancelSave) throw kotlinx.coroutines.CancellationException("Import cancelled")
        return if (shouldFailSave) {
            Result.failure(Exception("Lỗi mô phỏng ghi stream"))
        } else {
            val target = mock<Uri>()
            savedCopies.add(target)
            Result.success(target)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MiniGalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMediaRepository
    private lateinit var viewModel: MiniGalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMediaRepository()
        viewModel = MiniGalleryViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `chon hon 10 anh chi lay toi da 10 anh`() = runTest {
        // Giả lập người dùng chọn 15 ảnh từ Photo Picker
        val mockUris = List(15) { mock<Uri>() }

        viewModel.onPhotosSelected(mockUris)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Số lượng ảnh được chọn để import phải bị giới hạn đúng 10", 10, state.selectedImportList.size)
        assertTrue(state.userMessage?.contains("10 ảnh") == true)
    }

    @Test
    fun `tim kiem theo ten loc dung ket qua theo Collections filter`() = runTest {
        val sampleImages = listOf(
            MediaImage(1L, mock<Uri>(), "beach_holiday.jpg", "image/jpeg", 2048L, 1000L),
            MediaImage(2L, mock<Uri>(), "mountain_trip.jpg", "image/jpeg", 4096L, 2000L),
            MediaImage(3L, mock<Uri>(), "family_beach.png", "image/png", 1024L, 3000L)
        )

        viewModel.updatePermissionState(PermissionState.GrantedFull)
        fakeRepository.imagesFlow.emit(sampleImages)
        advanceUntilIdle()

        // Tìm từ khóa "beach"
        viewModel.setSearchQuery("beach")
        val state = viewModel.uiState.value

        assertEquals(3, state.totalCount)
        assertEquals(2, state.displayedCount)
        assertTrue(state.displayedImages.all { it.displayName.contains("beach", ignoreCase = true) })
    }

    @Test
    fun `sap xep dung thu tu theo Collections sortedBy va sortedByDescending`() = runTest {
        val sampleImages = listOf(
            MediaImage(1L, mock<Uri>(), "b_photo.jpg", "image/jpeg", 2048L, 100L),
            MediaImage(2L, mock<Uri>(), "a_photo.jpg", "image/jpeg", 4096L, 300L),
            MediaImage(3L, mock<Uri>(), "c_photo.jpg", "image/png", 1024L, 200L)
        )

        viewModel.updatePermissionState(PermissionState.GrantedFull)
        fakeRepository.imagesFlow.emit(sampleImages)
        advanceUntilIdle()

        // Sắp xếp ngày mới nhất trước (DATE_DESC)
        viewModel.setSortOrder(SortOrder.DATE_DESC)
        assertEquals("a_photo.jpg", viewModel.uiState.value.displayedImages[0].displayName)
        assertEquals("c_photo.jpg", viewModel.uiState.value.displayedImages[1].displayName)
        assertEquals("b_photo.jpg", viewModel.uiState.value.displayedImages[2].displayName)

        // Sắp xếp tên A-Z (NAME_ASC)
        viewModel.setSortOrder(SortOrder.NAME_ASC)
        assertEquals("a_photo.jpg", viewModel.uiState.value.displayedImages[0].displayName)
        assertEquals("b_photo.jpg", viewModel.uiState.value.displayedImages[1].displayName)
        assertEquals("c_photo.jpg", viewModel.uiState.value.displayedImages[2].displayName)

        // Sắp xếp tên Z-A (NAME_DESC)
        viewModel.setSortOrder(SortOrder.NAME_DESC)
        assertEquals("c_photo.jpg", viewModel.uiState.value.displayedImages[0].displayName)
        assertEquals("b_photo.jpg", viewModel.uiState.value.displayedImages[1].displayName)
        assertEquals("a_photo.jpg", viewModel.uiState.value.displayedImages[2].displayName)
    }

    @Test
    fun `trang thai import ghi nhan tien do tung anh va ket qua thanh cong that bai`() = runTest {
        val mockUris = List(3) { mock<Uri>() }
        viewModel.onPhotosSelected(mockUris)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertEquals(3, viewModel.uiState.value.selectedImportList.size)

        // Bắt đầu lưu bản sao
        viewModel.startSavingCopies()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isImporting)
        assertTrue("Tất cả 3 ảnh phải thành công", state.selectedImportList.all { it.status is ImportStatus.Success })
        assertTrue(state.importProgressText?.contains("3 thành công") == true)
    }

    @Test
    fun `khi bi tu choi quyen thi xoa danh sach anh va ve trang thai Denied`() = runTest {
        val sampleImages = listOf(
            MediaImage(1L, mock<Uri>(), "photo.jpg", "image/jpeg", 2048L, 100L)
        )
        viewModel.updatePermissionState(PermissionState.GrantedFull)
        fakeRepository.imagesFlow.emit(sampleImages)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.displayedImages.size)

        // Người dùng thu hồi quyền
        viewModel.updatePermissionState(PermissionState.Denied)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PermissionState.Denied, state.permissionState)
        assertTrue(state.displayedImages.isEmpty())
        assertTrue(state.allImages.isEmpty())
    }

    @Test
    fun `bam luu lien tiep chi tao mot ban sao`() = runTest {
        viewModel.onPhotosSelected(listOf(mock<Uri>()))
        advanceUntilIdle()
        viewModel.startSavingCopies()
        viewModel.startSavingCopies()
        advanceUntilIdle()
        assertEquals(1, fakeRepository.savedCopies.size)
    }

    @Test
    fun `batch da hoan tat khong duoc luu lai`() = runTest {
        viewModel.onPhotosSelected(listOf(mock<Uri>()))
        advanceUntilIdle()
        viewModel.startSavingCopies()
        advanceUntilIdle()
        viewModel.startSavingCopies()
        advanceUntilIdle()
        assertEquals(1, fakeRepository.savedCopies.size)
    }

    @Test
    fun `loi metadata dung loading va thong bao cho nguoi dung`() = runTest {
        fakeRepository.shouldFailMetadata = true
        viewModel.onPhotosSelected(listOf(mock<Uri>()))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Quyền ảnh") == true)
    }

    @Test
    fun `huy import khong de giao dien bi khoa`() = runTest {
        viewModel.onPhotosSelected(listOf(mock<Uri>()))
        advanceUntilIdle()
        fakeRepository.cancelSave = true
        viewModel.startSavingCopies()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isImporting)
        assertTrue(viewModel.uiState.value.selectedImportList.none { it.status is ImportStatus.InProgress })
    }

    @Test
    fun `quay lai voi cung quyen van tai lai tap anh duoc phep`() = runTest {
        viewModel.updatePermissionState(PermissionState.GrantedFull)
        advanceUntilIdle()
        viewModel.updatePermissionState(PermissionState.GrantedFull)
        advanceUntilIdle()
        assertEquals(2, fakeRepository.observeCount)
        viewModel.updatePermissionState(PermissionState.Denied)
    }

    @Test
    fun `loi luu duoc hien thi cho tung anh va ket thuc batch`() = runTest {
        fakeRepository.shouldFailSave = true
        viewModel.onPhotosSelected(List(2) { mock<Uri>() })
        advanceUntilIdle()
        viewModel.startSavingCopies()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isImporting)
        assertTrue(viewModel.uiState.value.selectedImportList.all { it.status is ImportStatus.Error })
        assertTrue(viewModel.uiState.value.importProgressText?.contains("2 thất bại") == true)
    }
}
