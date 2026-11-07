package com.philkes.notallyx.core.preference

import com.philkes.notallyx.data.model.NoteViewMode
import kotlinx.serialization.Serializable

@Serializable
enum class NotesView {
    LIST,
    GRID,
}

@Serializable
enum class Theme {
    LIGHT,
    DARK,
    SUPER_DARK,
    FOLLOW_SYSTEM,
}

@Serializable
enum class ListItemSort {
    NO_AUTO_SORT,
    AUTO_SORT_BY_CHECKED,
    AUTO_SORT_BY_CHECKED_TIMESTAMP,
}

@Serializable
enum class DefaultListNoteViewMode {
    READ_ONLY,
    EDIT,
    LAST_USED;

    fun toNoteViewMode(lastUsed: NoteViewMode): NoteViewMode {
        return when (this) {
            READ_ONLY -> NoteViewMode.READ_ONLY
            EDIT -> NoteViewMode.EDIT
            LAST_USED -> lastUsed
        }
    }
}

@Serializable
enum class BiometricLock {
    ENABLED,
    DISABLED,
}
