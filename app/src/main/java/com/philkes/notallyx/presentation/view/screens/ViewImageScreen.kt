package com.philkes.notallyx.presentation.view.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.ImagePreviewCard

/** Composable replacing activity_view_image.xml layout. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewImageScreen(
    images: List<FileAttachment>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onBackClick: () -> Unit = {},
    onShareClick: (Int) -> Unit = {},
    onSaveClick: (Int) -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { images.size })
    val currentPosition = if (images.isNotEmpty()) pagerState.currentPage else 0

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
                ) {
                    ImagePreviewCard()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewImageScreenPreview() {
    NotallyXTheme { ViewImageScreen(images = emptyList()) }
}
