package com.philkes.notallyx.activity.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditListActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private fun itemCheckbox(text: String) = hasContentDescription(text) and isToggleable()

    private fun assertIsAbove(topText: String, bottomText: String) {
        val topY =
            composeTestRule
                .onNodeWithText(topText)
                .performScrollTo()
                .fetchSemanticsNode()
                .positionInWindow
                .y
        val bottomY =
            composeTestRule
                .onNodeWithText(bottomText)
                .performScrollTo()
                .fetchSemanticsNode()
                .positionInWindow
                .y
        assertTrue(
            "Expected '$topText' (y=$topY) to be above '$bottomText' (y=$bottomY)",
            topY < bottomY,
        )
    }

    @Test
    fun listItems() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val makeListLabel = context.getString(R.string.make_list)
        val titleLabel = context.getString(R.string.title)
        val addItemLabel = context.getString(R.string.add_item)
        val backLabel = context.getString(R.string.back)

        // 1. Open List Editor and verify the auto-added item is immediately focused
        composeTestRule.onNodeWithContentDescription(makeListLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Shopping List")

        composeTestRule
            .onNodeWithContentDescription(titleLabel)
            .performTextInput("Shopping List Note")

        // 2. Leave note and re-enter it to verify saving worked
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.onNodeWithText("Shopping List Note").performScrollTo().performClick()

        // Assert first item is displayed after re-entering
        composeTestRule.onNodeWithText("Shopping List").performScrollTo().assertIsDisplayed()

        // 3. Add remaining items with text, testing auto-focus on "Add item" click, Enter key, and
        // Backspace deletion
        composeTestRule.onNodeWithText(addItemLabel).performScrollTo().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Apples")

        // Test pressing Enter key adds a new item and immediately focuses it
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performImeAction()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Bananas")

        // Test pressing Enter key to create empty item, then Backspace to delete it and return
        // focus to "Bananas"
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performImeAction()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        //        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performKeyInput {
        //            pressKey(Key.Backspace)
        //        }
        //        composeTestRule.onNodeWithText("Bananas").assertIsFocused()

        composeTestRule.onNodeWithText(addItemLabel).performScrollTo().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Cherries")

        composeTestRule.onNodeWithText(addItemLabel).performScrollTo().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Dates")

        // Assert title and items are displayed
        composeTestRule.onNodeWithText("Shopping List Note").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping List").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Apples").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bananas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cherries").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dates").performScrollTo().assertIsDisplayed()

        // Assert initial item order
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Dates")

        // 4. Check some items ("Bananas" and "Dates")
        composeTestRule.onNode(itemCheckbox("Bananas")).performScrollTo().performClick()
        composeTestRule.onNode(itemCheckbox("Dates")).performScrollTo().performClick()

        // Assert toggleable states: "Shopping List", "Apples", and "Cherries" are unchecked;
        // "Bananas" and "Dates" are checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).performScrollTo().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).performScrollTo().assertIsOn()

        // 5. Uncheck an item ("Bananas")
        composeTestRule.onNode(itemCheckbox("Bananas")).performScrollTo().performClick()

        // Assert toggleable states: "Shopping List", "Apples", "Cherries", and "Bananas" are
        // unchecked; "Dates" is checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).performScrollTo().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Dates")).performScrollTo().assertIsOn()

        // Assert items content
        composeTestRule.onNodeWithText("Shopping List").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Apples").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cherries").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bananas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dates").performScrollTo().assertIsDisplayed()

        scenario.close()
    }
}
