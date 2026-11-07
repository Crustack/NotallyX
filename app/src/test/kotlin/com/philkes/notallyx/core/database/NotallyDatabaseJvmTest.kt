package com.philkes.notallyx.core.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.fakes.FakeDatabaseDriverFactory
import com.philkes.notallyx.test.fakes.FakePathProvider
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure-JVM unit tests for [NotallyDatabase] using [BundledSQLiteDriver] without Robolectric or
 * `android.content.Context`. Verifies schema migrations (v1 -> v11), CRUD operations, and
 * `checkpoint()`.
 */
class NotallyDatabaseJvmTest {

    private lateinit var pathProvider: FakePathProvider
    private lateinit var driverFactory: FakeDatabaseDriverFactory

    @Before
    fun setUp() {
        pathProvider = FakePathProvider()
        driverFactory = FakeDatabaseDriverFactory(pathProvider)
    }

    @After
    fun tearDown() {
        pathProvider.rootDirectory.deleteRecursively()
    }

    @Test
    fun `inMemory database CRUD and ping work on pure JVM without Robolectric`() = runTest {
        val db = NotallyDatabase.createDatabase(driverFactory, inMemory = true)
        try {
            assertTrue(db.ping())

            val note =
                BaseNote(
                    id = 0L,
                    type = Type.NOTE,
                    folder = Folder.NOTES,
                    color = "#FAAFA8",
                    title = "JVM Note",
                    pinned = true,
                    timestamp = 1700000000000L,
                    modifiedTimestamp = 1700000005000L,
                    labels = listOf("kmp", "desktop"),
                    body = "Hello from BundledSQLiteDriver",
                    spans = listOf(SpanRepresentation(start = 0, end = 5, bold = true)),
                    items =
                        listOf(
                            ListItem(
                                "Checklist item",
                                checked = true,
                                isChild = false,
                                order = 0,
                                children = mutableListOf(),
                            )
                        ),
                    images = listOf(FileAttachment("img.jpg", "photo.jpg", "image/jpeg")),
                    files = listOf(FileAttachment("doc.pdf", "manual.pdf", "application/pdf")),
                    audios = listOf(Audio("voice.m4a", 3000L, 1700000000000L)),
                    reminders =
                        listOf(
                            Reminder(
                                id = 1L,
                                dateTime = Date(1700000000000L),
                                repetition = null,
                                isNotificationVisible = false,
                            )
                        ),
                    viewMode = NoteViewMode.READ_ONLY,
                    isPinnedToStatus = false,
                )

            val insertedId = db.getBaseNoteDao().insert(note)
            db.getLabelDao().insert(Label("kmp", 0))
            db.getLabelDao().insert(Label("desktop", 1))

            val loaded = db.getBaseNoteDao().get(insertedId)
            assertNotNull(loaded)
            assertEquals("JVM Note", loaded!!.title)
            assertEquals("#FAAFA8", loaded.color)
            assertEquals(listOf("kmp", "desktop"), loaded.labels)
            assertEquals("Hello from BundledSQLiteDriver", loaded.body)
            assertEquals(1, loaded.spans.size)
            assertEquals(1, loaded.images.size)
            assertEquals(1, loaded.files.size)
            assertEquals(1, loaded.audios.size)
            assertEquals(1, loaded.reminders.size)
            assertEquals(NoteViewMode.READ_ONLY, loaded.viewMode)

            val labels = db.getLabelDao().getAll().first()
            assertEquals(2, labels.size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `file-backed database migrates v1 to v11 and supports checkpoint on pure JVM`() = runTest {
        val dbFile = pathProvider.getDatabaseFile(NotallyDatabase.DATABASE_NAME)
        val rawDriver = BundledSQLiteDriver()
        rawDriver.open(dbFile.absolutePath).use { conn ->
            conn.execSQL(
                "CREATE TABLE IF NOT EXISTS `BaseNote` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `folder` TEXT NOT NULL, `title` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `labels` TEXT NOT NULL, `body` TEXT NOT NULL, `spans` TEXT NOT NULL, `items` TEXT NOT NULL)"
            )
            conn.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_BaseNote_id_folder_pinned_timestamp_labels` ON `BaseNote` (`id`, `folder`, `pinned`, `timestamp`, `labels`)"
            )
            conn.execSQL(
                "CREATE TABLE IF NOT EXISTS `Label` (`value` TEXT NOT NULL, PRIMARY KEY(`value`))"
            )
            conn.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            conn.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '561523a76ccf21fe0a7ff6c1fef657a9')"
            )
            conn.execSQL(
                "INSERT INTO `BaseNote` (`id`, `type`, `folder`, `title`, `pinned`, `timestamp`, `labels`, `body`, `spans`, `items`) VALUES (1, 'NOTE', 'NOTES', 'Legacy v1 Note', 1, 1600000000000, '[\"work\"]', 'Migrated body', '[]', '[]')"
            )
            conn.execSQL("INSERT INTO `Label` (`value`) VALUES ('work')")
            conn.execSQL("INSERT INTO `Label` (`value`) VALUES ('archive')")
            conn.execSQL("PRAGMA user_version = 1")
        }

        val db = NotallyDatabase.createDatabase(driverFactory, inMemory = false)
        try {
            val migratedNote = db.getBaseNoteDao().get(1L)
            assertNotNull(migratedNote)
            assertEquals("Legacy v1 Note", migratedNote!!.title)
            assertEquals("Migrated body", migratedNote.body)
            assertEquals(listOf("work"), migratedNote.labels)
            assertEquals("DEFAULT", migratedNote.color)
            assertEquals(NoteViewMode.EDIT, migratedNote.viewMode)
            assertEquals(false, migratedNote.isPinnedToStatus)
            assertTrue(migratedNote.images.isEmpty())
            assertTrue(migratedNote.files.isEmpty())
            assertTrue(migratedNote.audios.isEmpty())
            assertTrue(migratedNote.reminders.isEmpty())

            val migratedLabels = db.getLabelDao().getAll().first()
            assertEquals(2, migratedLabels.size)
            // Migration11 orders labels by value DESC and assigns 0, 1, ...
            val workLabel = migratedLabels.first { it.value == "work" }
            val archiveLabel = migratedLabels.first { it.value == "archive" }
            assertEquals(0, workLabel.order)
            assertEquals(1, archiveLabel.order)

            assertTrue(db.checkpoint())
            assertTrue(db.ping())
        } finally {
            db.close()
        }
    }
}
