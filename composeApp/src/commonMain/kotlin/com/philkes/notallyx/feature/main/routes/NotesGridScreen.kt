package com.philkes.notallyx.feature.main.routes

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.all
import com.philkes.notallyx.archived
import com.philkes.notallyx.cancel
import com.philkes.notallyx.close
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.deleted
import com.philkes.notallyx.elapsed
import com.philkes.notallyx.empty_list
import com.philkes.notallyx.feature.main.routes.notes.NotesAction
import com.philkes.notallyx.feature.main.routes.notes.NotesUiState
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.feature.reminder.ReminderRow
import com.philkes.notallyx.note
import com.philkes.notallyx.notebook
import com.philkes.notallyx.notes
import com.philkes.notallyx.search
import com.philkes.notallyx.upcoming
import java.io.File
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Stateless composable for displaying notes in grid or list format. */
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
    backgroundDrawable: DrawableResource = Res.drawable.notebook,
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
                    label = { Text(text = stringResource(Res.string.notes)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFolder == Folder.DELETED,
                    onClick = { onFolderSelected(Folder.DELETED) },
                    label = { Text(text = stringResource(Res.string.deleted)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFolder == Folder.ARCHIVED,
                    onClick = { onFolderSelected(Folder.ARCHIVED) },
                    label = { Text(text = stringResource(Res.string.archived)) },
                )
            }
        }

        if (selectedFilterOption != null && onFilterOptionSelected != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.UPCOMING,
                    onClick = { onFilterOptionSelected(FilterOptions.UPCOMING) },
                    label = { Text(text = stringResource(Res.string.upcoming)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.ELAPSED,
                    onClick = { onFilterOptionSelected(FilterOptions.ELAPSED) },
                    label = { Text(text = stringResource(Res.string.elapsed)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = selectedFilterOption == FilterOptions.ALL,
                    onClick = { onFilterOptionSelected(FilterOptions.ALL) },
                    label = { Text(text = stringResource(Res.string.all)) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).testTag(TestTags.EMPTY_NOTES),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(backgroundDrawable),
                        contentDescription = stringResource(Res.string.empty_list),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(96.dp),
                    )
                    Text(
                        text = if (searchKeyword.isBlank()) "No notes yet" else "No matching notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (isGrid) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier.fillMaxSize().testTag(TestTags.NOTES_GRID),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item ->
                            when (item) {
                                is Header -> "header_${item.label}_$index"
                                is BaseNote -> item.id
                            }
                        },
                        span = { _, item ->
                            if (item is Header) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        },
                        contentType = { _, item ->
                            when (item) {
                                is Header -> "header"
                                is BaseNote -> "note"
                            }
                        },
                    ) { index, item ->
                        when (item) {
                            is Header -> {
                                SectionHeaderItem(title = item.label)
                            }
                            is BaseNote -> {
                                val isSelected = selectedNoteIds.contains(item.id)
                                BaseNoteCard(
                                    baseNote = item,
                                    isGrid = true,
                                    config = displayConfig,
                                    imageRoot = imageRoot,
                                    isSelected = isSelected,
                                    onClick = { onNoteClick(item, index) },
                                    onLongClick = { onNoteLongClick(item, index, items) },
                                    onReminderClick = { onReminderClick(item, index) },
                                    modifier = Modifier.testTag(TestTags.NOTE_CARD),
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag(TestTags.NOTES_GRID),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item ->
                            when (item) {
                                is Header -> "header_${item.label}_$index"
                                is BaseNote -> item.id
                            }
                        },
                        contentType = { _, item ->
                            when (item) {
                                is Header -> "header"
                                is BaseNote -> "note"
                            }
                        },
                    ) { index, item ->
                        when (item) {
                            is Header -> {
                                SectionHeaderItem(title = item.label)
                            }
                            is BaseNote -> {
                                val isSelected = selectedNoteIds.contains(item.id)
                                if (selectedFilterOption != null) {
                                    ReminderRow(
                                        title =
                                            item.title.ifEmpty { stringResource(Res.string.note) },
                                        nextNotificationText = "",
                                        remindersCountText = item.reminders.size.toString(),
                                        onClick = { onReminderClick(item, index) },
                                        onOpenNote = { onNoteClick(item, index) },
                                    )
                                } else {
                                    BaseNoteCard(
                                        baseNote = item,
                                        isGrid = false,
                                        config = displayConfig,
                                        imageRoot = imageRoot,
                                        isSelected = isSelected,
                                        onClick = { onNoteClick(item, index) },
                                        onLongClick = { onNoteLongClick(item, index, items) },
                                        onReminderClick = { onReminderClick(item, index) },
                                        modifier = Modifier.testTag(TestTags.NOTE_CARD),
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

/** MVI overload for NotesGridScreen. */
@Composable
fun NotesGridScreen(
    state: NotesUiState,
    onAction: (NotesAction) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onOpenDrawer: () -> Unit = {},
) {
    NotesGridScreen(
        items = state.items,
        isGrid = state.isGrid,
        selectedNoteIds = state.selectedIds,
        displayConfig = state.displayConfig,
        backgroundDrawable = Res.drawable.notebook,
        isLoading = state.isLoading,
        onNoteClick = { note, pos -> onAction(NotesAction.NoteClicked(note, pos)) },
        onNoteLongClick = { note, pos, items ->
            onAction(NotesAction.NoteLongClicked(note, pos, items))
        },
        onReminderClick = { note, _ -> onAction(NotesAction.ReminderClicked(note)) },
        modifier = modifier,
    )
}

@Composable
private fun SearchBar(
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = searchKeyword,
        onValueChange = onSearchKeywordChange,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(TestTags.SEARCH_INPUT),
        placeholder = { Text(text = stringResource(Res.string.search)) },
        leadingIcon = {
            Image(
                painter = painterResource(Res.drawable.search),
                contentDescription = stringResource(Res.string.search),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (searchKeyword.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchKeywordChange("") },
                    modifier = Modifier.testTag(TestTags.SEARCH_CLEAR_BUTTON),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.cancel),
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
