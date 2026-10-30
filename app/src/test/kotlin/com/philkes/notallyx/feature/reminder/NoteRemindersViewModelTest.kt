package com.philkes.notallyx.feature.reminder

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class NoteRemindersViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var viewModel: NoteRemindersViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()

        NotallyDatabase.clearInstance()
        database = NotallyDatabase.getDatabase(application).value!!

        viewModel = NoteRemindersViewModel(application, preferences)
        drainTasks()
    }

    @After
    fun tearDown() {
        drainTasks()
        NotallyDatabase.clearInstance()
        NotallyXPreferences.clearInstance()
    }

    private fun drainTasks() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 3000
        while (!condition() && System.currentTimeMillis() < end) {
            drainTasks()
            Thread.sleep(10)
        }
    }

    @Test
    fun init_withExistingReminders_loadsReminders() = runTest {
        val reminder1 = Reminder(1L, Date(), null)
        val reminder2 = Reminder(2L, Date(), Repetition(1, RepetitionTimeUnit.DAYS))
        val note =
            SampleNotes.RICH_TEXT_NOTE.copy(
                id = 100L,
                reminders = listOf(reminder1, reminder2),
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        viewModel.onAction(NoteRemindersAction.Init(100L))
        waitUntil { viewModel.uiState.value.reminders.size == 2 }

        assertThat(viewModel.uiState.value.reminders).hasSize(2)
        assertThat(viewModel.uiState.value.reminders[0].id).isEqualTo(1L)
        assertThat(viewModel.uiState.value.reminders[1].id).isEqualTo(2L)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun datePicker_transitionsToTimePicker_andBack() = runTest {
        viewModel.onAction(NoteRemindersAction.CheckPermissionsResult(hasPermission = true))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.DatePicker }

        viewModel.onAction(NoteRemindersAction.DateSelected(2026, 9, 31))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.TimePicker }

        viewModel.onAction(NoteRemindersAction.TimePickerBack)
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.DatePicker }
    }

    @Test
    fun timeSelected_transitionsToRepetitionDialog() = runTest {
        viewModel.onAction(NoteRemindersAction.CheckPermissionsResult(hasPermission = true))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.DatePicker }

        viewModel.onAction(NoteRemindersAction.DateSelected(2026, 9, 31))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.TimePicker }

        viewModel.onAction(NoteRemindersAction.TimeSelected(14, 30))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.Repetition }
    }

    @Test
    fun repetitionOptionSelected_savesReminderAndDismissesDialog() = runTest {
        val note = SampleNotes.RICH_TEXT_NOTE.copy(id = 200L, reminders = emptyList())
        runBlocking { database.getBaseNoteDao().insert(note) }

        viewModel.onAction(NoteRemindersAction.Init(200L))
        // When empty, it opens date picker
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.DatePicker }

        viewModel.onAction(NoteRemindersAction.DateSelected(2026, 9, 31))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.TimePicker }

        viewModel.onAction(NoteRemindersAction.TimeSelected(10, 0))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.Repetition }

        viewModel.onAction(NoteRemindersAction.RepetitionOptionSelected(RepetitionOption.DAILY))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                viewModel.uiState.value.reminders.isNotEmpty()
        }
        waitUntil {
            runBlocking { database.getBaseNoteDao().get(200L)?.reminders?.isNotEmpty() == true }
        }

        assertThat(viewModel.uiState.value.reminders).hasSize(1)
        assertThat(viewModel.uiState.value.reminders[0].repetition?.unit)
            .isEqualTo(RepetitionTimeUnit.DAYS)
    }

    @Test
    fun requestDeleteReminder_andConfirm_deletesReminder() = runTest {
        val reminder = Reminder(1L, Date(), null)
        val note =
            SampleNotes.RICH_TEXT_NOTE.copy(
                id = 300L,
                reminders = listOf(reminder),
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        viewModel.onAction(NoteRemindersAction.Init(300L))
        waitUntil { viewModel.uiState.value.reminders.isNotEmpty() }

        viewModel.onAction(NoteRemindersAction.RequestDeleteReminder(reminder))
        waitUntil { viewModel.uiState.value.activeDialog is NoteRemindersDialogState.ConfirmDelete }

        viewModel.onAction(NoteRemindersAction.ConfirmDeleteReminder(reminder))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                viewModel.uiState.value.reminders.isEmpty()
        }
        waitUntil {
            runBlocking { database.getBaseNoteDao().get(300L)?.reminders?.isEmpty() == true }
        }

        assertThat(viewModel.uiState.value.reminders).isEmpty()
    }
}
