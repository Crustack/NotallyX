package com.philkes.notallyx.activity.main.fragments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DeletedFragmentTest : UiTestBase() {

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
        composeTestRule.navigateTo(R.id.Deleted)

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
        onView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
            .inRoot(isDialog())
            .perform(scrollTo(), click())

        // 10. Verify "Test" is permanently deleted
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        scenario.close()
    }
}
