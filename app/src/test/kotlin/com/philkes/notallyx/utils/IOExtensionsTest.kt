package com.philkes.notallyx.utils

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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

    @Test
    fun `sha256Hash returns published digest for abc`() {
        val digest = ByteArrayInputStream("abc".toByteArray()).sha256Hash()
        val digestHex = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            digestHex,
        )
    }
}
