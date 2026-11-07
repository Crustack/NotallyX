package com.philkes.notallyx.feature.media.image

import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.fakes.FakeFileStorage
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
class ViewImageViewModelTest {

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
    fun testViewImageActionsAndState() = runTest {
        val viewModel =
            ViewImageViewModel(
                preferences = preferences,
                pathProvider = pathProvider,
                fileStorage = FakeFileStorage(),
            )

        val key = NotallyNavKey.ViewImage(imageUris = listOf("uri1", "uri2"), initialIndex = 1)
        viewModel.onAction(ViewImageAction.Init(key))

        assertThat(viewModel.uiState.value.images).hasSize(2)
        assertThat(viewModel.uiState.value.initialPage).isEqualTo(1)

        viewModel.onAction(ViewImageAction.DeleteClicked(0))
        assertThat(viewModel.uiState.value.activeDialog)
            .isEqualTo(ViewImageDialogState.ConfirmDelete(0))

        viewModel.onAction(ViewImageAction.DismissDialog)
        assertThat(viewModel.uiState.value.activeDialog).isNull()

        viewModel.onAction(ViewImageAction.ConfirmDelete(0))
        assertThat(viewModel.uiState.value.images).hasSize(1)

        viewModel.onAction(ViewImageAction.BackClicked)
        val effect = viewModel.sideEffects.first()
        assertThat(effect).isInstanceOf(ViewImageSideEffect.NotifyImagesDeleted::class.java)
    }
}
