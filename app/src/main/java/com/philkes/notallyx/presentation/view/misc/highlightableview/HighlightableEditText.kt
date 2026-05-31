package com.philkes.notallyx.presentation.view.misc.highlightableview

import android.content.Context
import android.text.Spanned
import android.text.style.CharacterStyle
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.philkes.notallyx.presentation.removeSelectionFromSpans
import com.philkes.notallyx.presentation.view.misc.EditTextWithWatcher
import com.philkes.notallyx.presentation.withAlpha

private const val SEARCH_HIGHLIGHT_LIMIT = 1_000

/**
 * [AppCompatEditText] whose changes (text edits or span changes) are pushed to [changeHistory].
 * *
 */
open class HighlightableEditText(context: Context, attrs: AttributeSet) :
    EditTextWithWatcher(context, attrs) {

    fun getSpanRange(span: CharacterStyle): Pair<Int, Int> {
        val text = super.getText()!!
        return Pair(text.getSpanStart(span), text.getSpanEnd(span))
    }

    /**
     * Removes [span] from `text`.
     *
     * @param removeText if this is `true` the text of the [span] is removed from `text`.
     */
    protected fun removeSpan(span: CharacterStyle, removeText: Boolean = false) {
        val (start, end) = getSpanRange(span)
        if (span is HighlightSpan) {
            text?.removeSpan(span)
        } else {
            text?.removeSelectionFromSpans(start, end)
        }
        if (removeText) {
            text?.delete(start, end)
        }
    }

    protected fun applySpan(
        span: CharacterStyle,
        start: Int = selectionStart,
        end: Int = selectionEnd,
    ) {
        text?.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private val highlightedSpans: MutableList<CharacterStyle> = mutableListOf()
    private var selectedHighlightedSpan: CharacterStyle? = null

    fun clearHighlights() {
        highlightedSpans.apply {
            forEach { span -> removeSpan(span) }
            clear()
        }
        selectedHighlightedSpan = null
    }

    /**
     * Highlights all occurrences and returns the Y-offset of the selected match for scrolling
     * purposes.
     */
    fun highlight(newOccurrences: List<Pair<Int, Int>>, selectedIndex: Int = -1) {
        val editable = text ?: return
        highlightedSpans.forEach { editable.removeSpan(it) }
        highlightedSpans.clear()
        selectedHighlightedSpan = null

        if (newOccurrences.isEmpty()) return

        val nonSelectedColor = highlightColor.withAlpha(0.1f)
        val selectedColor = highlightColor

        newOccurrences.take(SEARCH_HIGHLIGHT_LIMIT).forEachIndexed { index, (start, end) ->
            if (start >= 0 && end <= editable.length) {
                val isSelected = (index == selectedIndex)
                val span = HighlightSpan(if (isSelected) selectedColor else nonSelectedColor)

                applySpan(span, start, end)
                highlightedSpans.add(span)

                if (isSelected) {
                    selectedHighlightedSpan = span
                }
            }
        }
    }

    /**
     * Updates the selection state for a specific range without redrawing all spans. Returns the
     * Y-offset of the newly selected range.
     */
    fun select(startIdx: Int, endIdx: Int): Int? {
        val editable = text ?: return null
        // 1. Revert the previous selection to a normal highlight
        selectedHighlightedSpan?.let { oldSpan ->
            val oldStart = editable.getSpanStart(oldSpan)
            val oldEnd = editable.getSpanEnd(oldSpan)

            if (oldStart != -1 && oldEnd != -1) {
                editable.removeSpan(oldSpan)
                val dimmedSpan = HighlightSpan(highlightColor.withAlpha(0.1f))
                editable.setSpan(dimmedSpan, oldStart, oldEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                highlightedSpans.add(dimmedSpan)
            }
        }

        // 2. Find and upgrade the span at the new indices
        // We look for any existing HighlightSpan at this exact location to "upgrade" it
        val existingSpans = editable.getSpans(startIdx, endIdx, HighlightSpan::class.java)
        existingSpans.forEach {
            if (editable.getSpanStart(it) == startIdx && editable.getSpanEnd(it) == endIdx) {
                editable.removeSpan(it)
                highlightedSpans.remove(it)
            }
        }

        // 3. Create and apply the new "Selected" span
        val newSelectedSpan = HighlightSpan(highlightColor)
        editable.setSpan(newSelectedSpan, startIdx, endIdx, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        selectedHighlightedSpan = newSelectedSpan
        highlightedSpans.add(newSelectedSpan)

        // 4. Return Y-position for scrolling
        return layout?.let {
            val line = it.getLineForOffset(startIdx)
            it.getLineTop(line)
        }
    }

    fun unselectHighlight() {
        selectedHighlightedSpan?.unselect()
    }

    private fun CharacterStyle.unselect() {
        val (previousHighlightedStartIdx, previousHighlightedEndIdx) = getSpanRange(this)
        if (previousHighlightedStartIdx != -1) {
            removeSpan(this)
            highlight(listOf(Pair(previousHighlightedStartIdx, previousHighlightedEndIdx)), -1)
        }
    }
}
