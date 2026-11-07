package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.routes.archived.ArchivedRoute
import com.philkes.notallyx.feature.main.routes.deleted.DeletedRoute
import com.philkes.notallyx.test.CommonComposeUiTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ArchivedDeletedRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun archivedScreen_displaysNotesAndRestoresToNotesFolder() = runUiTest {
        val noteId =
            insertNote(title = "Archived Note", body = "Old content", folder = Folder.ARCHIVED)

        setContent {
            NotallyXTheme {
                ArchivedRoute(
                    noteDao = noteDao,
                    onBack = {},
                    onOpenNote = {},
                    onOpenList = {},
                )
            }
        }

        onNodeWithText("Archived Note").assertIsDisplayed()

        // Click restore (unarchive) button
        onNodeWithTag(TestTags.RESTORE_NOTE_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                val note = noteDao.get(noteId)
                note?.folder == Folder.NOTES
            }
        }

        runBlocking {
            val note = noteDao.get(noteId)
            assertEquals(Folder.NOTES, note?.folder)
        }
    }

    @Test
    fun deletedScreen_emptyTrash_permanentlyDeletesNotes() = runUiTest {
        val noteId1 = insertNote(title = "Trash 1", body = "Delete me", folder = Folder.DELETED)
        val noteId2 = insertNote(title = "Trash 2", body = "Delete me too", folder = Folder.DELETED)

        setContent {
            NotallyXTheme {
                DeletedRoute(
                    noteDao = noteDao,
                    onBack = {},
                )
            }
        }

        onNodeWithText("Trash 1").assertIsDisplayed()
        onNodeWithText("Trash 2").assertIsDisplayed()

        onNodeWithTag(TestTags.DELETE_ALL_BUTTON).performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("ConfirmDialog_Confirm").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("ConfirmDialog_Confirm").performClick()

        waitUntil(timeoutMillis = 5_000) {
            runBlocking { noteDao.getByFolder(Folder.DELETED).first().isEmpty() }
        }

        runBlocking {
            val deletedNotes = noteDao.getByFolder(Folder.DELETED).first()
            assertTrue(deletedNotes.isEmpty())
            assertEquals(null, noteDao.get(noteId1))
            assertEquals(null, noteDao.get(noteId2))
        }
    }
}
