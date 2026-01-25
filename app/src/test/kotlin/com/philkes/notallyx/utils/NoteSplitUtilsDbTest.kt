package com.philkes.notallyx.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
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
import com.philkes.notallyx.data.model.createNoteUrl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class NoteSplitUtilsDbTest {

    private lateinit var db: NotallyDatabase
    private lateinit var dao: BaseNoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, NotallyDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.getBaseNoteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun base(body: String, spans: List<SpanRepresentation> = emptyList()): BaseNote =
        BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = "DEFAULT" as ColorString,
            title = "T",
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

    @Test
    fun splitAndInsertForImport_insertsTwoParts_andAddsLinkSpan() = runBlocking {
        val overBy = 25
        val body = "x".repeat(MAX_BODY_CHAR_LENGTH + overBy)
        val original = base(body)

        val (firstId, inserted) = NoteSplitUtils.splitAndInsertForImport(original, dao)

        // We expect two parts inserted
        assertEquals(2, inserted.size)

        // Load all notes and sort by id
        val notes = dao.getAll().sortedBy { it.id }
        assertEquals(2, notes.size)
        val finalPart = notes[0]
        val firstPart = notes[1]

        // First part body must end with the link text and have length == MAX
        assertTrue(firstPart.body.endsWith(NoteSplitUtils.LINK_TEXT))
        assertEquals(MAX_BODY_CHAR_LENGTH, firstPart.body.length)

        // Final part must not contain link text and be <= MAX
        assertFalse(finalPart.body.endsWith(NoteSplitUtils.LINK_TEXT))
        assertTrue(finalPart.body.length <= MAX_BODY_CHAR_LENGTH)

        // Link span should point to the id of the next (final) part
        val linkSpan = firstPart.spans.firstOrNull { it.link }
        assertTrue(linkSpan != null)
        val expectedUrl = finalPart.id.createNoteUrl(Type.NOTE)
        assertEquals(expectedUrl, linkSpan!!.linkData)

        // Link span position should map to "\n\nOpen next part" located at end
        val linkTextOnly = "Open next part"
        val linkStartExpected = firstPart.body.length - linkTextOnly.length
        val linkEndExpected = firstPart.body.length
        assertEquals(linkStartExpected, linkSpan.start)
        assertEquals(linkEndExpected, linkSpan.end)

        // Returned firstId should correspond to the first part's id (the one with link)
        assertEquals(firstPart.id, firstId)
    }
}
