package com.example.minigallery.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.minigallery.R
import com.example.minigallery.model.MediaImage
import com.example.minigallery.model.PermissionState
import com.example.minigallery.model.SortOrder
import com.example.minigallery.theme.GalleryDimens
import com.example.minigallery.theme.MiniGalleryTheme
import com.example.minigallery.ui.MiniGalleryUiState

/** State-hoisted UI: no MediaStore query or file I/O occurs during composition. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: MiniGalleryUiState,
    onSearch: (String) -> Unit,
    onSort: (SortOrder) -> Unit,
    onPermission: () -> Unit,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onCloseImport: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("MiniGallery Compose", style = MaterialTheme.typography.titleLarge)
                    Text("Thư viện ảnh trên thiết bị", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        floatingActionButton = {
            if (!state.isImporting) {
                ExtendedFloatingActionButton(onClick = onAdd,
                    modifier = Modifier.semantics { contentDescription = "Thêm ảnh" },
                    text = { Text("Thêm ảnh") },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .padding(horizontal = GalleryDimens.gutter),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
            PermissionBanner(state.permissionState, onPermission)
            OutlinedTextField(value = state.searchQuery, onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth().testTag("search"), singleLine = true,
                label = { Text("Tìm kiếm ảnh theo tên") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Xóa tìm kiếm")
                    }
                })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đang hiển thị ${state.displayedCount} / ${state.totalCount} ảnh",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    TextButton(onClick = { sortExpanded = true }) {
                        Icon(painterResource(R.drawable.ic_sort), contentDescription = null)
                        Spacer(Modifier.width(GalleryDimens.gap))
                        Text(state.sortOrder.label)
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(text = { Text(order.label) }, onClick = {
                                sortExpanded = false
                                onSort(order)
                            })
                        }
                    }
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.displayedImages.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(when {
                        state.isLoading -> "Đang đọc thư viện…"
                        state.permissionState == PermissionState.Denied -> "Cấp quyền để xem thư viện. Bạn vẫn có thể thêm ảnh bằng Photo Picker."
                        state.searchQuery.isNotBlank() -> "Không tìm thấy ảnh khớp với từ khóa."
                        else -> "Thư viện chưa có ảnh."
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(GalleryDimens.minimumTile),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = GalleryDimens.fabClearance),
                    horizontalArrangement = Arrangement.spacedBy(GalleryDimens.gap),
                    verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
                    items(state.displayedImages, key = { it.id }) { photo ->
                        PhotoCard(photo) { selectedId = photo.id }
                    }
                }
            }
        }
    }
    // Persist only an ID across rotation, never a Bitmap or Activity.
    state.allImages.firstOrNull { it.id == selectedId }?.let { photo ->
        ImageDetailDialog(photo) { selectedId = null }
    }
    if (state.selectedImportList.isNotEmpty()) ImportDialog(state, onSave, onCloseImport)
}

@Composable
private fun PermissionBanner(permission: PermissionState, onPermission: () -> Unit) {
    if (permission == PermissionState.GrantedFull) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(GalleryDimens.gutter),
            verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
            Text(if (permission is PermissionState.GrantedPartial) "Quyền truy cập một phần" else "Chưa được cấp quyền đọc ảnh",
                style = MaterialTheme.typography.titleSmall)
            Text(if (permission is PermissionState.GrantedPartial)
                "Chỉ hiển thị ảnh bạn cho phép và ảnh do ứng dụng tạo."
            else "Cho phép đọc ảnh để hiển thị thư viện thiết bị.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onPermission, modifier = Modifier.align(Alignment.End)) {
                Text(if (permission is PermissionState.GrantedPartial) "Chọn thêm ảnh" else "Cấp quyền đọc ảnh")
            }
        }
    }
}

@Composable
private fun PhotoCard(photo: MediaImage, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(1f).testTag("photo_${photo.id}")) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = photo.uri, contentDescription = photo.displayName,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_image), error = painterResource(R.drawable.ic_image))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(GalleryDimens.gap)) {
                Text(photo.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(photo.formattedSize, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniGalleryPreview() {
    MiniGalleryTheme(dynamicColor = false) {
        GalleryScreen(MiniGalleryUiState(), {}, {}, {}, {}, {}, {})
    }
}
