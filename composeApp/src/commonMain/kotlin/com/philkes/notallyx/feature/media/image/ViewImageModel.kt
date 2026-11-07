package com.philkes.notallyx.feature.media.image

import com.philkes.notallyx.core.platform.PlatformUri
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.navigation.NotallyNavKey

data class ViewImageUiState(
    val images: List<FileAttachment> = emptyList(),
    val initialPage: Int = 0,
    val activeDialog: ViewImageDialogState? = null,
    val isLoading: Boolean = false,
)

sealed interface ViewImageDialogState {
    data class ConfirmDelete(val position: Int) : ViewImageDialogState
}

sealed interface ViewImageAction {
    data class Init(val key: NotallyNavKey.ViewImage) : ViewImageAction

    data class ShareClicked(val position: Int) : ViewImageAction

    data class SaveClicked(val position: Int) : ViewImageAction

    data class DeleteClicked(val position: Int) : ViewImageAction

    data object DismissDialog : ViewImageAction

    data class ConfirmDelete(val position: Int) : ViewImageAction

    data class ExportConfirmed(val destinationUri: PlatformUri) : ViewImageAction

    data object BackClicked : ViewImageAction
}

sealed interface ViewImageSideEffect {
    data class ShareImage(val uri: PlatformUri, val mimeType: String) : ViewImageSideEffect

    data class LaunchExport(val defaultName: String, val mimeType: String) : ViewImageSideEffect

    data class ShowToast(val messageRes: Int) : ViewImageSideEffect

    data class NotifyImagesDeleted(val deletedImages: List<String>) : ViewImageSideEffect

    data object NavigateBack : ViewImageSideEffect
}
