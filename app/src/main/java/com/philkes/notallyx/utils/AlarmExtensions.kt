package com.philkes.notallyx.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.getSystemService
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.nextRepetition
import com.philkes.notallyx.presentation.activity.note.reminders.ReminderReceiver
import com.philkes.notallyx.presentation.format
import java.util.Date

private const val TAG = "ReminderExtensions"

fun Context.scheduleReminder(noteId: Long, reminder: Reminder, forceRepetition: Boolean = false) {
    val now = Date()
    if (forceRepetition || reminder.dateTime.before(now)) {
        reminder.repetition?.let {
            val nextRepetition = reminder.nextRepetition(now)!!
            Log.d(
                TAG,
                "scheduleReminder: noteId: $noteId reminderId: ${reminder.id} nextRepetition: ${nextRepetition.format()}",
            )
            scheduleReminder(noteId, reminder.id, nextRepetition)
        }
    } else {
        Log.d(
            TAG,
            "scheduleReminder: noteId: $noteId reminderId: ${reminder.id} dateTime: ${reminder.dateTime.format()}",
        )
        scheduleReminder(noteId, reminder.id, reminder.dateTime)
    }
}

fun Context.pinAndScheduleReminders(notes: List<BaseNote>) {
    notes.forEach { note ->
        note.reminders.forEach { reminder -> scheduleReminder(note.id, reminder) }
        if (note.isPinnedToStatus) {
            PinnedNotificationManager.notify(this, note)
        }
    }
}

fun Context.cancelPinAndReminders(noteId: Long, reminders: List<Reminder>) {
    reminders.forEach { reminder -> cancelReminder(noteId, reminder.id) }
    PinnedNotificationManager.cancel(this, noteId)
}

fun Context.cancelPinAndReminders(notes: List<BaseNote>) {
    notes.forEach { note -> cancelPinAndReminders(note.id, note.reminders) }
}

@SuppressLint("ScheduleExactAlarm")
private fun Context.scheduleReminder(noteId: Long, reminderId: Long, dateTime: Date) {
    val pendingIntent = createReminderAlarmIntent(noteId, reminderId)
    val alarmManager = getSystemService<AlarmManager>()
    if (canScheduleAlarms()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dateTime.time,
                pendingIntent,
            )
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, dateTime.time, pendingIntent)
        }
    }
}

fun Array<StatusBarNotification>.noneExceptFor(noteId: Long, reminderId: Long) = none {
    ReminderReceiver.isReminderNotification(it.tag) &&
        !(it.tag == ReminderReceiver.reminderNotificationTag(noteId) && it.id == reminderId.toInt())
}

fun Context.cancelReminder(noteId: Long, reminderId: Long) {
    Log.d(TAG, "cancelReminder: noteId: $noteId reminderId: $reminderId")
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = createReminderAlarmIntent(noteId, reminderId)
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
    getSystemService<NotificationManager>()?.let { manager ->
        val notificationTag = ReminderReceiver.reminderNotificationTag(noteId)
        manager.cancel(notificationTag, reminderId.toInt())
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                manager.activeNotifications.noneExceptFor(noteId, reminderId)
        ) {
            Log.d(TAG, "cancelReminder: cancel reminder summary notification")
            manager.cancel(ReminderReceiver.SUMMARY_ID)
        }
    }
}

private fun Context.createReminderAlarmIntent(noteId: Long, reminderId: Long): PendingIntent {
    val intent = Intent(this, ReminderReceiver::class.java)
    intent.putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
    intent.putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
    return PendingIntent.getBroadcast(
        this,
        (noteId.toString() + reminderId.toString()).toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}

fun Context.canScheduleAlarms() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    } else true
