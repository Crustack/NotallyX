package com.philkes.notallyx.feature.media.audio

import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.fakes.FakeAudioPlayer
import com.philkes.notallyx.test.fakes.FakeFileStorage
import com.philkes.notallyx.test.fakes.FakePathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayAudioViewModelTest {

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
    fun testPlayAudioActionsAndState() {
        runBlocking {
            val viewModel =
                PlayAudioViewModel(
                    preferences = preferences,
                    pathProvider = pathProvider,
                    fileStorage = FakeFileStorage(),
                    audioPlayer = FakeAudioPlayer(),
                )

            val key =
                NotallyNavKey.PlayAudio(
                    audioName = "recording.m4a",
                    duration = 5000L,
                    timestamp = 1000L,
                )
            viewModel.onAction(PlayAudioAction.Init(key))

            assertThat(viewModel.uiState.value.audio?.name).isEqualTo("recording.m4a")
            assertThat(viewModel.uiState.value.duration).isEqualTo(5000L)

            viewModel.onAction(PlayAudioAction.DeleteClicked)
            assertThat(viewModel.uiState.value.activeDialog)
                .isEqualTo(PlayAudioDialogState.ConfirmDelete)

            viewModel.onAction(PlayAudioAction.DismissDialog)
            assertThat(viewModel.uiState.value.activeDialog).isNull()

            viewModel.onAction(PlayAudioAction.BackClicked)
            val backEffect = viewModel.sideEffects.first()
            assertThat(backEffect).isEqualTo(PlayAudioSideEffect.NavigateBack)
        }
    }
}
