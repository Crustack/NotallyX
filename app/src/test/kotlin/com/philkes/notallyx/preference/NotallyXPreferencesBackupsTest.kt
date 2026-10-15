package com.philkes.notallyx.preference

import android.app.Application
import android.os.Environment
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_BACKUPS
import com.philkes.notallyx.core.common.extensions.getDocumentFolder
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.migration.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.feature.migration.backup.PERIODIC_BACKUP_FILE_PREFIX
import com.philkes.notallyx.feature.migration.backup.autoBackupOnSave
import com.philkes.notallyx.feature.migration.backup.autoBackupOnSaveFileExists
import com.philkes.notallyx.feature.migration.backup.createBackup
import com.philkes.notallyx.feature.migration.backup.deleteModifiedNoteBackup
import com.philkes.notallyx.feature.migration.backup.modifiedNoteBackupExists
import com.philkes.notallyx.test.FakeAndroidKeyStore
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        FakeAndroidKeyStore.setup
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
            folder = Folder.NOTES,
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
    fun createBackup_withoutNotes_doesNotRotateOutOlderBackups() {
        runBlocking {
            val noteDao = createFileDatabase().getBaseNoteDao()
            preferences.periodicBackups.save(PeriodicBackup(periodInDays = 1, maxBackups = 2))
            noteDao.insert(createSampleNote(title = "Note", body = "Body"))
            application.createBackup()
            val firstBackup = periodicBackupZips().single()

            // When database is emptied: backups without notes are skipped when previous backups
            // exist
            noteDao.deleteFrom(Folder.NOTES)
            repeat(2) {
                delay(20)
                application.createBackup()
            }
            assertThat(periodicBackupZips()).hasSize(1).contains(firstBackup)

            // With notes again, new backups are created normally
            noteDao.insert(createSampleNote(title = "New Note", body = "Body"))
            delay(20)
            application.createBackup()
            assertThat(periodicBackupZips()).hasSize(2).contains(firstBackup)
        }
    }

    @Test
    fun autoBackupOnSave_withoutNotes_doesNotReplaceExistingBackup() {
        runBlocking {
            val noteDao = createFileDatabase().getBaseNoteDao()
            val backupsFolder = preferences.backupsFolder.value
            noteDao.insert(createSampleNote(title = "First", body = "Body"))
            application.autoBackupOnSave(backupsFolder, "", null)
            assertThat(autoBackupZip().exists()).isTrue()

            // Empty the database
            noteDao.deleteFrom(Folder.NOTES)
            delay(50)
            application.autoBackupOnSave(backupsFolder, "", null)

            // Existing auto backup is preserved when database has 0 notes
            assertThat(autoBackupZip().exists()).isTrue()
            assertThat(
                    File(
                        application.getExternalBackupsDirectory(),
                        "${ON_SAVE_BACKUP_FILE}_tmp.zip",
                    )
                )
                .doesNotExist()
        }
    }

    @Test
    fun autoBackupOnSave_recoversInterruptedReplace() {
        runBlocking {
            createFileDatabase()
                .getBaseNoteDao()
                .insert(createSampleNote(title = "Recover Note", body = "Body"))
            val backupsFolder = preferences.backupsFolder.value

            // Create valid initial backup
            application.autoBackupOnSave(backupsFolder, "", null)
            assertThat(autoBackupZip().exists()).isTrue()

            // Simulate interrupted replace: move auto backup to _previous.zip
            val prevFile =
                File(
                    application.getExternalBackupsDirectory(),
                    "${ON_SAVE_BACKUP_FILE}_previous.zip",
                )
            autoBackupZip().renameTo(prevFile)
            assertThat(autoBackupZip().exists()).isFalse()
            assertThat(prevFile.exists()).isTrue()

            // Next auto backup run should recover the interrupted backup
            application.autoBackupOnSave(backupsFolder, "", null)
            assertThat(autoBackupZip().exists()).isTrue()
            assertThat(prevFile.exists()).isFalse()
        }
    }

    @Test
    fun autoBackupOnSave_withChangedPassword_preservesExistingBackupWithoutRenamingToUnreadable() {
        runBlocking {
            createFileDatabase()
                .getBaseNoteDao()
                .insert(createSampleNote(title = "Pass Note", body = "Body"))
            val backupsFolder = preferences.backupsFolder.value

            // Create initial backup with password "oldPass"
            application.autoBackupOnSave(backupsFolder, "oldPass", null)
            assertThat(autoBackupZip().exists()).isTrue()

            // Update backup with "newPass" and previousPassword "oldPass"
            application.autoBackupOnSave(
                backupsFolder,
                password = "newPass",
                savedNote = null,
                previousPassword = "oldPass",
            )
            assertThat(autoBackupZip().exists()).isTrue()

            // Verify no unreadable backup was created
            val unreadableFiles =
                application.getExternalBackupsDirectory().listFiles()?.filter {
                    it.name.contains("_unreadable_")
                } ?: emptyList()
            assertThat(unreadableFiles).isEmpty()
        }
    }
}
