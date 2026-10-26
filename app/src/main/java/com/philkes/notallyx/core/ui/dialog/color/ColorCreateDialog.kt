package com.philkes.notallyx.core.ui.dialog.color

import android.content.res.Configuration
import android.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.FullScreenDialogContent
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Color as NoteColor
import com.philkes.notallyx.data.model.toColorString

@Composable
internal fun ColorCreateDialog(
    existingColors: Set<String>,
    onDismissRequest: () -> Unit,
    onSaveColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialColor: String = NoteColor.CORAL.toColorString(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ColorCreateContent(
            existingColors = existingColors,
            onDismissRequest = onDismissRequest,
            onSaveColor = onSaveColor,
            modifier = modifier,
            initialColor = initialColor,
        )
    }
}

@Composable
internal fun ColorCreateContent(
    existingColors: Set<String>,
    onDismissRequest: () -> Unit,
    onSaveColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialColor: String = NoteColor.CORAL.toColorString(),
) {
    val initialColorInt = remember(initialColor) { parseColorOrNull(initialColor) ?: Color.WHITE }
    val state = rememberColorPickerState(initialColorInt)
    val hexString = state.hexString
    val isDuplicate = hexString in existingColors

    FullScreenDialogContent(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.new_color),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.back)) }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onSaveColor(hexString) },
                enabled = !isDuplicate,
            ) {
                Text(stringResource(R.string.save))
            }
        },
    ) {
        ColorPickerControls(
            state = state,
            isDuplicate = isDuplicate,
        )
    }
}

@Preview(name = "Light Mode", showBackground = true, showSystemUi = true)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun ColorCreateDialogPreview() {
    NotallyXTheme {
        ColorCreateContent(
            existingColors = setOf("#FAAFA9", "#FFCC80"),
            onDismissRequest = {},
            onSaveColor = {},
        )
    }
}
