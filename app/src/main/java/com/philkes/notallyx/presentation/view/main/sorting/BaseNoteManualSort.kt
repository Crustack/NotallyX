package com.philkes.notallyx.presentation.view.main.sorting

import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection

open class BaseNoteManualSort(adapter: RecyclerView.Adapter<*>?, sortDirection: SortDirection) :
    ItemSort(adapter, sortDirection) {

    override fun compare(note1: BaseNote, note2: BaseNote, sortDirection: SortDirection): Int {
        val sortIdx1 = note1.sortIdx ?: Int.MAX_VALUE
        val sortIdx2 = note2.sortIdx ?: Int.MAX_VALUE
        return if (sortDirection == SortDirection.ASC) {
            sortIdx1.compareTo(sortIdx2)
        } else {
            sortIdx2.compareTo(sortIdx1)
        }
    }
}
