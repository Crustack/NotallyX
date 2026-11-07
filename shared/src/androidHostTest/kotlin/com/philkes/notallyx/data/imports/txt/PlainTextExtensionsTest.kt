package com.philkes.notallyx.feature.migration.imports.txt

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

    @Test
    fun `conversion roundtrip preserves hierarchy and checked states`() {
        val originalItems =
            listOf(
                com.philkes.notallyx.data.model.ListItem(
                    body = "Parent 1",
                    checked = false,
                    isChild = false,
                    order = 0,
                    children = mutableListOf(),
                ),
                com.philkes.notallyx.data.model.ListItem(
                    body = "Child 1.1",
                    checked = true,
                    isChild = true,
                    order = 1,
                    children = mutableListOf(),
                ),
                com.philkes.notallyx.data.model.ListItem(
                    body = "Child 1.2",
                    checked = false,
                    isChild = true,
                    order = 2,
                    children = mutableListOf(),
                ),
                com.philkes.notallyx.data.model.ListItem(
                    body = "Parent 2",
                    checked = true,
                    isChild = false,
                    order = 3,
                    children = mutableListOf(),
                ),
            )

        val anyCheckedOrChild = originalItems.any { it.checked || it.isChild }
        val text =
            if (anyCheckedOrChild) {
                originalItems.joinToString(separator = "\n") { item ->
                    val indent = if (item.isChild) "    " else ""
                    val check = if (item.checked) "[x]" else "[ ]"
                    "$indent- $check ${item.body}"
                }
            } else {
                originalItems.joinToString(separator = "\n") { it.body }
            }

        val syntax = text.findListSyntaxRegex(checkContains = true, plainNewLineAllowed = true)
        assertNotNull(syntax)
        val convertedItems = text.extractListItems(syntax!!)
        assertThat(convertedItems)
            .hasSize(4)
            .extracting("body", "checked", "isChild")
            .containsExactly(
                Tuple("Parent 1", false, false),
                Tuple("Child 1.1", true, true),
                Tuple("Child 1.2", false, true),
                Tuple("Parent 2", true, false),
            )
    }
}
