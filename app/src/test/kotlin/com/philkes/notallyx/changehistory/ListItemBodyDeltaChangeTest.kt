package com.philkes.notallyx.changehistory

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.changes.ListItemBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeListItemBodyDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ListItemBodyDeltaChangeTest {

    private fun createMockModelWithItems(items: ArrayList<ListItem>): NoteModel {
        val model = mock<NoteModel>()
        `when`(model.items).thenReturn(items)
        return model
    }

    private fun createListItem(id: Int, body: String = ""): ListItem {
        return ListItem(
            body = body,
            checked = false,
            isChild = false,
            order = id,
            children = mutableListOf(),
            id = id,
        )
    }

    @Test
    fun `computeListItemBodyDelta creates delta for plain string insertion deletion and replacement`() {
        val item = createListItem(id = 1, body = "")
        val model = createMockModelWithItems(arrayListOf(item))

        // Insertion: "" -> "Apples"
        val insertDelta =
            computeListItemBodyDelta(
                oldText = "",
                newText = "Apples",
                itemId = 1,
                notallyModel = model,
            )
        assertNotNull(insertDelta)
        assertEquals(1, insertDelta!!.itemId)
        assertEquals(0, insertDelta.startOffset)
        assertEquals("", insertDelta.deletedText.text)
        assertEquals("Apples", insertDelta.insertedText.text)

        // Deletion: "Apples" -> "Apple"
        val deleteDelta =
            computeListItemBodyDelta(
                oldText = "Apples",
                newText = "Apple",
                itemId = 1,
                notallyModel = model,
            )
        assertNotNull(deleteDelta)
        assertEquals(5, deleteDelta!!.startOffset)
        assertEquals("s", deleteDelta.deletedText.text)
        assertEquals("", deleteDelta.insertedText.text)

        // Replacement: "Apple" -> "Banana"
        val replaceDelta =
            computeListItemBodyDelta(
                oldText = "Apple",
                newText = "Banana",
                itemId = 1,
                notallyModel = model,
            )
        assertNotNull(replaceDelta)
        assertEquals(0, replaceDelta!!.startOffset)
        assertEquals("Apple", replaceDelta.deletedText.text)
        assertEquals("Banana", replaceDelta.insertedText.text)

        // No-op / identical returns null
        val noOpDelta =
            computeListItemBodyDelta(
                oldText = "Same",
                newText = "Same",
                itemId = 1,
                notallyModel = model,
            )
        assertNull(noOpDelta)
    }

    @Test
    fun `computeListItemBodyDelta supports AnnotatedString and formatting changes`() {
        val item = createListItem(id = 42, body = "Hello World")
        val model = createMockModelWithItems(arrayListOf(item))
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

        val unformatted = AnnotatedString("Hello World")
        val formatted =
            AnnotatedString(
                text = "Hello World",
                spanStyles = listOf(AnnotatedString.Range(boldStyle, 6, 11)),
            )

        val delta =
            computeListItemBodyDelta(
                oldValue = unformatted,
                newValue = formatted,
                itemId = 42,
                notallyModel = model,
            )
        assertNotNull(delta)
        assertEquals(42, delta!!.itemId)
        assertEquals(6, delta.startOffset)
        assertEquals("World", delta.deletedText.text)
        assertEquals(0, delta.deletedText.spanStyles.size)
        assertEquals("World", delta.insertedText.text)
        assertEquals(1, delta.insertedText.spanStyles.size)
    }

    @Test
    fun `computeListItemBodyDelta supports TextFieldValue with selection`() {
        val item = createListItem(id = 5, body = "Note")
        val model = createMockModelWithItems(arrayListOf(item))

        val oldVal = TextFieldValue("Note", selection = TextRange(4))
        val newVal = TextFieldValue("Notes", selection = TextRange(5))

        val delta =
            computeListItemBodyDelta(
                oldValue = oldVal,
                newValue = newVal,
                itemId = 5,
                notallyModel = model,
            )
        assertNotNull(delta)
        assertEquals(5, delta!!.itemId)
        assertEquals(4, delta.startOffset)
        assertEquals("", delta.deletedText.text)
        assertEquals("s", delta.insertedText.text)
        assertEquals(TextRange(4), delta.oldSelection)
        assertEquals(TextRange(5), delta.newSelection)
    }

    @Test
    fun `ListItemBodyDeltaChange undo and redo correctly restores list item body`() {
        var updatedItem: ListItem? = null
        var restoredAnnotated: AnnotatedString? = null
        val item = createListItem(id = 10, body = "Hello World")
        val model = createMockModelWithItems(arrayListOf(item))

        val change =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 10,
                startOffset = 5,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString(" Beautiful"),
                onItemUpdated = { itm, ann ->
                    updatedItem = itm
                    restoredAnnotated = ann
                },
            )

        // Simulate redo / user typing
        item.body = "Hello Beautiful World"

        // Undo
        change.undo()
        assertEquals("Hello World", item.body)
        assertEquals(item, updatedItem)
        assertEquals("Hello World", restoredAnnotated?.text)

        // Redo
        change.redo()
        assertEquals("Hello Beautiful World", item.body)
        assertEquals(item, updatedItem)
        assertEquals("Hello Beautiful World", restoredAnnotated?.text)
    }

    @Test
    fun `ListItemBodyDeltaChange preserves rich text spans on undo and redo`() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        var restoredAnnotated: AnnotatedString? = null
        val item = createListItem(id = 20, body = "Buy Milk")
        val model = createMockModelWithItems(arrayListOf(item))

        val plainMilk = AnnotatedString("Milk")
        val boldMilk =
            AnnotatedString(
                text = "Milk",
                spanStyles = listOf(AnnotatedString.Range(boldStyle, 0, 4)),
            )

        // Make "Milk" bold: startOffset = 4, deletedText = plainMilk, insertedText = boldMilk
        val change =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 20,
                startOffset = 4,
                deletedText = plainMilk,
                insertedText = boldMilk,
                onItemUpdated = { _, ann -> restoredAnnotated = ann },
            )

        // Redo -> applies bold "Milk"
        change.redo()
        assertEquals("Buy Milk", item.body)
        val redoResult = restoredAnnotated
        assertNotNull(redoResult)
        assertEquals("Buy Milk", redoResult?.text)
        assertEquals(1, redoResult?.spanStyles?.size)
        assertEquals(4, redoResult?.spanStyles?.get(0)?.start)
        assertEquals(8, redoResult?.spanStyles?.get(0)?.end)
        assertEquals(boldStyle, redoResult?.spanStyles?.get(0)?.item)

        // Undo -> restores plain "Milk"
        change.undo()
        assertEquals("Buy Milk", item.body)
        val undoResult = restoredAnnotated
        assertNotNull(undoResult)
        assertEquals("Buy Milk", undoResult?.text)
        assertEquals(0, undoResult?.spanStyles?.size)
    }

    @Test
    fun `tryMergeWith merges consecutive single character keystrokes on same item within window`() {
        val item = createListItem(id = 1, body = "")
        val model = createMockModelWithItems(arrayListOf(item))
        val t0 = 2000L

        val c1 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 0,
                deletedText = "",
                insertedText = "A",
                oldSelection = TextRange(0),
                newSelection = TextRange(1),
                timestamp = t0,
            )

        val c2 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 1,
                deletedText = "",
                insertedText = "p",
                oldSelection = TextRange(1),
                newSelection = TextRange(2),
                timestamp = t0 + 150L,
            )

        assertTrue(c1.tryMergeWith(c2))
        assertEquals("Ap", c1.insertedText.text)
        assertEquals(TextRange(2), c1.newSelection)
        assertEquals(t0 + 150L, c1.timestamp)

        // Merge third character
        val c3 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 2,
                deletedText = "",
                insertedText = "p",
                oldSelection = TextRange(2),
                newSelection = TextRange(3),
                timestamp = t0 + 300L,
            )

        assertTrue(c1.tryMergeWith(c3))
        assertEquals("App", c1.insertedText.text)

        // Undo after merged typing restores empty string
        item.body = "App"
        c1.undo()
        assertEquals("", item.body)
    }

    @Test
    fun `tryMergeWith rejects merges across different item IDs`() {
        val model = createMockModelWithItems(arrayListOf())
        val t0 = 1000L

        val cItem1 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 0,
                deletedText = "",
                insertedText = "A",
                timestamp = t0,
            )

        val cItem2 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 2,
                startOffset = 0,
                deletedText = "",
                insertedText = "B",
                timestamp = t0 + 50L,
            )

        assertFalse(cItem1.tryMergeWith(cItem2))
    }

    @Test
    fun `tryMergeWith rejects expired keystrokes non-consecutive offsets newlines and paste`() {
        val model = createMockModelWithItems(arrayListOf())
        val t0 = 1000L

        val c1 =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 0,
                deletedText = "",
                insertedText = "X",
                timestamp = t0,
            )

        // Expired (> 1000ms)
        val expired =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 1,
                deletedText = "",
                insertedText = "Y",
                timestamp = t0 + 1500L,
            )
        assertFalse(c1.tryMergeWith(expired))

        // Non-consecutive offset
        val nonConsecutive =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 5,
                deletedText = "",
                insertedText = "Y",
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(nonConsecutive))

        // Newline
        val newline =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 1,
                deletedText = "",
                insertedText = "\n",
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(newline))

        // Multi-character insert (paste)
        val paste =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 1,
                startOffset = 1,
                deletedText = "",
                insertedText = "YZ",
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(paste))
    }

    @Test
    fun `undo and redo safely handles missing item in model`() {
        val model = createMockModelWithItems(arrayListOf()) // Empty list

        val change =
            ListItemBodyDeltaChange(
                notallyModel = model,
                itemId = 999, // Non-existent ID
                startOffset = 0,
                deletedText = "",
                insertedText = "Ghost",
            )

        // Should not throw NPE or crash
        change.undo()
        change.redo()
    }
}
