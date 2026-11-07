package com.philkes.notallyx.feature.editor.history.changes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.Change
import com.philkes.notallyx.feature.editor.history.replaceRangeWithSpans

class ListItemBodyDeltaChange(
    private val notallyModel: NoteModel,
    val itemId: Int,
    var startOffset: Int,
    var deletedText: AnnotatedString,
    var insertedText: AnnotatedString,
    var oldSelection: TextRange = TextRange.Zero,
    var newSelection: TextRange = TextRange.Zero,
    var timestamp: Long = System.currentTimeMillis(),
    private val onItemUpdated: (ListItem, AnnotatedString) -> Unit = { _, _ -> },
) : Change {

    constructor(
        notallyModel: NoteModel,
        itemId: Int,
        startOffset: Int,
        deletedText: AnnotatedString,
        insertedText: AnnotatedString,
        oldSelection: TextRange = TextRange.Zero,
        newSelection: TextRange = TextRange.Zero,
        timestamp: Long = System.currentTimeMillis(),
        onItemUpdated: (ListItem) -> Unit,
    ) : this(
        notallyModel = notallyModel,
        itemId = itemId,
        startOffset = startOffset,
        deletedText = deletedText,
        insertedText = insertedText,
        oldSelection = oldSelection,
        newSelection = newSelection,
        timestamp = timestamp,
        onItemUpdated = { item, _ -> onItemUpdated(item) },
    )

    constructor(
        notallyModel: NoteModel,
        itemId: Int,
        startOffset: Int,
        deletedText: String,
        insertedText: String,
        oldSelection: TextRange = TextRange.Zero,
        newSelection: TextRange = TextRange.Zero,
        timestamp: Long = System.currentTimeMillis(),
        onItemUpdated: (ListItem) -> Unit = {},
    ) : this(
        notallyModel = notallyModel,
        itemId = itemId,
        startOffset = startOffset,
        deletedText = AnnotatedString(deletedText),
        insertedText = AnnotatedString(insertedText),
        oldSelection = oldSelection,
        newSelection = newSelection,
        timestamp = timestamp,
        onItemUpdated = { item, _ -> onItemUpdated(item) },
    )

    override fun undo() {
        val item = notallyModel.items.find { it.id == itemId } ?: return
        val current = AnnotatedString(item.body)
        val restored =
            current.replaceRangeWithSpans(
                start = startOffset,
                end = startOffset + insertedText.length,
                replacement = deletedText,
            )
        item.body = restored.text
        onItemUpdated(item, restored)
    }

    override fun redo() {
        val item = notallyModel.items.find { it.id == itemId } ?: return
        val current = AnnotatedString(item.body)
        val applied =
            current.replaceRangeWithSpans(
                start = startOffset,
                end = startOffset + deletedText.length,
                replacement = insertedText,
            )
        item.body = applied.text
        onItemUpdated(item, applied)
    }

    fun tryMergeWith(
        next: ListItemBodyDeltaChange,
        coalesceWindowMs: Long = DEFAULT_COALESCE_WINDOW_MS,
    ): Boolean {
        val isSameItem = this.itemId == next.itemId
        val isSequentialInsert =
            isSameItem &&
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

    companion object {
        const val DEFAULT_COALESCE_WINDOW_MS = 1000L
    }
}
