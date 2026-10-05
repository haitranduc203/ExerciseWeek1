package com.example.minigallery.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.minigallery.R
import com.example.minigallery.model.ImportItem
import com.example.minigallery.model.ImportStatus
import com.example.minigallery.model.MediaImage
import com.example.minigallery.theme.GalleryDimens
import com.example.minigallery.ui.MiniGalleryUiState

/** Scrollable body and fixed actions fit both portrait and landscape. */
@Composable
private fun GalleryDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    val maximumHeight = LocalConfiguration.current.screenHeightDp.dp * 0.86f
    Dialog(onDismissRequest = { if (dismissEnabled) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = dismissEnabled, dismissOnClickOutside = dismissEnabled)) {
        Surface(modifier = Modifier.widthIn(max = GalleryDimens.dialogWidth).fillMaxWidth(0.94f).heightIn(max = maximumHeight),
            shape = MaterialTheme.shapes.extraLarge, tonalElevation = GalleryDimens.gap) {
            Column(Modifier.padding(GalleryDimens.gutter), verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                body()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GalleryDimens.gap, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

@Composable
internal fun ImportDialog(state: MiniGalleryUiState, onSave: () -> Unit, onClose: () -> Unit) {
    GalleryDialog("Xem trước và lưu bản sao", onClose, dismissEnabled = !state.isImporting,
        actions = {
            TextButton(onClick = onClose, enabled = !state.isImporting) { Text("Đóng") }
            Button(onClick = onSave, enabled = !state.isImporting && state.selectedImportList.any { it.status is ImportStatus.Pending }) {
                Text("Lưu bản sao")
            }
        }) {
        Text("${state.selectedImportList.size} ảnh được chọn · Bản sao lưu vào Pictures/MiniGallery",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.isImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.importProgressText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
            items(state.selectedImportList, key = { it.sourceUri.toString() }) { item -> ImportRow(item) }
        }
    }
}

@Composable
private fun ImportRow(item: ImportItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(GalleryDimens.gap),
            horizontalArrangement = Arrangement.spacedBy(GalleryDimens.gap), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.sourceUri, contentDescription = item.displayName,
                modifier = Modifier.size(GalleryDimens.thumbnail), contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_image))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GalleryDimens.gap)) {
                Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(if (item.sizeBytes > 0) "${item.sizeBytes} bytes" else "Kích thước chưa rõ",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when (val status = item.status) {
                    ImportStatus.Pending -> "Chờ lưu bản sao"
                    ImportStatus.InProgress -> "Đang lưu…"
                    is ImportStatus.Success -> "Đã lưu vào thư viện"
                    is ImportStatus.Error -> "Không thể lưu: ${status.message}"
                }, style = MaterialTheme.typography.bodySmall,
                    color = if (item.status is ImportStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
internal fun ImageDetailDialog(photo: MediaImage, onDismiss: () -> Unit) {
    GalleryDialog("Chi tiết ảnh", onDismiss, actions = { TextButton(onClick = onDismiss) { Text("Đóng") } }) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(GalleryDimens.gutter)) {
            item {
                AsyncImage(photo.uri, contentDescription = photo.displayName,
                    modifier = Modifier.fillMaxWidth().height(GalleryDimens.detailImageHeight),
                    contentScale = ContentScale.Fit, error = painterResource(R.drawable.ic_image))
            }
            item { Metadata("Tên ảnh", photo.displayName) }
            item { Metadata("MIME type", photo.mimeType) }
            item { Metadata("Dung lượng", "${photo.formattedSize} (${photo.sizeBytes} bytes)") }
            item { Metadata("Ngày thêm", photo.formattedDate) }
            item { Metadata("Content URI", photo.uri.toString()) }
        }
    }
}

@Composable
private fun Metadata(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}
