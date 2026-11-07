package com.philkes.notallyx.feature.media.audio

import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.test.fakes.FakeAudioRecorder
import com.philkes.notallyx.test.fakes.FakePathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordAudioViewModelTest {

    private lateinit var pathProvider: FakePathProvider
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        pathProvider = FakePathProvider()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.createPortable(pathProvider)
    }

    @After
    fun tearDown() {
        NotallyXPreferences.clearInstance()
        pathProvider.rootDirectory.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun testRecordAudioActionsAndState() = runTest {
        val viewModel =
            RecordAudioViewModel(
                preferences = preferences,
                pathProvider = pathProvider,
                audioRecorder = FakeAudioRecorder(),
            )

        // When in READY state, BackClicked navigates back directly
        viewModel.onAction(RecordAudioAction.BackClicked)
        val effect = viewModel.sideEffects.first()
        assertThat(effect).isEqualTo(RecordAudioSideEffect.NavigateBack)

        // Dismiss dialog
        viewModel.onAction(RecordAudioAction.DismissDialog)
        assertThat(viewModel.uiState.value.activeDialog).isNull()

        // MainClicked starts recording
        viewModel.onAction(RecordAudioAction.MainClicked)
        assertThat(viewModel.uiState.value.status).isEqualTo(Status.RECORDING)

        // MainClicked pauses recording
        viewModel.onAction(RecordAudioAction.MainClicked)
        assertThat(viewModel.uiState.value.status).isEqualTo(Status.PAUSED)

        // MainClicked resumes recording
        viewModel.onAction(RecordAudioAction.MainClicked)
        assertThat(viewModel.uiState.value.status).isEqualTo(Status.RECORDING)

        // StopClicked finishes recording
        viewModel.onAction(RecordAudioAction.StopClicked)
        assertThat(viewModel.uiState.value.status).isEqualTo(Status.READY)
    }
}
