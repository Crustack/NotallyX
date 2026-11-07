package com.philkes.notallyx.feature.media.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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

class RecordAudioViewModel(
    preferences: NotallyXPreferences,
    databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    pathProvider: PathProvider = DesktopPathProvider(),
    private val audioRecorder: AudioRecorder = NoOpAudioRecorder(),
) : BaseViewModel(preferences, databaseStateFlow, pathProvider) {

    constructor(
        app: Application,
        preferences: NotallyXPreferences,
    ) : this(
        preferences = preferences,
        databaseStateFlow = NotallyDatabase.getDatabase(app),
        pathProvider = AndroidPathProvider(app),
        audioRecorder = NoOpAudioRecorder(),
    ) {
        this.app = app
        val intent = Intent(app, AudioRecordService::class.java)
        serviceIntent = intent
        app.startService(intent)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    private val _sideEffects = Channel<RecordAudioSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<RecordAudioSideEffect> = _sideEffects.receiveAsFlow()

    private val _status = MutableStateFlow(Status.READY)
    private val _baseTime = MutableStateFlow(0L)
    private val _activeDialog = MutableStateFlow<RecordAudioDialogState?>(null)

    private var service: AudioRecordService? = null
    private var isBound = false
    private var serviceIntent: Intent? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val s = (binder as? LocalBinder<*>)?.getService() as? AudioRecordService ?: return
                service = s
                viewModelScope.launch {
                    s.status.collect { currentStatus ->
                        _status.value = currentStatus
                        _baseTime.value = s.getBase()
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
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
                val s = service ?: return
                when (s.status.value) {
                    Status.PAUSED -> s.resume()
                    Status.READY -> s.start()
                    Status.RECORDING -> s.pause()
                }
                _baseTime.value = s.getBase()
            }

            is RecordAudioAction.StopClicked -> {
                val s = service ?: return
                s.stop()
                viewModelScope.launch {
                    _sideEffects.send(RecordAudioSideEffect.RecordingFinished)
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }

            is RecordAudioAction.BackClicked -> {
                val s = service
                if (s == null || _status.value == Status.READY) {
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
                service?.stop()
                viewModelScope.launch {
                    _sideEffects.send(RecordAudioSideEffect.RecordingFinished)
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }

            is RecordAudioAction.DiscardConfirmed -> {
                _activeDialog.update { null }
                service?.stop()
                viewModelScope.launch(Dispatchers.IO) {
                    File(pathProvider.getCacheDirectory("audio_records"), "Temp.m4a").delete()
                    _sideEffects.send(RecordAudioSideEffect.NavigateBack)
                }
            }
        }
    }

    override fun onCleared() {
        if (isBound && hasApp) {
            app.unbindService(serviceConnection)
            service = null
            serviceIntent?.let { app.stopService(it) }
            isBound = false
        }
    }
}
