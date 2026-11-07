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
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.SubcomposeAsyncImage
import com.philkes.notallyx.Res
import com.philkes.notallyx.back
import com.philkes.notallyx.close
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.delete
import com.philkes.notallyx.delete_image_forever
import com.philkes.notallyx.feature.editor.screens.ImagePreviewCard
import com.philkes.notallyx.save
import com.philkes.notallyx.save_to_device
import com.philkes.notallyx.share
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ViewImageScreen(
    state: ViewImageUiState,
    onAction: (ViewImageAction) -> Unit,
    modifier: Modifier = Modifier,
    resolveImageModel: (FileAttachment) -> Any? = { it.localName },
) {
    val deletePosition = (state.activeDialog as? ViewImageDialogState.ConfirmDelete)?.position
    ViewImageScreen(
        images = state.images,
        initialPage = state.initialPage,
        imageToDeletePosition = deletePosition,
        resolveImageModel = resolveImageModel,
        onBackClick = { onAction(ViewImageAction.BackClicked) },
        onShareClick = { onAction(ViewImageAction.ShareClicked(it)) },
        onSaveClick = { onAction(ViewImageAction.SaveClicked(it)) },
        onDeleteClick = { onAction(ViewImageAction.DeleteClicked(it)) },
        onDismissDeleteConfirm = { onAction(ViewImageAction.DismissDialog) },
        onConfirmDelete = { onAction(ViewImageAction.ConfirmDelete(it)) },
        modifier = modifier,
    )
}

/** Stateless composable displaying an image viewer with pager support. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewImageScreen(
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
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    if (images.isNotEmpty()) {
                        IconButton(onClick = { onShareClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(Res.drawable.share),
                                contentDescription = stringResource(Res.string.share),
                            )
                        }
                        IconButton(onClick = { onSaveClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(Res.drawable.save),
                                contentDescription = stringResource(Res.string.save_to_device),
                            )
                        }
                        IconButton(onClick = { onDeleteClick(currentPosition) }) {
                            Icon(
                                painter = painterResource(Res.drawable.delete),
                                contentDescription = stringResource(Res.string.delete),
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
                message = stringResource(Res.string.delete_image_forever),
                positiveButtonText = stringResource(Res.string.delete),
                onDismissRequest = onDismissDeleteConfirm,
                onConfirm = { onConfirmDelete(imageToDeletePosition) },
            )
        }
    }
}

@Preview
@Composable
private fun ViewImageScreenPreview() {
    NotallyXTheme { ViewImageScreen(images = emptyList()) }
}
