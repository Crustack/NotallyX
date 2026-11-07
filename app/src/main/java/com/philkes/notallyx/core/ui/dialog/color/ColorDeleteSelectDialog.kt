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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.ColorSwatchItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote

@Composable
internal fun ColorDeleteSelectDialog(
    colorToDelete: String,
    colors: Set<String>,
    onDismissRequest: () -> Unit,
    onColorDeleted: (replacementColor: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectableColors =
        remember(colors, colorToDelete) {
            (listOf(BaseNote.COLOR_DEFAULT) + colors)
                .filter { (it != colorToDelete) && (it != BaseNote.COLOR_NEW) }
                .distinct()
        }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.delete_color_message)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    selectableColors.forEach { replacementColor ->
                        val composeColor = parseHexToComposeColor(replacementColor)
                        ColorSwatchItem(
                            color = composeColor,
                            contentDescription = replacementColor,
                            onClick = { onColorDeleted(replacementColor) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.back)) }
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
