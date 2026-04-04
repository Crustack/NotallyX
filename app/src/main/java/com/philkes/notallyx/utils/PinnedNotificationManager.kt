package com.philkes.notallyx.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.presentation.activity.note.reminders.ReminderReceiver
import com.philkes.notallyx.presentation.activity.note.reminders.createNotification

object PinnedNotificationManager {
    private const val NOTIFICATION_TAG = "notallyx.notifications.pinned-notes"
    private const val NOTIFICATION_CHANNEL_ID = "Pinned Notes"
    private const val GROUP_PINNED = "notallyx.notifications.group.1.pinned"
    private const val SUMMARY_ID = -1

    fun notify(context: Context, note: BaseNote) {
        val manager = context.getSystemService<NotificationManager>()!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createChannelIfNotExists(NOTIFICATION_CHANNEL_ID)
        }
        val unpinIntent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_UNPIN_NOTE
                putExtra(ReminderReceiver.EXTRA_NOTE_ID, note.id)
            }
        val unpinPendingIntent =
            PendingIntent.getBroadcast(
                context,
                note.id.toInt(),
                unpinIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val (notification, summaryNotification) =
            context.createNotification(
                note,
                null,
                NOTIFICATION_CHANNEL_ID,
                GROUP_PINNED,
                context.getString(R.string.pinned),
                ongoing = true,
                silent = true,
                iconResId = R.drawable.unpin,
                summaryIconResId = R.drawable.pinboard_filled,
                actions =
                    listOf(
                        NotificationCompat.Action(
                            R.drawable.pin,
                            context.getString(R.string.unpin),
                            unpinPendingIntent,
                        )
                    ),
            )
        manager.notify(NOTIFICATION_TAG, note.id.toInt(), notification)
        manager.notify(SUMMARY_ID, summaryNotification)
    }

    fun cancel(context: Context, noteId: Long) {
        val manager = context.getSystemService<NotificationManager>()!!
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
}
