package com.philkes.notallyx.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.feature.media.audio.PlayAudioDialogState
import com.philkes.notallyx.feature.media.audio.PlayAudioScreen
import com.philkes.notallyx.feature.media.audio.PlayAudioUiState
import com.philkes.notallyx.feature.media.audio.RecordAudioDialogState
import com.philkes.notallyx.feature.media.audio.RecordAudioScreen
import com.philkes.notallyx.feature.media.audio.RecordAudioUiState
import com.philkes.notallyx.feature.media.audio.Status

private val SAMPLE_AUDIO =
    Audio(
        name = "Meeting Notes Recording.m4a",
        duration = 185000L,
        timestamp = 1700000000000L,
    )

@Preview(showBackground = true)
@PreviewTest
@Composable
fun PlayAudioScreen_Playing_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        PlayAudioScreen(
            state =
                PlayAudioUiState(
                    audio = SAMPLE_AUDIO,
                    currentPosition = 64000L,
                    duration = 185000L,
                    isPlaying = true,
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun PlayAudioScreen_Paused_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        PlayAudioScreen(
            state =
                PlayAudioUiState(
                    audio = SAMPLE_AUDIO,
                    currentPosition = 120000L,
                    duration = 185000L,
                    isPlaying = false,
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun PlayAudioScreen_DeleteConfirm() {
    NotallyXTheme(theme = Theme.LIGHT) {
        PlayAudioScreen(
            state =
                PlayAudioUiState(
                    audio = SAMPLE_AUDIO,
                    currentPosition = 30000L,
                    duration = 185000L,
                    isPlaying = false,
                    activeDialog = PlayAudioDialogState.ConfirmDelete,
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RecordAudioScreen_Ready_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RecordAudioScreen(
            state = RecordAudioUiState(status = Status.READY, baseTime = 0L),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RecordAudioScreen_Recording_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        RecordAudioScreen(
            state = RecordAudioUiState(status = Status.RECORDING, baseTime = 45000L),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RecordAudioScreen_Paused() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RecordAudioScreen(
            state = RecordAudioUiState(status = Status.PAUSED, baseTime = 72000L),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RecordAudioScreen_ConfirmSave() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RecordAudioScreen(
            state =
                RecordAudioUiState(
                    status = Status.PAUSED,
                    baseTime = 30000L,
                    activeDialog = RecordAudioDialogState.ConfirmSave,
                ),
            onAction = {},
        )
    }
}
