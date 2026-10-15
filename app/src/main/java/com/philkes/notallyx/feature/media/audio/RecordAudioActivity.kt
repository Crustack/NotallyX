package com.philkes.notallyx.feature.media.audio

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

class RecordAudioActivity : LockedActivity<ComposeBinding>() {

    private var service: AudioRecordService? = null
    private lateinit var connection: ServiceConnection
    private lateinit var cancelRecordCallback: OnBackPressedCallback

    private val statusState = mutableStateOf(Status.READY)
    private val baseTimeState = mutableLongStateOf(0L)
    private val showSaveConfirmState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                RecordAudioScreen(
                    status = statusState.value,
                    baseTime = baseTimeState.longValue,
                    showSaveConfirmDialog = showSaveConfirmState.value,
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
                    onBackClick = { onBackPressedDispatcher.onBackPressed() },
                    onDismissSaveConfirm = { showSaveConfirmState.value = false },
                    onSaveConfirm = {
                        showSaveConfirmState.value = false
                        service?.let { stopRecording(it) }
                    },
                    onDiscardConfirm = {
                        showSaveConfirmState.value = false
                        service?.let { discard(it) }
                    },
                )
            }
        }

        val intent = Intent(this, AudioRecordService::class.java)
        startService(intent)

        connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val s = (binder as LocalBinder<AudioRecordService>).getService()
                    service = s
                    s.status.collectIn(this@RecordAudioActivity) { status ->
                        statusState.value = status
                        baseTimeState.longValue = s.getBase()
                        cancelRecordCallback.isEnabled = status != Status.READY
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

        bindService(intent, connection, BIND_AUTO_CREATE)

        cancelRecordCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val s = service
                    if (s == null || statusState.value == Status.READY) {
                        finish()
                    } else {
                        showSaveConfirmState.value = true
                    }
                }
            }
        onBackPressedDispatcher.addCallback(cancelRecordCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        service?.let {
            unbindService(connection)
            service = null
        }
        if (isFinishing) {
            val intent = Intent(this, AudioRecordService::class.java)
            stopService(intent)
        }
    }

    private fun discard(service: AudioRecordService) {
        service.stop()
        getTempAudioFile().delete()
        finish()
    }

    private fun stopRecording(service: AudioRecordService) {
        service.stop()
        setResult(RESULT_OK)
        finish()
    }
}
