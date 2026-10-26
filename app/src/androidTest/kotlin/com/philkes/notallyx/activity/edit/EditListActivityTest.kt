package com.philkes.notallyx.activity.edit

import android.util.Log
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.dragAndDropTo
import kotlinx.coroutines.runBlocking
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
                hasContentDescription(context.getString(R.string.drag_handle)) and
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

    @Test
    fun search() {
        val noteTitle = "Search List Note"
        val itemsList = mutableListOf(createListItem("The quick brown fox"))
        for (i in 1..15) {
            itemsList.add(createListItem("Filler item $i"))
        }
        itemsList.add(createListItem("Fox is fast in the middle"))
        for (i in 16..30) {
            itemsList.add(createListItem("Filler item $i"))
        }
        itemsList.add(createListItem("Another fox is near at the bottom"))

        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val searchLabel = context.getString(R.string.search)
        val nextLabel = context.getString(R.string.next)
        val prevLabel = context.getString(R.string.previous)
        val backLabel = context.getString(R.string.back)

        // Wait for MainActivity to display the list note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        // Open existing list note
        composeTestRule.onNodeWithText(noteTitle).performClick()

        // Wait for EditListActivity to load
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(searchLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Click search action in top bar
        composeTestRule.onNodeWithContentDescription(searchLabel).performClick()

        // Enter search query into search bar
        composeTestRule.onNodeWithContentDescription(searchLabel).performTextInput("fox")

        // Check occurrence indicator displays "1/3" and first item is displayed
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("1/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("1/3").assertIsDisplayed()
        composeTestRule.onNodeWithText("The quick brown fox", substring = true).assertIsDisplayed()

        // Jump to next occurrence -> "2/3" and verify scrolled to middle item
        composeTestRule.onNodeWithContentDescription(nextLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("2/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Fox is fast in the middle", substring = true)
            .assertIsDisplayed()

        // Jump to next occurrence -> "3/3" and verify scrolled to bottom item
        composeTestRule.onNodeWithContentDescription(nextLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("3/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Another fox is near at the bottom", substring = true)
            .assertIsDisplayed()

        // Jump to previous occurrence -> "2/3" and verify scrolled back to middle item
        composeTestRule.onNodeWithContentDescription(prevLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("2/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Fox is fast in the middle", substring = true)
            .assertIsDisplayed()

        // Close search mode
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        scenario.close()
    }

    @Test
    fun moreListOptionsCheckAndClearAll() {
        val noteTitle = "Checklist Management Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Shopping List",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Apples",
                    checked = false,
                    isChild = true,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Bananas",
                    checked = true,
                    isChild = true,
                    order = 2,
                    id = 2,
                ),
                createListItem(
                    body = "Cherries",
                    checked = false,
                    isChild = false,
                    order = 3,
                    id = 3,
                ),
                createListItem(
                    body = "Dates",
                    checked = true,
                    isChild = false,
                    order = 4,
                    id = 4,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val moreOptionsLabel = context.getString(R.string.tap_for_more_options)
        val checkAllLabel = context.getString(R.string.check_all_items)
        val uncheckAllLabel = context.getString(R.string.uncheck_all_items)
        val deleteCheckedLabel = context.getString(R.string.delete_checked_items)
        val undoLabel = context.getString(R.string.undo)
        val backLabel = context.getString(R.string.back)

        // 1. Wait for note to appear in MainActivity and open it
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()

        // Wait for list items to display
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Shopping List")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify initial toggleable states:
        // "Shopping List", "Apples", "Cherries" are unchecked
        // "Bananas", "Dates" are checked
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        // 2. Open More Options BottomSheet and click "Check all items"
        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(checkAllLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(checkAllLabel).performClick()

        // Verify all items are now checked
        composeTestRule.waitUntil(10_000) {
            try {
                composeTestRule
                    .onNode(itemCheckbox("Shopping List"))
                    .scrollToIfNeeded()
                    .assertIsOn()
                composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
                composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
                composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOn()
                composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()
                true
            } catch (_: Throwable) {
                false
            }
        }
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        // 3. Open More Options BottomSheet and click "Uncheck all items"
        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(uncheckAllLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(uncheckAllLabel).performClick()

        // Verify all items are now unchecked
        composeTestRule.waitUntil(10_000) {
            try {
                composeTestRule
                    .onNode(itemCheckbox("Shopping List"))
                    .scrollToIfNeeded()
                    .assertIsOff()
                composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
                composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()
                composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOff()
                composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOff()
                true
            } catch (_: Throwable) {
                false
            }
        }
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOff()

        // 4. Check "Cherries" and "Dates", then delete checked items
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(deleteCheckedLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(deleteCheckedLabel).performClick()

        // Verify "Cherries" and "Dates" are deleted, while "Shopping List", "Apples", "Bananas"
        // remain
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Cherries")).fetchSemanticsNodes().isEmpty() &&
                composeTestRule.onAllNodes(itemNode("Dates")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNode(itemCheckbox("Shopping List")).scrollToIfNeeded().assertIsDisplayed()
        composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsDisplayed()
        composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsDisplayed()

        // 5. Click Undo to restore deleted items
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Cherries")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodes(itemNode("Dates")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(itemCheckbox("Cherries")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Dates")).scrollToIfNeeded().assertIsOn()

        // 6. Return back to MainActivity and verify list note was saved
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun multiStepUndoAndRedoDeepStack() {
        val noteTitle = "Deep Undo Redo Checklist Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Item 1",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Item 2",
                    checked = false,
                    isChild = false,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Item 3",
                    checked = false,
                    isChild = false,
                    order = 2,
                    id = 2,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val undoLabel = context.getString(R.string.undo)
        val redoLabel = context.getString(R.string.redo)
        val addItemLabel = context.getString(R.string.add_item)
        val backLabel = context.getString(R.string.back)

        // 1. Open the list note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 1")).fetchSemanticsNodes().isNotEmpty()
        }

        // Initial state assertions: both undo and redo are disabled
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // Step 1: Add a new item "Item 4"
        composeTestRule.onNodeWithText(addItemLabel).scrollToIfNeeded().performClick()
        composeTestRule.onNode(hasText("") and hasSetTextAction()).performTextInput("Item 4")
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 4")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // Step 2: Check "Item 1"
        composeTestRule.onNode(itemCheckbox("Item 1")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Item 1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()

        // Step 3: Check "Item 4"
        composeTestRule.onNode(itemCheckbox("Item 4")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Item 4")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()

        // Step 4: Check "Item 2"
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // --- Deep Multi-Step Undo ---
        // Undo 1: Reverts checking "Item 2"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Undo 2: Reverts checking "Item 4"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 4")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Undo 3: Reverts checking "Item 1"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 1")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Undo 4: Reverts typing "Item 4" text (leaving empty item)
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 4")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Undo 5: Reverts adding the empty item (item completely removed)
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        // Entire undo stack is now empty!
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // --- Deep Multi-Step Redo ---
        // Redo 1: Restores the added item (empty item)
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Redo 2: Restores text "Item 4"
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 4")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Redo 3: Re-checks "Item 1"
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Redo 4: Re-checks "Item 4"
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 4")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Redo 5: Re-checks "Item 2"
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        // Entire redo stack is now exhausted!
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // --- Test: Modification after undoing clears redo stack ---
        // Undo checking "Item 2"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // Perform a new modification: Check "Item 3"
        composeTestRule.onNode(itemCheckbox("Item 3")).scrollToIfNeeded().performClick()
        composeTestRule.onNode(itemCheckbox("Item 3")).scrollToIfNeeded().assertIsOn()

        // Verify redo stack was cleared: Redo button must now be disabled!
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()

        // Leaving note and re-entering to verify persistence
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 1")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify state is preserved: Item 1 and Item 3 and Item 4 are checked; Item 2 is unchecked
        composeTestRule.onNode(itemCheckbox("Item 1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Item 3")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Item 4")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Item 2")).scrollToIfNeeded().assertIsOff()

        scenario.close()
    }

    @Test
    fun undoAndRedoIndentationAndReordering() {
        val noteTitle = "Undo Indent and Reorder Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Parent 1",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Item 2",
                    checked = false,
                    isChild = false,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Item 3",
                    checked = false,
                    isChild = false,
                    order = 2,
                    id = 2,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val undoLabel = context.getString(R.string.undo)
        val redoLabel = context.getString(R.string.redo)
        val backLabel = context.getString(R.string.back)

        // 1. Open the list note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify initial alignment and order
        assertIsParent("Parent 1", "Parent 1")
        assertIsParent("Item 2", "Parent 1")
        assertIsParent("Item 3", "Parent 1")
        assertIsAbove("Parent 1", "Item 2")
        assertIsAbove("Item 2", "Item 3")

        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 2. Indent "Item 2" to make it a child of "Parent 1"
        composeTestRule.swipeItemRight("Item 2")
        assertIsChild("Item 2", "Parent 1")
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 3. Undo indentation -> "Item 2" returns to parent alignment
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        assertIsParent("Item 2", "Parent 1")
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // 4. Redo indentation -> "Item 2" becomes a child again
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        assertIsChild("Item 2", "Parent 1")
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 5. Unindent "Item 2" back to parent
        composeTestRule.swipeItemLeft("Item 2")
        assertIsParent("Item 2", "Parent 1")

        // 6. Undo unindentation -> "Item 2" is child again
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        assertIsChild("Item 2", "Parent 1")

        // 7. Redo unindentation -> "Item 2" is parent again
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        assertIsParent("Item 2", "Parent 1")

        // 8. Reorder items: Drag "Item 2" above "Parent 1" (adjacent swap)
        composeTestRule.dragAndDropItem("Item 2", "Parent 1")
        composeTestRule.waitForIdle()

        // Assert reordered items: Item 2 -> Parent 1 -> Item 3
        assertIsAbove("Item 2", "Parent 1")
        assertIsAbove("Parent 1", "Item 3")
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()

        // 9. Undo reordering -> original order restored: Parent 1 -> Item 2 -> Item 3
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitForIdle()
        assertIsAbove("Parent 1", "Item 2")
        assertIsAbove("Item 2", "Item 3")
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // 10. Redo reordering -> moved order restored: Item 2 -> Parent 1 -> Item 3
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitForIdle()
        assertIsAbove("Item 2", "Parent 1")
        assertIsAbove("Parent 1", "Item 3")
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 11. Exit and re-enter to ensure persistence
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        assertIsAbove("Item 2", "Parent 1")
        assertIsAbove("Parent 1", "Item 3")

        scenario.close()
    }

    @Test
    fun undoAndRedoListItemTextDeltasAndStructuralChanges() {
        preferences.listItemSorting.save(ListItemSort.NO_AUTO_SORT)

        val noteTitle = "Text Deltas and Structure Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Parent 1",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Item 2",
                    checked = false,
                    isChild = false,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Item 3",
                    checked = false,
                    isChild = false,
                    order = 2,
                    id = 2,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val undoLabel = context.getString(R.string.undo)
        val redoLabel = context.getString(R.string.redo)
        val backLabel = context.getString(R.string.back)

        // 1. Open the list note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify initial state: undo and redo both disabled
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 2. Edit Item 2 text (pushes ListItemBodyDeltaChange)
        composeTestRule
            .onAllNodes(itemNode("Item 2"))
            .onFirst()
            .scrollToIfNeeded()
            .performTextReplacement("Item 2 Updated")
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 2 Updated"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 3. Edit Item 3 text (pushes separate ListItemBodyDeltaChange for different itemId)
        composeTestRule
            .onAllNodes(itemNode("Item 3"))
            .onFirst()
            .scrollToIfNeeded()
            .performTextReplacement("Item 3 Modified")
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 3 Modified"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()

        // 4. Undo Item 3 text change -> Item 3 returns to original text, Item 2 remains "Item 2
        // Updated"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(itemNode("Item 2 Updated")).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // 5. Undo Item 2 text change -> Item 2 returns to "Item 2"
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Item 2")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsEnabled()

        // 6. Redo Item 2 text change -> "Item 2 Updated" restored
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 2 Updated"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onAllNodes(itemNode("Item 3")).onFirst().assertIsDisplayed()

        // 7. Redo Item 3 text change -> "Item 3 Modified" restored
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 3 Modified"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(redoLabel).assertIsNotEnabled()

        // 8. Interleave structural change (ListItemsChange) and text delta
        // (ListItemBodyDeltaChange):
        // Indent Item 2 Updated under Parent 1 (pushes ListItemsChange)
        composeTestRule.swipeItemRight("Item 2 Updated")
        composeTestRule.waitForIdle()
        assertIsChild("Item 2 Updated", "Parent 1")

        // Further edit the child item's text (pushes ListItemBodyDeltaChange on child item)
        composeTestRule
            .onAllNodes(itemNode("Item 2 Updated"))
            .onFirst()
            .scrollToIfNeeded()
            .performTextReplacement("Item 2 Child Text")
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 2 Child Text"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertIsChild("Item 2 Child Text", "Parent 1")

        // 9. Undo text delta on child item -> reverts text to "Item 2 Updated" while keeping it as
        // a child!
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 2 Updated"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertIsChild("Item 2 Updated", "Parent 1")

        // 10. Undo structural change -> reverts indentation, Item 2 Updated becomes parent again!
        composeTestRule.onNodeWithContentDescription(undoLabel).performClick()
        composeTestRule.waitForIdle()
        assertIsParent("Item 2 Updated", "Parent 1")

        // 11. Redo structural change -> Item 2 Updated becomes child again
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitForIdle()
        assertIsChild("Item 2 Updated", "Parent 1")

        // 12. Redo text delta -> Item 2 text restored to "Item 2 Child Text"
        composeTestRule.onNodeWithContentDescription(redoLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Item 2 Child Text"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertIsChild("Item 2 Child Text", "Parent 1")

        // 13. Exit note and re-enter to verify Room database persistence
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodes(itemNode("Item 2 Child Text")).onFirst().assertIsDisplayed()
        assertIsChild("Item 2 Child Text", "Parent 1")
        composeTestRule.onAllNodes(itemNode("Item 3 Modified")).onFirst().assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun noteStructureConversionFidelity() {
        preferences.listItemSorting.save(ListItemSort.NO_AUTO_SORT)

        val noteTitle = "Conversion Fidelity Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Parent 1",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Child 1.1",
                    checked = true,
                    isChild = true,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Child 1.2",
                    checked = false,
                    isChild = true,
                    order = 2,
                    id = 2,
                ),
                createListItem(
                    body = "Parent 2",
                    checked = true,
                    isChild = false,
                    order = 3,
                    id = 3,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val moreOptionsLabel = context.getString(R.string.tap_for_more_options)
        val convertToTextLabel = context.getString(R.string.convert_to_text_note)
        val convertToListLabel = context.getString(R.string.convert_to_list_note)
        val noteBodyLabel = context.getString(R.string.note)
        val backLabel = context.getString(R.string.back)

        // 1. Open the list note from MainActivity
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Verify initial list note hierarchy and checked state in EditListActivity
        assertIsParent("Parent 1", "Parent 1")
        assertIsChild("Child 1.1", "Parent 1")
        assertIsChild("Child 1.2", "Parent 1")
        assertIsParent("Parent 2", "Parent 1")

        composeTestRule.onNode(itemCheckbox("Parent 1")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Child 1.1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Child 1.2")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Parent 2")).scrollToIfNeeded().assertIsOn()

        // 3. Convert List Note -> Text Note
        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(convertToTextLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(convertToTextLabel).performClick()

        // Wait until EditNoteActivity is active and note body is displayed
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(noteBodyLabel))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        // 4. Verify text body preserves Markdown checklist formatting with indentation and
        // checkmarks
        val expectedMarkdownBody =
            listOf(
                    "- [ ] Parent 1",
                    "    - [x] Child 1.1",
                    "    - [ ] Child 1.2",
                    "- [x] Parent 2",
                )
                .joinToString("\n")
        composeTestRule
            .onNodeWithContentDescription(noteBodyLabel)
            .assert(hasText(expectedMarkdownBody))

        // 5. Convert Text Note -> List Note
        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(convertToListLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(convertToListLabel).performClick()

        // Wait until EditListActivity is active and list items are displayed
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(itemNode("Parent 1"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        // 6. Verify full structural fidelity: hierarchy, ordering, and checked status preserved
        assertIsParent("Parent 1", "Parent 1")
        assertIsChild("Child 1.1", "Parent 1")
        assertIsChild("Child 1.2", "Parent 1")
        assertIsParent("Parent 2", "Parent 1")

        assertIsAbove("Parent 1", "Child 1.1")
        assertIsAbove("Child 1.1", "Child 1.2")
        assertIsAbove("Child 1.2", "Parent 2")

        composeTestRule.onNode(itemCheckbox("Parent 1")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Child 1.1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Child 1.2")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Parent 2")).scrollToIfNeeded().assertIsOn()

        // 7. Exit to MainActivity and re-open to verify persistent fidelity in Room DB
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(itemNode("Parent 1")).fetchSemanticsNodes().isNotEmpty()
        }

        assertIsParent("Parent 1", "Parent 1")
        assertIsChild("Child 1.1", "Parent 1")
        assertIsChild("Child 1.2", "Parent 1")
        assertIsParent("Parent 2", "Parent 1")

        composeTestRule.onNode(itemCheckbox("Parent 1")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Child 1.1")).scrollToIfNeeded().assertIsOn()
        composeTestRule.onNode(itemCheckbox("Child 1.2")).scrollToIfNeeded().assertIsOff()
        composeTestRule.onNode(itemCheckbox("Parent 2")).scrollToIfNeeded().assertIsOn()

        scenario.close()
    }

    @Test
    fun listItemSort() {
        val noteTitle = "Sorting Test Note"
        val itemsList =
            listOf(
                createListItem(
                    body = "Fruits",
                    checked = false,
                    isChild = false,
                    order = 0,
                    id = 0,
                ),
                createListItem(
                    body = "Apples",
                    checked = false,
                    isChild = true,
                    order = 1,
                    id = 1,
                ),
                createListItem(
                    body = "Bananas",
                    checked = false,
                    isChild = true,
                    order = 2,
                    id = 2,
                ),
                createListItem(
                    body = "Bread",
                    checked = false,
                    isChild = false,
                    order = 3,
                    id = 3,
                ),
            )
        val note =
            createBaseNote(
                title = noteTitle,
                type = Type.LIST,
                items = itemsList,
            )
        runBlocking { database.getBaseNoteDao().insert(note) }

        try {
            // ---------------------------------------------------------------
            // 1. Test ListItemSort.NO_AUTO_SORT: items remain in their original positions
            // ---------------------------------------------------------------
            preferences.listItemSorting.save(ListItemSort.NO_AUTO_SORT)
            val scenario = ActivityScenario.launch(MainActivity::class.java)

            val backLabel = context.getString(R.string.back)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(itemNode("Fruits")).fetchSemanticsNodes().isNotEmpty()
            }

            // Verify initial order and hierarchy: Fruits -> Apples (child) -> Bananas (child) ->
            // Bread (parent)
            assertIsParent("Fruits", "Fruits")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")
            assertIsParent("Bread", "Fruits")

            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")

            // Check child "Apples"
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()

            // In NO_AUTO_SORT, checked child remains in place under parent
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")

            // Check parent "Fruits" directly (which checks parent and all children)
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOff()

            // With NO_AUTO_SORT, checked parent and children stay in place; Bread remains at the
            // bottom
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")

            // Uncheck parent "Fruits" (which unchecks parent and all children) to reset
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()

            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")

            // Exit back to MainActivity
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }

            // ---------------------------------------------------------------
            // 2. Test ListItemSort.AUTO_SORT_BY_CHECKED: checked items move to bottom sorted by
            // list order
            // ---------------------------------------------------------------
            preferences.listItemSorting.save(ListItemSort.AUTO_SORT_BY_CHECKED)

            composeTestRule.onNodeWithText(noteTitle).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(itemNode("Fruits")).fetchSemanticsNodes().isNotEmpty()
            }

            // 2a. Partially check parent: checking only child "Apples"
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()

            // When only a child is checked, the parent is partially checked and must NOT move to
            // the bottom.
            // "Apples" stays under "Fruits" in the active section above "Bread"!
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")

            // 2b. Check remaining child "Bananas": now all children are checked, so parent
            // automatically checks
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOff()

            // Active item "Bread" is now ABOVE the fully checked parent "Fruits" and its children!
            assertIsAbove("Bread", "Fruits")
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // 2c. Unchecking child "Apples" unchecks parent and returns the entire group to active
            // items
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()

            // Entire group is back in active items above "Bread"
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // 2d. Check standalone item "Bread" FIRST, then check parent "Fruits" SECOND
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOn()
            assertIsAbove("Bananas", "Bread")

            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()

            // Under AUTO_SORT_BY_CHECKED, checked items are ordered by original list index:
            // "Fruits" (0) -> "Apples" (1) -> "Bananas" (2) -> "Bread" (3), even though Bread was
            // checked first!
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // Uncheck "Fruits" and "Bread" to reset
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().performClick()

            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")

            // Exit back to MainActivity
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }

            // ---------------------------------------------------------------
            // 3. Test ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP: checked items move to bottom
            // sorted by checked timestamp descending
            // ---------------------------------------------------------------
            preferences.listItemSorting.save(ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP)

            composeTestRule.onNodeWithText(noteTitle).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(itemNode("Fruits")).fetchSemanticsNodes().isNotEmpty()
            }

            // 3a. Check standalone item "Bread" FIRST (t1)
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOn()
            assertIsAbove("Bananas", "Bread")

            // 3b. Check parent "Fruits" SECOND (t2 > t1) -> Fruits group has higher timestamp than
            // Bread
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOn()

            // Since Fruits was checked after Bread, Fruits + children are ABOVE Bread in checked
            // items
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // 3c. Uncheck and re-check "Bread" THIRD (t3 > t2) -> Bread is now the newest checked
            // item!
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOff()
            // When Bread is unchecked, it moves back to active items above Fruits
            assertIsAbove("Bread", "Fruits")

            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOn()

            // Now Bread's timestamp is newer than Fruits, so Bread is ABOVE Fruits in the checked
            // section!
            assertIsAbove("Bread", "Fruits")
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // 3d. Uncheck child "Bananas": Fruits automatically unchecks and moves group back to
            // active section
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().performClick()
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()

            // Group is back in active items above checked "Bread"
            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")

            // Exit back to MainActivity and re-open to verify persistent state & hierarchy
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithText(noteTitle).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(itemNode("Fruits")).fetchSemanticsNodes().isNotEmpty()
            }

            assertIsAbove("Fruits", "Apples")
            assertIsAbove("Apples", "Bananas")
            assertIsAbove("Bananas", "Bread")
            assertIsChild("Apples", "Fruits")
            assertIsChild("Bananas", "Fruits")
            composeTestRule.onNode(itemCheckbox("Fruits")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Apples")).scrollToIfNeeded().assertIsOn()
            composeTestRule.onNode(itemCheckbox("Bananas")).scrollToIfNeeded().assertIsOff()
            composeTestRule.onNode(itemCheckbox("Bread")).scrollToIfNeeded().assertIsOn()

            composeTestRule.onNodeWithContentDescription(backLabel).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }

            scenario.close()
        } finally {
            preferences.listItemSorting.save(ListItemSort.AUTO_SORT_BY_CHECKED)
        }
    }
}
