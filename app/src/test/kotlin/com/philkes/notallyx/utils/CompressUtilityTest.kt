package com.philkes.notallyx.utils

import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.test.mockAndroidLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompressUtilityTest {

    @Before
    fun setUp() {
        // Silence android.util.Log in unit tests
        mockAndroidLog()
    }

    @Test
    fun `compressTextAndSpans round-trips text and spans`() {
        val text = buildString {
            append("Header ")
            append("x".repeat(500))
            append(" Middle ")
            append("y".repeat(500))
            append(" Footer")
        }
        val spans =
            listOf(
                SpanRepresentation(start = 0, end = 6, bold = true),
                SpanRepresentation(start = 10, end = 20, italic = true),
                SpanRepresentation(start = 40, end = 65, monospace = true),
                SpanRepresentation(start = 80, end = 95, strikethrough = true),
                SpanRepresentation(
                    start = 100,
                    end = 120,
                    link = true,
                    linkData = "https://example.com",
                ),
            )

        val compressed = CompressUtility.compressTextAndSpans(text, spans)

        // Expect some bytes and that Zstd frame is decompressible
        assertTrue(compressed.isNotEmpty())

        val (outText, outSpans) = CompressUtility.decompressTextAndSpans(compressed)

        assertEquals(text, outText)

        // Order is preserved as encoded in JSON; compare element-wise
        assertEquals(spans.size, outSpans.size)
        spans.indices.forEach { i ->
            assertEquals(spans[i].start, outSpans[i].start)
            assertEquals(spans[i].end, outSpans[i].end)
            assertEquals(spans[i].bold, outSpans[i].bold)
            assertEquals(spans[i].italic, outSpans[i].italic)
            assertEquals(spans[i].monospace, outSpans[i].monospace)
            assertEquals(spans[i].strikethrough, outSpans[i].strikethrough)
            assertEquals(spans[i].link, outSpans[i].link)
            assertEquals(spans[i].linkData, outSpans[i].linkData)
        }
    }

    @Test
    fun `compressTextAndSpans handles empty spans`() {
        val text = "a".repeat(1024)
        val spans = emptyList<SpanRepresentation>()

        val compressed = CompressUtility.compressTextAndSpans(text, spans)
        assertTrue(compressed.isNotEmpty())

        val (outText, outSpans) = CompressUtility.decompressTextAndSpans(compressed)
        assertEquals(text, outText)
        assertTrue(outSpans.isEmpty())
    }

    @Test
    fun `decompressTextAndSpans returns empty on invalid data`() {
        val invalid = byteArrayOf(1, 2, 3, 4, 5, 6)

        val (outText, outSpans) = CompressUtility.decompressTextAndSpans(invalid)

        assertEquals("", outText)
        assertTrue(outSpans.isEmpty())
    }
}
