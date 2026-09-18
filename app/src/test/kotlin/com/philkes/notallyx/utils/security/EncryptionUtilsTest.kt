package com.philkes.notallyx.utils.security

import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.charset.StandardCharsets
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
class EncryptionUtilsTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var context: ContextWrapper

    private fun loadDatabaseResource(resourcePath: String): File {
        val cleanPath = resourcePath.removePrefix("/")
        val url =
            javaClass.classLoader?.getResource(cleanPath)
                ?: throw IllegalArgumentException("Test database resource not found: $resourcePath")
        val subFolder = tempFolder.newFolder()
        val tempFile = File(subFolder, File(cleanPath).name)
        url.openStream().use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        return tempFile
    }

    @Test
    fun encryptDatabase_unencryptedDatabase_becomesEncrypted() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")
        assertTrue(dbFile.isUnencryptedDatabase(context))

        encryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))

        assertTrue(dbFile.isEncryptedDatabase(context))
        assertFalse(dbFile.isUnencryptedDatabase(context))
    }

    @Test
    fun decryptDatabase_encryptedDatabase_becomesUnencrypted() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = loadDatabaseResource("database/encrypted/NotallyDatabase")
        assertTrue(dbFile.isEncryptedDatabase(context))

        decryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))

        assertTrue(dbFile.isUnencryptedDatabase(context))
        assertFalse(dbFile.isEncryptedDatabase(context))
    }

    @Test
    fun encryptThenDecryptDatabase_roundTrip_returnsToUnencrypted() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")
        assertTrue(dbFile.isUnencryptedDatabase(context))

        encryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertTrue(dbFile.isEncryptedDatabase(context))

        decryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertTrue(dbFile.isUnencryptedDatabase(context))
    }
}
