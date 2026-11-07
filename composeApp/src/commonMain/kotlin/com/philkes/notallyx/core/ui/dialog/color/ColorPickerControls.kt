package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.philkes.notallyx.core.ui.components.ColorSwatchItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color as NoteColor

internal class ColorPickerState(
    val initialColorInt: Int = ComposeColor.White.toArgb(),
    val controller: ColorPickerController = ColorPickerController(),
) {
    var pickerColorInt by mutableIntStateOf(initialColorInt)
    var hexText by mutableStateOf(colorIntToHex(initialColorInt).removePrefix("#"))

    val hexString: String
        get() = colorIntToHex(pickerColorInt)

    fun updateColor(colorInt: Int) {
        pickerColorInt = colorInt
        hexText = colorIntToHex(colorInt).removePrefix("#")
        controller.selectByColor(ComposeColor(colorInt), fromUser = false)
    }

    fun onHexTextChanged(newText: String) {
        val filtered = newText.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
        hexText = filtered
        if (filtered.length == 6) {
            val parsed = parseColorOrNull("#$filtered")
            if (parsed != null) {
                pickerColorInt = parsed
                controller.selectByColor(ComposeColor(parsed), fromUser = false)
            }
        }
    }

    fun onColorChanged(envelope: ColorEnvelope) {
        val colorInt = envelope.color.toArgb()
        pickerColorInt = colorInt
        hexText = colorIntToHex(colorInt).removePrefix("#")
    }
}

@Composable
internal fun rememberColorPickerState(
    initialColorInt: Int = ComposeColor.White.toArgb(),
    controller: ColorPickerController = rememberColorPickerController(),
): ColorPickerState {
    return remember(initialColorInt) {
        ColorPickerState(initialColorInt = initialColorInt, controller = controller)
    }
}

@Composable
internal fun ColorPickerControls(
    state: ColorPickerState,
    isDuplicate: Boolean,
    modifier: Modifier = Modifier,
    colorSize: Dp = 54.dp,
    presetColors: List<String> = remember { NoteColor.allColorStrings() },
    strings: ColorPickerStrings = rememberDefaultColorPickerStrings(),
) {
    val actualPresetColors =
        remember(presetColors) {
            if (presetColors.firstOrNull() == BaseNote.COLOR_DEFAULT) {
                presetColors
            } else {
                listOf(BaseNote.COLOR_DEFAULT) +
                    presetColors.filter { it != BaseNote.COLOR_DEFAULT }
            }
        }

    Column(modifier = modifier) {
        HsvColorPicker(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(10.dp),
            controller = state.controller,
            onColorChanged = { envelope ->
                if (envelope.fromUser) {
                    state.onColorChanged(envelope)
                }
            },
            initialColor = ComposeColor(state.initialColorInt),
        )

        @Suppress("DEPRECATION")
        val clipboardManager: ClipboardManager = LocalClipboardManager.current

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val restoreLabel = strings.restore
            IconButton(
                onClick = { state.updateColor(state.initialColorInt) },
                modifier = Modifier.size(36.dp).semantics { contentDescription = restoreLabel },
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = restoreLabel,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            BrightnessSlider(
                modifier = Modifier.weight(1f).height(35.dp),
                controller = state.controller,
                initialColor = ComposeColor(state.initialColorInt),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier.size(30.dp)
                        .background(ComposeColor(state.pickerColorInt), RoundedCornerShape(4.dp))
                        .border(
                            Dp.Hairline,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(4.dp),
                        )
            )

            Spacer(modifier = Modifier.width(8.dp))

            val lineColor = MaterialTheme.colorScheme.onSurface
            Row(
                modifier =
                    Modifier.drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                )

                Spacer(modifier = Modifier.width(2.dp))

                BasicTextField(
                    value = state.hexText,
                    onValueChange = { state.onHexTextChanged(it) },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.width(68.dp).semantics { contentDescription = "ColorCode" },
                )

                Spacer(modifier = Modifier.width(4.dp))
                val copyLabel = strings.copy
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(state.hexString)) },
                    modifier = Modifier.size(32.dp).semantics { contentDescription = copyLabel },
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = copyLabel,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        if (isDuplicate) {
            Text(
                text = strings.colorExists,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }

        Text(
            text = strings.color,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            actualPresetColors.forEach { presetHex ->
                val isDefault = (presetHex == BaseNote.COLOR_DEFAULT)
                val parsedColor = parseHexToComposeColor(presetHex)
                val isSelected =
                    if (isDefault) {
                        state.hexString.equals("#FFFFFF", ignoreCase = true)
                    } else {
                        state.hexString.equals(presetHex, ignoreCase = true)
                    }
                ColorSwatchItem(
                    color = parsedColor,
                    size = colorSize,
                    isSelected = isSelected,
                    isDottedBorder = isDefault,
                    contentDescription = presetHex,
                    onClick = {
                        if (isDefault) {
                            state.updateColor(ComposeColor.White.toArgb())
                        } else {
                            parseColorOrNull(presetHex)?.let { parsed -> state.updateColor(parsed) }
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ColorPickerControlsPreview() {
    NotallyXTheme {
        val state = rememberColorPickerState(ComposeColor.Red.toArgb())
        ColorPickerControls(
            state = state,
            isDuplicate = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
