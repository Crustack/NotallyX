package com.philkes.notallyx.presentation.activity.main.fragments

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.activity.note.refreshStatusBarPin
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.assertLogAppeared
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
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
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSettingsValue
import com.philkes.notallyx.test.waitUntilSucceeds
import com.philkes.notallyx.utils.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.utils.backup.PERIODIC_BACKUP_FILE_PREFIX
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import com.philkes.notallyx.utils.getExternalMediaDirectory
import com.philkes.notallyx.utils.getUriForFile
import com.philkes.notallyx.utils.listZipFiles
import java.io.File
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class SettingsFragmentTest : UiTestBase() {

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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity -> activity.refreshStatusBarPin(pinnedToStatusNote) }

        navigateTo(R.id.Settings)

        onView(withId(R.id.View)).perform(scrollTo(), click())
        val viewSettingOption =
            onData(anything())
                .inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
                .atPosition(1)
        viewSettingOption.perform(click())

        onView(withId(R.id.ShowSearchInTopBar)).perform(scrollTo(), click())
        val showSearchEnabledOption =
            onDisplayView(allOf(withId(R.id.EnabledButton), withText(R.string.enabled)))
        showSearchEnabledOption.perform(click())

        onView(withId(R.id.NotesSortOrder)).perform(scrollTo(), click())
        onDisplayView(withText(R.string.ascending)).perform(click())
        val saveSortOrderButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.save)))
        saveSortOrderButton.perform(scrollTo(), click())

        onView(withId(R.id.BackupPassword)).perform(scrollTo(), click())
        val backupPasswordInput =
            onDisplayView(allOf(withId(R.id.InputText), withContentDescription("Input")))
        backupPasswordInput.perform(replaceText("1234"), closeSoftKeyboard())
        val saveBackupPasswordButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.save)))
        saveBackupPasswordButton.perform(scrollTo(), click())

        onView(withId(R.id.SecureFlag)).perform(scrollTo(), click())
        R.id.EnabledButton.byId().perform(click())

        onView(withId(R.id.SecureFlag)).perform(scrollTo(), click())
        R.id.DisabledButton.byId().perform(click())

        enableDataInPublic()

        enableBiometricLock()

        navigateTo(R.id.Notes)
        checkAndUpdateNoteTitle(0, "Test", " Foo")

        navigateTo(R.id.Settings)
        onView(withId(R.id.ExportSettings)).perform(scrollTo(), click())
        R.string.export.byText(withId(android.R.id.button1)).perform(click())
        waitUntil(10_000) { settingsExportFile.exists() }

        onView(withId(R.id.ResetSettings)).perform(scrollTo(), click())
        R.string.reset_settings.byText(withId(android.R.id.button1)).perform(click())
        waitUntilSettingsValue(R.id.ShowSearchInTopBar, R.string.disabled)
        waitUntilSettingsValue(R.id.BiometricLock, R.string.disabled)
        waitUntilSettingsValue(R.id.DataInPublicFolder, R.string.disabled)

        onView(withId(R.id.ImportSettings)).perform(scrollTo(), click())
        R.string.import_action.byText(withId(android.R.id.button1)).perform(click())

        waitUntilSettingsValue(R.id.ShowSearchInTopBar, R.string.enabled)
        waitUntilSettingsValue(R.id.DataInPublicFolder, R.string.disabled)
        waitUntilSettingsValue(R.id.BiometricLock, R.string.disabled)

        navigateTo(R.id.Notes)
        checkAndUpdateNoteTitle(0, "Test Foo", " Bar")

        Intents.release()
        scenario.close()
    }

    @Test
    fun periodicBackupCreatedAndImport() {
        Intents.init()
        val backupPath = context.getExternalBackupsDirectory().toUri()
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitUntil(10_000L) {
            DocumentFile.fromFile(File(backupPath.path!!))
                .listZipFiles(PERIODIC_BACKUP_FILE_PREFIX)
                .isNotEmpty()
        }

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        R.string.tap_for_more_options.byContentDescription().perform(click())
        R.string.delete_forever.byText().perform(click())
        R.string.delete.byText(withId(android.R.id.button1)).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Settings)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(scrollTo(), click())
        R.string.import_backup.byText(withId(android.R.id.button1)).perform(click())
        waitUntil(15_000L) {
            runBlocking {
                withContext(Dispatchers.IO) { database.getBaseNoteDao().getAll().isNotEmpty() }
            }
        }

        navigateTo(R.id.Notes)
        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test")))
        }

        Intents.release()
        scenario.close()
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        R.id.EnterTitle.byId().perform(typeText(" Foo"))
        R.id.EnterBody.byId().perform(typeText(" Foo"), closeSoftKeyboard())
        Espresso.pressBack()
        assertLogAppeared("ExportExtensions", "Finished full backup")

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
        R.string.delete.byContentDescription().perform(click())
        onView(withText("Test Foo")).check(doesNotExist())
        navigateTo(R.id.Deleted)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
        R.string.delete_forever.byContentDescription().perform(click())
        R.string.delete.byText(withId(android.R.id.button1)).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Settings)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(scrollTo())
        SystemClock.sleep(3000)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(click())
        R.string.import_backup.byText(withId(android.R.id.button1)).perform(click())
        waitUntil(15_000L) {
            runBlocking {
                withContext(Dispatchers.IO) { database.getBaseNoteDao().getAll().isNotEmpty() }
            }
        }

        navigateTo(R.id.Notes)
        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test Foo")))
        }

        Intents.release()
        scenario.close()
    }

    @Test
    fun biometricLock() {
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Settings)
        enableBiometricLock()

        navigateTo(R.id.Notes)

        checkAndUpdateNoteTitle(0, "Test", " Foo")

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

        checkAndUpdateNoteTitle(0, "Test Foo", " Bar")

        val expectedEncrypt = if (preferences.biometricLockEncryptsDb.value) 1 else 0
        val expectedDecrypt1 = if (preferences.biometricLockEncryptsDb.value) 1 else 0
        val expectedDecrypt2 = if (preferences.biometricLockEncryptsDb.value) 2 else 0

        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt1, fakeBiometricAuthenticator.getDecryptionCounter())

        navigateTo(R.id.Settings)
        disableBiometricLock()
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())

        navigateTo(R.id.Notes)

        checkAndUpdateNoteTitle(0, "Test Foo Bar", " 123")

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test Foo Bar 123")))
        }
        assertEquals(expectedEncrypt, fakeBiometricAuthenticator.getEncryptionCounter())
        assertEquals(expectedDecrypt2, fakeBiometricAuthenticator.getDecryptionCounter())

        scenario.close()
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Settings)

        enableDataInPublic()
        navigateTo(R.id.Notes)
        checkAndUpdateNoteTitle(0, "Test", " Foo")

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

        checkAndUpdateNoteTitle(0, "Test Foo", " Bar")

        navigateTo(R.id.Settings)
        disableDataInPublic()

        navigateTo(R.id.Notes)

        checkAndUpdateNoteTitle(0, "Test Foo Bar", " 123")

        device.sleep()
        SystemClock.sleep(2000)
        device.wakeUp()
        device.pressMenu()

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test Foo Bar 123")))
        }

        scenario.close()
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity -> activity.refreshStatusBarPin(pinnedToStatusNote) }

        checkAndUpdateNoteTitle(0, "Text", "")
        checkAndUpdateNoteTitle(1, "List", "")

        navigateTo(R.id.Settings)
        enableDataInPublic()

        navigateTo(R.id.Notes)
        checkAndUpdateNoteTitle(0, "Text", " Foo")
        checkAndUpdateNoteTitle(1, "List", " Foo")

        lockAndUnlockDevice()

        checkAndUpdateNoteTitle(0, "Text Foo", " Bar")
        checkAndUpdateNoteTitle(1, "List Foo", " Bar")

        navigateTo(R.id.Settings)
        enableBiometricLock()

        navigateTo(R.id.Notes)
        checkAndUpdateNoteTitle(0, "Text Foo Bar", " Bio")
        checkAndUpdateNoteTitle(1, "List Foo Bar", " Bio")

        lockAndUnlockDevice()

        checkAndUpdateNoteTitle(0, "Text Foo Bar Bio", " Lock")
        checkAndUpdateNoteTitle(1, "List Foo Bar Bio", " Lock")

        navigateTo(R.id.Settings)
        disableDataInPublic()

        navigateTo(R.id.Notes)

        checkAndUpdateNoteTitle(0, "Text Foo Bar Bio Lock", " Public")
        checkAndUpdateNoteTitle(1, "List Foo Bar Bio Lock", " Public")

        lockAndUnlockDevice()

        checkAndUpdateNoteTitle(0, "Text Foo Bar Bio Lock Public", " Lock")
        checkAndUpdateNoteTitle(1, "List Foo Bar Bio Lock Public", " Lock")

        navigateTo(R.id.Settings)
        disableBiometricLock()

        navigateTo(R.id.Notes)

        checkAndUpdateNoteTitle(0, "Text Foo Bar Bio Lock Public Lock", " BioDis")
        checkAndUpdateNoteTitle(1, "List Foo Bar Bio Lock Public Lock", " BioDis")

        lockAndUnlockDevice()

        checkAndUpdateNoteTitle(0, "Text Foo Bar Bio Lock Public Lock BioDis", " Lock")
        checkAndUpdateNoteTitle(1, "List Foo Bar Bio Lock Public Lock BioDis", " Lock")

        scenario.close()
    }
}
