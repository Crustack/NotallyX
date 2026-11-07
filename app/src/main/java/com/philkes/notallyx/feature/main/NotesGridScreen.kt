package com.philkes.notallyx.feature.main

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.feature.reminder.NoteReminderRow
import java.io.File

/** Composable replacing fragment_notes.xml layout. */
@Composable
internal fun NotesGridScreen(
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
    @DrawableRes backgroundResId: Int = R.drawable.notebook,
    isLoading: Boolean = false,
    dateFormat: DateFormat = DateFormat.RELATIVE,
    timeFormat: TimeFormat = TimeFormat.AM_PM,
    sortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    textSize: Float = 16f,
    maxTitleLines: Int = 2,
    maxLines: Int = 10,
    maxItems: Int = 5,
    hideLabels: Boolean = false,
    hideImages: Boolean = false,
    onNoteClick: (BaseNote, Int) -> Unit = { _, _ -> },
    onNoteLongClick: (BaseNote, Int) -> Unit = { _, _ -> },
    onReminderClick: (BaseNote, Int) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (showSearchBar && onSearchKeywordChange != null) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("EnterSearchKeyword"),
                placeholder = { Text(text = stringResource(R.string.search)) },
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                singleLine = true,
            )
        }

        if (selectedFolder != null && onFolderSelected != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                FilterChip(
                    selected = selectedFolder == Folder.NOTES,
                    onClick = { onFolderSelected(Folder.NOTES) },
                    label = { Text(text = stringResource(R.string.notes)) },
                    modifier = Modifier.padding(end = 8.dp).testTag("NotesFilterChip"),
                )
                FilterChip(
                    selected = selectedFolder == Folder.DELETED,
                    onClick = { onFolderSelected(Folder.DELETED) },
                    label = { Text(text = stringResource(R.string.deleted)) },
                    modifier = Modifier.padding(end = 8.dp).testTag("DeletedFilterChip"),
                )
                FilterChip(
                    selected = selectedFolder == Folder.ARCHIVED,
                    onClick = { onFolderSelected(Folder.ARCHIVED) },
                    label = { Text(text = stringResource(R.string.archived)) },
                    modifier = Modifier.testTag("ArchivedFilterChip"),
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
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
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
                                        modifier = Modifier.testTag("NoteCard_${item.id}"),
                                        imageRoot = imageRoot,
                                        isSelected = isSelected,
                                        dateFormat = dateFormat,
                                        timeFormat = timeFormat,
                                        sortBy = sortBy,
                                        textSize = textSize,
                                        maxTitleLines = maxTitleLines,
                                        maxLines = maxLines,
                                        maxItems = maxItems,
                                        hideLabels = hideLabels,
                                        hideImages = hideImages,
                                        onClick = { onNoteClick(item, index) },
                                        onLongClick = { onNoteLongClick(item, index) },
                                        onReminderClick = { onReminderClick(item, index) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
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
                                        NoteReminderRow(
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
                                            modifier = Modifier.testTag("NoteCard_${item.id}"),
                                            imageRoot = imageRoot,
                                            isSelected = isSelected,
                                            dateFormat = dateFormat,
                                            timeFormat = timeFormat,
                                            sortBy = sortBy,
                                            textSize = textSize,
                                            maxTitleLines = maxTitleLines,
                                            maxLines = maxLines,
                                            maxItems = maxItems,
                                            hideLabels = hideLabels,
                                            hideImages = hideImages,
                                            onClick = { onNoteClick(item, index) },
                                            onLongClick = { onNoteLongClick(item, index) },
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

@Preview(showBackground = true)
@Composable
private fun NotesGridScreenPreview() {
    NotallyXTheme {
        NotesGridScreen(
            items = emptyList(),
            isGrid = true,
        )
    }
}
