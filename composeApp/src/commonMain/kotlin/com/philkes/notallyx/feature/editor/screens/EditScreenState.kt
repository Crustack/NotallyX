package com.philkes.notallyx.feature.editor.screens

import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import java.io.File

data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val resultsCount: Int = 0,
    val resultPos: Int = 0,
)

data class EditNoteState(
    val title: String = "",
    val bodyValue: TextFieldValue = TextFieldValue(),
    val items: List<ListItem> = emptyList(),
    val checkedItems: List<ListItem> = emptyList(),
    val createdTimestamp: Long? = null,
    val modifiedTimestamp: Long? = null,
    val images: List<FileAttachment> = emptyList(),
    val mediaRoot: File? = null,
    val files: List<FileAttachment> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val labels: List<String> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val pinned: Boolean = false,
    val viewMode: NoteViewMode = NoteViewMode.EDIT,
    val folder: Folder = Folder.NOTES,
    val type: Type = Type.NOTE,
    val isPinnedToStatus: Boolean = false,
)

data class EditNotePreferences(
    val notesSortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormat: TimeFormat = TimeFormat.NONE,
    val topActions: List<EditAction> =
        listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
    val bottomAction: EditAction = EditAction.TOGGLE_VIEW_MODE,
)
