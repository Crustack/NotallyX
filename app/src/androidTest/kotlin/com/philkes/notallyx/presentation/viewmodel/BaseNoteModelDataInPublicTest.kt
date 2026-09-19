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
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
class BaseNoteModelDataInPublicTest {

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

        // Start each test from a known state with the data in the internal (private) folder.
        assertFalse(preferences.dataInPublicFolder.value)
    }

    @After
    fun tearDown() {
        // Leave the database in the internal folder so following tests/runs start clean.
        if (preferences.dataInPublicFolder.value) {
            runCallback { callback -> onMain { model.disableDataInPublic(callback) } }
        }
    }

    @Test
    fun enableDisableDataInPublic() {
        // Enable: the database is moved from the internal to the public/external folder.
        runCallback { callback -> onMain { model.enableDataInPublic(callback) } }
        waitUntil { preferences.dataInPublicFolder.value }
        assertTrue(preferences.dataInPublicFolder.value)
        assertTrue(externalDatabaseFile().exists())
        assertEquals(externalDatabaseFile(), NotallyDatabase.getCurrentDatabaseFile(context))
        var note = notallyDatabase.getBaseNoteDao().get(1L)!!
        assertEquals("Note", note.title)
        assertFalse(NotallyDatabase.isBeingReplaced())

        // Disable: the database is moved back from the public to the internal folder.
        runCallback { callback -> onMain { model.disableDataInPublic(callback) } }
        waitUntil { !preferences.dataInPublicFolder.value }
        assertFalse(preferences.dataInPublicFolder.value)
        assertTrue(internalDatabaseFile().exists())
        assertEquals(internalDatabaseFile(), NotallyDatabase.getCurrentDatabaseFile(context))
        note = notallyDatabase.getBaseNoteDao().get(1L)!!
        assertEquals("Note", note.title)
        assertFalse(NotallyDatabase.isBeingReplaced())
    }

    private fun internalDatabaseFile(): File = NotallyDatabase.getInternalDatabaseFile(context)

    private fun externalDatabaseFile(): File = NotallyDatabase.getExternalDatabaseFile(context)

    private fun runCallback(timeoutMs: Long = 30_000L, block: (callback: () -> Unit) -> Unit) {
        val latch = CountDownLatch(1)
        block { latch.countDown() }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

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
