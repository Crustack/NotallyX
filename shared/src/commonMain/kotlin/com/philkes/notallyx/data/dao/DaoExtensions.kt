package com.philkes.notallyx.data.dao

import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder

suspend fun BaseNoteDao.moveBaseNotes(
    ids: LongArray,
    folder: Folder,
    onNotesMoved: (notes: List<BaseNote>, folder: Folder) -> Unit = { _, _ -> },
) {
    // Only reminders of notes in NOTES folder are active
    if (folder == Folder.DELETED) {
        move(ids, folder, System.currentTimeMillis())
    } else {
        move(ids, folder)
    }
    val notes = getByIds(ids)
    onNotesMoved(notes, folder)
}
