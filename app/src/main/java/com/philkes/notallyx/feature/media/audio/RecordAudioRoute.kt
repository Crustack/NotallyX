package com.philkes.notallyx.feature.media.audio

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philkes.notallyx.core.common.extensions.getTempAudioFile

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun RecordAudioRoute(
    onRecorded: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { requireNotNull(context.findActivity()) }

    var service by remember { mutableStateOf<AudioRecordService?>(null) }
    var status by remember { mutableStateOf(Status.READY) }
    var baseTime by remember { mutableLongStateOf(0L) }
    var showSaveConfirm by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val intent = Intent(activity, AudioRecordService::class.java)
        activity.startService(intent)

        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val s = (binder as LocalBinder<AudioRecordService>).getService()
                    service = s
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                }
            }

        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            service?.let { activity.unbindService(connection) }
            service = null
            activity.stopService(intent)
        }
    }

    LaunchedEffect(service) {
        val s = service ?: return@LaunchedEffect
        s.status.collect { currentStatus ->
            status = currentStatus
            baseTime = s.getBase()
        }
    }

    val handleBack: () -> Unit = {
        val s = service
        if (s == null || status == Status.READY) {
            onBackClick()
        } else {
            showSaveConfirm = true
        }
    }

    BackHandler(onBack = handleBack)

    fun stopRecording(s: AudioRecordService) {
        s.stop()
        onRecorded()
        onBackClick()
    }

    fun discard(s: AudioRecordService) {
        s.stop()
        activity.getTempAudioFile().delete()
        onBackClick()
    }

    RecordAudioScreen(
        status = status,
        baseTime = baseTime,
        showSaveConfirmDialog = showSaveConfirm,
        onMainClick = {
            val s = service ?: return@RecordAudioScreen
            when (s.status.value) {
                Status.PAUSED -> s.resume()
                Status.READY -> s.start()
                Status.RECORDING -> s.pause()
            }
        },
        onStopClick = {
            val s = service ?: return@RecordAudioScreen
            stopRecording(s)
        },
        onBackClick = handleBack,
        onDismissSaveConfirm = { showSaveConfirm = false },
        onSaveConfirm = {
            showSaveConfirm = false
            service?.let { stopRecording(it) }
        },
        onDiscardConfirm = {
            showSaveConfirm = false
            service?.let { discard(it) }
        },
        modifier = modifier,
    )
}
