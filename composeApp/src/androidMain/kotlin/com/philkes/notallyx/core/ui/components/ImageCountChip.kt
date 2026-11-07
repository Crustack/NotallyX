package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.FileAttachment

@Composable
internal fun ImageCountChip(
    count: Int,
    modifier: Modifier = Modifier,
    currentPage: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val text = if (currentPage != null) "$currentPage/$count" else count.toString()
    ImageCountChip(
        text = text,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
internal fun ImageCountChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = clickableModifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add_images),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun BoxScope.ImageCountChip(
    images: List<FileAttachment>,
    pagerState: PagerState,
    onImageClick: (FileAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentImage = images.getOrNull(pagerState.currentPage) ?: images.firstOrNull()
    ImageCountChip(
        count = images.size,
        currentPage = pagerState.currentPage + 1,
        modifier = modifier.align(Alignment.TopEnd).padding(12.dp),
        onClick = { currentImage?.let(onImageClick) },
    )
}

@Preview(showBackground = true)
@Composable
private fun ImageCountChipEditScreenPreview() {
    NotallyXTheme { ImageCountChip(text = "1/2") }
}

@Preview(showBackground = true)
@Composable
private fun ImageCountChipCardPreview() {
    NotallyXTheme { ImageCountChip(count = 2) }
}
