package com.philkes.notallyx.feature.media.audio

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.preference.NotallyXPreferences
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class RecordAudioViewModel(
    preferences: NotallyXPreferences,
    databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    pathProvider: PathProvider = DesktopPathProvider(),
    private val audioRecorder: AudioRecorder = NoOpAudioRecorder(),
) : BaseViewModel(preferences, databaseStateFlow, pathProvider) {

    constructor(
        app: Application,
        preferences: NotallyXPreferences,
        audioRecorder: AudioRecorder =
            GlobalContext.getOrNull()?.getOrNull<AudioRecorder>() ?: NoOpAudioRecorder(),
    ) : this(
        preferences = preferences,
        databaseStateFlow = NotallyDatabase.getDatabase(app),
        pathProvider = AndroidPathProvider(app),
        audioRecorder = audioRecorder,
    ) {
        this.app = app
    }

    private val _sideEffects = Channel<RecordAudioSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<RecordAudioSideEffect> = _sideEffects.receiveAsFlow()

    private val _status = MutableStateFlow(Status.READY)
    private val _baseTime = MutableStateFlow(0L)
    private val _activeDialog = MutableStateFlow<RecordAudioDialogState?>(null)

    init {
        viewModelScope.launch { audioRecorder.status.collect { status -> _status.value = status } }
        viewModelScope.launch {
            audioRecorder.elapsedDurationMs.collect { duration -> _baseTime.value = duration }
        }
    }

    val uiState: StateFlow<RecordAudioUiState> =
        combine(_status, _baseTime, _activeDialog) { status, baseTime, activeDialog ->
                RecordAudioUiState(
                    status = status,
                    baseTime = baseTime,
                    activeDialog = activeDialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = RecordAudioUiState(),
            )

    fun onAction(action: RecordAudioAction) {
        when (action) {
            is RecordAudioAction.MainClicked -> {
                when (_status.value) {
                    Status.READY -> audioRecorder.startRecording()
                    Status.RECORDING -> audioRecorder.pauseRecording()
                    Status.PAUSED -> audioRecorder.resumeRecording()
                }
            }

            is RecordAudioAction.StopClicked -> {
                audioRecorder.stopRecording()
                viewModelScope.launch {
                    _sideEffects.send(RecordAudioSideEffect.RecordingFinished)
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }

            is RecordAudioAction.BackClicked -> {
                if (_status.value == Status.READY) {
                    viewModelScope.launch { _sideEffects.send(RecordAudioSideEffect.NavigateBack) }
                } else {
                    _activeDialog.update { RecordAudioDialogState.ConfirmSave }
                }
            }

            is RecordAudioAction.DismissDialog -> {
                _activeDialog.update { null }
            }

            is RecordAudioAction.SaveConfirmed -> {
                _activeDialog.update { null }
                audioRecorder.stopRecording()
                viewModelScope.launch {
                    _sideEffects.send(RecordAudioSideEffect.RecordingFinished)
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }

            is RecordAudioAction.DiscardConfirmed -> {
                _activeDialog.update { null }
                val file = audioRecorder.stopRecording()
                file?.delete()
                viewModelScope.launch(Dispatchers.IO) {
                    File(pathProvider.getCacheDirectory("audio_records"), "Temp.m4a").delete()
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }
}
