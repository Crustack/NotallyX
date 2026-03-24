package com.philkes.notallyx.presentation.view.main.sorting

import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection

class BaseNoteMostRecentNotificationSort(
    adapter: RecyclerView.Adapter<*>?,
    sortDirection: SortDirection,
) : ItemSort(adapter, sortDirection) {

    override fun compare(note1: BaseNote, note2: BaseNote, sortDirection: SortDirection): Int {
        val sort =
            note1.compareNextNotification(note2).takeIf { it != 0 }
                ?: note1.compareLastNotification(note2)
        return if (sortDirection == SortDirection.ASC) sort else -1 * sort
    }
}
