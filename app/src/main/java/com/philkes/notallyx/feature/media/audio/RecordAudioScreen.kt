package com.philkes.notallyx.feature.media.audio

import android.os.SystemClock
import android.text.format.DateUtils
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import kotlinx.coroutines.delay

/** Composable replacing activity_record_audio.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordAudioScreen(
    status: Status,
    baseTime: Long,
    modifier: Modifier = Modifier,
    onMainClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
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
                                containerColor = colorResource(R.color.stop_background),
                                contentColor = colorResource(R.color.stop_text),
                            ),
                    ) {
                        Text(text = stringResource(R.string.stop))
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
                                        Status.READY -> R.string.start
                                        Status.RECORDING -> R.string.pause
                                        Status.PAUSED -> R.string.resume
                                    }
                                )
                        )
                    }
                }
            }
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
            while (true) {
                val now = SystemClock.elapsedRealtime()
                elapsedTime = maxOf(0L, now - baseTime)
                delay(100)
            }
        } else if (status == Status.PAUSED) {
            val now = SystemClock.elapsedRealtime()
            elapsedTime = maxOf(0L, now - baseTime)
        } else {
            elapsedTime = 0L
        }
    }

    val seconds = elapsedTime / 1000
    val formattedTime = DateUtils.formatElapsedTime(seconds)

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

@Preview(showBackground = true)
@Composable
private fun RecordAudioScreenPreview() {
    NotallyXTheme {
        RecordAudioScreen(
            status = Status.RECORDING,
            baseTime = SystemClock.elapsedRealtime() - 12000L,
        )
    }
}
