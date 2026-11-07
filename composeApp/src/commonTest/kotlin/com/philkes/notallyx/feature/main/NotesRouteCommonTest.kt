package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.feature.main.routes.notes.CommonNotesScreen
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NotesRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun emptyNotes_displaysEmptyState() = runUiTest {
        setContent {
            NotallyXTheme {
                CommonNotesScreen(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithTag(TestTags.EMPTY_NOTES).assertIsDisplayed()
        onNodeWithText("No notes yet").assertIsDisplayed()
    }

    @Test
    fun populatedNotes_displaysNotesGridAndOpensNoteOnCardClick() = runUiTest {
        val noteId1 = insertNote(title = "First Note", body = "Body 1")
        val noteId2 = insertNote(title = "Second Note", body = "Body 2")
        var openedNoteId: Long? = null

        setContent {
            NotallyXTheme {
                CommonNotesScreen(
                    noteDao = noteDao,
                    onOpenNote = { openedNoteId = it },
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithTag(TestTags.NOTES_GRID).assertIsDisplayed()
        onNodeWithText("First Note").assertIsDisplayed()
        onNodeWithText("Second Note").assertIsDisplayed()

        onNodeWithText("First Note").performClick()
        assertEquals(noteId1, openedNoteId)
    }

    @Test
    fun searchNotes_filtersGridAndClearsSearch() = runUiTest {
        insertNote(title = "Apple Pie Recipe", body = "Flour, apples, sugar")
        insertNote(title = "Banana Smoothie", body = "Bananas, yogurt")

        setContent {
            NotallyXTheme {
                CommonNotesScreen(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithText("Apple Pie Recipe").assertIsDisplayed()
        onNodeWithText("Banana Smoothie").assertIsDisplayed()

        // Filter by typing "Apple"
        onNodeWithTag(TestTags.SEARCH_INPUT).performTextInput("Apple")

        onNodeWithText("Apple Pie Recipe").assertIsDisplayed()
        assertEquals(1, onAllNodesWithTag(TestTags.NOTE_CARD).fetchSemanticsNodes().size)

        // Clear search
        onNodeWithTag(TestTags.SEARCH_CLEAR_BUTTON).performClick()

        onNodeWithText("Banana Smoothie").assertIsDisplayed()
        assertEquals(2, onAllNodesWithTag(TestTags.NOTE_CARD).fetchSemanticsNodes().size)
    }

    @Test
    fun navigationMenu_triggersOnOpenDrawer() = runUiTest {
        var drawerOpened = false

        setContent {
            NotallyXTheme {
                CommonNotesScreen(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = { drawerOpened = true },
                )
            }
        }

        onNodeWithTag(TestTags.NAVIGATION).performClick()
        assertTrue("Expected onOpenDrawer callback to be called", drawerOpened)
    }
}
