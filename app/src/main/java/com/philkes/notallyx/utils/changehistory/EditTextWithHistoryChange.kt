package com.philkes.notallyx.utils.changehistory

import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.core.text.getSpans
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.presentation.applySpans
import com.philkes.notallyx.presentation.clone
import com.philkes.notallyx.presentation.view.misc.StylableEditTextWithHistory
import com.philkes.notallyx.presentation.view.misc.highlightableview.HighlightSpan
import com.philkes.notallyx.utils.CompressUtility

class EditTextWithHistoryChange(
    private val editText: StylableEditTextWithHistory,
    before: EditTextState,
    after: EditTextState,
    private val updateModel: (newValue: Editable) -> Unit,
) : ValueChange<EditTextState>(before, after) {

    override fun update(value: EditTextState, isUndo: Boolean) {
        editText.applyWithoutTextWatcher {
            val text = value.getEditableText().withoutSpans<HighlightSpan>()
            setText(text)
            updateModel.invoke(text)
            requestFocus()
            setSelection(value.cursorPos)
        }
    }
}

/**
 * Represents the state of an EditText, storing either the full text or a compressed version for
 * large text to reduce memory usage.
 */
class EditTextState(text: Editable, val cursorPos: Int) {
    companion object {}

    // Either Editable (for small text) or ByteArray (compressed, for large text and spans)
    private val textContent: Any

    init {
        // Extract spans from the Editable
        // Compress text and spans together
        this.textContent =
            if (text.length > CompressUtility.COMPRESSION_THRESHOLD) {
                // Extract spans from the Editable
                val spans = extractSpansFromEditable(text)
                // Compress text and spans together
                CompressUtility.compressTextAndSpans(
                    text.toString(),
                    spans as List<SpanRepresentation>,
                )
            } else {
                text
            }
    }

    /** Extracts spans from an Editable and converts them to SpanRepresentation objects. */
    private fun extractSpansFromEditable(text: Editable): List<SpanRepresentation> {
        val representations = mutableListOf<SpanRepresentation>()

        text.getSpans(0, text.length, CharacterStyle::class.java).forEach { span ->
            val end = text.getSpanEnd(span)
            val start = text.getSpanStart(span)

            // Skip invalid spans
            if (start < 0 || end < 0 || start >= text.length || end > text.length) {
                return@forEach
            }

            val representation =
                SpanRepresentation(
                    start = start,
                    end = end,
                    bold = false,
                    link = false,
                    linkData = null,
                    italic = false,
                    monospace = false,
                    strikethrough = false,
                )

            when (span) {
                is StyleSpan ->
                    when (span.style) {
                        Typeface.BOLD -> representation.bold = true
                        Typeface.ITALIC -> representation.italic = true
                        Typeface.BOLD_ITALIC -> {
                            representation.bold = true
                            representation.italic = true
                        }
                    }

                is URLSpan -> {
                    representation.link = true
                    representation.linkData = span.url
                }
                is TypefaceSpan -> {
                    if (span.family == "monospace") {
                        representation.monospace = true
                    }
                }
                is StrikethroughSpan -> {
                    representation.strikethrough = true
                }
            }

            if (representation.isNotUseless()) {
                representations.add(representation)
            }
        }

        return representations
    }

    /** Returns the Editable text, decompressing it if necessary and applying spans. */
    fun getEditableText(): Editable {
        return when (textContent) {
            is Editable -> textContent
            is ByteArray -> {
                val (text, spans) = CompressUtility.decompressTextAndSpans(textContent)
                text.applySpans(spans)
            }
            else -> SpannableStringBuilder()
        }
    }
}

inline fun <reified T : Any> Editable.withoutSpans(): Editable =
    clone().apply { this.getSpans<T>().forEach { removeSpan(it) } }
