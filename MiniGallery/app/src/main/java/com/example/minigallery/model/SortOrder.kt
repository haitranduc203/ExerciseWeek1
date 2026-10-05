package com.example.minigallery.model

/**
 * Các chế độ sắp xếp danh sách ảnh.
 * Dùng để áp dụng Kotlin Collections extensions: sortedBy, sortedByDescending.
 */
enum class SortOrder(val label: String) {
    DATE_DESC("Mới nhất trước"),
    DATE_ASC("Cũ nhất trước"),
    NAME_ASC("Tên (A → Z)"),
    NAME_DESC("Tên (Z → A)")
}
