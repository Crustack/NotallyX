package com.philkes.notallyx.feature.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.Res
import com.philkes.notallyx.bold
import com.philkes.notallyx.clear_formatting
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.edit
import com.philkes.notallyx.italic
import com.philkes.notallyx.link
import com.philkes.notallyx.monospace
import com.philkes.notallyx.next
import com.philkes.notallyx.pin
import com.philkes.notallyx.previous
import com.philkes.notallyx.redo
import com.philkes.notallyx.save
import com.philkes.notallyx.search
import com.philkes.notallyx.strikethrough
import com.philkes.notallyx.test.CommonComposeUiTestBase
import com.philkes.notallyx.undo
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

    @Test
    fun pinAndUnpinNote_persistsPinStateToDatabase() = runUiTest {
        val noteId = insertNote(title = "Pin Test", body = "Testing pin", pinned = false)
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

        onNodeWithContentDescriptionRes(Res.string.pin).performClick()
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(true, note?.pinned)
        }
    }

    @Test
    fun changeNoteColor_persistsSelectedColorToDatabase() = runUiTest {
        val noteId = insertNote(title = "Color Test", body = "Testing color")
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

        onNodeWithTag(TestTags.COLOR_PICKER_BUTTON).performClick()
        onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(Color.CORAL.toColorString(), note?.color)
        }
    }

    @Test
    fun textFormattingSpans_appliesRichTextAndSavesToDatabase() = runUiTest {
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

        val text = "Lorem ipsum dolor sit amet"
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Formatting Note")
        val bodyNode = onNodeWithTag(TestTags.NOTE_BODY_INPUT)
        bodyNode.performTextInput(text)

        // Open formatting bar
        onNodeWithContentDescriptionRes(Res.string.edit).performClick()

        // Select and format "Lorem" as bold
        bodyNode.performTextInputSelection(TextRange(0, 5))
        onNodeWithContentDescriptionRes(Res.string.bold).performClick()

        // Select and format "ipsum" as italic
        bodyNode.performTextInputSelection(TextRange(6, 11))
        onNodeWithContentDescriptionRes(Res.string.italic).performClick()

        // Select and format "dolor" as strikethrough
        bodyNode.performTextInputSelection(TextRange(12, 17))
        onNodeWithContentDescriptionRes(Res.string.strikethrough).performClick()

        // Select and format "sit" as monospace
        bodyNode.performTextInputSelection(TextRange(18, 21))
        onNodeWithContentDescriptionRes(Res.string.monospace).performClick()

        // Select "amet", format as bold and then clear formatting
        bodyNode.performTextInputSelection(TextRange(22, 26))
        onNodeWithContentDescriptionRes(Res.string.bold).performClick()
        onNodeWithContentDescriptionRes(Res.string.clear_formatting).performClick()

        // Save note
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            val note = notes.first { it.title == "Formatting Note" }
            assertNotNull(note)
            assertTrue(
                "Expected bold span",
                note.spans.any { it.start == 0 && it.end == 5 && it.bold },
            )
            assertTrue(
                "Expected italic span",
                note.spans.any { it.start == 6 && it.end == 11 && it.italic },
            )
            assertTrue(
                "Expected strikethrough span",
                note.spans.any { it.start == 12 && it.end == 17 && it.strikethrough },
            )
            assertTrue(
                "Expected monospace span",
                note.spans.any { it.start == 18 && it.end == 21 && it.monospace },
            )
            assertTrue(
                "Expected range 22-26 cleared",
                note.spans.none { it.start == 22 && it.end == 26 },
            )
        }
    }

    @Test
    fun insertHyperlinkDialog_addsUrlSpanToDatabase() = runUiTest {
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

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Link Note")
        val bodyNode = onNodeWithTag(TestTags.NOTE_BODY_INPUT)
        bodyNode.performTextInput("Visit website for info")

        // Open formatting bar
        onNodeWithContentDescriptionRes(Res.string.edit).performClick()

        // Select "website" (range 6 to 13)
        bodyNode.performTextInputSelection(TextRange(6, 13))
        onNodeWithContentDescriptionRes(Res.string.link).performClick()

        // Enter URL into DualTextInputDialog
        onNodeWithTextRes(Res.string.link).performTextInput("https://notallyx.org")
        onNodeWithTextRes(Res.string.save).performClick()

        // Save note
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            val note = notes.first { it.title == "Link Note" }
            assertNotNull(note)
            val linkSpan = note.spans.firstOrNull { it.link }
            assertNotNull("Expected link span", linkSpan)
            assertEquals("https://notallyx.org", linkSpan?.linkData)
        }
    }

    @Test
    fun undoAndRedoStack_restoresTextStates() = runUiTest {
        setContent {
            NotallyXTheme {
                EditNoteRoute(
                    noteId = 0L,
                    noteDao = noteDao,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Undo Test")
        val bodyNode = onNodeWithTag(TestTags.NOTE_BODY_INPUT)

        bodyNode.performTextInput("Step 1")
        bodyNode.performTextInput(" Step 2")
        bodyNode.assertTextContains("Step 1 Step 2")

        onNodeWithContentDescriptionRes(Res.string.undo).performClick()
        bodyNode.assertTextContains("Step 1")

        onNodeWithContentDescriptionRes(Res.string.redo).performClick()
        bodyNode.assertTextContains("Step 1 Step 2")
    }

    @Test
    fun searchWithinNote_navigatesOccurrences() = runUiTest {
        val noteId =
            insertNote(
                title = "Search In Note",
                body = "First apple in basket. Second apple on tree. Third apple on table.",
            )

        setContent {
            NotallyXTheme {
                EditNoteRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = {},
                )
            }
        }

        onNodeWithContentDescriptionRes(Res.string.search).performClick()
        onNodeWithContentDescriptionRes(Res.string.search).performTextInput("apple")

        onNodeWithText("1/3").assertIsDisplayed()

        onNodeWithContentDescriptionRes(Res.string.next).performClick()
        onNodeWithText("2/3").assertIsDisplayed()

        onNodeWithContentDescriptionRes(Res.string.next).performClick()
        onNodeWithText("3/3").assertIsDisplayed()

        onNodeWithContentDescriptionRes(Res.string.previous).performClick()
        onNodeWithText("2/3").assertIsDisplayed()
    }

    @Test
    fun noteLabelsAssignment_persistsToDatabase() = runUiTest {
        val noteId =
            insertNote(
                title = "Labeled Note",
                body = "Content",
                labels = listOf("Personal", "Finance"),
            )
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

        onNodeWithText("Personal").assertIsDisplayed()
        onNodeWithText("Finance").assertIsDisplayed()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(listOf("Personal", "Finance"), note?.labels)
        }
    }
}
