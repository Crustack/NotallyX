package com.philkes.notallyx.core.platform

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopPlatformTest {

    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun testDesktopFileStorageOperations() = runTest {
        val storage = DesktopFileStorage()
        val tempDir = temporaryFolder.newFolder("storage_test")
        val dirUri = PlatformUri.fromFile(tempDir)

        val childUri = storage.createChildFile(dirUri, "text/plain", "test_file", "txt")
        assertNotNull(childUri)
        val nonNullUri = childUri!!
        assertTrue(storage.exists(nonNullUri))
        assertEquals("test_file.txt", storage.getFileName(nonNullUri))
        assertEquals("text/plain", storage.getMimeType(nonNullUri))

        val output = storage.openOutputStream(nonNullUri)
        assertNotNull(output)
        output!!.use { it.write("Hello Desktop".toByteArray()) }

        val input = storage.openInputStream(nonNullUri)
        assertNotNull(input)
        val content = input!!.use { it.bufferedReader().readText() }
        assertEquals("Hello Desktop", content)

        val children = storage.listChildren(dirUri)
        assertEquals(1, children.size)

        val destFile = File(tempDir, "copy_dest.txt")
        val copySuccess = storage.copyToInternalFile(nonNullUri, destFile)
        assertTrue(copySuccess)
        assertTrue(destFile.exists())
        assertEquals("Hello Desktop", destFile.readText())

        val deleted = storage.delete(nonNullUri)
        assertTrue(deleted)
        assertFalse(storage.exists(nonNullUri))
    }

    @Test
    fun testDesktopLogger() {
        val logger = DesktopLogger()
        logger.d("TAG", "Debug message")
        logger.i("TAG", "Info message")
        logger.w("TAG", "Warn message", RuntimeException("Warn cause"))
        logger.e("TAG", "Error message", RuntimeException("Error cause"))
    }
}
