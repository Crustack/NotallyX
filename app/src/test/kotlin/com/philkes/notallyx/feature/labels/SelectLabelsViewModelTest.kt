package com.philkes.notallyx.feature.labels

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
class SelectLabelsViewModelTest {

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
    fun selectLabelsFlow() = runTest {
        val viewModel = SelectLabelsViewModel(preferences, dbFlow, pathProvider)

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
