package com.philkes.notallyx.feature.media.audio

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.ShadowContextImplMedia
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
class RecordAudioViewModelTest {

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
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        NotallyXPreferences.clearInstance()
    }

    @Test
    fun testRecordAudioActionsAndState() = runTest {
        val viewModel = RecordAudioViewModel(application, preferences)

        // When in READY state, BackClicked navigates back directly
        viewModel.onAction(RecordAudioAction.BackClicked)
        val effect = viewModel.sideEffects.first()
        assertThat(effect).isEqualTo(RecordAudioSideEffect.NavigateBack)

        // Dismiss dialog
        viewModel.onAction(RecordAudioAction.DismissDialog)
        assertThat(viewModel.uiState.value.activeDialog).isNull()
    }
}
