package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color as NoteColor
import com.philkes.notallyx.data.model.parseToColorString

private fun parseHexToComposeColor(colorStr: String): ComposeColor {
    val hex = colorStr.parseToColorString()
    if (hex == BaseNote.COLOR_DEFAULT) return ComposeColor.Transparent
    return try {
        ComposeColor(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        ComposeColor.Transparent
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerDialog(
    currentColor: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    val presetColors = NoteColor.allColorStrings()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.change_color)) },
        text = {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    presetColors.forEach { colorString ->
                        val isSelected = (colorString == selectedColor)
                        val colorValue = parseHexToComposeColor(colorString)

                        Box(
                            modifier =
                                Modifier.size(44.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color =
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    )
                                    .semantics { contentDescription = colorString }
                                    .testTag("CardView")
                                    .clickable { selectedColor = colorString },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.checked_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    // Add new color option
                    Box(
                        modifier =
                            Modifier.size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .semantics { contentDescription = BaseNote.COLOR_NEW }
                                .testTag("CardView")
                                .clickable { selectedColor = BaseNote.COLOR_NEW },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = stringResource(R.string.add_item),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismissRequest()
                },
                modifier = Modifier.testTag("button1"),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}
