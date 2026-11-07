package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote

@Composable
fun ColorPickerDialog(
    currentColor: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: Set<String> = emptySet(),
    defaultColor: String = BaseNote.COLOR_DEFAULT,
    onColorChanged: ((oldColor: String, newColor: String) -> Unit)? = null,
    onColorDeleted: ((colorToDelete: String, newColor: String) -> Unit)? = null,
    onSetDefaultColor: ((String) -> Unit)? = null,
    strings: ColorPickerStrings = rememberDefaultColorPickerStrings(),
) {
    var mode by remember { mutableStateOf(DialogMode.SELECT) }
    var editingColor by remember { mutableStateOf<String?>(null) }
    var currentDefaultColor by
        remember(defaultColor) { mutableStateOf(defaultColor.ifEmpty { BaseNote.COLOR_DEFAULT }) }

    when (mode) {
        DialogMode.SELECT -> {
            ColorSelectDialog(
                currentColor = currentColor,
                colors = colors,
                defaultColor = currentDefaultColor,
                onDismissRequest = onDismissRequest,
                onColorSelected = onColorSelected,
                onAddColorClick = { mode = DialogMode.CREATE },
                onEditColorClick = { selectedColor ->
                    editingColor = selectedColor
                    mode = DialogMode.EDIT
                },
                modifier = modifier,
                strings = strings,
            )
        }

        DialogMode.CREATE -> {
            ColorCreateDialog(
                existingColors = colors,
                onDismissRequest = { mode = DialogMode.SELECT },
                onSaveColor = { newColor ->
                    onColorSelected(newColor)
                    onDismissRequest()
                },
                modifier = modifier,
                strings = strings,
            )
        }

        DialogMode.EDIT -> {
            val colorToEdit = editingColor ?: BaseNote.COLOR_DEFAULT
            ColorEditDialog(
                colorToEdit = colorToEdit,
                colors = colors,
                isDefault = (colorToEdit == currentDefaultColor),
                onDismissRequest = { mode = DialogMode.SELECT },
                onDeleteClick = { mode = DialogMode.DELETE_SELECT },
                onMakeDefaultClick = {
                    currentDefaultColor = colorToEdit
                    onSetDefaultColor?.invoke(colorToEdit)
                },
                onSaveColor = { newColor ->
                    if ((colorToEdit != BaseNote.COLOR_DEFAULT) && (newColor != colorToEdit)) {
                        onColorChanged?.invoke(colorToEdit, newColor)
                    }
                    onDismissRequest()
                },
                modifier = modifier,
                strings = strings,
            )
        }

        DialogMode.DELETE_SELECT -> {
            val colorToDelete = editingColor ?: ""
            ColorDeleteSelectDialog(
                colorToDelete = colorToDelete,
                colors = colors,
                onDismissRequest = { mode = DialogMode.EDIT },
                onColorDeleted = { replacementColor ->
                    onColorDeleted?.invoke(colorToDelete, replacementColor)
                    onDismissRequest()
                },
                modifier = modifier,
                strings = strings,
            )
        }
    }
}

@Preview
@Composable
private fun ColorPickerDialogPreview() {
    NotallyXTheme {
        ColorPickerDialog(
            currentColor = BaseNote.COLOR_DEFAULT,
            onDismissRequest = {},
            onColorSelected = {},
        )
    }
}
