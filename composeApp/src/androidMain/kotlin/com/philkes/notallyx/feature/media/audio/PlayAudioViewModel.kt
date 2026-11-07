package com.philkes.notallyx.feature.media.audio

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_AUDIOS
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AndroidFileStorage
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.FileStorage
import com.philkes.notallyx.core.platform.JvmFileStorage
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.platform.PlatformUri
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.FileInputStream
import java.text.DateFormat
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

class PlayAudioViewModel(
    preferences: NotallyXPreferences,
    databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    pathProvider: PathProvider = DesktopPathProvider(),
    private val fileStorage: FileStorage = JvmFileStorage(),
    private val audioPlayer: AudioPlayer = NoOpAudioPlayer(),
) : BaseViewModel(preferences, databaseStateFlow, pathProvider) {

    constructor(
        app: Application,
        preferences: NotallyXPreferences,
        audioPlayer: AudioPlayer =
            GlobalContext.getOrNull()?.getOrNull<AudioPlayer>() ?: NoOpAudioPlayer(),
    ) : this(
        preferences = preferences,
        databaseStateFlow = NotallyDatabase.getDatabase(app),
        pathProvider = AndroidPathProvider(app),
        fileStorage = AndroidFileStorage(app),
        audioPlayer = audioPlayer,
    ) {
        this.app = app
    }

    private val _sideEffects = Channel<PlayAudioSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<PlayAudioSideEffect> = _sideEffects.receiveAsFlow()

    private val _audio = MutableStateFlow<Audio?>(null)
    private val _currentPosition = MutableStateFlow(0L)
    private val _isPlaying = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _activeDialog = MutableStateFlow<PlayAudioDialogState?>(null)

    init {
        viewModelScope.launch { audioPlayer.isPlaying.collect { _isPlaying.value = it } }
        viewModelScope.launch {
            audioPlayer.currentPositionMs.collect { _currentPosition.value = it }
        }
        viewModelScope.launch {
            audioPlayer.errorCode.collect { code ->
                _errorMessage.value = code?.let {
                    if (hasApp) app.getString(R.string.something_went_wrong_audio, "", it)
                    else "Audio error code: $it"
                }
            }
        }
    }

    val uiState: StateFlow<PlayAudioUiState> =
        combine(
                _audio,
                _currentPosition,
                _isPlaying,
                _errorMessage,
                _activeDialog,
            ) { audio, currentPosition, isPlaying, errorMessage, activeDialog ->
                PlayAudioUiState(
                    audio = audio,
                    currentPosition = currentPosition,
                    duration = audio?.duration,
                    isPlaying = isPlaying,
                    errorMessage = errorMessage,
                    activeDialog = activeDialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PlayAudioUiState(),
            )

    fun onAction(action: PlayAudioAction) {
        when (action) {
            is PlayAudioAction.Init -> initAudio(action.key)
            is PlayAudioAction.PlayPauseClicked -> {
                if (audioPlayer.isPlaying.value) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.play()
                }
            }
            is PlayAudioAction.Seek -> audioPlayer.seekTo(action.positionMs)
            is PlayAudioAction.ShareClicked -> share()
            is PlayAudioAction.SaveClicked -> saveToDevice()
            is PlayAudioAction.ExportConfirmed -> exportConfirmed(action.destinationUri)
            is PlayAudioAction.DeleteClicked -> {
                _activeDialog.update { PlayAudioDialogState.ConfirmDelete }
            }
            is PlayAudioAction.DismissDialog -> {
                _activeDialog.update { null }
            }
            is PlayAudioAction.ConfirmDelete -> confirmDelete()
            is PlayAudioAction.BackClicked -> {
                viewModelScope.launch { _sideEffects.send(PlayAudioSideEffect.NavigateBack) }
            }
        }
    }

    private fun initAudio(key: NotallyNavKey.PlayAudio) {
        val audio = Audio(name = key.audioName, duration = key.duration, timestamp = key.timestamp)
        _audio.value = audio

        val audioRoot =
            pathProvider.getMediaDirectory(SUBFOLDER_AUDIOS, preferences.dataInPublicFolder.value)
        val file = java.io.File(audioRoot, audio.name)
        viewModelScope.launch { audioPlayer.loadAudio(file) }
    }

    private fun share() {
        val audio = _audio.value ?: return
        val audioRoot =
            pathProvider.getMediaDirectory(SUBFOLDER_AUDIOS, preferences.dataInPublicFolder.value)
        val file = java.io.File(audioRoot, audio.name)
        if (file.exists() && hasApp) {
            val uri = app.getUriForFile(file)
            viewModelScope.launch { _sideEffects.send(PlayAudioSideEffect.ShareAudio(uri)) }
        }
    }

    private fun saveToDevice() {
        val audio = _audio.value ?: return
        val formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
        val defaultName = "${formatter.format(audio.timestamp)}.m4a"
        viewModelScope.launch { _sideEffects.send(PlayAudioSideEffect.LaunchExport(defaultName)) }
    }

    private fun exportConfirmed(destinationUri: Uri) {
        val audio = _audio.value ?: return
        val audioRoot =
            pathProvider.getMediaDirectory(SUBFOLDER_AUDIOS, preferences.dataInPublicFolder.value)
        val file = java.io.File(audioRoot, audio.name)
        if (file.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    fileStorage.openOutputStream(PlatformUri(destinationUri.toString()))?.use {
                        output ->
                        FileInputStream(file).use { input -> input.copyTo(output) }
                    }
                    _sideEffects.send(PlayAudioSideEffect.ShowToast(R.string.saved_to_device))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun confirmDelete() {
        _activeDialog.update { null }
        val audio = _audio.value ?: return
        viewModelScope.launch {
            _sideEffects.send(PlayAudioSideEffect.NotifyAudioDeleted(audio.name))
            _sideEffects.send(PlayAudioSideEffect.NavigateBack)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
