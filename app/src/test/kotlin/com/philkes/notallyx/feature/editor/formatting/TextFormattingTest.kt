package com.philkes.notallyx.feature.editor.formatting

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.philkes.notallyx.core.ui.extensions.addOrUpdateLink
import com.philkes.notallyx.core.ui.extensions.applySpans
import com.philkes.notallyx.core.ui.extensions.clearFormatting
import com.philkes.notallyx.core.ui.extensions.preserveStylesFrom
import com.philkes.notallyx.core.ui.extensions.removeLink
import com.philkes.notallyx.core.ui.extensions.setSpan
import com.philkes.notallyx.core.ui.extensions.toAnnotatedString
import com.philkes.notallyx.core.ui.extensions.toHtml
import com.philkes.notallyx.core.ui.extensions.toSpanRepresentations
import com.philkes.notallyx.core.ui.extensions.toggleBold
import com.philkes.notallyx.core.ui.extensions.toggleItalic
import com.philkes.notallyx.core.ui.extensions.toggleMonospace
import com.philkes.notallyx.core.ui.extensions.toggleStrikethrough
import com.philkes.notallyx.data.model.SpanRepresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFormattingTest {

    @Test
    fun `test toAnnotatedString and toSpanRepresentations round-trip`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val originalSpans =
            listOf(
                SpanRepresentation(start = 0, end = 3, bold = true),
                SpanRepresentation(start = 4, end = 9, italic = true),
                SpanRepresentation(start = 10, end = 15, monospace = true),
                SpanRepresentation(start = 16, end = 19, strikethrough = true),
                SpanRepresentation(
                    start = 20,
                    end = 25,
                    link = true,
                    linkData = "https://example.com",
                ),
                SpanRepresentation(start = 31, end = 34, bold = true, italic = true),
            )

        val annotated = text.toAnnotatedString(originalSpans)
        assertEquals(text, annotated.text)

        val roundTripSpans = annotated.toSpanRepresentations()
        assertEquals(originalSpans.size, roundTripSpans.size)

        for (i in originalSpans.indices) {
            val expected = originalSpans[i]
            val actual = roundTripSpans[i]
            assertEquals(expected.start, actual.start)
            assertEquals(expected.end, actual.end)
            assertEquals(expected.bold, actual.bold)
            assertEquals(expected.italic, actual.italic)
            assertEquals(expected.monospace, actual.monospace)
            assertEquals(expected.strikethrough, actual.strikethrough)
            assertEquals(expected.link, actual.link)
            if (expected.link) {
                assertEquals(expected.linkData, actual.linkData)
            }
        }
    }

    @Test
    fun `test toggleBold on unformatted text turns bold on`() {
        val text = "Hello World"
        val initial = TextFieldValue(text = text, selection = TextRange(0, 5))

        val updated = initial.toggleBold()

        val formatting = updated.annotatedString.getFormattingState(0, 5)
        assertTrue(formatting.isBold)
        assertFalse(formatting.isItalic)

        val unformattedRegion = updated.annotatedString.getFormattingState(6, 11)
        assertFalse(unformattedRegion.isBold)
    }

    @Test
    fun `test toggleBold on bold text turns bold off`() {
        val text = "Hello World"
        val initial = TextFieldValue(text = text, selection = TextRange(0, 5)).toggleBold()
        assertTrue(initial.annotatedString.getFormattingState(0, 5).isBold)

        val toggledOff = initial.toggleBold()
        assertFalse(toggledOff.annotatedString.getFormattingState(0, 5).isBold)
    }

    @Test
    fun `test toggleBold partial unbold splits bold range`() {
        val text = "Hello Beautiful World"
        // Make entire string bold
        val allBold =
            TextFieldValue(text = text, selection = TextRange(0, text.length)).toggleBold()

        // Unbold "Beautiful" (indices 6 to 15)
        val middleUnbold = allBold.copy(selection = TextRange(6, 15)).toggleBold()

        assertTrue(middleUnbold.annotatedString.getFormattingState(0, 5).isBold)
        assertFalse(middleUnbold.annotatedString.getFormattingState(6, 15).isBold)
        assertTrue(middleUnbold.annotatedString.getFormattingState(16, 21).isBold)
    }

    @Test
    fun `test toggleItalic, toggleStrikethrough, toggleMonospace`() {
        val text = "Testing formatting features"
        var value = TextFieldValue(text = text, selection = TextRange(0, 7))

        value = value.toggleItalic()
        assertTrue(value.annotatedString.getFormattingState(0, 7).isItalic)

        value = value.copy(selection = TextRange(8, 18)).toggleStrikethrough()
        assertTrue(value.annotatedString.getFormattingState(8, 18).isStrikethrough)

        value = value.copy(selection = TextRange(19, 27)).toggleMonospace()
        assertTrue(value.annotatedString.getFormattingState(19, 27).isMonospace)
    }

    @Test
    fun `test overlapping multiple styles`() {
        val text = "Complex formatting"
        var value = TextFieldValue(text = text, selection = TextRange(0, 10))

        // Bold on [0, 10)
        value = value.toggleBold()
        // Italic on [5, 15)
        value = value.copy(selection = TextRange(5, 15)).toggleItalic()

        // [0, 5) should be bold only
        val state0to5 = value.annotatedString.getFormattingState(0, 5)
        assertTrue(state0to5.isBold)
        assertFalse(state0to5.isItalic)

        // [5, 10) should be bold and italic
        val state5to10 = value.annotatedString.getFormattingState(5, 10)
        assertTrue(state5to10.isBold)
        assertTrue(state5to10.isItalic)

        // [10, 15) should be italic only
        val state10to15 = value.annotatedString.getFormattingState(10, 15)
        assertFalse(state10to15.isBold)
        assertTrue(state10to15.isItalic)

        // Now toggle off bold on [0, 10)
        value = value.copy(selection = TextRange(0, 10)).toggleBold()
        val stateAfterBoldOff = value.annotatedString.getFormattingState(5, 10)
        assertFalse(stateAfterBoldOff.isBold)
        assertTrue(stateAfterBoldOff.isItalic) // Italic must be preserved!
    }

    @Test
    fun `test clearFormatting on selected region`() {
        val text = "Clear this formatting please"
        var value =
            TextFieldValue(text = text, selection = TextRange(0, text.length))
                .toggleBold()
                .toggleItalic()

        // Clear formatting only on "this formatting" (indices 6 to 21)
        value = value.copy(selection = TextRange(6, 21)).clearFormatting()

        val startState = value.annotatedString.getFormattingState(0, 5)
        assertTrue(startState.isBold)
        assertTrue(startState.isItalic)

        val clearedState = value.annotatedString.getFormattingState(6, 21)
        assertFalse(clearedState.isBold)
        assertFalse(clearedState.isItalic)

        val endState = value.annotatedString.getFormattingState(22, 28)
        assertTrue(endState.isBold)
        assertTrue(endState.isItalic)
    }

    @Test
    fun `test clearFormatting with no selection clears entire string`() {
        val text = "All formatted"
        val value =
            TextFieldValue(text = text, selection = TextRange(0, text.length))
                .toggleBold()
                .toggleItalic()

        val fullyCleared = value.copy(selection = TextRange(5, 5)).clearFormatting()
        val state = fullyCleared.annotatedString.getFormattingState(0, text.length)
        assertFalse(state.isBold)
        assertFalse(state.isItalic)
    }

    @Test
    fun `test addOrUpdateLink`() {
        val text = "Click here for more"
        val value = TextFieldValue(text = text, selection = TextRange(6, 10))

        val updated = value.addOrUpdateLink("https://notallyx.org", "here")
        assertEquals(text, updated.text)

        val state = updated.annotatedString.getFormattingState(6, 10)
        assertTrue(state.isLink)

        val annotations = updated.annotatedString.getStringAnnotations("URL", 6, 10)
        assertEquals(1, annotations.size)
        assertEquals("https://notallyx.org", annotations.first().item)
    }

    @Test
    fun `test addOrUpdateLink with explicit range and text change`() {
        val text = "Click here for more"
        val value = TextFieldValue(text = text, selection = TextRange(0, 0))

        val updated =
            value.addOrUpdateLink("https://notallyx.org", "this link", range = TextRange(6, 10))
        assertEquals("Click this link for more", updated.text)

        val state = updated.annotatedString.getFormattingState(6, 15)
        assertTrue(state.isLink)

        val annotations = updated.annotatedString.getStringAnnotations("URL", 6, 15)
        assertEquals(1, annotations.size)
        assertEquals("https://notallyx.org", annotations.first().item)
    }

    @Test
    fun `test removeLink preserves text and other styles`() {
        val text = "Click here for more"
        var value = TextFieldValue(text = text, selection = TextRange(6, 10))
        value = value.toggleBold()
        value = value.addOrUpdateLink("https://notallyx.org", "here")

        var state = value.annotatedString.getFormattingState(6, 10)
        assertTrue(state.isLink)
        assertTrue(state.isBold)

        val withoutLink = value.removeLink(TextRange(6, 10))
        assertEquals(text, withoutLink.text)

        state = withoutLink.annotatedString.getFormattingState(6, 10)
        assertFalse(state.isLink)
        assertTrue(state.isBold)
        assertTrue(
            withoutLink.annotatedString.getStringAnnotations("URL", 0, text.length).isEmpty()
        )
    }

    @Test
    fun `test applySpans and Builder setSpan`() {
        val text = "Sample formatted text"
        val builder = AnnotatedString.Builder(text)
        builder.setSpan(SpanStyle(fontWeight = FontWeight.Bold), 0, 6)
        builder.setSpan("URL", "https://notallyx.org", 7, 16)
        // Out-of-bounds safety check
        builder.setSpan(SpanStyle(fontWeight = FontWeight.Bold), 50, 100)

        val annotatedFromBuilder = builder.toAnnotatedString()
        val spansFromBuilder = annotatedFromBuilder.toSpanRepresentations()
        assertEquals(2, spansFromBuilder.size)
        assertTrue(spansFromBuilder.any { it.bold && it.start == 0 && it.end == 6 })
        assertTrue(spansFromBuilder.any { it.link && it.linkData == "https://notallyx.org" })

        // Check applySpans produces identical result
        val spans =
            listOf(
                SpanRepresentation(start = 0, end = 6, bold = true),
                SpanRepresentation(
                    start = 7,
                    end = 16,
                    link = true,
                    linkData = "https://notallyx.org",
                ),
            )
        val applied = text.applySpans(spans)
        assertEquals(annotatedFromBuilder.text, applied.text)
        assertEquals(2, applied.toSpanRepresentations().size)
    }

    @Test
    fun `test AnnotatedString toHtml`() {
        val text = "Line 1 bold and link\nLine 2 italic"
        val spans =
            listOf(
                SpanRepresentation(start = 7, end = 11, bold = true),
                SpanRepresentation(
                    start = 16,
                    end = 20,
                    link = true,
                    linkData = "https://example.com",
                ),
                SpanRepresentation(start = 28, end = 34, italic = true),
            )
        val annotated = text.applySpans(spans)
        val html = annotated.toHtml()

        val expected =
            "<p dir=\"ltr\">Line 1 <b>bold</b> and <a href=\"https://example.com\">link</a></p>" +
                "<p dir=\"ltr\">Line 2 <i>italic</i></p>"
        assertEquals(expected, html)
    }

    @Test
    fun `test preserveStylesFrom when cursor moves or selection changes`() {
        val text = "Lorem ipsum dolor"
        // Bold "Lorem" (0..5)
        val initial = TextFieldValue(text = text, selection = TextRange(0, 5)).toggleBold()
        assertTrue(initial.annotatedString.getFormattingState(0, 5).isBold)

        // Simulate IME reporting selection change (e.g. cursor moved to 8..8 or selected 6..11)
        val selectionChange = TextFieldValue(text = text, selection = TextRange(6, 11))
        // Verify input from IME has empty spanStyles
        assertTrue(selectionChange.annotatedString.spanStyles.isEmpty())

        // preserveStylesFrom must keep the bold on "Lorem"
        val preserved = selectionChange.preserveStylesFrom(initial)
        assertEquals(text, preserved.text)
        assertEquals(TextRange(6, 11), preserved.selection)
        assertTrue(preserved.annotatedString.getFormattingState(0, 5).isBold)
        assertFalse(preserved.annotatedString.getFormattingState(6, 11).isBold)
    }

    @Test
    fun `test preserveStylesFrom when text is inserted at end`() {
        val initial = TextFieldValue(text = "Lorem", selection = TextRange(0, 5)).toggleBold()
        assertTrue(initial.annotatedString.getFormattingState(0, 5).isBold)

        // Type " ipsum" at end
        val typed = TextFieldValue(text = "Lorem ipsum", selection = TextRange(11, 11))
        val preserved = typed.preserveStylesFrom(initial)
        assertEquals("Lorem ipsum", preserved.text)
        assertTrue(preserved.annotatedString.getFormattingState(0, 5).isBold)
        assertFalse(preserved.annotatedString.getFormattingState(5, 11).isBold)
    }

    @Test
    fun `test preserveStylesFrom when text is inserted before styled text`() {
        val initial = TextFieldValue(text = "ipsum", selection = TextRange(0, 5)).toggleItalic()
        assertTrue(initial.annotatedString.getFormattingState(0, 5).isItalic)

        // Type "Lorem " at start
        val typed = TextFieldValue(text = "Lorem ipsum", selection = TextRange(6, 6))
        val preserved = typed.preserveStylesFrom(initial)
        assertEquals("Lorem ipsum", preserved.text)
        assertFalse(preserved.annotatedString.getFormattingState(0, 6).isItalic)
        assertTrue(preserved.annotatedString.getFormattingState(6, 11).isItalic)
    }
}
