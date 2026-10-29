package com.philkes.notallyx.activity.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.getSystemService
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.feature.reminder.ReminderReceiver
import com.philkes.notallyx.feature.reminder.cancelReminder
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.waitUntil
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RemindersActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    /**
     * 1. System Notification Triggering: Firing alarm broadcast (ReminderReceiver) and verifying
     *    the notification appears in the system tray with title, body, action buttons, summary
     *    notification, and updates isNotificationVisible.
     */
    @Test
    fun test1_triggerSystemNotificationViaAlarmBroadcast() {
        val reminder =
            Reminder(
                id = 101L,
                dateTime = Date(System.currentTimeMillis() + 60_000),
                repetition = null,
                isNotificationVisible = false,
            )
        val note =
            createBaseNote(
                title = "Reminder Alarm Title",
                body = "Reminder Alarm Body Content",
                reminders = listOf(reminder),
            )
        val noteId = runBlocking { database.getBaseNoteDao().insert(note) }

        try {
            // Fire alarm broadcast to ReminderReceiver
            val alarmIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                }
            context.sendBroadcast(alarmIntent)

            val notificationManager = context.getSystemService<NotificationManager>()!!
            val expectedTag = ReminderReceiver.reminderNotificationTag(noteId)

            // Verify notification is posted in NotificationManager
            waitUntil(10_000) {
                notificationManager.activeNotifications.any {
                    it.tag == expectedTag && it.id == reminder.id.toInt()
                }
            }

            val activeNotification =
                notificationManager.activeNotifications.first {
                    it.tag == expectedTag && it.id == reminder.id.toInt()
                }
            assertNotNull(activeNotification)

            val notification = activeNotification.notification
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            assertEquals("Reminder Alarm Title", title)
            assertEquals("Reminder Alarm Body Content", text)

            // Verify Open Note and Delete actions exist
            val openNoteLabel = context.getString(R.string.open_note)
            val deleteLabel = context.getString(R.string.delete)
            val actionTitles = notification.actions.map { it.title.toString() }
            assertTrue(actionTitles.any { it.equals(openNoteLabel, ignoreCase = true) })
            assertTrue(actionTitles.any { it.equals(deleteLabel, ignoreCase = true) })

            // Verify group summary notification was posted
            assertTrue(
                notificationManager.activeNotifications.any { it.id == ReminderReceiver.SUMMARY_ID }
            )

            // Verify database has isNotificationVisible = true
            waitUntil(5_000) {
                val updatedNote = runBlocking { database.getBaseNoteDao().get(noteId) }
                updatedNote
                    ?.reminders
                    ?.firstOrNull { it.id == reminder.id }
                    ?.isNotificationVisible == true
            }
        } finally {
            context.cancelReminder(noteId, reminder.id)
        }
    }

    /**
     * 2. Notification Direct Actions: Interacting with notification direct actions:
     * - Open Note action / Content Intent: opens target note in EditNoteActivity
     * - Delete Note action: moves note to Trash (Folder.DELETED)
     * - Notification dismissal: updates isNotificationVisible = false
     */
    @Test
    fun test2_triggerNotificationAndPerformActions() {
        val reminder =
            Reminder(
                id = 202L,
                dateTime = Date(System.currentTimeMillis() + 60_000),
                repetition = null,
                isNotificationVisible = false,
            )
        val note =
            createBaseNote(
                title = "Actionable Reminder Note",
                body = "Body for actionable notification",
                reminders = listOf(reminder),
            )
        val noteId = runBlocking { database.getBaseNoteDao().insert(note) }

        try {
            // Trigger notification
            val alarmIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                }
            context.sendBroadcast(alarmIntent)

            val notificationManager = context.getSystemService<NotificationManager>()!!
            val expectedTag = ReminderReceiver.reminderNotificationTag(noteId)

            waitUntil(10_000) {
                notificationManager.activeNotifications.any {
                    it.tag == expectedTag && it.id == reminder.id.toInt()
                }
            }

            val activeNotification =
                notificationManager.activeNotifications.first {
                    it.tag == expectedTag && it.id == reminder.id.toInt()
                }
            val notification = activeNotification.notification

            // Action 1: Tap notification / content intent -> opens note in EditNoteActivity
            assertNotNull(notification.contentIntent)
            notification.contentIntent.send()

            val backLabel = context.getString(R.string.back)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Actionable Reminder Note"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Actionable Reminder Note").assertIsDisplayed()

            // Navigate back to close EditNoteActivity
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()

            // Action 2: Trigger Notification Delete Action
            val deleteLabel = context.getString(R.string.delete)
            val deleteAction =
                notification.actions.first {
                    it.title.toString().equals(deleteLabel, ignoreCase = true)
                }
            assertNotNull(deleteAction.actionIntent)
            deleteAction.actionIntent.send()

            // Verify note moved to Folder.DELETED
            waitUntil(10_000) {
                val dbNote = runBlocking { database.getBaseNoteDao().get(noteId) }
                dbNote?.folder == Folder.DELETED
            }

            // Action 3: Notification dismissal intent sets isNotificationVisible = false
            // Create a second note to test dismissal
            val reminder2 =
                Reminder(
                    id = 203L,
                    dateTime = Date(System.currentTimeMillis() + 60_000),
                    repetition = null,
                    isNotificationVisible = true,
                )
            val note2 =
                createBaseNote(
                    title = "Dismissible Reminder Note",
                    body = "Body for dismissible notification",
                    reminders = listOf(reminder2),
                )
            val note2Id = runBlocking { database.getBaseNoteDao().insert(note2) }

            val dismissIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_NOTIFICATION_DISMISSED
                    putExtra(ReminderReceiver.EXTRA_NOTE_ID, note2Id)
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder2.id)
                }
            context.sendBroadcast(dismissIntent)

            waitUntil(10_000) {
                val dbNote = runBlocking { database.getBaseNoteDao().get(note2Id) }
                dbNote?.reminders?.firstOrNull { it.id == reminder2.id }?.isNotificationVisible ==
                    false
            }
        } finally {
            context.cancelReminder(noteId, reminder.id)
        }
    }

    /**
     * 3. Editing and Deleting Existing Reminders in RemindersActivity:
     * - Opens RemindersActivity for a note pre-filled with multiple reminders.
     * - Edits an existing reminder's repetition (Daily -> Yearly) and verifies updates in UI & DB.
     * - Deletes an existing reminder with confirmation dialog and verifies deletion in UI & DB.
     * - Adds a new reminder from the FAB and saves it.
     */
    @Test
    fun test3_editAndDeleteExistingRemindersInRemindersActivity() {
        val now = System.currentTimeMillis()
        val reminder1 =
            Reminder(
                id = 301L,
                dateTime = Date(now + 24 * 60 * 60 * 1000L),
                repetition = Repetition(1, RepetitionTimeUnit.DAYS),
            )
        val reminder2 =
            Reminder(
                id = 302L,
                dateTime = Date(now + 48 * 60 * 60 * 1000L),
                repetition = Repetition(1, RepetitionTimeUnit.WEEKS),
            )
        val note =
            createBaseNote(
                title = "Note With Reminders",
                body = "Body text",
                reminders = listOf(reminder1, reminder2),
            )
        val noteId = runBlocking { database.getBaseNoteDao().insert(note) }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                putNavKey(NotallyNavKey.NoteReminders(noteId))
            }
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        val dailyLabel = context.getString(R.string.daily)
        val weeklyLabel = context.getString(R.string.weekly)
        val yearlyLabel = context.getString(R.string.yearly)
        val editLabel = context.getString(R.string.edit)
        val deleteLabel = context.getString(R.string.delete)
        val nextLabel = context.getString(R.string.next)
        val saveLabel = context.getString(R.string.save)

        // Verify both reminders are rendered initially
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(dailyLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(dailyLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(weeklyLabel).assertIsDisplayed()

        // 1. Edit existing reminder 1 (change repetition from Daily to Yearly)
        composeTestRule.onAllNodes(hasContentDescription(editLabel))[0].performClick()

        // Step through DatePicker -> Next
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText(nextLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(nextLabel).performClick()

        // Step through TimePicker -> Next
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText(nextLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(nextLabel).performClick()

        // In Repetition dialog, select Yearly and Save
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasText(yearlyLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(yearlyLabel).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        // Verify row updated to Yearly
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(yearlyLabel)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(yearlyLabel).assertIsDisplayed()

        // Verify database persistence of updated repetition
        waitUntil(10_000) {
            val dbNote = runBlocking { database.getBaseNoteDao().get(noteId) }
            dbNote?.reminders?.any { it.repetition?.unit == RepetitionTimeUnit.YEARS } == true
        }

        // 2. Delete existing reminder 2 (Weekly)
        composeTestRule.onAllNodes(hasContentDescription(deleteLabel))[1].performClick()

        // Confirm deletion in ConfirmDialog
        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(
                    hasText(deleteLabel) and hasAnyAncestor(isComposeDialog()) and hasClickAction()
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNode(hasText(deleteLabel) and hasAnyAncestor(isComposeDialog()) and hasClickAction())
            .performClick()

        // Verify weekly reminder row is removed
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(weeklyLabel)).fetchSemanticsNodes().isEmpty()
        }

        // Verify database has only 1 reminder remaining
        waitUntil(10_000) {
            val dbNote = runBlocking { database.getBaseNoteDao().get(noteId) }
            dbNote?.reminders?.size == 1
        }

        scenario.close()
    }

    /**
     * 4. Live Pinned Notification Updates:
     * - Sets up a note pinned to status bar (isPinnedToStatus = true).
     * - Posts initial pinned notification via PinnedNotificationManager.
     * - Opens EditNoteActivity, edits title and body via UI.
     * - Navigates back, triggering auto-save and ACTION_UPDATE_NOTIFICATIONS broadcast.
     * - Asserts that the active status bar notification updates dynamically with new title and
     *   text.
     */
    @Test
    fun test4_livePinnedNotificationUpdatesWhenNoteEdited() {
        val originalNote =
            createBaseNote(
                title = "Original Pinned Title",
                body = "Original Pinned Body",
                isPinnedToStatus = true,
            )
        val noteId = runBlocking { database.getBaseNoteDao().insert(originalNote) }

        try {
            // Post initial pinned notification
            PinnedNotificationManager.notify(context, originalNote.copy(id = noteId))

            waitUntil(10_000) {
                PinnedNotificationManager.getPinnedNotifications(context).any {
                    it?.id?.toLong() == noteId &&
                        it.notification.extras.getString(Notification.EXTRA_TITLE) ==
                            "Original Pinned Title"
                }
            }

            // Launch MainActivity with EditNote navKey to edit the note
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_SELECTED_BASE_NOTE, noteId)
                }
            val scenario = ActivityScenario.launch<MainActivity>(intent)

            val titleLabel = context.getString(R.string.title)
            val bodyLabel = context.getString(R.string.note)
            val backLabel = context.getString(R.string.back)

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(titleLabel))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Replace title and body in the UI
            composeTestRule
                .onNodeWithContentDescription(titleLabel)
                .performTextReplacement("Live Updated Title")
            composeTestRule
                .onNodeWithContentDescription(bodyLabel)
                .performTextReplacement("Live Updated Body")

            // Press back to trigger auto-save (saveNote sends ACTION_UPDATE_NOTIFICATIONS)
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()

            // Verify pinned notification updates dynamically in status bar
            waitUntil(10_000) {
                PinnedNotificationManager.getPinnedNotifications(context).any {
                    it?.id?.toLong() == noteId &&
                        it.notification.extras.getString(Notification.EXTRA_TITLE) ==
                            "Live Updated Title" &&
                        it.notification.extras.getString(Notification.EXTRA_TEXT) ==
                            "Live Updated Body"
                }
            }

            val updatedPinned =
                PinnedNotificationManager.getPinnedNotifications(context).first {
                    it?.id?.toLong() == noteId
                }
            assertEquals(
                "Live Updated Title",
                updatedPinned?.notification?.extras?.getString(Notification.EXTRA_TITLE),
            )
            assertEquals(
                "Live Updated Body",
                updatedPinned?.notification?.extras?.getString(Notification.EXTRA_TEXT),
            )

            // Also test Unpin Action from the pinned notification
            val unpinAction =
                updatedPinned?.notification?.actions?.firstOrNull {
                    it.title.toString().equals(context.getString(R.string.unpin), ignoreCase = true)
                }
            assertNotNull(unpinAction)
            unpinAction?.actionIntent?.send()

            // Verify unpinned in DB and notification cancelled
            waitUntil(10_000) {
                val dbNote = runBlocking { database.getBaseNoteDao().get(noteId) }
                dbNote?.isPinnedToStatus == false &&
                    PinnedNotificationManager.getPinnedNotifications(context).none {
                        it?.id?.toLong() == noteId
                    }
            }

            scenario.close()
        } finally {
            PinnedNotificationManager.cancel(context, noteId)
        }
    }
}
