package com.philkes.notallyx.core.ui.dialog.color

import android.content.res.Configuration
import android.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.FullScreenDialogContent
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote

@Composable
internal fun ColorEditDialog(
    colorToEdit: String,
    colors: Set<String>,
    isDefault: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onMakeDefaultClick: () -> Unit,
    onSaveColor: (newColor: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ColorEditContent(
            colorToEdit = colorToEdit,
            colors = colors,
            isDefault = isDefault,
            onDismissRequest = onDismissRequest,
            onDeleteClick = onDeleteClick,
            onMakeDefaultClick = onMakeDefaultClick,
            onSaveColor = onSaveColor,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ColorEditContent(
    colorToEdit: String,
    colors: Set<String>,
    isDefault: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onMakeDefaultClick: () -> Unit,
    onSaveColor: (newColor: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDefaultNoteColor = (colorToEdit == BaseNote.COLOR_DEFAULT)
    val initialColorInt =
        remember(colorToEdit) {
            if (isDefaultNoteColor) {
                Color.WHITE
            } else {
                parseColorOrNull(colorToEdit) ?: Color.WHITE
            }
        }
    val state = rememberColorPickerState(initialColorInt)
    val hexString = if (isDefaultNoteColor) BaseNote.COLOR_DEFAULT else state.hexString
    val isDuplicate = !isDefaultNoteColor && (hexString != colorToEdit) && (hexString in colors)

    FullScreenDialogContent(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.edit_color),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!isDefaultNoteColor) {
                val deleteLabel = stringResource(R.string.delete)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.semantics { contentDescription = deleteLabel },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = deleteLabel,
                    )
                }
            }
        },
        buttons = {
            TextButton(onClick = onMakeDefaultClick) {
                Icon(
                    painter =
                        painterResource(
                            if (isDefault) R.drawable.star_rate_filled else R.drawable.star
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp),
                )
                Text(
                    stringResource(if (isDefault) R.string.text_default else R.string.make_default)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row {
                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.back)) }
                TextButton(
                    onClick = { onSaveColor(hexString) },
                    enabled = !isDuplicate,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        },
    ) {
        if (!isDefaultNoteColor) {
            ColorPickerControls(
                state = state,
                isDuplicate = isDuplicate,
            )
        }
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
private fun ColorEditDialogPreview() {
    NotallyXTheme {
        ColorEditContent(
            colorToEdit = "#FAAFA9",
            colors = setOf("#FAAFA9", "#FFCC80"),
            isDefault = false,
            onDismissRequest = {},
            onDeleteClick = {},
            onMakeDefaultClick = {},
            onSaveColor = {},
        )
    }
}
