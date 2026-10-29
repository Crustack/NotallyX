package com.philkes.notallyx.activity.main.routes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.dragAndDropTo
import com.philkes.notallyx.test.navigateTo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class LabelsRouteTest : UiTestBase() {

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

        composeTestRule.navigateTo(NotallyNavKey.Labels)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().size >= 2
        }
        composeTestRule.onAllNodes(hasText("label")).onFirst().assertIsDisplayed()

        // Edit label
        composeTestRule
            .onAllNodes(hasContentDescription(context.getString(R.string.edit)))
            .onFirst()
            .performClick()

        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("label1")
        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label1")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(hasText("label1")).onFirst().assertIsDisplayed()

        // Add label by clicking toolbar Add button
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_label))
            .performClick()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("label2")
        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label2")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodes(hasText("label2")).onFirst().assertIsDisplayed()

        // Delete label "label1"
        composeTestRule
            .onAllNodes(hasContentDescription(context.getString(R.string.delete)))
            .onLast()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.delete)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("label1")).fetchSemanticsNodes().isEmpty()
        }
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

        composeTestRule.navigateTo(NotallyNavKey.Unlabeled)

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

        composeTestRule.onAllNodes(hasText("Work")).onLast().assertIsDisplayed()
        composeTestRule.onAllNodes(hasText("Personal")).onLast().assertIsDisplayed()

        // Click on "Work" label in navigation view
        composeTestRule.onAllNodes(hasText("Work")).onLast().performClick()

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

        composeTestRule.onAllNodes(hasText("Personal")).onLast().performClick()

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

    private fun labelOnScreen(label: String) = hasText(label) and !isSelectable()

    private fun labelInDrawer(label: String) = hasText(label) and isSelectable()

    @Test
    fun reorderLabelsAndToggleHiddenFromDrawer() {
        // 1. Insert initial labels and notes referencing them
        runBlocking {
            database
                .getLabelDao()
                .insert(
                    listOf(
                        Label("Work", 2),
                        Label("Personal", 1),
                        Label("Hobbies", 0),
                    )
                )
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Work Note",
                            body = "Work Note Body",
                            labels = listOf("Work"),
                        ),
                        createBaseNote(
                            title = "Hobbies Note",
                            body = "Hobbies Note Body",
                            labels = listOf("Hobbies"),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Verify initial notes are displayed on Notes screen
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Work Note")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule
                    .onAllNodes(hasText("Hobbies Note"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Work Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hobbies Note").assertIsDisplayed()

        // Verify initial drawer order: Work -> Personal -> Hobbies
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelInDrawer("Hobbies")).fetchSemanticsNodes().isNotEmpty()
        }
        val initialDrawerWorkY =
            composeTestRule.onNode(labelInDrawer("Work")).fetchSemanticsNode().positionInWindow.y
        val initialDrawerPersonalY =
            composeTestRule
                .onNode(labelInDrawer("Personal"))
                .fetchSemanticsNode()
                .positionInWindow
                .y
        val initialDrawerHobbiesY =
            composeTestRule.onNode(labelInDrawer("Hobbies")).fetchSemanticsNode().positionInWindow.y

        assertTrue(
            "In drawer: Work should be above Personal initially",
            initialDrawerWorkY < initialDrawerPersonalY,
        )
        assertTrue(
            "In drawer: Personal should be above Hobbies initially",
            initialDrawerPersonalY < initialDrawerHobbiesY,
        )

        // Close drawer and navigate to Labels screen
        composeTestRule
            .onAllNodes(hasText(context.getString(R.string.labels)))
            .onLast()
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelOnScreen("Work")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule
                    .onAllNodes(labelOnScreen("Personal"))
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                composeTestRule
                    .onAllNodes(labelOnScreen("Hobbies"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }

        // Verify initial vertical order on LabelsScreen: Work -> Personal -> Hobbies
        val initialLabelsWorkY =
            composeTestRule.onNode(labelOnScreen("Work")).fetchSemanticsNode().positionInWindow.y
        val initialLabelsPersonalY =
            composeTestRule
                .onNode(labelOnScreen("Personal"))
                .fetchSemanticsNode()
                .positionInWindow
                .y
        val initialLabelsHobbiesY =
            composeTestRule.onNode(labelOnScreen("Hobbies")).fetchSemanticsNode().positionInWindow.y

        assertTrue(
            "On LabelsScreen: Work should be above Personal initially",
            initialLabelsWorkY < initialLabelsPersonalY,
        )
        assertTrue(
            "On LabelsScreen: Personal should be above Hobbies initially",
            initialLabelsPersonalY < initialLabelsHobbiesY,
        )

        // 2. Drag-and-drop reordering: Drag "Hobbies" drag handle above "Work"
        val hobbiesDragHandle =
            composeTestRule.onNode(
                hasContentDescription(context.getString(R.string.drag_handle)) and
                    hasAnySibling(hasText("Hobbies")),
                useUnmergedTree = true,
            )
        val workNode = composeTestRule.onNode(labelOnScreen("Work"))

        hobbiesDragHandle.dragAndDropTo(
            targetNode = workNode,
            verticalOffset = -15f,
            holdDurationMillis = 600L,
        )

        // Wait until database reflects updated order: Hobbies should now be first (highest order)
        composeTestRule.waitUntil(5_000) {
            runBlocking {
                val array = database.getLabelDao().getArrayOfAll()
                array.isNotEmpty() && (array[0] == "Hobbies")
            }
        }

        // Verify updated order on LabelsScreen: Hobbies is now above Work
        val reorderedLabelsHobbiesY =
            composeTestRule.onNode(labelOnScreen("Hobbies")).fetchSemanticsNode().positionInWindow.y
        val reorderedLabelsWorkY =
            composeTestRule.onNode(labelOnScreen("Work")).fetchSemanticsNode().positionInWindow.y
        assertTrue(
            "On LabelsScreen: Hobbies should be above Work after drag",
            reorderedLabelsHobbiesY < reorderedLabelsWorkY,
        )

        // 3. Verify updated order in navigation drawer: Hobbies -> Work -> Personal
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelInDrawer("Hobbies")).fetchSemanticsNodes().isNotEmpty()
        }

        val reorderedDrawerHobbiesY =
            composeTestRule.onNode(labelInDrawer("Hobbies")).fetchSemanticsNode().positionInWindow.y
        val reorderedDrawerWorkY =
            composeTestRule.onNode(labelInDrawer("Work")).fetchSemanticsNode().positionInWindow.y
        val reorderedDrawerPersonalY =
            composeTestRule
                .onNode(labelInDrawer("Personal"))
                .fetchSemanticsNode()
                .positionInWindow
                .y

        assertTrue(
            "In drawer: Hobbies should be above Work after reordering",
            reorderedDrawerHobbiesY < reorderedDrawerWorkY,
        )
        assertTrue(
            "In drawer: Work should be above Personal after reordering",
            reorderedDrawerWorkY < reorderedDrawerPersonalY,
        )

        // Return to LabelsScreen
        composeTestRule
            .onAllNodes(hasText(context.getString(R.string.labels)))
            .onLast()
            .performClick()

        // 4. Hide "Hobbies" label from navigation drawer & overview
        composeTestRule
            .onNode(
                hasContentDescription(
                    context.getString(R.string.labels_hidden_in_overview_title)
                ) and hasAnyAncestor(labelOnScreen("Hobbies"))
            )
            .performClick()

        composeTestRule.waitUntil(5_000) { preferences.labelsHidden.value.contains("Hobbies") }

        // Open navigation drawer and verify "Hobbies" is excluded while "Work" and "Personal"
        // remain
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelInDrawer("Work")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodes(labelInDrawer("Hobbies")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNode(labelInDrawer("Work")).assertIsDisplayed()
        composeTestRule.onNode(labelInDrawer("Personal")).assertIsDisplayed()
        composeTestRule.onNode(labelInDrawer("Hobbies")).assertDoesNotExist()

        // 5. Navigate to Notes screen and verify "Hobbies Note" is excluded from notes overview
        composeTestRule
            .onAllNodes(hasText(context.getString(R.string.notes)))
            .onLast()
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Work Note")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Work Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hobbies Note").assertDoesNotExist()

        // 6. Navigate back to Labels screen and unhide "Hobbies"
        composeTestRule.navigateTo(NotallyNavKey.Labels)

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelOnScreen("Hobbies")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNode(
                hasContentDescription(
                    context.getString(R.string.labels_hidden_in_overview_title)
                ) and hasAnyAncestor(labelOnScreen("Hobbies"))
            )
            .performClick()

        composeTestRule.waitUntil(5_000) { !preferences.labelsHidden.value.contains("Hobbies") }

        // 7. Verify "Hobbies" is restored in both navigation drawer and notes overview
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.app_name))
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(labelInDrawer("Hobbies")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(labelInDrawer("Hobbies")).assertIsDisplayed()

        // Navigate to Notes and verify "Hobbies Note" is visible again
        composeTestRule
            .onAllNodes(hasText(context.getString(R.string.notes)))
            .onLast()
            .performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText("Hobbies Note")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Hobbies Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work Note").assertIsDisplayed()

        scenario.close()
    }
}
