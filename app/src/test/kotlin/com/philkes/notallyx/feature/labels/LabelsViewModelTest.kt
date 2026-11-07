package com.philkes.notallyx.feature.labels

import com.philkes.notallyx.R
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.test.fakes.FakeDatabaseDriverFactory
import com.philkes.notallyx.test.fakes.FakePathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class LabelsViewModelTest {

    private lateinit var pathProvider: FakePathProvider
    private lateinit var preferences: NotallyXPreferences
    private lateinit var database: NotallyDatabase
    private lateinit var dbFlow: MutableStateFlow<NotallyDatabase?>

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
    fun addAndEditAndToggleAndReorderLabels() = runTest {
        val viewModel = LabelsViewModel(preferences, dbFlow, pathProvider)

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
