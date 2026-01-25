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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class NoteRepairUtilsDbTest {

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

    private fun oversizedNote(): BaseNote {
        val body = "x".repeat(MAX_BODY_CHAR_LENGTH + 50)
        // Span goes beyond the max and should be clipped to MAX after repair
        val spans = listOf(SpanRepresentation(10, MAX_BODY_CHAR_LENGTH + 20, bold = true))
        return BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = "DEFAULT" as ColorString,
            title = "Oversized",
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
    fun truncateBodyAndFixSpans_clipsBodyAndSpans() = runBlocking {
        val id = dao.insert(oversizedNote())

        // Sanity: inserted body is oversized in this in-memory DB context
        var before = dao.get(id)!!
        assertTrue(before.body.length > MAX_BODY_CHAR_LENGTH)
        assertTrue(before.spans.first().end > MAX_BODY_CHAR_LENGTH)

        NoteRepairUtils.truncateBodyAndFixSpans(dao, id)

        val after = dao.get(id)!!
        assertEquals(MAX_BODY_CHAR_LENGTH, after.body.length)
        // Span should be clipped to end of body
        val span = after.spans.first()
        assertEquals(MAX_BODY_CHAR_LENGTH, span.end)
        assertEquals(10, span.start)
    }
}
