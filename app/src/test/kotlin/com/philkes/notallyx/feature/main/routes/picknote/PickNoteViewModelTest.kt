package com.philkes.notallyx.feature.main.routes.picknote

import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.SampleNotes
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
class PickNoteViewModelTest {

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
    fun testPickNoteActionsAndSideEffects() = runTest {
        val viewModel = PickNoteViewModel(preferences, pathProvider = pathProvider)

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
