package com.philkes.notallyx.core.ui.dialog.color

import android.content.res.Configuration
import android.graphics.Color
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.ColorSwatchItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color as NoteColor

internal class ColorPickerState(
    val initialColorInt: Int = Color.WHITE,
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

    fun onHexTextChange(newText: String) {
        val filtered = newText.filter { it in "0123456789abcdefABCDEF" }.take(6)
        hexText = filtered
        if (filtered.length == 6) {
            parseColorOrNull(filtered)?.let { parsed ->
                pickerColorInt = parsed
                controller.selectByColor(ComposeColor(parsed), fromUser = false)
            }
        }
    }

    fun onColorChanged(envelope: ColorEnvelope) {
        if (envelope.fromUser) {
            val colorInt = envelope.color.toArgb()
            pickerColorInt = colorInt
            hexText = colorIntToHex(colorInt).removePrefix("#")
        }
    }
}

@Composable
internal fun rememberColorPickerState(
    initialColorInt: Int = Color.WHITE,
    controller: ColorPickerController = rememberColorPickerController(),
): ColorPickerState {
    return remember(initialColorInt, controller) { ColorPickerState(initialColorInt, controller) }
}

@Composable
internal fun ColorPickerControls(
    state: ColorPickerState,
    isDuplicate: Boolean,
    modifier: Modifier = Modifier,
    colorSize: Dp = 54.dp,
    presetColors: List<String> = remember { NoteColor.allColorStrings() },
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
    @Suppress("DEPRECATION") val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column(modifier = modifier) {
        HsvColorPicker(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(10.dp),
            controller = state.controller,
            initialColor = remember(state.initialColorInt) { ComposeColor(state.initialColorInt) },
            onColorChanged = { envelope: ColorEnvelope -> state.onColorChanged(envelope) },
            onColorPickingFinished = { envelope: ColorEnvelope -> state.onColorChanged(envelope) },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val restoreLabel = stringResource(R.string.restore)
            IconButton(
                onClick = { state.updateColor(state.initialColorInt) },
                modifier = Modifier.size(36.dp).semantics { contentDescription = restoreLabel },
            ) {
                Icon(
                    painter = painterResource(R.drawable.restore),
                    contentDescription = restoreLabel,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            BrightnessSlider(
                modifier = Modifier.weight(1f).height(35.dp),
                controller = state.controller,
                initialColor =
                    remember(state.initialColorInt) { ComposeColor(state.initialColorInt) },
                borderRadius = 4.dp,
                wheelRadius = 12.dp,
                onColorChanged = { envelope: ColorEnvelope -> state.onColorChanged(envelope) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(30.dp)
                        .background(
                            ComposeColor(state.pickerColorInt),
                            RoundedCornerShape(6.dp),
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RectangleShape,
                        )
            )
            Spacer(modifier = Modifier.width(8.dp))
            val outlineColor = MaterialTheme.colorScheme.outline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            val y = size.height - (strokeWidth / 2)
                            drawLine(
                                color = outlineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth,
                            )
                        }
                        .padding(bottom = 2.dp),
            ) {
                Text(
                    text = "#",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(2.dp))
                BasicTextField(
                    value = state.hexText,
                    onValueChange = state::onHexTextChange,
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.width(68.dp).semantics { contentDescription = "ColorCode" },
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            val copyLabel = stringResource(R.string.copy)
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(state.hexString)) },
                modifier = Modifier.size(32.dp).semantics { contentDescription = copyLabel },
            ) {
                Icon(
                    painter = painterResource(R.drawable.content_copy),
                    contentDescription = copyLabel,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isDuplicate) {
            Text(
                text = stringResource(R.string.color_exists),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = stringResource(R.string.color),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 6,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            state.updateColor(Color.WHITE)
                        } else {
                            parseColorOrNull(presetHex)?.let { parsed -> state.updateColor(parsed) }
                        }
                    },
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ColorPickerControlsPreview() {
    NotallyXTheme {
        ColorPickerControls(
            state = rememberColorPickerState(Color.RED),
            isDuplicate = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
