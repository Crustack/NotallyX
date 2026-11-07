package com.philkes.notallyx.feature.main.routes.search

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Folder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
class SearchViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()
    }

    @After
    fun tearDown() {
        drainTasks()
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
    fun testSearchActionsAndSideEffects() = runTest {
        val viewModel = SearchViewModel(application, preferences)
        drainTasks()

        viewModel.onAction(SearchAction.Init(Folder.ARCHIVED, "Work"))
        waitUntil { viewModel.uiState.value.selectedFolder == Folder.ARCHIVED }
        assertThat(viewModel.uiState.value.selectedFolder).isEqualTo(Folder.ARCHIVED)
        assertThat(viewModel.uiState.value.currentLabel).isEqualTo("Work")

        viewModel.onAction(SearchAction.KeywordChanged("shopping"))
        waitUntil { viewModel.uiState.value.searchKeyword == "shopping" }
        assertThat(viewModel.uiState.value.searchKeyword).isEqualTo("shopping")

        viewModel.onAction(SearchAction.FolderSelected(Folder.DELETED))
        waitUntil { viewModel.uiState.value.selectedFolder == Folder.DELETED }
        assertThat(viewModel.uiState.value.selectedFolder).isEqualTo(Folder.DELETED)

        val note = SampleNotes.LOREM_IPSUM_NOTE

        viewModel.onAction(SearchAction.NoteClicked(note, 0))
        val openEffect = viewModel.sideEffects.first()
        assertThat(openEffect).isEqualTo(SearchSideEffect.OpenNote(note, 0))

        viewModel.onAction(SearchAction.ReminderClicked(note))
        val reminderEffect = viewModel.sideEffects.first()
        assertThat(reminderEffect).isEqualTo(SearchSideEffect.OpenReminder(note))
    }
}
