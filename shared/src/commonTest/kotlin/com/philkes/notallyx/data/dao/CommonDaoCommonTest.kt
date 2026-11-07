package com.philkes.notallyx.data.dao

import com.philkes.notallyx.core.database.DesktopDatabaseDriverFactory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.createNoteUrl
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Multiplatform unit tests for [CommonDao] running against in-memory SQLite with
 * BundledSQLiteDriver.
 */
class CommonDaoCommonTest {

    private lateinit var tempDir: File
    private lateinit var db: NotallyDatabase
    private lateinit var baseNoteDao: BaseNoteDao
    private lateinit var labelDao: LabelDao
    private lateinit var commonDao: CommonDao

    @Before
    fun setUp() {
        tempDir =
            File(
                    System.getProperty("java.io.tmpdir"),
                    "commondao-test-${System.nanoTime()}",
                )
                .apply { mkdirs() }
        val pathProvider = DesktopPathProvider(baseDirOverride = tempDir)
        val driverFactory = DesktopDatabaseDriverFactory(pathProvider)
        db = NotallyDatabase.createDatabase(driverFactory, inMemory = true)
        baseNoteDao = db.getBaseNoteDao()
        labelDao = db.getLabelDao()
        commonDao = db.getCommonDao()
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    private fun createTestNote(
        title: String,
        body: String = "Test Body",
        type: Type = Type.NOTE,
        spans: List<SpanRepresentation> = emptyList(),
        items: List<ListItem> = emptyList(),
        labels: List<String> = emptyList(),
    ): BaseNote =
        BaseNote(
            id = 0,
            type = type,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = title,
            pinned = false,
            timestamp = System.currentTimeMillis(),
            modifiedTimestamp = System.currentTimeMillis(),
            labels = labels,
            body = body,
            spans = spans,
            items = items,
            images = emptyList<FileAttachment>(),
            files = emptyList<FileAttachment>(),
            audios = emptyList(),
            reminders = emptyList<Reminder>(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    @Test
    fun deleteLabel_removesLabelFromNotesAndLabelTable() = runTest {
        labelDao.insert(Label("Work", 0))
        labelDao.insert(Label("Personal", 1))

        val noteId =
            baseNoteDao.insert(
                createTestNote(
                    title = "Work Note",
                    labels = listOf("Work", "Personal"),
                )
            )

        commonDao.deleteLabel("Work")

        val labels = labelDao.getAll().first()
        assertEquals(1, labels.size)
        assertEquals("Personal", labels.first().value)

        val note = baseNoteDao.get(noteId)
        assertNotNull(note)
        assertEquals(listOf("Personal"), note!!.labels)
    }

    @Test
    fun updateLabel_renamesLabelInNotesAndLabelTable() = runTest {
        labelDao.insert(Label("OldWork", 0))

        val noteId =
            baseNoteDao.insert(
                createTestNote(
                    title = "Work Note",
                    labels = listOf("OldWork"),
                )
            )

        commonDao.updateLabel("OldWork", "NewWork")

        val labels = labelDao.getAll().first()
        assertEquals(1, labels.size)
        assertEquals("NewWork", labels.first().value)

        val note = baseNoteDao.get(noteId)
        assertNotNull(note)
        assertEquals(listOf("NewWork"), note!!.labels)
    }

    @Test
    fun importBackup_remapsInternalNoteLinks() = runTest {
        val oldNote1Id = 101L
        val oldNote2Id = 102L
        val oldListNoteId = 103L

        val externalUrl = "https://example.com"
        val note2Url = oldNote2Id.createNoteUrl(Type.NOTE)
        val listNoteUrl = oldListNoteId.createNoteUrl(Type.LIST)

        val note1 =
            createTestNote(
                title = "Note 1 with Links",
                body = "Link to Note2, Link to List, External",
                type = Type.NOTE,
                spans =
                    listOf(
                        SpanRepresentation(
                            link = true,
                            linkData = note2Url,
                            start = 0,
                            end = 13,
                        ),
                        SpanRepresentation(
                            link = true,
                            linkData = listNoteUrl,
                            start = 15,
                            end = 27,
                        ),
                        SpanRepresentation(
                            link = true,
                            linkData = externalUrl,
                            start = 29,
                            end = 37,
                        ),
                    ),
            )

        val note2 =
            createTestNote(
                title = "Target Note 2",
                body = "Content of Note 2",
                type = Type.NOTE,
            )

        val listNote =
            createTestNote(
                title = "Target List Note",
                type = Type.LIST,
                items =
                    listOf(
                        ListItem(
                            id = 0,
                            body = "Item 1",
                            checked = false,
                            isChild = false,
                            order = 1,
                            children = mutableListOf(),
                        )
                    ),
            )

        val result =
            commonDao.importBackup(
                baseNotes = listOf(note1, note2, listNote),
                originalIds = listOf(oldNote1Id, oldNote2Id, oldListNoteId),
                labels = emptyList(),
                readCorrupted = 0,
                checkDuplicates = false,
            )

        assertEquals(3, result.inserted)
        assertEquals(0, result.duplicates)
        assertEquals(0, result.corruptedNotes)

        val allNotes = baseNoteDao.getAll()
        val importedNote1 = allNotes.find { it.title == "Note 1 with Links" }
        val importedNote2 = allNotes.find { it.title == "Target Note 2" }
        val importedListNote = allNotes.find { it.title == "Target List Note" }

        assertNotNull(importedNote1)
        assertNotNull(importedNote2)
        assertNotNull(importedListNote)

        // IDs must have changed upon import
        assertNotEquals(oldNote1Id, importedNote1!!.id)
        assertNotEquals(oldNote2Id, importedNote2!!.id)
        assertNotEquals(oldListNoteId, importedListNote!!.id)

        // Internal note links must be remapped to the newly assigned IDs
        val expectedNewNote2Url = importedNote2.id.createNoteUrl(Type.NOTE)
        val expectedNewListNoteUrl = importedListNote.id.createNoteUrl(Type.LIST)

        val spans = importedNote1.spans
        assertEquals(3, spans.size)

        val remappedNote2Span = spans.find { it.start == 0 }
        assertNotNull(remappedNote2Span)
        assertEquals(expectedNewNote2Url, remappedNote2Span!!.linkData)

        val remappedListSpan = spans.find { it.start == 15 }
        assertNotNull(remappedListSpan)
        assertEquals(expectedNewListNoteUrl, remappedListSpan!!.linkData)

        val preservedExternalSpan = spans.find { it.start == 29 }
        assertNotNull(preservedExternalSpan)
        assertEquals(externalUrl, preservedExternalSpan!!.linkData)
    }

    @Test
    fun importBackup_remapsInternalNoteLinks_toExistingDuplicates() = runTest {
        // Pre-insert target note into database as existing note
        val existingTarget =
            createTestNote(
                title = "Duplicate Target Note",
                body = "Exact matching body for duplicate detection",
                type = Type.NOTE,
            )
        val existingTargetId = baseNoteDao.insert(existingTarget)

        val oldNote1Id = 501L
        val oldTargetId = 502L

        val note1 =
            createTestNote(
                title = "Source Note",
                body = "Link to duplicate target",
                type = Type.NOTE,
                spans =
                    listOf(
                        SpanRepresentation(
                            link = true,
                            linkData = oldTargetId.createNoteUrl(Type.NOTE),
                            start = 0,
                            end = 10,
                        )
                    ),
            )

        val duplicateTarget =
            createTestNote(
                title = "Duplicate Target Note",
                body = "Exact matching body for duplicate detection",
                type = Type.NOTE,
            )

        val result =
            commonDao.importBackup(
                baseNotes = listOf(note1, duplicateTarget),
                originalIds = listOf(oldNote1Id, oldTargetId),
                labels = emptyList(),
                readCorrupted = 0,
                checkDuplicates = true,
            )

        assertEquals(1, result.inserted)
        assertEquals(1, result.duplicates)

        val allNotes = baseNoteDao.getAll()
        val importedSource = allNotes.find { it.title == "Source Note" }
        assertNotNull(importedSource)

        val expectedUrl = existingTargetId.createNoteUrl(Type.NOTE)
        assertEquals(1, importedSource!!.spans.size)
        assertEquals(expectedUrl, importedSource.spans.first().linkData)
    }

    @Test
    fun importBackup_unmatchedInternalNoteLink_remainsUnchanged() = runTest {
        val oldNoteId = 601L
        val missingTargetId = 9999L
        val originalUrl = missingTargetId.createNoteUrl(Type.NOTE)

        val note =
            createTestNote(
                title = "Note With Missing Target",
                body = "Points to missing note",
                spans =
                    listOf(
                        SpanRepresentation(
                            link = true,
                            linkData = originalUrl,
                            start = 0,
                            end = 10,
                        )
                    ),
            )

        val result =
            commonDao.importBackup(
                baseNotes = listOf(note),
                originalIds = listOf(oldNoteId),
                labels = emptyList(),
                readCorrupted = 0,
                checkDuplicates = false,
            )

        assertEquals(1, result.inserted)
        val imported = baseNoteDao.getAll().find { it.title == "Note With Missing Target" }
        assertNotNull(imported)
        assertEquals(originalUrl, imported!!.spans.first().linkData)
    }
}
