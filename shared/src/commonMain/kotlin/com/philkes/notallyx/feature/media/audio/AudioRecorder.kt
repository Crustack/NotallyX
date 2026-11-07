package com.philkes.notallyx.feature.media.audio

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {
    val isSupported: Boolean
    val isRecording: StateFlow<Boolean>
    val elapsedDurationMs: StateFlow<Long>

    fun startRecording()

    fun stopRecording(): File?

    fun release()
}

class NoOpAudioRecorder : AudioRecorder {
    override val isSupported: Boolean = false
    override val isRecording: StateFlow<Boolean> = MutableStateFlow(false)
    override val elapsedDurationMs: StateFlow<Long> = MutableStateFlow(0L)

    override fun startRecording() {}

    override fun stopRecording(): File? = null

    override fun release() {}
}
