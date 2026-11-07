package com.philkes.notallyx.feature.media.image

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
class ViewImageViewModelTest {

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
    fun testViewImageActionsAndState() = runTest {
        val viewModel = ViewImageViewModel(application, preferences)
        drainTasks()

        val key = NotallyNavKey.ViewImage(imageUris = listOf("uri1", "uri2"), initialIndex = 1)
        viewModel.onAction(ViewImageAction.Init(key))

        waitUntil { viewModel.uiState.value.images.size == 2 }
        assertThat(viewModel.uiState.value.images).hasSize(2)
        assertThat(viewModel.uiState.value.initialPage).isEqualTo(1)

        viewModel.onAction(ViewImageAction.DeleteClicked(0))
        waitUntil { viewModel.uiState.value.activeDialog != null }
        assertThat(viewModel.uiState.value.activeDialog)
            .isEqualTo(ViewImageDialogState.ConfirmDelete(0))

        viewModel.onAction(ViewImageAction.DismissDialog)
        waitUntil { viewModel.uiState.value.activeDialog == null }
        assertThat(viewModel.uiState.value.activeDialog).isNull()

        viewModel.onAction(ViewImageAction.ConfirmDelete(0))
        waitUntil { viewModel.uiState.value.images.size == 1 }
        assertThat(viewModel.uiState.value.images).hasSize(1)

        viewModel.onAction(ViewImageAction.BackClicked)
        val effect = viewModel.sideEffects.first()
        assertThat(effect).isInstanceOf(ViewImageSideEffect.NotifyImagesDeleted::class.java)
    }
}
