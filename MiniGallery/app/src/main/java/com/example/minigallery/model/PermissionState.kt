package com.example.minigallery.model

/**
 * Biểu diễn trạng thái cấp quyền đọc ảnh trên các phiên bản Android (API 29-34+).
 *
 * - [Denied]: Chưa được cấp quyền truy cập bộ nhớ ảnh.
 * - [GrantedPartial]: Chỉ được cấp quyền truy cập một phần ảnh (Android 14+ với READ_MEDIA_VISUAL_USER_SELECTED).
 * - [GrantedFull]: Được cấp toàn quyền đọc ảnh (READ_MEDIA_IMAGES hoặc READ_EXTERNAL_STORAGE).
 */
sealed interface PermissionState {
    data object Denied : PermissionState

    data class GrantedPartial(
        val selectedCount: Int = 0
    ) : PermissionState

    data object GrantedFull : PermissionState
}
