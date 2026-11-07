package com.philkes.notallyx.feature.reminder

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Reminder
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class RemindersViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var actionMode: ActionMode
    private lateinit var viewModel: RemindersViewModel

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

        viewModel = RemindersViewModel(application, preferences, actionMode)
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

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 3000
        while (!condition() && System.currentTimeMillis() < end) {
            drainTasks()
            Thread.sleep(10)
        }
    }

    @Test
    fun filterOptionSelected_updatesFilterMode() = runTest {
        assertThat(viewModel.uiState.value.filterMode).isEqualTo(FilterOptions.UPCOMING)

        viewModel.onAction(RemindersAction.FilterOptionSelected(FilterOptions.ELAPSED))
        waitUntil { viewModel.uiState.value.filterMode == FilterOptions.ELAPSED }
        assertThat(viewModel.uiState.value.filterMode).isEqualTo(FilterOptions.ELAPSED)

        viewModel.onAction(RemindersAction.FilterOptionSelected(FilterOptions.ALL))
        waitUntil { viewModel.uiState.value.filterMode == FilterOptions.ALL }
        assertThat(viewModel.uiState.value.filterMode).isEqualTo(FilterOptions.ALL)
    }

    @Test
    fun noteClicked_emitsOpenNoteSideEffect() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        var emittedEffect: RemindersSideEffect? = null
        val job = launch(testDispatcher) { viewModel.sideEffects.collect { emittedEffect = it } }

        val note =
            SampleNotes.RICH_TEXT_NOTE.copy(
                id = 10L,
                title = "Reminder Note",
                reminders = listOf(Reminder(1L, Date(), null)),
            )

        viewModel.onAction(RemindersAction.NoteClicked(note, 2))

        assertThat(emittedEffect).isInstanceOf(RemindersSideEffect.OpenNote::class.java)
        val openEffect = emittedEffect as RemindersSideEffect.OpenNote
        assertThat(openEffect.note.id).isEqualTo(10L)
        assertThat(openEffect.position).isEqualTo(2)

        job.cancel()
    }

    @Test
    fun reminderClicked_emitsOpenReminderSideEffect() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        var emittedEffect: RemindersSideEffect? = null
        val job = launch(testDispatcher) { viewModel.sideEffects.collect { emittedEffect = it } }

        val note =
            SampleNotes.RICH_TEXT_NOTE.copy(
                id = 15L,
                title = "Reminder Note 2",
                reminders = listOf(Reminder(2L, Date(), null)),
            )

        viewModel.onAction(RemindersAction.ReminderClicked(note))

        assertThat(emittedEffect).isInstanceOf(RemindersSideEffect.OpenReminder::class.java)
        val reminderEffect = emittedEffect as RemindersSideEffect.OpenReminder
        assertThat(reminderEffect.note.id).isEqualTo(15L)

        job.cancel()
    }
}
