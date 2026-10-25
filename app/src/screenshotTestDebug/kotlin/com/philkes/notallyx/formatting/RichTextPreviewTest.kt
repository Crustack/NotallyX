package com.philkes.notallyx.formatting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.philkes.notallyx.core.ui.extensions.toAnnotatedString
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState

private const val RICH_TEXT_BODY =
    "Headline Section\n" +
        "This text contains bold styling and italic emphasis with nested bold-italic.\n" +
        "Code sample: val x = 42 with monospace and strikethrough deprecated.\n" +
        "Visit official repository at https://github.com/Crustack/NotallyX for project details."

private val RICH_TEXT_SPANS =
    listOf(
        SpanRepresentation(start = 0, end = 16, bold = true),
        SpanRepresentation(start = 36, end = 48, bold = true),
        SpanRepresentation(start = 53, end = 68, italic = true),
        SpanRepresentation(start = 74, end = 92, bold = true, italic = true),
        SpanRepresentation(start = 106, end = 116, monospace = true),
        SpanRepresentation(start = 135, end = 159, strikethrough = true),
        SpanRepresentation(
            start = 189,
            end = 224,
            link = true,
            linkData = "https://github.com/Crustack/NotallyX",
        ),
    )

private val RICH_TEXT_NOTE =
    BaseNote(
        id = 201L,
        type = Type.NOTE,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = "Rich Text Formatting",
        pinned = false,
        timestamp = 1700000000000L,
        modifiedTimestamp = 1700000000000L,
        labels = listOf("Docs", "Formatting"),
        body = RICH_TEXT_BODY,
        spans = RICH_TEXT_SPANS,
        items = emptyList(),
        images = emptyList(),
        files = emptyList(),
        audios = emptyList(),
        reminders = emptyList(),
        viewMode = NoteViewMode.EDIT,
        isPinnedToStatus = false,
    )

private const val LONG_URL_BODY =
    "Check the complete documentation and release notes at: " +
        "https://github.com/Crustack/NotallyX/wiki/Rich-Text-Formatting-Guidelines-And-Edge-Cases-For-Modern-Android-Note-Taking-Applications"

private val LONG_URL_SPANS =
    listOf(
        SpanRepresentation(
            start = 55,
            end = 55 + 130,
            link = true,
            linkData =
                "https://github.com/Crustack/NotallyX/wiki/Rich-Text-Formatting-Guidelines-And-Edge-Cases-For-Modern-Android-Note-Taking-Applications",
        )
    )

private val LONG_URL_NOTE =
    BaseNote(
        id = 202L,
        type = Type.NOTE,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = "Long URL Wrapping",
        pinned = false,
        timestamp = 1700000000000L,
        modifiedTimestamp = 1700000000000L,
        labels = listOf("Links"),
        body = LONG_URL_BODY,
        spans = LONG_URL_SPANS,
        items = emptyList(),
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

// ==================== EditNoteScreen Rich Text Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_RichText_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = RICH_TEXT_NOTE.title,
                    bodyValue = TextFieldValue(RICH_TEXT_BODY.toAnnotatedString(RICH_TEXT_SPANS)),
                    createdTimestamp = RICH_TEXT_NOTE.timestamp,
                    modifiedTimestamp = RICH_TEXT_NOTE.modifiedTimestamp,
                    labels = RICH_TEXT_NOTE.labels,
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
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_RichText_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = RICH_TEXT_NOTE.title,
                    bodyValue = TextFieldValue(RICH_TEXT_BODY.toAnnotatedString(RICH_TEXT_SPANS)),
                    createdTimestamp = RICH_TEXT_NOTE.timestamp,
                    modifiedTimestamp = RICH_TEXT_NOTE.modifiedTimestamp,
                    labels = RICH_TEXT_NOTE.labels,
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
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_RichText_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = RICH_TEXT_NOTE.title,
                    bodyValue = TextFieldValue(RICH_TEXT_BODY.toAnnotatedString(RICH_TEXT_SPANS)),
                    createdTimestamp = RICH_TEXT_NOTE.timestamp,
                    modifiedTimestamp = RICH_TEXT_NOTE.modifiedTimestamp,
                    labels = RICH_TEXT_NOTE.labels,
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
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditNoteScreen_RichText_WithFormattingMenu() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = RICH_TEXT_NOTE.title,
                    bodyValue = TextFieldValue(RICH_TEXT_BODY.toAnnotatedString(RICH_TEXT_SPANS)),
                    createdTimestamp = RICH_TEXT_NOTE.timestamp,
                    modifiedTimestamp = RICH_TEXT_NOTE.modifiedTimestamp,
                    labels = RICH_TEXT_NOTE.labels,
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
            canUndo = true,
            isFormattingMenuVisible = true,
            formattingState = TextFormattingState(isBold = true, isItalic = true),
        )
    }
}

// ==================== BaseNoteCard Rich Text Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_RichText_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = RICH_TEXT_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_RichText_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = RICH_TEXT_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_RichText_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = RICH_TEXT_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_RichText_LongUrl() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = LONG_URL_NOTE) }
    }
}
