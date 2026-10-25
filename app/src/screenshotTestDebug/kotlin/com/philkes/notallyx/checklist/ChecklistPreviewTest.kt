package com.philkes.notallyx.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.screens.ChecklistItemRow
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteState

private val HIERARCHY_ITEMS =
    listOf(
        ListItem(
            id = 1,
            body = "Weekly Groceries",
            checked = false,
            isChild = false,
            order = 0,
            children = mutableListOf(),
        ),
        ListItem(
            id = 2,
            body = "Organic Whole Milk",
            checked = false,
            isChild = true,
            order = 1,
            children = mutableListOf(),
        ),
        ListItem(
            id = 3,
            body = "Honeycrisp Apples",
            checked = false,
            isChild = true,
            order = 2,
            children = mutableListOf(),
        ),
        ListItem(
            id = 4,
            body = "Household Chores",
            checked = false,
            isChild = false,
            order = 3,
            children = mutableListOf(),
        ),
        ListItem(
            id = 5,
            body = "Eco-friendly laundry detergent",
            checked = false,
            isChild = true,
            order = 4,
            children = mutableListOf(),
        ),
    )

private val CHECKED_ITEMS =
    listOf(
        ListItem(
            id = 6,
            body = "Sourdough bread loaf",
            checked = true,
            isChild = false,
            order = 5,
            children = mutableListOf(),
        ),
        ListItem(
            id = 7,
            body = "Cold brew coffee concentrate",
            checked = true,
            isChild = true,
            order = 6,
            children = mutableListOf(),
        ),
    )

private val LONG_TEXT_ITEMS =
    listOf(
        ListItem(
            id = 10,
            body =
                "Review and summarize the entire system architecture document including migration strategy for Room database, Koin dependency injection framework, and screenshot testing infrastructure.",
            checked = false,
            isChild = false,
            order = 0,
            children = mutableListOf(),
        ),
        ListItem(
            id = 11,
            body =
                "Ensure backward compatibility for SQLCipher encrypted database migrations across Android API levels 24 to 36.",
            checked = false,
            isChild = true,
            order = 1,
            children = mutableListOf(),
        ),
        ListItem(
            id = 12,
            body =
                "Completed task: verify all edge cases for dark mode contrast and typography scale across varied screen form factors.",
            checked = true,
            isChild = false,
            order = 2,
            children = mutableListOf(),
        ),
    )

private val CHECKLIST_NOTE =
    BaseNote(
        id = 401L,
        type = Type.LIST,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = "Checklist Hierarchy Note",
        pinned = false,
        timestamp = 1700000000000L,
        modifiedTimestamp = 1700000000000L,
        labels = listOf("Chores", "Weekly"),
        body = "",
        spans = emptyList(),
        items = HIERARCHY_ITEMS + CHECKED_ITEMS,
        images = emptyList(),
        files = emptyList(),
        audios = emptyList(),
        reminders = emptyList(),
        viewMode = NoteViewMode.EDIT,
        isPinnedToStatus = false,
    )

@Composable
private fun CardScreenshotWrapper(content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(360.dp).padding(8.dp)) { content() }
}

// ==================== EditListScreen Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditListScreen_HierarchyAndCompleted_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Checklist with Hierarchy & Completed Section",
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    items = HIERARCHY_ITEMS,
                    checkedItems = CHECKED_ITEMS,
                    labels = listOf("Weekly"),
                    type = Type.LIST,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            canUndo = true,
            canRedo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditListScreen_HierarchyAndCompleted_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Checklist with Hierarchy & Completed Section",
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    items = HIERARCHY_ITEMS,
                    checkedItems = CHECKED_ITEMS,
                    labels = listOf("Weekly"),
                    type = Type.LIST,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            canUndo = true,
            canRedo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditListScreen_LongMultiLineItems() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Checklist with Long Multi-Line Items",
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    items = LONG_TEXT_ITEMS.filter { !it.checked },
                    checkedItems = LONG_TEXT_ITEMS.filter { it.checked },
                    type = Type.LIST,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            canUndo = true,
        )
    }
}

// ==================== ChecklistItemRow Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ChecklistItemRow_Unchecked_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ChecklistItemRow(
            value = "Unchecked active item",
            onValueChange = {},
            isChecked = false,
            onCheckedChange = {},
            isChild = false,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ChecklistItemRow_Checked_Strikethrough_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ChecklistItemRow(
            value = "Checked item with strikethrough and muted opacity",
            onValueChange = {},
            isChecked = true,
            onCheckedChange = {},
            isChild = false,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ChecklistItemRow_Child_Indented_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ChecklistItemRow(
            value = "Indented child checklist item",
            onValueChange = {},
            isChecked = false,
            onCheckedChange = {},
            isChild = true,
        )
    }
}

// ==================== BaseNoteCard Checklist Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Checklist_Hierarchy_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper {
            BaseNoteCard(
                baseNote = CHECKLIST_NOTE,
                config = NoteCardDisplayConfig(maxItems = 5),
            )
        }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Checklist_Overflow_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper {
            BaseNoteCard(
                baseNote = CHECKLIST_NOTE,
                config = NoteCardDisplayConfig(maxItems = 3),
            )
        }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Checklist_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper {
            BaseNoteCard(
                baseNote = CHECKLIST_NOTE,
                config = NoteCardDisplayConfig(maxItems = 5),
            )
        }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Checklist_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper {
            BaseNoteCard(
                baseNote = CHECKLIST_NOTE,
                config = NoteCardDisplayConfig(maxItems = 5),
            )
        }
    }
}
