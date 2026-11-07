package com.philkes.notallyx.core.ui.dialog.color

import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.parseToColorString

internal enum class DialogMode {
    SELECT,
    CREATE,
    EDIT,
    DELETE_SELECT,
}

internal fun parseHexToComposeColor(colorStr: String): ComposeColor {
    val hex = colorStr.parseToColorString()
    if (hex == BaseNote.COLOR_DEFAULT) return ComposeColor.Transparent
    return try {
        ComposeColor(Color.parseColor(hex))
    } catch (_: Exception) {
        ComposeColor.Transparent
    }
}

internal fun parseColorOrNull(hex: String): Int? {
    val trimmed = hex.trim()
    val formatted = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    return try {
        Color.parseColor(formatted)
    } catch (_: Exception) {
        null
    }
}

internal fun colorIntToHex(colorInt: Int): String {
    return String.format("#%06X", 0xFFFFFF and colorInt)
}
