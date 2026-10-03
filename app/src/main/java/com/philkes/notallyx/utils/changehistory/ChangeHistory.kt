package com.philkes.notallyx.utils.changehistory

import android.util.Log
import kotlin.IllegalStateException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChangeHistory(
    /** Maximum number of changes to keep in memory. Oldest entries are evicted when full. */
    private val maxSize: Int = 1000
) {
    private val changeStack = ArrayList<Change>()
    val stackPointer: StateFlow<Int>
        field = MutableStateFlow(-1)

    val canUndo: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val canRedo: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        updateStackPointer(-1)
    }

    fun push(change: Change) {
        // Drop all redo entries after current pointer
        popRedos()
        // If full, evict the oldest entry and shift the pointer accordingly
        var newStackPointer = stackPointer.value
        if (changeStack.size >= maxSize) {
            if (changeStack.isNotEmpty()) {
                changeStack.removeAt(0)
                // Shift pointer left because we removed the head
                newStackPointer = (newStackPointer - 1).coerceAtLeast(-1)
            }
        }
        changeStack.add(change)
        updateStackPointer(newStackPointer + 1)
    }

    fun redo() {
        val newPointer = stackPointer.value + 1
        if (newPointer >= changeStack.size) {
            throw ChangeHistoryException("There is no Change to redo!")
        }
        updateStackPointer(newPointer)
        val makeListAction = changeStack[stackPointer.value]
        Log.d(TAG, "redo: $makeListAction")
        makeListAction.redo()
    }

    fun redoAll() {
        while (stackPointer.value < changeStack.lastIndex) {
            redo()
        }
    }

    fun undo() {
        if (stackPointer.value < 0) {
            throw ChangeHistoryException("There is no Change to undo!}")
        }
        val makeListAction = changeStack[stackPointer.value]
        Log.d(TAG, "undo: $makeListAction")
        makeListAction.undo()
        updateStackPointer(stackPointer.value - 1)
    }

    fun undoAll() {
        while (stackPointer.value >= 0) {
            undo()
        }
    }

    fun reset() {
        changeStack.clear()
        updateStackPointer(-1)
    }

    internal fun lookUp(position: Int = 0): Change {
        if (stackPointer.value - position < 0) {
            throw ChangeHistoryException("ChangeHistory only has ${stackPointer.value+1} changes!")
        }
        return changeStack[stackPointer.value - position]
    }

    private fun popRedos() {
        while (changeStack.size > stackPointer.value + 1) {
            changeStack.removeAt(stackPointer.value + 1)
        }
        updateStackPointer(stackPointer.value)
    }

    private fun updateStackPointer(value: Int) {
        stackPointer.value = value
        canUndo.value = value > -1
        canRedo.value = value >= -1 && value < changeStack.size - 1
    }

    inner class ChangeHistoryException(message: String) :
        IllegalStateException(
            "$message\nstackPointer: ${stackPointer.value}\nchangeStack:\n${changeStack.joinToString("\n")}"
        )

    companion object {
        private const val TAG = "ChangeHistory"
    }
}
