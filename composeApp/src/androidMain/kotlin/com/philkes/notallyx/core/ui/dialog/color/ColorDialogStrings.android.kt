package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.philkes.notallyx.R

@Composable
actual fun rememberDefaultColorPickerStrings(): ColorPickerStrings =
    ColorPickerStrings(
        changeColorTitle = stringResource(R.string.change_color),
        changeColorMessage = stringResource(R.string.change_color_message),
        changeColorDefaultMessage = stringResource(R.string.change_color_default_message),
        newColorTitle = stringResource(R.string.new_color),
        editColorTitle = stringResource(R.string.edit_color),
        deleteColorMessage = stringResource(R.string.delete_color_message),
        colorExists = stringResource(R.string.color_exists),
        color = stringResource(R.string.color),
        restore = stringResource(R.string.restore),
        copy = stringResource(R.string.copy),
        delete = stringResource(R.string.delete),
        back = stringResource(R.string.back),
        save = stringResource(R.string.save),
        cancel = stringResource(R.string.cancel),
        defaultText = stringResource(R.string.text_default),
        makeDefault = stringResource(R.string.make_default),
    )
