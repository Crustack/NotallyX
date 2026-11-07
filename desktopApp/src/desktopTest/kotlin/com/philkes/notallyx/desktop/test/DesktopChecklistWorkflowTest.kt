package com.philkes.notallyx.desktop.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.philkes.notallyx.Res
import com.philkes.notallyx.check_all_items
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.delete_checked_items
import com.philkes.notallyx.tap_for_more_options
import com.philkes.notallyx.uncheck_all_items
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopChecklistWorkflowTest : DesktopComposeUiTestBase() {

    @Test
    fun manageChecklistItems() = runDesktopAppTest {
        val noteId =
            insertChecklistNote(
                title = "Manage Checklist",
                itemBodies = listOf("Task Alpha", "Task Beta"),
            )

        waitForText("Manage Checklist")
        onNodeWithText("Manage Checklist", useUnmergedTree = true).performClick()
        onNode(hasTestTag(TestTags.CHECKLIST_ITEM_INPUT) and hasText("Task Alpha"))
            .assertIsDisplayed()
        onNode(hasTestTag(TestTags.CHECKLIST_ITEM_INPUT) and hasText("Task Beta"))
            .assertIsDisplayed()

        // Delete Task Beta
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_DELETE)[1].performClick()

        // Add Task Gamma
        onNodeWithTag(TestTags.CHECKLIST_ADD_BUTTON).performClick()
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_INPUT)[1].performTextInput("Task Gamma")

        // Toggle checkbox on Task Alpha
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_CHECKBOX)[0].performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.items?.size == 2 && note.items.any { it.body == "Task Gamma" }
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(2, note?.items?.size)
            assertEquals("Task Alpha", note?.items?.get(0)?.body)
            assertEquals(true, note?.items?.get(0)?.checked)
            assertEquals("Task Gamma", note?.items?.get(1)?.body)
            assertEquals(false, note?.items?.get(1)?.checked)
        }
    }

    @Test
    fun bulkChecklistActions() = runDesktopAppTest {
        val noteId =
            insertChecklistNote(
                title = "Bulk Checklist",
                itemBodies = listOf("Item 1", "Item 2", "Item 3"),
            )

        waitForText("Bulk Checklist")
        onNodeWithText("Bulk Checklist", useUnmergedTree = true).performClick()
        onNode(hasTestTag(TestTags.CHECKLIST_ITEM_INPUT) and hasText("Item 1")).assertIsDisplayed()

        // Check all
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        onNodeWithTextRes(Res.string.check_all_items).performClick()

        // Uncheck all
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        onNodeWithTextRes(Res.string.uncheck_all_items).performClick()

        // Check Item 1
        onAllNodesWithTag(TestTags.CHECKLIST_ITEM_CHECKBOX)[0].performClick()

        // Delete checked items
        onNodeWithContentDescriptionRes(Res.string.tap_for_more_options).performClick()
        onNodeWithTextRes(Res.string.delete_checked_items).performClick()

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.items?.size == 2
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertEquals(2, note?.items?.size)
            assertEquals(listOf("Item 2", "Item 3"), note?.items?.map { it.body })
        }
    }

    @Test
    fun checklistHierarchy() = runDesktopAppTest {
        val noteId =
            insertChecklistNote(
                title = "Hierarchy Checklist",
                itemBodies = listOf("Parent Item", "Child Item"),
            )

        waitForText("Hierarchy Checklist")
        onNodeWithText("Hierarchy Checklist", useUnmergedTree = true).performClick()
        onNode(hasTestTag(TestTags.CHECKLIST_ITEM_INPUT) and hasText("Child Item"))
            .performTouchInput { swipeRight() }

        onNodeWithTag(TestTags.SAVE_AND_CLOSE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.items?.getOrNull(1)?.isChild == true
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertNotNull(note)
            assertTrue("Expected second item to be child", note?.items?.get(1)?.isChild == true)
        }
    }
}
