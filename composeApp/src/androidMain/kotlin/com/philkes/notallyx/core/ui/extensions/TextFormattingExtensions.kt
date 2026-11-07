package com.philkes.notallyx.core.ui.extensions

import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.philkes.notallyx.core.common.extensions.getUrl
import com.philkes.notallyx.data.model.SpanRepresentation

fun AnnotatedString.Builder.setSpan(style: SpanStyle, start: Int, end: Int) {
    val s = start.coerceIn(0, length)
    val e = end.coerceIn(0, length)
    if (s < e) {
        addStyle(style, s, e)
    }
}

fun AnnotatedString.Builder.setSpan(tag: String, annotation: String, start: Int, end: Int) {
    val s = start.coerceIn(0, length)
    val e = end.coerceIn(0, length)
    if (s < e) {
        addStringAnnotation(tag, annotation, s, e)
    }
}

fun String.applySpans(representations: List<SpanRepresentation>): AnnotatedString {
    val builder = AnnotatedString.Builder(this)
    val textLength = length
    for (span in representations) {
        val start = span.start
        val end = span.end

        if (span.bold) {
            builder.setSpan(SpanStyle(fontWeight = FontWeight.Bold), start, end)
        }
        if (span.italic) {
            builder.setSpan(SpanStyle(fontStyle = FontStyle.Italic), start, end)
        }
        if (span.strikethrough) {
            builder.setSpan(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        }
        if (span.monospace) {
            builder.setSpan(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
        }
        if (span.link) {
            val url =
                span.linkData ?: getUrl(start.coerceIn(0, textLength), end.coerceIn(0, textLength))
            builder.setSpan("URL", url, start, end)
            builder.setSpan(
                SpanStyle(
                    color = ComposeColor.Blue,
                    textDecoration = TextDecoration.Underline,
                ),
                start,
                end,
            )
        }
    }
    return builder.toAnnotatedString()
}

fun String.toAnnotatedString(spans: List<SpanRepresentation>): AnnotatedString = applySpans(spans)

fun AnnotatedString.toSpanRepresentations(): ArrayList<SpanRepresentation> {
    val representations = mutableListOf<SpanRepresentation>()
    spanStyles.forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val end = range.end.coerceIn(0, text.length)
        if (start < end) {
            val style = range.item
            val isBold =
                style.fontWeight == FontWeight.Bold ||
                    (style.fontWeight != null && style.fontWeight!!.weight >= 700)
            val isItalic = style.fontStyle == FontStyle.Italic
            val isMonospace = style.fontFamily == FontFamily.Monospace
            val isStrikethrough = style.textDecoration?.contains(TextDecoration.LineThrough) == true

            if (isBold || isItalic || isMonospace || isStrikethrough) {
                representations.add(
                    SpanRepresentation(
                        start = start,
                        end = end,
                        bold = isBold,
                        italic = isItalic,
                        monospace = isMonospace,
                        strikethrough = isStrikethrough,
                    )
                )
            }
        }
    }
    getStringAnnotations("URL", 0, text.length).forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val end = range.end.coerceIn(0, text.length)
        if (start < end) {
            representations.add(
                SpanRepresentation(
                    start = start,
                    end = end,
                    link = true,
                    linkData = range.item,
                )
            )
        }
    }
    return representations.mergeEqualRanges().apply {
        sortWith(compareBy({ it.start }, { it.end }))
    }
}

fun List<SpanRepresentation>.mergeEqualRanges(): ArrayList<SpanRepresentation> {
    val map = LinkedHashMap<Pair<Int, Int>, SpanRepresentation>()
    for (rep in this) {
        val key = Pair(rep.start, rep.end)
        val existing = map[key]
        if (existing == null) {
            map[key] = rep.copy()
        } else {
            if (rep.bold) existing.bold = true
            if (rep.italic) existing.italic = true
            if (rep.monospace) existing.monospace = true
            if (rep.strikethrough) existing.strikethrough = true
            if (rep.link) {
                existing.link = true
                existing.linkData = rep.linkData ?: existing.linkData
            }
        }
    }
    return ArrayList(map.values)
}

fun AnnotatedString.isFormatCovered(
    start: Int,
    end: Int,
    predicate: (SpanStyle) -> Boolean,
): Boolean {
    if (start >= end) return false
    for (i in start until end) {
        val covered = spanStyles.any { it.start <= i && it.end > i && predicate(it.item) }
        if (!covered) return false
    }
    return true
}

fun List<AnnotatedString.Range<SpanStyle>>.removeStyleRange(
    selStart: Int,
    selEnd: Int,
    transform: (SpanStyle) -> SpanStyle?,
): List<AnnotatedString.Range<SpanStyle>> {
    val result = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for (range in this) {
        val rStart = range.start
        val rEnd = range.end
        val style = range.item

        when {
            selEnd <= rStart || selStart >= rEnd -> {
                result.add(range)
            }
            selStart <= rStart && selEnd >= rEnd -> {
                val modified = transform(style)
                if (modified != null) {
                    result.add(AnnotatedString.Range(modified, rStart, rEnd))
                }
            }
            selStart > rStart && selEnd < rEnd -> {
                result.add(AnnotatedString.Range(style, rStart, selStart))
                val modified = transform(style)
                if (modified != null) {
                    result.add(AnnotatedString.Range(modified, selStart, selEnd))
                }
                result.add(AnnotatedString.Range(style, selEnd, rEnd))
            }
            selStart <= rStart && selEnd < rEnd -> {
                val modified = transform(style)
                if (modified != null) {
                    result.add(AnnotatedString.Range(modified, rStart, selEnd))
                }
                result.add(AnnotatedString.Range(style, selEnd, rEnd))
            }
            else -> {
                result.add(AnnotatedString.Range(style, rStart, selStart))
                val modified = transform(style)
                if (modified != null) {
                    result.add(AnnotatedString.Range(modified, selStart, rEnd))
                }
            }
        }
    }
    return result
}

fun List<AnnotatedString.Range<String>>.removeAnnotationRange(
    selStart: Int,
    selEnd: Int,
): List<AnnotatedString.Range<String>> {
    val result = mutableListOf<AnnotatedString.Range<String>>()
    for (range in this) {
        val rStart = range.start
        val rEnd = range.end
        val item = range.item

        when {
            selEnd <= rStart || selStart >= rEnd -> result.add(range)
            selStart <= rStart && selEnd >= rEnd -> {}
            selStart > rStart && selEnd < rEnd -> {
                result.add(AnnotatedString.Range(item, rStart, selStart))
                result.add(AnnotatedString.Range(item, selEnd, rEnd))
            }
            selStart <= rStart && selEnd < rEnd -> {
                result.add(AnnotatedString.Range(item, selEnd, rEnd))
            }
            else -> {
                result.add(AnnotatedString.Range(item, rStart, selStart))
            }
        }
    }
    return result
}

fun List<AnnotatedString.Range<SpanStyle>>.normalize(): List<AnnotatedString.Range<SpanStyle>> {
    if (size <= 1) return this
    val grouped = this.filter { it.start < it.end }.groupBy { it.item }
    val result = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for ((style, ranges) in grouped) {
        val sorted = ranges.sortedBy { it.start }
        var current: AnnotatedString.Range<SpanStyle>? = null
        for (range in sorted) {
            if (current == null) {
                current = range
            } else if (current.end >= range.start) {
                current = AnnotatedString.Range(style, current.start, maxOf(current.end, range.end))
            } else {
                result.add(current)
                current = range
            }
        }
        if (current != null) {
            result.add(current)
        }
    }
    return result.sortedBy { it.start }
}

fun SpanStyle.withoutBold(): SpanStyle? {
    val weight = fontWeight
    if (weight == null || weight.weight < 700) return this
    val newStyle = copy(fontWeight = null)
    return if (newStyle.hasAnyStyle()) newStyle else null
}

fun SpanStyle.withoutItalic(): SpanStyle? {
    if (fontStyle == null) return this
    val newStyle = copy(fontStyle = null)
    return if (newStyle.hasAnyStyle()) newStyle else null
}

fun SpanStyle.withoutMonospace(): SpanStyle? {
    if (fontFamily == null) return this
    val newStyle = copy(fontFamily = null)
    return if (newStyle.hasAnyStyle()) newStyle else null
}

fun SpanStyle.withoutStrikethrough(): SpanStyle? {
    val decoration = textDecoration ?: return this
    if (!decoration.contains(TextDecoration.LineThrough)) return this
    val hasUnderline = decoration.contains(TextDecoration.Underline)
    val newDecoration = if (hasUnderline) TextDecoration.Underline else null
    val newStyle = copy(textDecoration = newDecoration)
    return if (newStyle.hasAnyStyle()) newStyle else null
}

private fun SpanStyle.hasAnyStyle(): Boolean {
    return fontWeight != null ||
        fontStyle != null ||
        textDecoration != null ||
        fontFamily != null ||
        color != ComposeColor.Unspecified ||
        background != ComposeColor.Unspecified ||
        fontSize != androidx.compose.ui.unit.TextUnit.Unspecified ||
        letterSpacing != androidx.compose.ui.unit.TextUnit.Unspecified ||
        shadow != null
}

fun buildAnnotatedString(
    text: String,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    urlAnnotations: List<AnnotatedString.Range<String>> = emptyList(),
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (range in spanStyles) {
        val s = range.start.coerceIn(0, text.length)
        val e = range.end.coerceIn(0, text.length)
        if (s < e) {
            builder.addStyle(range.item, s, e)
        }
    }
    for (range in urlAnnotations) {
        val s = range.start.coerceIn(0, text.length)
        val e = range.end.coerceIn(0, text.length)
        if (s < e) {
            builder.addStringAnnotation("URL", range.item, s, e)
        }
    }
    return builder.toAnnotatedString()
}

fun AnnotatedString.toHtml(): String {
    if (text.isEmpty()) return ""

    val sortedStyles = spanStyles.filter { it.start < it.end }
    val sortedUrls = getStringAnnotations("URL", 0, text.length).filter { it.start < it.end }

    val lines = text.split('\n')
    val sb = StringBuilder()
    var lineStart = 0

    for (line in lines) {
        val lineEnd = lineStart + line.length
        sb.append("<p dir=\"ltr\">")
        if (line.isEmpty()) {
            sb.append("<br>")
        } else {
            val boundaries = java.util.TreeSet<Int>()
            boundaries.add(lineStart)
            boundaries.add(lineEnd)
            for (range in sortedStyles) {
                if (range.start in lineStart..lineEnd) boundaries.add(range.start)
                if (range.end in lineStart..lineEnd) boundaries.add(range.end)
            }
            for (range in sortedUrls) {
                if (range.start in lineStart..lineEnd) boundaries.add(range.start)
                if (range.end in lineStart..lineEnd) boundaries.add(range.end)
            }

            var curr = lineStart
            for (boundary in boundaries) {
                if (boundary > curr) {
                    val segmentText = text.substring(curr, boundary)
                    val activeStyles =
                        sortedStyles.filter { it.start < boundary && it.end > curr }.map { it.item }
                    val activeUrl =
                        sortedUrls.firstOrNull { it.start < boundary && it.end > curr }?.item

                    val isBold = activeStyles.any {
                        it.fontWeight == FontWeight.Bold || (it.fontWeight?.weight ?: 0) >= 700
                    }
                    val isItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
                    val isStrike = activeStyles.any {
                        it.textDecoration?.contains(TextDecoration.LineThrough) == true
                    }
                    val isMonospace = activeStyles.any { it.fontFamily == FontFamily.Monospace }

                    var formatted = segmentText.escapeHtml()
                    if (isMonospace) formatted = "<tt>$formatted</tt>"
                    if (isStrike) formatted = "<s>$formatted</s>"
                    if (isItalic) formatted = "<i>$formatted</i>"
                    if (isBold) formatted = "<b>$formatted</b>"
                    if (activeUrl != null) formatted = "<a href=\"$activeUrl\">$formatted</a>"

                    sb.append(formatted)
                    curr = boundary
                }
            }
        }
        sb.append("</p>")
        lineStart = lineEnd + 1
    }

    return sb.toString()
}

private fun String.escapeHtml(): String {
    return buildString(length) {
        for (c in this@escapeHtml) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }
}
