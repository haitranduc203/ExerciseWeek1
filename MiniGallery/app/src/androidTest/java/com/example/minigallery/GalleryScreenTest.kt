package com.example.minigallery

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.minigallery.model.*
import com.example.minigallery.theme.MiniGalleryTheme
import com.example.minigallery.ui.MiniGalleryUiState
import com.example.minigallery.ui.screen.GalleryScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GalleryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedPermissionStillAllowsPicker() {
        var permissionRequests = 0
        var pickerRequests = 0
        compose.setContent {
            MiniGalleryTheme {
                GalleryScreen(MiniGalleryUiState(), {}, {}, { permissionRequests++ }, { pickerRequests++ }, {}, {})
            }
        }
        compose.onNodeWithText("Cấp quyền đọc ảnh").performClick()
        compose.onNodeWithContentDescription("Thêm ảnh").performClick()
        assertEquals(1, permissionRequests)
        assertEquals(1, pickerRequests)
    }

    @Test fun searchPublishesInputToState() {
        val state = mutableStateOf(MiniGalleryUiState())
        compose.setContent {
            MiniGalleryTheme {
                GalleryScreen(state.value, { state.value = state.value.copy(searchQuery = it) }, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("search").performTextInput("demo")
        compose.onNodeWithTag("search").assertTextContains("demo")
    }

    @Test fun completedBatchCannotBeSavedAgainAndCanBeClosed() {
        val state = mutableStateOf(MiniGalleryUiState(
            selectedImportList = listOf(ImportItem(Uri.parse("content://sample/1"), "demo.png", 1024,
                ImportStatus.Success(Uri.parse("content://target/2")))),
            importProgressText = "Hoàn tất: 1 thành công, 0 thất bại."
        ))
        compose.setContent {
            MiniGalleryTheme {
                GalleryScreen(state.value, {}, {}, {}, {}, {}, { state.value = state.value.copy(selectedImportList = emptyList()) })
            }
        }
        compose.onNodeWithText("Lưu bản sao").assertIsNotEnabled()
        compose.onNodeWithText("Hoàn tất: 1 thành công, 0 thất bại.").assertIsDisplayed()
        compose.onNodeWithText("Đóng").performClick()
        compose.onNodeWithText("Xem trước và lưu bản sao").assertDoesNotExist()
    }

    @Test fun photoClickShowsMetadata() {
        val photo = MediaImage(1, Uri.parse("content://sample/1"), "demo.png", "image/png", 1024, 1700000000)
        compose.setContent {
            MiniGalleryTheme {
                GalleryScreen(MiniGalleryUiState(allImages = listOf(photo), displayedImages = listOf(photo),
                    permissionState = PermissionState.GrantedFull), {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("photo_1").performClick()
        compose.onNodeWithText("Chi tiết ảnh").assertIsDisplayed()
        compose.onNodeWithText("image/png").assertExists()
    }
}
