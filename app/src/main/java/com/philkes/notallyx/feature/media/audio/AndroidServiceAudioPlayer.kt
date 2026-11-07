package com.philkes.notallyx.feature.media.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.philkes.notallyx.data.model.Audio
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidServiceAudioPlayer(private val context: Context) : AudioPlayer {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs

    private val _errorCode = MutableStateFlow<Int?>(null)
    override val errorCode: StateFlow<Int?> = _errorCode

    private var service: AudioPlayService? = null
    private var isBound = false
    private var serviceIntent: Intent? = null
    private var pendingAudio: Audio? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val s = (binder as? LocalBinder<*>)?.getService() as? AudioPlayService ?: return
                service = s
                pendingAudio?.let { s.initialise(it) }
                s.onStateChange = { syncFromService(s) }
                syncFromService(s)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }

    private fun syncFromService(s: AudioPlayService) {
        _currentPositionMs.value = s.getCurrentPosition().toLong()
        when (s.getState()) {
            AudioPlayService.STARTED -> {
                _isPlaying.value = true
                _errorCode.value = null
            }
            AudioPlayService.ERROR -> {
                _isPlaying.value = false
                _errorCode.value = s.getErrorCode()
            }
            else -> {
                _isPlaying.value = false
                _errorCode.value = null
            }
        }
    }

    override suspend fun loadAudio(file: File) {
        val audio = Audio(file.name, null, file.lastModified())
        pendingAudio = audio
        if (!isBound) {
            val intent = Intent(context, AudioPlayService::class.java)
            serviceIntent = intent
            context.startService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        } else {
            service?.initialise(audio)
        }
    }

    override fun play() {
        val s = service ?: return
        if (s.getState() != AudioPlayService.STARTED) {
            s.play()
        }
    }

    override fun pause() {
        val s = service ?: return
        if (s.getState() == AudioPlayService.STARTED) {
            s.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        service?.seek(positionMs)
    }

    override fun seekBy(deltaMs: Long) {
        val s = service ?: return
        val next = (s.getCurrentPosition().toLong() + deltaMs).coerceAtLeast(0L)
        s.seek(next)
    }

    override fun release() {
        if (isBound) {
            service?.onStateChange = null
            context.unbindService(serviceConnection)
            service = null
            serviceIntent?.let { context.stopService(it) }
            isBound = false
        }
    }
}
