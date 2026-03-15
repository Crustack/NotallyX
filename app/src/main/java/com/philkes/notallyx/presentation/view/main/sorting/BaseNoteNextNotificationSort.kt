package com.philkes.notallyx.presentation.view.main.sorting

import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.findNextNotificationDate
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection

class BaseNoteNextNotificationSort(
    adapter: RecyclerView.Adapter<*>?,
    sortDirection: SortDirection,
) : ItemSort(adapter, sortDirection) {

    override fun compare(note1: BaseNote, note2: BaseNote, sortDirection: SortDirection): Int {
        val sort = note1.compareNextNotification(note2)
        return if (sortDirection == SortDirection.ASC) sort else -1 * sort
    }
}

fun BaseNote.compareNextNotification(other: BaseNote): Int {
    if (other.reminders.isEmpty() && reminders.isNotEmpty()) {
        return 1
    }
    if (other.reminders.isNotEmpty() && reminders.isEmpty()) {
        return -1
    }
    if (other.reminders.isEmpty() && reminders.isEmpty()) {
        return 0
    }
    val nextNotification = reminders.findNextNotificationDate()
    val otherNextNotification = other.reminders.findNextNotificationDate()
    if (nextNotification == null && otherNextNotification != null) {
        return -1
    }
    if (nextNotification != null && otherNextNotification == null) {
        return 1
    }
    if (nextNotification == null && otherNextNotification == null) {
        return 0
    }
    return nextNotification!!.compareTo(otherNextNotification!!)
}
