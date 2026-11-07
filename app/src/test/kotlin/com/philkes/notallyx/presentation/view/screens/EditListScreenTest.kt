package com.philkes.notallyx.presentation.view.screens

import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import org.junit.Assert.assertEquals
import org.junit.Test

class EditListScreenTest {

    @Test
    fun testEditNoteStateInstantiatesWithListItems() {
        val item1 =
            ListItem(
                body = "Buy Milk",
                checked = false,
                isChild = false,
                order = 0,
                children = mutableListOf(),
                id = 1,
            )
        val item2 =
            ListItem(
                body = "Buy Bread",
                checked = true,
                isChild = false,
                order = 1,
                children = mutableListOf(),
                id = 2,
            )

        val noteState =
            EditNoteState(
                title = "Shopping List",
                items = listOf(item1),
                checkedItems = listOf(item2),
            )

        assertEquals("Shopping List", noteState.title)
        assertEquals(1, noteState.items.size)
        assertEquals(1, noteState.checkedItems.size)
        assertEquals("Buy Milk", noteState.items[0].body)
        assertEquals("Buy Bread", noteState.checkedItems[0].body)
    }
}
