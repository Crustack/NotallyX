package com.philkes.notallyx.activity.main.routes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DeletedRouteTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun deleteNotes() {
        // 1. Insert data
        runBlocking {
            database.getLabelDao().insert(listOf(Label("label", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 2. Long click the note card "Test"
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }

        // 3. Click Delete action icon
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete))
            .performClick()

        // 4. Verify "Test" is no longer in Notes list
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        // 5. Navigate to Deleted folder
        composeTestRule.navigateTo(NotallyNavKey.Deleted)

        // 6. Verify "Test" note is displayed in Deleted list
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Test")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()

        // 7. Long click "Test" note in Deleted folder
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }

        // 8. Click "Delete forever" action icon
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete_forever))
            .performClick()

        // 9. Confirm in dialog
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()

        // 10. Verify "Test" is permanently deleted
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        scenario.close()
    }

    @Test
    fun emptyTrashBulkAction() {
        // 1. Insert multiple notes in Deleted folder
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(title = "Trash 1", body = "Body 1", folder = Folder.DELETED),
                        createBaseNote(title = "Trash 2", body = "Body 2", folder = Folder.DELETED),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 2. Navigate to Deleted folder
        composeTestRule.navigateTo(NotallyNavKey.Deleted)

        // 3. Verify both trashed notes are displayed
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Trash 1"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty() &&
                composeTestRule
                    .onAllNodes(hasText("Trash 2"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Trash 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trash 2").assertIsDisplayed()

        // 4. Long click first note to trigger action mode
        composeTestRule.onNodeWithText("Trash 1").performTouchInput { longClick() }

        // 5. Click "Select all" in action mode top bar
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.select_all))
            .performClick()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()

        // 6. Click "Delete forever" in action mode
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete_forever))
            .performClick()

        // 7. Confirm deletion dialog
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.delete)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()

        // 8. Verify all trashed notes are permanently deleted from UI
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Trash 1"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty() &&
                composeTestRule
                    .onAllNodes(hasText("Trash 2"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
        }
        composeTestRule.onNodeWithText("Trash 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Trash 2").assertDoesNotExist()

        // 9. Verify in database
        val remainingDeleted = runBlocking {
            database.getBaseNoteDao().getAll().filter { it.folder == Folder.DELETED }
        }
        assertTrue("All trashed notes should be deleted permanently", remainingDeleted.isEmpty())

        scenario.close()
    }
}
