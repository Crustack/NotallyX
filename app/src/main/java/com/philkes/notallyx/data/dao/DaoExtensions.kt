package com.philkes.notallyx.data.dao

import android.content.Context
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.utils.cancelPinAndReminders
import com.philkes.notallyx.utils.pinAndScheduleReminders

suspend fun Context.moveBaseNotes(baseNoteDao: BaseNoteDao, ids: LongArray, folder: Folder) {
    // Only reminders of notes in NOTES folder are active
    if (folder == Folder.DELETED) {
        baseNoteDao.move(ids, folder, System.currentTimeMillis())
    } else {
        baseNoteDao.move(ids, folder)
    }
    val notes = baseNoteDao.getByIds(ids)
    // Only reminders of notes in NOTES folder are active
    when (folder) {
        Folder.NOTES -> pinAndScheduleReminders(notes)
        else -> cancelPinAndReminders(notes)
    }
}
