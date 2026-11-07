package com.philkes.notallyx.core.database

import android.app.Application
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.encryptDatabase
import com.philkes.notallyx.core.security.isEncryptedDatabase
import com.philkes.notallyx.core.security.isUnencryptedDatabase
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying that an existing SQLCipher-encrypted v11 database is
 * non-destructively decrypted to plaintext SQLite on startup when
 * [NotallyDatabase.FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB] is `false`, preserving 100% of notes,
 * labels, and attachments while keeping [BiometricLock.ENABLED] and setting
 * `biometricLockEncryptsDb = false`.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseDecryptionMigrationTest {

    private lateinit var app: Application
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        preferences = NotallyXPreferences.getInstance(app)
        NotallyDatabase.clearInstance()
        NotallyDatabase.getCurrentDatabaseFiles(app).forEach { it.delete() }
    }

    @After
    fun tearDown() {
        NotallyDatabase.clearInstance()
        NotallyDatabase.getCurrentDatabaseFiles(app).forEach { it.delete() }
    }

    @Test
    fun decryptDatabaseOnStartupIfNeeded_decryptsEncryptedV11DatabaseWithoutDataLoss() =
        runBlocking {
            val dbFile = NotallyDatabase.getInternalDatabaseFile(app)
            dbFile.parentFile?.mkdirs()

            // 1. Create a populated v11 database on disk
            val seedDb =
                Room.databaseBuilder(
                        app,
                        NotallyDatabase::class.java,
                        NotallyDatabase.DATABASE_NAME,
                    )
                    .allowMainThreadQueries()
                    .build()
            val expectedNoteId =
                seedDb
                    .getBaseNoteDao()
                    .insert(
                        BaseNote(
                            id = 0L,
                            type = Type.NOTE,
                            folder = Folder.NOTES,
                            color = "DEFAULT",
                            title = "Encrypted Note",
                            pinned = true,
                            timestamp = 1700000000000L,
                            modifiedTimestamp = 1700000000000L,
                            labels = listOf("important"),
                            body = "Secret content preserved across decryption",
                            spans = emptyList(),
                            items = emptyList(),
                            images = listOf(FileAttachment("img1.jpg", "photo.jpg", "image/jpeg")),
                            files = emptyList(),
                            audios = emptyList(),
                            reminders = emptyList(),
                            viewMode = NoteViewMode.EDIT,
                            isPinnedToStatus = false,
                        )
                    )
            seedDb.getLabelDao().insert(Label("important", 0))
            seedDb.checkpoint()
            seedDb.close()

            // 2. Encrypt the database file with SQLCipher as legacy biometric DB encryption did
            System.loadLibrary("sqlcipher")
            val passphrase = ByteArray(32) { (it + 1).toByte() }
            encryptDatabase(app, dbFile, passphrase)
            assertTrue(dbFile.isEncryptedDatabase(app))

            preferences.fallbackDatabaseEncryptionKey.save(passphrase)
            preferences.biometricLock.save(BiometricLock.ENABLED)
            preferences.biometricLockEncryptsDb.save(true)
            preferences.dataInPublicFolder.save(false)

            // 3. Run the startup decryption migration
            val migrated = NotallyDatabase.decryptDatabaseOnStartupIfNeeded(app, preferences, false)
            assertTrue(migrated)
            assertTrue(dbFile.isUnencryptedDatabase(app))
            assertFalse(preferences.biometricLockEncryptsDb.value)
            assertEquals(BiometricLock.ENABLED, preferences.biometricLock.value)

            // 4. Open with standard plaintext Room and verify 100% of notes, labels, and
            // attachments
            val decryptedDb =
                Room.databaseBuilder(
                        app,
                        NotallyDatabase::class.java,
                        NotallyDatabase.DATABASE_NAME,
                    )
                    .allowMainThreadQueries()
                    .build()
            try {
                val restoredNote = decryptedDb.getBaseNoteDao().get(expectedNoteId)
                assertNotNull(restoredNote)
                assertEquals("Encrypted Note", restoredNote!!.title)
                assertEquals("Secret content preserved across decryption", restoredNote.body)
                assertEquals(listOf("important"), restoredNote.labels)
                assertEquals(1, restoredNote.images.size)
                assertEquals("img1.jpg", restoredNote.images.first().localName)

                val restoredLabels = decryptedDb.getLabelDao().getAll().first()
                assertEquals(1, restoredLabels.size)
                assertEquals("important", restoredLabels.first().value)
            } finally {
                decryptedDb.close()
            }
        }
}
