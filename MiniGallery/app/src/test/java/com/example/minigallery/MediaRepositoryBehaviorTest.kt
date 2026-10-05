package com.example.minigallery

import android.net.Uri
import com.example.minigallery.model.MediaImage
import com.example.minigallery.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class MediaRepositoryBehaviorTest {

    @Test
    fun `MediaImage formatSize hien thi dung don vi dung luong`() {
        val uri = mock<Uri>()
        val imgBytes = MediaImage(1L, uri, "test.jpg", "image/jpeg", 500L, 0L)
        assertEquals("500.0 B", imgBytes.formattedSize)

        val imgKb = MediaImage(2L, uri, "test.jpg", "image/jpeg", 1024L * 50L, 0L)
        assertEquals("50.0 KB", imgKb.formattedSize)

        val imgMb = MediaImage(3L, uri, "test.jpg", "image/jpeg", 1024L * 1024L * 3L + (500L * 1024L), 0L)
        assertEquals("3.5 MB", imgMb.formattedSize)
    }

    @Test
    fun `MediaImage formatDate tra ve chuoi hop le voi timestamp duong`() {
        val uri = mock<Uri>()
        val imgWithDate = MediaImage(1L, uri, "test.jpg", "image/jpeg", 1000L, 1700000000L)
        assertNotEquals("Không xác định", imgWithDate.formattedDate)
        assertTrue(imgWithDate.formattedDate.contains("/"))

        val imgZeroDate = MediaImage(2L, uri, "test.jpg", "image/jpeg", 1000L, 0L)
        assertEquals("Không xác định", imgZeroDate.formattedDate)
    }

    @Test
    fun `SortOrder cung cap nhan tieng Viet ro rang`() {
        assertEquals("Mới nhất trước", SortOrder.DATE_DESC.label)
        assertEquals("Cũ nhất trước", SortOrder.DATE_ASC.label)
        assertEquals("Tên (A → Z)", SortOrder.NAME_ASC.label)
        assertEquals("Tên (Z → A)", SortOrder.NAME_DESC.label)
    }
}
