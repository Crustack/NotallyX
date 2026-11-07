@file:JvmName("AndroidNotesGridScreenKt")

package com.philkes.notallyx.feature.main.routes

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philkes.notallyx.R
import com.philkes.notallyx.Res
import com.philkes.notallyx.archive
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.delete
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.label
import com.philkes.notallyx.label_off
import com.philkes.notallyx.notebook
import com.philkes.notallyx.notifications
import com.philkes.notallyx.search
import java.io.File
import org.jetbrains.compose.resources.DrawableResource

/** Backwards-compatible overload for Android-specific @DrawableRes backgroundResId usages. */
@Composable
fun NotesGridScreen(
    items: List<Item>,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    imageRoot: File? = null,
    searchKeyword: String = "",
    onSearchKeywordChange: ((String) -> Unit)? = null,
    showSearchBar: Boolean = false,
    selectedFolder: Folder? = null,
    onFolderSelected: ((Folder) -> Unit)? = null,
    selectedFilterOption: FilterOptions? = null,
    onFilterOptionSelected: ((FilterOptions) -> Unit)? = null,
    selectedNoteIds: Set<Long> = emptySet(),
    @DrawableRes backgroundResId: Int,
    isLoading: Boolean = false,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    onNoteClick: (BaseNote, Int) -> Unit = { _, _ -> },
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit = { _, _, _ -> },
    onReminderClick: (BaseNote, Int) -> Unit = { _, _ -> },
) {
    val drawable: DrawableResource =
        when (backgroundResId) {
            R.drawable.notebook -> Res.drawable.notebook
            R.drawable.archive -> Res.drawable.archive
            R.drawable.delete -> Res.drawable.delete
            R.drawable.search -> Res.drawable.search
            R.drawable.label -> Res.drawable.label
            R.drawable.label_off -> Res.drawable.label_off
            R.drawable.notifications -> Res.drawable.notifications
            else -> Res.drawable.notebook
        }
    NotesGridScreen(
        items = items,
        modifier = modifier,
        isGrid = isGrid,
        imageRoot = imageRoot,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = onSearchKeywordChange,
        showSearchBar = showSearchBar,
        selectedFolder = selectedFolder,
        onFolderSelected = onFolderSelected,
        selectedFilterOption = selectedFilterOption,
        onFilterOptionSelected = onFilterOptionSelected,
        selectedNoteIds = selectedNoteIds,
        backgroundDrawable = drawable,
        isLoading = isLoading,
        displayConfig = displayConfig,
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = onReminderClick,
    )
}
