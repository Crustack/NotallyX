package com.philkes.notallyx.changehistory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.philkes.notallyx.test.mockAndroidLog
import com.philkes.notallyx.utils.changehistory.Change
import com.philkes.notallyx.utils.changehistory.ChangeHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ChangeHistoryTest {
    private lateinit var changeHistory: ChangeHistory

    @get:Rule val rule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        mockAndroidLog()
        changeHistory = ChangeHistory()
    }

    @Test
    fun `test push adds change to stack and updates stackPointer`() {
        val change = mock<Change>()

        changeHistory.push(change)

        assertTrue(changeHistory.canUndo.value)
    }

    @Test
    fun `test undo when stack has one change`() {
        val change = mock<Change>()

        changeHistory.push(change)
        changeHistory.undo()

        verify(change).undo()
        assertFalse(changeHistory.canUndo.value)
        assertTrue(changeHistory.canRedo.value)
    }

    @Test
    fun `test undoAll when stack has multiple changes`() {
        val change = mock<Change>()
        val change1 = mock<Change>()
        val change2 = mock<Change>()

        changeHistory.push(change)
        changeHistory.push(change1)
        changeHistory.push(change2)
        changeHistory.undoAll()

        verify(change).undo()
        verify(change1).undo()
        verify(change2).undo()
        assertFalse(changeHistory.canUndo.value)
        assertTrue(changeHistory.canRedo.value)
    }

    @Test
    fun `test redo when stack has one change`() {
        val change = mock<Change>()

        changeHistory.push(change)
        changeHistory.undo()
        changeHistory.redo()

        verify(change).redo()
        assertTrue(changeHistory.canUndo.value)
        assertFalse(changeHistory.canRedo.value)
    }

    @Test
    fun `test redoAll when stack has multiple changes`() {
        val change = mock<Change>()
        val change1 = mock<Change>()
        val change2 = mock<Change>()

        changeHistory.push(change)
        changeHistory.push(change1)
        changeHistory.push(change2)
        changeHistory.undoAll()
        changeHistory.redoAll()

        verify(change2).redo()
        verify(change1).redo()
        verify(change).redo()
        assertTrue(changeHistory.canUndo.value)
        assertFalse(changeHistory.canRedo.value)
    }

    @Test
    fun `test canUndo and canRedo logic`() {
        val change = mock<Change>()

        assertFalse(changeHistory.canUndo.value)
        assertFalse(changeHistory.canRedo.value)

        changeHistory.push(change)

        assertTrue(changeHistory.canUndo.value)
        assertFalse(changeHistory.canRedo.value)

        changeHistory.undo()

        assertFalse(changeHistory.canUndo.value)
        assertTrue(changeHistory.canRedo.value)
    }

    @Test
    fun `test invalidateRedos`() {
        val change1 = TestChange()
        val change2 = TestChange()
        val change3 = TestChange()
        val change4 = TestChange()

        changeHistory.push(change1)
        changeHistory.push(change2)
        changeHistory.push(change3)
        changeHistory.undo()
        changeHistory.push(change4)

        assertEquals(change4, changeHistory.lookUp())
        assertEquals(change2, changeHistory.lookUp(1))
        assertEquals(change1, changeHistory.lookUp(2))
        assertThrows(ChangeHistory.ChangeHistoryException::class.java) { changeHistory.lookUp(3) }
    }

    class TestChange : Change {
        override fun redo() {}

        override fun undo() {}
    }

    @Test
    fun `bounded history evicts oldest when capacity reached`() {
        // Use a small capacity to test eviction behavior
        changeHistory = ChangeHistory(maxSize = 3)
        val c1 = TestChange()
        val c2 = TestChange()
        val c3 = TestChange()
        val c4 = TestChange()

        changeHistory.push(c1)
        changeHistory.push(c2)
        changeHistory.push(c3)
        // Next push should evict c1
        changeHistory.push(c4)

        assertTrue(changeHistory.canUndo.value)
        // Top of stack is c4
        assertEquals(c4, changeHistory.lookUp())
        // Next is c3
        assertEquals(c3, changeHistory.lookUp(1))
        // Next is c2
        assertEquals(c2, changeHistory.lookUp(2))
        // c1 should be gone
        assertThrows(ChangeHistory.ChangeHistoryException::class.java) { changeHistory.lookUp(3) }
    }

    @Test
    fun `pushing after undo with full capacity drops redos then evicts oldest if needed`() {
        changeHistory = ChangeHistory(maxSize = 3)
        val c1 = TestChange()
        val c2 = TestChange()
        val c3 = TestChange()
        val c4 = TestChange()
        val c5 = TestChange()

        // Fill to capacity
        changeHistory.push(c1)
        changeHistory.push(c2)
        changeHistory.push(c3)

        // Undo 1 -> pointer at c2
        changeHistory.undo()
        assertTrue(changeHistory.canRedo.value)

        // Push new change: should drop redo (c3) first, then possibly evict if full when pushing
        changeHistory.push(c4)

        // Stack should now have [c1, c2, c4] with pointer at top
        assertEquals(c4, changeHistory.lookUp())
        assertEquals(c2, changeHistory.lookUp(1))
        assertEquals(c1, changeHistory.lookUp(2))

        // Push another to force eviction of c1
        changeHistory.push(c5)
        assertEquals(c5, changeHistory.lookUp())
        assertEquals(c4, changeHistory.lookUp(1))
        assertEquals(c2, changeHistory.lookUp(2))
        assertThrows(ChangeHistory.ChangeHistoryException::class.java) { changeHistory.lookUp(3) }

        // No redo available after pushes
        assertFalse(changeHistory.canRedo.value)
    }
}
