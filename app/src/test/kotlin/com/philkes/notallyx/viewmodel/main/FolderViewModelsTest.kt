package com.philkes.notallyx.viewmodel.main

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.main.routes.archived.ArchivedAction
import com.philkes.notallyx.feature.main.routes.archived.ArchivedSideEffect
import com.philkes.notallyx.feature.main.routes.archived.ArchivedViewModel
import com.philkes.notallyx.feature.main.routes.deleted.DeletedAction
import com.philkes.notallyx.feature.main.routes.deleted.DeletedDialogState
import com.philkes.notallyx.feature.main.routes.deleted.DeletedSideEffect
import com.philkes.notallyx.feature.main.routes.deleted.DeletedViewModel
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledAction
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledSideEffect
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesAction
import com.philkes.notallyx.feature.main.routes.notes.NotesSideEffect
import com.philkes.notallyx.feature.main.routes.notes.NotesViewModel
import kotlinx.coroutines.flow.first
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
class FolderViewModelsTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var actionMode: ActionMode

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()

        NotallyDatabase.clearInstance()
        database = NotallyDatabase.getDatabase(application).value!!
        actionMode = ActionMode()
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
        id: Long = 1L,
        title: String = "Test Note",
        folder: Folder = Folder.NOTES,
        labels: List<String> = emptyList(),
    ) =
        BaseNote(
            id = id,
            type = Type.NOTE,
            folder = folder,
            color = BaseNote.COLOR_DEFAULT,
            title = title,
            pinned = false,
            timestamp = System.currentTimeMillis(),
            modifiedTimestamp = System.currentTimeMillis(),
            labels = labels,
            body = "Body",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    @Test
    fun notesViewModel_actionsAndState() {
        runBlocking {
            val note = createTestNote(1L, "Notes item", Folder.NOTES)
            database.getBaseNoteDao().insert(listOf(note))
            drainTasks()

            val viewModel = NotesViewModel(application, preferences, actionMode)
            drainTasks()

            // 1. Initial State
            assertThat(viewModel.uiState.value.isGrid)
                .isEqualTo(preferences.notesView.value == NotesView.GRID)

            // 2. Selection reaction
            actionMode.add(note.id, note)
            drainTasks()
            assertThat(viewModel.uiState.value.selectedIds).containsExactly(1L)

            // 3. Action handling & side effects
            viewModel.onAction(NotesAction.NoteClicked(note, 0))
            val clickEffect = viewModel.sideEffects.first()
            assertThat(clickEffect).isInstanceOf(NotesSideEffect.OpenNote::class.java)
            assertThat((clickEffect as NotesSideEffect.OpenNote).note.id).isEqualTo(1L)

            viewModel.onAction(NotesAction.NoteLongClicked(note, 0, emptyList()))
            val longClickEffect = viewModel.sideEffects.first()
            assertThat(longClickEffect).isInstanceOf(NotesSideEffect.SelectNote::class.java)

            viewModel.onAction(NotesAction.ReminderClicked(note))
            val reminderEffect = viewModel.sideEffects.first()
            assertThat(reminderEffect).isInstanceOf(NotesSideEffect.OpenReminder::class.java)
        }
    }

    @Test
    fun deletedViewModel_dialogsAndActions() {
        runBlocking {
            val note = createTestNote(2L, "Deleted item", Folder.DELETED)
            database.getBaseNoteDao().insert(listOf(note))
            drainTasks()

            val viewModel = DeletedViewModel(application, preferences, actionMode)
            drainTasks()

            // 1. Initial state
            assertThat(viewModel.uiState.value.activeDialog).isNull()

            // 2. Request Delete All
            viewModel.onAction(DeletedAction.RequestDeleteAll)
            drainTasks()
            assertThat(viewModel.uiState.value.activeDialog)
                .isEqualTo(DeletedDialogState.ConfirmDeleteAll)
            assertThat(viewModel.showConfirmDeleteAllDialog.value).isTrue()

            // 3. Dismiss Dialog
            viewModel.onAction(DeletedAction.DismissDialog)
            drainTasks()
            assertThat(viewModel.uiState.value.activeDialog).isNull()
            assertThat(viewModel.showConfirmDeleteAllDialog.value).isFalse()

            // 4. Confirm Delete All
            viewModel.onAction(DeletedAction.RequestDeleteAll)
            drainTasks()
            viewModel.onAction(DeletedAction.ConfirmDeleteAll)
            drainTasks()
            assertThat(viewModel.uiState.value.activeDialog).isNull()

            // 5. Action and side effects
            viewModel.onAction(DeletedAction.NoteClicked(note, 0))
            val clickEffect = viewModel.sideEffects.first()
            assertThat(clickEffect).isInstanceOf(DeletedSideEffect.OpenNote::class.java)
        }
    }

    @Test
    fun archivedViewModel_actionsAndState() {
        runBlocking {
            val note = createTestNote(3L, "Archived item", Folder.ARCHIVED)
            database.getBaseNoteDao().insert(listOf(note))
            drainTasks()

            val viewModel = ArchivedViewModel(application, preferences, actionMode)
            drainTasks()

            actionMode.add(note.id, note)
            drainTasks()
            assertThat(viewModel.uiState.value.selectedIds).containsExactly(3L)

            viewModel.onAction(ArchivedAction.NoteClicked(note, 0))
            val clickEffect = viewModel.sideEffects.first()
            assertThat(clickEffect).isInstanceOf(ArchivedSideEffect.OpenNote::class.java)

            viewModel.onAction(ArchivedAction.NoteLongClicked(note, 0, emptyList()))
            val longClickEffect = viewModel.sideEffects.first()
            assertThat(longClickEffect).isInstanceOf(ArchivedSideEffect.SelectNote::class.java)

            viewModel.onAction(ArchivedAction.ReminderClicked(note))
            val reminderEffect = viewModel.sideEffects.first()
            assertThat(reminderEffect).isInstanceOf(ArchivedSideEffect.OpenReminder::class.java)
        }
    }

    @Test
    fun unlabeledViewModel_actionsAndState() {
        runBlocking {
            val note = createTestNote(4L, "Unlabeled item", Folder.NOTES, emptyList())
            database.getBaseNoteDao().insert(listOf(note))
            drainTasks()

            val viewModel = UnlabeledViewModel(application, preferences, actionMode)
            drainTasks()

            viewModel.onAction(UnlabeledAction.NoteClicked(note, 0))
            val clickEffect = viewModel.sideEffects.first()
            assertThat(clickEffect).isInstanceOf(UnlabeledSideEffect.OpenNote::class.java)

            viewModel.onAction(UnlabeledAction.NoteLongClicked(note, 0, emptyList()))
            val longClickEffect = viewModel.sideEffects.first()
            assertThat(longClickEffect).isInstanceOf(UnlabeledSideEffect.SelectNote::class.java)

            viewModel.onAction(UnlabeledAction.ReminderClicked(note))
            val reminderEffect = viewModel.sideEffects.first()
            assertThat(reminderEffect).isInstanceOf(UnlabeledSideEffect.OpenReminder::class.java)
        }
    }
}
