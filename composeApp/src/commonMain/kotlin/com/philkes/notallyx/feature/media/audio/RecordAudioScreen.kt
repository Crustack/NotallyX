package com.philkes.notallyx.feature.media.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.Res
import com.philkes.notallyx.back
import com.philkes.notallyx.close
import com.philkes.notallyx.core.common.extensions.formatElapsedTime
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.discard
import com.philkes.notallyx.pause
import com.philkes.notallyx.resume
import com.philkes.notallyx.save
import com.philkes.notallyx.save_recording
import com.philkes.notallyx.start
import com.philkes.notallyx.stop
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecordAudioScreen(
    state: RecordAudioUiState,
    onAction: (RecordAudioAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordAudioScreen(
        status = state.status,
        baseTime = state.baseTime,
        showSaveConfirmDialog = state.activeDialog is RecordAudioDialogState.ConfirmSave,
        onMainClick = { onAction(RecordAudioAction.MainClicked) },
        onStopClick = { onAction(RecordAudioAction.StopClicked) },
        onBackClick = { onAction(RecordAudioAction.BackClicked) },
        onDismissSaveConfirm = { onAction(RecordAudioAction.DismissDialog) },
        onSaveConfirm = { onAction(RecordAudioAction.SaveConfirmed) },
        onDiscardConfirm = { onAction(RecordAudioAction.DiscardConfirmed) },
        modifier = modifier,
    )
}

/** Stateless composable displaying audio recording controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAudioScreen(
    status: Status,
    baseTime: Long,
    modifier: Modifier = Modifier,
    showSaveConfirmDialog: Boolean = false,
    onMainClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismissSaveConfirm: () -> Unit = {},
    onSaveConfirm: () -> Unit = {},
    onDiscardConfirm: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            RecordingTimer(status = status, baseTime = baseTime)

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onStopClick,
                        enabled = status != Status.READY,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Text(text = stringResource(Res.string.stop))
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(onClick = onMainClick) {
                        Text(
                            text =
                                stringResource(
                                    when (status) {
                                        Status.READY -> Res.string.start
                                        Status.RECORDING -> Res.string.pause
                                        Status.PAUSED -> Res.string.resume
                                    }
                                )
                        )
                    }
                }
            }
        }

        if (showSaveConfirmDialog) {
            ConfirmDialog(
                message = stringResource(Res.string.save_recording),
                positiveButtonText = stringResource(Res.string.save),
                neutralButtonText = stringResource(Res.string.discard),
                onNeutralButtonClick = onDiscardConfirm,
                onDismissRequest = onDismissSaveConfirm,
                onConfirm = onSaveConfirm,
            )
        }
    }
}

@Composable
private fun RecordingTimer(
    status: Status,
    baseTime: Long,
    modifier: Modifier = Modifier,
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(status, baseTime) {
        if (status == Status.RECORDING) {
            val startMark = TimeSource.Monotonic.markNow()
            while (true) {
                elapsedTime = maxOf(0L, baseTime + startMark.elapsedNow().inWholeMilliseconds)
                delay(100)
            }
        } else if (status == Status.PAUSED) {
            elapsedTime = baseTime
        } else {
            elapsedTime = 0L
        }
    }

    val seconds = elapsedTime / 1000
    val formattedTime = formatElapsedTime(seconds)

    Text(
        text = formattedTime,
        style =
            MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 40.sp,
            ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RecordingTimerPreview() {
    NotallyXTheme {
        RecordingTimer(
            status = Status.RECORDING,
            baseTime = 5000L,
        )
    }
}

@Preview
@Composable
private fun RecordAudioScreenPreview() {
    NotallyXTheme {
        RecordAudioScreen(
            status = Status.RECORDING,
            baseTime = 12000L,
        )
    }
}
