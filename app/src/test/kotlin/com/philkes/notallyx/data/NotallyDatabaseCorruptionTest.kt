package com.philkes.notallyx.data

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.NonDestructiveOpenHelperFactory
import com.philkes.notallyx.test.corruptPage
import com.philkes.notallyx.test.databaseFiles
import com.philkes.notallyx.test.destroySqliteHeaderMagic
import com.philkes.notallyx.test.rootPageOf
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import com.philkes.notallyx.utils.getExternalMediaDirectory
import com.philkes.notallyx.utils.getPrivateAttachmentsRoot
import java.io.File
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
import org.robolectric.shadows.ShadowLog

/**
 * Validates culprit #1 of `diagnose-database-wipe`: `NotallyDatabase` installs no corruption
 * handler, so Room inherits [SupportSQLiteOpenHelper.Callback.onCorruption], whose default
 * implementation **deletes** `NotallyDatabase`, `NotallyDatabase-wal` and `NotallyDatabase-shm`. A
 * single transient `SQLITE_CORRUPT` therefore destroys every note, while the attachments in the
 * media directories survive - exactly the shape of the user reports.
 *
 * [SQLiteMode.Mode.NATIVE] is asserted explicitly (it is Robolectric's default since 4.9): only in
 * native mode does the real AOSP `android.database.sqlite` code run, so `SQLiteQuery.fillWindow` ->
 * `onCorruption` -> `SQLiteDatabase.deleteDatabase` is executed for real instead of being emulated.
 *
 * Deliberately **not** covered: the SQLCipher configuration of `createInstance`, because
 * `System.loadLibrary("sqlcipher")` cannot load the `net.zetetic` native library on the JVM. Note
 * that path does not delete anyway, as `net.zetetic.database.DefaultDatabaseErrorHandler` returns
 * early when a codec is in use.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class NotallyDatabaseCorruptionTest {

    private lateinit var application: Application
    private var database: NotallyDatabase? = null

    private val databaseFile: File
        get() = NotallyDatabase.getInternalDatabaseFile(application)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        ShadowLog.clear()
        ShadowLog.stream = System.out
        databaseFile.parentFile?.mkdirs()
        databaseFile.databaseFiles().forEach { it.delete() }
    }

    @After
    fun tearDown() {
        database?.let { if (it.isOpen) it.close() }
        database = null
        databaseFile.databaseFiles().forEach { it.delete() }
    }

    /** TC1: the reported stack trace, reproduced - and the whole database is gone afterwards. */
    @Test
    fun queryOnCorruptBaseNotePage_deletesEntireDatabase() {
        val rootPage = seedAndClose()

        databaseFile.corruptPage(rootPage)

        val database = openDatabase()
        assertThrows(SQLiteException::class.java) {
            runBlocking { database.getBaseNoteDao().getAllPinnedToStatusNotes() }
        }

        assertDatabaseWasDeleted()
        assertCorruptionWasReportedByAndroidX()
    }

    /**
     * TC2: corruption detected *before* the database is open - the `db.isOpen == false` branch of
     * [SupportSQLiteOpenHelper.Callback.onCorruption]. This is the worst variant: AndroidX
     * (`FrameworkSQLiteOpenHelper.innerGetDatabase`) deletes the file and then **retries the
     * open**, so the app silently continues on a brand new empty database and no exception ever
     * reaches it - matching the reports where no exception precedes the data loss.
     */
    @Test
    fun openOnCorruptHeader_deletesDatabaseAndSilentlyRecreatesItEmpty() {
        seedAndClose()

        databaseFile.destroySqliteHeaderMagic()

        val database = openDatabase()
        assertThat(database.ping()).isTrue()

        assertCorruptionWasReportedByAndroidX()
        assertThat(database.getBaseNoteDao().count()).isZero()
        assertThat(runBlocking { database.getBaseNoteDao().getAllPinnedToStatusNotes() }).isEmpty()
    }

    /**
     * TC3: the counter-proof. The exact same corruption as TC1, but Room is built with an open
     * helper whose `onCorruption` does not delete - the query still fails, yet the database file
     * (and therefore every note in it) survives. So the deletion comes from the default callback,
     * and overriding it is by itself enough to prevent the data loss.
     */
    @Test
    fun corruptPageWithNonDestructiveHandler_keepsDatabaseFile() {
        val rootPage = seedAndClose()
        val lengthBeforeCorruption = databaseFile.length()

        databaseFile.corruptPage(rootPage)

        val factory = NonDestructiveOpenHelperFactory()
        val database = openDatabase(factory)
        assertThrows(SQLiteException::class.java) {
            runBlocking { database.getBaseNoteDao().getAllPinnedToStatusNotes() }
        }

        assertThat(factory.corruptionReported).isTrue()
        assertThat(databaseFile).exists()
        assertThat(databaseFile.length()).isEqualTo(lengthBeforeCorruption)
    }

    /** TC4: the notes are gone, every attachment is untouched - as the users described it. */
    @Test
    fun databaseWipe_leavesAttachmentsUntouched() {
        val attachments =
            listOf(SUBFOLDER_IMAGES, SUBFOLDER_FILES, SUBFOLDER_AUDIOS).map { subfolder ->
                val directory = File(attachmentsRoot(), subfolder).apply { mkdirs() }
                File(directory, "attachment.bin").apply { writeText("attachment in $subfolder") }
            }
        val rootPage = seedAndClose()

        databaseFile.corruptPage(rootPage)

        val database = openDatabase()
        assertThrows(SQLiteException::class.java) {
            runBlocking { database.getBaseNoteDao().getAllPinnedToStatusNotes() }
        }

        assertDatabaseWasDeleted()
        attachments.forEach { attachment ->
            assertThat(attachment).exists()
            assertThat(attachment.readText())
                .isEqualTo("attachment in ${attachment.parentFile!!.name}")
        }
    }

    /** Builds exactly the production Room configuration, optionally with a custom open helper. */
    private fun openDatabase(factory: SupportSQLiteOpenHelper.Factory? = null): NotallyDatabase {
        val builder =
            NotallyDatabase.createBuilder(application, NotallyDatabase.DATABASE_NAME)
                .allowMainThreadQueries()
        factory?.let { builder.openHelperFactory(it) }
        return builder.build().also { database = it }
    }

    /**
     * Inserts pinned notes into a real database file, returns `rootPageOf("BaseNote")` and closes
     * the instance. Asserts the file is on disk and its `-wal`/`-shm` are gone, so a corruption
     * that ends up pointing into stale data cannot make a test pass vacuously.
     */
    private fun seedAndClose(noteCount: Int = 20): Int {
        val database = openDatabase()
        runBlocking {
            repeat(noteCount) { index -> database.getBaseNoteDao().insert(pinnedNote(index)) }
        }
        assertThat(database.getBaseNoteDao().count()).isEqualTo(noteCount)
        val rootPage = database.rootPageOf("BaseNote")
        database.checkpoint()
        database.close()
        this.database = null

        val (main, wal, shm) = databaseFile.databaseFiles()
        assertThat(main).exists()
        assertThat(main.length()).isGreaterThan(0)
        assertThat(wal).doesNotExist()
        assertThat(shm).doesNotExist()
        return rootPage
    }

    private fun assertDatabaseWasDeleted() {
        databaseFile.databaseFiles().forEach { assertThat(it).doesNotExist() }
    }

    private fun assertCorruptionWasReportedByAndroidX() {
        val logs = ShadowLog.getLogs().filter { it.tag == SUPPORT_SQLITE_TAG }.map { it.msg ?: "" }
        assertThat(logs).anyMatch { it.contains("Corruption reported by sqlite on database:") }
        assertThat(logs).anyMatch { it.contains("deleting the database file:") }
    }

    /**
     * `externalMediaDirs` can be empty under Robolectric; either root is fine here, all that
     * matters is that the attachments live outside the database directory.
     */
    private fun attachmentsRoot(): File =
        try {
            application.getExternalMediaDirectory()
        } catch (_: Exception) {
            application.getPrivateAttachmentsRoot()
        }

    private fun pinnedNote(index: Int) =
        BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = "DEFAULT",
            title = "Pinned note $index",
            pinned = true,
            timestamp = index.toLong(),
            modifiedTimestamp = index.toLong(),
            labels = emptyList(),
            body = "Body of pinned note $index. ".repeat(64),
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = true,
        )

    companion object {
        /** `SupportSQLiteOpenHelper.Callback.onCorruption` logs under this tag. */
        private const val SUPPORT_SQLITE_TAG = "SupportSQLite"
    }
}
