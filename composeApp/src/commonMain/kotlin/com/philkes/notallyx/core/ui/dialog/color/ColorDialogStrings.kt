package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
data class ColorPickerStrings(
    val changeColorTitle: String = "Change Color",
    val changeColorMessage: String =
        "Pick a color or create a new one.\nYou can also edit a Color by long clicking it.",
    val changeColorDefaultMessage: String = "The ★ marks the default color for new notes.",
    val newColorTitle: String = "New Color",
    val editColorTitle: String = "Edit Color",
    val deleteColorMessage: String =
        "What color should the notes have that currently use this color?",
    val colorExists: String = "This color already exists!",
    val color: String = "Color",
    val restore: String = "Restore",
    val copy: String = "Copy",
    val delete: String = "Delete",
    val back: String = "Back",
    val save: String = "Save",
    val cancel: String = "Cancel",
    val defaultText: String = "Default",
    val makeDefault: String = "Make default",
)

@Composable expect fun rememberDefaultColorPickerStrings(): ColorPickerStrings
