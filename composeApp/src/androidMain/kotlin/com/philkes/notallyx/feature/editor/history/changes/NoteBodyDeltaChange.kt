package com.philkes.notallyx.feature.editor.history.changes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.Change
import com.philkes.notallyx.feature.editor.history.replaceRangeWithSpans

class NoteBodyDeltaChange(
    private val notallyModel: NoteModel,
    var startOffset: Int,
    var deletedText: AnnotatedString,
    var insertedText: AnnotatedString,
    var oldSelection: TextRange,
    var newSelection: TextRange,
    var timestamp: Long = System.currentTimeMillis(),
    private val onBodyUpdated: (TextFieldValue) -> Unit = {},
) : Change {

    override fun undo() {
        val current = notallyModel.body.annotatedString
        val restored =
            current.replaceRangeWithSpans(
                start = startOffset,
                end = startOffset + insertedText.length,
                replacement = deletedText,
            )
        val safeSelection = oldSelection.coerceIn(0, restored.length)
        val value = TextFieldValue(restored, selection = safeSelection)
        notallyModel.body = value
        onBodyUpdated(value)
    }

    override fun redo() {
        val current = notallyModel.body.annotatedString
        val applied =
            current.replaceRangeWithSpans(
                start = startOffset,
                end = startOffset + deletedText.length,
                replacement = insertedText,
            )
        val safeSelection = newSelection.coerceIn(0, applied.length)
        val value = TextFieldValue(applied, selection = safeSelection)
        notallyModel.body = value
        onBodyUpdated(value)
    }

    fun tryMergeWith(
        next: NoteBodyDeltaChange,
        coalesceWindowMs: Long = DEFAULT_COALESCE_WINDOW_MS,
    ): Boolean {
        val isSequentialInsert =
            this.deletedText.isEmpty() &&
                next.deletedText.isEmpty() &&
                next.insertedText.length == 1 &&
                next.startOffset == this.startOffset + this.insertedText.length &&
                next.insertedText.text != "\n" &&
                next.timestamp - this.timestamp < coalesceWindowMs

        if (isSequentialInsert) {
            this.insertedText = this.insertedText + next.insertedText
            this.newSelection = next.newSelection
            this.timestamp = next.timestamp
            return true
        }
        return false
    }

    private fun TextRange.coerceIn(min: Int, max: Int): TextRange {
        return TextRange(
            start.coerceIn(min, max),
            end.coerceIn(min, max),
        )
    }

    companion object {
        const val DEFAULT_COALESCE_WINDOW_MS = 1000L
    }
}
