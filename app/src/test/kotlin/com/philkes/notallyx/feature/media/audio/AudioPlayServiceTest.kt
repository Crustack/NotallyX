package com.philkes.notallyx.feature.media.audio

import android.content.Intent
import androidx.media3.session.MediaSessionService
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Audio
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class AudioPlayServiceTest {

    private lateinit var controller: ServiceController<AudioPlayService>
    private lateinit var service: AudioPlayService

    @Before
    fun setUp() {
        controller = Robolectric.buildService(AudioPlayService::class.java).create()
        service = controller.get()
    }

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertThat(service.getState()).isEqualTo(AudioPlayService.IDLE)
        assertThat(service.getCurrentPosition()).isEqualTo(0)
    }

    @Test
    fun testBindingReturnsLocalBinderForStandardIntent() {
        val binder = service.onBind(Intent())
        assertThat(binder).isInstanceOf(LocalBinder::class.java)
        @Suppress("UNCHECKED_CAST") val localBinder = binder as LocalBinder<AudioPlayService>
        assertThat(localBinder.getService()).isSameAs(service)
    }

    @Test
    fun testMediaSessionCreatedAndAccessible() {
        // MediaSessionService provides onGetSession for media controllers
        val binder = service.onBind(Intent(MediaSessionService.SERVICE_INTERFACE))
        // Binder for MediaSessionService interface should be non-null
        assertThat(binder).isNotNull
    }

    @Test
    fun testInitialiseWithValidFilePreparesPlayer() {
        val audioRoot =
            ApplicationProvider.getApplicationContext<android.app.Application>()
                .getCurrentAudioDirectory()
        File(audioRoot, "test_note.m4a").apply { writeBytes(ByteArray(100)) }

        val audio = Audio(name = "test_note.m4a", duration = 3000L, timestamp = 1000L)
        service.initialise(audio)

        assertThat(service.getState()).isIn(AudioPlayService.INITIALISED, AudioPlayService.PREPARED)
    }

    @Test
    fun testPlayAndSeekWithoutCrashing() {
        val audioRoot =
            ApplicationProvider.getApplicationContext<android.app.Application>()
                .getCurrentAudioDirectory()
        File(audioRoot, "test_audio.m4a").apply { writeBytes(ByteArray(100)) }

        val audio = Audio(name = "test_audio.m4a", duration = 3000L, timestamp = 1000L)
        service.initialise(audio)
        service.play()
        service.seek(500L)

        assertThat(service.getState()).isNotEqualTo(AudioPlayService.IDLE)
    }
}
