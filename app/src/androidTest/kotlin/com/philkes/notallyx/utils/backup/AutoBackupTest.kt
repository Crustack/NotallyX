package com.philkes.notallyx.utils.backup

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.feature.migration.backup.copyDatabase
import com.philkes.notallyx.feature.migration.backup.exportAsZip
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.waitUntil
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class AutoBackupTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun autoBackupOnNoteSave() {
        val backupDir = context.getExternalBackupsDirectory()
        backupDir.mkdirs()
        val backupZipFile = File(backupDir, "$ON_SAVE_BACKUP_FILE.zip")
        if (backupZipFile.exists()) backupZipFile.delete()

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
            val takeNoteDesc = context.getString(R.string.take_note)
            val titleDesc = context.getString(R.string.title)
            val noteDesc = context.getString(R.string.note)
            val backDesc = context.getString(R.string.back)
            val addItemDesc = context.getString(R.string.add_item)

            composeTestRule.onNodeWithContentDescription(takeNoteDesc).performClick()
            composeTestRule
                .onNodeWithContentDescription(titleDesc)
                .performTextInput("Basic Test Note")
            composeTestRule
                .onNodeWithContentDescription(noteDesc)
                .performTextInput("This is a basic test note.")

            composeTestRule.onNodeWithContentDescription(backDesc).performClick()

            waitUntil(15_000L) { backupZipFile.exists() && backupZipFile.length() > 0 }
            val initialSize = backupZipFile.length()
            val initialZip = ZipFile(backupZipFile)
            assertTrue("Initial backup file should be a valid ZIP", initialZip.isValidZipFile)

            // 2. Go back into the note and add a huge attachment file
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Basic Test Note"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Basic Test Note").performClick()

            composeTestRule.onNodeWithContentDescription(addItemDesc).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.attach_file)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(attachmentFileName, substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(attachmentFileName, substring = true).assertIsDisplayed()

            val lastModifiedBeforeFirstSave = backupZipFile.lastModified()

            // 3. Exit the note and make sure auto backup was created (zip file should be larger)
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()

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
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Basic Test Note"))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Basic Test Note").performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(attachmentFileName, substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(attachmentFileName, substring = true).performTouchInput {
                longClick()
            }

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasText(context.getString(R.string.delete_file, attachmentFileName))
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(attachmentFileName, substring = true))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }

            val lastModifiedBeforeSecondSave = backupZipFile.lastModified()

            // 5. Exit the note and make sure auto backup was created (zip file becomes smaller)
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()

            waitUntil(15_000L) {
                backupZipFile.lastModified() > lastModifiedBeforeSecondSave &&
                    backupZipFile.length() < sizeWithAttachment
            }
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
            attachmentFile.delete()
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
            largeFile.delete()
            dbFile.delete()
        }
    }
}
