package com.philkes.notallyx.presentation.view.note.listitem.sorting

import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.ListItem

/**
 * Sort algorithm that only sorts by [ListItem.checkedTimestamp]. A children is always below it's
 * parent and above parents with a lower order.
 */
class ListItemCheckedTimestampSortCallback(adapter: RecyclerView.Adapter<*>?) :
    ListItemParentSortCallback(adapter) {

    override fun compareItems(item1: ListItem, item2: ListItem): Int {
        return (item2.checkedTimestamp ?: 0L).compareTo(item1.checkedTimestamp ?: 0L)
    }

    override fun areContentsTheSame(oldItem: ListItem?, newItem: ListItem?): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(item1: ListItem?, item2: ListItem?): Boolean {
        return item1?.id == item2?.id
    }
}
