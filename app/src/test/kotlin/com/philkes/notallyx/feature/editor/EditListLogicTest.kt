package com.philkes.notallyx.feature.editor

import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.listitem.checkItem
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.editor.listitem.initChildren
import com.philkes.notallyx.feature.editor.listitem.removeWithChildren
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.test.createListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditListLogicTest {

    private lateinit var changeHistory: ChangeHistory

    @Before
    fun setUp() {
        changeHistory = ChangeHistory()
    }

    @Test
    fun `test parent check toggles all children and sets checkedTimestamp`() {
        val parent = createListItem(body = "Parent", checked = false, isChild = false, id = 0)
        val child1 = createListItem(body = "Child 1", checked = false, isChild = true, id = 1)
        val child2 = createListItem(body = "Child 2", checked = false, isChild = true, id = 2)
        val items = mutableListOf(parent, child1, child2).init()

        // Check parent
        items.checkItem(items[0], true)

        assertTrue(items[0].checked)
        assertNotNull(items[0].checkedTimestamp)
        assertTrue(items[1].checked)
        assertNotNull(items[1].checkedTimestamp)
        assertTrue(items[2].checked)
        assertNotNull(items[2].checkedTimestamp)

        // Uncheck parent
        items.checkItem(items[0], false)

        assertFalse(items[0].checked)
        assertNull(items[0].checkedTimestamp)
        assertFalse(items[1].checked)
        assertNull(items[1].checkedTimestamp)
        assertFalse(items[2].checked)
        assertNull(items[2].checkedTimestamp)
    }

    @Test
    fun `test checking all children automatically checks parent`() {
        val parent = createListItem(body = "Parent", checked = false, isChild = false, id = 0)
        val child1 = createListItem(body = "Child 1", checked = false, isChild = true, id = 1)
        val child2 = createListItem(body = "Child 2", checked = false, isChild = true, id = 2)
        val items = mutableListOf(parent, child1, child2).init()

        // Check child 1
        items.checkItem(items[1], true)

        assertFalse(items[0].checked)

        // Check child 2
        items.checkItem(items[2], true)

        assertTrue(items[0].checked)
        assertNotNull(items[0].checkedTimestamp)
    }

    @Test
    fun `test unchecking any child automatically unchecks checked parent`() {
        val parent = createListItem(body = "Parent", checked = true, isChild = false, id = 0)
        val child1 = createListItem(body = "Child 1", checked = true, isChild = true, id = 1)
        val child2 = createListItem(body = "Child 2", checked = true, isChild = true, id = 2)
        val items = mutableListOf(parent, child1, child2).init()

        // Uncheck child 1
        items.checkItem(items[1], false)

        assertFalse(items[0].checked)
        assertNull(items[0].checkedTimestamp)
        assertTrue(items[2].checked)
    }

    @Test
    fun `test splitByChecked partitions checked and active items correctly`() {
        val p1 = createListItem(body = "P1", checked = false, isChild = false, id = 0)
        val c1 = createListItem(body = "C1", checked = true, isChild = true, id = 1)
        val c2 = createListItem(body = "C2", checked = false, isChild = true, id = 2)

        val p2 = createListItem(body = "P2", checked = true, isChild = false, id = 3)
        val c3 = createListItem(body = "C3", checked = true, isChild = true, id = 4)
        val c4 = createListItem(body = "C4", checked = true, isChild = true, id = 5)

        val s1 = createListItem(body = "S1", checked = false, isChild = false, id = 6)
        val s2 = createListItem(body = "S2", checked = true, isChild = false, id = 7)

        val list = mutableListOf(p1, c1, c2, p2, c3, c4, s1, s2).init()

        val (checkedItems, activeItems) = list.splitByChecked()

        assertEquals(listOf("P1", "C1", "C2", "S1"), activeItems.map { it.body })
        assertEquals(listOf("P2", "C3", "C4", "S2"), checkedItems.map { it.body })
    }

    @Test
    fun `test swipe right demotes item to child under preceding parent`() {
        val p1 = createListItem(body = "P1", checked = false, isChild = false, id = 0)
        val item2 = createListItem(body = "Item2", checked = false, isChild = false, id = 1)
        val items = mutableListOf(p1, item2).init()

        // Swipe right on Item2
        items[1].isChild = true
        initChildren(items)

        assertTrue(items[1].isChild)
        assertEquals(1, items[0].children.size)
        assertEquals(items[1], items[0].children[0])
    }

    @Test
    fun `test swipe right on index 0 is ignored`() {
        val item1 = createListItem(body = "Item1", checked = false, isChild = false, id = 0)
        val item2 = createListItem(body = "Item2", checked = false, isChild = false, id = 1)
        val items = mutableListOf(item1, item2).init()

        // Swipe right on Item1 (index 0)
        val idx = 0
        if (idx > 0) {
            items[0].isChild = true
        }
        initChildren(items)

        assertFalse(items[0].isChild)
        assertTrue(items[0].children.isEmpty())
    }

    @Test
    fun `test swipe left promotes child item to top level parent`() {
        val p1 = createListItem(body = "P1", checked = false, isChild = false, id = 0)
        val c1 = createListItem(body = "C1", checked = false, isChild = true, id = 1)
        val items = mutableListOf(p1, c1).init()

        assertEquals(1, items[0].children.size)

        // Swipe left on C1
        items[1].isChild = false
        initChildren(items)

        assertFalse(items[1].isChild)
        assertTrue(items[0].children.isEmpty())
    }

    @Test
    fun `test delete parent item with children removes parent and children`() {
        val p1 = createListItem(body = "P1", checked = false, isChild = false, id = 0)
        val c1 = createListItem(body = "C1", checked = false, isChild = true, id = 1)
        val s1 = createListItem(body = "S1", checked = false, isChild = false, id = 2)
        val items = mutableListOf(p1, c1, s1).init().toMutableList()

        items.removeWithChildren(items[0])
        initChildren(items)

        assertEquals(1, items.size)
        assertEquals("S1", items[0].body)
    }
}
