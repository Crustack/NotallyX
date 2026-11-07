package com.philkes.notallyx.feature.media.audio

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.navigation.NotallyNavKey
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayAudioRoute(
    key: NotallyNavKey.PlayAudio,
    onAudioDeleted: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayAudioViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key) { viewModel.onAction(PlayAudioAction.Init(key)) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.onAction(PlayAudioAction.ExportConfirmed(uri))
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is PlayAudioSideEffect.ShareAudio -> {
                    val intent =
                        Intent(Intent.ACTION_SEND)
                            .apply {
                                type = "audio/mp4"
                                putExtra(Intent.EXTRA_STREAM, effect.uri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            .wrapWithChooser(context)
                    context.startActivity(intent)
                }

                is PlayAudioSideEffect.LaunchExport -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = "audio/mp4"
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, effect.defaultName)
                            }
                            .wrapWithChooser(context)
                    exportLauncher.launch(intent)
                }

                is PlayAudioSideEffect.ShowToast -> {
                    context.showToast(effect.messageRes)
                }

                is PlayAudioSideEffect.NotifyAudioDeleted -> {
                    onAudioDeleted(effect.audioName)
                }

                is PlayAudioSideEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    PlayAudioScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

const val EXTRA_AUDIO = "notallyx.intent.extra.AUDIO"
