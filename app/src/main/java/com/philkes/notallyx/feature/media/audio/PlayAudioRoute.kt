package com.philkes.notallyx.feature.media.audio

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun PlayAudioRoute(
    key: NotallyNavKey.PlayAudio,
    onAudioDeleted: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { requireNotNull(context.findActivity()) }
    val scope = rememberCoroutineScope()

    val audio =
        remember(key) {
            Audio(name = key.audioName, duration = key.duration, timestamp = key.timestamp)
        }

    var service by remember { mutableStateOf<AudioPlayService?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun updateUI(s: AudioPlayService) {
        currentPosition = s.getCurrentPosition().toLong()
        when (s.getState()) {
            AudioPlayService.PREPARED,
            AudioPlayService.PAUSED,
            AudioPlayService.COMPLETED -> {
                isPlaying = false
                errorMessage = null
            }
            AudioPlayService.STARTED -> {
                isPlaying = true
                errorMessage = null
            }
            AudioPlayService.ERROR -> {
                isPlaying = false
                errorMessage =
                    activity.getString(
                        R.string.something_went_wrong_audio,
                        s.getErrorType(),
                        s.getErrorCode(),
                    )
            }
        }
    }

    DisposableEffect(audio) {
        val intent = Intent(activity, AudioPlayService::class.java)
        activity.startService(intent)

        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val s = (binder as LocalBinder<AudioPlayService>).getService()
                    s.initialise(audio)
                    s.onStateChange = { updateUI(s) }
                    service = s
                    updateUI(s)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                }
            }

        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            service?.let { s ->
                activity.unbindService(connection)
                s.onStateChange = null
            }
            service = null
            activity.stopService(intent)
        }
    }

    LaunchedEffect(service) {
        while (isActive) {
            service?.let { s ->
                if (s.getState() == AudioPlayService.STARTED) {
                    currentPosition = s.getCurrentPosition().toLong()
                }
            }
            delay(100)
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val audioRoot = activity.application.getCurrentAudioDirectory()
                    val file = if (audioRoot != null) File(audioRoot, audio.name) else null
                    if (file != null && file.exists()) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                activity.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                                    FileOutputStream(pfd.fileDescriptor).use { output ->
                                        FileInputStream(file).use { input -> input.copyTo(output) }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                            activity,
                                            R.string.saved_to_device,
                                            Toast.LENGTH_LONG,
                                        )
                                        .show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

    fun share() {
        val audioRoot = activity.application.getCurrentAudioDirectory()
        val file = if (audioRoot != null) File(audioRoot, audio.name) else null
        if (file != null && file.exists()) {
            val uri = activity.getUriForFile(file)
            val intent =
                Intent(Intent.ACTION_SEND)
                    .apply {
                        type = "audio/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    .wrapWithChooser(activity)
            activity.startActivity(intent)
        }
    }

    fun saveToDevice() {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
        val defaultName = "${formatter.format(audio.timestamp)}.m4a"
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .apply {
                    type = "audio/mp4"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, defaultName)
                }
                .wrapWithChooser(activity)
        exportLauncher.launch(intent)
    }

    PlayAudioScreen(
        currentPosition = currentPosition,
        duration = audio.duration,
        isPlaying = isPlaying,
        errorMessage = errorMessage,
        showDeleteConfirmDialog = showDeleteConfirm,
        actions =
            PlayAudioActions(
                onPlayPauseClick = { service?.play() },
                onSeek = { ms -> service?.seek(ms) },
                onBackClick = onBackClick,
                onShareClick = { share() },
                onSaveClick = { saveToDevice() },
                onDeleteClick = { showDeleteConfirm = true },
                onDismissDeleteConfirm = { showDeleteConfirm = false },
                onConfirmDelete = {
                    showDeleteConfirm = false
                    onAudioDeleted(audio.name)
                    onBackClick()
                },
            ),
        modifier = modifier,
    )
}

const val EXTRA_AUDIO = "notallyx.intent.extra.AUDIO"
