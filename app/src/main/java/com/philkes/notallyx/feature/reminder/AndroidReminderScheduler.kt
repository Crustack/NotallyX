package com.philkes.notallyx.feature.reminder

import android.content.Context
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun scheduleReminder(
        noteId: Long,
        reminder: Reminder,
        forceRepetition: Boolean,
    ) {
        context.scheduleReminder(noteId, reminder, forceRepetition)
    }

    override fun cancelReminder(noteId: Long, reminderId: Long) {
        context.cancelReminder(noteId, reminderId)
    }

    override fun cancelAllRemindersForNote(noteId: Long, reminders: List<Reminder>) {
        context.cancelPinAndReminders(noteId, reminders)
    }

    override fun cancelAllRemindersForNotes(notes: Collection<BaseNote>) {
        context.cancelPinAndReminders(notes.toList())
    }

    override fun rescheduleAll(notesWithReminders: List<BaseNote>) {
        context.pinAndScheduleReminders(notesWithReminders)
    }

    override fun pinNoteToNotificationBar(note: BaseNote) {
        PinnedNotificationManager.notify(context, note)
    }

    override fun unpinNoteFromNotificationBar(noteId: Long) {
        PinnedNotificationManager.cancel(context, noteId)
    }

    override fun canScheduleExactAlarms(): Boolean = context.canScheduleAlarms()
}
