package com.philkes.notallyx.core.database

import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.SpanRepresentation
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    @Test
    fun `labelsToJson and jsonToLabels round-trip and malformed fallback`() {
        val labels = listOf("Work", "Personal", "2026/Goals")
        val json = Converters.labelsToJson(labels)
        assertEquals("""["Work","Personal","2026/Goals"]""", json)
        assertEquals(labels, Converters.jsonToLabels(json))

        // Malformed fallback
        assertTrue(Converters.jsonToLabels("{invalid json").isEmpty())
    }

    @Test
    fun `filesToJson and jsonToFiles round-trip with v1 name and v11 localName-originalName`() {
        val files =
            listOf(
                FileAttachment("img_001.jpg", "vacation.jpg", "image/jpeg"),
                FileAttachment("doc_002.pdf", "spec.pdf", "application/pdf"),
            )
        val json = Converters.filesToJson(files)
        assertEquals(files, Converters.jsonToFiles(json))

        // Legacy v1 format with only "name" field
        val legacyJson =
            """[{"name":"content://some/path/file123.pdf","mimeType":"application/pdf"}]"""
        val legacyParsed = Converters.jsonToFiles(legacyJson)
        assertEquals(
            listOf(
                FileAttachment("content://some/path/file123.pdf", "file123.pdf", "application/pdf")
            ),
            legacyParsed,
        )

        // Malformed fallback
        assertTrue(Converters.jsonToFiles("not a json array").isEmpty())
    }

    @Test
    fun `jsonToFiles input with localName and originalName fields`() {
        val json =
            """
            [
                {
                    "localName": "content://some/random/path/123435",
                    "originalName": "file1.pdf",
                    "mimeType": "application/pdf"
                },
                 {
                    "localName": "content://some/other/random/path/123435",
                    "originalName": "random.jpeg",
                    "mimeType": "image/jpeg"
                }
            ]
            """
                .trimIndent()

        val files = Converters.jsonToFiles(json)

        assertEquals(2, files.size)
        assertEquals(
            FileAttachment("content://some/random/path/123435", "file1.pdf", "application/pdf"),
            files[0],
        )
        assertEquals(
            FileAttachment("content://some/other/random/path/123435", "random.jpeg", "image/jpeg"),
            files[1],
        )
    }

    @Test
    fun `jsonToFiles input with only name field`() {
        val json =
            """
            [
                {
                    "name": "content://some/random/path/123435",
                    "mimeType": "application/pdf"
                },
                 {
                    "name": "content://some/other/random/path/123435",
                    "mimeType": "image/jpeg"
                }
            ]
            """
                .trimIndent()

        val files = Converters.jsonToFiles(json)

        assertEquals(2, files.size)
        assertEquals(
            FileAttachment("content://some/random/path/123435", "123435", "application/pdf"),
            files[0],
        )
        assertEquals(
            FileAttachment("content://some/other/random/path/123435", "123435", "image/jpeg"),
            files[1],
        )
    }

    @Test
    fun `audiosToJson and jsonToAudios round-trip with nullable duration`() {
        val audios =
            listOf(
                Audio("voice_1.m4a", 4500L, 1700000000000L),
                Audio("voice_2.m4a", null, 1700000005000L),
            )
        val json = Converters.audiosToJson(audios)
        assertEquals(audios, Converters.jsonToAudios(json))
        assertTrue(Converters.jsonToAudios("corrupted").isEmpty())
    }

    @Test
    fun `spansToJson and jsonToSpans round-trip and partial corruption resilience`() {
        val spans =
            listOf(
                SpanRepresentation(
                    start = 0,
                    end = 5,
                    bold = true,
                    link = true,
                    linkData = "https://example.com",
                    italic = false,
                    monospace = true,
                    strikethrough = false,
                ),
                SpanRepresentation(start = 6, end = 12, italic = true, strikethrough = true),
            )
        val json = Converters.spansToJson(spans)
        assertEquals(spans, Converters.jsonToSpans(json))

        // Missing start/end on one element is filtered out without losing valid elements
        val mixedJson = """[{"bold":true,"start":0,"end":4},{"bold":true}]"""
        val parsedMixed = Converters.jsonToSpans(mixedJson)
        assertEquals(1, parsedMixed.size)
        assertEquals(SpanRepresentation(0, 4, bold = true), parsedMixed[0])
    }

    @Test
    fun `itemsToJson and jsonToItems round-trip with optional order and checkedTimestamp`() {
        val items =
            listOf(
                ListItem(
                    body = "Parent task",
                    checked = false,
                    isChild = false,
                    order = 0,
                    children = mutableListOf(),
                    checkedTimestamp = null,
                ),
                ListItem(
                    body = "Child task",
                    checked = true,
                    isChild = true,
                    order = 1,
                    children = mutableListOf(),
                    checkedTimestamp = 1710000000000L,
                ),
            )
        val json = Converters.itemsToJson(items)
        assertEquals(items, Converters.jsonToItems(json))
        assertTrue(Converters.jsonToItems("invalid").isEmpty())
    }

    @Test
    fun `remindersToJson and jsonToReminders supports both nested JsonObject and stringified repetition`() {
        val reminders =
            listOf(
                Reminder(
                    id = 1L,
                    dateTime = Date(1743253506957L),
                    repetition =
                        Repetition(
                            value = 2,
                            unit = RepetitionTimeUnit.MONTHS,
                            occurrence = -1,
                            dayOfWeek = Calendar.FRIDAY,
                        ),
                    isNotificationVisible = true,
                ),
                Reminder(
                    id = 2L,
                    dateTime = Date(1743260000000L),
                    repetition = null,
                    isNotificationVisible = false,
                ),
            )
        val json = Converters.remindersToJson(reminders)
        val parsed = Converters.jsonToReminders(json)
        assertEquals(reminders, parsed)

        // Legacy stringified repetition field
        val legacyReminderJson =
            """[{"id":9,"dateTime":1742822940000,"repetition":"{\"value\":1,\"unit\":\"DAYS\"}","isNotificationVisible":false}]"""
        val parsedLegacy = Converters.jsonToReminders(legacyReminderJson)
        assertEquals(1, parsedLegacy.size)
        assertEquals(Repetition(1, RepetitionTimeUnit.DAYS), parsedLegacy[0].repetition)

        // Verify JSON structure matches expected keys
        val jsonArray = Json.parseToJsonElement(json)
        assertTrue(jsonArray.toString().contains("\"dateTime\":1743253506957"))
    }
}
