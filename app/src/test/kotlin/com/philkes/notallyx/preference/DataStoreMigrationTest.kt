package com.philkes.notallyx.preference

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
import androidx.datastore.tink.AeadSerializer
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.toPreservedString
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.preference.migration.LegacyEncryptedPrefsMigration
import com.philkes.notallyx.core.preference.migration.SharedPreferencesToUserPreferencesMigration
import com.philkes.notallyx.core.preference.model.SecurePreferences
import com.philkes.notallyx.core.preference.model.SecurePreferencesCorruptionHandler
import com.philkes.notallyx.core.preference.model.SecurePreferencesSerializer
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.core.preference.model.UserPreferencesCorruptionHandler
import com.philkes.notallyx.core.preference.model.UserPreferencesSerializer
import com.philkes.notallyx.core.security.TinkAeadProvider
import com.philkes.notallyx.data.model.Type
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class DataStoreMigrationTest {

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.filesDir, "test_datastore").apply { mkdirs() }
        context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE).edit(
            commit = true
        ) {
            clear()
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE).edit(
            commit = true
        ) {
            clear()
        }
    }

    @Test
    fun userPreferencesSerializer_roundTripSerialization() = runTest {
        val original =
            UserPreferences(
                theme = Theme.DARK,
                useDynamicColors = true,
                textSizeNoteEditor = 22f,
                textSizeOverview = 18f,
                dateFormatOverview = DateFormat.SHORT_ISO,
                timeFormatOverview = TimeFormat.AM_PM,
                notesView = NotesView.GRID,
                startView = "CustomStart",
                maxItems = 8,
                labelsHidden = setOf("labelA", "labelB"),
                iv = byteArrayOf(1, 2, 3),
                databaseEncryptionKey = byteArrayOf(9, 8, 7),
                widgets = mapOf(1 to 100L, 2 to 200L),
                widgetNoteTypes = mapOf(1 to Type.NOTE, 2 to Type.LIST),
            )

        val output = ByteArrayOutputStream()
        UserPreferencesSerializer.writeTo(original, output)

        val input = ByteArrayInputStream(output.toByteArray())
        val deserialized = UserPreferencesSerializer.readFrom(input)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun userPreferencesSerializer_corruptedDataThrowsCorruptionException() = runTest {
        val corruptedInput = ByteArrayInputStream("not a valid json string {]".encodeToByteArray())

        assertThatThrownBy {
                kotlinx.coroutines.runBlocking {
                    UserPreferencesSerializer.readFrom(corruptedInput)
                }
            }
            .isInstanceOf(CorruptionException::class.java)
    }

    @Test
    fun securePreferencesSerializer_roundTripAndEncryptionWithAead() = runTest {
        val aead = TinkAeadProvider.createInMemoryAead()
        val original =
            SecurePreferences(
                backupPassword = "SuperSecretPassword42!",
                fallbackDatabaseEncryptionKey = byteArrayOf(10, 20, 30, 40),
            )

        val fileName = "secure_prefs.bin"
        val aeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = SecurePreferencesSerializer,
                associatedData = fileName.encodeToByteArray(),
            )

        val output = ByteArrayOutputStream()
        aeadSerializer.writeTo(original, output)
        val encryptedBytes = output.toByteArray()

        // Verify the sensitive plaintext password is not in the encrypted bytes
        assertThat(encryptedBytes.decodeToString()).doesNotContain("SuperSecretPassword42!")

        // Verify successful decryption
        val decrypted = aeadSerializer.readFrom(ByteArrayInputStream(encryptedBytes))
        assertThat(decrypted).isEqualTo(original)

        // Verify decryption with different associated data fails
        val wrongAeadSerializer =
            AeadSerializer(
                aead = aead,
                wrappedSerializer = SecurePreferencesSerializer,
                associatedData = "different_file.bin".encodeToByteArray(),
            )
        assertThatThrownBy {
                kotlinx.coroutines.runBlocking {
                    wrongAeadSerializer.readFrom(ByteArrayInputStream(encryptedBytes))
                }
            }
            .isInstanceOf(CorruptionException::class.java)
    }

    @Test
    fun sharedPreferencesToUserPreferencesMigration_fullMigrationAndCleanup() = runTest {
        val prefs = context.getSharedPreferences("test_shared_prefs", Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString("theme", "dark")
            putBoolean("useDynamicColors", true)
            putFloat("textSizeNoteEditor", 24f)
            putFloat("textSizeOverview", 14f)
            putString("dateFormatOverview", "shortIso")
            putString("timeFormatOverview", "amPm")
            putString("dateFormatNoteView", "full")
            putString("timeFormatNoteView", "twentyFourH")
            putString("view", "grid")
            putString("notesSorting", "autoSortByTitle")
            putString("notesSortingDirection", "ASC")
            putString("startView", "Archived")
            putString("listItemSorting", "noAutoSort")
            putString("defaultListNoteViewMode", "readOnly")
            putInt("maxItemsToDisplayInList.v1", 6)
            putInt("maxLinesToDisplayInNote.v1", 10)
            putInt("maxLinesToDisplayInTitle", 3)
            putStringSet("labelsHiddenInNavigation", setOf("archivedTag", "personal"))
            putBoolean("labelsHiddenInOverview", true)
            putBoolean("imagesHiddenInOverview", true)
            putBoolean("alwaysShowSearchBar", true)
            putInt("maxLabelsInNavigation", 12)
            putString("autoBackup", "content://custom/backups")
            putBoolean("backupOnSave", true)
            putInt("autoBackupPeriodDays", 7)
            putInt("maxBackups", 4)
            putLong("periodicBackupLastExecution", 9999999L)
            putInt("autoRemoveDeletedNotesAfterDays", 14)
            putInt("autoSaveAfterIdleTime", 20)
            putString("biometricLock", "enabled")
            putBoolean("biometricLockEncryptsDb", true)
            putString("encryption_iv", byteArrayOf(1, 2, 3, 4).toPreservedString)
            putString("database_encryption_key", byteArrayOf(5, 6, 7, 8).toPreservedString)
            putBoolean("secureFlag", true)
            putBoolean("dataOnExternalStorage", true)
            putString("editNoteActivityTopActions", "SEARCH,PIN,SHARE")
            putString("editNoteActivityBottomAction", "CONVERT")
            putInt("dataSchemaId", 5)
            putString("defaultNoteColor", "#AABBCC")
            putLong("widget:42", 12345L)
            putString("widgetNoteType:42", "NOTE")
        }

        val migration = SharedPreferencesToUserPreferencesMigration(context) { prefs }

        assertThat(migration.shouldMigrate(UserPreferences())).isTrue()

        val migrated = migration.migrate(UserPreferences())

        assertThat(migrated.theme).isEqualTo(Theme.DARK)
        assertThat(migrated.useDynamicColors).isTrue()
        assertThat(migrated.textSizeNoteEditor).isEqualTo(24f)
        assertThat(migrated.textSizeOverview).isEqualTo(14f)
        assertThat(migrated.dateFormatOverview).isEqualTo(DateFormat.SHORT_ISO)
        assertThat(migrated.timeFormatOverview).isEqualTo(TimeFormat.AM_PM)
        assertThat(migrated.dateFormatNoteView).isEqualTo(DateFormat.FULL)
        assertThat(migrated.timeFormatNoteView).isEqualTo(TimeFormat.TWENTY_FOUR_H)
        assertThat(migrated.notesView).isEqualTo(NotesView.GRID)
        assertThat(migrated.notesSorting.sortedBy).isEqualTo(NotesSortBy.TITLE)
        assertThat(migrated.notesSorting.sortDirection).isEqualTo(SortDirection.ASC)
        assertThat(migrated.startView).isEqualTo("Archived")
        assertThat(migrated.listItemSorting).isEqualTo(ListItemSort.NO_AUTO_SORT)
        assertThat(migrated.defaultListNoteViewMode).isEqualTo(DefaultListNoteViewMode.READ_ONLY)
        assertThat(migrated.maxItems).isEqualTo(6)
        assertThat(migrated.maxLines).isEqualTo(10)
        assertThat(migrated.maxTitle).isEqualTo(3)
        assertThat(migrated.labelsHidden).containsExactlyInAnyOrder("archivedTag", "personal")
        assertThat(migrated.labelTagsHiddenInOverview).isTrue()
        assertThat(migrated.imagesHiddenInOverview).isTrue()
        assertThat(migrated.alwaysShowSearchBar).isTrue()
        assertThat(migrated.maxLabels).isEqualTo(12)
        assertThat(migrated.backupsFolder).isEqualTo("content://custom/backups")
        assertThat(migrated.backupOnSave).isTrue()
        assertThat(migrated.periodicBackups.periodInDays).isEqualTo(7)
        assertThat(migrated.periodicBackups.maxBackups).isEqualTo(4)
        assertThat(migrated.periodicBackupLastExecution).isEqualTo(9999999L)
        assertThat(migrated.autoRemoveDeletedNotesAfterDays).isEqualTo(14)
        assertThat(migrated.autoSaveAfterIdleTime).isEqualTo(20)
        assertThat(migrated.biometricLock).isEqualTo(BiometricLock.ENABLED)
        assertThat(migrated.biometricLockEncryptsDb).isTrue()
        assertThat(migrated.iv).isEqualTo(byteArrayOf(1, 2, 3, 4))
        assertThat(migrated.databaseEncryptionKey).isEqualTo(byteArrayOf(5, 6, 7, 8))
        assertThat(migrated.secureFlag).isTrue()
        assertThat(migrated.dataInPublicFolder).isTrue()
        assertThat(migrated.editNoteActivityTopActions)
            .containsExactly(EditAction.SEARCH, EditAction.PIN, EditAction.SHARE)
        assertThat(migrated.editNoteActivityBottomAction).isEqualTo(EditAction.CONVERT)
        assertThat(migrated.dataSchemaId).isEqualTo(5)
        assertThat(migrated.defaultNoteColor).isEqualTo("#AABBCC")
        assertThat(migrated.widgets[42]).isEqualTo(12345L)
        assertThat(migrated.widgetNoteTypes[42]).isEqualTo(Type.NOTE)

        migration.cleanUp()
        assertThat(prefs.all).isEmpty()
        assertThat(migration.shouldMigrate(migrated)).isFalse()
    }

    @Test
    fun sharedPreferencesToUserPreferencesMigration_e2eDataStoreIntegration() = runTest {
        val testScope = TestScope(testScheduler)
        val dataStoreFile = File(tempDir, "user_preferences.json")
        val prefs = context.getSharedPreferences("e2e_sp", Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString("theme", "light")
            putInt("maxItemsToDisplayInList.v1", 9)
        }

        val dataStore =
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                produceFile = { dataStoreFile },
                migrations = listOf(SharedPreferencesToUserPreferencesMigration(context) { prefs }),
                corruptionHandler = UserPreferencesCorruptionHandler,
                scope = testScope,
            )

        val result = dataStore.data.first()
        assertThat(result.theme).isEqualTo(Theme.LIGHT)
        assertThat(result.maxItems).isEqualTo(9)

        // SharedPreferences should be cleaned up after migration completes
        assertThat(prefs.all).isEmpty()
    }

    @Test
    fun legacyEncryptedPrefsMigration_fullMigrationAndCleanup() = runTest {
        val legacyPrefs = context.getSharedPreferences("test_legacy_secret", Context.MODE_PRIVATE)
        val testKey = byteArrayOf(11, 22, 33, 44)
        legacyPrefs.edit(commit = true) {
            putString("backupPassword", "myBackupSecret123")
            putString("fallback_database_encryption_key", testKey.toPreservedString)
        }

        val migration = LegacyEncryptedPrefsMigration(context) { legacyPrefs }

        assertThat(migration.shouldMigrate(SecurePreferences())).isTrue()

        val migrated = migration.migrate(SecurePreferences())
        assertThat(migrated.backupPassword).isEqualTo("myBackupSecret123")
        assertThat(migrated.fallbackDatabaseEncryptionKey).isEqualTo(testKey)

        migration.cleanUp()
        assertThat(legacyPrefs.all).isEmpty()
        assertThat(migration.shouldMigrate(migrated)).isFalse()
    }

    @Test
    fun legacyEncryptedPrefsMigration_e2eEncryptedDataStoreIntegration() = runTest {
        val testScope = TestScope(testScheduler)
        val dataStoreFile = File(tempDir, "secure_preferences.bin")
        val aead = TinkAeadProvider.createInMemoryAead()

        val legacyPrefs = context.getSharedPreferences("e2e_legacy_secret", Context.MODE_PRIVATE)
        val testKey = byteArrayOf(99, 88, 77)
        legacyPrefs.edit(commit = true) {
            putString("backupPassword", "E2eSecretPassword")
            putString("fallback_database_encryption_key", testKey.toPreservedString)
        }

        val secureDataStore =
            DataStoreFactory.create(
                storage =
                    FileStorage(
                        serializer =
                            AeadSerializer(
                                aead = aead,
                                wrappedSerializer = SecurePreferencesSerializer,
                                associatedData = dataStoreFile.name.encodeToByteArray(),
                            ),
                        produceFile = { dataStoreFile },
                    ),
                migrations = listOf(LegacyEncryptedPrefsMigration(context) { legacyPrefs }),
                corruptionHandler = SecurePreferencesCorruptionHandler,
                scope = testScope,
            )

        val result = secureDataStore.data.first()
        assertThat(result.backupPassword).isEqualTo("E2eSecretPassword")
        assertThat(result.fallbackDatabaseEncryptionKey).isEqualTo(testKey)

        // Verify the file on disk is encrypted
        val fileBytes = dataStoreFile.readBytes()
        assertThat(fileBytes.decodeToString()).doesNotContain("E2eSecretPassword")

        // Verify legacy prefs were cleared
        assertThat(legacyPrefs.all).isEmpty()
    }
}
