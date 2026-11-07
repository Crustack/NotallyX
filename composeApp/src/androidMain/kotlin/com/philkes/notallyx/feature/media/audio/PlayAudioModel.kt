package com.philkes.notallyx.feature.media.audio

import android.net.Uri
import androidx.annotation.StringRes
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.navigation.NotallyNavKey

data class PlayAudioUiState(
    val audio: Audio? = null,
    val currentPosition: Long = 0L,
    val duration: Long? = null,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val activeDialog: PlayAudioDialogState? = null,
)

sealed interface PlayAudioDialogState {
    data object ConfirmDelete : PlayAudioDialogState
}

sealed interface PlayAudioAction {
    data class Init(val key: NotallyNavKey.PlayAudio) : PlayAudioAction

    data object PlayPauseClicked : PlayAudioAction

    data class Seek(val positionMs: Long) : PlayAudioAction

    data object ShareClicked : PlayAudioAction

    data object SaveClicked : PlayAudioAction

    data object DeleteClicked : PlayAudioAction

    data object DismissDialog : PlayAudioAction

    data object ConfirmDelete : PlayAudioAction

    data class ExportConfirmed(val destinationUri: Uri) : PlayAudioAction

    data object BackClicked : PlayAudioAction
}

sealed interface PlayAudioSideEffect {
    data class ShareAudio(val uri: Uri) : PlayAudioSideEffect

    data class LaunchExport(val defaultName: String) : PlayAudioSideEffect

    data class ShowToast(@StringRes val messageRes: Int) : PlayAudioSideEffect

    data class NotifyAudioDeleted(val audioName: String) : PlayAudioSideEffect

    data object NavigateBack : PlayAudioSideEffect
}
