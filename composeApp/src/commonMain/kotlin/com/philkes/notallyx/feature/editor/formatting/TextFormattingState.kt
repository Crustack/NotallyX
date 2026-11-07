package com.philkes.notallyx.feature.editor.formatting

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

data class TextFormattingState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isMonospace: Boolean = false,
    val isLink: Boolean = false,
)

fun AnnotatedString.getFormattingState(start: Int, end: Int): TextFormattingState {
    if (start >= end) return TextFormattingState()
    var isBold = false
    var isItalic = false
    var isStrikethrough = false
    var isMonospace = false
    var isLink = false

    spanStyles.forEach { range ->
        if (range.start < end && range.end > start) {
            val style = range.item
            if (
                style.fontWeight == FontWeight.Bold ||
                    (style.fontWeight != null && style.fontWeight!!.weight >= 700)
            ) {
                isBold = true
            }
            if (style.fontStyle == FontStyle.Italic) {
                isItalic = true
            }
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                isStrikethrough = true
            }
            if (style.fontFamily == FontFamily.Monospace) {
                isMonospace = true
            }
        }
    }
    getStringAnnotations("URL", start, end).forEach { range ->
        if (range.start < end && range.end > start) {
            isLink = true
        }
    }
    return TextFormattingState(
        isBold = isBold,
        isItalic = isItalic,
        isStrikethrough = isStrikethrough,
        isMonospace = isMonospace,
        isLink = isLink,
    )
}
