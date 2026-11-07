package com.philkes.notallyx.feature.media.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidServiceAudioRecorder(private val context: Context) : AudioRecorder {
    override val isSupported: Boolean = true
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording

    private val _elapsedDurationMs = MutableStateFlow(0L)
    override val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs

    private var service: AudioRecordService? = null
    private var isBound = false
    private var serviceIntent: Intent? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val s = (binder as? LocalBinder<*>)?.getService() as? AudioRecordService ?: return
                service = s
                _isRecording.value = s.status.value == Status.RECORDING
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                _isRecording.value = false
            }
        }

    override fun startRecording() {
        if (!isBound) {
            val intent = Intent(context, AudioRecordService::class.java)
            serviceIntent = intent
            context.startService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        }
        service?.let { s ->
            when (s.status.value) {
                Status.PAUSED -> s.resume()
                Status.READY -> s.start()
                Status.RECORDING -> s.pause()
            }
            _isRecording.value = s.status.value == Status.RECORDING
            _elapsedDurationMs.value = s.getBase()
        }
    }

    override fun stopRecording(): File? {
        service?.stop()
        _isRecording.value = false
        return context.getTempAudioFile()
    }

    override fun release() {
        if (isBound) {
            context.unbindService(serviceConnection)
            service = null
            serviceIntent?.let { context.stopService(it) }
            isBound = false
        }
    }
}
