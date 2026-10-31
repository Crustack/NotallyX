package com.philkes.notallyx.feature.labels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.R
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
class LabelsViewModelTest {

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
    fun addAndEditAndToggleAndReorderLabels() = runTest {
        val viewModel = LabelsViewModel(application, preferences)
        drainTasks()

        viewModel.onAction(LabelsAction.RequestAddLabel)
        waitUntil { viewModel.uiState.value.activeDialog == LabelDialogState.Add }
        assertThat(viewModel.uiState.value.activeDialog).isEqualTo(LabelDialogState.Add)

        viewModel.onAction(LabelsAction.ConfirmAddLabel("Personal"))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                runBlocking { database.getLabelDao().exists("Personal") }
        }
        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(runBlocking { database.getLabelDao().exists("Personal") }).isTrue()

        // Duplicate label triggers toast side effect
        viewModel.onAction(LabelsAction.ConfirmAddLabel("Personal"))
        val toastEffect = viewModel.sideEffects.first()
        assertThat(toastEffect).isInstanceOf(LabelsSideEffect.ShowToast::class.java)
        assertThat((toastEffect as LabelsSideEffect.ShowToast).messageRes)
            .isEqualTo(R.string.label_exists)

        // Edit label
        viewModel.onAction(LabelsAction.RequestEditLabel(LabelData("Personal", true, 0)))
        waitUntil { viewModel.uiState.value.activeDialog == LabelDialogState.Edit("Personal") }
        assertThat(viewModel.uiState.value.activeDialog)
            .isEqualTo(LabelDialogState.Edit("Personal"))

        viewModel.onAction(LabelsAction.ConfirmEditLabel("Personal", "Work"))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                runBlocking { database.getLabelDao().exists("Work") }
        }
        assertThat(viewModel.uiState.value.activeDialog).isNull()
        assertThat(runBlocking { database.getLabelDao().exists("Work") }).isTrue()
        assertThat(runBlocking { database.getLabelDao().exists("Personal") }).isFalse()

        // Toggle visibility
        viewModel.onAction(LabelsAction.ToggleVisibility(LabelData("Work", true, 0)))
        waitUntil { preferences.labelsHidden.value.contains("Work") }
        assertThat(preferences.labelsHidden.value).contains("Work")

        // Click label
        viewModel.onAction(LabelsAction.LabelClicked(LabelData("Work", false, 0)))
        val navEffect = viewModel.sideEffects.first()
        assertThat(navEffect).isInstanceOf(LabelsSideEffect.NavigateToLabel::class.java)
        assertThat((navEffect as LabelsSideEffect.NavigateToLabel).label).isEqualTo("Work")

        // Reorder
        viewModel.onAction(
            LabelsAction.ReorderLabels(
                listOf(
                    LabelData("Work", false, 0),
                    LabelData("Other", true, 1),
                )
            )
        )
        drainTasks()

        // Delete label
        viewModel.onAction(LabelsAction.ConfirmDeleteLabel("Work"))
        waitUntil {
            viewModel.uiState.value.activeDialog == null &&
                runBlocking { !database.getLabelDao().exists("Work") }
        }
        assertThat(runBlocking { database.getLabelDao().exists("Work") }).isFalse()
        assertThat(preferences.labelsHidden.value).doesNotContain("Work")
    }
}
