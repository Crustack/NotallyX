package com.philkes.notallyx.feature.media.audio

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayAudioActivity : LockedActivity<ComposeBinding>() {

    private var service: AudioPlayService? = null
    private lateinit var connection: ServiceConnection
    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var audio: Audio

    private val currentPositionState = mutableLongStateOf(0L)
    private val isPlayingState = mutableStateOf(false)
    private val errorMessageState = mutableStateOf<String?>(null)
    private val showDeleteConfirmState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        audio =
            requireNotNull(
                intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_AUDIO, Audio::class.java) },
                { "PlayAudioActivity intent has no '$EXTRA_AUDIO' extra" },
            )

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                PlayAudioScreen(
                    currentPosition = currentPositionState.longValue,
                    duration = audio.duration,
                    isPlaying = isPlayingState.value,
                    errorMessage = errorMessageState.value,
                    showDeleteConfirmDialog = showDeleteConfirmState.value,
                    actions =
                        PlayAudioActions(
                            onPlayPauseClick = { service?.play() },
                            onSeek = { ms -> service?.seek(ms) },
                            onBackClick = { onBackPressedDispatcher.onBackPressed() },
                            onShareClick = { share() },
                            onSaveClick = { saveToDevice() },
                            onDeleteClick = { showDeleteConfirmState.value = true },
                            onDismissDeleteConfirm = { showDeleteConfirmState.value = false },
                            onConfirmDelete = {
                                showDeleteConfirmState.value = false
                                val intent = Intent()
                                intent.putExtra(EXTRA_AUDIO, audio)
                                setResult(RESULT_OK, intent)
                                finish()
                            },
                        ),
                )
            }
        }

        val intent = Intent(this, AudioPlayService::class.java)
        startService(intent)

        connection =
            object : ServiceConnection {

                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val service = (binder as LocalBinder<AudioPlayService>).getService()
                    service.initialise(audio)
                    service.onStateChange = { updateUI(service) }
                    this@PlayAudioActivity.service = service
                    updateUI(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

        bindService(intent, connection, BIND_AUTO_CREATE)

        lifecycleScope.launch {
            while (isActive) {
                service?.let { s ->
                    if (s.getState() == AudioPlayService.STARTED) {
                        currentPositionState.longValue = s.getCurrentPosition().toLong()
                    }
                }
                delay(100)
            }
        }

        exportFileActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri -> writeAudioToUri(uri) }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (service != null) {
            unbindService(connection)
            requireNotNull(service, { "service is null" }).onStateChange = null
            service = null
        }
        if (isFinishing) {
            val intent = Intent(this, AudioPlayService::class.java)
            stopService(intent)
        }
    }

    private fun share() {
        val audioRoot = application.getCurrentAudioDirectory()
        val file = if (audioRoot != null) File(audioRoot, audio.name) else null
        if (file != null && file.exists()) {
            val uri = getUriForFile(file)
            val intent =
                Intent(Intent.ACTION_SEND)
                    .apply {
                        type = "audio/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    }
                    .wrapWithChooser(this@PlayAudioActivity)
            startActivity(intent)
        }
    }

    private fun saveToDevice() {
        val audioRoot = application.getCurrentAudioDirectory()
        val file = if (audioRoot != null) File(audioRoot, audio.name) else null
        if (file != null && file.exists()) {
            val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .apply {
                        type = "audio/mp4"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    .wrapWithChooser(this)

            val formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
            val title = formatter.format(audio.timestamp)

            intent.putExtra(Intent.EXTRA_TITLE, title)
            exportFileActivityResultLauncher.launch(intent)
        }
    }

    private fun writeAudioToUri(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val audioRoot = application.getCurrentAudioDirectory()
                val file = if (audioRoot != null) File(audioRoot, audio.name) else null
                if (file != null && file.exists()) {
                    val output = contentResolver.openOutputStream(uri) as FileOutputStream
                    output.channel.truncate(0)
                    val input = FileInputStream(file)
                    input.copyTo(output)
                    input.close()
                    output.close()
                }
            }
            Toast.makeText(this@PlayAudioActivity, R.string.saved_to_device, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun updateUI(service: AudioPlayService) {
        currentPositionState.longValue = service.getCurrentPosition().toLong()
        when (service.getState()) {
            AudioPlayService.PREPARED,
            AudioPlayService.PAUSED,
            AudioPlayService.COMPLETED -> {
                isPlayingState.value = false
                errorMessageState.value = null
            }
            AudioPlayService.STARTED -> {
                isPlayingState.value = true
                errorMessageState.value = null
            }
            AudioPlayService.ERROR -> {
                isPlayingState.value = false
                errorMessageState.value =
                    getString(
                        R.string.something_went_wrong_audio,
                        service.getErrorType(),
                        service.getErrorCode(),
                    )
            }
        }
    }

    companion object {
        const val EXTRA_AUDIO = "notallyx.intent.extra.AUDIO"
    }
}
