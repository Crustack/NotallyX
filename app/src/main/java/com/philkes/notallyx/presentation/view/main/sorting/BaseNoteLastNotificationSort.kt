package com.philkes.notallyx.presentation.view.main.sorting

import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.findLastNotificationDate
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection

class BaseNoteLastNotificationSort(
    adapter: RecyclerView.Adapter<*>?,
    sortDirection: SortDirection,
) : ItemSort(adapter, sortDirection) {

    override fun compare(note1: BaseNote, note2: BaseNote, sortDirection: SortDirection): Int {
        val sort = note1.compareLastNotification(note2)
        return if (sortDirection == SortDirection.ASC) sort else -1 * sort
    }
}

fun BaseNote.compareLastNotification(other: BaseNote): Int {
    if (other.reminders.isEmpty() && reminders.isNotEmpty()) {
        return 1
    }
    if (other.reminders.isNotEmpty() && reminders.isEmpty()) {
        return -1
    }
    if (other.reminders.isEmpty() && reminders.isEmpty()) {
        return 0
    }
    val lastNotification = reminders.findLastNotificationDate()
    val otherLastNotification = other.reminders.findLastNotificationDate()
    if (lastNotification == null && otherLastNotification != null) {
        return -1
    }
    if (lastNotification != null && otherLastNotification == null) {
        return 1
    }
    if (lastNotification == null && otherLastNotification == null) {
        return 0
    }
    return lastNotification!!.compareTo(otherLastNotification!!)
}
