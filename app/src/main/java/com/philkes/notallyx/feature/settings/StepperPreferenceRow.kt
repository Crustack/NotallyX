package com.philkes.notallyx.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Composable replacing preference_stepper.xml layout. */
@Composable
internal fun StepperPreferenceRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelFormatter: ((Int) -> String)? = null,
    onValueChange: (Int) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var textValue by
        remember(value) { mutableStateOf(labelFormatter?.invoke(value) ?: value.toString()) }

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val minusInteractionSource = remember { MutableInteractionSource() }
    val plusInteractionSource = remember { MutableInteractionSource() }

    val cornerRadius = 8.dp
    val strokeWidth = 1.dp
    val outlineColor = MaterialTheme.colorScheme.outline
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val minusEnabled = enabled && value > valueRange.first
    val plusEnabled = enabled && value < valueRange.last

    val buttonMinusColor = if (minusEnabled) primaryColor else primaryColor.copy(alpha = 0.38f)
    val iconMinusColor = if (minusEnabled) onPrimaryColor else onPrimaryColor.copy(alpha = 0.38f)

    val buttonPlusColor = if (plusEnabled) primaryColor else primaryColor.copy(alpha = 0.38f)
    val iconPlusColor = if (plusEnabled) onPrimaryColor else onPrimaryColor.copy(alpha = 0.38f)

    Column(modifier = modifier.wrapContentWidth().padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp).height(36.dp),
        ) {
            // Minus Button (Left Segment)
            Surface(
                shape =
                    RoundedCornerShape(
                        topStart = cornerRadius,
                        bottomStart = cornerRadius,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp,
                    ),
                border = BorderStroke(strokeWidth, buttonMinusColor),
                color = buttonMinusColor,
                modifier =
                    Modifier.height(36.dp)
                        .width(36.dp)
                        .testTag("${TestTags.STEPPER_DECREASE}_$title")
                        .repeatingClickable(
                            enabled = minusEnabled,
                            interactionSource = minusInteractionSource,
                            onAction = {
                                val newValue = (currentValue - 1).coerceIn(valueRange)
                                currentOnValueChange(newValue)
                            },
                        ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.remove),
                        contentDescription = "Decrease",
                        tint = iconMinusColor,
                    )
                }
            }

            // Value Box (Middle Segment)
            Surface(
                shape = RoundedCornerShape(0.dp),
                border =
                    BorderStroke(
                        strokeWidth,
                        if (enabled) outlineColor else outlineColor.copy(alpha = 0.38f),
                    ),
                color = containerColor,
                modifier = Modifier.height(36.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(36.dp).padding(0.dp),
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            textValue = input
                            val parsed = input.toIntOrNull()
                            if (parsed != null && parsed in valueRange) {
                                currentOnValueChange(parsed)
                            }
                        },
                        enabled = enabled,
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                color =
                                    if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    val parsed =
                                        textValue.toIntOrNull()?.coerceIn(valueRange) ?: value
                                    currentOnValueChange(parsed)
                                }
                            ),
                        modifier =
                            Modifier.wrapContentWidth()
                                .widthIn(min = 24.dp)
                                .testTag("${TestTags.STEPPER_INPUT}_$title"),
                    )
                }
            }

            // Plus Button (Right Segment)
            Surface(
                shape =
                    RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = cornerRadius,
                        bottomEnd = cornerRadius,
                    ),
                border = BorderStroke(strokeWidth, buttonPlusColor),
                color = buttonPlusColor,
                modifier =
                    Modifier.height(36.dp)
                        .width(36.dp)
                        .testTag("${TestTags.STEPPER_INCREASE}_$title")
                        .repeatingClickable(
                            enabled = plusEnabled,
                            interactionSource = plusInteractionSource,
                            onAction = {
                                val newValue = (currentValue + 1).coerceIn(valueRange)
                                currentOnValueChange(newValue)
                            },
                        ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Increase",
                        tint = iconPlusColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.repeatingClickable(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onAction: () -> Unit,
): Modifier {
    val currentOnAction by rememberUpdatedState(onAction)
    val scope = rememberCoroutineScope()
    return this.indication(interactionSource, ripple()).pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures(
            onPress = { offset ->
                val press = PressInteraction.Press(offset)
                interactionSource.emit(press)
                currentOnAction()
                val job = scope.launch {
                    delay(400L)
                    while (isActive) {
                        currentOnAction()
                        delay(100L)
                    }
                }
                try {
                    awaitRelease()
                } finally {
                    job.cancel()
                    interactionSource.emit(PressInteraction.Release(press))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepperPreferenceRowPreview() {
    NotallyXTheme {
        Column {
            StepperPreferenceRow(
                title = "Auto Remove Bin Notes (Days)",
                value = 7,
                valueRange = 0..30,
            )
            StepperPreferenceRow(
                title = "Auto Save After Idle Time",
                value = -1,
                valueRange = -1..60,
                labelFormatter = { if (it == -1) "Off" else "${it}s" },
            )
            StepperPreferenceRow(
                title = "Auto Empty Bin",
                value = 30,
                valueRange = 0..365,
                labelFormatter = { "$it days" },
            )
        }
    }
}
