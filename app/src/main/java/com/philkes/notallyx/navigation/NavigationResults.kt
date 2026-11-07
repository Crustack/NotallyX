package com.philkes.notallyx.navigation

import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.migration.ExportMimeType
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmResult(
    val actionType: String,
    val confirmed: Boolean = true,
)

@Serializable
sealed interface ColorPickerResult {
    @Serializable data class Selected(val color: String) : ColorPickerResult

    @Serializable data class Changed(val oldColor: String, val newColor: String) : ColorPickerResult

    @Serializable
    data class Deleted(val colorToDelete: String, val newColor: String) : ColorPickerResult

    @Serializable data class DefaultSet(val defaultColor: String) : ColorPickerResult
}

@Serializable data class ExportFormatResult(val format: ExportMimeType)

@Serializable
data class PickedNoteResult(
    val noteId: Long,
    val noteTitle: String,
    val noteType: Type,
)

@Serializable data class SelectedLabelsResult(val selectedLabels: List<String>)

@Serializable data class ImagesDeletedResult(val deletedImages: List<String>)

@Serializable data class AudioDeletedResult(val audioName: String)

@Serializable data class AudioRecordedResult(val success: Boolean = true)

@Serializable data class RemindersUpdatedResult(val noteId: Long)
