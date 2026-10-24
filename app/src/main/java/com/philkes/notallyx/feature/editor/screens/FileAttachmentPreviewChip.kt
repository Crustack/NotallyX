package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing recycler_preview_file.xml layout. */
@Composable
internal fun FileAttachmentPreviewChip(
    fileName: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = RoundedCornerShape(20),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.05f),
            modifier = modifier,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (showIcon) {
                    Icon(
                        painter = painterResource(id = R.drawable.text_file),
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = fileName.ifEmpty { stringResource(R.string.cant_load_file) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current,
                )
            }
        }
    }
}

/** Composable replacing recycler_preview_image.xml layout. */
@Composable
internal fun ImagePreviewCard(
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
