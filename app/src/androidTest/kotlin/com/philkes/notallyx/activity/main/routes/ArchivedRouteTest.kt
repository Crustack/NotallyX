package com.philkes.notallyx.activity.main.routes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
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
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class ArchivedRouteTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun archiveNotes() {
        // 1. Insert data
        runBlocking {
            database.getBaseNoteDao().insert(listOf(createBaseNote(title = "Test", body = "Body")))
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 2. Long click the note card "Test"
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }

        // 3. Open overflow menu and click "Archive"
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.archive)).performClick()

        // 4. Verify "Test" is no longer displayed in Notes list
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        // 5. Navigate to Archived folder
        composeTestRule.navigateTo(NotallyNavKey.Archived)

        // 6. Verify "Test" note is displayed in Archived list
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Test")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()

        // 7. Long click the "Test" note in Archived folder
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }

        // 8. Click "Unarchive" action icon
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.unarchive))
            .performClick()

        // 9. Verify "Test" is no longer in Archived list
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        // 10. Navigate back to Notes folder
        composeTestRule.navigateTo(NotallyNavKey.Notes)

        // 11. Verify "Test" note is displayed in Notes list
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Test")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()

        scenario.close()
    }
}
