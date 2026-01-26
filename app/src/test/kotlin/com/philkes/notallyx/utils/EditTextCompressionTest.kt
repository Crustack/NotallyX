package com.philkes.notallyx.utils

import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import com.philkes.notallyx.test.mockAndroidLog
import com.philkes.notallyx.utils.changehistory.EditTextState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class EditTextCompressionTest {

    @Before
    fun setUp() {
        // Silence Android Log calls in unit tests
        mockAndroidLog()
    }

    @Test
    fun `EditTextState stores uncompressed Editable when at or below threshold`() {
        val text = "a".repeat(CompressUtility.COMPRESSION_THRESHOLD)
        val editable = Editable.Factory.getInstance().newEditable(text)
        val state = EditTextState(editable, cursorPos = 0)

        // Reflect to access the private field textContent and assert it's an Editable
        val field =
            EditTextState::class.java.getDeclaredField("textContent").apply { isAccessible = true }
        val content = field.get(state)
        assertTrue("Expected Editable for small text", content is Editable)

        // getEditableText should return the same text
        val out = state.getEditableText()
        assertEquals(text, out.toString())
    }

    @Test
    fun `EditTextState compresses large text and round-trips text and spans`() {
        // Create text larger than threshold with distinct regions for spans
        val threshold = CompressUtility.COMPRESSION_THRESHOLD
        val prefix = "p".repeat(10)
        val body = "b".repeat(threshold + 100)
        val suffix = "s".repeat(10)
        val longText = prefix + body + suffix

        val editable = Editable.Factory.getInstance().newEditable(longText)

        // Apply multiple span types on defined ranges
        // bold on [5, 25)
        editable.setSpan(StyleSpan(Typeface.BOLD), 5, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // italic on [30, 60)
        editable.setSpan(StyleSpan(Typeface.ITALIC), 30, 60, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // monospace on [100, 130)
        editable.setSpan(TypefaceSpan("monospace"), 100, 130, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // strikethrough on [200, 240)
        editable.setSpan(StrikethroughSpan(), 200, 240, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // link on [300, 330)
        editable.setSpan(URLSpan("https://example.com"), 300, 330, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val state = EditTextState(editable, cursorPos = 42)

        // Verify compressed backing storage via reflection
        val field =
            EditTextState::class.java.getDeclaredField("textContent").apply { isAccessible = true }
        val content = field.get(state)
        assertTrue("Expected ByteArray backing for large text", content is ByteArray)

        // Decompress and verify text and spans are preserved
        val out = state.getEditableText()
        assertEquals(longText, out.toString())

        // Validate spans exist at the same ranges
        fun hasSpan(
            rangeStart: Int,
            rangeEnd: Int,
            clazz: Class<*>,
            extra: (Any) -> Boolean = { true },
        ): Boolean {
            val spans = out.getSpans(rangeStart, rangeEnd, clazz)
            return spans.any { span ->
                val s = out.getSpanStart(span)
                val e = out.getSpanEnd(span)
                s == rangeStart && e == rangeEnd && extra(span)
            }
        }

        assertTrue(
            hasSpan(5, 25, StyleSpan::class.java) { (it as StyleSpan).style == Typeface.BOLD }
        )
        assertTrue(
            hasSpan(30, 60, StyleSpan::class.java) { (it as StyleSpan).style == Typeface.ITALIC }
        )
        assertTrue(
            hasSpan(100, 130, TypefaceSpan::class.java) {
                (it as TypefaceSpan).family == "monospace"
            }
        )
        assertTrue(hasSpan(200, 240, StrikethroughSpan::class.java))
        assertTrue(
            hasSpan(300, 330, URLSpan::class.java) { (it as URLSpan).url == "https://example.com" }
        )
    }
}
