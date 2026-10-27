package com.philkes.notallyx.preference

import android.app.Application
import android.os.Environment
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.utils.SUBFOLDER_BACKUPS
import com.philkes.notallyx.utils.backup.BackupHistory
import com.philkes.notallyx.utils.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.utils.backup.PERIODIC_BACKUP_FILE_PREFIX
import com.philkes.notallyx.utils.backup.SqliteSnapshotInspector
import com.philkes.notallyx.utils.backup.autoBackupOnSave
import com.philkes.notallyx.utils.backup.autoBackupOnSaveFileExists
import com.philkes.notallyx.utils.backup.createBackup
import com.philkes.notallyx.utils.backup.deleteModifiedNoteBackup
import com.philkes.notallyx.utils.backup.modifiedNoteBackupExists
import com.philkes.notallyx.utils.getDocumentFolder
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class NotallyXPreferencesBackupsTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(application)
            .edit()
            .clear()
            .commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        NotallyDatabase.clearInstance()
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
    }

    @After
    fun tearDown() {
        NotallyDatabase.clearInstance()
        NotallyXPreferences.clearInstance()
        application.deleteDatabase(NotallyDatabase.DATABASE_NAME)
        application.getExternalBackupsDirectory().listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Backups copy the database file, so the in-memory test database can't be used. This creates a
     * file based database and makes it the current instance.
     */
    private fun createFileDatabase(): NotallyDatabase =
        Room.databaseBuilder(
                application,
                NotallyDatabase::class.java,
                NotallyDatabase.DATABASE_NAME,
            )
            .allowMainThreadQueries()
            .build()
            .also { NotallyDatabase.postInstance(it) }

    private fun periodicBackupZips(): List<File> =
        application.getExternalBackupsDirectory().listFiles()?.filter {
            it.name.startsWith(PERIODIC_BACKUP_FILE_PREFIX) && it.name.endsWith(".zip")
        } ?: emptyList()

    private fun autoBackupZip(): File =
        File(application.getExternalBackupsDirectory(), "$ON_SAVE_BACKUP_FILE.zip")

    private fun autoBackupEntries(): List<String> =
        ZipFile(autoBackupZip()).fileHeaders.filterNot { it.isDirectory }.map { it.fileName }

    @Test
    fun defaultPreferences_backupsFolderAndPeriodicBackupsConfiguredCorrectly() {
        val expectedBackupDir = application.getExternalBackupsDirectory()
        val expectedBackupUriString = expectedBackupDir.toUri().toString()

        assertThat(preferences.backupsFolder.value)
            .isEqualTo(expectedBackupUriString)
            .contains(SUBFOLDER_BACKUPS)

        assertThat(preferences.periodicBackups.value)
            .isEqualTo(PeriodicBackup(periodInDays = 1, maxBackups = 3))
    }

    @Test
    fun getDocumentFolder_resolvesDefaultBackupsFolder() {
        val backupUri = preferences.backupsFolder.value.toUri()
        val documentFolder = application.getDocumentFolder(backupUri)

        assertThat(documentFolder).isNotNull
        assertThat(documentFolder!!.exists()).isTrue()
        assertThat(documentFolder.isDirectory).isTrue()
        assertThat(documentFolder.name).isEqualTo(SUBFOLDER_BACKUPS)
    }

    private fun createSampleNote(title: String, body: String): BaseNote =
        BaseNote(
            id = 0,
            type = Type.NOTE,
            folder = com.philkes.notallyx.data.model.Folder.NOTES,
            color = "DEFAULT",
            title = title,
            pinned = false,
            timestamp = System.currentTimeMillis(),
            modifiedTimestamp = System.currentTimeMillis(),
            labels = emptyList(),
            body = body,
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = com.philkes.notallyx.data.model.NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    @Test
    fun periodicBackupAndAutoBackupOnSave_workWithDefaultBackupsFolder() {
        runBlocking {
            val database = createFileDatabase()
            val noteDao = database.getBaseNoteDao()
            noteDao.insert(createSampleNote(title = "Test Note", body = "Test Body"))

            val backupResult = application.createBackup()
            assertThat(backupResult).isNotNull

            val backupsFolderFile = application.getExternalBackupsDirectory()
            val createdFiles =
                backupsFolderFile.listFiles()?.filter { it.name.endsWith(".zip") } ?: emptyList()
            assertThat(createdFiles).isNotEmpty
            assertThat(createdFiles).allMatch { it.length() > 0 }

            val note = noteDao.getAll().first()
            application.autoBackupOnSave(preferences.backupsFolder.value, "", note)

            assertThat(application.autoBackupOnSaveFileExists(preferences.backupsFolder.value))
                .isTrue()
            assertThat(application.modifiedNoteBackupExists(preferences.backupsFolder.value))
                .isTrue()

            application.deleteModifiedNoteBackup(preferences.backupsFolder.value)
            assertThat(application.modifiedNoteBackupExists(preferences.backupsFolder.value))
                .isFalse()
        }
    }

    @Test
    fun createBackup_withoutNotes_doesNotRotateOutOlderBackups() {
        runBlocking {
            val noteDao = createFileDatabase().getBaseNoteDao()
            preferences.periodicBackups.save(PeriodicBackup(periodInDays = 1, maxBackups = 1))
            noteDao.insert(createSampleNote(title = "Note", body = "Body"))
            application.createBackup()
            assertThat(periodicBackupZips()).hasSize(1)

            // The database got emptied: the backup with notes must survive
            noteDao.deleteFrom(Folder.NOTES)
            delay(20)
            application.createBackup()
            assertThat(periodicBackupZips()).hasSize(2)

            // Once there are notes again, rotation works as usual
            noteDao.insert(createSampleNote(title = "New Note", body = "Body"))
            delay(20)
            application.createBackup()
            assertThat(periodicBackupZips()).hasSize(1)
        }
    }

    @Test
    fun createBackup_deletesEmptyLeftoversOfInterruptedBackups() {
        runBlocking {
            val leftover =
                File(
                    application.getExternalBackupsDirectory(),
                    "${PERIODIC_BACKUP_FILE_PREFIX}leftover.zip",
                )
            leftover.createNewFile()
            createFileDatabase()
                .getBaseNoteDao()
                .insert(createSampleNote(title = "Note", body = "Body"))

            application.createBackup()

            assertThat(leftover).doesNotExist()
            assertThat(periodicBackupZips()).hasSize(1).allMatch { it.length() > 0 }
        }
    }

    @Test
    fun autoBackupOnSave_keepsPreviousStatesInHistory() {
        runBlocking {
            val noteDao = createFileDatabase().getBaseNoteDao()
            val backupsFolder = preferences.backupsFolder.value
            noteDao.insert(createSampleNote(title = "First", body = "Body"))
            application.autoBackupOnSave(backupsFolder, "", null)
            assertThat(autoBackupEntries()).contains(NotallyDatabase.DATABASE_NAME)
            assertThat(autoBackupEntries()).noneMatch {
                it.startsWith(BackupHistory.HISTORY_FOLDER)
            }

            noteDao.insert(createSampleNote(title = "Second", body = "Body"))
            application.autoBackupOnSave(backupsFolder, "", null)
            val snapshot =
                "${BackupHistory.HISTORY_FOLDER}/${BackupHistory.snapshotName(autoBackupZip().lastModified())}"
            assertThat(autoBackupEntries()).contains(NotallyDatabase.DATABASE_NAME, snapshot)

            // Emptying the database must not replace the last state with notes
            noteDao.deleteFrom(Folder.NOTES)
            application.autoBackupOnSave(backupsFolder, "", null)
            application.autoBackupOnSave(backupsFolder, "", null)

            val extracted = File(application.cacheDir, "history_test").apply { mkdirs() }
            ZipFile(autoBackupZip()).extractFile(snapshot, extracted.path)
            assertThat(SqliteSnapshotInspector.inspect(File(extracted, snapshot)).noteCount)
                .isEqualTo(2)
            assertThat(
                    File(
                        application.getExternalBackupsDirectory(),
                        "${ON_SAVE_BACKUP_FILE}_tmp.zip",
                    )
                )
                .doesNotExist()
            extracted.deleteRecursively()
        }
    }
}
