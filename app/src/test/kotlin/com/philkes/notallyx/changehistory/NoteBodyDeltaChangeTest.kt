package com.philkes.notallyx.changehistory

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeBodyDelta
import com.philkes.notallyx.feature.editor.history.findDifferingRange
import com.philkes.notallyx.feature.editor.history.replaceRangeWithSpans
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class NoteBodyDeltaChangeTest {

    private fun createMockModel(initialValue: TextFieldValue): NoteModel {
        val model = mock<NoteModel>()
        var body = initialValue
        `when`(model.body).thenAnswer { body }
        doAnswer { invocation ->
                body = invocation.getArgument(0)
                null
            }
            .`when`(model)
            .body = any()
        return model
    }

    @Test
    fun `replaceRangeWithSpans inserts at start middle and end`() {
        val base = AnnotatedString("Hello World")

        // Insert at start
        val atStart = base.replaceRangeWithSpans(0, 0, AnnotatedString("Prefix "))
        assertEquals("Prefix Hello World", atStart.text)

        // Insert at middle
        val atMiddle = base.replaceRangeWithSpans(5, 5, AnnotatedString(" Beautiful"))
        assertEquals("Hello Beautiful World", atMiddle.text)

        // Insert at end
        val atEnd = base.replaceRangeWithSpans(11, 11, AnnotatedString("!"))
        assertEquals("Hello World!", atEnd.text)
    }

    @Test
    fun `replaceRangeWithSpans deletes and replaces slices`() {
        val base = AnnotatedString("Hello Beautiful World")

        // Delete " Beautiful"
        val deleted = base.replaceRangeWithSpans(5, 15, AnnotatedString(""))
        assertEquals("Hello World", deleted.text)

        // Replace "World" with "Compose"
        val replaced = base.replaceRangeWithSpans(16, 21, AnnotatedString("Compose"))
        assertEquals("Hello Beautiful Compose", replaced.text)
    }

    @Test
    fun `replaceRangeWithSpans preserves spans on prefix replacement and suffix`() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val initial =
            AnnotatedString(
                text = "Hello World",
                spanStyles = listOf(AnnotatedString.Range(boldStyle, 6, 11)), // "World" is bold
            )

        // Replace "Hello" with "Hi"
        val updated = initial.replaceRangeWithSpans(0, 5, AnnotatedString("Hi"))
        assertEquals("Hi World", updated.text)
        assertEquals(1, updated.spanStyles.size)
        // "World" span should be shifted from 6..11 to 3..8
        assertEquals(3, updated.spanStyles[0].start)
        assertEquals(8, updated.spanStyles[0].end)
        assertEquals(boldStyle, updated.spanStyles[0].item)
    }

    @Test
    fun `findDifferingRange detects span changes when text is unchanged`() {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val unformatted = AnnotatedString("Hello World")
        val withBold =
            AnnotatedString(
                text = "Hello World",
                spanStyles = listOf(AnnotatedString.Range(boldStyle, 6, 11)),
            )

        val (start, end) = findDifferingRange(unformatted, withBold)
        assertEquals(6, start)
        assertEquals(11, end)
    }

    @Test
    fun `computeBodyDelta creates delta for insertion deletion and replacement`() {
        val model = createMockModel(TextFieldValue())

        // Insertion: "" -> "Hello"
        val insertDelta =
            computeBodyDelta(
                oldValue = TextFieldValue(""),
                newValue = TextFieldValue("Hello", selection = TextRange(5)),
                notallyModel = model,
            )
        assertNotNull(insertDelta)
        assertEquals(0, insertDelta!!.startOffset)
        assertEquals("", insertDelta.deletedText.text)
        assertEquals("Hello", insertDelta.insertedText.text)

        // Deletion: "Hello World" -> "Hello"
        val deleteDelta =
            computeBodyDelta(
                oldValue = TextFieldValue("Hello World", selection = TextRange(11)),
                newValue = TextFieldValue("Hello", selection = TextRange(5)),
                notallyModel = model,
            )
        assertNotNull(deleteDelta)
        assertEquals(5, deleteDelta!!.startOffset)
        assertEquals(" World", deleteDelta.deletedText.text)
        assertEquals("", deleteDelta.insertedText.text)

        // Identical: returns null
        val noOpDelta =
            computeBodyDelta(
                oldValue = TextFieldValue("Same"),
                newValue = TextFieldValue("Same"),
                notallyModel = model,
            )
        assertNull(noOpDelta)
    }

    @Test
    fun `computeBodyDelta handles formatting changes with identical text`() {
        val model = createMockModel(TextFieldValue())
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val oldVal = TextFieldValue(AnnotatedString("Hello World"))
        val newVal =
            TextFieldValue(
                AnnotatedString(
                    text = "Hello World",
                    spanStyles = listOf(AnnotatedString.Range(boldStyle, 6, 11)),
                ),
                selection = TextRange(6, 11),
            )

        val delta = computeBodyDelta(oldVal, newVal, model)
        assertNotNull(delta)
        assertEquals(6, delta!!.startOffset)
        assertEquals("World", delta.deletedText.text)
        assertEquals(0, delta.deletedText.spanStyles.size)
        assertEquals("World", delta.insertedText.text)
        assertEquals(1, delta.insertedText.spanStyles.size)
    }

    @Test
    fun `NoteBodyDeltaChange undo and redo restores body and selection`() {
        var updated: TextFieldValue? = null
        val initial = TextFieldValue("Hello", selection = TextRange(5))
        val model = createMockModel(initial)

        val change =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 5,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString(" World"),
                oldSelection = TextRange(5),
                newSelection = TextRange(11),
                onBodyUpdated = { updated = it },
            )

        // Assume redo/initial apply was done
        model.body = TextFieldValue("Hello World", selection = TextRange(11))

        // Undo
        change.undo()
        assertEquals("Hello", model.body.text)
        assertEquals(TextRange(5), model.body.selection)
        assertEquals(model.body, updated)

        // Redo
        change.redo()
        assertEquals("Hello World", model.body.text)
        assertEquals(TextRange(11), model.body.selection)
        assertEquals(model.body, updated)
    }

    @Test
    fun `tryMergeWith merges consecutive single character keystrokes within window`() {
        val model = createMockModel(TextFieldValue())
        val t0 = 1000L

        val c1 =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 0,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("H"),
                oldSelection = TextRange(0),
                newSelection = TextRange(1),
                timestamp = t0,
            )

        val c2 =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 1,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("e"),
                oldSelection = TextRange(1),
                newSelection = TextRange(2),
                timestamp = t0 + 200L,
            )

        assertTrue(c1.tryMergeWith(c2))
        assertEquals("He", c1.insertedText.text)
        assertEquals(TextRange(2), c1.newSelection)
        assertEquals(t0 + 200L, c1.timestamp)

        // Merging third character
        val c3 =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 2,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("l"),
                oldSelection = TextRange(2),
                newSelection = TextRange(3),
                timestamp = t0 + 400L,
            )

        assertTrue(c1.tryMergeWith(c3))
        assertEquals("Hel", c1.insertedText.text)

        // Undo after merged changes restores empty string
        model.body = TextFieldValue("Hel", selection = TextRange(3))
        c1.undo()
        assertEquals("", model.body.text)
        assertEquals(TextRange(0), model.body.selection)
    }

    @Test
    fun `tryMergeWith rejects non consecutive or expired keystrokes`() {
        val model = createMockModel(TextFieldValue())
        val t0 = 1000L

        val c1 =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 0,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("A"),
                oldSelection = TextRange(0),
                newSelection = TextRange(1),
                timestamp = t0,
            )

        // Expired (> 1000ms)
        val expired =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 1,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("B"),
                oldSelection = TextRange(1),
                newSelection = TextRange(2),
                timestamp = t0 + 1500L,
            )
        assertFalse(c1.tryMergeWith(expired))

        // Non-consecutive offset
        val nonConsecutive =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 5,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("B"),
                oldSelection = TextRange(5),
                newSelection = TextRange(6),
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(nonConsecutive))

        // Newline
        val newline =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 1,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("\n"),
                oldSelection = TextRange(1),
                newSelection = TextRange(2),
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(newline))

        // Multi-char insert (e.g. paste)
        val multiChar =
            NoteBodyDeltaChange(
                notallyModel = model,
                startOffset = 1,
                deletedText = AnnotatedString(""),
                insertedText = AnnotatedString("BC"),
                oldSelection = TextRange(1),
                newSelection = TextRange(3),
                timestamp = t0 + 100L,
            )
        assertFalse(c1.tryMergeWith(multiChar))
    }
}
