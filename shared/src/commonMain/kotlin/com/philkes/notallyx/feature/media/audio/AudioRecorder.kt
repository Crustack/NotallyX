package com.philkes.notallyx.feature.media.audio

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {
    val isSupported: Boolean
    val status: StateFlow<Status>
    val isRecording: StateFlow<Boolean>
    val elapsedDurationMs: StateFlow<Long>

    fun startRecording()

    fun pauseRecording()

    fun resumeRecording()

    fun stopRecording(): File?

    fun release()
}

class NoOpAudioRecorder : AudioRecorder {
    override val isSupported: Boolean = false
    override val status: StateFlow<Status> = MutableStateFlow(Status.READY)
    override val isRecording: StateFlow<Boolean> = MutableStateFlow(false)
    override val elapsedDurationMs: StateFlow<Long> = MutableStateFlow(0L)

    override fun startRecording() {}

    override fun pauseRecording() {}

    override fun resumeRecording() {}

    override fun stopRecording(): File? = null

    override fun release() {}
}
