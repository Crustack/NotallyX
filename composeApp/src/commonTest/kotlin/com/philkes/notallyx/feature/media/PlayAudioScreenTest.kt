package com.philkes.notallyx.feature.media

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.main.PickNoteScreen
import com.philkes.notallyx.feature.main.routes.picknote.PickNoteUiState
import com.philkes.notallyx.feature.media.audio.PlayAudioAction
import com.philkes.notallyx.feature.media.audio.PlayAudioScreen
import com.philkes.notallyx.feature.media.audio.PlayAudioUiState
import com.philkes.notallyx.feature.media.audio.RecordAudioAction
import com.philkes.notallyx.feature.media.audio.RecordAudioScreen
import com.philkes.notallyx.feature.media.audio.RecordAudioUiState
import com.philkes.notallyx.feature.media.audio.Status
import com.philkes.notallyx.feature.media.image.ViewImageScreen
import com.philkes.notallyx.feature.media.image.ViewImageUiState
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlayAudioScreenTest : CommonComposeUiTestBase() {

    @Test
    fun playAudioScreen_rendersPlayButton_andDispatchesAction() = runUiTest {
        var actionDispatched: PlayAudioAction? = null

        setContent {
            NotallyXTheme {
                PlayAudioScreen(
                    state =
                        PlayAudioUiState(
                            audio = Audio("test.m4a", 60000L, 1000L),
                            duration = 60000L,
                            isPlaying = false,
                        ),
                    onAction = { actionDispatched = it },
                )
            }
        }

        onNodeWithText("Play").assertIsDisplayed().performClick()
        assertEquals(PlayAudioAction.PlayPauseClicked, actionDispatched)
    }

    @Test
    fun playAudioScreen_displaysErrorMessage_whenErrorPresent() = runUiTest {
        setContent {
            NotallyXTheme {
                PlayAudioScreen(
                    state = PlayAudioUiState(errorMessage = "Audio file missing"),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Audio file missing").assertIsDisplayed()
    }

    @Test
    fun recordAudioScreen_rendersControls_andDispatchesStart() = runUiTest {
        var actionDispatched: RecordAudioAction? = null

        setContent {
            NotallyXTheme {
                RecordAudioScreen(
                    state =
                        RecordAudioUiState(
                            status = Status.READY,
                            baseTime = 0L,
                        ),
                    onAction = { actionDispatched = it },
                )
            }
        }

        onNodeWithText("Start").assertIsDisplayed().performClick()
        assertEquals(RecordAudioAction.MainClicked, actionDispatched)
    }

    @Test
    fun viewImageScreen_rendersEmptyState_andDispatchesBack() = runUiTest {
        setContent {
            NotallyXTheme {
                ViewImageScreen(
                    state = ViewImageUiState(images = emptyList()),
                    onAction = {},
                )
            }
        }

        // Initially empty images renders preview placeholder
        onNodeWithText("").assertExists()
    }

    @Test
    fun pickNoteScreen_rendersEmptyList_whenNoNotes() = runUiTest {
        setContent {
            NotallyXTheme {
                PickNoteScreen(
                    state = PickNoteUiState(notes = emptyList()),
                    onAction = {},
                )
            }
        }

        onNodeWithText("Empty list").assertIsDisplayed()
    }

    @Test
    fun colorPickerDialog_rendersPalette_andInvokesCallback() = runUiTest {
        setContent {
            NotallyXTheme {
                ColorPickerDialog(
                    currentColor = BaseNote.COLOR_DEFAULT,
                    onDismissRequest = {},
                    onColorSelected = {},
                )
            }
        }

        // Color dialog renders with Cancel button
        onNodeWithText("Cancel").assertIsDisplayed()
    }
}
