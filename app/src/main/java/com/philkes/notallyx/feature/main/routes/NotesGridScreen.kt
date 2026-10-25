package com.philkes.notallyx.feature.main.routes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.feature.reminder.ReminderRow
import java.io.File

/** Composable replacing fragment_notes.xml layout. */
@Composable
internal fun NotesGridScreen(
    items: List<Item>,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    imageRoot: File? = LocalContext.current.getCurrentImagesDirectory(),
    searchKeyword: String = "",
    onSearchKeywordChange: ((String) -> Unit)? = null,
    showSearchBar: Boolean = false,
    selectedFolder: Folder? = null,
    onFolderSelected: ((Folder) -> Unit)? = null,
    selectedFilterOption: FilterOptions? = null,
    onFilterOptionSelected: ((FilterOptions) -> Unit)? = null,
    selectedNoteIds: Set<Long> = emptySet(),
    @DrawableRes backgroundResId: Int = R.drawable.notebook,
    isLoading: Boolean = false,
    displayConfig: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    onNoteClick: (BaseNote, Int) -> Unit = { _, _ -> },
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit = { _, _, _ -> },
    onReminderClick: (BaseNote, Int) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (showSearchBar && onSearchKeywordChange != null) {
            SearchBar(searchKeyword, onSearchKeywordChange)
        }

        if (selectedFolder != null && onFolderSelected != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = selectedFolder == Folder.NOTES,
                    onClick = { onFolderSelected(Folder.NOTES) },
                    label = { Text(text = stringResource(R.string.notes)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFolder == Folder.DELETED,
                    onClick = { onFolderSelected(Folder.DELETED) },
                    label = { Text(text = stringResource(R.string.deleted)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFolder == Folder.ARCHIVED,
                    onClick = { onFolderSelected(Folder.ARCHIVED) },
                    label = { Text(text = stringResource(R.string.archived)) },
                )
            }
        }

        if (selectedFilterOption != null && onFilterOptionSelected != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.UPCOMING,
                    onClick = { onFilterOptionSelected(FilterOptions.UPCOMING) },
                    label = { Text(text = stringResource(R.string.upcoming)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.ELAPSED,
                    onClick = { onFilterOptionSelected(FilterOptions.ELAPSED) },
                    label = { Text(text = stringResource(R.string.elapsed)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.ALL,
                    onClick = { onFilterOptionSelected(FilterOptions.ALL) },
                    label = { Text(text = stringResource(R.string.all)) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                Image(
                    painter = painterResource(id = backgroundResId),
                    contentDescription = stringResource(R.string.empty_list),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(96.dp).align(Alignment.Center),
                )
            } else if (isGrid) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items.forEachIndexed { index, item ->
                        when (item) {
                            is Header -> {
                                item(
                                    span = StaggeredGridItemSpan.FullLine,
                                    key = "header_${item.label}_$index",
                                ) {
                                    SectionHeaderItem(title = item.label)
                                }
                            }
                            is BaseNote -> {
                                item(key = item.id) {
                                    val isSelected = selectedNoteIds.contains(item.id)
                                    BaseNoteCard(
                                        baseNote = item,
                                        config = displayConfig,
                                        imageRoot = imageRoot,
                                        isSelected = isSelected,
                                        onClick = { onNoteClick(item, index) },
                                        onLongClick = { onNoteLongClick(item, index, items) },
                                        onReminderClick = { onReminderClick(item, index) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    items.forEachIndexed { index, item ->
                        when (item) {
                            is Header -> {
                                item(key = "header_${item.label}_$index") {
                                    SectionHeaderItem(title = item.label)
                                }
                            }
                            is BaseNote -> {
                                item(key = item.id) {
                                    val isSelected = selectedNoteIds.contains(item.id)
                                    if (selectedFilterOption != null) {
                                        ReminderRow(
                                            title =
                                                item.title.ifEmpty {
                                                    stringResource(R.string.note)
                                                },
                                            nextNotificationText = "",
                                            remindersCountText = item.reminders.size.toString(),
                                            onClick = { onReminderClick(item, index) },
                                            onOpenNote = { onNoteClick(item, index) },
                                        )
                                    } else {
                                        BaseNoteCard(
                                            baseNote = item,
                                            config = displayConfig,
                                            imageRoot = imageRoot,
                                            isSelected = isSelected,
                                            onClick = { onNoteClick(item, index) },
                                            onLongClick = { onNoteLongClick(item, index, items) },
                                            onReminderClick = { onReminderClick(item, index) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = searchKeyword,
        onValueChange = onSearchKeywordChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        placeholder = { Text(text = stringResource(R.string.search)) },
        leadingIcon = {
            Image(
                painter = painterResource(id = R.drawable.search),
                contentDescription = stringResource(R.string.search),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (searchKeyword.isNotEmpty()) {
                IconButton(onClick = { onSearchKeywordChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun NotesGridScreenPreview() {
    NotallyXTheme {
        NotesGridScreen(
            items = SampleNotes.ALL,
            showSearchBar = true,
            onSearchKeywordChange = {},
            selectedFolder = Folder.NOTES,
            onFolderSelected = {},
            isGrid = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesGridScreenDisplayConfigPreview() {
    NotallyXTheme {
        NotesGridScreen(
            items = SampleNotes.ALL,
            showSearchBar = false,
            onSearchKeywordChange = {},
            onFolderSelected = {},
            isGrid = true,
            displayConfig =
                NoteCardDisplayConfig(
                    dateFormat = DateFormat.NONE,
                    timeFormat = TimeFormat.NONE,
                    maxItems = 1,
                    maxLines = 1,
                    hideLabels = true,
                    hideImages = true,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesListScreenPreview() {
    NotallyXTheme {
        NotesGridScreen(
            items = SampleNotes.ALL,
            showSearchBar = true,
            onSearchKeywordChange = {},
            selectedFolder = Folder.NOTES,
            onFolderSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesListScreenDisplayConfigPreview() {
    NotallyXTheme {
        NotesGridScreen(
            items = SampleNotes.ALL,
            showSearchBar = false,
            onSearchKeywordChange = {},
            displayConfig =
                NoteCardDisplayConfig(
                    dateFormat = DateFormat.NONE,
                    timeFormat = TimeFormat.NONE,
                    maxItems = 1,
                    maxLines = 1,
                    hideLabels = true,
                    hideImages = true,
                ),
            onFolderSelected = {},
        )
    }
}
