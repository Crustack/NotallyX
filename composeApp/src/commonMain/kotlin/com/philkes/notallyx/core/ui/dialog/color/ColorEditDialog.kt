package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onSaveColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    strings: ColorPickerStrings = rememberDefaultColorPickerStrings(),
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
            strings = strings,
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
    onSaveColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    strings: ColorPickerStrings = rememberDefaultColorPickerStrings(),
) {
    val isDefaultNoteColor = (colorToEdit == BaseNote.COLOR_DEFAULT)
    val initialColorInt =
        remember(colorToEdit) {
            if (isDefaultNoteColor) {
                0xFFFFFFFF.toInt()
            } else {
                parseColorOrNull(colorToEdit) ?: 0xFFFFFFFF.toInt()
            }
        }
    val state = rememberColorPickerState(initialColorInt)
    val hexString = if (isDefaultNoteColor) BaseNote.COLOR_DEFAULT else state.hexString
    val isDuplicate = !isDefaultNoteColor && (hexString != colorToEdit) && (hexString in colors)

    FullScreenDialogContent(
        modifier = modifier,
        title = {
            Text(
                text = strings.editColorTitle,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!isDefaultNoteColor) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.semantics { contentDescription = strings.delete },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                    )
                }
            }
        },
        buttons = {
            TextButton(onClick = onMakeDefaultClick) {
                Icon(
                    imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp),
                )
                Text(if (isDefault) strings.defaultText else strings.makeDefault)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismissRequest) { Text(strings.back) }
            TextButton(
                onClick = { onSaveColor(hexString) },
                enabled = !isDuplicate,
            ) {
                Text(strings.save)
            }
        },
    ) {
        if (!isDefaultNoteColor) {
            ColorPickerControls(
                state = state,
                isDuplicate = isDuplicate,
                strings = strings,
            )
        }
    }
}

@Preview
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
