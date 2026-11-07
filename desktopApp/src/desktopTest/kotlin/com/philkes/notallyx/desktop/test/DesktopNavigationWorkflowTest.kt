package com.philkes.notallyx.desktop.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.philkes.notallyx.core.ui.views.TestTags
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopNavigationWorkflowTest : DesktopComposeUiTestBase() {

    @Test
    fun drawerNavigation_switchesToLabelsRoute() = runDesktopAppTest {
        onNodeWithTag(TestTags.NAVIGATION).performClick()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Labels")).performClick()

        onNodeWithTag(TestTags.NAV_LABELS).assertIsDisplayed()
        onNodeWithText("No labels yet").assertIsDisplayed()
    }

    @Test
    fun drawerNavigation_switchesToArchiveAndTrash() = runDesktopAppTest {
        insertArchivedNote(title = "Old Archived Note", body = "Archived Content")
        insertDeletedNote(title = "Old Trash Note", body = "Trash Content")

        // Switch to Archive
        onNodeWithTag(TestTags.NAVIGATION).performClick()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Archive"))
            .performClick()

        onNodeWithTag(TestTags.NAV_ARCHIVED).assertIsDisplayed()
        waitForText("Old Archived Note")
        onNodeWithText("Old Archived Note", useUnmergedTree = true).assertIsDisplayed()

        // Switch to Trash
        onNodeWithContentDescription("Back").performClick()
        onNodeWithTag(TestTags.NAVIGATION).performClick()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Trash")).performClick()

        onNodeWithTag(TestTags.NAV_DELETED).assertIsDisplayed()
        waitForText("Old Trash Note")
        onNodeWithText("Old Trash Note", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun drawerNavigation_switchesToSettingsRoute() = runDesktopAppTest {
        onNodeWithTag(TestTags.NAVIGATION).performClick()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Settings"))
            .performClick()

        onNodeWithTag(TestTags.NAV_SETTINGS).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()

        // Navigate back to Notes
        onNodeWithContentDescription("Back").performClick()
        onNodeWithTag(TestTags.NAV_NOTES).assertIsDisplayed()
    }

    @Test
    fun adaptiveListDetail_twoPaneLayout() = runDesktopAppTest {
        insertNote(title = "Two Pane Note", body = "Testing side by side display")

        waitForText("Two Pane Note")
        onNodeWithText("Two Pane Note", useUnmergedTree = true).assertIsDisplayed()

        // Open note from list pane
        onNodeWithText("Two Pane Note", useUnmergedTree = true).performClick()

        // On desktop expanded layout, detail editor opens without displacing list pane
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertIsDisplayed()
        onNodeWithTag(TestTags.NAV_NOTES).assertIsDisplayed()
    }
}
