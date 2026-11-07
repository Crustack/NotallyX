package com.philkes.notallyx.editor

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.bottomsheet.AddNoteBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.ExportBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreListBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreNoteBottomSheet
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.feature.main.PickNoteScreen
import java.util.Date

private val SAMPLE_NOTE_STATE =
    EditNoteState(
        title = "Project Planning Note",
        bodyValue =
            TextFieldValue(
                "Here are the core tasks for the upcoming release:\n- Review design mockups\n- Finalize Jetpack Compose migration\n- Conduct QA testing"
            ),
        createdTimestamp = 1700000000000L,
        modifiedTimestamp = 1700086400000L,
        images =
            listOf(
                FileAttachment(
                    localName = "sample_image.png",
                    originalName = "Sample Image.png",
                    mimeType = "image/png",
                )
            ),
        files =
            listOf(
                FileAttachment(
                    localName = "spec.pdf",
                    originalName = "Project_Spec.pdf",
                    mimeType = "application/pdf",
                )
            ),
        audios =
            listOf(
                Audio(
                    name = "Voice Memo 01",
                    duration = 42000L,
                    timestamp = 1700000000000L,
                )
            ),
        labels = listOf("Work", "Important"),
        reminders =
            listOf(
                Reminder(
                    id = 1L,
                    dateTime = Date(1700086400000L),
                    repetition = Repetition(1, RepetitionTimeUnit.DAYS),
                )
            ),
        pinned = true,
        type = Type.NOTE,
    )

private val SAMPLE_LIST_STATE =
    EditNoteState(
        title = "Weekly Grocery List",
        createdTimestamp = 1700000000000L,
        modifiedTimestamp = 1700086400000L,
        items =
            listOf(
                ListItem(
                    id = 0,
                    body = "Fresh Organic Apples",
                    checked = false,
                    isChild = false,
                    order = 1,
                    children = mutableListOf(),
                ),
                ListItem(
                    id = 1,
                    body = "Gala variety",
                    checked = false,
                    isChild = true,
                    order = 2,
                    children = mutableListOf(),
                ),
                ListItem(
                    id = 2,
                    body = "Whole Grain Bread",
                    checked = true,
                    isChild = false,
                    order = 3,
                    children = mutableListOf(),
                ),
            ),
        pinned = false,
        type = Type.LIST,
    )

private val SAMPLE_PREFERENCES =
    EditNotePreferences(
        notesSortBy = NotesSortBy.MODIFIED_DATE,
        dateFormat = DateFormat.DD_MM_YY_GER,
        timeFormat = TimeFormat.AM_PM,
        topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
        bottomAction = EditAction.TOGGLE_VIEW_MODE,
    )

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditNoteScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditNoteScreen(
            noteState = SAMPLE_NOTE_STATE,
            preferences = SAMPLE_PREFERENCES,
            canUndo = true,
            canRedo = false,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditNoteScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        EditNoteScreen(
            noteState = SAMPLE_NOTE_STATE,
            preferences = SAMPLE_PREFERENCES,
            canUndo = true,
            canRedo = false,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditListScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditListScreen(
            noteState = SAMPLE_LIST_STATE,
            preferences = SAMPLE_PREFERENCES,
            canUndo = false,
            canRedo = true,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EditListScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        EditListScreen(
            noteState = SAMPLE_LIST_STATE,
            preferences = SAMPLE_PREFERENCES,
            canUndo = false,
            canRedo = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@PreviewTest
@Composable
fun MoreNoteBottomSheet_Preview() {
    NotallyXTheme(theme = Theme.LIGHT) {
        MoreNoteBottomSheet(
            actionHandler = object : NoteActionHandler {},
            onDismissRequest = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@PreviewTest
@Composable
fun MoreListBottomSheet_Preview() {
    NotallyXTheme(theme = Theme.LIGHT) {
        MoreListBottomSheet(
            actionHandler = object : NoteActionHandler {},
            onDismissRequest = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@PreviewTest
@Composable
fun ExportBottomSheet_Preview() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ExportBottomSheet(
            onExport = {},
            onDismissRequest = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@PreviewTest
@Composable
fun AddNoteBottomSheet_Preview() {
    NotallyXTheme(theme = Theme.LIGHT) {
        AddNoteBottomSheet(
            actionHandler = object : NoteActionHandler {},
            onDismissRequest = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun PickNoteScreen_Preview() {
    NotallyXTheme(theme = Theme.LIGHT) {
        PickNoteScreen(
            notes =
                listOf(
                    BaseNote(
                        id = 1L,
                        type = Type.NOTE,
                        folder = Folder.NOTES,
                        color = BaseNote.COLOR_DEFAULT,
                        title = "Meeting Notes",
                        body = "Discuss roadmap",
                        pinned = false,
                        timestamp = 1700000000000L,
                        modifiedTimestamp = 1700000000000L,
                        labels = emptyList(),
                        spans = emptyList(),
                        items = emptyList(),
                        images = emptyList(),
                        files = emptyList(),
                        audios = emptyList(),
                        reminders = emptyList(),
                        viewMode = com.philkes.notallyx.data.model.NoteViewMode.EDIT,
                        isPinnedToStatus = false,
                    ),
                    BaseNote(
                        id = 2L,
                        type = Type.LIST,
                        folder = Folder.NOTES,
                        color = BaseNote.COLOR_DEFAULT,
                        title = "Shopping List",
                        body = "",
                        pinned = false,
                        timestamp = 1700000000000L,
                        modifiedTimestamp = 1700000000000L,
                        labels = emptyList(),
                        spans = emptyList(),
                        items = emptyList(),
                        images = emptyList(),
                        files = emptyList(),
                        audios = emptyList(),
                        reminders = emptyList(),
                        viewMode = com.philkes.notallyx.data.model.NoteViewMode.EDIT,
                        isPinnedToStatus = false,
                    ),
                ),
            isGrid = false,
            onNoteClick = {},
            onBackClick = {},
        )
    }
}
