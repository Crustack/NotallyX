package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.theme.contentColorFor

/** Composable replacing recycler_color.xml layout. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColorSwatchItem(
    color: Color,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isDefaultColor: Boolean = false,
    isDottedBorder: Boolean = false,
    isAddButton: Boolean = false,
    contentDescription: String? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.selected = isSelected
            }
        } else if (isSelected) {
            Modifier.semantics { this.selected = true }
        } else {
            Modifier
        }

    val iconTint =
        if (color == Color.Transparent) MaterialTheme.colorScheme.onSurface
        else contentColorFor(color)

    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeWidth = if (isSelected) 3.dp else 1.5.dp
    val borderColor = if (isSelected) primaryColor else outlineColor
    val dottedBorderModifier =
        if (isDottedBorder) {
            dottedBorder(strokeWidth, borderColor)
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .size(size)
                .padding(2.dp)
                .then(semanticsModifier)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxSize().then(dottedBorderModifier),
            shape = RectangleShape,
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isAddButton) MaterialTheme.colorScheme.surface
                        else if (color == Color.Transparent)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else color
                ),
            border =
                if (isDottedBorder) {
                    null
                } else if (isSelected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                },
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isAddButton) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = stringResource(R.string.new_color),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(id = R.drawable.checked_circle),
                            contentDescription = null,
                            tint =
                                if (color == Color.Transparent) MaterialTheme.colorScheme.primary
                                else iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    if (isDefaultColor) {
                        Icon(
                            painter = painterResource(id = R.drawable.star_rate_filled),
                            contentDescription = stringResource(R.string.text_default),
                            tint =
                                if (isSelected && color != Color.Transparent) iconTint
                                else MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.size(size / 3).align(Alignment.TopEnd).padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun dottedBorder(
    strokeWidth: Dp,
    borderColor: Color,
): Modifier = Modifier.drawWithContent {
    drawContent()
    val strokePx = strokeWidth.toPx()
    val halfStroke = strokePx / 2f
    drawRect(
        color = borderColor,
        topLeft = Offset(halfStroke, halfStroke),
        size = Size(this.size.width - strokePx, this.size.height - strokePx),
        style =
            Stroke(
                width = strokePx,
                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ColorSwatchItemPreview() {
    NotallyXTheme {
        ColorSwatchItem(
            color = Color(0xFFF28B82),
            isSelected = true,
            isDefaultColor = true,
            isDottedBorder = true,
        )
    }
}
