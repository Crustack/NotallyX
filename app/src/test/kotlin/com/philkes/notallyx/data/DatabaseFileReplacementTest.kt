package com.philkes.notallyx.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.databaseFiles
import com.philkes.notallyx.test.integrityCheckOnCopy
import com.philkes.notallyx.utils.databaseCompanionFiles
import com.philkes.notallyx.utils.deleteDatabaseCompanionFiles
import com.philkes.notallyx.utils.replaceDatabaseFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * Validates the P1 hardening for culprit #3/#4: every database file replacement (used by the
 * enable/disable-lock and change-storage-location flows) must be atomic and must delete the stale
 * `-wal`/`-shm`/`-journal` files of the replaced database. Leaving those behind makes SQLite
 * recover a write-ahead-log that belongs to a *different* database into the new file, which then
 * reports `SQLITE_CORRUPT` on the next open - exactly the data-loss scenario the users hit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DatabaseFileReplacementTest {

    private lateinit var application: Application
    private val workDir: File
        get() = application.getDatabasePath(NotallyDatabase.DATABASE_NAME).parentFile!!

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        workDir.mkdirs()
    }

    @After
    fun tearDown() {
        workDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Reproduces culprit #3: the target database has a leftover `-wal`/`-shm` from its own past.
     * Replacing its file while those stale companions remain makes the next open recover a foreign
     * write-ahead-log and corrupts the database. [replaceDatabaseFile] must instead leave a clean,
     * readable database containing exactly the replacement's notes.
     */
    @Test
    fun replaceDatabaseFile_removesStaleWalAndOpensCleanly() {
        val target = seedDatabaseFile("target", noteCount = 20)
        // Simulate a stale write-ahead-log/shared-memory index left over from the target database.
        File(workDir, "target-wal").writeBytes(ByteArray(4096) { 0x7F })
        File(workDir, "target-shm").writeBytes(ByteArray(4096) { 0x7F })

        val source = seedDatabaseFile("source", noteCount = 5)

        source.replaceDatabaseFile(target)

        // The stale companions of the replaced database must be gone.
        assertThat(File(workDir, "target-wal")).doesNotExist()
        assertThat(File(workDir, "target-shm")).doesNotExist()
        assertThat(File(workDir, "target-journal")).doesNotExist()
        // No leftover temporary/rollback files.
        assertThat(File(workDir, "target.tmp")).doesNotExist()
        assertThat(File(workDir, "target.rollback")).doesNotExist()

        assertThat(target.integrityCheckOnCopy()).isEqualTo("ok")
        assertThat(countNotes(target)).isEqualTo(5)
    }

    /**
     * If the source is missing the replacement must fail loudly and leave the existing target
     * database untouched - never delete it first and end up with nothing.
     */
    @Test
    fun replaceDatabaseFile_missingSource_keepsTargetIntact() {
        val target = seedDatabaseFile("target", noteCount = 7)
        val lengthBefore = target.length()
        val missingSource = File(workDir, "does-not-exist")

        assertThrows(IOException::class.java) { missingSource.replaceDatabaseFile(target) }

        assertThat(target).exists()
        assertThat(target.length()).isEqualTo(lengthBefore)
        assertThat(target.integrityCheckOnCopy()).isEqualTo("ok")
        assertThat(countNotes(target)).isEqualTo(7)
    }

    /** Replacing onto a not-yet-existing target simply installs the source there. */
    @Test
    fun replaceDatabaseFile_newTarget_isCreated() {
        val source = seedDatabaseFile("source", noteCount = 3)
        val target = File(workDir, "brand-new")
        assertThat(target).doesNotExist()

        source.replaceDatabaseFile(target)

        assertThat(target).exists()
        assertThat(target.integrityCheckOnCopy()).isEqualTo("ok")
        assertThat(countNotes(target)).isEqualTo(3)
    }

    /** The companion-file helpers point at and remove exactly the SQLite side files. */
    @Test
    fun databaseCompanionFiles_areResolvedAndDeleted() {
        val database = File(workDir, "companions")
        database.writeText("db")
        val companions = database.databaseCompanionFiles()
        assertThat(companions.map { it.name })
            .containsExactlyInAnyOrder("companions-wal", "companions-shm", "companions-journal")
        companions.forEach { it.writeText("stale") }

        database.deleteDatabaseCompanionFiles()

        companions.forEach { assertThat(it).doesNotExist() }
        assertThat(database).exists()
    }

    private fun seedDatabaseFile(name: String, noteCount: Int): File {
        val file = File(workDir, name)
        val database =
            NotallyDatabase.createBuilder(application, file.absolutePath)
                .allowMainThreadQueries()
                .build()
        runBlocking { repeat(noteCount) { index -> database.getBaseNoteDao().insert(note(index)) } }
        assertThat(database.getBaseNoteDao().count()).isEqualTo(noteCount)
        database.checkpointOrThrow()
        database.close()
        val (main, wal, shm) = file.databaseFiles()
        assertThat(main).exists()
        assertThat(wal).doesNotExist()
        assertThat(shm).doesNotExist()
        return file
    }

    private fun countNotes(file: File): Int {
        val database =
            NotallyDatabase.createBuilder(application, file.absolutePath)
                .allowMainThreadQueries()
                .build()
        return try {
            database.getBaseNoteDao().count()
        } finally {
            database.close()
        }
    }

    private fun note(index: Int) =
        BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = "DEFAULT",
            title = "Note $index",
            pinned = false,
            timestamp = index.toLong(),
            modifiedTimestamp = index.toLong(),
            labels = emptyList(),
            body = "Body of note $index. ".repeat(64),
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
}
