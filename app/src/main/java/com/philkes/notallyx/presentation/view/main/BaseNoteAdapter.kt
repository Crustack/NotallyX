package com.philkes.notallyx.presentation.view.main

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.databinding.RecyclerBaseNoteBinding
import com.philkes.notallyx.databinding.RecyclerHeaderBinding
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteColorSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteCreationDateSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteManualSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteModifiedDateSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteTitleSort
import com.philkes.notallyx.presentation.view.misc.ItemListener
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSort
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import java.io.File

class BaseNoteAdapter(
    private val selectedIds: Set<Long>,
    private val dateFormat: DateFormat,
    private var notesSort: NotesSort,
    private val preferences: BaseNoteVHPreferences,
    private val imageRoot: File?,
    private val listener: ItemListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var searchKeyword: String = ""

    private interface ItemListContainer {
        fun size(): Int

        operator fun get(position: Int): Item

        fun replaceAll(items: List<Item>)

        fun clear()

        fun addAll(items: List<Item>)

        fun toList(): List<Item>
    }

    private class SortedListContainer(private val list: SortedList<Item>) : ItemListContainer {
        override fun size(): Int = list.size()

        override fun get(position: Int): Item = list[position]

        override fun replaceAll(items: List<Item>) = list.replaceAll(items)

        override fun clear() = list.clear()

        override fun addAll(items: List<Item>) = list.addAll(items)

        override fun toList(): List<Item> {
            val result = mutableListOf<Item>()
            for (i in 0 until list.size()) {
                result.add(list[i])
            }
            return result
        }
    }

    private inner class MutableListContainer(
        private var items: MutableList<Item>,
        private val sort: NotesSort,
    ) : ItemListContainer {
        override fun size(): Int = items.size

        override fun get(position: Int): Item = items[position]

        override fun replaceAll(newItems: List<Item>) {
            val oldItems = items
            items = newItems.toMutableList()
            sortIfNeeded()
            val diffResult =
                DiffUtil.calculateDiff(
                    object : DiffUtil.Callback() {
                        override fun getOldListSize(): Int = oldItems.size

                        override fun getNewListSize(): Int = items.size

                        override fun areItemsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            val oldItem = oldItems[oldItemPosition]
                            val newItem = items[newItemPosition]
                            return when {
                                oldItem is BaseNote && newItem is BaseNote ->
                                    oldItem.id == newItem.id
                                oldItem is Header && newItem is Header ->
                                    oldItem.label == newItem.label
                                else -> false
                            }
                        }

                        override fun areContentsTheSame(
                            oldItemPosition: Int,
                            newItemPosition: Int,
                        ): Boolean {
                            return oldItems[oldItemPosition] == items[newItemPosition]
                        }
                    }
                )
            diffResult.dispatchUpdatesTo(this@BaseNoteAdapter)
        }

        override fun clear() {
            val oldSize = items.size
            items.clear()
            notifyItemRangeRemoved(0, oldSize)
        }

        override fun addAll(newItems: List<Item>) {
            val oldSize = items.size
            items.addAll(newItems)
            sortIfNeeded()
            notifyItemRangeInserted(oldSize, items.size - oldSize)
        }

        override fun toList(): List<Item> = items.toList()

        fun sortIfNeeded() {
            val callback = sort.createCallback()
            items.sortWith { a, b -> callback.compare(a, b) }
        }

        fun move(fromPosition: Int, toPosition: Int) {
            val fromItem = items.removeAt(fromPosition)
            items.add(toPosition, fromItem)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    private var list: ItemListContainer = createListContainer(notesSort)

    private fun createListContainer(sort: NotesSort): ItemListContainer {
        return if (sort.sortedBy == NotesSortBy.MANUAL) {
            MutableListContainer(mutableListOf(), sort)
        } else {
            SortedListContainer(SortedList(Item::class.java, sort.createCallback()))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is Header -> 0
            is BaseNote -> 1
        }
    }

    override fun getItemCount(): Int {
        return list.size()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {
            is Header -> (holder as HeaderVH).bind(item)
            is BaseNote -> {
                (holder as BaseNoteVH).apply {
                    setSearchKeyword(searchKeyword)
                    bind(item, imageRoot, selectedIds.contains(item.id), notesSort.sortedBy)
                }
            }
        }
    }

    fun setSearchKeyword(keyword: String) {
        Log.d("SearchResult", "keyword: $keyword")
        if (searchKeyword != keyword) {
            val oldKeyword = searchKeyword
            searchKeyword = keyword
            for (i in 0 until list.size()) {
                val item = list[i]
                if (item is BaseNote) {
                    if (matchesKeyword(item, oldKeyword) || matchesKeyword(item, keyword)) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    private fun matchesKeyword(baseNote: BaseNote, keyword: String): Boolean {
        if (keyword.isBlank()) {
            return false
        }
        if (baseNote.title.contains(keyword, true)) {
            return true
        }
        if (baseNote.body.contains(keyword, true)) {
            return true
        }
        for (label in baseNote.labels) {
            if (label.contains(keyword, true)) {
                return true
            }
        }
        for (item in baseNote.items) {
            if (item.body.contains(keyword, true)) {
                return true
            }
        }
        return false
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else handleCheck(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RecyclerHeaderBinding.inflate(inflater, parent, false)
                HeaderVH(binding)
            }
            else -> {
                val binding = RecyclerBaseNoteBinding.inflate(inflater, parent, false)
                BaseNoteVH(binding, dateFormat, preferences, listener)
            }
        }
    }

    fun setNotesSort(notesSort: NotesSort) {
        this.notesSort = notesSort
        val mutableList = list.toList()
        list.clear()
        list = createListContainer(notesSort)
        list.addAll(mutableList)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Item? {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= list.size()) {
            return null
        }
        return list[position]
    }

    val currentList: List<Item>
        get() = list.toList()

    fun submitList(items: List<Item>) {
        list.replaceAll(items)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (list is MutableListContainer) {
            (list as MutableListContainer).move(fromPosition, toPosition)
            updateSortIndices()
        }
    }

    /**
     * @return a pair of (top-most moved position, new position of the last selected note). If there
     *   is no moved position, return null.
     */
    fun moveSelectedNotesUp(lastSelectedId: Long?): Pair<Int, Int>? {
        if (list is MutableListContainer) {
            val items = list.toList()
            val selectedIndices =
                items.mapIndexedNotNull { index, item ->
                    if (item is BaseNote && selectedIds.contains(item.id)) index else null
                }
            if (selectedIndices.isEmpty()) return null

            var moved = false
            selectedIndices.forEachIndexed { i, index ->
                if (index > i) {
                    (list as MutableListContainer).move(index, index - 1)
                    moved = true
                }
            }
            if (!moved) return null

            val newItems = list.toList()
            val topMostMovedPosition =
                newItems
                    .indexOfFirst { it is BaseNote && selectedIds.contains(it.id) }
                    .takeIf { it != -1 }

            val newLastSelectedPos =
                if (lastSelectedId != null) {
                    newItems.indexOfFirst { it is BaseNote && it.id == lastSelectedId }
                } else -1

            updateSortIndices()
            return topMostMovedPosition?.let { it to newLastSelectedPos }
        }
        return null
    }

    /**
     * @return a pair of (top-most moved position, new position of the last selected note). If there
     *   is no moved position, return null.
     */
    fun moveSelectedNotesDown(lastSelectedId: Long?): Pair<Int, Int>? {
        if (list is MutableListContainer) {
            val items = list.toList()
            val selectedIndices =
                items.mapIndexedNotNull { index, item ->
                    if (item is BaseNote && selectedIds.contains(item.id)) index else null
                }
            if (selectedIndices.isEmpty()) return null

            var moved = false
            val n = items.size
            selectedIndices.reversed().forEachIndexed { i, index ->
                if (index < n - 1 - i) {
                    (list as MutableListContainer).move(index, index + 1)
                    moved = true
                }
            }
            if (!moved) return null

            val newItems = list.toList()
            val topMostMovedPosition =
                newItems
                    .indexOfFirst { it is BaseNote && selectedIds.contains(it.id) }
                    .takeIf { it != -1 }

            val newLastSelectedPos =
                if (lastSelectedId != null) {
                    newItems.indexOfFirst { it is BaseNote && it.id == lastSelectedId }
                } else -1

            updateSortIndices()
            return topMostMovedPosition?.let { it to newLastSelectedPos }
        }
        return null
    }

    private fun updateSortIndices() {
        val currentItems = list.toList()
        val notes = currentItems.filterIsInstance<BaseNote>()
        notes.forEachIndexed { index, note -> note.sortIdx = notes.size - 1 - index }
    }

    private fun NotesSort.createCallback() =
        when (sortedBy) {
            NotesSortBy.TITLE -> BaseNoteTitleSort(this@BaseNoteAdapter, sortDirection)
            NotesSortBy.MODIFIED_DATE ->
                BaseNoteModifiedDateSort(this@BaseNoteAdapter, sortDirection)
            NotesSortBy.CREATION_DATE ->
                BaseNoteCreationDateSort(this@BaseNoteAdapter, sortDirection)
            NotesSortBy.COLOR -> BaseNoteColorSort(this@BaseNoteAdapter, sortDirection)
            NotesSortBy.MANUAL -> BaseNoteManualSort(this@BaseNoteAdapter, sortDirection)
        }

    private fun handleCheck(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if (item is BaseNote) {
            (holder as BaseNoteVH).updateCheck(selectedIds.contains(item.id), item.color)
        }
    }
}
