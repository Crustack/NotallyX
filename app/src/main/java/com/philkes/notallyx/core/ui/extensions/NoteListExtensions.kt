package com.philkes.notallyx.core.ui.extensions

import android.content.Context
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.sorting.compareColor
import com.philkes.notallyx.feature.main.sorting.compareCreated
import com.philkes.notallyx.feature.main.sorting.compareModified
import com.philkes.notallyx.feature.main.sorting.compareTitle

fun Context.createItemsFromNotes(list: List<BaseNote>, notesSort: NotesSort? = null): List<Item> {
    val sortedList = if (notesSort != null) list.sortWith(notesSort) else list
    val pinned by lazy { Header(getString(R.string.pinned)) }
    val others by lazy { Header(getString(R.string.others)) }
    val archived by lazy { Header(getString(R.string.archived)) }
    if (sortedList.isEmpty()) {
        return sortedList
    } else {
        val firstPinnedNote = sortedList.indexOfFirst { baseNote -> baseNote.pinned }
        val firstUnpinnedNote = sortedList.indexOfFirst { baseNote ->
            !baseNote.pinned && baseNote.folder != Folder.ARCHIVED
        }
        val mutableList: MutableList<Item> = sortedList.toMutableList()
        if (firstPinnedNote != -1) {
            mutableList.add(firstPinnedNote, pinned)
            if (firstUnpinnedNote != -1) {
                mutableList.add(firstUnpinnedNote + 1, others)
            }
        }
        val firstArchivedNote = mutableList.indexOfFirst { item ->
            item is BaseNote && item.folder == Folder.ARCHIVED
        }
        if (firstArchivedNote != -1) {
            mutableList.add(firstArchivedNote, archived)
        }
        return mutableList
    }
}

fun List<BaseNote>.sortWith(notesSort: NotesSort): List<BaseNote> {
    val comparator =
        Comparator<BaseNote> { note1, note2 ->
            val pinnedCompare = note2.pinned.compareTo(note1.pinned)
            if (pinnedCompare != 0) return@Comparator pinnedCompare

            val primaryCompare =
                when (notesSort.sortedBy) {
                    NotesSortBy.TITLE -> note1.compareTitle(note2)
                    NotesSortBy.CREATION_DATE -> note1.compareCreated(note2)
                    NotesSortBy.MODIFIED_DATE -> note1.compareModified(note2)
                    NotesSortBy.COLOR -> note1.compareColor(note2)
                }
            val dir = if (notesSort.sortDirection == SortDirection.ASC) 1 else -1
            if (primaryCompare != 0) primaryCompare * dir
            else note2.timestamp.compareTo(note1.timestamp)
        }
    return sortedWith(comparator)
}
