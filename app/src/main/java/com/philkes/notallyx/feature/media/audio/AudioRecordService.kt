package com.philkes.notallyx.feature.media.audio

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.feature.media.audio.Status.PAUSED
import com.philkes.notallyx.feature.media.audio.Status.RECORDING
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRecordService : Service() {

    private val _status = MutableStateFlow(Status.READY)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var lastStart = 0L
    private var audioDuration = 0L

    private var recorder: MediaRecorder? = null

    override fun onCreate() {
        super.onCreate()
        prepareRecorder()
    }

    private fun prepareRecorder() {
        try {
            val r =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION") MediaRecorder()
                }

            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                val output = this@AudioRecordService.getTempAudioFile()
                output.parentFile?.mkdirs()
                setOutputFile(output.path)
                prepare()
            }
            recorder = r
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = LocalBinder(this)

    fun start() {
        try {
            if (recorder == null) {
                prepareRecorder()
            }
            audioDuration = 0L
            recorder?.start()
            _status.value = RECORDING
            lastStart = SystemClock.elapsedRealtime()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            recorder?.resume()
            _status.value = RECORDING
            lastStart = SystemClock.elapsedRealtime()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            recorder?.pause()
            _status.value = PAUSED
            if (lastStart != 0L) {
                audioDuration += SystemClock.elapsedRealtime() - lastStart
            }
            lastStart = 0L
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            if (_status.value == RECORDING || _status.value == PAUSED) {
                recorder?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                recorder?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            recorder = null
            _status.value = Status.READY
            audioDuration = 0L
            lastStart = 0L
            stopSelf()
        }
    }

    fun getElapsedDurationMs(): Long {
        return if (_status.value == RECORDING && lastStart != 0L) {
            audioDuration + (SystemClock.elapsedRealtime() - lastStart)
        } else {
            audioDuration
        }
    }

    fun getBase(): Long = getElapsedDurationMs()
}
