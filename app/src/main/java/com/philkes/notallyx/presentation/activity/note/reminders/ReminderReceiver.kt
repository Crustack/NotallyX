package com.philkes.notallyx.presentation.activity.note.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.findLastNotified
import com.philkes.notallyx.data.model.lastNotification
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.utils.PinnedNotificationManager
import com.philkes.notallyx.utils.canScheduleAlarms
import com.philkes.notallyx.utils.cancelReminder
import com.philkes.notallyx.utils.createChannelIfNotExists
import com.philkes.notallyx.utils.scheduleReminder
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [BroadcastReceiver] for sending notifications via [NotificationManager] for [Reminder]s.
 * Reschedules reminders on [Intent.ACTION_BOOT_COMPLETED] or if
 * [AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED] has changed and exact alarms
 * are allowed. For [Reminder] that have [Reminder.repetition] it automatically reschedules the next
 * alarm.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: ${intent?.action}")
        if (intent == null || context == null) {
            return
        }
        val canScheduleExactAlarms = context.canScheduleAlarms()
        goAsyncScope {
            if (intent.action == null) {
                if (!canScheduleExactAlarms) {
                    return@goAsyncScope
                }
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                notify(context, noteId, reminderId)
            } else {
                when {
                    intent.action == Intent.ACTION_BOOT_COMPLETED -> {
                        if (canScheduleExactAlarms) {
                            rescheduleAlarms(context)
                        }
                        restoreRemindersNotifications(context)
                        restorePinnedNotifications(context)
                    }

                    intent.action ==
                        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        if (canScheduleExactAlarms) {
                            rescheduleAlarms(context)
                        } else {
                            cancelAlarms(context)
                        }
                    }

                    intent.action == ACTION_NOTIFICATION_DISMISSED -> {
                        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        Log.d(
                            TAG,
                            "Notification dismissed for noteId: $noteId, reminderId: $reminderId",
                        )
                        setIsNotificationVisible(false, context, noteId, reminderId)
                    }

                    intent.action == ACTION_UNPIN_NOTE -> {
                        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                        Log.d(TAG, "Unpin note: $noteId")
                        if (noteId != -1L) {
                            unpinNote(context, noteId)
                        }
                    }

                    intent.action == ACTION_PINNED_NOTIFICATION_DISMISSED -> {
                        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                        Log.d(TAG, "Pinned notification dismissed for noteId: $noteId")
                        if (noteId != -1L) {
                            reNotifyStatusNotification(context, noteId)
                        }
                    }

                    intent.action == ACTION_DELETE_REMINDER -> {
                        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        Log.d(TAG, "Deleting reminderId: $reminderId of noteId: $noteId")
                        if (noteId != -1L && reminderId != -1L) {
                            deleteReminder(context, noteId, reminderId)
                        }
                    }
                }
            }
        }
    }

    private fun reNotifyStatusNotification(context: Context, noteId: Long) {
        val database = getDatabase(context)
        database.getBaseNoteDao().get(noteId)?.let { note ->
            if (note.isPinnedToStatus) {
                PinnedNotificationManager.notify(context, note)
            }
        }
    }

    private fun unpinNote(context: Context, noteId: Long) {
        val database = getDatabase(context)
        database.getBaseNoteDao().updatePinnedToStatus(noteId, false)
        PinnedNotificationManager.cancel(context, noteId)
    }

    private suspend fun deleteReminder(context: Context, noteId: Long, reminderId: Long) {
        context.getSystemService<NotificationManager>()?.let { manager ->
            manager.cancel(NOTIFICATION_TAG, noteId.toInt())
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    manager.activeNotifications.none {
                        it.tag == NOTIFICATION_TAG && it.id != noteId.toInt()
                    }
            ) {
                manager.cancel(SUMMARY_ID)
            }
        }
        context.cancelReminder(noteId, reminderId)
        val baseNoteDao = getDatabase(context).getBaseNoteDao()
        baseNoteDao.get(noteId)?.let { note ->
            val updatedReminders = note.reminders.filter { it.id != reminderId }
            baseNoteDao.updateReminders(noteId, updatedReminders)
        }
    }

    private suspend fun notify(
        context: Context,
        noteId: Long,
        reminderId: Long,
        schedule: Boolean = true,
    ) {
        Log.d(TAG, "notify: noteId: $noteId reminderId: $reminderId")
        val database = getDatabase(context)
        val manager = context.getSystemService<NotificationManager>()!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createChannelIfNotExists(
                NOTIFICATION_CHANNEL_ID,
                importance = NotificationManager.IMPORTANCE_HIGH,
            )
        }
        database.getBaseNoteDao().get(noteId)?.let { note ->
            val deleteReminderIntent =
                Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_DELETE_REMINDER
                    putExtra(EXTRA_NOTE_ID, note.id)
                    putExtra(EXTRA_REMINDER_ID, reminderId)
                }
            val deleteReminderPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    "${note.id}-${reminderId}".hashCode(),
                    deleteReminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val (notification, summaryNotification) =
                context.createNotification(
                    note,
                    reminderId,
                    NOTIFICATION_CHANNEL_ID,
                    GROUP_REMINDERS,
                    context.getString(R.string.reminders),
                    actions =
                        listOf(
                            NotificationCompat.Action(
                                R.drawable.notification_delete,
                                context.getString(R.string.delete_reminder),
                                deleteReminderPendingIntent,
                            )
                        ),
                )
            note.reminders
                .find { it.id == reminderId }
                ?.let { reminder: Reminder ->
                    setIsNotificationVisible(true, context, note.id, reminderId)
                    manager.notify(NOTIFICATION_TAG, note.id.toInt(), notification)
                    manager.notify(SUMMARY_ID, summaryNotification)
                    if (schedule)
                        context.scheduleReminder(note.id, reminder, forceRepetition = true)
                }
        }
    }

    private suspend fun rescheduleAlarms(context: Context) {
        val database = getDatabase(context)
        val now = Date()
        val noteReminders = database.getBaseNoteDao().getAllReminders()
        val noteRemindersWithFutureNotify =
            noteReminders.flatMap { (noteId, reminders) ->
                reminders
                    .filter { reminder ->
                        reminder.repetition != null || reminder.dateTime.after(now)
                    }
                    .map { reminder -> Pair(noteId, reminder) }
            }
        Log.d(TAG, "rescheduleAlarms: ${noteRemindersWithFutureNotify.size} alarms")
        noteRemindersWithFutureNotify.forEach { (noteId, reminder) ->
            context.scheduleReminder(noteId, reminder)
        }
    }

    private suspend fun cancelAlarms(context: Context) {
        val database = getDatabase(context)
        val noteReminders = database.getBaseNoteDao().getAllReminders()
        val noteRemindersWithFutureNotify =
            noteReminders.flatMap { (noteId, reminders) ->
                reminders.map { reminder -> Pair(noteId, reminder.id) }
            }
        Log.d(TAG, "cancelAlarms: ${noteRemindersWithFutureNotify.size} alarms")
        noteRemindersWithFutureNotify.forEach { (noteId, reminderId) ->
            context.cancelReminder(noteId, reminderId)
        }
    }

    private suspend fun setIsNotificationVisible(
        isNotificationVisible: Boolean,
        context: Context,
        noteId: Long,
        reminderId: Long,
    ) {
        val baseNoteDao = getDatabase(context).getBaseNoteDao()
        val note = baseNoteDao.get(noteId) ?: return
        val currentReminders = note.reminders.toMutableList()
        val index = currentReminders.indexOfFirst { it.id == reminderId }
        if (index != -1) {
            if (currentReminders[index].isNotificationVisible != isNotificationVisible) {
                currentReminders[index] =
                    currentReminders[index].copy(isNotificationVisible = isNotificationVisible)
                baseNoteDao.updateReminders(noteId, currentReminders)
            }
        }
    }

    private suspend fun restoreRemindersNotifications(context: Context) {
        val baseNoteDao = getDatabase(context).getBaseNoteDao()
        val allNotes = baseNoteDao.getAllNotes()
        allNotes.forEach { note ->
            val mostRecentReminder = note.reminders.findLastNotified() ?: return@forEach
            if (mostRecentReminder.isNotificationVisible) {
                Log.d(
                    TAG,
                    "restoreRemindersNotifications: Notifying noteID: ${note.id} reminderId: ${mostRecentReminder.id} from ${mostRecentReminder.lastNotification()?.format()}",
                )
                notify(context, note.id, mostRecentReminder.id, schedule = false)
            }
        }
    }

    private suspend fun restorePinnedNotifications(context: Context) {
        val baseNoteDao = getDatabase(context).getBaseNoteDao()
        val allNotes = baseNoteDao.getAllPinnedToStatusNotes()
        allNotes
            .filter { it.isPinnedToStatus }
            .forEach { note ->
                Log.d(TAG, "restorePinnedNotifications: Pinning noteID: ${note.id} to status bar")
                PinnedNotificationManager.notify(context, note)
            }
    }

    private fun getDatabase(context: Context): NotallyDatabase {
        return NotallyDatabase.getDatabase(context.applicationContext as Application, false).value
    }

    private fun goAsyncScope(codeBlock: suspend CoroutineScope.() -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                codeBlock()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"

        private const val SUMMARY_ID = -2
        private const val NOTIFICATION_CHANNEL_ID = "Reminders"
        private const val GROUP_REMINDERS = "notallyx.notifications.group.2.reminders"
        private const val NOTIFICATION_TAG = "notallyx.notifications.note-reminders"

        const val EXTRA_REMINDER_ID = "notallyx.intent.extra.REMINDER_ID"
        const val EXTRA_NOTE_ID = "notallyx.intent.extra.NOTE_ID"
        const val ACTION_NOTIFICATION_DISMISSED =
            "com.philkes.notallyx.ACTION_NOTIFICATION_DISMISSED"
        const val ACTION_UNPIN_NOTE = "com.philkes.notallyx.ACTION_UNPIN_NOTE"
        const val ACTION_PINNED_NOTIFICATION_DISMISSED =
            "com.philkes.notallyx.ACTION_STATUS_NOTIFICATION_DISMISSED"
        const val ACTION_DELETE_REMINDER = "com.philkes.notallyx.ACTION_DELETE_REMINDER"
    }
}
