package com.philkes.notallyx.utils

import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSplitUtilsTest {

    private fun baseNote(body: String, spans: List<SpanRepresentation> = emptyList()): BaseNote {
        return BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = "DEFAULT" as ColorString,
            title = "Title",
            pinned = false,
            timestamp = 0L,
            modifiedTimestamp = 0L,
            labels = emptyList(),
            body = body,
            spans = spans,
            items = emptyList<ListItem>(),
            images = emptyList<FileAttachment>(),
            files = emptyList<FileAttachment>(),
            audios = emptyList(),
            reminders = emptyList<Reminder>(),
            viewMode = NoteViewMode.EDIT,
        )
    }

    @Test
    fun sliceSpans_intersectionsAreClippedAndShifted() {
        val spans =
            listOf(
                SpanRepresentation(0, 5, bold = true), // fully inside
                SpanRepresentation(4, 10, italic = true), // overlaps end
                SpanRepresentation(10, 15, monospace = true), // outside
            )
        val sliced = NoteSplitUtils.sliceSpans(spans, rangeStart = 0, rangeEnd = 8, shift = -0)

        // Expect first unchanged, second clipped to end 8, third dropped
        assertEquals(2, sliced.size)
        assertEquals(SpanRepresentation(0, 5, bold = true), sliced[0])
        assertEquals(SpanRepresentation(4, 8, italic = true), sliced[1])
    }

    @Test
    fun sliceIntoChunks_reservesSpaceForLinkAndProducesTwoParts() {
        val overBy = 10
        val linkTextLen = NoteSplitUtils.LINK_TEXT.length
        val body = "x".repeat(MAX_BODY_CHAR_LENGTH + overBy)
        val note = baseNote(body)

        val chunks = NoteSplitUtils.sliceIntoChunks(note, linkTextLen)

        assertEquals("Expected two chunks when exceeding by a small amount", 2, chunks.size)
        val first = chunks[0].first
        val second = chunks[1].first

        // First chunk should be shortened by link length
        assertEquals(MAX_BODY_CHAR_LENGTH - linkTextLen, first.length)
        // Remaining should be <= MAX
        assertTrue(second.length <= MAX_BODY_CHAR_LENGTH)
        // Combined with link text, first part would hit the MAX limit
        assertEquals(MAX_BODY_CHAR_LENGTH, first.length + linkTextLen)
    }
}
