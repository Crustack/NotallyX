package com.philkes.notallyx.data.imports.txt

import junit.framework.TestCase.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.Test

class PlainTextExtensionsTest {

    @Test
    fun `extractListItems NotallyX syntax`() {
        val text =
            """
            [ ] 🧪 10:00 AM - Chemistry Lab
            [✓] 📖 1:00 PM - History Lecture
            [✓] 🏋️ 5:00 PM - Gym Session
            """
                .trimIndent()

        val syntax = text.findListSyntaxRegex()
        assertNotNull(syntax)
        val items = text.extractListItems(syntax!!)
        assertThat(items)
            .hasSize(3)
            .extracting("body", "checked")
            .containsExactly(
                Tuple("🧪 10:00 AM - Chemistry Lab", false),
                Tuple("📖 1:00 PM - History Lecture", true),
                Tuple("🏋️ 5:00 PM - Gym Session", true),
            )
    }

    @Test
    fun `extractListItems Markdown syntax`() {
        val text =
            """
            - [ ] 🧪 10:00 AM - Chemistry Lab
            - [x] 📖 1:00 PM - History Lecture
            - [X] 🏋️ 5:00 PM - Gym Session
            """
                .trimIndent()

        val syntax = text.findListSyntaxRegex()
        assertNotNull(syntax)
        val items = text.extractListItems(syntax!!)
        assertThat(items)
            .hasSize(3)
            .extracting("body", "checked")
            .containsExactly(
                Tuple("🧪 10:00 AM - Chemistry Lab", false),
                Tuple("📖 1:00 PM - History Lecture", true),
                Tuple("🏋️ 5:00 PM - Gym Session", true),
            )
    }

    @Test
    fun `extractListItems isChild indentation`() {
        val text =
            """
            - [ ] Monday:
               - [ ] 🧪 10:00 AM - Chemistry Lab
               - [x] 📖 1:00 PM - History Lecture
               - [X] 🏋️ 5:00 PM - Gym Session
            """
                .trimIndent()

        val syntax = text.findListSyntaxRegex()
        assertNotNull(syntax)
        val items = text.extractListItems(syntax!!)
        assertThat(items)
            .hasSize(4)
            .extracting("body", "isChild")
            .containsExactly(
                Tuple("Monday:", false),
                Tuple("🧪 10:00 AM - Chemistry Lab", true),
                Tuple("📖 1:00 PM - History Lecture", true),
                Tuple("🏋️ 5:00 PM - Gym Session", true),
            )
    }

    @Test
    fun `extractListItems with checkContains`() {
        val text =
            """
            Monday:
            - 🧪 10:00 AM - Chemistry Lab
            - 📖 1:00 PM - History Lecture
            - 🏋️ 5:00 PM - Gym Session
            """
                .trimIndent()

        val syntax = text.findListSyntaxRegex(checkContains = true)
        assertNotNull(syntax)
        val items = text.extractListItems(syntax!!)
        assertThat(items)
            .hasSize(4)
            .extracting("body")
            .containsExactly(
                "Monday:",
                "🧪 10:00 AM - Chemistry Lab",
                "📖 1:00 PM - History Lecture",
                "🏋️ 5:00 PM - Gym Session",
            )
    }

    @Test
    fun `extractListItems with plainNewLineAllowed`() {
        val text =
            """
            🧪 10:00 AM - Chemistry Lab
            📖 1:00 PM - History Lecture
            🏋️ 5:00 PM - Gym Session
            """
                .trimIndent()

        val syntax = text.findListSyntaxRegex(plainNewLineAllowed = true)
        assertNotNull(syntax)
        val items = text.extractListItems(syntax!!)
        assertThat(items)
            .hasSize(3)
            .extracting("body")
            .containsExactly(
                "🧪 10:00 AM - Chemistry Lab",
                "📖 1:00 PM - History Lecture",
                "🏋️ 5:00 PM - Gym Session",
            )
    }
}
