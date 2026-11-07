package com.philkes.notallyx.feature.editor

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
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
class NoteModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var noteModel: NoteModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(application).edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()

        NotallyDatabase.clearInstance()
        database = NotallyDatabase.getDatabase(application).value!!

        noteModel = NoteModel(application, preferences)
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

    @Test
    fun defaultState_isNewNoteAndEmpty() {
        assertThat(noteModel.isNewNote).isTrue()
        assertThat(noteModel.id).isEqualTo(0L)
        assertThat(noteModel.type).isEqualTo(Type.NOTE)
        assertThat(noteModel.folder).isEqualTo(Folder.NOTES)
        assertThat(noteModel.isEmpty()).isTrue()
    }

    @Test
    fun isEmpty_returnsFalseWhenContentAdded() {
        assertThat(noteModel.isEmpty()).isTrue()

        noteModel.title = "Note Title"
        assertThat(noteModel.isEmpty()).isFalse()

        noteModel.title = ""
        noteModel.body = TextFieldValue("Note Body")
        assertThat(noteModel.isEmpty()).isFalse()
    }

    @Test
    fun viewMode_toggleAndSet() {
        assertThat(noteModel.viewMode.value).isEqualTo(NoteViewMode.EDIT)

        val newMode = noteModel.toggleViewMode()
        assertThat(newMode).isEqualTo(NoteViewMode.READ_ONLY)
        assertThat(noteModel.viewMode.value).isEqualTo(NoteViewMode.READ_ONLY)

        noteModel.toggleViewMode()
        assertThat(noteModel.viewMode.value).isEqualTo(NoteViewMode.EDIT)

        noteModel.setViewMode(NoteViewMode.READ_ONLY)
        assertThat(noteModel.viewMode.value).isEqualTo(NoteViewMode.READ_ONLY)
    }

    @Test
    fun saveNote_persistsNoteAndUpdatesId() {
        runBlocking {
            noteModel.title = "Shopping Note"
            noteModel.body = TextFieldValue("Milk, Eggs, Bread")

            val savedId = noteModel.saveNote(checkBackupOnSave = false)
            drainTasks()

            assertThat(savedId).isGreaterThan(0L)
            assertThat(noteModel.id).isEqualTo(savedId)
            assertThat(noteModel.isNewNote).isFalse()

            val persisted = database.getBaseNoteDao().get(savedId)
            assertThat(persisted).isNotNull
            assertThat(persisted?.title).isEqualTo("Shopping Note")
            assertThat(persisted?.body).isEqualTo("Milk, Eggs, Bread")
        }
    }

    @Test
    fun isModified_tracksChangesFromOriginal() {
        runBlocking {
            noteModel.title = "Original Title"
            noteModel.body = TextFieldValue("Original Body")
            noteModel.saveNote(checkBackupOnSave = false)
            drainTasks()

            assertThat(noteModel.isModified()).isFalse()

            noteModel.title = "Modified Title"
            assertThat(noteModel.isModified()).isTrue()

            noteModel.title = "Original Title"
            assertThat(noteModel.isModified()).isFalse()
        }
    }

    @Test
    fun setItems_setsListItems() {
        noteModel.type = Type.LIST
        val items =
            listOf(
                ListItem(
                    body = "Buy coffee",
                    checked = false,
                    isChild = false,
                    order = 0,
                    children = mutableListOf(),
                    id = 1,
                ),
                ListItem(
                    body = "Espresso beans",
                    checked = false,
                    isChild = true,
                    order = 1,
                    children = mutableListOf(),
                    id = 2,
                ),
            )
        noteModel.setItems(items)

        assertThat(noteModel.items).hasSize(2)
        assertThat(noteModel.items[0].body).isEqualTo("Buy coffee")
        assertThat(noteModel.items[1].body).isEqualTo("Espresso beans")
    }

    @Test
    fun deleteBaseNote_removesFromDatabase() {
        runBlocking {
            noteModel.title = "Ephemeral Note"
            val savedId = noteModel.saveNote(checkBackupOnSave = false)
            drainTasks()

            assertThat(database.getBaseNoteDao().get(savedId)).isNotNull

            noteModel.deleteBaseNote(checkAutoSave = false)
            drainTasks()

            assertThat(database.getBaseNoteDao().get(savedId)).isNull()
        }
    }
}
