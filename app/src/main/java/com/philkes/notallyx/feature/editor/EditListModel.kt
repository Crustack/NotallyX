package com.philkes.notallyx.feature.editor

import android.net.Uri
import androidx.annotation.StringRes
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction as PrefEditListAction
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File

data class EditListUiState(
    val isLoading: Boolean = true,
    val noteId: Long = 0L,
    val title: String = "",
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
    val isPinnedToStatus: Boolean = false,
    val viewMode: NoteViewMode = NoteViewMode.EDIT,
    val folder: Folder = Folder.NOTES,
    val type: Type = Type.LIST,
    val color: ColorString = BaseNote.COLOR_DEFAULT,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val searchState: SearchState = SearchState(),
    val focusFirstItem: Boolean = false,
    val preferences: EditNotePreferences = EditNotePreferences(),
    val activeDialog: EditorDialogState? = null,
    val activeBottomSheet: EditorBottomSheetState? = null,
)

sealed interface EditListAction {
    data class Init(val key: NotallyNavKey.EditList) : EditListAction

    data class TitleChanged(val title: String) : EditListAction

    data class ItemTextChanged(val item: ListItem, val newText: String) : EditListAction

    data class ItemChecked(val item: ListItem, val checked: Boolean) : EditListAction

    data class ItemDeleted(val item: ListItem) : EditListAction

    data class ItemSwipeRight(val item: ListItem) : EditListAction

    data class ItemSwipeLeft(val item: ListItem) : EditListAction

    data class ItemMove(val fromIndex: Int, val toIndex: Int, val originalItem: ListItem? = null) :
        EditListAction

    data object AddItemClicked : EditListAction

    data class SearchQueryChanged(val query: String) : EditListAction

    data object NextSearchResult : EditListAction

    data object PrevSearchResult : EditListAction

    data object CloseSearch : EditListAction

    data object StartSearch : EditListAction

    data object Undo : EditListAction

    data object Redo : EditListAction

    data class ActionClicked(val action: EditAction) : EditListAction

    data class ActionLongClicked(val action: EditAction) : EditListAction

    data class ListActionClicked(val action: PrefEditListAction) : EditListAction

    data class UpdateTopActions(val topActions: List<EditAction>) : EditListAction

    data class UpdateBottomAction(val bottomAction: EditAction) : EditListAction

    data object MoreOptionsClicked : EditListAction

    data class ImageClicked(val image: FileAttachment) : EditListAction

    data class FileClicked(val file: FileAttachment) : EditListAction

    data class FileDelete(val file: FileAttachment) : EditListAction

    data class AudioClicked(val audio: Audio) : EditListAction

    data class ReminderClicked(val reminder: Reminder) : EditListAction

    data object BackClicked : EditListAction

    data object DismissDialog : EditListAction

    data object DismissBottomSheet : EditListAction

    // Results from Navigation 3 / Activity Launchers
    data class LabelsSelected(val selectedLabels: List<String>) : EditListAction

    data class ImagesDeleted(val deletedImages: List<String>) : EditListAction

    data class AudioDeleted(val audioName: String) : EditListAction

    data class AudioRecorded(val success: Boolean) : EditListAction

    data class RemindersUpdated(val noteId: Long) : EditListAction

    data class ImagesPicked(val uris: Array<Uri>) : EditListAction

    data class FilesPicked(val uris: Array<Uri>) : EditListAction

    data class ExportFilePicked(val uri: Uri) : EditListAction

    data class AudioPermissionResult(val granted: Boolean) : EditListAction

    data class NotificationPermissionResult(val granted: Boolean) : EditListAction

    data class ColorSelected(val color: String) : EditListAction

    data class ColorChanged(val oldColor: String, val newColor: String) : EditListAction

    data class ColorDeleted(val colorToDelete: String, val newColor: String) : EditListAction

    data class SetDefaultColor(val newDefault: String) : EditListAction

    data class ExportConfirmed(val mimeType: ExportMimeType) : EditListAction

    data object DeleteForeverConfirmed : EditListAction
}

sealed interface EditListSideEffect {
    data class ShowToast(@StringRes val messageResId: Int) : EditListSideEffect

    data class ShareNote(val note: BaseNote) : EditListSideEffect

    data class Navigate(val navKey: NotallyNavKey) : EditListSideEffect

    data class ExportNote(val note: BaseNote, val mimeType: ExportMimeType) : EditListSideEffect

    data class ExportFile(val uri: Uri, val note: BaseNote) : EditListSideEffect

    data class RequestAudioPermission(val permission: String, val showRationale: Boolean) :
        EditListSideEffect

    data class RequestNotificationPermission(val showRationale: Boolean) : EditListSideEffect

    data class RefreshStatusBarPin(val note: BaseNote, val isPinned: Boolean) : EditListSideEffect

    data class LaunchAddImages(val allowMultiple: Boolean) : EditListSideEffect

    data class LaunchAttachFiles(val allowMultiple: Boolean) : EditListSideEffect

    data class OpenActionSelection(val action: EditAction) : EditListSideEffect

    data class ViewAttachmentFile(val uri: Uri, val mimeType: String) : EditListSideEffect

    data object NavigateBack : EditListSideEffect
}
