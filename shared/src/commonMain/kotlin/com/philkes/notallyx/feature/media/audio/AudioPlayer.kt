package com.philkes.notallyx.feature.media.audio

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val errorCode: StateFlow<Int?>

    suspend fun loadAudio(file: File)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun seekBy(deltaMs: Long)

    fun release()
}

class NoOpAudioPlayer : AudioPlayer {
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentPositionMs: StateFlow<Long> = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = MutableStateFlow(0L)
    override val errorCode: StateFlow<Int?> = MutableStateFlow(null)

    override suspend fun loadAudio(file: File) {}

    override fun play() {}

    override fun pause() {}

    override fun seekTo(positionMs: Long) {}

    override fun seekBy(deltaMs: Long) {}

    override fun release() {}
}
