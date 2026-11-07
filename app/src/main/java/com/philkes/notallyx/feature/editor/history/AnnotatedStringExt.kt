package com.philkes.notallyx.feature.editor.history

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange

fun AnnotatedString.replaceRangeWithSpans(
    start: Int,
    end: Int,
    replacement: AnnotatedString,
): AnnotatedString {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(safeStart, length)
    val builder = AnnotatedString.Builder()

    if (safeStart > 0) {
        builder.append(this.subSequence(0, safeStart))
    }
    builder.append(replacement)
    if (safeEnd < length) {
        builder.append(this.subSequence(safeEnd, length))
    }

    return builder.toAnnotatedString()
}

fun findDifferingRange(oldStr: AnnotatedString, newStr: AnnotatedString): Pair<Int, Int> {
    var minStart = Int.MAX_VALUE
    var maxEnd = Int.MIN_VALUE

    val oldSpans = oldStr.spanStyles.toSet()
    val newSpans = newStr.spanStyles.toSet()
    for (span in oldSpans) {
        if (span !in newSpans) {
            minStart = minOf(minStart, span.start)
            maxEnd = maxOf(maxEnd, span.end)
        }
    }
    for (span in newSpans) {
        if (span !in oldSpans) {
            minStart = minOf(minStart, span.start)
            maxEnd = maxOf(maxEnd, span.end)
        }
    }

    val oldParagraphs = oldStr.paragraphStyles.toSet()
    val newParagraphs = newStr.paragraphStyles.toSet()
    for (span in oldParagraphs) {
        if (span !in newParagraphs) {
            minStart = minOf(minStart, span.start)
            maxEnd = maxOf(maxEnd, span.end)
        }
    }
    for (span in newParagraphs) {
        if (span !in oldParagraphs) {
            minStart = minOf(minStart, span.start)
            maxEnd = maxOf(maxEnd, span.end)
        }
    }

    val oldUrls = oldStr.getStringAnnotations("URL", 0, oldStr.length).toSet()
    val newUrls = newStr.getStringAnnotations("URL", 0, newStr.length).toSet()
    for (url in oldUrls) {
        if (url !in newUrls) {
            minStart = minOf(minStart, url.start)
            maxEnd = maxOf(maxEnd, url.end)
        }
    }
    for (url in newUrls) {
        if (url !in oldUrls) {
            minStart = minOf(minStart, url.start)
            maxEnd = maxOf(maxEnd, url.end)
        }
    }

    if (minStart == Int.MAX_VALUE || maxEnd == Int.MIN_VALUE) {
        return Pair(0, oldStr.text.length)
    }

    val start = minStart.coerceIn(0, oldStr.text.length)
    val end = maxEnd.coerceIn(start, oldStr.text.length)
    if (start == end) {
        return Pair(0, oldStr.text.length)
    }
    return Pair(start, end)
}

fun computeBodyDelta(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    notallyModel: NoteModel,
    onBodyUpdated: (TextFieldValue) -> Unit = {},
): NoteBodyDeltaChange? {
    val oldStr = oldValue.annotatedString
    val newStr = newValue.annotatedString
    if (oldStr == newStr) return null

    val oldText = oldStr.text
    val newText = newStr.text

    if (oldText == newText) {
        val (diffStart, diffEnd) = findDifferingRange(oldStr, newStr)
        val deletedSlice = oldStr.subSequence(diffStart, diffEnd)
        val insertedSlice = newStr.subSequence(diffStart, diffEnd)

        return NoteBodyDeltaChange(
            notallyModel = notallyModel,
            startOffset = diffStart,
            deletedText = deletedSlice,
            insertedText = insertedSlice,
            oldSelection = oldValue.selection,
            newSelection = newValue.selection,
            onBodyUpdated = onBodyUpdated,
        )
    }

    var prefix = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefix < minLen && oldText[prefix] == newText[prefix]) {
        prefix++
    }

    var oldSuffix = oldText.length - 1
    var newSuffix = newText.length - 1
    while (oldSuffix >= prefix && newSuffix >= prefix && oldText[oldSuffix] == newText[newSuffix]) {
        oldSuffix--
        newSuffix--
    }

    val deletedSlice = oldStr.subSequence(prefix, oldSuffix + 1)
    val insertedSlice = newStr.subSequence(prefix, newSuffix + 1)

    return NoteBodyDeltaChange(
        notallyModel = notallyModel,
        startOffset = prefix,
        deletedText = deletedSlice,
        insertedText = insertedSlice,
        oldSelection = oldValue.selection,
        newSelection = newValue.selection,
        onBodyUpdated = onBodyUpdated,
    )
}
