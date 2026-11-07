package com.philkes.notallyx.feature.media.audio

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.navigation.NotallyNavKey
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
class PlayAudioViewModelTest {

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
    fun testPlayAudioActionsAndState() = runTest {
        val viewModel = PlayAudioViewModel(application, preferences)
        drainTasks()

        val key =
            NotallyNavKey.PlayAudio(
                audioName = "recording.m4a",
                duration = 5000L,
                timestamp = 1000L,
            )
        viewModel.onAction(PlayAudioAction.Init(key))

        waitUntil { viewModel.uiState.value.audio?.name == "recording.m4a" }
        assertThat(viewModel.uiState.value.audio?.name).isEqualTo("recording.m4a")
        assertThat(viewModel.uiState.value.duration).isEqualTo(5000L)

        viewModel.onAction(PlayAudioAction.DeleteClicked)
        waitUntil { viewModel.uiState.value.activeDialog != null }
        assertThat(viewModel.uiState.value.activeDialog)
            .isEqualTo(PlayAudioDialogState.ConfirmDelete)

        viewModel.onAction(PlayAudioAction.DismissDialog)
        waitUntil { viewModel.uiState.value.activeDialog == null }
        assertThat(viewModel.uiState.value.activeDialog).isNull()

        viewModel.onAction(PlayAudioAction.BackClicked)
        val backEffect = viewModel.sideEffects.first()
        assertThat(backEffect).isEqualTo(PlayAudioSideEffect.NavigateBack)
    }
}
