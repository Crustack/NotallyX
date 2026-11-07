package com.philkes.notallyx.feature.editor

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File

data class EditNoteUiState(
    val isLoading: Boolean = true,
    val noteId: Long = 0L,
    val title: String = "",
    val bodyValue: TextFieldValue = TextFieldValue(),
    val createdTimestamp: Long? = null,
    val modifiedTimestamp: Long? = null,
    val images: List<FileAttachment> = emptyList(),
    val mediaRoot: File? = null,
    val files: List<FileAttachment> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val labels: List<String> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val pinned: Boolean = false,
    val isPinnedToStatus: Boolean = false,
    val viewMode: NoteViewMode = NoteViewMode.EDIT,
    val folder: Folder = Folder.NOTES,
    val type: Type = Type.NOTE,
    val color: ColorString = BaseNote.COLOR_DEFAULT,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isFormattingMenuVisible: Boolean = false,
    val formattingState: TextFormattingState = TextFormattingState(),
    val searchState: SearchState = SearchState(),
    val preferences: EditNotePreferences = EditNotePreferences(),
    val activeDialog: EditorDialogState? = null,
    val activeBottomSheet: EditorBottomSheetState? = null,
)

sealed interface EditNoteAction {
    data class Init(val key: NotallyNavKey.EditNote) : EditNoteAction

    data class TitleChanged(val title: String) : EditNoteAction

    data class BodyChanged(val bodyValue: TextFieldValue) : EditNoteAction

    data class SearchQueryChanged(val query: String) : EditNoteAction

    data object NextSearchResult : EditNoteAction

    data object PrevSearchResult : EditNoteAction

    data object CloseSearch : EditNoteAction

    data object StartSearch : EditNoteAction

    data object ToggleFormattingMenu : EditNoteAction

    data object CloseFormattingMenu : EditNoteAction

    data object Undo : EditNoteAction

    data object Redo : EditNoteAction

    data object ToggleBold : EditNoteAction

    data object ToggleItalic : EditNoteAction

    data object ToggleStrikethrough : EditNoteAction

    data object ToggleMonospace : EditNoteAction

    data object ClearFormatting : EditNoteAction

    data object RequestLinkDialog : EditNoteAction

    data class LinkClicked(val url: String, val displayText: String, val range: TextRange) :
        EditNoteAction

    data class ApplyLink(val url: String, val displayText: String, val range: TextRange?) :
        EditNoteAction

    data class RemoveLink(val range: TextRange) : EditNoteAction

    data class OpenLink(val url: String) : EditNoteAction

    data class ActionClicked(val action: EditAction) : EditNoteAction

    data class ActionLongClicked(val action: EditAction) : EditNoteAction

    data class UpdateTopActions(val topActions: List<EditAction>) : EditNoteAction

    data class UpdateBottomAction(val bottomAction: EditAction) : EditNoteAction

    data object AddItemClicked : EditNoteAction

    data object MoreOptionsClicked : EditNoteAction

    data class ImageClicked(val image: FileAttachment) : EditNoteAction

    data class FileClicked(val file: FileAttachment) : EditNoteAction

    data class FileDelete(val file: FileAttachment) : EditNoteAction

    data class AudioClicked(val audio: Audio) : EditNoteAction

    data class ReminderClicked(val reminder: Reminder) : EditNoteAction

    data object BackClicked : EditNoteAction

    data object DismissDialog : EditNoteAction

    data object DismissBottomSheet : EditNoteAction

    // Results from Navigation 3 / Activity Launchers
    data class LabelsSelected(val selectedLabels: List<String>) : EditNoteAction

    data class ImagesDeleted(val deletedImages: List<String>) : EditNoteAction

    data class AudioDeleted(val audioName: String) : EditNoteAction

    data class AudioRecorded(val success: Boolean) : EditNoteAction

    data class RemindersUpdated(val noteId: Long) : EditNoteAction

    data class NotePicked(val noteId: Long, val noteTitle: String, val noteType: Type) :
        EditNoteAction

    data class ImagesPicked(val uris: Array<Uri>) : EditNoteAction

    data class FilesPicked(val uris: Array<Uri>) : EditNoteAction

    data class ExportFilePicked(val uri: Uri) : EditNoteAction

    data class AudioPermissionResult(val granted: Boolean) : EditNoteAction

    data class NotificationPermissionResult(val granted: Boolean) : EditNoteAction

    data class ColorSelected(val color: String) : EditNoteAction

    data class ColorChanged(val oldColor: String, val newColor: String) : EditNoteAction

    data class ColorDeleted(val colorToDelete: String, val newColor: String) : EditNoteAction

    data class SetDefaultColor(val newDefault: String) : EditNoteAction

    data class ExportConfirmed(val mimeType: ExportMimeType) : EditNoteAction

    data object DeleteForeverConfirmed : EditNoteAction
}

sealed interface EditNoteSideEffect {
    data class ShowToast(@StringRes val messageResId: Int) : EditNoteSideEffect

    data class OpenUrl(val url: String) : EditNoteSideEffect

    data class ShareNote(val note: BaseNote) : EditNoteSideEffect

    data class Navigate(val navKey: NotallyNavKey) : EditNoteSideEffect

    data class ExportNote(val note: BaseNote, val mimeType: ExportMimeType) : EditNoteSideEffect

    data class ExportFile(val uri: Uri, val note: BaseNote) : EditNoteSideEffect

    data class RequestAudioPermission(val permission: String, val showRationale: Boolean) :
        EditNoteSideEffect

    data class RequestNotificationPermission(val showRationale: Boolean) : EditNoteSideEffect

    data class RefreshStatusBarPin(val note: BaseNote, val isPinned: Boolean) : EditNoteSideEffect

    data class LaunchAddImages(val allowMultiple: Boolean) : EditNoteSideEffect

    data class LaunchAttachFiles(val allowMultiple: Boolean) : EditNoteSideEffect

    data class OpenActionSelection(val action: EditAction) : EditNoteSideEffect

    data class ViewAttachmentFile(val uri: Uri, val mimeType: String) : EditNoteSideEffect

    data object NavigateBack : EditNoteSideEffect
}
