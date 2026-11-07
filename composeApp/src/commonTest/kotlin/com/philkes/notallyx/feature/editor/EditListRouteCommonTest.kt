package com.philkes.notallyx.feature.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.CommonComposeUiTestBase
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
                CommonEditListScreen(
                    noteId = 0L,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Weekend Shopping")
        onNodeWithTag(TestTags.CHECKLIST_NEW_ITEM_INPUT).performTextInput("Milk")
        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()

        onNodeWithTag(TestTags.CHECKLIST_NEW_ITEM_INPUT).performTextInput("Eggs")
        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()

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
                            body = "Passport",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                        ),
                        ListItem(
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
                CommonEditListScreen(
                    noteId = noteId,
                    noteDao = noteDao,
                    onBack = { backCalled = true },
                )
            }
        }

        onNodeWithText("Passport").assertIsDisplayed()
        onNodeWithText("Extra Shoes").assertIsDisplayed()

        // Toggle checkbox for Passport
        onNodeWithContentDescription("Passport").performClick()

        // Delete "Extra Shoes" - find delete icon button (contentDescription = "Delete")
        onAllNodesWithContentDescription("Delete")[1].performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

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
                CommonEditListScreen(
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
}
