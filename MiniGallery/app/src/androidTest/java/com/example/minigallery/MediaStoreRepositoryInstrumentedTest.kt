package com.example.minigallery

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.content.ContentResolver
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minigallery.data.MediaStoreRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Tests use app-owned temporary MediaStore rows and never delete the user's images. */
@RunWith(AndroidJUnit4::class)
class MediaStoreRepositoryInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val repository = MediaStoreRepositoryImpl(resolver, context)
    private val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val cleanup = mutableListOf<Uri>()
    private val tokens = mutableListOf<String>()

    private fun source(large: Boolean = false): Pair<Uri, String> {
        val token = "test_${UUID.randomUUID().toString().replace("-", "")}"
        tokens.add(token)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$token.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiniGalleryTests")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values))
        cleanup.add(uri)
        resolver.openOutputStream(uri)!!.use { output ->
            val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
            try { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            finally { bitmap.recycle() }
            if (large) {
                // A large stream makes cancellation observable while the target is pending.
                val block = ByteArray(1024 * 1024)
                repeat(64) { output.write(block) }
            }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        return uri to token
    }

    private fun targets(token: String): List<Uri> {
        val found = mutableListOf<Uri>()
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID), Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("MiniGallery_%_${token}.png"))
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
        }, null
        )!!.use { cursor ->
            while (cursor.moveToNext()) found.add(ContentUris.withAppendedId(collection, cursor.getLong(0)))
        }
        return found
    }

    @After
    fun cleanUp() {
        tokens.forEach { cleanup.addAll(targets(it)) }
        cleanup.distinct().forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun copyPreservesSourceBytesAndPublishesTarget() = runBlocking {
        val (source, _) = source()
        val target = repository.saveImageCopy(source).getOrThrow()
        cleanup.add(target)
        assertNotEquals(source, target)
        val originalBytes = resolver.openInputStream(source)!!.use { it.readBytes() }
        val copiedBytes = resolver.openInputStream(target)!!.use { it.readBytes() }
        assertArrayEquals(originalBytes, copiedBytes)
        resolver.query(target, arrayOf(MediaStore.Images.Media.IS_PENDING,
            MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.MIME_TYPE), null, null, null
        )!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals("Pictures/MiniGallery/", cursor.getString(1))
            assertEquals("image/png", cursor.getString(2))
        }
        assertTrue(repository.queryImages().any { it.uri.lastPathSegment == target.lastPathSegment })
    }

    @Test
    fun deletedSourceFailsWithoutLeavingPendingRow() = runBlocking {
        val (source, token) = source()
        resolver.delete(source, null, null)
        cleanup.remove(source)
        assertTrue(repository.saveImageCopy(source).isFailure)
        assertTrue(targets(token).isEmpty())
    }

    @Test
    fun cancellationDuringCopyRemovesUnfinishedTarget() = runBlocking {
        val (source, token) = source(large = true)
        val job = launch(Dispatchers.IO) { repository.saveImageCopy(source) }
        withTimeout(10_000) {
            while (targets(token).isEmpty() && job.isActive) delay(1)
        }
        assertTrue("The copy must still be running when cancellation is requested", job.isActive)
        job.cancelAndJoin()
        assertTrue("Cancelled copy must not publish or leave a pending image", targets(token).isEmpty())
        assertNotNull(resolver.openInputStream(source)?.use { it.read() })
    }
}
