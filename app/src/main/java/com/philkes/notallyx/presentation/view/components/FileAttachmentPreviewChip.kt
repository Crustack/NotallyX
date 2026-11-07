package com.philkes.notallyx.presentation.view.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing recycler_preview_file.xml layout. */
@Composable
fun FileAttachmentPreviewChip(
    fileName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = fileName.ifEmpty { stringResource(R.string.cant_load_file) },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.text_file),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = modifier.padding(end = 6.dp),
    )
}

/** Composable replacing recycler_preview_image.xml layout. */
@Composable
fun ImagePreviewCard(
    modifier: Modifier = Modifier,
    errorMessage: String = stringResource(R.string.cant_load_image),
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FileAttachmentPreviewChipPreview() {
    NotallyXTheme { FileAttachmentPreviewChip(fileName = "document.pdf") }
}

@Preview(showBackground = true)
@Composable
private fun ImagePreviewCardPreview() {
    NotallyXTheme { ImagePreviewCard() }
}
