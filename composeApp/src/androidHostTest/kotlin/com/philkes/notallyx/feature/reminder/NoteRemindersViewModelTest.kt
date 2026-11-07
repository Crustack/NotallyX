package com.philkes.notallyx.feature.reminder

import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.test.fakes.FakeDatabaseDriverFactory
import com.philkes.notallyx.test.fakes.FakePathProvider
import com.philkes.notallyx.test.fakes.FakePermissionChecker
import com.philkes.notallyx.test.fakes.FakeReminderScheduler
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRemindersViewModelTest {

    private lateinit var pathProvider: FakePathProvider
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var dbFlow: MutableStateFlow<NotallyDatabase?>
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var permissionChecker: FakePermissionChecker
    private lateinit var viewModel: NoteRemindersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        pathProvider = FakePathProvider()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.createPortable(pathProvider)

        NotallyDatabase.clearInstance()
        database =
            NotallyDatabase.createDatabase(
                driverFactory = FakeDatabaseDriverFactory(pathProvider),
                inMemory = true,
            )
        dbFlow = MutableStateFlow(database)
        reminderScheduler = FakeReminderScheduler()
        permissionChecker = FakePermissionChecker()

        viewModel =
            NoteRemindersViewModel(
                preferences = preferences,
                databaseStateFlow = dbFlow,
                pathProvider = pathProvider,
                reminderScheduler = reminderScheduler,
                permissionChecker = permissionChecker,
            )
    }

    @After
    fun tearDown() {
        database.close()
        NotallyDatabase.clearInstance()
        NotallyXPreferences.clearInstance()
        pathProvider.rootDirectory.deleteRecursively()
        Dispatchers.resetMain()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 3000
        while (!condition() && System.currentTimeMillis() < end) {
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
        assertThat(reminderScheduler.scheduledReminders).hasSize(1)
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
        assertThat(reminderScheduler.cancelledReminders).containsExactly(300L to 1L)
    }
}
