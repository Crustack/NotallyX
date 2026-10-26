package com.philkes.notallyx.core.ui.dialog.color

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.ColorSwatchItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote

@Composable
internal fun ColorSelectDialog(
    currentColor: String,
    colors: Set<String>,
    defaultColor: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
    onAddColorClick: () -> Unit,
    onEditColorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actualColors =
        remember(colors, defaultColor, currentColor) {
            val list = mutableListOf<String>()
            list.add(BaseNote.COLOR_NEW)
            list.add(BaseNote.COLOR_DEFAULT)
            val extraColors =
                (colors + defaultColor + currentColor)
                    .filter { (it != BaseNote.COLOR_DEFAULT) && (it != BaseNote.COLOR_NEW) }
                    .distinct()
            list.addAll(extraColors)
            list
        }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.change_color)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text =
                        "${stringResource(R.string.change_color_message)}\n${stringResource(R.string.change_color_default_message)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    actualColors.forEach { colorString ->
                        if (colorString == BaseNote.COLOR_NEW) {
                            ColorSwatchItem(
                                color = ComposeColor.Transparent,
                                size = 64.dp,
                                isAddButton = true,
                                contentDescription = BaseNote.COLOR_NEW,
                                onClick = onAddColorClick,
                            )
                        } else {
                            val isSelected = (colorString == currentColor)
                            val isDefault = (colorString == defaultColor)
                            val composeColor = parseHexToComposeColor(colorString)

                            ColorSwatchItem(
                                color = composeColor,
                                size = 64.dp,
                                isSelected = isSelected,
                                isDefaultColor = isDefault,
                                contentDescription = colorString,
                                onClick = {
                                    onColorSelected(colorString)
                                    onDismissRequest()
                                },
                            ) {
                                onEditColorClick(colorString)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(name = "Light Mode", showBackground = true, showSystemUi = true)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun ColorSelectDialogPreview() {
    NotallyXTheme {
        ColorSelectDialog(
            currentColor = BaseNote.COLOR_DEFAULT,
            colors = setOf("#FAAFA9", "#FFCC80", "#FFF8B9"),
            defaultColor = BaseNote.COLOR_DEFAULT,
            onDismissRequest = {},
            onColorSelected = {},
            onAddColorClick = {},
            onEditColorClick = {},
        )
    }
}
