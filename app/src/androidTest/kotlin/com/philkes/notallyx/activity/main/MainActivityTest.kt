package com.philkes.notallyx.activity.main

import android.app.Activity
import android.app.Instrumentation
import android.app.Notification
import android.content.Intent
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSettingsValue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
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
                            timestamp = 3000L,
                            modifiedTimestamp = 3000L,
                        ),
                        createBaseNote(
                            title = "List A",
                            pinned = true,
                            items =
                                listOf(createListItem("A"), createListItem("B", isChild = true)),
                            timestamp = 2000L,
                            modifiedTimestamp = 2000L,
                        ),
                        createBaseNote(
                            title = "List B",
                            pinned = true,
                            items = listOf(createListItem("C"), createListItem("D")),
                            timestamp = 1000L,
                            modifiedTimestamp = 1000L,
                        ),
                    )
                )
        }
        // 2. Recreate Activity so it reads the newly inserted data
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 1. longclick the first note ("Test")
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }
        // 2. longclick the 3rd note ("List B") to also select List A and List B
        composeTestRule.onNodeWithText("List B").performTouchInput { longClick() }

        // Assert all 3 notes are selected (action mode count is 3)
        composeTestRule.onNodeWithText("3").assertIsDisplayed()

        // 3. Press unpin action to unpin all notes
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.unpin))
            .performClick()

        // 4. Select all notes again in reverse direction (longclick List B, then longclick Test)
        composeTestRule.onNodeWithText("List B").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }

        // Assert all 3 notes are selected again
        composeTestRule.onNodeWithText("3").assertIsDisplayed()

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
        composeTestRule.onAllNodes(hasText("label"))[0].assertIsDisplayed()
        composeTestRule.onAllNodes(hasText("label"))[1].assertIsDisplayed()
        composeTestRule.onAllNodes(hasText("label"))[2].assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun moreOptions() {
        // 1. Prepare test data: 3 notes
        val note1 =
            createBaseNote(
                title = "Note 1",
                body = "Body 1",
                timestamp = 3000L,
                modifiedTimestamp = 3000L,
            )
        val note2 =
            createBaseNote(
                title = "Note 2",
                body = "Body 2",
                timestamp = 2000L,
                modifiedTimestamp = 2000L,
            )
        val note3 =
            createBaseNote(
                title = "Note 3",
                body = "Body 3",
                timestamp = 1000L,
                modifiedTimestamp = 1000L,
            )
        runBlocking {
            database.clearAllTables()
            database.getBaseNoteDao().insert(listOf(note1, note2, note3))
        }

        Intents.init()
        try {
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            `is`(Intent.EXTRA_INTENT),
                            hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
                        ),
                    )
                )
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            val moreOptionsLabel = context.getString(R.string.tap_for_more_options)
            val cancelLabel = context.getString(R.string.cancel)
            val selectAllLabel = context.getString(R.string.select_all)
            val saveLabel = context.getString(R.string.save)

            // 1. Test onClose()
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(cancelLabel).performClick()
            composeTestRule.onNodeWithText("2").assertDoesNotExist()

            // 2. Test onSelectAll()
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(selectAllLabel).performClick()
            composeTestRule.onNodeWithText("3").assertIsDisplayed()

            // 3. Test onPin() (Pin all notes)
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.pin))
                .performClick()
            waitUntil(10_000) {
                runBlocking { database.getBaseNoteDao().getAll().all { it.pinned } }
            }

            // Unpin the selected notes
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.unpin))
                .performClick()
            waitUntil(10_000) {
                runBlocking {
                    val all = database.getBaseNoteDao().getAll()
                    all.filter { it.title in listOf("Note 1", "Note 2") }.none { it.pinned }
                }
            }

            // 4. Test overflow menu and verify onShare is NOT displayed for multiple selected notes
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.share)).assertDoesNotExist()

            // 5. Test onChangeColor()
            composeTestRule.onNodeWithText(context.getString(R.string.change_color)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(Color.CORAL.toColorString()))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
            composeTestRule.onNodeWithText(saveLabel).performClick()

            waitUntil(10_000) {
                runBlocking {
                    val notes = database.getBaseNoteDao().getAll()
                    notes
                        .filter { it.title in listOf("Note 1", "Note 2") }
                        .all { it.color == Color.CORAL.toColorString() }
                }
            }

            // 6. Test onPinToStatus() (pin to status bar and unpin)
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.pin_to_status_bar))
                .performClick()

            waitUntil(10_000) {
                val pinned = PinnedNotificationManager.getPinnedNotifications(context)
                pinned.size >= 2
            }

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.unpin_from_status_bar))
                .performClick()

            waitUntil(10_000) {
                PinnedNotificationManager.getPinnedNotifications(context).isEmpty()
            }

            // 7. Test onDuplicate()
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.duplicate)).performClick()

            val copySuffix = context.getString(R.string.copy)
            val copyNote1 = "Note 1 ($copySuffix)"
            val copyNote2 = "Note 2 ($copySuffix)"
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(copyNote1))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText(copyNote2))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }
            composeTestRule.onNodeWithText(copyNote1).assertIsDisplayed()
            composeTestRule.onNodeWithText(copyNote2).assertIsDisplayed()

            // 8. Test onExport()
            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.export)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(ExportMimeType.TXT.name))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(ExportMimeType.TXT.name).performClick()

            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        `is`(Intent.EXTRA_INTENT),
                        hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
                    ),
                )
            )

            // 9. Test onArchive()
            if (
                composeTestRule
                    .onAllNodes(hasText("2"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
            ) {
                composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
                composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            }
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.archive)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Note 2").assertDoesNotExist()

            // 10. Test onRestore() from Archived folder
            composeTestRule.navigateTo(NotallyNavKey.Archived)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertIsDisplayed()
            composeTestRule.onNodeWithText("Note 2").assertIsDisplayed()

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.unarchive))
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Note 2").assertDoesNotExist()

            // 11. Test onDelete() in Notes folder
            composeTestRule.navigateTo(NotallyNavKey.Notes)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertIsDisplayed()
            composeTestRule.onNodeWithText("Note 2").assertIsDisplayed()

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Note 2").assertDoesNotExist()

            // 12. In Deleted folder: test onRestore() for Note 1 and Note 2
            composeTestRule.navigateTo(NotallyNavKey.Deleted)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertIsDisplayed()
            composeTestRule.onNodeWithText("Note 2").assertIsDisplayed()

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.restore))
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Note 2").assertDoesNotExist()

            // 13. In Notes folder: delete Note 1 and Note 2 again, then test onDeleteForever() in
            // Deleted folder
            composeTestRule.navigateTo(NotallyNavKey.Notes)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()

            composeTestRule.navigateTo(NotallyNavKey.Deleted)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }

            composeTestRule.onNodeWithText("Note 1").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("Note 2").performTouchInput { longClick() }
            composeTestRule.onNodeWithText("2").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete_forever))
                .performClick()

            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Note 1"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Note 2"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }
            composeTestRule.onNodeWithText("Note 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Note 2").assertDoesNotExist()

            waitUntil(10_000) {
                runBlocking {
                    val all = database.getBaseNoteDao().getAll()
                    all.none { it.title in listOf("Note 1", "Note 2") }
                }
            }

            scenario.close()
        } finally {
            Intents.release()
        }
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

    @Test
    fun sortNotesOrderDialog() {
        // 1. Pre-fill database with test notes
        val note1 =
            createBaseNote(
                title = "Alpha Note",
                body = "Body 1",
                timestamp = 1000L,
                modifiedTimestamp = 3000L,
                color = BaseNote.COLOR_DEFAULT,
            )
        val note2 =
            createBaseNote(
                title = "Beta Note",
                body = "Body 2",
                timestamp = 2000L,
                modifiedTimestamp = 1000L,
                color = "#FFFF00",
            )
        val note3 =
            createBaseNote(
                title = "Gamma Note",
                body = "Body 3",
                timestamp = 3000L,
                modifiedTimestamp = 2000L,
                color = "#FF0000",
            )
        runBlocking { database.getBaseNoteDao().insert(listOf(note1, note2, note3)) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val notesSortedByLabel = context.getString(R.string.notes_sorted_by)
        val titleLabel = context.getString(R.string.title)
        val ascendingLabel = context.getString(R.string.ascending)
        val descendingLabel = context.getString(R.string.descending)
        val saveLabel = context.getString(R.string.save)

        // 2. Navigate to Settings screen
        composeTestRule.navigateTo(NotallyNavKey.Settings)

        // 3. Open NotesSortDialog
        composeTestRule.onNodeWithText(notesSortedByLabel).performScrollTo().performClick()

        // 4. Select Title and Ascending in dialog
        composeTestRule
            .onNode(hasText(titleLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(ascendingLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(saveLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // 5. Verify preferences and settings UI state
        composeTestRule.waitUntilSettingsValue(
            R.string.notes_sorted_by,
            "$titleLabel ($ascendingLabel)",
        )
        assertEquals(
            NotesSort(sortedBy = NotesSortBy.TITLE, sortDirection = SortDirection.ASC),
            preferences.notesSorting.value,
        )

        // 6. Return to Notes main screen and verify items are ordered Title Ascending (Alpha < Beta
        // < Gamma)
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Alpha Note")).fetchSemanticsNodes().isNotEmpty()
        }
        val alphaY =
            composeTestRule.onNodeWithText("Alpha Note").fetchSemanticsNode().positionInRoot.y
        val betaY =
            composeTestRule.onNodeWithText("Beta Note").fetchSemanticsNode().positionInRoot.y
        val gammaY =
            composeTestRule.onNodeWithText("Gamma Note").fetchSemanticsNode().positionInRoot.y
        assertTrue("Alpha ($alphaY) should be above Beta ($betaY)", alphaY < betaY)
        assertTrue("Beta ($betaY) should be above Gamma ($gammaY)", betaY < gammaY)

        // 7. Return to Settings, re-open dialog and change to Title and Descending
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(notesSortedByLabel).performScrollTo().performClick()

        composeTestRule
            .onNode(hasText(titleLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(descendingLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(saveLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // 8. Verify updated preferences
        composeTestRule.waitUntilSettingsValue(
            R.string.notes_sorted_by,
            "$titleLabel ($descendingLabel)",
        )
        assertEquals(
            NotesSort(sortedBy = NotesSortBy.TITLE, sortDirection = SortDirection.DESC),
            preferences.notesSorting.value,
        )

        // 9. Return to Notes main screen and verify items are ordered Title Descending (Gamma <
        // Beta < Alpha)
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Gamma Note")).fetchSemanticsNodes().isNotEmpty()
        }
        val alphaY2 =
            composeTestRule.onNodeWithText("Alpha Note").fetchSemanticsNode().positionInRoot.y
        val betaY2 =
            composeTestRule.onNodeWithText("Beta Note").fetchSemanticsNode().positionInRoot.y
        val gammaY2 =
            composeTestRule.onNodeWithText("Gamma Note").fetchSemanticsNode().positionInRoot.y
        assertTrue("Gamma ($gammaY2) should be above Beta ($betaY2)", gammaY2 < betaY2)
        assertTrue("Beta ($betaY2) should be above Alpha ($alphaY2)", betaY2 < alphaY2)

        scenario.close()
    }

    @Test
    fun toggleNotesViewMode() {
        // 1. Seed database with test notes
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(title = "First Grid Note", body = "Grid Content 1"),
                        createBaseNote(title = "Second Grid Note", body = "Grid Content 2"),
                    )
                )
        }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val viewLabel = context.getString(R.string.view)
        val gridLabel = context.getString(R.string.grid)
        val listLabel = context.getString(R.string.list)
        val saveLabel = context.getString(R.string.save)

        // 2. Navigate to Settings
        composeTestRule.navigateTo(NotallyNavKey.Settings)

        // 3. Open View Mode selection dialog
        composeTestRule.onNodeWithText(viewLabel).performScrollTo().performClick()

        // 4. Select "Grid" mode and save
        composeTestRule
            .onNode(hasText(gridLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(saveLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // 5. Verify preference updated to GRID
        composeTestRule.waitUntilSettingsValue(R.string.view, R.string.grid)
        assertEquals(NotesView.GRID, preferences.notesView.value)

        // 6. Navigate back to Notes and verify cards are visible
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("First Grid Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("First Grid Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Grid Note").assertIsDisplayed()

        // 7. Navigate back to Settings and switch back to "List" mode
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(viewLabel).performScrollTo().performClick()

        composeTestRule
            .onNode(hasText(listLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule
            .onNode(hasText(saveLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // 8. Verify preference updated to LIST
        composeTestRule.waitUntilSettingsValue(R.string.view, R.string.list)
        assertEquals(NotesView.LIST, preferences.notesView.value)

        // 9. Navigate back to Notes screen and verify cards are displayed
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("First Grid Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("First Grid Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Grid Note").assertIsDisplayed()

        scenario.close()
    }
}
