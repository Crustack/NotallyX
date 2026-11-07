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

val NotesSortBy.textResId: Int
    get() =
        when (this) {
            NotesSortBy.TITLE -> R.string.title
            NotesSortBy.CREATION_DATE -> R.string.creation_date
            NotesSortBy.MODIFIED_DATE -> R.string.modified_date
            NotesSortBy.COLOR -> R.string.color
        }

val NotesSortBy.iconResId: Int
    get() =
        when (this) {
            NotesSortBy.TITLE -> R.drawable.sort_by_alpha
            NotesSortBy.CREATION_DATE -> R.drawable.calendar_add_on
            NotesSortBy.MODIFIED_DATE -> R.drawable.edit_calendar
            NotesSortBy.COLOR -> R.drawable.change_color
        }

val NotesSortBy.value: String
    get() =
        when (this) {
            NotesSortBy.TITLE -> "autoSortByTitle"
            NotesSortBy.CREATION_DATE -> "autoSortByCreationDate"
            NotesSortBy.MODIFIED_DATE -> "autoSortByModifiedDate"
            NotesSortBy.COLOR -> "autoSortByColor"
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
