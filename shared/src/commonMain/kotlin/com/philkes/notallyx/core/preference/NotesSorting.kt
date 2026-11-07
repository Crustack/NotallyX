package com.philkes.notallyx.core.preference

import kotlinx.serialization.Serializable

@Serializable
enum class SortDirection {
    ASC,
    DESC,
}

@Serializable
enum class NotesSortBy {
    TITLE,
    CREATION_DATE,
    MODIFIED_DATE,
    COLOR;

    companion object {
        fun fromValue(value: String): NotesSortBy? {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                    when (it) {
                        TITLE -> value == "autoSortByTitle"
                        CREATION_DATE -> value == "autoSortByCreationDate"
                        MODIFIED_DATE -> value == "autoSortByModifiedDate"
                        COLOR -> value == "autoSortByColor"
                    }
            }
        }
    }
}

@Serializable
data class NotesSort(
    val sortedBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val sortDirection: SortDirection = SortDirection.DESC,
)
