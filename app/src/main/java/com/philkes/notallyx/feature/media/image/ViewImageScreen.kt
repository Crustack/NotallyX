package com.philkes.notallyx.feature.media.image

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import coil3.compose.SubcomposeAsyncImage
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.editor.screens.ImagePreviewCard

@Composable
internal fun ViewImageScreen(
    state: ViewImageUiState,
    onAction: (ViewImageAction) -> Unit,
    modifier: Modifier = Modifier,
    resolveImageModel: ((FileAttachment) -> Any?)? = null,
) {
    val context = LocalContext.current
    val deletePosition = (state.activeDialog as? ViewImageDialogState.ConfirmDelete)?.position
    ViewImageScreen(
        images = state.images,
        initialPage = state.initialPage,
        imageToDeletePosition = deletePosition,
        resolveImageModel =
            resolveImageModel
                ?: { image ->
                    if (
                        image.localName.startsWith("content://") ||
                            image.localName.startsWith("file://")
                    ) {
                        image.localName.toUri()
                    } else {
                        context.applicationContext.resolveAttachmentFile(
                            SUBFOLDER_IMAGES,
                            image.localName,
                        )
                    }
                },
        onBackClick = { onAction(ViewImageAction.BackClicked) },
        onShareClick = { onAction(ViewImageAction.ShareClicked(it)) },
        onSaveClick = { onAction(ViewImageAction.SaveClicked(it)) },
        onDeleteClick = { onAction(ViewImageAction.DeleteClicked(it)) },
        onDismissDeleteConfirm = { onAction(ViewImageAction.DismissDialog) },
        onConfirmDelete = { onAction(ViewImageAction.ConfirmDelete(it)) },
        modifier = modifier,
    )
}

/** Composable replacing activity_view_image.xml layout. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ViewImageScreen(
    images: List<FileAttachment>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    imageToDeletePosition: Int? = null,
    resolveImageModel: (FileAttachment) -> Any? = { null },
    onBackClick: () -> Unit = {},
    onShareClick: (Int) -> Unit = {},
    onSaveClick: (Int) -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onConfirmDelete: (Int) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })

    var hasScrolledToInitialPage by rememberSaveable(initialPage) { mutableStateOf(false) }

    LaunchedEffect(images.size, initialPage) {
        if (images.isNotEmpty() && !hasScrolledToInitialPage && initialPage in images.indices) {
            pagerState.scrollToPage(initialPage)
            hasScrolledToInitialPage = true
        } else if (images.isNotEmpty() && pagerState.currentPage >= images.size) {
            pagerState.scrollToPage(images.size - 1)
        }
    }

    val currentPosition =
        if (images.isNotEmpty()) pagerState.currentPage.coerceIn(0, images.size - 1) else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            if (images.isNotEmpty()) "${currentPosition + 1} / ${images.size}"
                            else ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (images.isNotEmpty()) {
                        IconButton(onClick = { onShareClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.share),
                                contentDescription = stringResource(R.string.share),
                            )
                        }
                        IconButton(onClick = { onSaveClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.save),
                                contentDescription = stringResource(R.string.save_to_device),
                            )
                        }
                        IconButton(onClick = { onDeleteClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.delete),
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (images.isEmpty()) {
                ImagePreviewCard()
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val image = images[page]
                    SubcomposeAsyncImage(
                        model = resolveImageModel(image),
                        contentDescription = image.originalName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        error = { ImagePreviewCard() },
                    )
                }
            }
        }

        if (imageToDeletePosition != null) {
            ConfirmDialog(
                message = stringResource(R.string.delete_image_forever),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = onDismissDeleteConfirm,
                onConfirm = { onConfirmDelete(imageToDeletePosition) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewImageScreenPreview() {
    NotallyXTheme { ViewImageScreen(images = emptyList()) }
}
