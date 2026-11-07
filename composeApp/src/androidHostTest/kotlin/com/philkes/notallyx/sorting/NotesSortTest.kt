package com.philkes.notallyx.sorting

import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.ui.extensions.sortWith
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.createBaseNote
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesSortTest {

    private val note1 =
        createBaseNote(
            id = 1L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            title = "Alpha Note",
            body = "Body 1",
            timestamp = 2000L,
            modifiedTimestamp = 3000L,
            color = "#222222",
        )
    private val note2 =
        createBaseNote(
            id = 2L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            title = "Beta Note",
            body = "Body 2",
            timestamp = 1000L,
            modifiedTimestamp = 1000L,
            color = "#333333",
        )
    private val note3 =
        createBaseNote(
            id = 3L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            title = "Gamma Note",
            body = "Body 3",
            timestamp = 3000L,
            modifiedTimestamp = 2000L,
            color = "#111111",
        )
    private val testNotes = listOf(note1, note2, note3)

    @Test
    fun testAllNotesSortCombinations() {
        val testCases =
            listOf(
                NotesSort(NotesSortBy.TITLE, SortDirection.ASC) to
                    listOf("Alpha Note", "Beta Note", "Gamma Note"),
                NotesSort(NotesSortBy.TITLE, SortDirection.DESC) to
                    listOf("Gamma Note", "Beta Note", "Alpha Note"),
                NotesSort(NotesSortBy.CREATION_DATE, SortDirection.ASC) to
                    listOf("Beta Note", "Alpha Note", "Gamma Note"),
                NotesSort(NotesSortBy.CREATION_DATE, SortDirection.DESC) to
                    listOf("Gamma Note", "Alpha Note", "Beta Note"),
                NotesSort(NotesSortBy.MODIFIED_DATE, SortDirection.ASC) to
                    listOf("Beta Note", "Gamma Note", "Alpha Note"),
                NotesSort(NotesSortBy.MODIFIED_DATE, SortDirection.DESC) to
                    listOf("Alpha Note", "Gamma Note", "Beta Note"),
                NotesSort(NotesSortBy.COLOR, SortDirection.ASC) to
                    listOf("Gamma Note", "Alpha Note", "Beta Note"),
                NotesSort(NotesSortBy.COLOR, SortDirection.DESC) to
                    listOf("Beta Note", "Alpha Note", "Gamma Note"),
            )

        assertEquals(NotesSortBy.entries.toSet(), testCases.map { it.first.sortedBy }.toSet())

        testCases.forEach { (notesSort, expectedTitles) ->
            val sortedTitles = testNotes.sortWith(notesSort).map { it.title }
            assertEquals(
                "Failed sorting for ${notesSort.sortedBy} ${notesSort.sortDirection}",
                expectedTitles,
                sortedTitles,
            )
        }
    }
}
