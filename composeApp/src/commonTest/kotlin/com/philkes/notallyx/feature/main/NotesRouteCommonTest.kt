package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.philkes.notallyx.Res
import com.philkes.notallyx.ascending
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.notes.NotesRoute
import com.philkes.notallyx.grid
import com.philkes.notallyx.list
import com.philkes.notallyx.save
import com.philkes.notallyx.test.CommonComposeUiTestBase
import com.philkes.notallyx.title
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NotesRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun emptyNotes_displaysEmptyState() = runUiTest {
        setContent {
            NotallyXTheme {
                NotesRoute(
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
                NotesRoute(
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
                NotesRoute(
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
                NotesRoute(
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

    @Test
    fun checklistNote_opensListOnCardClick() = runUiTest {
        val listId =
            insertChecklistNote(
                title = "Weekend Tasks",
                itemBodies = listOf("Wash car", "Buy groceries"),
            )
        var openedListId: Long? = null

        setContent {
            NotallyXTheme {
                NotesRoute(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = { openedListId = it },
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithText("Weekend Tasks").assertIsDisplayed()
        onNodeWithText("Weekend Tasks").performClick()
        assertEquals(listId, openedListId)
    }

    @Test
    fun searchNotes_matchesChecklistItemContent() = runUiTest {
        insertChecklistNote(
            title = "Pantry Inventory",
            itemBodies = listOf("Olive Oil", "Cheddar Cheese"),
        )
        insertNote(title = "General Note", body = "Some unrelated body text")

        setContent {
            NotallyXTheme {
                NotesRoute(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithTag(TestTags.SEARCH_INPUT).performTextInput("Cheddar")
        onNodeWithText("Pantry Inventory").assertIsDisplayed()
        assertEquals(1, onAllNodesWithTag(TestTags.NOTE_CARD).fetchSemanticsNodes().size)
    }

    @Test
    fun sortNotesDialog_sortsNotesByTitle() = runUiTest {
        insertNote(title = "Zebra Note", body = "Zebra content")
        insertNote(title = "Apple Note", body = "Apple content")
        insertNote(title = "Mango Note", body = "Mango content")

        setContent {
            NotallyXTheme {
                NotesRoute(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_SORTING).performClick()
        onNodeWithTextRes(Res.string.title).performClick()
        onNodeWithTextRes(Res.string.ascending).performClick()
        onNodeWithTextRes(Res.string.save).performClick()

        val cards = onAllNodesWithTag(TestTags.NOTE_CARD)
        assertEquals(3, cards.fetchSemanticsNodes().size)
    }

    @Test
    fun toggleViewMode_switchesBetweenGridAndList() = runUiTest {
        insertNote(title = "Test Note", body = "Test Body")

        setContent {
            NotallyXTheme {
                NotesRoute(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        // Toggle from Grid to List
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).performClick()
        onNodeWithContentDescriptionRes(Res.string.grid).assertIsDisplayed()

        // Toggle from List to Grid
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).performClick()
        onNodeWithContentDescriptionRes(Res.string.list).assertIsDisplayed()
    }

    @Test
    fun multiSelectBulkActions_archiveNotes() = runUiTest {
        val id1 = insertNote(title = "Bulk Note 1", body = "Body 1")
        val id2 = insertNote(title = "Bulk Note 2", body = "Body 2")

        setContent {
            NotallyXTheme {
                NotesRoute(
                    noteDao = noteDao,
                    onOpenNote = {},
                    onOpenList = {},
                    onOpenDrawer = {},
                )
            }
        }

        onNodeWithText("Bulk Note 1").performTouchInput { longClick() }
        onNodeWithText("Bulk Note 2").performClick()

        onNodeWithText("2").assertIsDisplayed()

        // Click Archive in selection action bar
        onNodeWithTag(TestTags.NAV_ARCHIVED).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking { noteDao.getByFolder(Folder.NOTES).first().isEmpty() }
        }

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            assertEquals(0, notes.size)
            val archived = noteDao.getByFolder(Folder.ARCHIVED).first()
            assertEquals(2, archived.size)
            assertTrue(archived.any { it.id == id1 })
            assertTrue(archived.any { it.id == id2 })
        }
    }
}
