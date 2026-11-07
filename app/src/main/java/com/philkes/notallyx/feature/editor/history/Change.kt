package com.philkes.notallyx.feature.editor.history

interface Change {
    fun redo()

    fun undo()
}
