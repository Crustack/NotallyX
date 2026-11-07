package com.philkes.notallyx.feature.labels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class SelectLabelsViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()

        NotallyDatabase.clearInstance()
        database = NotallyDatabase.getDatabase(application).value!!
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
    fun selectLabelsFlow() = runTest {
        val viewModel = SelectLabelsViewModel(application, preferences)
        drainTasks()

        viewModel.onAction(SelectLabelsAction.Init(listOf("Personal")))
        waitUntil { viewModel.uiState.value.selectedLabels.contains("Personal") }
        assertThat(viewModel.uiState.value.selectedLabels).containsExactly("Personal")

        viewModel.onAction(SelectLabelsAction.ToggleLabel("Work", true))
        waitUntil { viewModel.uiState.value.selectedLabels.contains("Work") }
        assertThat(viewModel.uiState.value.selectedLabels)
            .containsExactlyInAnyOrder("Personal", "Work")

        viewModel.onAction(SelectLabelsAction.ToggleLabel("Personal", false))
        waitUntil { !viewModel.uiState.value.selectedLabels.contains("Personal") }
        assertThat(viewModel.uiState.value.selectedLabels).containsExactly("Work")

        viewModel.onAction(SelectLabelsAction.RequestAddLabel)
        waitUntil { viewModel.uiState.value.activeDialog == LabelDialogState.Add }
        assertThat(viewModel.uiState.value.activeDialog).isEqualTo(LabelDialogState.Add)

        viewModel.onAction(SelectLabelsAction.ConfirmAddLabel("Urgent"))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                runBlocking { database.getLabelDao().exists("Urgent") }
        }
        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(runBlocking { database.getLabelDao().exists("Urgent") }).isTrue()
        assertThat(viewModel.uiState.value.selectedLabels).contains("Urgent")

        viewModel.onAction(SelectLabelsAction.BackClicked)
        val effect = viewModel.sideEffects.first()
        assertThat(effect).isInstanceOf(SelectLabelsSideEffect.FinishWithResult::class.java)
        assertThat((effect as SelectLabelsSideEffect.FinishWithResult).selectedLabels)
            .containsExactlyInAnyOrder("Work", "Urgent")
    }
}
