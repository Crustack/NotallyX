package com.philkes.notallyx.preference

import android.app.Application
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.DatabaseManager
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.utils.SUBFOLDER_BACKUPS
import com.philkes.notallyx.utils.backup.autoBackupOnSave
import com.philkes.notallyx.utils.backup.autoBackupOnSaveFileExists
import com.philkes.notallyx.utils.backup.createBackup
import com.philkes.notallyx.utils.backup.deleteModifiedNoteBackup
import com.philkes.notallyx.utils.backup.modifiedNoteBackupExists
import com.philkes.notallyx.utils.getDocumentFolder
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
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
        DatabaseManager.clearInstance(application)
    }

    @After
    fun tearDown() {
        DatabaseManager.clearInstance(application)
        NotallyXPreferences.clearInstance()
    }

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
            val dbFile =
                com.philkes.notallyx.data.NotallyDatabase.getInternalDatabaseFile(application)
            dbFile.parentFile?.mkdirs()
            dbFile.writeText("dummy database content")

            val database = DatabaseManager.getDatabase(application).value
            val noteDao = database.getBaseNoteDao()
            noteDao.insert(createSampleNote(title = "Test Note", body = "Test Body"))

            val backupResult = application.createBackup()
            assertThat(backupResult).isNotNull

            val backupsFolderFile = application.getExternalBackupsDirectory()
            val createdFiles =
                backupsFolderFile.listFiles()?.filter { it.name.endsWith(".zip") } ?: emptyList()
            assertThat(createdFiles).isNotEmpty

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
}
