package com.philkes.notallyx.feature.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.philkes.notallyx.Res
import com.philkes.notallyx.check_all_items
import com.philkes.notallyx.convert_to_text_note
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.delete_checked_items
import com.philkes.notallyx.pin
import com.philkes.notallyx.redo
import com.philkes.notallyx.tap_for_more_options
import com.philkes.notallyx.test.CommonComposeUiTestBase
import com.philkes.notallyx.uncheck_all_items
import com.philkes.notallyx.undo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EditListRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun createNewList_addsItemsAndSavesToDatabase() = runUiTest {
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = 0L,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Weekend Shopping")
        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[0].performTextInput("Milk")

        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[1].performTextInput("Eggs")

        onNodeWithText("Milk").assertIsDisplayed()
        onNodeWithText("Eggs").assertIsDisplayed()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        assertTrue("Expected onBack to be called after saving checklist", backCalled)

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            assertEquals(1, notes.size)
            val note = notes.first()
            assertEquals("Weekend Shopping", note.title)
            assertEquals(Type.LIST, note.type)
            assertEquals(2, note.items.size)
            assertEquals("Milk", note.items[0].body)
            assertEquals("Eggs", note.items[1].body)
            assertEquals(false, note.items[0].checked)
            assertEquals(false, note.items[1].checked)
        }
    }

    @Test
    fun editExistingList_togglesItemAndDeletesItem() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "Packing List",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Passport",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        ),
                        ListItem(
                            id = 1,
                            body = "Extra Shoes",
                            checked = false,
                            isChild = false,
                            order = 1,
                            children = mutableListOf(),
                        ),
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithText("Passport").assertIsDisplayed()
        onNodeWithText("Extra Shoes").assertIsDisplayed()

        // Delete "Extra Shoes" using test tag
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_DELETE)[1].performClick()

        // Toggle checkbox for the remaining item (Passport)
        onNodeWithTag(TestTags.CHECKLIST_ITEM_CHECKBOX).performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        assertTrue("Expected onBack to be called after editing checklist", backCalled)

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(1, note?.items?.size)
            assertEquals("Passport", note?.items?.first()?.body)
            assertEquals(true, note?.items?.first()?.checked)
        }
    }

    @Test
    fun deleteExistingList_removesFromDatabaseAfterConfirmation() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "Checklist to delete",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Item 1",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        )
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.DELETE_NOTE_BUTTON).performClick()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        assertTrue("Expected onBack to be called after deleting checklist", backCalled)

        runBlocking {
            val note = noteDao.get(noteId)
            assertEquals(null, note)
        }
    }

    @Test
    fun pinAndColorChecklist_persistsToDatabase() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "Shopping List",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Milk",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        )
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertTextContains("Shopping List")
        onNodeWithContentDescriptionRes(Res.string.pin).performClick()
        onNodeWithTag(TestTags.COLOR_PICKER_BUTTON).performClick()
        onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(true, note?.pinned)
            assertEquals(Color.CORAL.toColorString(), note?.color)
        }
    }

    @Test
    fun checklistBulkActions_checkAllUncheckAllDeleteChecked() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "Bulk Actions List",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Alpha",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        ),
                        ListItem(
                            id = 1,
                            body = "Beta",
                            checked = false,
                            isChild = false,
                            order = 1,
                            children = mutableListOf(),
                        ),
                        ListItem(
                            id = 2,
                            body = "Gamma",
                            checked = false,
                            isChild = false,
                            order = 2,
                            children = mutableListOf(),
                        ),
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithText("Alpha").assertIsDisplayed()

        // Check all
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.check_all_items).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTextRes(Res.string.check_all_items).performScrollTo().performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.check_all_items).fetchSemanticsNodes().isEmpty()
        }

        // Uncheck all
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.uncheck_all_items).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTextRes(Res.string.uncheck_all_items).performScrollTo().performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.uncheck_all_items).fetchSemanticsNodes().isEmpty()
        }

        // Check Alpha only
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_CHECKBOX)[0].performClick()

        // Delete checked items
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.delete_checked_items)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNodeWithTextRes(Res.string.delete_checked_items).performScrollTo().performClick()
        waitUntil {
            onAllNodesWithTextRes(Res.string.delete_checked_items).fetchSemanticsNodes().isEmpty()
        }

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(2, note?.items?.size)
            assertEquals(listOf("Beta", "Gamma"), note?.items?.map { it.body })
        }
    }

    @Test
    fun parentChildIndentation_swipesIndentAndOutdent() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "Indentation List",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Parent Item",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        ),
                        ListItem(
                            id = 1,
                            body = "Child Item",
                            checked = false,
                            isChild = false,
                            order = 1,
                            children = mutableListOf(),
                        ),
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithText("Child Item").performTouchInput { swipeRight() }

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(true, note?.items?.get(1)?.isChild)
        }
    }

    @Test
    fun deepUndoRedoStack_rollsBackItemModifications() = runUiTest {
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = 0L,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Deep Undo List")

        for (i in 1..4) {
            onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
            onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[i - 1].performTextInput("Item $i")
        }

        onNodeWithText("Item 1").assertIsDisplayed()
        onNodeWithText("Item 4").assertIsDisplayed()

        // Undo 2 item creations
        onNodeWithContentDescriptionRes(Res.string.undo).performClick()
        onNodeWithContentDescriptionRes(Res.string.undo).performClick()

        // Redo 1 item creation
        onNodeWithContentDescriptionRes(Res.string.redo).performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            val note = notes.first { it.title == "Deep Undo List" }
            assertNotNull(note)
            assertTrue("Expected at least 1 item", note.items.isNotEmpty())
        }
    }

    @Test
    fun convertListToTextNote_producesMarkdownChecklist() = runUiTest {
        val now = System.currentTimeMillis()
        val listNote =
            BaseNote(
                id = 0L,
                type = Type.LIST,
                folder = Folder.NOTES,
                color = BaseNote.COLOR_DEFAULT,
                title = "List to Convert",
                pinned = false,
                timestamp = now,
                modifiedTimestamp = now,
                labels = emptyList(),
                body = "",
                spans = emptyList(),
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Buy coffee",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        ),
                        ListItem(
                            id = 1,
                            body = "Brew coffee",
                            checked = true,
                            isChild = true,
                            order = 1,
                            children = mutableListOf(),
                        ),
                    ),
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        val noteId = runBlocking { noteDao.insert(listNote) }
        var backCalled = false

        setContent {
            NotallyXTheme {
                EditListRoute(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithText("Buy coffee").assertIsDisplayed()

        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        onNodeWithTextRes(Res.string.convert_to_text_note).performClick()

        waitUntil(timeoutMillis = 5_000) { backCalled }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(Type.NOTE, note?.type)
            assertTrue(
                "Expected unchecked markdown",
                note?.body?.contains("- [ ] Buy coffee") == true,
            )
            assertTrue(
                "Expected checked markdown with indentation",
                note?.body?.contains("    - [x] Brew coffee") == true,
            )
        }
    }
}
