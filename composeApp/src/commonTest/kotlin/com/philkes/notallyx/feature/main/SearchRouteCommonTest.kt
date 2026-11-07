package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.search.CommonSearchScreen
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SearchRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun searchScreen_filtersNotesByKeyword() = runUiTest {
        insertNote(title = "Meeting Notes", body = "Discussion about Q4 goals")
        insertNote(title = "Grocery List", body = "Tomatoes, onions")

        setContent {
            NotallyXTheme {
                CommonSearchScreen(
                    noteDao = noteDao,
                    onBack = {},
                    onOpenNote = {},
                    onOpenList = {},
                )
            }
        }

        onNodeWithText("Meeting Notes").assertIsDisplayed()
        onNodeWithText("Grocery List").assertIsDisplayed()

        // Filter by typing "Meeting"
        onNodeWithTag(TestTags.SEARCH_INPUT).performTextInput("Meeting")

        onNodeWithText("Meeting Notes").assertIsDisplayed()
        assertEquals(1, onAllNodesWithTag(TestTags.NOTE_CARD).fetchSemanticsNodes().size)

        // Clear search
        onNodeWithTag(TestTags.SEARCH_CLEAR_BUTTON).performClick()

        assertEquals(2, onAllNodesWithTag(TestTags.NOTE_CARD).fetchSemanticsNodes().size)
    }

    @Test
    fun searchScreen_switchesFolders() = runUiTest {
        insertNote(title = "Active Note", body = "Notes folder", folder = Folder.NOTES)
        insertNote(title = "Archived Note", body = "Archived folder", folder = Folder.ARCHIVED)

        setContent {
            NotallyXTheme {
                CommonSearchScreen(
                    noteDao = noteDao,
                    onBack = {},
                    onOpenNote = {},
                    onOpenList = {},
                )
            }
        }

        onNodeWithText("Active Note").assertIsDisplayed()

        // Switch to Archived folder chip
        onNodeWithText("Archived").performClick()
        waitUntilAtLeastOneExists(hasText("Archived Note"))
        onNodeWithText("Archived Note").assertIsDisplayed()
    }
}
