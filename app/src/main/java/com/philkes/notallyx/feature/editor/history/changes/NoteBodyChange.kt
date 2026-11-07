package com.philkes.notallyx.feature.editor.history.changes

import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.feature.editor.NoteModel

class NoteBodyChange(
    private val notallyModel: NoteModel,
    oldBody: TextFieldValue,
    newBody: TextFieldValue,
    private val onBodyUpdated: (TextFieldValue) -> Unit = {},
) : ValueChange<TextFieldValue>(oldBody, newBody) {

    override fun update(value: TextFieldValue, isUndo: Boolean) {
        notallyModel.body = value
        onBodyUpdated(value)
    }
}
