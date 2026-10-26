package com.philkes.notallyx.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState

private fun createColorSampleNote(color: Color, index: Long): BaseNote =
    BaseNote(
        id = 300L + index,
        type = Type.NOTE,
        folder = Folder.NOTES,
        color = color.toColorString(),
        title = "Note in ${color.name.lowercase().replaceFirstChar { it.uppercase() }}",
        pinned = index == 1L,
        timestamp = 1700000000000L + index * 1000L,
        modifiedTimestamp = 1700000000000L + index * 1000L,
        labels = listOf(color.name.lowercase()),
        body = "Checking text and icon contrast against ${color.name} background.",
        spans = emptyList(),
        items = emptyList(),
        images = emptyList(),
        files = emptyList(),
        audios = emptyList(),
        reminders = emptyList(),
        viewMode = NoteViewMode.EDIT,
        isPinnedToStatus = false,
    )

private val SAMPLE_COLOR_NOTES =
    Color.entries.mapIndexed { index, color -> createColorSampleNote(color, index.toLong()) }

@Composable
private fun ColorPaletteCards(theme: Theme) {
    NotallyXTheme(theme = theme) {
        Column(
            modifier = Modifier.width(360.dp).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SAMPLE_COLOR_NOTES.forEach { note -> BaseNoteCard(baseNote = note) }
        }
    }
}

// ==================== ColorPickerDialog Previews ====================

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun ColorPickerDialog_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ColorPickerDialog(
            currentColor = BaseNote.COLOR_DEFAULT,
            onDismissRequest = {},
            onColorSelected = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun ColorPickerDialog_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ColorPickerDialog(
            currentColor = BaseNote.COLOR_DEFAULT,
            onDismissRequest = {},
            onColorSelected = {},
        )
    }
}

// ==================== BaseNoteCard Palette Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCards_AllColors_Light() {
    ColorPaletteCards(Theme.LIGHT)
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCards_AllColors_Dark() {
    ColorPaletteCards(Theme.DARK)
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCards_AllColors_SuperDark() {
    ColorPaletteCards(Theme.SUPER_DARK)
}

// ==================== EditNoteScreen Colored Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_Colored_Coral_Light() {
    val coralComposeColor =
        ComposeColor(LocalContext.current.extractColor(Color.CORAL.toColorString()))
    NotallyXTheme(theme = Theme.LIGHT) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = "Coral Note in Light Mode",
                    bodyValue = TextFieldValue("Text and controls on Coral background."),
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    labels = listOf("Coral", "Design"),
                    type = Type.NOTE,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            backgroundColor = coralComposeColor,
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_Colored_Dusk_Dark() {
    val duskComposeColor =
        ComposeColor(LocalContext.current.extractColor(Color.DUSK.toColorString()))
    NotallyXTheme(theme = Theme.DARK) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = "Dusk Note in Dark Mode",
                    bodyValue =
                        TextFieldValue("Text and controls on Dusk background in dark mode."),
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    labels = listOf("Dusk"),
                    type = Type.NOTE,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            backgroundColor = duskComposeColor,
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_Colored_Mint_SuperDark() {
    val mintComposeColor =
        ComposeColor(LocalContext.current.extractColor(Color.MINT.toColorString()))
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = "Mint Note in OLED SuperDark",
                    bodyValue = TextFieldValue("High contrast test on Mint background."),
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    labels = listOf("Mint"),
                    type = Type.NOTE,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.TWENTY_FOUR_H,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            backgroundColor = mintComposeColor,
            canUndo = true,
        )
    }
}

// ==================== EditListScreen Colored Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditListScreen_Colored_Sand_Light() {
    val sandComposeColor =
        ComposeColor(LocalContext.current.extractColor(Color.SAND.toColorString()))
    NotallyXTheme(theme = Theme.LIGHT) {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Sand Checklist in Light Mode",
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    items =
                        listOf(
                            ListItem(
                                id = 1,
                                body = "Active checklist item",
                                checked = false,
                                isChild = false,
                                order = 0,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                id = 2,
                                body = "Indented child item",
                                checked = false,
                                isChild = true,
                                order = 1,
                                children = mutableListOf(),
                            ),
                        ),
                    checkedItems =
                        listOf(
                            ListItem(
                                id = 3,
                                body = "Completed checklist item",
                                checked = true,
                                isChild = false,
                                order = 2,
                                children = mutableListOf(),
                            )
                        ),
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
            backgroundColor = sandComposeColor,
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditListScreen_Colored_Storm_Dark() {
    val stormComposeColor =
        ComposeColor(LocalContext.current.extractColor(Color.STORM.toColorString()))
    NotallyXTheme(theme = Theme.DARK) {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Storm Checklist in Dark Mode",
                    createdTimestamp = 1700000000000L,
                    modifiedTimestamp = 1700000000000L,
                    items =
                        listOf(
                            ListItem(
                                id = 1,
                                body = "Task to complete",
                                checked = false,
                                isChild = false,
                                order = 0,
                                children = mutableListOf(),
                            )
                        ),
                    checkedItems =
                        listOf(
                            ListItem(
                                id = 2,
                                body = "Done task with strikethrough",
                                checked = true,
                                isChild = false,
                                order = 1,
                                children = mutableListOf(),
                            )
                        ),
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
            backgroundColor = stormComposeColor,
            canUndo = true,
        )
    }
}
