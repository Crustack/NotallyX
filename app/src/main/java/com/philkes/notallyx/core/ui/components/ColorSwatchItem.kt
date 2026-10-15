package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing recycler_color.xml layout. */
@Composable
internal fun ColorSwatchItem(
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isDefaultColor: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(modifier = modifier.padding(4.dp).aspectRatio(1f).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = color),
            border =
                if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isDefaultColor) {
                    Icon(
                        painter = painterResource(id = R.drawable.star_rate_filled),
                        contentDescription = stringResource(R.string.text_default),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).align(Alignment.TopEnd).padding(2.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorSwatchItemPreview() {
    NotallyXTheme {
        ColorSwatchItem(
            color = Color(0xFFF28B82),
            isSelected = true,
            isDefaultColor = true,
        )
    }
}
