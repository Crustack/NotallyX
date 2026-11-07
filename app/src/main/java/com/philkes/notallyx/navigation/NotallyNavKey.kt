package com.philkes.notallyx.navigation

import androidx.navigation3.runtime.NavKey
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.migration.ExportMimeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NotallyNavKey : NavKey {

    // --- Top-Level Drawer Destinations (Independent Back Stacks) ---

    @Serializable @SerialName("Notes") data object Notes : NotallyNavKey

    @Serializable @SerialName("Unlabeled") data object Unlabeled : NotallyNavKey

    @Serializable @SerialName("Labels") data object Labels : NotallyNavKey

    @Serializable @SerialName("Deleted") data object Deleted : NotallyNavKey

    @Serializable @SerialName("Archived") data object Archived : NotallyNavKey

    @Serializable @SerialName("Reminders") data object Reminders : NotallyNavKey

    @Serializable @SerialName("Settings") data object Settings : NotallyNavKey

    // --- Sub-Routes & Screens ---

    @Serializable
    @SerialName("DisplayLabel")
    data class DisplayLabel(val label: String) : NotallyNavKey

    @Serializable
    @SerialName("Search")
    data class Search(
        val initialFolder: Folder? = null,
        val initialLabel: String? = null,
    ) : NotallyNavKey

    // --- Editor Destinations (Migrated from Activities) ---

    @Serializable
    @SerialName("EditNote")
    data class EditNote(
        val noteId: Long? = null,
        val initialFolder: Folder? = null,
        val initialLabel: String? = null,
        val initialTitle: String? = null,
        val initialBody: String? = null,
        val initialImageUris: List<String> = emptyList(),
        val initialFileUris: List<String> = emptyList(),
    ) : NotallyNavKey

    @Serializable
    @SerialName("EditList")
    data class EditList(
        val noteId: Long? = null,
        val initialFolder: Folder? = null,
        val initialLabel: String? = null,
    ) : NotallyNavKey

    @Serializable
    @SerialName("EditTextPlain")
    data class EditTextPlain(val fileUri: String? = null) : NotallyNavKey

    // --- Media & Utility Screens ---

    @Serializable
    @SerialName("ViewImage")
    data class ViewImage(
        val noteId: Long? = null,
        val imageUris: List<String> = emptyList(),
        val initialIndex: Int = 0,
    ) : NotallyNavKey

    @Serializable
    @SerialName("PlayAudio")
    data class PlayAudio(
        val audioName: String,
        val duration: Long? = null,
        val timestamp: Long = 0L,
    ) : NotallyNavKey

    @Serializable @SerialName("RecordAudio") data object RecordAudio : NotallyNavKey

    @Serializable
    @SerialName("PickNote")
    data class PickNote(val excludeNoteId: Long? = null) : NotallyNavKey

    // --- Navigation 3 Dialog & Scene Keys ---

    @Serializable
    @SerialName("NoteReminders")
    data class NoteReminders(val noteId: Long) : NotallyNavKey

    @Serializable
    @SerialName("SelectLabels")
    data class SelectLabels(val currentLabels: List<String> = emptyList()) : NotallyNavKey

    @Serializable
    @SerialName("ColorPicker")
    data class ColorPicker(
        val currentColor: String? = null,
        val colors: List<String> = emptyList(),
        val defaultColor: String = BaseNote.COLOR_DEFAULT,
    ) : NotallyNavKey

    @Serializable
    @SerialName("ExportFormat")
    data class ExportFormat(val formats: List<ExportMimeType> = ExportMimeType.entries) :
        NotallyNavKey

    @Serializable
    @SerialName("ConfirmDialog")
    data class Confirm(
        val title: String? = null,
        val message: String,
        val actionType: String,
        val positiveButtonText: String? = null,
        val negativeButtonText: String? = null,
    ) : NotallyNavKey
}
