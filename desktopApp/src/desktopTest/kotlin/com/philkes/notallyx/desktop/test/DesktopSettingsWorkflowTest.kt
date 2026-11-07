package com.philkes.notallyx.desktop.test

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.dark
import com.philkes.notallyx.desktop.DesktopPreferencesStore
import com.philkes.notallyx.feature.settings.PreferencesUiModel
import com.philkes.notallyx.save
import com.philkes.notallyx.super_dark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopSettingsWorkflowTest : DesktopComposeUiTestBase() {

    @Test
    fun theme_changingThemeInSettingsUI_updatesPreferencesAndDisplay() = runDesktopAppTest {
        // Open navigation drawer and navigate to Settings
        onNodeWithTag(TestTags.NAVIGATION).performClick()
        onNode(hasAnyAncestor(hasTestTag(TestTags.NAV_DRAWER)) and hasText("Settings"))
            .performClick()

        onNodeWithTag(TestTags.NAV_SETTINGS).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()

        // Open Theme dialog
        onNodeWithTag(TestTags.SETTINGS_THEME).performClick()
        onNodeWithTextRes(Res.string.dark).assertIsDisplayed()

        // Select Dark theme and confirm
        onNodeWithTextRes(Res.string.dark).performClick()
        onNodeWithTextRes(Res.string.save).performClick()

        // Verify preferences were updated in store
        assertEquals(Theme.DARK, preferencesStore.preferences.value.theme)

        // Change to Super Dark
        onNodeWithTag(TestTags.SETTINGS_THEME).performClick()
        onNodeWithTextRes(Res.string.super_dark).performClick()
        onNodeWithTextRes(Res.string.save).performClick()

        assertEquals(Theme.SUPER_DARK, preferencesStore.preferences.value.theme)

        // Navigate back to Notes
        onNodeWithContentDescription("Back").performClick()
        onNodeWithTag(TestTags.EMPTY_NOTES).assertIsDisplayed()
    }

    @Test
    fun maxItems_changingPreference_updatesChecklistOverviewDisplay() = runDesktopAppTest {
        insertChecklistNote(
            title = "Checklist Tasks",
            itemBodies = listOf("Task 1", "Task 2", "Task 3", "Task 4", "Task 5"),
        )

        // Default maxItems is 4
        waitForText("Checklist Tasks")
        onNodeWithText("Task 1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 2", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 3", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 4", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 5").assertDoesNotExist()

        // Update maxItems to 2
        preferencesStore.updatePreferences { it.copy(maxItems = 2) }

        onNodeWithText("Task 1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 2", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 3").assertDoesNotExist()
        onNodeWithText("Task 4").assertDoesNotExist()
        onNodeWithText("Task 5").assertDoesNotExist()

        // Update maxItems to 5
        preferencesStore.updatePreferences { it.copy(maxItems = 5) }

        onNodeWithText("Task 1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 2", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 3", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 4", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Task 5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun maxLines_changingPreference_updatesNoteBodyDisplay() = runDesktopAppTest {
        val multilineBody = "First Line\nSecond Line\nThird Line\nFourth Line"
        insertNote(title = "Multiline Note", body = multilineBody)

        waitForText("Multiline Note")
        onNodeWithText(multilineBody, useUnmergedTree = true).assertIsDisplayed()

        // Setting maxLines to 0 hides note body in overview when note has a title
        preferencesStore.updatePreferences { it.copy(maxLines = 0) }

        onNodeWithText("Multiline Note", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText(multilineBody).assertDoesNotExist()

        // Restoring maxLines restores note body in overview
        preferencesStore.updatePreferences { it.copy(maxLines = 8) }

        onNodeWithText(multilineBody, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun textSizeOverview_changingPreference_updatesNoteCardTextSize() = runDesktopAppTest {
        insertNote(title = "Scalable Title Note", body = "Body Text")

        waitForText("Scalable Title Note")

        val initialLayouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Scalable Title Note", useUnmergedTree = true).performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) {
            it(initialLayouts)
        }

        assertEquals(1, initialLayouts.size)
        val initialFontSize = initialLayouts.first().layoutInput.style.fontSize
        assertEquals(16.sp, initialFontSize)

        // Update textSizeOverview to 24f
        preferencesStore.updatePreferences { it.copy(textSizeOverview = 24f) }

        val updatedLayouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Scalable Title Note", useUnmergedTree = true).performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) {
            it(updatedLayouts)
        }

        assertEquals(1, updatedLayouts.size)
        val updatedFontSize = updatedLayouts.first().layoutInput.style.fontSize
        assertEquals(24.sp, updatedFontSize)
        assertTrue(updatedFontSize.value > initialFontSize.value)
    }

    @Test
    fun settingsPreferences_persistAcrossStoreRestarts() {
        val testPrefs =
            PreferencesUiModel(
                theme = Theme.SUPER_DARK,
                maxItems = 3,
                maxLines = 2,
                textSizeOverview = 22f,
                notesView = NotesView.GRID,
            )

        preferencesStore.setPreferences(testPrefs)

        // Instantiate a second store pointing to the same pathProvider
        val secondStore = DesktopPreferencesStore(pathProvider)

        val loaded = secondStore.preferences.value
        assertEquals(Theme.SUPER_DARK, loaded.theme)
        assertEquals(3, loaded.maxItems)
        assertEquals(2, loaded.maxLines)
        assertEquals(22f, loaded.textSizeOverview)
        assertEquals(NotesView.GRID, loaded.notesView)
    }
}
