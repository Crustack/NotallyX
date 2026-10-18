package com.philkes.notallyx.recyclerview.listmanager

import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.test.assertChecked
import com.philkes.notallyx.test.assertOrder
import com.philkes.notallyx.test.assertSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChecklistItemActionsTest : ListManagerTestBase() {

    // 1. Change text (body field)
    @Test
    fun `change text updates item body field`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeText(position = 0, text = "Updated Title")

        assertEquals("Updated Title", items[0].body)
    }

    // 2. Swiping an item to the right makes it a child of the item above it
    @Test
    fun `swipe item to right makes item child of item above it`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        "B".assertIsParent()

        // Swiping "B" right at position 1 makes "B" a child of "A"
        listManager.changeIsChild(position = 1, isChild = true)

        "B".assertIsChild()
        "A".assertChildren("B")
    }

    @Test
    fun `swipe item at index 0 right remains parent`() {
        setSorting(ListItemSort.NO_AUTO_SORT)

        // Item at index 0 cannot become a child
        listManager.changeIsChild(position = 0, isChild = true)

        "A".assertIsParent()
    }

    @Test
    fun `swipe child item to left makes child item parent again`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(position = 1, isChild = true)
        "B".assertIsChild()

        // Swiping "B" left at position 1 makes "B" a top-level parent again
        listManager.changeIsChild(position = 1, isChild = false)

        "B".assertIsParent()
        "A".assertIsParent()
    }

    // 3. Check/Uncheck item with preferences and parent-child rules
    @Test
    fun `checking parent checks all children under NO_AUTO_SORT`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A

        listManager.changeChecked(0, checked = true)

        "A".assertIsChecked()
        "B".assertIsChecked()
        items.assertSize(6) // All stay in unchecked list under NO_AUTO_SORT
    }

    @Test
    fun `checking all children checks parent under NO_AUTO_SORT`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A

        listManager.changeChecked(1, checked = true) // Check B

        "A".assertIsChecked()
        "B".assertIsChecked()
    }

    @Test
    fun `checking child when parent has other unchecked children does not check parent`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        listManager.changeIsChild(2, isChild = true) // C is child of A

        listManager.changeChecked(1, checked = true) // Check B only

        "B".assertIsChecked()
        "C".assertIsNotChecked()
        "A".assertIsNotChecked()
    }

    @Test
    fun `checking child when parent has other unchecked children does not move child to checked list under AUTO_SORT`() {
        setSorting(ListItemSort.AUTO_SORT_BY_CHECKED)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        listManager.changeIsChild(2, isChild = true) // C is child of A

        listManager.changeChecked(1, checked = true) // Check B only

        "B".assertIsChecked()
        "C".assertIsNotChecked()
        "A".assertIsNotChecked()
        items.assertOrder("A", "B", "C", "D", "E", "F")
        itemsChecked!!.assertSize(0) // None moved to checked list yet
    }

    @Test
    fun `checking parent moves parent and all children to checked list under AUTO_SORT`() {
        setSorting(ListItemSort.AUTO_SORT_BY_CHECKED)
        listManager.changeIsChild(1, isChild = true) // B is child of A

        listManager.changeChecked(0, checked = true) // Check parent A

        itemsChecked!!.assertSize(2)
        itemsChecked!!.assertOrder("A", "B")
        itemsChecked!!.assertChecked(true, true)
    }

    @Test
    fun `checking last unchecked child moves parent and all children to checked list under AUTO_SORT`() {
        setSorting(ListItemSort.AUTO_SORT_BY_CHECKED)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        listManager.changeIsChild(2, isChild = true) // C is child of A

        listManager.changeChecked(1, checked = true) // Check B
        itemsChecked!!.assertSize(0) // B stays under A

        listManager.changeChecked(2, checked = true) // Check C (last child)

        itemsChecked!!.assertSize(3) // Now A, B, C move to checked list together
        itemsChecked!!.assertOrder("A", "B", "C")
    }

    // 4. Reorder via drag and drop
    @Test
    fun `drag and drop parent moves children as group`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A

        // Move parent A (index 0) below C (index 2)
        listManager.move(positionFrom = 0, positionTo = 2)

        items.assertOrder("C", "A", "B", "D", "E", "F")
        "A".assertChildren("B")
    }

    @Test
    fun `drag child below another parent adopts new parent`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        // A (parent), B (child), C (parent), D (parent)

        // Move B (index 1) to after C (index 2)
        val (movedPos, count) = listManager.move(positionFrom = 1, positionTo = 2)
        listManager.finishMove(
            positionTo = movedPos,
            count = count,
            parentBefore = items[0],
            stateBefore = listManager.getState(),
            pushChange = true,
        )

        items.assertOrder("A", "C", "B", "D", "E", "F")
        "C".assertChildren("B")
    }

    // 5. Delete item
    @Test
    fun `delete parent deletes parent and all its children`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        listManager.changeIsChild(2, isChild = true) // C is child of A

        listManager.delete(position = 0) // Delete A

        items.assertOrder("D", "E", "F")
        assertFalse(items.any { it.body == "A" || it.body == "B" || it.body == "C" })
    }

    @Test
    fun `delete child removes child and updates parent checked status if all remaining children are checked`() {
        setSorting(ListItemSort.NO_AUTO_SORT)
        listManager.changeIsChild(1, isChild = true) // B is child of A
        listManager.changeIsChild(2, isChild = true) // C is child of A
        listManager.changeChecked(1, checked = true) // Check B

        "A".assertIsNotChecked()

        listManager.delete(position = 2) // Delete C (the unchecked child)

        "A".assertIsChecked() // Now remaining child B is checked, so A becomes checked!
    }
}
