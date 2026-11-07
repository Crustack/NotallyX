package com.philkes.notallyx.core.ui.extensions

import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

fun TextFieldValue.toggleBold(): TextFieldValue =
    toggleSpanStyle(
        predicate = { it.fontWeight == FontWeight.Bold || (it.fontWeight?.weight ?: 0) >= 700 },
        removeModifier = { it.withoutBold() },
        createStyle = { SpanStyle(fontWeight = FontWeight.Bold) },
    )

fun TextFieldValue.toggleItalic(): TextFieldValue =
    toggleSpanStyle(
        predicate = { it.fontStyle == FontStyle.Italic },
        removeModifier = { it.withoutItalic() },
        createStyle = { SpanStyle(fontStyle = FontStyle.Italic) },
    )

fun TextFieldValue.toggleStrikethrough(): TextFieldValue =
    toggleSpanStyle(
        predicate = { it.textDecoration?.contains(TextDecoration.LineThrough) == true },
        removeModifier = { it.withoutStrikethrough() },
        createStyle = { SpanStyle(textDecoration = TextDecoration.LineThrough) },
    )

fun TextFieldValue.toggleMonospace(): TextFieldValue =
    toggleSpanStyle(
        predicate = { it.fontFamily == FontFamily.Monospace },
        removeModifier = { it.withoutMonospace() },
        createStyle = { SpanStyle(fontFamily = FontFamily.Monospace) },
    )

private fun TextFieldValue.toggleSpanStyle(
    predicate: (SpanStyle) -> Boolean,
    removeModifier: (SpanStyle) -> SpanStyle?,
    createStyle: () -> SpanStyle,
): TextFieldValue {
    val start = selection.min
    val end = selection.max
    if (start >= end) return this

    val text = annotatedString.text
    val coversAll = annotatedString.isFormatCovered(start, end, predicate)
    val currentStyles = annotatedString.spanStyles
    val updatedStyles = currentStyles.removeStyleRange(start, end, removeModifier)

    val finalStyles =
        if (coversAll) {
            updatedStyles
        } else {
            updatedStyles + AnnotatedString.Range(createStyle(), start, end)
        }

    val urlAnnotations = annotatedString.getStringAnnotations("URL", 0, text.length)
    val newAnnotatedString = buildAnnotatedString(text, finalStyles.normalize(), urlAnnotations)
    return copy(annotatedString = newAnnotatedString)
}

fun TextFieldValue.clearFormatting(): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val text = annotatedString.text

    if (start >= end) {
        return copy(annotatedString = AnnotatedString(text))
    }

    val updatedStyles = annotatedString.spanStyles.removeStyleRange(start, end) { null }
    val currentUrls = annotatedString.getStringAnnotations("URL", 0, text.length)
    val updatedUrls = currentUrls.removeAnnotationRange(start, end)

    val newAnnotatedString = buildAnnotatedString(text, updatedStyles.normalize(), updatedUrls)
    return copy(annotatedString = newAnnotatedString)
}

fun TextFieldValue.addOrUpdateLink(
    url: String,
    displayText: String = "",
    range: TextRange? = null,
): TextFieldValue {
    val start = range?.min ?: selection.min
    val end = range?.max ?: selection.max
    val currentText = annotatedString.text
    val textToUse =
        when {
            displayText.isNotEmpty() -> displayText
            start < end -> currentText.substring(start, end)
            else -> url
        }

    val newText = currentText.substring(0, start) + textToUse + currentText.substring(end)
    val newEnd = start + textToUse.length
    val delta = textToUse.length - (end - start)

    val updatedStyles =
        annotatedString.spanStyles.removeStyleRange(start, end) { style ->
            val hasBlue = style.color == ComposeColor.Blue
            val hasUnderline = style.textDecoration?.contains(TextDecoration.Underline) == true
            if (hasBlue || hasUnderline) {
                val newColor = if (hasBlue) ComposeColor.Unspecified else style.color
                val newDecoration =
                    if (hasUnderline) {
                        if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                            TextDecoration.LineThrough
                        } else null
                    } else style.textDecoration
                if (
                    newColor == ComposeColor.Unspecified &&
                        newDecoration == null &&
                        style.fontWeight == null &&
                        style.fontStyle == null &&
                        style.fontFamily == null
                ) {
                    null
                } else {
                    style.copy(color = newColor, textDecoration = newDecoration)
                }
            } else {
                style
            }
        }
    val updatedAnnotations =
        annotatedString
            .getStringAnnotations("URL", 0, currentText.length)
            .removeAnnotationRange(start, end)

    val shiftedStyles = updatedStyles.mapNotNull { r ->
        val s = if (r.start >= end) r.start + delta else r.start
        val e = if (r.end >= end) r.end + delta else r.end
        if (s in 0..newText.length && e in 0..newText.length && s < e) {
            AnnotatedString.Range(r.item, s, e)
        } else null
    }
    val shiftedAnnotations = updatedAnnotations.mapNotNull { r ->
        val s = if (r.start >= end) r.start + delta else r.start
        val e = if (r.end >= end) r.end + delta else r.end
        if (s in 0..newText.length && e in 0..newText.length && s < e) {
            AnnotatedString.Range(r.item, s, e)
        } else null
    }

    val finalStyles =
        shiftedStyles +
            AnnotatedString.Range(
                SpanStyle(
                    color = ComposeColor.Blue,
                    textDecoration = TextDecoration.Underline,
                ),
                start,
                newEnd,
            )
    val finalAnnotations = shiftedAnnotations + AnnotatedString.Range(url, start, newEnd)

    val newAnnotatedString =
        buildAnnotatedString(newText, finalStyles.normalize(), finalAnnotations)
    return copy(
        annotatedString = newAnnotatedString,
        selection = TextRange(newEnd),
    )
}

fun TextFieldValue.removeLink(range: TextRange): TextFieldValue {
    val start = range.min
    val end = range.max
    if (start >= end) return this

    val text = annotatedString.text
    val currentUrls = annotatedString.getStringAnnotations("URL", 0, text.length)
    val updatedUrls = currentUrls.removeAnnotationRange(start, end)

    val updatedStyles =
        annotatedString.spanStyles.removeStyleRange(start, end) { style ->
            val hasBlue = style.color == ComposeColor.Blue
            val hasUnderline = style.textDecoration?.contains(TextDecoration.Underline) == true
            if (hasBlue || hasUnderline) {
                val newColor = if (hasBlue) ComposeColor.Unspecified else style.color
                val newDecoration =
                    if (hasUnderline) {
                        if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                            TextDecoration.LineThrough
                        } else null
                    } else style.textDecoration
                if (
                    newColor == ComposeColor.Unspecified &&
                        newDecoration == null &&
                        style.fontWeight == null &&
                        style.fontStyle == null &&
                        style.fontFamily == null
                ) {
                    null
                } else {
                    style.copy(color = newColor, textDecoration = newDecoration)
                }
            } else {
                style
            }
        }

    val newAnnotatedString = buildAnnotatedString(text, updatedStyles.normalize(), updatedUrls)
    return copy(annotatedString = newAnnotatedString)
}

fun TextFieldValue.preserveStylesFrom(previousValue: TextFieldValue): TextFieldValue {
    val oldAnnotated = previousValue.annotatedString
    val oldText = oldAnnotated.text
    val newText = this.text

    if (
        annotatedString.spanStyles.isNotEmpty() ||
            annotatedString.getStringAnnotations("URL", 0, newText.length).isNotEmpty()
    ) {
        return this
    }

    if (
        oldAnnotated.spanStyles.isEmpty() &&
            oldAnnotated.getStringAnnotations("URL", 0, oldText.length).isEmpty()
    ) {
        return this
    }

    if (newText == oldText) {
        val preserved =
            buildAnnotatedString(
                newText,
                oldAnnotated.spanStyles,
                oldAnnotated.getStringAnnotations("URL", 0, oldText.length),
            )
        return copy(annotatedString = preserved)
    }

    // When text changed, calculate the difference and shift/adjust styles
    var prefixLen = 0
    while (
        prefixLen < oldText.length &&
            prefixLen < newText.length &&
            oldText[prefixLen] == newText[prefixLen]
    ) {
        prefixLen++
    }

    var suffixLen = 0
    while (
        suffixLen < (oldText.length - prefixLen) &&
            suffixLen < (newText.length - prefixLen) &&
            oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
    ) {
        suffixLen++
    }

    val oldEditStart = prefixLen
    val oldEditEnd = oldText.length - suffixLen
    val newEditEnd = newText.length - suffixLen
    val delta = newText.length - oldText.length

    val newStyles = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for (range in oldAnnotated.spanStyles) {
        val adjusted =
            adjustRange(
                range.start,
                range.end,
                oldEditStart,
                oldEditEnd,
                newEditEnd,
                delta,
                newText.length,
            )
        if (adjusted != null) {
            newStyles.add(AnnotatedString.Range(range.item, adjusted.first, adjusted.second))
        }
    }

    val newUrls = mutableListOf<AnnotatedString.Range<String>>()
    for (range in oldAnnotated.getStringAnnotations("URL", 0, oldText.length)) {
        val adjusted =
            adjustRange(
                range.start,
                range.end,
                oldEditStart,
                oldEditEnd,
                newEditEnd,
                delta,
                newText.length,
            )
        if (adjusted != null) {
            newUrls.add(AnnotatedString.Range(range.item, adjusted.first, adjusted.second))
        }
    }

    val updatedAnnotatedString = buildAnnotatedString(newText, newStyles.normalize(), newUrls)
    return copy(annotatedString = updatedAnnotatedString)
}

private fun adjustRange(
    start: Int,
    end: Int,
    oldEditStart: Int,
    oldEditEnd: Int,
    newEditEnd: Int,
    delta: Int,
    maxLength: Int,
): Pair<Int, Int>? {
    val newStart: Int
    val newEnd: Int

    when {
        // Span is entirely before the edit
        end <= oldEditStart -> {
            newStart = start
            newEnd = end
        }
        // Span is entirely after the edit
        start >= oldEditEnd -> {
            newStart = start + delta
            newEnd = end + delta
        }
        // Edit is entirely within the span
        start < oldEditStart && end > oldEditEnd -> {
            newStart = start
            newEnd = end + delta
        }
        // Span is entirely within the replaced/deleted edit
        start >= oldEditStart && end <= oldEditEnd -> {
            return null
        }
        // Edit overlaps the end of the span
        start < oldEditStart -> {
            newStart = start
            newEnd = oldEditStart
        }
        // Edit overlaps the start of the span
        else -> {
            newStart = newEditEnd
            newEnd = end + delta
        }
    }

    val clampedStart = newStart.coerceIn(0, maxLength)
    val clampedEnd = newEnd.coerceIn(0, maxLength)
    return if (clampedStart < clampedEnd) Pair(clampedStart, clampedEnd) else null
}
