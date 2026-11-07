package com.philkes.notallyx.feature.reminder

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder

fun Context.canScheduleAlarms(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true

fun ReminderScheduler.pinAndScheduleReminders(notes: List<BaseNote>) {
    notes.forEach { note ->
        note.reminders.forEach { reminder -> scheduleReminder(note.id, reminder) }
        if (note.isPinnedToStatus) {
            pinNoteToNotificationBar(note)
        }
    }
}

fun ReminderScheduler.cancelPinAndReminders(notes: Collection<BaseNote>) {
    cancelAllRemindersForNotes(notes)
    notes.forEach { unpinNoteFromNotificationBar(it.id) }
}

fun ReminderScheduler.cancelPinAndReminders(noteId: Long, reminders: List<Reminder>) {
    cancelAllRemindersForNote(noteId, reminders)
    unpinNoteFromNotificationBar(noteId)
}
