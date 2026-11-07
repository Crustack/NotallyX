package com.philkes.notallyx.feature.media.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayAudioViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : BaseViewModel(app, preferences) {

    private val _sideEffects = Channel<PlayAudioSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<PlayAudioSideEffect> = _sideEffects.receiveAsFlow()

    private val _audio = MutableStateFlow<Audio?>(null)
    private val _currentPosition = MutableStateFlow(0L)
    private val _isPlaying = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _activeDialog = MutableStateFlow<PlayAudioDialogState?>(null)

    private var service: AudioPlayService? = null
    private var isBound = false
    private var serviceIntent: Intent? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val s = (binder as? LocalBinder<*>)?.getService() as? AudioPlayService ?: return
                val currentAudio = _audio.value ?: return
                s.initialise(currentAudio)
                s.onStateChange = { updateUI(s) }
                service = s
                updateUI(s)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }

    init {
        viewModelScope.launch {
            while (isActive) {
                service?.let { s ->
                    if (s.getState() == AudioPlayService.STARTED) {
                        _currentPosition.value = s.getCurrentPosition().toLong()
                    }
                }
                delay(100.milliseconds)
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

    private fun updateUI(s: AudioPlayService) {
        _currentPosition.value = s.getCurrentPosition().toLong()
        when (s.getState()) {
            AudioPlayService.PREPARED,
            AudioPlayService.PAUSED,
            AudioPlayService.COMPLETED -> {
                _isPlaying.value = false
                _errorMessage.value = null
            }
            AudioPlayService.STARTED -> {
                _isPlaying.value = true
                _errorMessage.value = null
            }
            AudioPlayService.ERROR -> {
                _isPlaying.value = false
                _errorMessage.value =
                    app.getString(
                        R.string.something_went_wrong_audio,
                        s.getErrorType(),
                        s.getErrorCode(),
                    )
            }
        }
    }

    fun onAction(action: PlayAudioAction) {
        when (action) {
            is PlayAudioAction.Init -> initAudio(action.key)
            is PlayAudioAction.PlayPauseClicked -> service?.play()
            is PlayAudioAction.Seek -> service?.seek(action.positionMs)
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

        if (!isBound) {
            val intent = Intent(app, AudioPlayService::class.java)
            serviceIntent = intent
            app.startService(intent)
            app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        }
    }

    private fun share() {
        val audio = _audio.value ?: return
        val audioRoot = app.getCurrentAudioDirectory()
        val file = if (audioRoot != null) File(audioRoot, audio.name) else null
        if (file != null && file.exists()) {
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
        val audioRoot = app.getCurrentAudioDirectory()
        val file = if (audioRoot != null) File(audioRoot, audio.name) else null
        if (file != null && file.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    app.contentResolver.openFileDescriptor(destinationUri, "w")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { output ->
                            FileInputStream(file).use { input -> input.copyTo(output) }
                        }
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
        if (isBound) {
            service?.onStateChange = null
            app.unbindService(serviceConnection)
            service = null
            serviceIntent?.let { app.stopService(it) }
            isBound = false
        }
    }
}
