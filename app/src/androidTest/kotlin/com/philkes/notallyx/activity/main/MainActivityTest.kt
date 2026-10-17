package com.philkes.notallyx.activity.main

import android.app.Notification
import android.util.Log
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.waitUntil
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class MainActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun multiSelectAndApplyLabel() {
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
        // 2. Recreate Activity so it reads the newly inserted data
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 1. longclick the first note ("Test")
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }
        // 2. click the 2nd note ("List") to also select it
        composeTestRule.onNodeWithText("List").performClick()

        // 3. Press unpin action to unpin both notes
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.unpin))
            .performClick()

        // 4. longclick first note again and click second note to select them
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("List").performClick()

        // 5. click labels action
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.labels))
            .performClick()

        // 6. click the "label" twice to give both notes the "label" Label
        composeTestRule
            .onNode(hasText("label") and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText("label") and hasAnyAncestor(isComposeDialog()))
            .performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().size >= 3
        }
        composeTestRule.onAllNodes(hasText("label"))[1].assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun restorePinnedNotifications() {
        var pinned1 =
            createBaseNote(
                title = "Pinned 1",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        var pinned2 =
            createBaseNote(
                title = "Pinned 2",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        runBlocking {
            withContext(Dispatchers.Main) {
                val baseNoteDao = database.getBaseNoteDao()
                pinned1 = pinned1.copy(id = baseNoteDao.insert(pinned1))
                pinned2 = pinned2.copy(id = baseNoteDao.insert(pinned2))
                baseNoteDao.insert(
                    createBaseNote(title = "Not Pinned", body = "Body", labels = listOf("label"))
                )
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.refreshStatusBarPin(pinned1)
            activity.refreshStatusBarPin(pinned2)
        }

        waitUntil(10_000) { PinnedNotificationManager.getPinnedNotifications(context).size == 2 }
        val pinnedNotifications = PinnedNotificationManager.getPinnedNotifications(context)
        assertTrue(
            "Notification for every pinned Note shown",
            setOf(pinned1, pinned2).all { pinnedNote ->
                pinnedNotifications.any {
                    it!!.id.toLong() == pinnedNote.id &&
                        pinnedNote.title.contains(
                            it.notification.extras.getString(Notification.EXTRA_TITLE)!!
                        ) &&
                        pinnedNote.body.contains(
                            it.notification.extras.getString(Notification.EXTRA_TEXT)!!
                        )
                }
            },
        )

        scenario.close()
    }

    @Test
    fun databaseParallel() {
        runBlocking {
            withContext(Dispatchers.Main) {
                database
                    .getBaseNoteDao()
                    .insert(
                        createBaseNote(title = "Note -1", body = "Body", labels = listOf("label"))
                    )
            }
        }

        val application = ApplicationProvider.getApplicationContext<NotallyXApplication>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val numTasks = 25
        runBlocking(Dispatchers.IO) {
            val baseNoteDao = database.getBaseNoteDao()
            val insertTasks =
                (1..numTasks).map {
                    async(Dispatchers.IO) {
                        baseNoteDao.insert(
                            createBaseNote(title = "Note $it", isPinnedToStatus = it % 2 == 0)
                        )
                    }
                }
            val restoreTasks =
                (1..numTasks).map {
                    async(Dispatchers.Main) { application.restorePinnedNotifications() }
                }
            val readTasks =
                (1..numTasks * 2).map {
                    async(Dispatchers.IO) {
                        val allNotes = baseNoteDao.getAll()
                        Log.d("FullUiTest", "Total notes: ${allNotes.size}")
                        delay(100.milliseconds)
                    }
                }
            (insertTasks + restoreTasks + readTasks).awaitAll()
        }

        waitUntil(10_000) {
            runBlocking { database.getBaseNoteDao().getAll().size == numTasks + 1 }
        }

        scenario.close()
    }

    @Test
    fun searchNotes() {
        // 1. Insert test data for normal, archived, and deleted notes
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Normal Note",
                            body = "Grocery list",
                            folder = Folder.NOTES,
                        ),
                        createBaseNote(
                            title = "Archived Note",
                            body = "Grocery history",
                            folder = Folder.ARCHIVED,
                        ),
                        createBaseNote(
                            title = "Deleted Note",
                            body = "Grocery trash",
                            folder = Folder.DELETED,
                        ),
                        createBaseNote(
                            title = "Work Meeting",
                            body = "Project status",
                            folder = Folder.NOTES,
                        ),
                    )
                )
        }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val searchLabel = context.getString(R.string.search)
        val cancelLabel = context.getString(R.string.cancel)

        // 2. Open Search fragment
        composeTestRule.onNodeWithContentDescription(searchLabel).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(searchLabel)).fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Type search keyword matching "Grocery"
        composeTestRule.onNode(hasText(searchLabel) and hasSetTextAction()).appendText("Grocery")

        // 4. Verify default "Notes" folder chip only shows normal notes
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Normal Note")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodes(hasText("Work Meeting")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Normal Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work Meeting").assertDoesNotExist()
        composeTestRule.onNodeWithText("Archived Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("Deleted Note").assertDoesNotExist()

        // 5. Switch filter to "Archived" folder chip
        //        composeTestRule.onNode(context.getString(R.string.archived)).performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.archived)) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
            )
            .performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Archived Note"))
                .fetchSemanticsNodes()
                .isNotEmpty() &&
                composeTestRule.onAllNodes(hasText("Normal Note")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Archived Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Normal Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("Deleted Note").assertDoesNotExist()

        // 6. Switch filter to "Deleted" folder chip
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.deleted)) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
            )
            .performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Deleted Note"))
                .fetchSemanticsNodes()
                .isNotEmpty() &&
                composeTestRule.onAllNodes(hasText("Archived Note")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Deleted Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Normal Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("Archived Note").assertDoesNotExist()

        // 7. Switch filter back to "Notes" folder chip
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.notes)) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
            )
            .performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Normal Note")).fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodes(hasText("Deleted Note")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Normal Note").assertIsDisplayed()

        // 8. Search for keyword with no matches
        composeTestRule
            .onNode(hasText("Grocery") and hasSetTextAction())
            .performTextReplacement("NonExistentKeyword")
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Normal Note")).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Normal Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("Archived Note").assertDoesNotExist()
        composeTestRule.onNodeWithText("Deleted Note").assertDoesNotExist()

        // 9. Cancel search to return to main notes list
        composeTestRule
            .onNode(hasContentDescription(cancelLabel) and hasTestTag(TestTags.NAVIGATION))
            .performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Work Meeting")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Work Meeting").assertIsDisplayed()

        scenario.close()
    }
}
