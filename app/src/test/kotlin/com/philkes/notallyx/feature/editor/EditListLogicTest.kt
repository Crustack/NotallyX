package com.philkes.notallyx.feature.editor

import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.listitem.checkItem
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.editor.listitem.initChildren
import com.philkes.notallyx.feature.editor.listitem.moveWithChildren
import com.philkes.notallyx.feature.editor.listitem.removeWithChildren
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.mockAndroidLog
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
        mockAndroidLog()
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

    @Test
    fun `test reordering item moves item and its children correctly`() {
        val p1 = createListItem(body = "Shopping List", checked = false, isChild = false, id = 0)
        val p2 = createListItem(body = "Apples", checked = false, isChild = false, id = 1)
        val p3 = createListItem(body = "Bananas", checked = false, isChild = false, id = 2)
        val items = mutableListOf(p1, p2, p3).init().toMutableList()

        // Move "Apples" (index 1) after "Bananas" (index 2)
        items.moveWithChildren(1, 2)
        initChildren(items)

        assertEquals(listOf("Shopping List", "Bananas", "Apples"), items.map { it.body })
    }

    @Test
    fun `test moving item between parent and its children converts moved item to child`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val parentB = createListItem(body = "Parent B", checked = false, isChild = false, id = 2)
        val childB1 = createListItem(body = "Child B1", checked = false, isChild = true, id = 3)
        val items = mutableListOf(parentA, childA1, parentB, childB1).init().toMutableList()

        // Move Parent B (index 2) between Parent A (index 0) and Child A1 (index 1)
        items.moveWithChildren(2, 1)
        initChildren(items)

        assertEquals(listOf("Parent A", "Parent B", "Child B1", "Child A1"), items.map { it.body })
        assertTrue(items[1].isChild) // Parent B becomes child of Parent A
        assertTrue(items[2].isChild) // Child B1 remains child (now under Parent A)
        assertTrue(items[3].isChild) // Child A1 remains child under Parent A
        assertEquals(3, items[0].children.size)
        assertEquals(listOf("Parent B", "Child B1", "Child A1"), items[0].children.map { it.body })
    }

    @Test
    fun `test moving parent item to position not between parent and children keeps it top level`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val parentB = createListItem(body = "Parent B", checked = false, isChild = false, id = 2)
        val childB1 = createListItem(body = "Child B1", checked = false, isChild = true, id = 3)
        val parentC = createListItem(body = "Parent C", checked = false, isChild = false, id = 4)
        val items =
            mutableListOf(parentA, childA1, parentB, childB1, parentC).init().toMutableList()

        // Move Parent B (index 2) after Child A1 (index 2 in remaining list, before Parent C)
        items.moveWithChildren(2, 2)
        initChildren(items)

        assertEquals(
            listOf("Parent A", "Child A1", "Parent B", "Child B1", "Parent C"),
            items.map { it.body },
        )
        assertFalse(items[2].isChild) // Parent B stays top-level
        assertEquals(1, items[2].children.size) // Parent B still has Child B1
    }

    @Test
    fun `test moving standalone item between parent and its children converts it to child`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val itemX = createListItem(body = "Item X", checked = false, isChild = false, id = 2)
        val items = mutableListOf(parentA, childA1, itemX).init().toMutableList()

        // Move Item X (index 2) between Parent A (index 0) and Child A1 (index 1)
        items.moveWithChildren(2, 1)
        initChildren(items)

        assertEquals(listOf("Parent A", "Item X", "Child A1"), items.map { it.body })
        assertTrue(items[1].isChild) // Item X became child of Parent A
        assertEquals(2, items[0].children.size)
    }

    @Test
    fun `test moving parent item into existing parent children places it as first child and updates parent children`() {
        val shoppingList =
            createListItem(body = "Shopping List", checked = false, isChild = false, id = 0)
        val apples = createListItem(body = "Apples", checked = false, isChild = true, id = 1)
        val bananas = createListItem(body = "Bananas", checked = false, isChild = true, id = 2)
        val cherries = createListItem(body = "Cherries", checked = false, isChild = false, id = 3)
        val dates = createListItem(body = "Dates", checked = false, isChild = false, id = 4)
        val items =
            mutableListOf(shoppingList, apples, bananas, cherries, dates).init().toMutableList()

        // Move Cherries (index 3) to Bananas (index 2)
        items.moveWithChildren(3, 2)

        assertEquals(
            listOf("Shopping List", "Apples", "Cherries", "Bananas", "Dates"),
            items.map { it.body },
        )
        assertTrue(items[2].isChild)
        assertEquals(
            listOf("Apples", "Cherries", "Bananas"),
            items[0].children.map { it.body },
        )
        items.forEachIndexed { index, item -> assertEquals(index, item.order) }
    }

    @Test
    fun `test moveWithChildren updates item order values to match list indices`() {
        val shoppingList =
            createListItem(body = "Shopping List", checked = false, isChild = false, id = 0)
        val apples = createListItem(body = "Apples", checked = false, isChild = true, id = 1)
        val bananas = createListItem(body = "Bananas", checked = false, isChild = true, id = 2)
        val cherries = createListItem(body = "Cherries", checked = false, isChild = false, id = 3)
        val dates = createListItem(body = "Dates", checked = false, isChild = false, id = 4)
        val items =
            mutableListOf(shoppingList, apples, bananas, cherries, dates).init().toMutableList()

        // Move Cherries (index 3) to 0
        items.moveWithChildren(3, 0)

        assertEquals(
            listOf("Cherries", "Shopping List", "Apples", "Bananas", "Dates"),
            items.map { it.body },
        )
        items.forEachIndexed { index, item -> assertEquals(index, item.order) }

        // Move Shopping List (index 1) down after Dates
        items.moveWithChildren(1, 4)

        assertEquals(
            listOf("Cherries", "Dates", "Shopping List", "Apples", "Bananas"),
            items.map { it.body },
        )
        items.forEachIndexed { index, item -> assertEquals(index, item.order) }
    }

    @Test
    fun `test dragging parent item under child converts it to child and moving further to top-level restores it as parent using originalItem`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val parentB = createListItem(body = "Parent B", checked = false, isChild = false, id = 2)
        val parentC = createListItem(body = "Parent C", checked = false, isChild = false, id = 3)
        val items = mutableListOf(parentA, childA1, parentB, parentC).init().toMutableList()

        // Capture original item state at start of drag
        val originalParentB = parentB.clone() as com.philkes.notallyx.data.model.ListItem

        // Step 1: Move Parent B (index 2) between Parent A and Child A1 (index 1)
        items.moveWithChildren(2, 1, originalParentB)
        initChildren(items)

        assertEquals(
            listOf("Parent A", "Parent B", "Child A1", "Parent C"),
            items.map { it.body },
        )
        assertTrue(items[1].isChild) // Converted to child of Parent A

        // Step 2: Continue drag of Parent B (now index 1) down after Parent C (index 3)
        items.moveWithChildren(1, 3, originalParentB)
        initChildren(items)

        assertEquals(
            listOf("Parent A", "Child A1", "Parent C", "Parent B"),
            items.map { it.body },
        )
        assertFalse(
            items[3].isChild
        ) // Restored to top-level parent because original item was not a child
    }

    @Test
    fun `test sequential drag steps moving parent Cherries from index 3 to index 0 above Shopping List`() {
        val shoppingList =
            createListItem(body = "Shopping List", checked = false, isChild = false, id = 0)
        val apples = createListItem(body = "Apples", checked = false, isChild = true, id = 1)
        val bananas = createListItem(body = "Bananas", checked = false, isChild = true, id = 2)
        val cherries = createListItem(body = "Cherries", checked = false, isChild = false, id = 3)
        val dates = createListItem(body = "Dates", checked = false, isChild = false, id = 4)
        val items =
            mutableListOf(shoppingList, apples, bananas, cherries, dates).init().toMutableList()

        val originalCherries = cherries.clone() as com.philkes.notallyx.data.model.ListItem

        // Step 1: Cherries (index 3) drags over Bananas (index 2) -> moveWithChildren(3, 2)
        items.moveWithChildren(3, 2, originalCherries)
        initChildren(items)

        // Cherries is placed at index 2 as a child under Shopping List (between Apples and Bananas)
        assertEquals(
            listOf("Shopping List", "Apples", "Cherries", "Bananas", "Dates"),
            items.map { it.body },
        )
        val cherriesIndexStep1 = items.indexOfFirst { it.id == cherries.id }
        assertEquals(2, cherriesIndexStep1)

        // Step 2: Cherries (now at index 2) continues drag over Shopping List (index 0) ->
        // moveWithChildren(2, 0)
        items.moveWithChildren(cherriesIndexStep1, 0, originalCherries)
        initChildren(items)

        // Cherries is placed at index 0 as top-level parent above Shopping List
        assertEquals(
            listOf("Cherries", "Shopping List", "Apples", "Bananas", "Dates"),
            items.map { it.body },
        )
        assertFalse(items[0].isChild)
        assertTrue(items[2].isChild) // Apples remains child under Shopping List
        assertTrue(items[3].isChild) // Bananas remains child under Shopping List
    }

    @Test
    fun `test dragging child item to index 0 converts it to parent`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val items = mutableListOf(parentA, childA1).init().toMutableList()

        val originalChild = childA1.clone() as com.philkes.notallyx.data.model.ListItem

        // Drag Child A1 (index 1) to index 0 (above Parent A)
        items.moveWithChildren(1, 0, originalChild)

        assertEquals(listOf("Child A1", "Parent A"), items.map { it.body })
        assertFalse(items[0].isChild) // Becomes parent because no parent exists above it
        assertFalse(items[1].isChild)
        assertTrue(items[0].children.isEmpty())
        items.forEachIndexed { index, item -> assertEquals(index, item.order) }
    }

    @Test
    fun `test dragging child item under another parent or children makes it child of that parent`() {
        val parentA = createListItem(body = "Parent A", checked = false, isChild = false, id = 0)
        val childA1 = createListItem(body = "Child A1", checked = false, isChild = true, id = 1)
        val parentB = createListItem(body = "Parent B", checked = false, isChild = false, id = 2)
        val childB1 = createListItem(body = "Child B1", checked = false, isChild = true, id = 3)
        val items = mutableListOf(parentA, childA1, parentB, childB1).init().toMutableList()

        val originalChildB1 = childB1.clone() as com.philkes.notallyx.data.model.ListItem

        // Drag Child B1 (index 3) under Parent A's children (index 1)
        items.moveWithChildren(3, 1, originalChildB1)

        assertEquals(listOf("Parent A", "Child B1", "Child A1", "Parent B"), items.map { it.body })
        assertTrue(items[1].isChild) // Child of Parent A
        assertTrue(items[2].isChild) // Child of Parent A
        assertEquals(listOf("Child B1", "Child A1"), items[0].children.map { it.body })
        assertTrue(items[3].children.isEmpty())
        items.forEachIndexed { index, item -> assertEquals(index, item.order) }
    }
}
