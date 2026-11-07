package com.philkes.notallyx.desktop.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.Res
import com.philkes.notallyx.ascending
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.make_list
import com.philkes.notallyx.save
import com.philkes.notallyx.take_note
import com.philkes.notallyx.title
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopNotesWorkflowTest : DesktopComposeUiTestBase() {

    @Test
    fun launchDesktopApp_displaysNotesAndDrawer() = runDesktopAppTest {
        onNodeWithTag(TestTags.EMPTY_NOTES).assertIsDisplayed()
        onNodeWithText("No notes yet").assertIsDisplayed()

        // Open navigation drawer
        onNodeWithTag(TestTags.NAVIGATION).performClick()

        onNodeWithTag(TestTags.NAV_DRAWER).assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Notes"))
            .assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Search"))
            .assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Labels"))
            .assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Archive"))
            .assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Trash"))
            .assertIsDisplayed()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Settings"))
            .assertIsDisplayed()
    }

    @Test
    fun createNote_navigatesToDetailPaneAndSaves() = runDesktopAppTest {
        onNodeWithContentDescriptionRes(Res.string.take_note).performClick()

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertIsDisplayed()
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Desktop Note Title")
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInput("Desktop Note Body")

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking { noteDao.getByFolder(Folder.NOTES).first().isNotEmpty() }
        }

        onNodeWithText("Desktop Note Title", useUnmergedTree = true).assertIsDisplayed()

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            assertEquals(1, notes.size)
            val note = notes.first()
            assertEquals("Desktop Note Title", note.title)
            assertEquals("Desktop Note Body", note.body)
        }
    }

    @Test
    fun createChecklist_addsItemsAndToggles() = runDesktopAppTest {
        onNodeWithContentDescriptionRes(Res.string.make_list).performClick()

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertIsDisplayed()
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Desktop Tasks")

        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[0].performTextInput("Task 1")

        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[1].performTextInput("Task 2")

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking { noteDao.getByFolder(Folder.NOTES).first().isNotEmpty() }
        }

        onNodeWithText("Desktop Tasks", useUnmergedTree = true).assertIsDisplayed()

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            val listNote = notes.firstOrNull { it.title == "Desktop Tasks" }
            assertNotNull(listNote)
            assertEquals(Type.LIST, listNote?.type)
            assertEquals(2, listNote?.items?.size)
            assertEquals("Task 1", listNote?.items?.get(0)?.body)
            assertEquals("Task 2", listNote?.items?.get(1)?.body)
        }
    }

    @Test
    fun searchInNotes_filtersGrid() = runDesktopAppTest {
        insertNote(title = "Shopping List", body = "Apples, Milk")
        insertNote(title = "Work Meeting", body = "Sprint planning")

        waitForText("Shopping List")
        waitForText("Work Meeting")

        onNodeWithText("Shopping List", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Work Meeting", useUnmergedTree = true).assertIsDisplayed()

        onNodeWithTag(TestTags.SEARCH_INPUT).performTextInput("Meeting")

        onNodeWithText("Work Meeting", useUnmergedTree = true).assertIsDisplayed()
        onAllNodesWithTag(TestTags.NOTE_CARD).assertCountEquals(1)

        onNodeWithTag(TestTags.SEARCH_CLEAR_BUTTON).performClick()

        waitForText("Shopping List")
        onNodeWithText("Shopping List", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Work Meeting", useUnmergedTree = true).assertIsDisplayed()
        onAllNodesWithTag(TestTags.NOTE_CARD).assertCountEquals(2)
    }

    @Test
    fun sortNotes_changesGridOrder() = runDesktopAppTest {
        insertNote(title = "Zebra", body = "Zebra details")
        insertNote(title = "Apple", body = "Apple details")
        insertNote(title = "Mango", body = "Mango details")

        waitForText("Zebra")
        waitForText("Apple")
        waitForText("Mango")

        onNodeWithText("Zebra", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Apple", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Mango", useUnmergedTree = true).assertIsDisplayed()

        onNodeWithTag(TestTags.SETTINGS_SORTING).performClick()
        onNodeWithTextRes(Res.string.title).performClick()
        onNodeWithTextRes(Res.string.ascending).performClick()
        onNodeWithTextRes(Res.string.save).performClick()

        onAllNodesWithTag(TestTags.NOTE_CARD).assertCountEquals(3)
        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            assertEquals(3, notes.size)
        }
    }
}
