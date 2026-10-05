package com.example.minigallery

import android.content.ContentResolver
import android.content.Context
import com.example.minigallery.data.MediaStoreRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MediaStoreQueryTest {
    @Test
    fun `loi quyen khi query khong duoc gia thanh thu vien rong`() = runTest {
        val resolver = mock<ContentResolver>()
        whenever(resolver.query(
            anyOrNull(), anyOrNull(), anyOrNull<String>(),
            anyOrNull<Array<String>>(), anyOrNull<String>()
        )).thenThrow(SecurityException("Permission revoked"))
        val repository = MediaStoreRepositoryImpl(resolver, mock<Context>())
        try {
            repository.queryImages()
            fail("Query error must reach ViewModel so the UI can report it")
        } catch (expected: SecurityException) {
            org.junit.Assert.assertEquals("Permission revoked", expected.message)
        }
    }
}
