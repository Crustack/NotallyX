package com.philkes.notallyx.utils.security

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import java.io.File
import java.nio.charset.StandardCharsets
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptionUtilsTest {

    private lateinit var context: ContextWrapper

    @Before
    fun setUp() {
        context =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as ContextWrapper
        System.loadLibrary("sqlcipher")
    }

    @After fun tearDown() {}

    @get:Rule val tempFolder = TemporaryFolder()

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
        val dbFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")
        assertValidDatabase(dbFile, null)

        encryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))

        assertValidDatabase(dbFile, "foo")
    }

    @Test
    fun decryptDatabase_encryptedDatabase_becomesUnencrypted() {
        val dbFile = loadDatabaseResource("database/encrypted/NotallyDatabase")
        assertValidDatabase(dbFile, "foo")

        decryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))

        assertValidDatabase(dbFile, null)
    }

    @Test
    fun encryptThenDecryptDatabase_roundTrip_returnsToUnencrypted() {
        val dbFile = loadDatabaseResource("database/unencrypted/NotallyDatabase")
        assertValidDatabase(dbFile, null)

        encryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertValidDatabase(dbFile, "foo")

        decryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertValidDatabase(dbFile, null)
    }

    @Test
    fun decryptThenEncryptDatabase_roundTrip_returnsToEncrypted() {
        val dbFile = loadDatabaseResource("database/encrypted/NotallyDatabase")
        assertValidDatabase(dbFile, "foo")

        decryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertValidDatabase(dbFile, null)

        encryptDatabase(context, dbFile, "foo".toByteArray(StandardCharsets.UTF_8))
        assertValidDatabase(dbFile, "foo")
    }

    private fun assertValidDatabase(dbFile: File, passphrase: String?) {
        if (passphrase != null) Assert.assertTrue(dbFile.isEncryptedDatabase(context))
        else Assert.assertTrue(dbFile.isUnencryptedDatabase(context))
        val database =
            SQLiteDatabase.openDatabase(
                dbFile.path,
                (passphrase ?: "").toByteArray(StandardCharsets.UTF_8),
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                null,
            )
        Assert.assertTrue(database.isOpen)
        Assert.assertNotNull(database.version)
        val integrityCheck =
            database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                buildList {
                        do {
                            add(cursor.getString(0))
                        } while (cursor.moveToNext())
                    }
                    .joinToString("; ")
            }
        Assert.assertEquals("ok", integrityCheck)
        val title =
            database.rawQuery("SELECT title FROM BaseNote", null).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(cursor.getColumnIndex("title"))
            }
        Assert.assertEquals("Test", title)
        database.close()
    }
}
