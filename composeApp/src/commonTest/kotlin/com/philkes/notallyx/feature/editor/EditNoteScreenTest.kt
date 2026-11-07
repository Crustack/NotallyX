package com.philkes.notallyx.feature.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EditNoteScreenTest : CommonComposeUiTestBase() {

    @Test
    fun editNoteScreen_rendersTitleAndBody() = runUiTest {
        val state =
            EditNoteUiState(
                title = "Meeting Notes",
                bodyValue = TextFieldValue("Discuss roadmap milestones."),
                color = BaseNote.COLOR_DEFAULT,
                type = Type.NOTE,
            )

        setContent {
            NotallyXTheme {
                EditNoteScreen(
                    state = state,
                    onAction = {},
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertTextContains("Meeting Notes")
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).assertTextContains("Discuss roadmap milestones.")
    }

    @Test
    fun editNoteScreen_onTitleChange_invokesAction() = runUiTest {
        var receivedTitle = ""
        val state =
            EditNoteUiState(
                title = "",
                bodyValue = TextFieldValue(""),
                color = BaseNote.COLOR_DEFAULT,
                type = Type.NOTE,
            )

        setContent {
            NotallyXTheme {
                EditNoteScreen(
                    state = state,
                    onAction = { action ->
                        if (action is EditNoteAction.TitleChanged) {
                            receivedTitle = action.title
                        }
                    },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("New Title")
        assertEquals("New Title", receivedTitle)
    }

    @Test
    fun editNoteScreen_topBarActions_rendersActionButtons() = runUiTest {
        var clickedAction: EditAction? = null
        val state =
            EditNoteUiState(
                title = "Title",
                bodyValue = TextFieldValue("Body"),
                color = BaseNote.COLOR_DEFAULT,
                type = Type.NOTE,
                preferences =
                    EditNotePreferences(
                        topActions =
                            listOf(EditAction.CHANGE_COLOR, EditAction.DELETE, EditAction.PIN)
                    ),
            )

        setContent {
            NotallyXTheme {
                EditNoteScreen(
                    state = state,
                    onAction = { action ->
                        if (action is EditNoteAction.ActionClicked) {
                            clickedAction = action.action
                        }
                    },
                )
            }
        }

        onNodeWithTag(TestTags.COLOR_PICKER_BUTTON).assertIsDisplayed()
        onNodeWithTag(TestTags.DELETE_NOTE_BUTTON).assertIsDisplayed()

        onNodeWithTag(TestTags.DELETE_NOTE_BUTTON).performClick()
        assertEquals(EditAction.DELETE, clickedAction)
    }

    @Test
    fun editNoteScreen_rendersActiveDialog() = runUiTest {
        var confirmed = false
        val state =
            EditNoteUiState(
                title = "Title",
                bodyValue = TextFieldValue("Body"),
                color = BaseNote.COLOR_DEFAULT,
                type = Type.NOTE,
                activeDialog =
                    EditorDialogState.DeleteForeverConfirm(onConfirm = { confirmed = true }),
            )

        setContent {
            NotallyXTheme {
                EditNoteScreen(
                    state = state,
                    onAction = {},
                )
            }
        }

        onNodeWithTag("ConfirmDialog_Confirm").assertIsDisplayed()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()
        assertTrue("Expected dialog confirmation to be invoked", confirmed)
    }
}
