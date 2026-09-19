package com.philkes.notallyx.presentation.viewmodel

import android.app.Application
import android.content.ContextWrapper
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.utils.security.getInitializedCipherForDecryption
import com.philkes.notallyx.utils.security.getInitializedCipherForEncryption
import com.philkes.notallyx.utils.security.isEncryptedDatabase
import com.philkes.notallyx.utils.security.isUnencryptedDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
class BaseNoteModelBiometricLockTest {

    private lateinit var app: Application
    private lateinit var context: ContextWrapper
    private lateinit var preferences: NotallyXPreferences
    private lateinit var model: BaseNoteModel

    private val notallyDatabase: NotallyDatabase
        get() = onMain { NotallyDatabase.getDatabase(context).value!! }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        app = instrumentation.targetContext.applicationContext as Application
        context = app as ContextWrapper
        System.loadLibrary("sqlcipher")

        preferences = NotallyXPreferences.getInstance(context)

        instrumentation.runOnMainSync {
            preferences.periodicBackups.save(PeriodicBackup(0, 0))
            model = BaseNoteModel(app)
            model.startObserving()
        }

        // Make sure the database file actually exists on disk before running the tests.
        runBlocking {
            withContext(Dispatchers.IO) {
                notallyDatabase.ping()
                notallyDatabase.getBaseNoteDao().delete(1)
                notallyDatabase.getBaseNoteDao().insert(createBaseNote(id = 1L, title = "Note"))
            }
        }

        // Start each test from a known, unencrypted state.
        assertTrue(databaseFile().isUnencryptedDatabase(context))
    }

    @Test
    fun enableDisableBiometricLock() {
        val encryptCipher = getInitializedCipherForEncryption()
        runBlocking { model.enableBiometricLock(encryptCipher) }
        waitUntil { preferences.biometricLock.value == BiometricLock.ENABLED }
        assertTrue(databaseFile().isEncryptedDatabase(context))
        assertFalse(NotallyDatabase.isBeingReplaced())
        var note = notallyDatabase.getBaseNoteDao().get(1L)!!
        assertEquals("Note", note.title)

        // Use the IV of the cipher that actually encrypted the passphrase instead of
        // preferences.iv, which is persisted asynchronously and may still hold a stale value
        // from a previous test run (causing decryption to fail when tests run together).
        val decryptCipher = getInitializedCipherForDecryption(iv = encryptCipher.iv)
        runBlocking { model.disableBiometricLock(decryptCipher) }

        waitUntil { preferences.biometricLock.value == BiometricLock.DISABLED }
        assertTrue(preferences.biometricLock.value == BiometricLock.DISABLED)
        assertTrue(databaseFile().isUnencryptedDatabase(context))
        note = notallyDatabase.getBaseNoteDao().get(1L)!!
        assertEquals("Note", note.title)
        assertFalse(NotallyDatabase.isBeingReplaced())
    }

    private fun databaseFile(): File = NotallyDatabase.getCurrentDatabaseFile(context)

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun waitUntil(timeoutMs: Long = 10_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    companion object {
        fun createBaseNote(
            id: Long = 0L,
            type: Type = Type.NOTE,
            folder: Folder = Folder.NOTES,
            color: String = BaseNote.COLOR_DEFAULT,
            title: String = "Note",
            pinned: Boolean = false,
            timestamp: Long = System.currentTimeMillis(),
            modifiedTimestamp: Long = System.currentTimeMillis(),
            labels: List<String> = listOf(),
            body: String = "",
            spans: List<SpanRepresentation> = listOf(),
            items: List<ListItem> = listOf(),
            images: List<FileAttachment> = listOf(),
            files: List<FileAttachment> = listOf(),
            audios: List<Audio> = listOf(),
            reminders: List<Reminder> = listOf(),
        ): BaseNote {
            return BaseNote(
                id,
                type,
                folder,
                color,
                title,
                pinned,
                timestamp,
                modifiedTimestamp,
                labels,
                body,
                spans,
                items,
                images,
                files,
                audios,
                reminders,
                NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        }
    }
}
