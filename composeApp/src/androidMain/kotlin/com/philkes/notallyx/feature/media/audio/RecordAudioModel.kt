package com.philkes.notallyx.feature.media.audio

data class RecordAudioUiState(
    val status: Status = Status.READY,
    val baseTime: Long = 0L,
    val activeDialog: RecordAudioDialogState? = null,
)

sealed interface RecordAudioDialogState {
    data object ConfirmSave : RecordAudioDialogState
}

sealed interface RecordAudioAction {
    data object MainClicked : RecordAudioAction

    data object StopClicked : RecordAudioAction

    data object BackClicked : RecordAudioAction

    data object DismissDialog : RecordAudioAction

    data object SaveConfirmed : RecordAudioAction

    data object DiscardConfirmed : RecordAudioAction
}

sealed interface RecordAudioSideEffect {
    data object RecordingFinished : RecordAudioSideEffect

    data object NavigateBack : RecordAudioSideEffect
}
