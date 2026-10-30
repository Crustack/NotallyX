package com.philkes.notallyx.core.preference

import android.content.Context
import com.philkes.notallyx.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

class NotesSortPreference(
    flow: StateFlow<NotesSort>,
    onUpdate: suspend (NotesSort) -> Unit,
    scope: CoroutineScope,
) :
    PreferenceHolder<NotesSort>(
        key = SORTED_BY_KEY,
        defaultValue = NotesSort(),
        titleResId = R.string.notes_sorted_by,
        flow = flow,
        onUpdate = onUpdate,
        scope = scope,
    ) {
    companion object {
        const val SORTED_BY_KEY = "notesSorting"
        const val SORT_DIRECTION_KEY = "notesSortingDirection"
    }
}

@Serializable
enum class SortDirection(val textResId: Int, val iconResId: Int) {
    ASC(R.string.ascending, R.drawable.arrow_upward),
    DESC(R.string.descending, R.drawable.arrow_downward),
}

@Serializable
enum class NotesSortBy(val textResId: Int, val iconResId: Int, val value: String) {
    TITLE(R.string.title, R.drawable.sort_by_alpha, "autoSortByTitle"),
    CREATION_DATE(R.string.creation_date, R.drawable.calendar_add_on, "autoSortByCreationDate"),
    MODIFIED_DATE(R.string.modified_date, R.drawable.edit_calendar, "autoSortByModifiedDate"),
    COLOR(R.string.color, R.drawable.change_color, "autoSortByColor");

    companion object {
        fun fromValue(value: String): NotesSortBy? {
            return entries.find { it.value == value }
        }
    }
}

@Serializable
data class NotesSort(
    val sortedBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val sortDirection: SortDirection = SortDirection.DESC,
) : TextProvider {
    override fun getText(context: Context): String {
        return "${context.getString(sortedBy.textResId)} (${context.getString(sortDirection.textResId)})"
    }
}
