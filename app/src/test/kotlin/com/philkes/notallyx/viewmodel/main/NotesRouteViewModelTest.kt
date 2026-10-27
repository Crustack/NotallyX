package com.philkes.notallyx.viewmodel.main

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class NotesRouteViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var viewModel: NotesRouteViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(application).edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()

        NotallyDatabase.clearInstance()
        database = NotallyDatabase.getDatabase(application).value!!

        viewModel = NotesRouteViewModel(application, preferences)
        drainTasks()
    }

    @After
    fun tearDown() {
        NotallyDatabase.clearInstance()
        NotallyXPreferences.clearInstance()
    }

    private fun drainTasks() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun createTestNote(
        title: String = "Test Note",
        body: String = "Test Body",
        folder: Folder = Folder.NOTES,
        pinned: Boolean = false,
        color: String = BaseNote.COLOR_DEFAULT,
    ): BaseNote {
        val now = System.currentTimeMillis()
        return BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = folder,
            color = color,
            title = title,
            pinned = pinned,
            timestamp = now,
            modifiedTimestamp = now,
            labels = emptyList(),
            body = body,
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
    }

    @Test
    fun setFolder_updatesFolderStateFlow() {
        assertThat(viewModel.folder.value).isEqualTo(Folder.NOTES)

        viewModel.setFolder(Folder.ARCHIVED)
        assertThat(viewModel.folder.value).isEqualTo(Folder.ARCHIVED)

        viewModel.setFolder(Folder.DELETED)
        assertThat(viewModel.folder.value).isEqualTo(Folder.DELETED)
    }

    @Test
    fun keyword_updatesKeywordFlowAndTriggersSearch() {
        runBlocking(Dispatchers.IO) {
            database.getBaseNoteDao().insert(createTestNote(title = "Kotlin Guide"))
            database.getBaseNoteDao().insert(createTestNote(title = "Java Notes"))
        }

        viewModel.keyword = "Kotlin"
        assertThat(viewModel.keywordFlow.value).isEqualTo("Kotlin")
        assertThat(viewModel.keyword).isEqualTo("Kotlin")
    }

    @Test
    fun actionMode_selectionAndClear() {
        val note1 = createTestNote(title = "Note 1").copy(id = 1L)
        val note2 = createTestNote(title = "Note 2").copy(id = 2L)

        viewModel.actionMode.add(note1.id, note1)
        assertThat(viewModel.actionMode.selectedIds).containsExactly(1L)
        assertThat(viewModel.actionMode.isEmpty()).isFalse()

        viewModel.actionMode.add(note2.id, note2)
        assertThat(viewModel.actionMode.selectedIds).containsExactlyInAnyOrder(1L, 2L)

        viewModel.actionMode.close(false)
        assertThat(viewModel.actionMode.isEmpty()).isTrue()
        assertThat(viewModel.actionMode.selectedIds).isEmpty()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 3000
        while (!condition() && System.currentTimeMillis() < end) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(10)
        }
    }

    @Test
    fun pinBaseNotes_updatesPinnedStateInDatabase() {
        runBlocking {
            val noteId =
                database
                    .getBaseNoteDao()
                    .insert(createTestNote(title = "Note to Pin", pinned = false))
            val note = database.getBaseNoteDao().get(noteId)!!

            viewModel.actionMode.add(note.id, note)
            viewModel.pinBaseNotes(true)
            waitUntil { runBlocking { database.getBaseNoteDao().get(noteId)?.pinned == true } }

            val updatedNote = database.getBaseNoteDao().get(noteId)
            assertThat(updatedNote?.pinned).isTrue()
            assertThat(viewModel.actionMode.isEmpty()).isTrue()
        }
    }

    @Test
    fun colorBaseNote_updatesColorInDatabase() {
        runBlocking {
            val noteId = database.getBaseNoteDao().insert(createTestNote(title = "Colored Note"))
            val note = database.getBaseNoteDao().get(noteId)!!

            val targetColor = Color.CORAL.toColorString()
            viewModel.actionMode.add(note.id, note)
            viewModel.colorBaseNote(targetColor)
            waitUntil {
                runBlocking { database.getBaseNoteDao().get(noteId)?.color == targetColor }
            }

            val updatedNote = database.getBaseNoteDao().get(noteId)
            assertThat(updatedNote?.color).isEqualTo(targetColor)
        }
    }

    @Test
    fun duplicateNotes_createsNoteCopyWithSuffix() {
        runBlocking {
            val noteId = database.getBaseNoteDao().insert(createTestNote(title = "Original Recipe"))
            val original = database.getBaseNoteDao().get(noteId)!!

            val newIds = viewModel.duplicateNotes(listOf(original))
            waitUntil {
                runBlocking {
                    newIds.isNotEmpty() && database.getBaseNoteDao().get(newIds.first()) != null
                }
            }

            assertThat(newIds).hasSize(1)
            val copy = database.getBaseNoteDao().get(newIds.first())!!
            assertThat(copy.title).contains("Original Recipe")
            assertThat(copy.title).contains("Copy")
            assertThat(copy.body).isEqualTo(original.body)
        }
    }

    @Test
    fun moveBaseNotes_changesFolderInDatabase() {
        runBlocking {
            val noteId =
                database
                    .getBaseNoteDao()
                    .insert(createTestNote(title = "Note to Archive", folder = Folder.NOTES))

            viewModel.moveBaseNotes(longArrayOf(noteId), Folder.ARCHIVED)
            waitUntil {
                runBlocking { database.getBaseNoteDao().get(noteId)?.folder == Folder.ARCHIVED }
            }

            val updatedNote = database.getBaseNoteDao().get(noteId)
            assertThat(updatedNote?.folder).isEqualTo(Folder.ARCHIVED)
        }
    }
}
