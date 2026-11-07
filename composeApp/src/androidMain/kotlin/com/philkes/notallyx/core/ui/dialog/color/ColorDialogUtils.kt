package com.philkes.notallyx.core.ui.dialog.color

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
    return parseColorOrNull(hex)?.let { ComposeColor(it) } ?: ComposeColor.Transparent
}

internal fun parseColorOrNull(hex: String): Int? {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> (0xFF000000L or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

internal fun colorIntToHex(colorInt: Int): String {
    return String.format("#%06X", 0xFFFFFF and colorInt)
}
