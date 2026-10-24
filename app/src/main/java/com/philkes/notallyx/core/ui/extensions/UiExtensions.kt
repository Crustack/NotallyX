package com.philkes.notallyx.core.ui.extensions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getUrl
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.main.sorting.compareColor
import com.philkes.notallyx.feature.main.sorting.compareCreated
import com.philkes.notallyx.feature.main.sorting.compareModified
import com.philkes.notallyx.feature.main.sorting.compareTitle
import com.philkes.notallyx.feature.migration.backup.NotesAndAttachments
import com.philkes.notallyx.navigation.NotallyNavKey
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles

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

@Deprecated("Legacy Bold StyleSpan for old views")
fun createBoldSpan() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        StyleSpan(Typeface.BOLD, FontWeight.Bold.weight)
    } else {
        StyleSpan(Typeface.BOLD)
    }

/**
 * Adjusts or removes spans based on the selection range.
 *
 * @param selectionStart the start index of the selection
 * @param selectionEnd the end index of the selection
 */
fun Editable.removeSelectionFromSpans(
    selectionStart: Int,
    selectionEnd: Int,
    spans: Collection<CharacterStyle> =
        getSpans(selectionStart, selectionEnd, CharacterStyle::class.java).toList(),
) {
    for (span in spans) {
        val spanStart = getSpanStart(span)
        val spanEnd = getSpanEnd(span)

        when {
            // Case 1: Selection is exactly the span's range (remove entire span)
            selectionStart <= spanStart && selectionEnd >= spanEnd -> {
                removeSpan(span)
            }
            // Case 2: Selection is part of the span (split the span)
            selectionStart > spanStart && selectionEnd < spanEnd -> {
                // Remove the original span
                removeSpan(span)
                // Reapply two new spans: one for the part before the selection, and one for the
                // part after
                val newSpan = span.cloneSpan()
                setSpan(span, spanStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(newSpan, selectionEnd, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Case 3: Selection overlaps the start of the span (trim the span)
            selectionStart <= spanStart && selectionEnd < spanEnd -> {
                removeSpan(span)
                setSpan(span, selectionEnd, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Case 4: Selection overlaps the end of the span (trim the span)
            else -> {
                removeSpan(span)
                setSpan(span, spanStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

fun CharacterStyle.cloneSpan(): CharacterStyle =
    when (this) {
        is StyleSpan -> StyleSpan(this.style)
        is StrikethroughSpan -> StrikethroughSpan()
        is TypefaceSpan -> TypefaceSpan(this.family)
        is URLSpan -> URLSpan(this.url)
        else -> this
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

fun Menu.add(
    title: Int,
    drawable: Int,
    showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM,
    groupId: Int = Menu.NONE,
    itemId: Int = Menu.NONE,
    order: Int = Menu.NONE,
    onClick: (item: MenuItem) -> Unit,
): MenuItem {
    val menuItem =
        add(groupId, itemId, order, title).setIcon(drawable).setOnMenuItemClickListener { item ->
            onClick(item)
            item.isChecked = true
            return@setOnMenuItemClickListener false
        }
    menuItem.setShowAsAction(showAsAction)
    return menuItem
}

val Int.toDp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Float.toDpFloat: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

fun View.getString(id: Int, vararg formatArgs: String): String {
    return context.resources.getString(id, *formatArgs)
}

fun View.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any): String {
    return context.getQuantityString(id, quantity, *formatArgs)
}

fun Folder.movedToResId(): Int {
    return when (this) {
        Folder.DELETED -> R.plurals.deleted_selected_notes
        Folder.ARCHIVED -> R.plurals.archived_selected_notes
        Folder.NOTES -> R.plurals.restored_selected_notes
    }
}

fun RadioGroup.checkedTag(): Any? {
    return this.findViewById<RadioButton?>(this.checkedRadioButtonId)?.tag
}

fun Context.showKeyboard(view: View) {
    ContextCompat.getSystemService(this, InputMethodManager::class.java)
        ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.hideKeyboard(view: View) {
    ContextCompat.getSystemService(this, InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(view.windowToken, 0)
}

inline fun LifecycleOwner.repeatOnLifecycleScope(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit,
) = lifecycleScope.launch { repeatOnLifecycle(state) { block() } }

inline fun <T> Flow<T>.collectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit,
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minActiveState) { collect { action(it) } }
    }
}

fun Activity.checkNotificationPermission(
    requestCode: Int,
    alsoCheckAlarmPermission: Boolean = false,
    alarmPermissionResultLauncher: ActivityResultLauncher<Intent>? = null,
    onShowRationale: ((messageResId: Int, onConfirm: () -> Unit) -> Unit)? = null,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                val onConfirm = { requestPermissions(arrayOf(permission), requestCode) }
                if (onShowRationale != null) {
                    onShowRationale(R.string.please_grant_notally_notification, onConfirm)
                } else {
                    onConfirm()
                }
            } else requestPermissions(arrayOf(permission), requestCode)
        } else if (alsoCheckAlarmPermission)
            checkAlarmPermission(alarmPermissionResultLauncher, onShowRationale, onSuccess)
        else onSuccess()
    } else if (alsoCheckAlarmPermission)
        checkAlarmPermission(alarmPermissionResultLauncher, onShowRationale, onSuccess)
    else onSuccess()
}

fun Activity.checkAlarmPermission(
    resultLauncher: ActivityResultLauncher<Intent>?,
    onShowRationale: ((messageResId: Int, onConfirm: () -> Unit) -> Unit)? = null,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (getSystemService<AlarmManager>()!!.canScheduleExactAlarms()) {
            onSuccess()
        } else {
            val onConfirm = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:$packageName")
                if (resultLauncher != null) {
                    resultLauncher.launch(intent)
                } else {
                    startActivity(intent)
                }
            }
            if (onShowRationale != null) {
                onShowRationale(R.string.please_grant_notally_alarm, onConfirm)
            } else {
                onConfirm()
            }
        }
    } else onSuccess()
}

fun Activity.setEnabledSecureFlag(enabled: Boolean) {
    if (enabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

fun Date.format(
    dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    timeFormat: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    ensureFullFormat: Boolean = false,
): String {
    val (effectiveDateFormat, effectiveTimeFormat) =
        if (ensureFullFormat && dateFormat != DateFormat.RELATIVE) {
            Pair(
                dateFormat.takeIf { it != DateFormat.NONE } ?: DateFormat.DD_MM_YY_GER,
                timeFormat.takeIf { it != TimeFormat.NONE } ?: TimeFormat.TWENTY_FOUR_H,
            )
        } else Pair(dateFormat, timeFormat)
    if (effectiveDateFormat == DateFormat.NONE && effectiveTimeFormat == TimeFormat.NONE) {
        return ""
    }
    val datePart = effectiveDateFormat.format(this)
    val timePart = effectiveTimeFormat.format(this)

    return if (datePart.isNotEmpty() && timePart.isNotEmpty()) {
        "$datePart $timePart"
    } else {
        datePart + timePart
    }
}

fun Context.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any): String {
    return resources.getQuantityString(id, quantity, quantity, *formatArgs)
}

fun Context.getQuantityStringPlain(id: Int, quantity: Int): String {
    return resources.getQuantityString(id, quantity)
}

@ColorInt
fun @receiver:ColorInt Int.withAlpha(alpha: Float): Int {
    return Color.argb(
        (255 * alpha).toInt(),
        Color.red(this),
        Color.green(this),
        Color.blue(this),
    )
}

fun Context.getColorFromAttr(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    val resolved = theme.resolveAttribute(attr, typedValue, true)
    if (!resolved) {
        throw IllegalArgumentException("Attribute not found in current theme")
    }
    return if (typedValue.resourceId != 0) {
        // It's a reference (@color/something), resolve it properly
        ContextCompat.getColor(this, typedValue.resourceId)
    } else {
        // It's a direct color value
        typedValue.data
    }
}

var counter = 0

private class HandleDescriptor(val resourceFieldName: String, val selectHandleFieldName: String)

private val handles =
    arrayOf(
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleRes",
            selectHandleFieldName = "mSelectHandleCenter",
        ),
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleLeftRes",
            selectHandleFieldName = "mSelectHandleLeft",
        ),
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleRightRes",
            selectHandleFieldName = "mSelectHandleRight",
        ),
    )

fun Drawable.withTint(@ColorInt color: Int): Drawable {
    val oldDrawable = DrawableCompat.unwrap<Drawable>(this)
    val newDrawable = oldDrawable.constantState?.newDrawable()?.mutate() ?: oldDrawable.mutate()
    val wrappedDrawable = DrawableCompat.wrap(newDrawable)
    DrawableCompat.setTint(wrappedDrawable, color)
    return wrappedDrawable
}

@ColorInt
fun Context.getContrastFontColor(@ColorInt backgroundColor: Int): Int {
    return if (backgroundColor.isLightColor()) ContextCompat.getColor(this, R.color.TextDark)
    else ContextCompat.getColor(this, R.color.TextLight)
}

fun @receiver:ColorInt Int.isLightColor() = ColorUtils.calculateLuminance(this) > 0.5

@ColorInt
fun @receiver:ColorInt Int.getSlightContrastColor(): Int {
    val overlayColor = if (this.isLightColor()) Color.BLACK else Color.WHITE
    // Composite black/white over the background color
    return ColorUtils.compositeColors(
        ColorUtils.setAlphaComponent(overlayColor, 15), // ~10% alpha
        this,
    )
}

fun Fragment.showToast(messageResId: Int) = requireContext().showToast(messageResId)

fun Context.showToast(messageResId: Int) =
    ContextCompat.getMainExecutor(this).execute {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show()
    }

fun Context.showToast(message: CharSequence) =
    ContextCompat.getMainExecutor(this).execute {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

fun View.showSnackbar(
    msgResId: Int,
    actionResId: Int? = null,
    onActionListener: View.OnClickListener? = null,
) = showSnackbar(getString(msgResId), actionResId, onActionListener)

fun View.showSnackbar(
    message: String,
    actionResId: Int? = null,
    onActionListener: View.OnClickListener? = null,
) =
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .apply { actionResId?.let { setAction(it, onActionListener) } }
        .show()

fun Context.restartApplication(
    destinationToOpen: NotallyNavKey? = null,
    extra: Pair<String, Boolean>? = null,
) {
    val intent =
        Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            destinationToOpen?.let { putNavKey(it) }
            extra?.let { (key, value) -> putExtra(key, value) }
        }
    startActivity(intent)
}

@ColorInt
fun Context.extractColor(color: String): Int {
    return when (color) {
        BaseNote.COLOR_DEFAULT -> return getColorFromAttr(R.attr.colorSurface)
        else -> Color.parseColor(color)
    }
}

fun ViewGroup.addFastScroll(context: Context, @ColorInt colorInt: Int) {
    FastScrollerBuilder(this).useColoredStyle(context, colorInt).build()
}

fun FastScrollerBuilder.useColoredStyle(
    context: Context,
    @ColorInt colorInt: Int,
): FastScrollerBuilder {
    AppCompatResources.getDrawable(context, me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb)
        ?.mutate()
        ?.let {
            DrawableCompat.setTint(it, colorInt)
            setThumbDrawable(it)
        }
    ContextCompat.getDrawable(context, R.drawable.scroll_track)?.let { setTrackDrawable(it) }
    setPadding(0, 0, 2.toDp, 0)
    setPopupStyle(PopupStyles.MD2)
    return this
}

fun Window.setLightStatusAndNavBar(value: Boolean, view: View = decorView) {
    val windowInsetsControllerCompat = WindowInsetsControllerCompat(this, view)
    windowInsetsControllerCompat.isAppearanceLightStatusBars = value
    windowInsetsControllerCompat.isAppearanceLightNavigationBars = value
}

private fun getOutlinedDrawable(context: Context): MaterialShapeDrawable {
    val model =
        ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

    return MaterialShapeDrawable(model).apply {
        fillColor = ColorStateList.valueOf(0)
        strokeWidth = context.resources.displayMetrics.density
        strokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke)
    }
}

fun RecyclerView.initListView(context: Context) {
    setHasFixedSize(true)
    layoutManager = LinearLayoutManager(context)
    addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
    setPadding(0, 0, 0, 0)
}

val RecyclerView.focusedViewHolder
    get() = focusedChild?.let { view ->
        val position = getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            null
        } else {
            findViewHolderForAdapterPosition(position)
        }
    }

fun MaterialAutoCompleteTextView.select(value: CharSequence) {
    setText(value, false)
}

fun Context.exportedText(notesAndAttachments: NotesAndAttachments) =
    "${getString(R.string.exported)} ${notesAndAttachments.first} ${if(notesAndAttachments.first == 1) getString(R.string.note) else getString(R.string.notes)} (${notesAndAttachments.second} ${getQuantityString(R.plurals.attachments, notesAndAttachments.second)})"

fun Context.createItemsFromNotes(list: List<BaseNote>, notesSort: NotesSort? = null): List<Item> {
    val sortedList = if (notesSort != null) list.sortWith(notesSort) else list
    val pinned by lazy { Header(getString(R.string.pinned)) }
    val others by lazy { Header(getString(R.string.others)) }
    val archived by lazy { Header(getString(R.string.archived)) }
    if (sortedList.isEmpty()) {
        return sortedList
    } else {
        val firstPinnedNote = sortedList.indexOfFirst { baseNote -> baseNote.pinned }
        val firstUnpinnedNote = sortedList.indexOfFirst { baseNote ->
            !baseNote.pinned && baseNote.folder != Folder.ARCHIVED
        }
        val mutableList: MutableList<Item> = sortedList.toMutableList()
        if (firstPinnedNote != -1) {
            mutableList.add(firstPinnedNote, pinned)
            if (firstUnpinnedNote != -1) {
                mutableList.add(firstUnpinnedNote + 1, others)
            }
        }
        val firstArchivedNote = mutableList.indexOfFirst { item ->
            item is BaseNote && item.folder == Folder.ARCHIVED
        }
        if (firstArchivedNote != -1) {
            mutableList.add(firstArchivedNote, archived)
        }
        return mutableList
    }
}

fun List<BaseNote>.sortWith(notesSort: NotesSort): List<BaseNote> {
    val comparator =
        Comparator<BaseNote> { note1, note2 ->
            val pinnedCompare = note2.pinned.compareTo(note1.pinned)
            if (pinnedCompare != 0) return@Comparator pinnedCompare

            val primaryCompare =
                when (notesSort.sortedBy) {
                    NotesSortBy.TITLE -> note1.compareTitle(note2)
                    NotesSortBy.CREATION_DATE -> note1.compareCreated(note2)
                    NotesSortBy.MODIFIED_DATE -> note1.compareModified(note2)
                    NotesSortBy.COLOR -> note1.compareColor(note2)
                }
            val dir = if (notesSort.sortDirection == SortDirection.ASC) 1 else -1
            if (primaryCompare != 0) primaryCompare * dir
            else note2.timestamp.compareTo(note1.timestamp)
        }
    return sortedWith(comparator)
}
