package com.philkes.notallyx.activity.edit

import android.util.Log
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.dragAndDropTo
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditListRouteTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private fun itemCheckbox(text: String) = hasContentDescription(text) and isToggleable()

    private fun itemNode(text: String) = hasText(text) and hasSetTextAction()

    private fun SemanticsNodeInteraction.scrollToIfNeeded(): SemanticsNodeInteraction {
        try {
            performScrollTo()
        } catch (ignored: Throwable) {
            // Ignore if node is already fully visible or parent has no scroll action
        }
        return this
    }

    private fun ComposeTestRule.scrollToTop() {
        try {
            onNode(hasScrollAction()).performTouchInput {
                repeat(5) { swipeDown(startY = centerY, endY = bottom - 10f) }
            }
            waitForIdle()
        } catch (ignored: Throwable) {
            // Ignore if no scroll container is active
        }
    }

    private fun assertIsAbove(topText: String, bottomText: String) {
        composeTestRule.scrollToTop()
        val topNode = composeTestRule.onAllNodes(itemNode(topText)).onFirst().scrollToIfNeeded()
        val bottomNode =
            composeTestRule.onAllNodes(itemNode(bottomText)).onFirst().scrollToIfNeeded()
        val topY = topNode.fetchSemanticsNode().positionInWindow.y
        val bottomY = bottomNode.fetchSemanticsNode().positionInWindow.y
        assertTrue(
            "Expected '$topText' (y=$topY) to be above '$bottomText' (y=$bottomY)",
            topY < bottomY,
        )
    }

    private fun assertIsChild(itemText: String, referenceParentText: String = "Shopping List") {
        composeTestRule.scrollToTop()
        val rootParentNode =
            composeTestRule.onAllNodes(itemNode(referenceParentText)).onFirst().scrollToIfNeeded()
        val rootParentX = rootParentNode.fetchSemanticsNode().positionInWindow.x
        val itemNode = composeTestRule.onAllNodes(itemNode(itemText)).onFirst().scrollToIfNeeded()
        val itemX = itemNode.fetchSemanticsNode().positionInWindow.x
        assertTrue(
            "Expected '$itemText' (x=$itemX) to be indented as a child relative to parent (x=$rootParentX)",
            itemX > rootParentX + 10f,
        )
    }

    private fun assertIsParent(itemText: String, referenceParentText: String = "Shopping List") {
        composeTestRule.scrollToTop()
        val rootParentNode =
            composeTestRule.onAllNodes(itemNode(referenceParentText)).onFirst().scrollToIfNeeded()
        val rootParentX = rootParentNode.fetchSemanticsNode().positionInWindow.x
        val itemNode = composeTestRule.onAllNodes(itemNode(itemText)).onFirst().scrollToIfNeeded()
        val itemX = itemNode.fetchSemanticsNode().positionInWindow.x
        assertTrue(
            "Expected '$itemText' (x=$itemX) to be a parent item matching parent alignment (x=$rootParentX)",
            kotlin.math.abs(itemX - rootParentX) < 5f,
        )
    }

    private fun ComposeTestRule.swipeItemRight(itemText: String) {
        scrollToTop()
        onAllNodes(itemNode(itemText)).onFirst().scrollToIfNeeded().performTouchInput {
            swipeRight()
        }
        waitForIdle()
    }

    private fun ComposeTestRule.swipeItemLeft(itemText: String) {
        scrollToTop()
        onAllNodes(itemNode(itemText)).onFirst().scrollToIfNeeded().performTouchInput {
            swipeLeft()
        }
        waitForIdle()
    }

    /**
     * Drag and drop the ListItem with body equal to [sourceItemBody] under ListItem with body equal
     * to [targetItemBody]
     */
    private fun ComposeTestRule.dragAndDropItem(sourceItemBody: String, targetItemBody: String) {
        scrollToTop()
        onNode(
                hasContentDescription(
                    context.getString(R.string.drag_handle),
                    ignoreCase = true,
                ) and
                    hasAnySibling(
                        hasAnyDescendant(itemNode(sourceItemBody)) or itemNode(sourceItemBody)
                    )
            )
            .dragAndDropTo(composeTestRule.onAllNodes(itemNode(targetItemBody)).onFirst())
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
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Shopping List Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Shopping List Note").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Shopping List")).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert first item is displayed after re-entering
        composeTestRule.onAllNodes(itemNode("Shopping List")).onFirst().assertIsDisplayed()

        // 3. Add remaining items with text, testing auto-focus on "Add item" click, Enter key, and
        // Backspace deletion
        composeTestRule.onNodeWithText(addItemLabel).scrollToIfNeeded().performClick()
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

        composeTestRule.onNodeWithText(addItemLabel).scrollToIfNeeded().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Cherries")

        composeTestRule.onNodeWithText(addItemLabel).scrollToIfNeeded().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Dates")

        // Assert title and items are displayed
        composeTestRule.onNodeWithText("Shopping List Note").assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Shopping List"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Apples"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Bananas"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Cherries"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Dates"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()

        // Assert initial item order
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Dates")

        // 4. Check some items ("Bananas" and "Dates")
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().performClick()

        // Assert toggleable states: "Shopping List", "Apples", and "Cherries" are unchecked;
        // "Bananas" and "Dates" are checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        // 5. Uncheck an item ("Bananas")
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().performClick()

        // Assert toggleable states: "Shopping List", "Apples", "Cherries", and "Bananas" are
        // unchecked; "Dates" is checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        // Assert items content
        composeTestRule
            .onAllNodes(itemNode("Shopping List"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Apples"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Cherries"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Bananas"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodes(itemNode("Dates"))
            .onFirst()
            .scrollToIfNeeded()
            .assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun dragAndDropAndSwipe() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val makeListLabel = context.getString(R.string.make_list)
        val titleLabel = context.getString(R.string.title)
        val addItemLabel = context.getString(R.string.add_item)
        val backLabel = context.getString(R.string.back)

        // 1. Open List Editor and verify the auto-added item is immediately focused
        composeTestRule.onNodeWithContentDescription(makeListLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Shopping List")

        composeTestRule
            .onNodeWithContentDescription(titleLabel)
            .performTextInput("Shopping List Note")

        // 2. Leave note and re-enter it to verify saving worked
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Shopping List Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Shopping List Note").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Shopping List")).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert first item is displayed after re-entering
        composeTestRule.onAllNodes(itemNode("Shopping List")).onFirst().assertIsDisplayed()

        // 3. Add more items ("Apples", "Bananas", "Cherries", "Dates")
        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Apples")

        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performImeAction()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Bananas")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Cherries")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Dates")

        // Close keyboard so layout pass and window insets remain completely stable during drag &
        // swipe
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        composeTestRule.scrollToTop()

        // Assert initial item order: Shopping List -> Apples -> Bananas -> Cherries -> Dates
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Dates")

        // Assert initial parent status: all items are parents
        assertIsParent("Shopping List")
        assertIsParent("Apples")
        assertIsParent("Bananas")
        assertIsParent("Cherries")
        assertIsParent("Dates")

        // 4. Swipe "Apples" and "Bananas" to the right to make them children
        composeTestRule.swipeItemRight("Apples")
        composeTestRule.swipeItemRight("Bananas")

        // Assert order remains unchanged
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Dates")

        // Assert parent/child status
        assertIsParent("Shopping List")
        assertIsChild("Apples")
        assertIsChild("Bananas")
        assertIsParent("Cherries")
        assertIsParent("Dates")

        Log.d("ListItem", "Move Cherries to Shopping List")
        // 5. Drag parent "Cherries" above parent "Shopping List"
        composeTestRule.dragAndDropItem("Cherries", "Shopping List")
        composeTestRule.waitForIdle()

        // Assert updated item order: Cherries -> Shopping List -> Apples -> Bananas -> Dates
        assertIsAbove("Cherries", "Shopping List")
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Dates")

        // Assert parent/child status
        assertIsParent("Cherries")
        assertIsParent("Shopping List")
        assertIsChild("Apples")
        assertIsChild("Bananas")
        assertIsParent("Dates")

        Log.d("ListItem", "Move Shopping List to Dates")
        // Drag parent "Shopping List" (along with its children) down below parent "Dates"
        composeTestRule.dragAndDropItem("Shopping List", "Dates")
        composeTestRule.waitForIdle()

        // Assert updated item order: Cherries -> Dates -> Shopping List -> Apples -> Bananas
        assertIsAbove("Cherries", "Dates")
        assertIsAbove("Dates", "Shopping List")
        assertIsAbove("Shopping List", "Apples")
        assertIsAbove("Apples", "Bananas")

        // Assert parent/child status
        assertIsParent("Cherries")
        assertIsParent("Dates")
        assertIsParent("Shopping List")
        assertIsChild("Apples")
        assertIsChild("Bananas")

        Log.d("ListItem", "Move Bananas List to Cherries")
        // 6. Drag child item "Bananas" directly underneath parent item "Cherries" (onto Dates)
        composeTestRule.dragAndDropItem("Bananas", "Dates")
        composeTestRule.waitForIdle()

        // Assert updated item order: Cherries -> Bananas -> Dates -> Shopping List -> Apples
        assertIsAbove("Cherries", "Bananas")
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Shopping List")
        assertIsAbove("Shopping List", "Apples")

        // Assert parent/child status: "Bananas" stays child, but now of "Cherries"
        assertIsParent("Cherries", "Cherries")
        assertIsChild("Bananas", "Cherries")
        assertIsParent("Dates", "Cherries")
        assertIsParent("Shopping List", "Cherries")
        assertIsChild("Apples", "Cherries")

        // 7. Drag child item "Apples" underneath child item "Bananas" of parent "Cherries" (onto
        // Dates)
        composeTestRule.dragAndDropItem("Apples", "Dates")
        composeTestRule.waitForIdle()

        // Assert updated item order: Cherries -> Bananas -> Apples -> Dates -> Shopping List
        assertIsAbove("Cherries", "Bananas")
        assertIsAbove("Bananas", "Apples")
        assertIsAbove("Apples", "Dates")
        assertIsAbove("Dates", "Shopping List")

        // Assert parent/child status: "Apples" stays child, but now of "Cherries"
        assertIsParent("Cherries", "Cherries")
        assertIsChild("Bananas", "Cherries")
        assertIsChild("Apples", "Cherries")
        assertIsParent("Dates", "Cherries")
        assertIsParent("Shopping List", "Cherries")

        // 8. Drag child item "Bananas" to the very top (above Cherries) -> gets made into a parent
        composeTestRule.dragAndDropItem("Bananas", "Cherries")
        composeTestRule.waitForIdle()

        // Assert updated item order: Bananas -> Cherries -> Apples -> Dates -> Shopping List
        assertIsAbove("Bananas", "Cherries")
        assertIsAbove("Cherries", "Apples")
        assertIsAbove("Apples", "Dates")
        assertIsAbove("Dates", "Shopping List")

        // Assert parent/child status: "Bananas" is made into a parent
        assertIsParent("Bananas", "Bananas")
        assertIsParent("Cherries", "Bananas")
        assertIsChild("Apples", "Bananas")
        assertIsParent("Dates", "Bananas")
        assertIsParent("Shopping List", "Bananas")

        // 9. Drag parent with children directly under another parent:
        // First make "Shopping List" a child under "Dates"
        composeTestRule.swipeItemRight("Shopping List")
        composeTestRule.waitForIdle()

        assertIsParent("Dates", "Bananas")
        assertIsChild("Shopping List", "Dates")

        // Now drag parent "Cherries" (with child "Apples") directly under parent "Dates" (above
        // Shopping List)
        composeTestRule.dragAndDropItem("Cherries", "Dates")
        composeTestRule.waitForIdle()

        // Assert updated item order: Bananas -> Dates -> Cherries -> Apples -> Shopping List
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Cherries")
        assertIsAbove("Cherries", "Apples")
        assertIsAbove("Apples", "Shopping List")

        // Assert parent/child status: "Cherries" and its children ("Apples") become children of
        // "Dates"
        assertIsParent("Bananas", "Bananas")
        assertIsParent("Dates", "Bananas")
        assertIsChild("Cherries", "Dates")
        assertIsChild("Apples", "Dates")
        assertIsChild("Shopping List", "Dates")

        // 10. Drag parent with children under the last child of another parent:
        // Make Cherries a parent again with child Apples
        composeTestRule.swipeItemLeft("Cherries")
        composeTestRule.waitForIdle()
        // Unindent Shopping List so it is an independent parent
        composeTestRule.swipeItemLeft("Shopping List")
        composeTestRule.waitForIdle()
        // Make Dates a child of Bananas (so Dates is the last child of Bananas)
        composeTestRule.swipeItemRight("Dates")
        composeTestRule.waitForIdle()

        // Drag Cherries (with child Apples) above Bananas
        composeTestRule.dragAndDropItem("Cherries", "Bananas")
        composeTestRule.waitForIdle()

        assertIsAbove("Cherries", "Apples")
        assertIsAbove("Apples", "Bananas")
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Shopping List")

        // Drag parent "Cherries" (with child "Apples") under the last child of "Bananas" ("Dates")
        composeTestRule.dragAndDropItem("Cherries", "Dates")
        composeTestRule.waitForIdle()

        // Assert updated item order: Bananas -> Dates -> Cherries -> Apples -> Shopping List
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Cherries")
        assertIsAbove("Cherries", "Apples")
        assertIsAbove("Apples", "Shopping List")

        // Assert parent/child status: "Cherries" stays a parent with same child ("Apples")
        assertIsParent("Bananas", "Bananas")
        assertIsChild("Dates", "Bananas")
        assertIsParent("Cherries", "Bananas")
        assertIsChild("Apples", "Cherries")
        assertIsParent("Shopping List", "Bananas")

        // 11. Drag parent with children to last position:
        // Drag parent "Cherries" (with child "Apples") down below "Shopping List" (the last item)
        composeTestRule.dragAndDropItem("Cherries", "Shopping List")
        composeTestRule.waitForIdle()

        // Assert updated item order: Bananas -> Dates -> Shopping List -> Cherries -> Apples
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Shopping List")
        assertIsAbove("Shopping List", "Cherries")
        assertIsAbove("Cherries", "Apples")

        // Assert parent/child status: "Cherries" stays a parent with same child ("Apples")
        assertIsParent("Bananas", "Bananas")
        assertIsChild("Dates", "Bananas")
        assertIsParent("Shopping List", "Bananas")
        assertIsParent("Cherries", "Bananas")
        assertIsChild("Apples", "Cherries")

        // 12. Leave note and re-enter to verify changes were saved to ViewModel / Database
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Shopping List Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Shopping List Note").performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Shopping List")).fetchSemanticsNodes().isNotEmpty()
        }

        // Assert final item order after re-entering: Bananas -> Dates -> Shopping List -> Cherries
        // -> Apples
        assertIsAbove("Bananas", "Dates")
        assertIsAbove("Dates", "Shopping List")
        assertIsAbove("Shopping List", "Cherries")
        assertIsAbove("Cherries", "Apples")

        // Assert final parent/child status after re-entering
        assertIsParent("Bananas", "Bananas")
        assertIsChild("Dates", "Bananas")
        assertIsParent("Shopping List", "Bananas")
        assertIsParent("Cherries", "Bananas")
        assertIsChild("Apples", "Cherries")

        scenario.close()
    }

    @Test
    fun dragParentWithChildrenDown() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val makeListLabel = context.getString(R.string.make_list)
        val addItemLabel = context.getString(R.string.add_item)

        // 1. Open List Editor
        composeTestRule.onNodeWithContentDescription(makeListLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Parent B")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Child C")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Parent D")

        composeTestRule.onNodeWithText(addItemLabel).performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Parent E")

        androidx.test.espresso.Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        // 2. Make Child C a child of Parent B
        composeTestRule.swipeItemRight("Child C")

        // Assert initial order & parent/child status
        assertIsParent("Parent B", "Parent B")
        assertIsChild("Child C", "Parent B")
        assertIsParent("Parent D", "Parent B")
        assertIsParent("Parent E", "Parent B")

        assertIsAbove("Parent B", "Child C")
        assertIsAbove("Child C", "Parent D")
        assertIsAbove("Parent D", "Parent E")

        // 3. Drag Parent B down below Parent D
        composeTestRule.dragAndDropItem("Parent B", "Parent D")
        composeTestRule.waitForIdle()

        // Assert order: Parent D -> Parent B -> Child C -> Parent E
        assertIsAbove("Parent D", "Parent B")
        assertIsAbove("Parent B", "Child C")
        assertIsAbove("Child C", "Parent E")

        assertIsParent("Parent D", "Parent B")
        assertIsParent("Parent B", "Parent B")
        assertIsChild("Child C", "Parent B")
        assertIsParent("Parent E", "Parent B")

        // 4. Drag Parent B further down below Parent E
        composeTestRule.dragAndDropItem("Parent B", "Parent E")
        composeTestRule.waitForIdle()

        // Assert order: Parent D -> Parent E -> Parent B -> Child C
        assertIsAbove("Parent D", "Parent E")
        assertIsAbove("Parent E", "Parent B")
        assertIsAbove("Parent B", "Child C")

        assertIsParent("Parent D", "Parent B")
        assertIsParent("Parent E", "Parent B")
        assertIsParent("Parent B", "Parent B")
        assertIsChild("Child C", "Parent B")

        scenario.close()
    }
}
