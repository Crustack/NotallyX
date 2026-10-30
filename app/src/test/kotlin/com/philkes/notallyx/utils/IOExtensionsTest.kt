package com.philkes.notallyx.utils

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IOExtensionsTest {
    @Test
    fun `sha256Hash returns equal digest bytes for identical content`() {
        val content = "unchanged export content".toByteArray()

        val originalDigest = ByteArrayInputStream(content).sha256Hash()
        val copiedDigest = ByteArrayInputStream(content.copyOf()).sha256Hash()

        assertArrayEquals(originalDigest, copiedDigest)
    }

    @Test
    fun `sha256Hash returns different digest bytes for modified content`() {
        val originalDigest = ByteArrayInputStream("original export content".toByteArray()).sha256Hash()
        val modifiedDigest = ByteArrayInputStream("modified export content".toByteArray()).sha256Hash()

        assertFalse(originalDigest.contentEquals(modifiedDigest))
    }
}
