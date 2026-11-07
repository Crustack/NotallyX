package com.philkes.notallyx.feature.main.routes.picknote

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
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
class PickNoteViewModelTest {

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
    fun testPickNoteActionsAndSideEffects() = runTest {
        val viewModel = PickNoteViewModel(application, preferences)

        viewModel.onAction(PickNoteAction.Init(excludeNoteId = 42L))

        val note = SampleNotes.LOREM_IPSUM_NOTE
        viewModel.onAction(PickNoteAction.NoteClicked(note))
        val pickedEffect = viewModel.sideEffects.first()
        assertThat(pickedEffect).isEqualTo(PickNoteSideEffect.NotePicked(note))

        viewModel.onAction(PickNoteAction.BackClicked)
        val backEffect = viewModel.sideEffects.first()
        assertThat(backEffect).isEqualTo(PickNoteSideEffect.NavigateBack)
    }
}
