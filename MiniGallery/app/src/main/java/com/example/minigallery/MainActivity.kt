package com.example.minigallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minigallery.data.MediaStoreRepositoryImpl
import com.example.minigallery.theme.MiniGalleryTheme
import com.example.minigallery.ui.MiniGalleryViewModel
import com.example.minigallery.ui.screen.GalleryScreen
import com.example.minigallery.util.PermissionHelper
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val gallery: MiniGalleryViewModel by viewModels {
        MiniGalleryViewModel.Factory(MediaStoreRepositoryImpl(contentResolver, applicationContext))
    }
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshPermission()
    }
    private val picker = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) {
        gallery.onPhotosSelected(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniGalleryTheme {
                val state by gallery.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.userMessage) {
                    state.userMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                        gallery.clearUserMessage()
                    }
                }
                GalleryScreen(
                    state = state,
                    onSearch = gallery::setSearchQuery,
                    onSort = gallery::setSortOrder,
                    onPermission = { permissions.launch(PermissionHelper.getRequiredPermissions()) },
                    onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSave = gallery::startSavingCopies,
                    onCloseImport = gallery::dismissImportPreview
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermission()
    }

    private fun refreshPermission() {
        gallery.updatePermissionState(PermissionHelper.checkPermissionState(this, gallery.uiState.value.totalCount))
    }
}
