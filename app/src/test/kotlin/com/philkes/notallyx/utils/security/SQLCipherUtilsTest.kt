package com.philkes.notallyx.utils.security

import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class SQLCipherUtilsTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private fun loadDatabaseResource(resourcePath: String): File {
        val cleanPath = resourcePath.removePrefix("/")
        val url =
            javaClass.classLoader?.getResource(cleanPath)
                ?: javaClass.classLoader?.getResource("database/$cleanPath")
                ?: javaClass.classLoader?.getResource("databases/$cleanPath")
                ?: throw IllegalArgumentException("Test database resource not found: $resourcePath")
        val subFolder = tempFolder.newFolder()
        val tempFile = File(subFolder, File(cleanPath).name)
        url.openStream().use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        return tempFile
    }

    @Test
    fun getDatabaseState_nonExistentFile_returnsDoesNotExist() {
        val nonExistent = File(tempFolder.root, "non_existent.db")
        assertEquals(
            SQLCipherUtils.State.DOES_NOT_EXIST,
            SQLCipherUtils.getDatabaseState(null, nonExistent),
        )
        assertFalse(nonExistent.isEncryptedDatabase(null))
        assertFalse(nonExistent.isUnencryptedDatabase(null))
    }

    @Test
    fun getDatabaseState_nullFile_returnsDoesNotExist() {
        assertEquals(
            SQLCipherUtils.State.DOES_NOT_EXIST,
            SQLCipherUtils.getDatabaseState(null, null as File?),
        )
    }

    @Test
    fun getDatabaseState_emptyFile_returnsDoesNotExist() {
        val emptyFile = tempFolder.newFile("empty.db")
        assertEquals(
            SQLCipherUtils.State.DOES_NOT_EXIST,
            SQLCipherUtils.getDatabaseState(null, emptyFile),
        )
        assertFalse(emptyFile.isEncryptedDatabase(null))
        assertFalse(emptyFile.isUnencryptedDatabase(null))
    }

    @Test
    fun getDatabaseState_unencryptedDatabase_returnsUnencrypted() {
        val unencryptedFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")

        assertEquals(
            SQLCipherUtils.State.UNENCRYPTED,
            SQLCipherUtils.getDatabaseState(null, unencryptedFile),
        )
        assertTrue(unencryptedFile.isUnencryptedDatabase(null))
        assertFalse(unencryptedFile.isEncryptedDatabase(null))
    }

    @Test
    fun getDatabaseState_encryptedDatabase_returnsEncrypted() {
        val encryptedFile = loadDatabaseResource("database/encrypted/NotallyDatabase")

        assertEquals(
            SQLCipherUtils.State.ENCRYPTED,
            SQLCipherUtils.getDatabaseState(null, encryptedFile),
        )
        assertTrue(encryptedFile.isEncryptedDatabase(null))
        assertFalse(encryptedFile.isUnencryptedDatabase(null))
    }

    @Test
    fun getDatabaseState_truncatedFileLessThan16Bytes_returnsDoesNotExist() {
        val singleByteFile = tempFolder.newFile("single_byte.db")
        FileOutputStream(singleByteFile).use { fos -> fos.write(byteArrayOf(0x53)) }

        assertEquals(
            SQLCipherUtils.State.DOES_NOT_EXIST,
            SQLCipherUtils.getDatabaseState(null, singleByteFile),
        )
    }

    @Test
    fun getDatabaseState_mismatched16BytesHeader_returnsEncrypted() {
        val mismatchedHeaderFile = tempFolder.newFile("mismatched.db")
        FileOutputStream(mismatchedHeaderFile).use { fos ->
            // 16 bytes but ends with \n instead of \0
            fos.write("foo bar test sql encrypted\n".toByteArray(StandardCharsets.US_ASCII))
        }

        assertEquals(
            SQLCipherUtils.State.ENCRYPTED,
            SQLCipherUtils.getDatabaseState(null, mismatchedHeaderFile),
        )
    }

    @Test
    fun getDatabaseState_withContextAndDbName_delegatesCorrectly() {
        val context = ApplicationProvider.getApplicationContext<ContextWrapper>()
        val dbName = "context_test.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        assertEquals(
            SQLCipherUtils.State.DOES_NOT_EXIST,
            SQLCipherUtils.getDatabaseState(context, dbName),
        )

        val unencryptedResourceFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")
        unencryptedResourceFile.copyTo(dbFile, overwrite = true)

        assertEquals(
            SQLCipherUtils.State.UNENCRYPTED,
            SQLCipherUtils.getDatabaseState(context, dbName),
        )

        val encryptedResourceFile = loadDatabaseResource("database/encrypted/NotallyDatabase")
        encryptedResourceFile.copyTo(dbFile, overwrite = true)

        assertEquals(
            SQLCipherUtils.State.ENCRYPTED,
            SQLCipherUtils.getDatabaseState(context, dbName),
        )

        dbFile.delete()
    }
}
