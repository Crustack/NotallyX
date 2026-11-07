package com.philkes.notallyx.feature.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.test.CommonComposeUiTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EditRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun createNewNote_savesTitleAndBodyToDatabase() = runUiTest {
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditNoteRoute(
                    noteId = 0L,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Grocery Planning")
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInput("Buy milk, bread, and apples.")

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        assertTrue("Expected onBack to be called after saving note", backCalled)

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            assertEquals(1, notes.size)
            val note = notes.first()
            assertEquals("Grocery Planning", note.title)
            assertEquals("Buy milk, bread, and apples.", note.body)
        }
    }

    @Test
    fun editExistingNote_displaysAndUpdatesContent() = runUiTest {
        val noteId = insertNote(title = "Initial Title", body = "Initial Body")
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditNoteRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertTextContains("Initial Title")
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).assertTextContains("Initial Body")

        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInput(" with appended text")
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        assertTrue("Expected onBack callback", backCalled)

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals("Initial Title", note?.title)
            assertTrue(note?.body?.contains("with appended text") == true)
        }
    }

    @Test
    fun deleteExistingNote_removesFromDatabaseAfterConfirmation() = runUiTest {
        val noteId = insertNote(title = "Note to delete", body = "Delete me")
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditNoteRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.DELETE_NOTE_BUTTON).performClick()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        assertTrue("Expected onBack to be called after deleting note", backCalled)

        runBlocking {
            val note = noteDao.get(noteId)
            assertEquals(null, note)
        }
    }
}
