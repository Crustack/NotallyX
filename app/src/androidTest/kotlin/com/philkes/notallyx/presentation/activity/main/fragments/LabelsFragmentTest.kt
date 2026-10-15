package com.philkes.notallyx.activity.main.fragments

// import com.philkes.notallyx.R
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.navigateTo
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class LabelsFragmentTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun manageLabels() {
        // 1. Insert data
        runBlocking {
            database.getLabelDao().insert(listOf(Label("label", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Test",
                            body = "Body",
                            pinned = true,
                            labels = listOf("label"),
                        ),
                        createBaseNote(
                            title = "List",
                            pinned = true,
                            items =
                                listOf(createListItem("A"), createListItem("B", isChild = true)),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.navigateTo(R.id.Labels)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().size >= 2
        }
        composeTestRule.onAllNodes(hasText("label")).onLast().assertIsDisplayed()

        // Edit label
        composeTestRule
            .onAllNodes(hasContentDescription(context.getString(R.string.edit)))
            .onLast()
            .performClick()

        R.id.EditText.byId(withText("label")).inRoot(isDialog()).perform(replaceText("label1"))
        R.string.save
            .byText(withId(android.R.id.button1))
            .inRoot(isDialog())
            .perform(scrollTo(), click())

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label1")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(hasText("label1")).onLast().assertIsDisplayed()

        // Add label by clicking toolbar Add button
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_label))
            .performClick()

        R.id.EditText.byId().inRoot(isDialog()).perform(replaceText("label2"), closeSoftKeyboard())
        R.string.save
            .byText(withId(android.R.id.button1))
            .inRoot(isDialog())
            .perform(scrollTo(), click())

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label2")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(hasText("label2")).onLast().assertIsDisplayed()

        // Delete label "label1"
        composeTestRule
            .onAllNodes(hasContentDescription(context.getString(R.string.delete)))
            .onLast()
            .performClick()
        R.string.delete
            .byText(withId(android.R.id.button1))
            .inRoot(isDialog())
            .perform(scrollTo(), click())

        composeTestRule.onNodeWithText("label1").assertDoesNotExist()

        scenario.close()
    }

    @Test
    fun unlabeledNotesFilter() {
        // 1. Insert unlabeled and labeled notes
        runBlocking {
            database.getLabelDao().insert(listOf(Label("Work", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Unlabeled Note",
                            body = "Unlabeled Body",
                            labels = emptyList(),
                        ),
                        createBaseNote(
                            title = "Labeled Note",
                            body = "Labeled Body",
                            labels = listOf("Work"),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.navigateTo(R.id.Unlabeled)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Unlabeled Note")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Unlabeled Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Labeled Note").assertDoesNotExist()

        scenario.close()
    }

    @Test
    fun labelsNavigationFilter() {
        // 1. Insert multiple notes with different labels
        runBlocking {
            database.getLabelDao().insert(listOf(Label("Work", 0), Label("Personal", 1)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Work Note 1",
                            body = "Work Body 1",
                            labels = listOf("Work"),
                        ),
                        createBaseNote(
                            title = "Work Note 2",
                            body = "Work Body 2",
                            labels = listOf("Work"),
                        ),
                        createBaseNote(
                            title = "Personal Note 1",
                            body = "Personal Body 1",
                            labels = listOf("Personal"),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Open drawer and check if each label is displayed in navigation view
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.onAllNodes(hasText("Work"))[0].assertIsDisplayed()
        composeTestRule.onAllNodes(hasText("Personal"))[0].assertIsDisplayed()

        // Click on "Work" label in navigation view
        composeTestRule.onAllNodes(hasText("Work"))[0].performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Work Note 1")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Work Note 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work Note 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Personal Note 1").assertDoesNotExist()

        // Open drawer again and click on "Personal" label
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.onAllNodes(hasText("Personal"))[0].performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(hasText("Personal Note 1"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Personal Note 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work Note 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Work Note 2").assertDoesNotExist()

        scenario.close()
    }
}
