package com.philkes.notallyx.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.presentation.setLightStatusAndNavBar
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.dialog.ColorPickerDialog
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences

fun AppCompatActivity.showColorSelectDialog(
    colors: Set<ColorString>,
    currentColor: ColorString?,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: ColorString, oldColor: ColorString?) -> Unit,
    deleteCallback: (colorToDelete: ColorString, newColor: ColorString) -> Unit,
) {
    val preferences = NotallyXPreferences.getInstance(this)
    val composeView = ComposeView(this)
    val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            ColorPickerDialog(
                currentColor = currentColor ?: BaseNote.COLOR_DEFAULT,
                onDismissRequest = { dialog.dismiss() },
                onColorSelected = { selectedColor ->
                    dialog.dismiss()
                    if (selectedColor == BaseNote.COLOR_NEW) {
                        showEditColorDialog(
                            (colors + preferences.defaultNoteColor.value).toList(),
                            null,
                            setNavigationbarLight,
                            callback,
                            deleteCallback,
                        )
                    } else callback(selectedColor, null)
                },
            )
        }
    }
    setNavigationbarLight?.let { dialog.window?.setLightStatusAndNavBar(it) }
    dialog.show()
}

private fun AppCompatActivity.showEditColorDialog(
    colors: List<ColorString>,
    oldColor: ColorString?,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: ColorString, oldColor: ColorString?) -> Unit,
    deleteCallback: (colorToDelete: ColorString, newColor: ColorString) -> Unit,
) {
    val preferences = NotallyXPreferences.getInstance(this)
    val composeView = ComposeView(this)
    val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            ColorPickerDialog(
                currentColor = oldColor ?: BaseNote.COLOR_DEFAULT,
                onDismissRequest = { dialog.dismiss() },
                onColorSelected = { selectedColor ->
                    dialog.dismiss()
                    if (selectedColor == oldColor) {
                        callback(oldColor, null)
                    } else {
                        callback(selectedColor, oldColor)
                    }
                },
            )
        }
    }
    setNavigationbarLight?.let { dialog.window?.setLightStatusAndNavBar(it) }
    dialog.show()
}

private fun AppCompatActivity.showDeleteColorDialog(
    colors: List<String>,
    oldColor: String,
    setNavigationbarLight: Boolean?,
    callback: (selectedColor: String, oldColor: String?) -> Unit,
    deleteCallback: (colorToDelete: String, newColor: String) -> Unit,
) {
    val preferences = NotallyXPreferences.getInstance(this)
    val composeView = ComposeView(this)
    val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            ColorPickerDialog(
                currentColor = oldColor,
                onDismissRequest = { dialog.dismiss() },
                onColorSelected = { selectedColor ->
                    dialog.dismiss()
                    deleteCallback(oldColor, selectedColor)
                },
            )
        }
    }
    setNavigationbarLight?.let { dialog.window?.setLightStatusAndNavBar(it) }
    dialog.show()
}
