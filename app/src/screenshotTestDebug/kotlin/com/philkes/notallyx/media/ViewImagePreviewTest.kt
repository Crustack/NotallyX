package com.philkes.notallyx.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.media.image.ViewImageDialogState
import com.philkes.notallyx.feature.media.image.ViewImageScreen
import com.philkes.notallyx.feature.media.image.ViewImageUiState

private val SAMPLE_IMAGE_1 =
    FileAttachment(
        localName = "sample_image_1.png",
        originalName = "Architecture_Diagram.png",
        mimeType = "image/png",
    )

private val SAMPLE_IMAGE_2 =
    FileAttachment(
        localName = "sample_image_2.png",
        originalName = "Wireframe_Mockup.png",
        mimeType = "image/png",
    )

private val SAMPLE_IMAGE_3 =
    FileAttachment(
        localName = "sample_image_3.png",
        originalName = "Screenshot_Result.png",
        mimeType = "image/png",
    )

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ViewImageScreen_Empty_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ViewImageScreen(
            state = ViewImageUiState(images = emptyList()),
            onAction = {},
            resolveImageModel = { null },
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ViewImageScreen_WithImage_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ViewImageScreen(
            state = ViewImageUiState(images = listOf(SAMPLE_IMAGE_1), initialPage = 0),
            onAction = {},
            resolveImageModel = { null },
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ViewImageScreen_MultipleImages_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ViewImageScreen(
            state =
                ViewImageUiState(
                    images = listOf(SAMPLE_IMAGE_1, SAMPLE_IMAGE_2, SAMPLE_IMAGE_3),
                    initialPage = 1,
                ),
            onAction = {},
            resolveImageModel = { null },
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ViewImageScreen_DeleteConfirm() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ViewImageScreen(
            state =
                ViewImageUiState(
                    images = listOf(SAMPLE_IMAGE_1),
                    initialPage = 0,
                    activeDialog = ViewImageDialogState.ConfirmDelete(position = 0),
                ),
            onAction = {},
            resolveImageModel = { null },
        )
    }
}
