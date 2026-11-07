package com.philkes.notallyx.activity.main.fragments

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.common.extensions.getExternalMediaDirectory
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.listZipFiles
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.main.MainActivity
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
class SettingsFragmentTest : UiTestBase() {

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
        composeTestRule.onNode(hasSetTextAction()).performTextInput("1234")
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
        composeTestRule.onNodeWithText(saveLabel).performClick()
        waitUntil(10_000) { settingsExportFile.exists() }

        composeTestRule
            .onNodeWithText(context.getString(R.string.reset_settings))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule.waitUntilSettingsValue(R.string.always_show_search_bar, R.string.disabled)
        composeTestRule.waitUntilSettingsValue(R.string.biometric_lock, R.string.disabled)
        composeTestRule.waitUntilSettingsValue(R.string.data_in_public, R.string.disabled)

        composeTestRule
            .onNodeWithText(context.getString(R.string.import_settings))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

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
            preferences.periodicBackups.save(PeriodicBackup(1, 1))
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
        composeTestRule.onNodeWithText(context.getString(R.string.import_backup)).performClick()
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
        composeTestRule.onNodeWithText(context.getString(R.string.import_backup)).performClick()
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

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

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

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

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

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

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

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

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

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

        composeTestRule.checkAndUpdateNoteTitle("Test Foo", " Bar")

        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.disableDataInPublic(context)

        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.checkAndUpdateNoteTitle("Test Foo Bar", " 123")

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

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
}
