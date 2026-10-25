package com.philkes.notallyx.feature.main.routes.settings

import android.app.Application
import android.os.Environment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.getPrivateAttachmentsRoot
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class AutoRemoveDeletedNotesWorkerTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase

    private val oneDayMillis = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(application).edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)

        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        database = NotallyDatabase.getDatabase(application).value!!
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        database.clearAllTables()
        NotallyXPreferences.clearInstance()
    }

    private fun createTestNote(
        title: String,
        folder: Folder,
        daysAgo: Long,
        images: List<FileAttachment> = emptyList(),
        files: List<FileAttachment> = emptyList(),
    ): BaseNote {
        val now = System.currentTimeMillis()
        val timestamp = now - (daysAgo * oneDayMillis)
        return BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = folder,
            color = BaseNote.COLOR_DEFAULT,
            title = title,
            pinned = false,
            timestamp = timestamp,
            modifiedTimestamp = timestamp,
            labels = emptyList(),
            body = "Body for $title",
            spans = emptyList(),
            items = mutableListOf(),
            images = images,
            files = files,
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
    }

    @Test
    fun removeOldDeletedNotes_whenDisabled_doesNotDeleteAnyNotes() = runBlocking {
        preferences.autoRemoveDeletedNotesAfterDays.save(0)

        val note =
            createTestNote(title = "Deleted 100 days ago", folder = Folder.DELETED, daysAgo = 100)
        val noteId = database.getBaseNoteDao().insert(note)

        val result = application.removeOldDeletedNotes(preferences)

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull(database.getBaseNoteDao().get(noteId))
    }

    @Test
    fun removeOldDeletedNotes_deletesNotesOlderThanDaysAndKeepsRecentAndActiveNotes() =
        runBlocking {
            preferences.autoRemoveDeletedNotesAfterDays.save(30)

            val noteOldDeleted =
                createTestNote(title = "Old Deleted", folder = Folder.DELETED, daysAgo = 35)
            val noteRecentDeleted =
                createTestNote(title = "Recent Deleted", folder = Folder.DELETED, daysAgo = 5)
            val noteActive =
                createTestNote(title = "Active Note", folder = Folder.NOTES, daysAgo = 45)

            val idOld = database.getBaseNoteDao().insert(noteOldDeleted)
            val idRecent = database.getBaseNoteDao().insert(noteRecentDeleted)
            val idActive = database.getBaseNoteDao().insert(noteActive)

            val result = application.removeOldDeletedNotes(preferences)

            assertEquals(ListenableWorker.Result.success(), result)
            assertNull(
                "Note deleted 35 days ago should be removed with 30-day retention",
                database.getBaseNoteDao().get(idOld),
            )
            assertNotNull(
                "Note deleted 5 days ago should be kept with 30-day retention",
                database.getBaseNoteDao().get(idRecent),
            )
            assertNotNull(
                "Active note in NOTES folder should never be removed by auto-remove",
                database.getBaseNoteDao().get(idActive),
            )
        }

    @Test
    fun removeOldDeletedNotes_cleansUpAssociatedAttachments() = runBlocking {
        preferences.autoRemoveDeletedNotesAfterDays.save(7)

        val imageDir = File(application.getPrivateAttachmentsRoot(), SUBFOLDER_IMAGES)
        val fileDir = File(application.getPrivateAttachmentsRoot(), SUBFOLDER_FILES)
        imageDir.mkdirs()
        fileDir.mkdirs()

        val imageFileName = "test_image.png"
        val attachmentFileName = "test_doc.pdf"
        val diskImageFile = File(imageDir, imageFileName).apply { writeText("fake image bytes") }
        val diskAttachmentFile =
            File(fileDir, attachmentFileName).apply { writeText("fake doc bytes") }

        assertTrue(diskImageFile.exists())
        assertTrue(diskAttachmentFile.exists())

        val imageAttachment =
            FileAttachment(
                localName = imageFileName,
                originalName = "test_image.png",
                mimeType = "image/png",
            )
        val docAttachment =
            FileAttachment(
                localName = attachmentFileName,
                originalName = "test_doc.pdf",
                mimeType = "application/pdf",
            )

        val noteWithAttachments =
            createTestNote(
                title = "Note With Attachments",
                folder = Folder.DELETED,
                daysAgo = 10,
                images = listOf(imageAttachment),
                files = listOf(docAttachment),
            )
        val noteId = database.getBaseNoteDao().insert(noteWithAttachments)

        val result = application.removeOldDeletedNotes(preferences)

        assertEquals(ListenableWorker.Result.success(), result)
        assertNull(database.getBaseNoteDao().get(noteId))
        assertFalse(
            "Image attachment file should be deleted from disk",
            diskImageFile.exists(),
        )
        assertFalse(
            "Doc attachment file should be deleted from disk",
            diskAttachmentFile.exists(),
        )
    }
}
