package com.philkes.notallyx.feature.media.audio

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.data.model.Audio
import java.io.File

class AudioPlayService : MediaSessionService() {

    private var state = IDLE
    private var stateBeforeSeeking = -1
    private var errorType = 0
    private var errorCode = 0
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    var onStateChange: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes =
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build()

        player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(
                        object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        if (state == INITIALISED) {
                                            setState(if (playWhenReady) STARTED else PREPARED)
                                        } else if (state == SEEKING) {
                                            setState(
                                                if (playWhenReady) STARTED
                                                else if (stateBeforeSeeking != -1)
                                                    stateBeforeSeeking
                                                else PREPARED
                                            )
                                        }
                                    }
                                    Player.STATE_ENDED -> {
                                        setState(COMPLETED)
                                    }
                                    Player.STATE_IDLE -> {
                                        // Idle
                                    }
                                    Player.STATE_BUFFERING -> {
                                        // Buffering
                                    }
                                }
                            }

                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                if (isPlaying) {
                                    setState(STARTED)
                                } else if (state == STARTED) {
                                    setState(PAUSED)
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                errorType = error.errorCode
                                errorCode = error.errorCodeName.hashCode()
                                setState(ERROR)
                            }
                        }
                    )
                }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE) {
            return super.onBind(intent)
        }
        return LocalBinder(this)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    fun initialise(audio: Audio) {
        if (state == IDLE) {
            val audioRoot = application.getCurrentAudioDirectory()
            if (audioRoot != null) {
                try {
                    val file = File(audioRoot, audio.name)
                    val metadata =
                        MediaMetadata.Builder()
                            .setTitle(audio.name)
                            .setDisplayTitle(audio.name)
                            .build()
                    val mediaItem =
                        MediaItem.Builder()
                            .setUri(Uri.fromFile(file))
                            .setMediaMetadata(metadata)
                            .build()
                    player.setMediaItem(mediaItem)
                    setState(INITIALISED)
                    player.prepare()
                } catch (exception: Exception) {
                    setIOError()
                    application.log(TAG, throwable = exception)
                }
            } else setIOError()
        }
    }

    fun play() {
        when (state) {
            INITIALISED,
            PREPARED,
            PAUSED -> {
                player.play()
                setState(STARTED)
            }
            COMPLETED -> {
                player.seekTo(0)
                player.play()
                setState(STARTED)
            }
            STARTED -> {
                player.pause()
                setState(PAUSED)
            }
        }
    }

    fun seek(milliseconds: Long) {
        if (state == PREPARED || state == STARTED || state == PAUSED || state == COMPLETED) {
            stateBeforeSeeking = state
            player.seekTo(milliseconds)
            setState(SEEKING)
        }
    }

    fun getState(): Int = state

    fun getErrorType(): Int = errorType

    fun getErrorCode(): Int = errorCode

    fun getCurrentPosition(): Int =
        if (state != IDLE && state != ERROR) player.currentPosition.toInt() else 0

    private fun setState(state: Int) {
        this.state = state
        onStateChange?.invoke()
    }

    private fun setIOError() {
        errorCode = 0
        errorType = 15
        setState(ERROR)
    }

    companion object {
        private const val TAG = "AudioPlayService"
        const val IDLE = 0
        const val INITIALISED = 1
        const val PREPARED = 2
        const val STARTED = 3
        const val PAUSED = 4
        const val SEEKING = 5
        const val COMPLETED = 6
        const val ERROR = 7
    }
}
