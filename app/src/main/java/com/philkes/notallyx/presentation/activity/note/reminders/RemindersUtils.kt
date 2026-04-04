package com.philkes.notallyx.presentation.activity.note.reminders

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.presentation.activity.note.reminders.ReminderReceiver.Companion.EXTRA_NOTE_ID
import com.philkes.notallyx.presentation.activity.note.reminders.ReminderReceiver.Companion.EXTRA_REMINDER_ID
import com.philkes.notallyx.utils.getOpenNotePendingIntent

fun Context.createNotification(
    note: BaseNote,
    reminderId: Long?,
    channelId: String,
    group: String,
    subtext: String,
    ongoing: Boolean? = null,
    silent: Boolean? = null,
    iconResId: Int = R.drawable.notebook,
    summaryIconResId: Int = R.drawable.notebook_multiple,
    actions: Collection<NotificationCompat.Action> = listOf(),
): Pair<Notification, Notification> {
    val contentText =
        if (note.type == com.philkes.notallyx.data.model.Type.LIST) {
            note.items.joinToString("\n") { (if (it.checked) "✅ " else "🔳 ") + it.body }
        } else {
            note.body
        }

    val bigText =
        if (note.type == com.philkes.notallyx.data.model.Type.LIST) {
            note.items.joinToString("\n") { (if (it.checked) "✅ " else "🔳 ") + it.body }
        } else {
            note.body
        }
    val notification =
        NotificationCompat.Builder(this, channelId)
            .apply {
                setSubText(subtext)
                setSmallIcon(iconResId)
                setCategory(NotificationCompat.CATEGORY_REMINDER)
                setContentTitle(note.title.ifEmpty { getString(R.string.note) })
                setContentText(contentText)
                setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                setGroup(group)
                val openIntent = getOpenNotePendingIntent(note)
                setContentIntent(openIntent)
                addAction(R.drawable.visibility, getString(R.string.open_note), openIntent)
                actions.forEach { addAction(it) }
                setDeleteIntent(
                    createDeleteReminderIntent(note.id, reminderId, requestCodePostfix = group)
                )
                ongoing?.let {
                    setOngoing(it)
                    setAutoCancel(!it)
                }
                silent?.let { setSilent(it) }
            }
            .build()
    val summaryNotification =
        NotificationCompat.Builder(this, channelId)
            .apply {
                setSubText(subtext)
                setSmallIcon(summaryIconResId)
                setCategory(NotificationCompat.CATEGORY_REMINDER)
                setGroup(group)
                setGroupSummary(true)
                setSortKey(group)
                ongoing?.let {
                    setOngoing(it)
                    setAutoCancel(!it)
                }
                silent?.let { setSilent(it) }
            }
            .build()

    return Pair(notification, summaryNotification)
}

fun Context.createDeleteReminderIntent(
    noteId: Long,
    reminderId: Long?,
    requestCodePostfix: String? = null,
): PendingIntent? {
    require(reminderId != null || requestCodePostfix != null) {
        "Either reminderId or requestCodePostfix must be non-null"
    }
    val deleteIntent =
        Intent(this, ReminderReceiver::class.java).apply {
            action =
                if (reminderId != null) ReminderReceiver.ACTION_NOTIFICATION_DISMISSED
                else ReminderReceiver.ACTION_PINNED_NOTIFICATION_DISMISSED
            putExtra(EXTRA_NOTE_ID, noteId)
            reminderId?.let { putExtra(EXTRA_REMINDER_ID, it) }
        }
    return PendingIntent.getBroadcast(
        this,
        "$noteId-${reminderId ?: requestCodePostfix!!}".hashCode(),
        deleteIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
