package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.components.ColorSwatchItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote

@Composable
internal fun ColorDeleteSelectDialog(
    colorToDelete: String,
    colors: Set<String>,
    onDismissRequest: () -> Unit,
    onColorDeleted: (String) -> Unit,
    modifier: Modifier = Modifier,
    strings: ColorPickerStrings = rememberDefaultColorPickerStrings(),
) {
    val replacementColors =
        remember(colors, colorToDelete) {
            val list = mutableListOf(BaseNote.COLOR_DEFAULT)
            list.addAll(
                colors.filter { (it != colorToDelete) && (it != BaseNote.COLOR_DEFAULT) }.distinct()
            )
            list
        }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = strings.deleteColorMessage) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    replacementColors.forEach { colorString ->
                        val composeColor = parseHexToComposeColor(colorString)
                        ColorSwatchItem(
                            color = composeColor,
                            size = 64.dp,
                            isDottedBorder = (colorString == BaseNote.COLOR_DEFAULT),
                            contentDescription = colorString,
                            onClick = { onColorDeleted(colorString) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(strings.back) } },
    )
}

@Preview
@Composable
private fun ColorDeleteSelectDialogPreview() {
    NotallyXTheme {
        ColorDeleteSelectDialog(
            colorToDelete = "#FAAFA9",
            colors = setOf("#FAAFA9", "#FFCC80", "#FFF8B9"),
            onDismissRequest = {},
            onColorDeleted = {},
        )
    }
}
