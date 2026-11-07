package com.philkes.notallyx.desktop.test

import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopOsIntegrationTest : DesktopComposeUiTestBase() {

    @Test
    fun keyboardShortcuts_triggerActions() = runDesktopAppTest {
        // 1. Ctrl+N: Launches new note editor
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.N) } }

        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertIsDisplayed()
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).performTextInput("Shortcut Note")
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performTextInput("Saved with shortcut")

        // 2. Ctrl+S: Saves active note
        onNodeWithTag(TestTags.NOTE_BODY_INPUT).performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.S) }
        }

        waitUntil(timeoutMillis = 5_000) {
            runBlocking { noteDao.getByFolder(Folder.NOTES).first().isNotEmpty() }
        }

        runBlocking {
            val notes = noteDao.getByFolder(Folder.NOTES).first()
            val note = notes.firstOrNull { it.title == "Shortcut Note" }
            assertNotNull(note)
            assertEquals("Saved with shortcut", note?.body)
        }

        onNodeWithText("Shortcut Note", useUnmergedTree = true).assertIsDisplayed()

        // 3. Esc: Opens note and dismisses editor
        onNodeWithText("Shortcut Note", useUnmergedTree = true).performClick()
        onNodeWithTag(TestTags.NOTE_TITLE_INPUT).assertIsDisplayed()

        onRoot().performKeyInput { pressKey(Key.Escape) }

        onNodeWithTag(TestTags.NAV_NOTES).assertIsDisplayed()
    }

    @Test
    fun appExit_triggersDatabaseCheckpoint() = runBlocking {
        insertNote(title = "Checkpoint Note", body = "Flushing WAL")

        database.checkpoint()

        val notes = noteDao.getByFolder(Folder.NOTES).first()
        assertEquals(1, notes.size)
        assertEquals("Checkpoint Note", notes.first().title)
    }

    @Test
    fun windowState_dimensionPersistence() {
        val state =
            WindowState(
                size = DpSize(width = 900.dp, height = 700.dp),
                position = WindowPosition(Alignment.Center),
            )
        assertEquals(900.dp, state.size.width)
        assertEquals(700.dp, state.size.height)

        state.size = DpSize(width = 1200.dp, height = 800.dp)
        assertEquals(1200.dp, state.size.width)
        assertEquals(800.dp, state.size.height)
    }
}
