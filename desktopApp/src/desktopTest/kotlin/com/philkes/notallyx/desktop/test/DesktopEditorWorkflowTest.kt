package com.philkes.notallyx.desktop.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.Res
import com.philkes.notallyx.bold
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.edit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopEditorWorkflowTest : DesktopComposeUiTestBase() {

    @Test
    fun editExistingNote_updatesDatabase() = runDesktopAppTest {
        val noteId = insertNote(title = "Existing Note", body = "Original Content")

        waitForText("Existing Note")
        onNodeWithText("Existing Note", useUnmergedTree = true).performClick()
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).assertIsDisplayed()

        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInput(" Updated")
        onNodeWithTag(TestTags.COLOR_PICKER_BUTTON).performClick()
        onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.color == Color.CORAL.toColorString()
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals("Existing Note", note?.title)
            assertTrue(note?.body?.contains("Updated") == true)
            assertEquals(Color.CORAL.toColorString(), note?.color)
        }
    }

    @Test
    fun deleteNoteFromEditor_promptsDialogAndRemoves() = runDesktopAppTest {
        val noteId = insertNote(title = "Note to delete", body = "Will be deleted")

        waitForText("Note to delete")
        onNodeWithText("Note to delete", useUnmergedTree = true).performClick()
        onNodeWithTag(TestTags.DELETE_NOTE_BUTTON).performClick()

        onNodeWithTag("ConfirmDialog_Confirm").assertIsDisplayed()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()

        waitUntil(timeoutMillis = 5_000) { runBlocking { noteDao.get(noteId) == null } }

        runBlocking { assertNull(noteDao.get(noteId)) }

        onNodeWithTag(TestTags.NAV_NOTES).assertIsDisplayed()
    }

    @Test
    fun textFormatting_appliesStyles() = runDesktopAppTest {
        val noteId = insertNote(title = "Rich Note", body = "Formatting test")

        waitForText("Rich Note")
        onNodeWithText("Rich Note", useUnmergedTree = true).performClick()
        onNodeWithContentDescriptionRes(Res.string.edit).performClick()

        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInputSelection(TextRange(0, 10))
        onNodeWithContentDescriptionRes(Res.string.bold).performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.spans?.isNotEmpty() == true
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertTrue(
                "Expected bold span on range 0..10",
                note?.spans?.any { it.start == 0 && it.end == 10 && it.bold } == true,
            )
        }
    }
}
