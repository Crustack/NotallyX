package com.philkes.notallyx.feature.media.image

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
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
fun ViewImageRoute(
    key: NotallyNavKey.ViewImage,
    onDeletedImages: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewImageViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key) { viewModel.onAction(ViewImageAction.Init(key)) }

    BackHandler { viewModel.onAction(ViewImageAction.BackClicked) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.onAction(ViewImageAction.ExportConfirmed(uri))
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ViewImageSideEffect.ShareImage -> {
                    val intent =
                        Intent(Intent.ACTION_SEND)
                            .apply {
                                type = effect.mimeType
                                putExtra(Intent.EXTRA_STREAM, effect.uri)
                                clipData = ClipData.newRawUri(null, effect.uri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            .wrapWithChooser(context)
                    context.startActivity(intent)
                }

                is ViewImageSideEffect.LaunchExport -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = effect.mimeType
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, effect.defaultName)
                            }
                            .wrapWithChooser(context)
                    exportLauncher.launch(intent)
                }

                is ViewImageSideEffect.ShowToast -> {
                    context.showToast(effect.messageRes)
                }

                is ViewImageSideEffect.NotifyImagesDeleted -> {
                    onDeletedImages(effect.deletedImages)
                }

                is ViewImageSideEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    ViewImageScreen(
        state = uiState.copy(initialPage = key.initialIndex),
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

const val EXTRA_POSITION = "notallyx.intent.extra.POSITION"
const val CURRENT_IMAGE = "CURRENT_IMAGE"
const val EXTRA_DELETED_IMAGES = "notallyx.intent.extra.DELETED_IMAGES"
