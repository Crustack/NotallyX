package com.philkes.notallyx.feature.reminder

import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder

/**
 * Platform contract for scheduling note reminders and status-bar pinned note notifications.
 * Isolates Android's `AlarmManager` and `NotificationManager` from shared ViewModels.
 */
interface ReminderScheduler {
    fun scheduleReminder(noteId: Long, reminder: Reminder, forceRepetition: Boolean = false)

    fun cancelReminder(noteId: Long, reminderId: Long)

    fun cancelAllRemindersForNote(noteId: Long, reminders: List<Reminder>)

    fun cancelAllRemindersForNotes(notes: Collection<BaseNote>)

    fun rescheduleAll(notesWithReminders: List<BaseNote>)

    fun pinNoteToNotificationBar(note: BaseNote)

    fun unpinNoteFromNotificationBar(noteId: Long)

    fun canScheduleExactAlarms(): Boolean
}

class NoOpReminderScheduler : ReminderScheduler {
    override fun scheduleReminder(
        noteId: Long,
        reminder: Reminder,
        forceRepetition: Boolean,
    ) {}

    override fun cancelReminder(noteId: Long, reminderId: Long) {}

    override fun cancelAllRemindersForNote(noteId: Long, reminders: List<Reminder>) {}

    override fun cancelAllRemindersForNotes(notes: Collection<BaseNote>) {}

    override fun rescheduleAll(notesWithReminders: List<BaseNote>) {}

    override fun pinNoteToNotificationBar(note: BaseNote) {}

    override fun unpinNoteFromNotificationBar(noteId: Long) {}

    override fun canScheduleExactAlarms(): Boolean = true
}
