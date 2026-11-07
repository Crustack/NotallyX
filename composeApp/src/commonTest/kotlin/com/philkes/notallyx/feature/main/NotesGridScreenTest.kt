package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.feature.labels.LabelData
import com.philkes.notallyx.feature.labels.LabelsAction
import com.philkes.notallyx.feature.labels.LabelsScreen
import com.philkes.notallyx.feature.labels.LabelsUiState
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.feature.main.routes.archived.ArchivedScreen
import com.philkes.notallyx.feature.main.routes.archived.ArchivedUiState
import com.philkes.notallyx.feature.main.routes.deleted.DeletedDialogState
import com.philkes.notallyx.feature.main.routes.deleted.DeletedScreen
import com.philkes.notallyx.feature.main.routes.deleted.DeletedUiState
import com.philkes.notallyx.feature.main.routes.notes.NotesAction
import com.philkes.notallyx.feature.main.routes.notes.NotesUiState
import com.philkes.notallyx.feature.main.routes.search.SearchAction
import com.philkes.notallyx.feature.main.routes.search.SearchScreen
import com.philkes.notallyx.feature.main.routes.search.SearchUiState
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NotesGridScreenTest : CommonComposeUiTestBase() {

    @Test
    fun notesGridScreen_rendersEmptyState() = runUiTest {
        setContent {
            NotallyXTheme {
                NotesGridScreen(
                    state = NotesUiState(items = emptyList()),
                    onAction = {},
                )
            }
        }

        onNodeWithTag(TestTags.EMPTY_NOTES).assertIsDisplayed()
        onNodeWithText("No notes yet").assertIsDisplayed()
    }

    @Test
    fun notesGridScreen_rendersNotesAndDispatchesClick() = runUiTest {
        val note = SampleNotes.LOREM_IPSUM_NOTE
        var clickedNoteId: Long? = null

        setContent {
            NotallyXTheme {
                NotesGridScreen(
                    state = NotesUiState(items = listOf(note), isGrid = true),
                    onAction = { action ->
                        if (action is NotesAction.NoteClicked) {
                            clickedNoteId = action.note.id
                        }
                    },
                )
            }
        }

        onNodeWithTag(TestTags.NOTES_GRID).assertIsDisplayed()
        onNodeWithText(note.title).assertIsDisplayed()

        onNodeWithText(note.title).performClick()
        assertEquals(note.id, clickedNoteId)
    }

    @Test
    fun searchScreen_rendersSearchBarAndDispatchesKeywordChange() = runUiTest {
        var newKeyword: String? = null
        val state =
            SearchUiState(
                items = listOf(SampleNotes.LOREM_IPSUM_NOTE),
                searchKeyword = "",
                isGrid = true,
            )

        setContent {
            NotallyXTheme {
                SearchScreen(
                    state = state,
                    onAction = { action ->
                        if (action is SearchAction.KeywordChanged) {
                            newKeyword = action.keyword
                        }
                    },
                )
            }
        }

        onNodeWithTag(TestTags.SEARCH_INPUT).assertIsDisplayed()
        onNodeWithTag(TestTags.SEARCH_INPUT).performTextInput("meeting")
        assertEquals("meeting", newKeyword)
    }

    @Test
    fun labelsScreen_rendersLabelsAndDispatchesClick() = runUiTest {
        var clickedLabel: String? = null
        val labelData = LabelData(value = "Work", visibleInNavigation = true, order = 0)
        val state = LabelsUiState(labels = listOf(labelData))

        setContent {
            NotallyXTheme {
                LabelsScreen(
                    state = state,
                    onAction = { action ->
                        if (action is LabelsAction.LabelClicked) {
                            clickedLabel = action.label.value
                        }
                    },
                )
            }
        }

        onNodeWithText("Work").assertIsDisplayed()
        onNodeWithTag(TestTags.LABEL_ITEM).performClick()
        assertEquals("Work", clickedLabel)
    }

    @Test
    fun archivedScreen_rendersArchivedNotes() = runUiTest {
        val note = SampleNotes.LOREM_IPSUM_NOTE
        val state = ArchivedUiState(items = listOf(note), isGrid = true)

        setContent {
            NotallyXTheme {
                ArchivedScreen(
                    state = state,
                    onAction = {},
                )
            }
        }

        onNodeWithTag(TestTags.NOTES_GRID).assertIsDisplayed()
        onNodeWithText(note.title).assertIsDisplayed()
    }

    @Test
    fun deletedScreen_rendersDeletedNotesAndConfirmDialog() = runUiTest {
        val state =
            DeletedUiState(
                items = listOf(SampleNotes.LOREM_IPSUM_NOTE),
                activeDialog = DeletedDialogState.ConfirmDeleteAll,
            )

        setContent {
            NotallyXTheme {
                DeletedScreen(
                    state = state,
                    onAction = {},
                )
            }
        }

        onNodeWithTag("ConfirmDialog_Confirm").assertIsDisplayed()
    }
}
