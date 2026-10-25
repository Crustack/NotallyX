package com.philkes.notallyx.activity.main.routes

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.common.extensions.getExternalMediaDirectory
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.listZipFiles
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.main.routes.settings.removeOldDeletedNotes
import com.philkes.notallyx.feature.migration.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.feature.migration.backup.PERIODIC_BACKUP_FILE_PREFIX
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.assertLogAppeared
import com.philkes.notallyx.test.checkAndUpdateNoteTitle
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.disableBiometricLock
import com.philkes.notallyx.test.disableDataInPublic
import com.philkes.notallyx.test.enableBiometricLock
import com.philkes.notallyx.test.enableDataInPublic
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.lockAndUnlockDevice
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSettingsValue
import java.io.File
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class SettingsRouteTest : UiTestBase() {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settings() {
        initFakeBiometric()
        Intents.init()
        val settingsExportFile =
            File(context.getExternalMediaDirectory(), "NotallyX_Settings.json").apply { delete() }
        var pinnedToStatusNote =
            createBaseNote(
                title = "Test",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        runBlocking {
            pinnedToStatusNote =
                pinnedToStatusNote.copy(id = database.getBaseNoteDao().insert(pinnedToStatusNote))
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            `is`(Intent.EXTRA_INTENT),
                            hasAction(Intent.ACTION_CREATE_DOCUMENT),
                        ),
                    )
                )
                .respondWithFunction { intent ->
                    Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            data = context.getUriForFile(settingsExportFile)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                    )
                }
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_OPEN_DOCUMENT)),
                    )
                )
                .respondWithFunction { intent ->
                    Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            data = context.getUriForFile(settingsExportFile)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                    )
                }
        }

        composeTestRule.activity.refreshStatusBarPin(pinnedToStatusNote)

        composeTestRule.navigateTo(NotallyNavKey.Settings)

        val saveLabel = context.getString(R.string.save)

        composeTestRule
            .onNodeWithText(context.getString(R.string.view))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(hasText(context.getString(R.string.grid)) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.always_show_search_bar))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.enabled)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.notes_sorted_by))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.ascending)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.backup_password))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(hasSetTextAction() and hasAnyAncestor(isComposeDialog()))
            .performTextInput("1234")
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.disallow_screenshots))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.enabled)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.disallow_screenshots))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.disabled)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.enableDataInPublic(context)

        composeTestRule.enableBiometricLock()

        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.checkAndUpdateNoteTitle("Test", " Foo")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule
            .onNodeWithText(context.getString(R.string.export_settings))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.export)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        waitUntil(10_000) { settingsExportFile.exists() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.reset_settings))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.reset_settings)) and
                    hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        composeTestRule.waitUntilSettingsValue(R.string.always_show_search_bar, R.string.disabled)
        composeTestRule.waitUntilSettingsValue(R.string.biometric_lock, R.string.disabled)
        composeTestRule.waitUntilSettingsValue(R.string.data_in_public, R.string.disabled)

        composeTestRule
            .onNodeWithText(context.getString(R.string.import_settings))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.import_action)) and
                    hasAnyAncestor(isComposeDialog())
            )
            .performClick()

        composeTestRule.waitUntilSettingsValue(R.string.always_show_search_bar, R.string.enabled)
        composeTestRule.waitUntilSettingsValue(R.string.data_in_public, R.string.disabled)
        composeTestRule.waitUntilSettingsValue(R.string.biometric_lock, R.string.disabled)

        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.checkAndUpdateNoteTitle("Test Foo", " Bar")

        Intents.release()
    }

    @Test
    fun periodicBackupCreatedAndImport() {
        Intents.init()
        val backupPath = context.getExternalBackupsDirectory().toUri()
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
            database.checkpoint()
            preferences.backupsFolder.save(backupPath.toString())
            preferences.periodicBackups.save(PeriodicBackup(maxBackups = 1))
            preferences.backupOnSave.save(false)
        }
        intending(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_OPEN_DOCUMENT)),
                )
            )
            .respondWithFunction { intent ->
                val periodicBackup =
                    DocumentFile.fromFile(File(backupPath.path!!))
                        .listZipFiles(PERIODIC_BACKUP_FILE_PREFIX)
                        .sortedByDescending { it.lastModified() }
                        .firstOrNull()
                        ?: throw IllegalStateException(
                            "No periodic backup zip found in $backupPath"
                        )
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        data = context.getUriForFile(File(periodicBackup.uri.path!!))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            }

        composeTestRule.waitUntil(15_000) {
            val zipFile =
                DocumentFile.fromFile(File(backupPath.path!!))
                    .listZipFiles(PERIODIC_BACKUP_FILE_PREFIX)
                    .firstOrNull()
            zipFile != null && zipFile.length() > 100
        }

        // Delete note "Test" from Notes
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete))
            .performClick()
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        // Delete note "Test" forever from Deleted
        composeTestRule.navigateTo(NotallyNavKey.Deleted)
        composeTestRule.onNodeWithText("Test").performTouchInput { longClick() }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete_forever))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()

        // Go to Settings and import backup
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule
            .onNodeWithText(context.getString(R.string.import_backup))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.import_backup)) and
                    hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        waitUntil(15_000L) {
            runBlocking {
                withContext(Dispatchers.IO) { database.getBaseNoteDao().getAll().isNotEmpty() }
            }
        }

        // Navigate back to Notes and verify restored note
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Test")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()

        Intents.release()
    }

    @Test
    fun autoSaveBackupCreatedAndImport() {
        Intents.init()
        val backupPath = context.getExternalBackupsDirectory().toUri()
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
            preferences.backupsFolder.save(backupPath.toString())
            preferences.periodicBackups.save(PeriodicBackup(0, 0))
            preferences.backupOnSave.save(true)
        }
        intending(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_OPEN_DOCUMENT)),
                )
            )
            .respondWithFunction { intent ->
                val periodicBackup =
                    DocumentFile.fromFile(File(backupPath.path!!))
                        .listZipFiles(ON_SAVE_BACKUP_FILE)
                        .firstOrNull()
                        ?: throw IllegalStateException(
                            "No auto save backup zip found in $backupPath"
                        )
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        data = context.getUriForFile(File(periodicBackup.uri.path!!))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            }

        composeTestRule.onNodeWithText("Test").performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.title))
            .appendText(" Foo")
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.note))
            .appendText(" Foo")
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
        assertLogAppeared("ExportExtensions", "Finished backup ")

        composeTestRule.onNodeWithText("Test Foo").performTouchInput { longClick() }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete))
            .performClick()
        composeTestRule.onNodeWithText("Test Foo").assertDoesNotExist()

        composeTestRule.navigateTo(NotallyNavKey.Deleted)
        composeTestRule.onNodeWithText("Test Foo").performTouchInput { longClick() }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.delete_forever))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText("Test Foo").assertDoesNotExist()

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule
            .onNodeWithText(context.getString(R.string.import_backup))
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.import_backup)) and
                    hasAnyAncestor(isComposeDialog())
            )
            .performClick()
        waitUntil(15_000L) {
            runBlocking {
                withContext(Dispatchers.IO) { database.getBaseNoteDao().getAll().isNotEmpty() }
            }
        }

        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Test Foo")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test Foo").assertIsDisplayed()

        Intents.release()
    }

    @Test
    fun biometricLock_no_encryption() {
        val fakeBiometricAuthenticator = initFakeBiometric()
        val pinnedToStatusNote = createBaseNote(title = "Pinned", isPinnedToStatus = true)
        runBlocking {
            withContext(Dispatchers.Main) {
                database.getBaseNoteDao().apply {
                    insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
                    insert(pinnedToStatusNote)
                }
            }
        }

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.enableBiometricLock()

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test", " Foo")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Test Foo", " Bar")

        assertFalse(preferences.biometricLockEncryptsDb.value)
        val expectedEncrypt = 0
        val expectedDecrypt1 = 0
        val expectedDecrypt2 = 0

        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt1, fakeBiometricAuthenticator.getDecryptionCounter())

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableBiometricLock()
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test Foo Bar", " 123")

        lockAndUnlockDevice()

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Test Foo Bar 123"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test Foo Bar 123").assertIsDisplayed()
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())
    }

    @Test
    fun biometricLock_with_encryption() {
        val fakeBiometricAuthenticator = initFakeBiometric()
        val pinnedToStatusNote = createBaseNote(title = "Pinned", isPinnedToStatus = true)
        runBlocking {
            withContext(Dispatchers.Main) {
                database.getBaseNoteDao().apply {
                    insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
                    insert(pinnedToStatusNote)
                }
            }
        }

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.enableBiometricLock(encryptDb = true)

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test", " Foo")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Test Foo", " Bar")

        assertTrue(preferences.biometricLockEncryptsDb.value)
        val expectedEncrypt = 1
        val expectedDecrypt1 = 1
        val expectedDecrypt2 = 2

        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt1, fakeBiometricAuthenticator.getDecryptionCounter())

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableBiometricLock()
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test Foo Bar", " 123")

        lockAndUnlockDevice()

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Test Foo Bar 123"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test Foo Bar 123").assertIsDisplayed()
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())
    }

    @Test
    fun dataInPublic() {
        initFakeBiometric()
        runBlocking {
            withContext(Dispatchers.Main) {
                database
                    .getBaseNoteDao()
                    .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
            }
        }

        composeTestRule.navigateTo(NotallyNavKey.Settings)

        composeTestRule.enableDataInPublic(context)
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.checkAndUpdateNoteTitle("Test", " Foo")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Test Foo", " Bar")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableDataInPublic(context)

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test Foo Bar", " 123")

        lockAndUnlockDevice()

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Test Foo Bar 123"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Test Foo Bar 123").assertIsDisplayed()
    }

    @Test
    fun dataInPublicAndBiometric() {
        val fakeBiometricAuthenticator = initFakeBiometric()
        var pinnedToStatusNote =
            createBaseNote(
                title = "Text",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        runBlocking {
            withContext(Dispatchers.Main) {
                database.getBaseNoteDao().apply {
                    insert(
                        createBaseNote(
                            title = "List",
                            timestamp = Date().time - 24 * 60 * 60 * 1000,
                            items = mutableListOf(createListItem(body = "Item1")),
                            labels = listOf("label"),
                        )
                    )
                    pinnedToStatusNote = pinnedToStatusNote.copy(id = insert(pinnedToStatusNote))
                }
            }
        }

        composeTestRule.activity.refreshStatusBarPin(pinnedToStatusNote)

        composeTestRule.checkAndUpdateNoteTitle("Text", "")
        composeTestRule.checkAndUpdateNoteTitle("List", "")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.enableDataInPublic(context)

        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.checkAndUpdateNoteTitle("Text", " Foo")
        composeTestRule.checkAndUpdateNoteTitle("List", " Foo")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Text Foo", " Bar")
        composeTestRule.checkAndUpdateNoteTitle("List Foo", " Bar")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.enableBiometricLock()

        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar", " Bio")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar", " Bio")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar Bio", " Lock")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar Bio", " Lock")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableDataInPublic(context)

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar Bio Lock", " Public")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar Bio Lock", " Public")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar Bio Lock Public", " Lock")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar Bio Lock Public", " Lock")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableBiometricLock()

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar Bio Lock Public Lock", " BioDis")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar Bio Lock Public Lock", " BioDis")

        lockAndUnlockDevice()

        composeTestRule.checkAndUpdateNoteTitle("Text Foo Bar Bio Lock Public Lock BioDis", " Lock")
        composeTestRule.checkAndUpdateNoteTitle("List Foo Bar Bio Lock Public Lock BioDis", " Lock")
    }

    @Test
    fun switchAppThemesAndOledMode() {
        val saveLabel = context.getString(R.string.save)
        val cancelLabel = context.getString(R.string.cancel)
        val themeLabel = context.getString(R.string.theme)
        val lightLabel = context.getString(R.string.light)
        val darkLabel = context.getString(R.string.dark)
        val superDarkLabel = context.getString(R.string.super_dark)
        val followSystemLabel = context.getString(R.string.follow_system)
        val dynamicColorsLabel = context.getString(R.string.theme_use_dynamic_colors)

        val initialTheme = preferences.theme.value
        val initialDynamicColors = preferences.useDynamicColors.value

        // Ensure dynamic colors start disabled for baseline theme checks
        if (initialDynamicColors) {
            composeTestRule.navigateTo(NotallyNavKey.Settings)
            composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
            composeTestRule
                .onNode(hasText(dynamicColorsLabel) and hasAnyAncestor(isComposeDialog()))
                .performClick()
            composeTestRule.onNodeWithText(saveLabel).performClick()
            waitUntil(10_000) { !preferences.useDynamicColors.value }
        }

        // 1. Verify dialog cancellation does not mutate theme
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(lightLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(cancelLabel).performClick()
        assertEquals(initialTheme, preferences.theme.value)

        // 2. Switch to Light theme and verify preference
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(lightLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule.waitUntilSettingsValue(R.string.theme, R.string.light)
        assertEquals(Theme.LIGHT, preferences.theme.value)

        // 3. Switch to Dark theme and verify preference
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(darkLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule.waitUntilSettingsValue(R.string.theme, R.string.dark)
        assertEquals(Theme.DARK, preferences.theme.value)

        // 4. Switch to Super Dark (OLED Black) theme and verify preference
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(superDarkLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule.waitUntilSettingsValue(R.string.theme, R.string.super_dark)
        assertEquals(Theme.SUPER_DARK, preferences.theme.value)

        // 5. Switch to Follow System theme and verify preference
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(followSystemLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule.waitUntilSettingsValue(R.string.theme, R.string.follow_system)
        assertEquals(Theme.FOLLOW_SYSTEM, preferences.theme.value)

        // 6. Test Dynamic Colors: toggle dynamic colors
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
        composeTestRule
            .onNode(hasText(dynamicColorsLabel) and hasAnyAncestor(isComposeDialog()))
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        waitUntil(10_000) { preferences.useDynamicColors.value }
        assertTrue(preferences.useDynamicColors.value)

        // Restore initial settings
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        if (preferences.useDynamicColors.value != initialDynamicColors) {
            composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
            composeTestRule
                .onNode(hasText(dynamicColorsLabel) and hasAnyAncestor(isComposeDialog()))
                .performClick()
            composeTestRule.onNodeWithText(saveLabel).performClick()
            waitUntil(10_000) { preferences.useDynamicColors.value == initialDynamicColors }
        }

        if (preferences.theme.value != initialTheme) {
            composeTestRule.onNodeWithText(themeLabel).performScrollTo().performClick()
            composeTestRule
                .onNode(
                    hasText(context.getString(initialTheme.textResId)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()
            composeTestRule.onNodeWithText(saveLabel).performClick()
            composeTestRule.waitUntilSettingsValue(R.string.theme, initialTheme.textResId)
        }
    }

    @Test
    fun autoRemoveDeletedNotesRetention() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        // Prepare test data with different deletion timestamps:
        // 1. Deleted 40 days ago (> 30 days retention threshold)
        val note40DaysTitle = "DeletedNote40Days"
        var note40Days =
            createBaseNote(
                title = note40DaysTitle,
                body = "Deleted 40 days ago",
                folder = Folder.DELETED,
                timestamp = now - (40 * oneDayMillis),
                modifiedTimestamp = now - (40 * oneDayMillis),
            )
        // 2. Deleted 15 days ago (between 7 and 30 days retention threshold)
        val note15DaysTitle = "DeletedNote15Days"
        var note15Days =
            createBaseNote(
                title = note15DaysTitle,
                body = "Deleted 15 days ago",
                folder = Folder.DELETED,
                timestamp = now - (15 * oneDayMillis),
                modifiedTimestamp = now - (15 * oneDayMillis),
            )
        // 3. Deleted 2 days ago (< 7 days retention threshold)
        val note2DaysTitle = "DeletedNote2Days"
        var note2Days =
            createBaseNote(
                title = note2DaysTitle,
                body = "Deleted 2 days ago",
                folder = Folder.DELETED,
                timestamp = now - (2 * oneDayMillis),
                modifiedTimestamp = now - (2 * oneDayMillis),
            )
        // 4. Active note in NOTES folder modified 50 days ago (must never be deleted by
        // auto-remove)
        val activeNoteTitle = "ActiveNote50Days"
        var activeNote =
            createBaseNote(
                title = activeNoteTitle,
                body = "Active note modified long ago",
                folder = Folder.NOTES,
                timestamp = now - (50 * oneDayMillis),
                modifiedTimestamp = now - (50 * oneDayMillis),
            )

        runBlocking {
            withContext(Dispatchers.IO) {
                note40Days = note40Days.copy(id = database.getBaseNoteDao().insert(note40Days))
                note15Days = note15Days.copy(id = database.getBaseNoteDao().insert(note15Days))
                note2Days = note2Days.copy(id = database.getBaseNoteDao().insert(note2Days))
                activeNote = activeNote.copy(id = database.getBaseNoteDao().insert(activeNote))
                database.checkpoint()
            }
        }

        val autoRemoveTitle = context.getString(R.string.auto_remove_deleted_notes)
        val offLabel = context.getString(R.string.off)
        val workManager = WorkManager.getInstance(context)

        // Ensure baseline preference starts at 0 (Off)
        preferences.autoRemoveDeletedNotesAfterDays.save(0)

        // 1. Navigate to Settings and scroll to auto remove preference
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performScrollTo()

        assertEquals(0, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .assertIsDisplayed()

        // 2. Change retention period to 30 days via text input
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performScrollTo()
            .performTextReplacement("30")
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performImeAction()

        composeTestRule.waitUntil(10_000) {
            preferences.autoRemoveDeletedNotesAfterDays.value == 30
        }
        assertEquals(30, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle") and hasText("30 Days"))
            .performScrollTo()
            .assertIsDisplayed()

        // Verify WorkManager periodic work is scheduled
        val workInfos30 =
            workManager
                .getWorkInfosForUniqueWork(NotallyXApplication.AUTO_REMOVE_DELETED_NOTES)
                .get()
        assertTrue("WorkManager should have scheduled auto-remove work", workInfos30.isNotEmpty())
        assertTrue(
            "Scheduled work should be ENQUEUED or RUNNING",
            workInfos30.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            },
        )

        // Trigger background cleanup worker behavior for 30 days retention
        val result30 = runBlocking {
            (context.applicationContext as ContextWrapper).removeOldDeletedNotes(preferences)
        }
        assertEquals(androidx.work.ListenableWorker.Result.success(), result30)

        // Verify database state after 30-day retention cleanup:
        // - Note deleted 40 days ago (> 30 days) should be permanently removed
        // - Note deleted 15 days ago (<= 30 days) should still exist
        // - Note deleted 2 days ago (<= 30 days) should still exist
        // - Active note should still exist
        runBlocking {
            withContext(Dispatchers.IO) {
                val remainingIds = database.getBaseNoteDao().getAll().map { it.id }.toSet()
                assertFalse(
                    "Note deleted 40 days ago should be removed",
                    remainingIds.contains(note40Days.id),
                )
                assertTrue(
                    "Note deleted 15 days ago should remain",
                    remainingIds.contains(note15Days.id),
                )
                assertTrue(
                    "Note deleted 2 days ago should remain",
                    remainingIds.contains(note2Days.id),
                )
                assertTrue("Active note should remain", remainingIds.contains(activeNote.id))
            }
        }

        // 3. Change retention period to 7 days via text input
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performScrollTo()
            .performTextReplacement("7")
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performImeAction()

        composeTestRule.waitUntil(10_000) { preferences.autoRemoveDeletedNotesAfterDays.value == 7 }
        assertEquals(7, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle") and hasText("7 Days"))
            .performScrollTo()
            .assertIsDisplayed()

        // Trigger background cleanup worker behavior for 7 days retention
        val result7 = runBlocking {
            (context.applicationContext as ContextWrapper).removeOldDeletedNotes(preferences)
        }
        assertEquals(androidx.work.ListenableWorker.Result.success(), result7)

        // Verify database state after 7-day retention cleanup:
        // - Note deleted 15 days ago (> 7 days) should now be permanently removed
        // - Note deleted 2 days ago (<= 7 days) should still exist
        // - Active note should still exist
        runBlocking {
            withContext(Dispatchers.IO) {
                val remainingIds = database.getBaseNoteDao().getAll().map { it.id }.toSet()
                assertFalse(
                    "Note deleted 15 days ago should be removed",
                    remainingIds.contains(note15Days.id),
                )
                assertTrue(
                    "Note deleted 2 days ago should remain",
                    remainingIds.contains(note2Days.id),
                )
                assertTrue("Active note should remain", remainingIds.contains(activeNote.id))
            }
        }

        // 4. Change retention period to Never (0 / Off) via text input
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performScrollTo()
            .performTextReplacement("0")
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle"))
            .performImeAction()

        composeTestRule.waitUntil(10_000) { preferences.autoRemoveDeletedNotesAfterDays.value == 0 }
        assertEquals(0, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle") and hasText(offLabel))
            .performScrollTo()
            .assertIsDisplayed()

        // Verify WorkManager periodic work is cancelled when disabled
        composeTestRule.waitUntil(10_000) {
            val infos =
                workManager
                    .getWorkInfosForUniqueWork(NotallyXApplication.AUTO_REMOVE_DELETED_NOTES)
                    .get()
            infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED }
        }

        // Running cleanup worker when days <= 0 should do nothing (keep note deleted 2 days ago)
        val result0 = runBlocking {
            (context.applicationContext as ContextWrapper).removeOldDeletedNotes(preferences)
        }
        assertEquals(androidx.work.ListenableWorker.Result.success(), result0)
        runBlocking {
            withContext(Dispatchers.IO) {
                val remainingIds = database.getBaseNoteDao().getAll().map { it.id }.toSet()
                assertTrue(
                    "Note deleted 2 days ago should remain when auto-remove is Off",
                    remainingIds.contains(note2Days.id),
                )
                assertTrue("Active note should remain", remainingIds.contains(activeNote.id))
            }
        }

        // 5. Test Stepper button increments and decrements (Increase & Decrease)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INCREASE}_$autoRemoveTitle"))
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntil(10_000) { preferences.autoRemoveDeletedNotesAfterDays.value == 1 }
        assertEquals(1, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle") and hasText("1 Day"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_DECREASE}_$autoRemoveTitle"))
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntil(10_000) { preferences.autoRemoveDeletedNotesAfterDays.value == 0 }
        assertEquals(0, preferences.autoRemoveDeletedNotesAfterDays.value)
        composeTestRule
            .onNode(hasTestTag("${TestTags.STEPPER_INPUT}_$autoRemoveTitle") and hasText(offLabel))
            .performScrollTo()
            .assertIsDisplayed()

        // 6. Verify UI reflected in Deleted screen: noteDeleted2Days should be visible, others not
        composeTestRule.navigateTo(NotallyNavKey.Deleted)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(note2DaysTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(note2DaysTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(note15DaysTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(note40DaysTitle).assertDoesNotExist()

        // Verify UI reflected in Notes screen: activeNoteOld should be visible
        composeTestRule.navigateTo(NotallyNavKey.Notes)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(activeNoteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(activeNoteTitle).assertIsDisplayed()
    }
}
