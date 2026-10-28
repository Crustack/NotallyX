package com.philkes.notallyx.utils.backup

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSucceeds
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class AutoBackupTest : UiTestBase() {

    @Test
    fun autoBackupOnNoteSave() {
        val backupDir = context.getExternalBackupsDirectory()
        backupDir.mkdirs()
        preferences.backupsFolder.save(backupDir.toUri().toString())
        preferences.periodicBackups.save(PeriodicBackup(periodInDays = -1, maxBackups = 0))
        preferences.backupOnSave.save(true)

        // Prepare large attachment file to attach via UI
        val attachmentFileName = "large_attachment.bin"
        val attachmentFile = File(context.cacheDir, attachmentFileName)
        val attachmentBytes = ByteArray(300 * 1024) { Random.nextInt().toByte() }
        attachmentFile.writeBytes(attachmentBytes)
        val attachmentUri = Uri.fromFile(attachmentFile)

        Intents.init()
        try {
            val attachFileResultIntent = Intent().apply { data = attachmentUri }
            val attachFileResult =
                Instrumentation.ActivityResult(Activity.RESULT_OK, attachFileResultIntent)

            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("*/*")),
                        ),
                    )
                )
                .respondWith(attachFileResult)

            // Launch the app as a user
            val scenario = ActivityScenario.launch(MainActivity::class.java)

            // 1. Create basic test note -> exit it to trigger and make sure auto backup was done
            R.id.TakeNote.byId().perform(click())
            R.id.EnterTitle.byId().perform(typeText("Basic Test Note"), closeSoftKeyboard())
            R.id.EnterBody.byId()
                .perform(typeText("This is a basic test note."), closeSoftKeyboard())

            val backupZipFile = File(backupDir, "$ON_SAVE_BACKUP_FILE.zip")

            toolbarBackButton.perform(click())

            waitUntil(15_000L) { backupZipFile.exists() && backupZipFile.length() > 0 }
            val initialSize = backupZipFile.length()
            val initialZip = ZipFile(backupZipFile)
            assertTrue("Initial backup file should be a valid ZIP", initialZip.isValidZipFile)

            // 2. Go back into the note and add a huge attachment file
            waitUntilSucceeds {
                R.id.MainListView.byId()
                    .onPositionView(0, withId(R.id.Title))
                    .check(matches(withText("Basic Test Note")))
            }
            R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))

            R.string.add_item.byContentDescription().perform(click())
            R.string.attach_file.byText().perform(click())

            waitUntilSucceeds {
                R.id.FilesPreview.byId().check(matches(isDisplayed()))
                attachmentFileName.byText().check(matches(isDisplayed()))
            }

            val lastModifiedBeforeFirstSave = backupZipFile.lastModified()

            // 3. Exit the note and make sure auto backup was created (zip file should be larger)
            toolbarBackButton.perform(click())

            waitUntil(15_000L) {
                backupZipFile.exists() &&
                    backupZipFile.lastModified() > lastModifiedBeforeFirstSave &&
                    backupZipFile.length() > initialSize + 250 * 1024
            }
            val sizeWithAttachment = backupZipFile.length()
            assertTrue(
                "Auto backup zip should become larger after adding attachment (was $initialSize, now $sizeWithAttachment)",
                sizeWithAttachment > initialSize,
            )
            val zipWithAttachment = ZipFile(backupZipFile)
            assertTrue(
                "Backup file with attachment should be a valid ZIP",
                zipWithAttachment.isValidZipFile,
            )

            // 4. Go back into the note and delete the attachment
            waitUntilSucceeds {
                R.id.MainListView.byId()
                    .onPositionView(0, withId(R.id.Title))
                    .check(matches(withText("Basic Test Note")))
            }
            R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))

            waitUntilSucceeds { R.id.FilesPreview.byId().check(matches(isDisplayed())) }
            R.id.FilesPreview.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
            R.string.delete.byText(withId(android.R.id.button1)).perform(click())

            val lastModifiedBeforeSecondSave = backupZipFile.lastModified()

            // 5. Exit the note and make sure auto backup was created (zip file becomes smaller)
            toolbarBackButton.perform(click())

            waitUntil(15_000L) { backupZipFile.lastModified() > lastModifiedBeforeSecondSave }
            val newSize = backupZipFile.length()
            assertTrue(
                "Auto backup zip should become smaller after removing attachment (was $sizeWithAttachment, now $newSize)",
                newSize < sizeWithAttachment,
            )
            val updatedZip = ZipFile(backupZipFile)
            assertTrue("Updated backup file should be a valid ZIP", updatedZip.isValidZipFile)

            scenario.close()
        } finally {
            Intents.release()
        }
    }

    @Test
    fun exportAsZipWhenBackupBecomesSmaller() {
        val backupDir = context.getExternalBackupsDirectory()
        backupDir.mkdirs()
        val testZip = File(backupDir, "export_as_zip_shrink_test.zip")
        if (testZip.exists()) testZip.delete()

        // 1. Create an initial backup ZIP with a database and a large 300 KB file
        val (_, dbFile) = runBlocking { context.copyDatabase() }
        val largeFile = File(context.cacheDir, "large_file.bin")
        val largeBytes = ByteArray(300 * 1024) { Random.nextInt().toByte() }
        largeFile.writeBytes(largeBytes)

        val zipFile = ZipFile(testZip)
        val params = ZipParameters().apply { compressionLevel = CompressionLevel.NO_COMPRESSION }
        params.fileNameInZip = DATABASE_NAME
        zipFile.addFile(dbFile, params)

        params.fileNameInZip = "$SUBFOLDER_FILES/data.bin"
        zipFile.addFile(largeFile, params)

        val initialSize = testZip.length()
        assertTrue("Initial ZIP should be > 300 KB (was $initialSize)", initialSize > 300 * 1024)
        assertTrue("Initial ZIP must be valid", zipFile.isValidZipFile)

        // 2. Call exportAsZip to overwrite the backup with the current database
        runBlocking { context.exportAsZip(testZip.toUri()) }

        // 3. Assert the ZIP on disk actually shrank and did not retain the old larger size
        val newSize = testZip.length()
        val updatedZip = ZipFile(testZip)
        assertTrue(
            "Exported zip should become smaller after export (was $initialSize, now $newSize)",
            newSize < initialSize / 2,
        )
        assertTrue("Updated backup file should be a valid ZIP", updatedZip.isValidZipFile)

        // 4. Verify the database exists in the updated ZIP
        val extractDir = File(context.cacheDir, "extracted_shrink_${System.currentTimeMillis()}")
        extractDir.mkdirs()
        try {
            updatedZip.extractAll(extractDir.absolutePath)
            val extractedDb = File(extractDir, DATABASE_NAME)
            assertTrue("Database should exist in ZIP", extractedDb.exists())
            val extractedData = File(extractDir, "$SUBFOLDER_FILES/data.bin")
            assertTrue("Old data.bin should no longer exist in ZIP", !extractedData.exists())
        } finally {
            extractDir.deleteRecursively()
            testZip.delete()
        }
    }
}
