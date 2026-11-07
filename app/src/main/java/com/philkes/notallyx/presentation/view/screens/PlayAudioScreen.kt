package com.philkes.notallyx.presentation.view.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing activity_play_audio.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayAudioScreen(
    currentPosition: Long,
    duration: Long?,
    modifier: Modifier = Modifier,
    titleText: String = stringResource(R.string.edit_reminders),
    isPlaying: Boolean = false,
    errorMessage: String? = null,
    onPlayPauseClick: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(currentPosition.toFloat()) }

    LaunchedEffect(currentPosition, isSeeking) {
        if (!isSeeking) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    val maxDuration = duration?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = titleText) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = stringResource(R.string.share),
                        )
                    }
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.save),
                            contentDescription = stringResource(R.string.save_to_device),
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.delete),
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = sliderPosition.coerceIn(0f, maxDuration),
                            onValueChange = {
                                isSeeking = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                onSeek(sliderPosition.toLong())
                            },
                            valueRange = 0f..maxDuration,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Text(
                                text =
                                    DateUtils.formatElapsedTime((sliderPosition / 1000).toLong()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text =
                                    duration?.let { DateUtils.formatElapsedTime(it / 1000) } ?: "-",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.padding(bottom = 32.dp),
                    ) {
                        Text(
                            text = stringResource(if (isPlaying) R.string.pause else R.string.play)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayAudioScreenPreview() {
    NotallyXTheme {
        PlayAudioScreen(
            currentPosition = 30000L,
            duration = 120000L,
            isPlaying = false,
        )
    }
}
