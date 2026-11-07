package com.philkes.notallyx.activity.edit

import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.dragAndDrop
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
        val topY = composeTestRule.onNodeWithText(topText).fetchSemanticsNode().positionInWindow.y
        val bottomY =
            composeTestRule.onNodeWithText(bottomText).fetchSemanticsNode().positionInWindow.y
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

        // 1. Open List Editor, set title, and fill the immediately auto-added item as "Shopping
        // List"
        composeTestRule.onNodeWithContentDescription(makeListLabel).performClick()
        composeTestRule
            .onNodeWithContentDescription(titleLabel)
            .performTextInput("Shopping List Note")
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Shopping List")

        // 2. Leave note and re-enter it to verify saving worked
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.onNodeWithText("Shopping List Note").performClick()

        // Assert first item is displayed after re-entering
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()

        // 3. Add remaining items with text
        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Apples")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Bananas")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Cherries")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Dates")

        // Assert title and items are displayed
        composeTestRule.onNodeWithText("Shopping List Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apples").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cherries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dates").assertIsDisplayed()

        // Assert initial item order
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Dates")

        // 4. Check some items ("Bananas" and "Dates")
        composeTestRule.onNode(itemCheckbox("Bananas")).performClick()
        composeTestRule.onNode(itemCheckbox("Dates")).performClick()

        // Assert toggleable states: "Shopping List", "Apples", and "Cherries" are unchecked;
        // "Bananas" and "Dates" are checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).assertIsOn()

        // 5. Uncheck an item ("Bananas")
        composeTestRule.onNode(itemCheckbox("Bananas")).performClick()

        // Assert toggleable states: "Shopping List", "Apples", "Cherries", and "Bananas" are
        // unchecked; "Dates" is checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).assertIsOff()
        composeTestRule.onNode(itemCheckbox("Dates")).assertIsOn()

        // Assert items content
        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apples").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cherries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dates").assertIsDisplayed()

        // 6. Reorder items via drag and drop (drag "Bananas" above "Apples")
        dragAndDrop(
            sourceSelector = By.text("Bananas"),
            targetSelector = By.text("Apples"),
        )

        // Assert items order after drag and drop reordering
        assertIsAbove("Shopping List", "Bananas")
        assertIsAbove("Bananas", "Apples")
        assertIsAbove("Apples", "Cherries")

        scenario.close()
    }
}
