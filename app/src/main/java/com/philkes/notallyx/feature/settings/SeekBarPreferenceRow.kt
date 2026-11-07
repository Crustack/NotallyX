package com.philkes.notallyx.feature.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing preference_seekbar.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeekBarPreferenceRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showPreviewText: Boolean = false,
    showValueBadge: Boolean = false,
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged || showValueBadge

    val steps = ((valueRange.endInclusive - valueRange.start).toInt() - 1).coerceAtLeast(0)

    Column(
        modifier =
            modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, end = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Slider(
            value = value,
            onValueChange = { rawValue ->
                val roundedValue = if (steps > 0) kotlin.math.round(rawValue) else rawValue
                onValueChange(roundedValue)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            interactionSource = interactionSource,
            thumb = {
                Box(contentAlignment = Alignment.Center) {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        enabled = enabled,
                    )
                    if (isInteracting) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier =
                                Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(0, 0) {
                                        placeable.placeRelative(
                                            x = -placeable.width / 2,
                                            y = -placeable.height - 12.dp.roundToPx(),
                                        )
                                    }
                                },
                        ) {
                            Text(
                                text = value.toInt().toString(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        if (showPreviewText) {
            Text(
                text = stringResource(id = R.string.text_size_preview),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = value.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeekBarPreferenceRowPreview() {
    NotallyXTheme {
        SeekBarPreferenceRow(
            title = "Text Size",
            value = 16f,
            valueRange = 12f..32f,
            showPreviewText = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SeekBarPreferenceRowValuePreview() {
    NotallyXTheme {
        SeekBarPreferenceRow(
            title = "Text Size",
            value = 20f,
            valueRange = 12f..32f,
            showPreviewText = true,
            showValueBadge = true,
        )
    }
}
