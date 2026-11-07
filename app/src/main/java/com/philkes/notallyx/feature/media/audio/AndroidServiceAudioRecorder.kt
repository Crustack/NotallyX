package com.philkes.notallyx.feature.media.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidServiceAudioRecorder(private val context: Context) : AudioRecorder {
    override val isSupported: Boolean = true

    private val _status = MutableStateFlow(Status.READY)
    override val status: StateFlow<Status> = _status.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedDurationMs = MutableStateFlow(0L)
    override val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs.asStateFlow()

    private var service: AudioRecordService? = null
    private var isBound = false
    private var serviceIntent: Intent? = null
    private var pendingAction: ((AudioRecordService) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var statusCollectorJob: Job? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val s = (binder as? LocalBinder<*>)?.getService() as? AudioRecordService ?: return
                service = s
                statusCollectorJob?.cancel()
                statusCollectorJob = scope.launch {
                    s.status.collect { st ->
                        _status.value = st
                        _isRecording.value = st == Status.RECORDING
                        _elapsedDurationMs.value = s.getElapsedDurationMs()
                    }
                }
                pendingAction?.let { action ->
                    pendingAction = null
                    action(s)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                statusCollectorJob?.cancel()
                statusCollectorJob = null
                service = null
                _status.value = Status.READY
                _isRecording.value = false
                _elapsedDurationMs.value = 0L
            }
        }

    private fun ensureBound(onBound: (AudioRecordService) -> Unit) {
        val s = service
        if (s != null) {
            onBound(s)
        } else {
            pendingAction = onBound
            if (!isBound) {
                val intent = Intent(context, AudioRecordService::class.java)
                serviceIntent = intent
                context.startService(intent)
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                isBound = true
            }
        }
    }

    override fun startRecording() {
        ensureBound { s ->
            s.start()
            _status.value = s.status.value
            _isRecording.value = s.status.value == Status.RECORDING
            _elapsedDurationMs.value = s.getElapsedDurationMs()
        }
    }

    override fun pauseRecording() {
        ensureBound { s ->
            s.pause()
            _status.value = s.status.value
            _isRecording.value = s.status.value == Status.RECORDING
            _elapsedDurationMs.value = s.getElapsedDurationMs()
        }
    }

    override fun resumeRecording() {
        ensureBound { s ->
            s.resume()
            _status.value = s.status.value
            _isRecording.value = s.status.value == Status.RECORDING
            _elapsedDurationMs.value = s.getElapsedDurationMs()
        }
    }

    override fun stopRecording(): File? {
        pendingAction = null
        service?.stop()
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {
                // ignore
            }
            service = null
            isBound = false
        }
        _status.value = Status.READY
        _isRecording.value = false
        _elapsedDurationMs.value = 0L
        return context.getTempAudioFile()
    }

    override fun release() {
        pendingAction = null
        statusCollectorJob?.cancel()
        statusCollectorJob = null
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {
                // ignore
            }
            service = null
            serviceIntent?.let {
                try {
                    context.stopService(it)
                } catch (_: Exception) {
                    // ignore
                }
            }
            isBound = false
        }
        _status.value = Status.READY
        _isRecording.value = false
        _elapsedDurationMs.value = 0L
    }
}
