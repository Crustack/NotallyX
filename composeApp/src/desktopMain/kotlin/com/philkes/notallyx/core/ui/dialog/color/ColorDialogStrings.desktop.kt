package com.philkes.notallyx.core.ui.dialog.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDefaultColorPickerStrings(): ColorPickerStrings = remember {
    ColorPickerStrings()
}
