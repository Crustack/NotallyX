package com.philkes.notallyx.activity.edit

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.content.IntentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.waitUntil
import java.io.File
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditRouteTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun reminders() {
        fun confirmDateAndTime() {
            composeTestRule.onNodeWithText(context.getString(R.string.next)).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.next)).performClick()
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val titleLabel = context.getString(R.string.title)
        val saveLabel = context.getString(R.string.save)
        val backLabel = context.getString(R.string.back)
        val remindersLabel = context.getString(R.string.reminders)

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Reminder Note")

        // Click Reminders button on top toolbar (opens RemindersActivity)
        composeTestRule.onNodeWithContentDescription(remindersLabel).performClick()

        // 1. Reminder 1: None repetition
        confirmDateAndTime()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.reminder_no_repetition))
            .assertIsDisplayed()

        // 2. Reminder 2: Daily repetition
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .performClick()
        confirmDateAndTime()
        composeTestRule.onNodeWithText(context.getString(R.string.daily)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.daily)).assertIsDisplayed()

        // 3. Reminder 3: Weekly repetition
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .performClick()
        confirmDateAndTime()
        composeTestRule.onNodeWithText(context.getString(R.string.weekly)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.weekly)).assertIsDisplayed()

        // 4. Reminder 4: Monthly repetition
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .performClick()
        confirmDateAndTime()
        composeTestRule.onNodeWithText(context.getString(R.string.monthly)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        val ofTheMonthText =
            context.getString(R.string.of_the_month, "").replace("%1\$s", "").trim()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(ofTheMonthText, substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onAllNodes(hasText(ofTheMonthText, substring = true))[0].assertIsDisplayed()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        // 5. Reminder 5: Yearly repetition
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .performClick()
        confirmDateAndTime()
        composeTestRule.onNodeWithText(context.getString(R.string.yearly)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.yearly)).assertIsDisplayed()

        // 6. Reminder 6: Custom repetition (every 3rd week)
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .performClick()
        confirmDateAndTime()
        composeTestRule.onNodeWithText(context.getString(R.string.custom)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.repetition_value_hint))
            .performTextReplacement("3")
        composeTestRule.onNodeWithText(context.getString(R.string.weeks)).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        val customRepetitionText =
            "${context.getString(R.string.every)} 3 ${context.getString(R.string.weeks)}"
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(customRepetitionText))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Go back from RemindersActivity to EditActivity
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Verify reminder chip is visible in EditScreen
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(remindersLabel))
                .fetchSemanticsNodes()
                .size >= 2
        }
        val reminderChip = composeTestRule.onAllNodes(hasContentDescription(remindersLabel))[1]
        reminderChip.assertIsDisplayed()

        // Click reminder chip and verify it brings user to RemindersActivity (reminders list view)
        reminderChip.performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(context.getString(R.string.add_reminder)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_reminder))
            .assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun addAttachments() {
        val packageName = context.packageName
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .grantRuntimePermission(
                packageName,
                Manifest.permission.RECORD_AUDIO,
            )

        val testContext = InstrumentationRegistry.getInstrumentation().context
        val catFile = File(context.cacheDir, "cat.jpg")
        val dogFile = File(context.cacheDir, "dog.jpg")
        val textFile = File(context.cacheDir, "text.txt")

        testContext.classLoader.getResourceAsStream("attachments/cat.jpg")!!.use { input ->
            catFile.outputStream().use { output -> input.copyTo(output) }
        }
        testContext.classLoader.getResourceAsStream("attachments/dog.jpg")!!.use { input ->
            dogFile.outputStream().use { output -> input.copyTo(output) }
        }
        testContext.classLoader.getResourceAsStream("attachments/text.txt")!!.use { input ->
            textFile.outputStream().use { output -> input.copyTo(output) }
        }

        val tempAudioFile = context.getTempAudioFile()
        createSampleAudioFile(tempAudioFile)

        val catUri = context.getUriForFile(catFile)
        val dogUri = context.getUriForFile(dogFile)
        val textUri = context.getUriForFile(textFile)

        val savedFile = File(context.cacheDir, "saved_cat.jpg").apply { if (exists()) delete() }
        val savedUri = context.getUriForFile(savedFile)

        Intents.init()
        try {
            val attachFileResultIntent = Intent().apply { data = textUri }
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

            val addImagesClipData =
                ClipData.newUri(context.contentResolver, "image", catUri).apply {
                    addItem(ClipData.Item(dogUri))
                }
            val addImagesResultIntent = Intent().apply { this.clipData = addImagesClipData }
            val addImagesResult =
                Instrumentation.ActivityResult(Activity.RESULT_OK, addImagesResultIntent)

            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*")),
                        ),
                    )
                )
                .respondWith(addImagesResult)

            val saveImageResultIntent = Intent().apply { data = savedUri }
            val saveImageResult =
                Instrumentation.ActivityResult(Activity.RESULT_OK, saveImageResultIntent)

            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_CREATE_DOCUMENT)),
                    )
                )
                .respondWith(saveImageResult)

            val openFileResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)

            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_VIEW)),
                    )
                )
                .respondWith(openFileResult)

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.take_note))
                .performClick()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.add_images)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1/2")).fetchSemanticsNodes().isNotEmpty()
            }

            // Swipe left on image pager in EditScreen to select 2nd image
            composeTestRule.onNodeWithContentDescription("Image Pager").performTouchInput {
                swipeLeft()
            }

            // Click image to open ViewImageActivity - should open at 2nd image
            composeTestRule.onNodeWithText("2/2").performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("2 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()

            // Test save image button in ViewImageActivity
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.save_to_device))
                .performClick()
            waitUntil { savedFile.exists() && (savedFile.length() > 0) }
            assertTrue(
                "Saved image file should exist and not be empty",
                savedFile.exists() && (savedFile.length() > 0),
            )

            // Scroll to 1st image in ViewImageActivity and check 1 / 2
            composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeRight() }

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()

            Espresso.pressBack()

            // Attach text file
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.attach_file)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("text.txt", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Click text file attachment to open it
            composeTestRule.onNodeWithText("text.txt").performClick()

            // Verify intent to open file was launched
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_VIEW)),
                )
            )

            // Record audio
            createSampleAudioFile(tempAudioFile)
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.record_audio)).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.start)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("00:01")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.stop)).performClick()

            // Verify audio section is rendered in EditScreen
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasText(context.getString(R.string.audio_recordings), substring = true)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            scenario.close()
        } finally {
            Intents.release()
            catFile.delete()
            dogFile.delete()
            textFile.delete()
            tempAudioFile.delete()
            if (savedFile.exists()) savedFile.delete()
        }
    }

    private fun createSampleAudioFile(file: File, durationSeconds: Int = 1) {
        file.parentFile?.mkdirs()
        val sampleRate = 8000
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val pcmDataSize = if (durationSeconds <= 1) 1600 else byteRate * durationSeconds
        val totalDataLen = pcmDataSize + 36

        val header =
            byteArrayOf(
                'R'.code.toByte(),
                'I'.code.toByte(),
                'F'.code.toByte(),
                'F'.code.toByte(),
                (totalDataLen and 0xff).toByte(),
                ((totalDataLen ushr 8) and 0xff).toByte(),
                ((totalDataLen ushr 16) and 0xff).toByte(),
                ((totalDataLen ushr 24) and 0xff).toByte(),
                'W'.code.toByte(),
                'A'.code.toByte(),
                'V'.code.toByte(),
                'E'.code.toByte(),
                'f'.code.toByte(),
                'm'.code.toByte(),
                't'.code.toByte(),
                ' '.code.toByte(),
                16,
                0,
                0,
                0,
                1,
                0,
                channels.toByte(),
                0,
                (sampleRate and 0xff).toByte(),
                ((sampleRate ushr 8) and 0xff).toByte(),
                0,
                0,
                (byteRate and 0xff).toByte(),
                ((byteRate ushr 8) and 0xff).toByte(),
                0,
                0,
                (channels * bitsPerSample / 8).toByte(),
                0,
                bitsPerSample.toByte(),
                0,
                'd'.code.toByte(),
                'a'.code.toByte(),
                't'.code.toByte(),
                'a'.code.toByte(),
                (pcmDataSize and 0xff).toByte(),
                ((pcmDataSize ushr 8) and 0xff).toByte(),
                ((pcmDataSize ushr 16) and 0xff).toByte(),
                ((pcmDataSize ushr 24) and 0xff).toByte(),
            )

        file.outputStream().use { out ->
            out.write(header)
            out.write(ByteArray(pcmDataSize))
        }
    }

    @Test
    fun attachmentDeletionAndImageViewer() {
        runBlocking { database.clearAllTables() }

        val testContext = InstrumentationRegistry.getInstrumentation().context
        val catFile = File(context.cacheDir, "cat_del.jpg")
        val dogFile = File(context.cacheDir, "dog_del.jpg")
        val textFile = File(context.cacheDir, "text_del.txt")

        testContext.classLoader.getResourceAsStream("attachments/cat.jpg")!!.use { input ->
            catFile.outputStream().use { output -> input.copyTo(output) }
        }
        testContext.classLoader.getResourceAsStream("attachments/dog.jpg")!!.use { input ->
            dogFile.outputStream().use { output -> input.copyTo(output) }
        }
        testContext.classLoader.getResourceAsStream("attachments/text.txt")!!.use { input ->
            textFile.outputStream().use { output -> input.copyTo(output) }
        }

        val catUri = context.getUriForFile(catFile)
        val dogUri = context.getUriForFile(dogFile)
        val textUri = context.getUriForFile(textFile)

        Intents.init()
        try {
            val attachFileResult =
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply { data = textUri },
                )
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

            val addImagesClipData =
                ClipData.newUri(context.contentResolver, "image", catUri).apply {
                    addItem(ClipData.Item(dogUri))
                }
            val addImagesResult =
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply { this.clipData = addImagesClipData },
                )
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*")),
                        ),
                    )
                )
                .respondWith(addImagesResult)

            val shareResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_SEND)),
                    )
                )
                .respondWith(shareResult)

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            // 1. Create a new note and add title/body
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.take_note))
                .performClick()

            val noteTitle = "Attachment Deletion Test Note"
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .performTextInput(noteTitle)
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.note))
                .performTextInput("Testing attachments deletion and image viewer")

            composeTestRule.waitUntil(10_000) {
                runBlocking { database.getBaseNoteDao().getAll().firstOrNull() != null }
            }
            val noteId = runBlocking { database.getBaseNoteDao().getAll().first().id }

            // 2. Add images
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.add_images)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1/2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1/2").assertIsDisplayed()
            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null && note.images.size == 2
            }

            // 3. Attach file
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.attach_file)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("text_del.txt", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("text_del.txt", substring = true).assertIsDisplayed()
            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null && note.files.isNotEmpty()
            }

            // 4. Test File Attachment Deletion:
            // 4a. Long-click file chip and cancel deletion
            composeTestRule.onNodeWithText("text_del.txt", substring = true).performTouchInput {
                longClick()
            }

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_file, "text_del.txt")))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

            // Verify file is still present
            composeTestRule.onNodeWithText("text_del.txt", substring = true).assertIsDisplayed()

            // 4b. Long-click file chip again and confirm deletion
            composeTestRule.onNodeWithText("text_del.txt", substring = true).performTouchInput {
                longClick()
            }

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_file, "text_del.txt")))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            // Wait until file chip is removed from EditScreen UI
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("text_del.txt", substring = true))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }

            // Verify file removal from database
            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null && note.files.isEmpty()
            }
            val noteAfterFileDelete = runBlocking { database.getBaseNoteDao().get(noteId) }
            assertTrue(
                "Note should exist and have no file attachments in database",
                noteAfterFileDelete != null && noteAfterFileDelete.files.isEmpty(),
            )

            // 5. Test Full-Screen Image Viewer Interactions:
            // Swipe left on Image Pager to switch to 2nd image ("2/2")
            composeTestRule.onNodeWithContentDescription("Image Pager").performTouchInput {
                swipeLeft()
            }
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("2/2")).fetchSemanticsNodes().isNotEmpty()
            }

            // Click image to open ViewImageActivity at 2nd image
            composeTestRule.onNodeWithText("2/2").performClick()

            // ViewImageActivity opens showing "2 / 2"
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("2 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()

            // 5a. Test swipe navigation in ViewImageActivity
            composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeRight() }
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()

            // 5b. Test Share Image action
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.share))
                .performClick()
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        Intent.EXTRA_INTENT,
                        allOf(hasAction(Intent.ACTION_SEND), hasType("image/jpeg")),
                    ),
                )
            )

            // 5c. Test Delete Image - cancel in dialog
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_image_forever)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
            // Still shows "1 / 2"
            composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()

            // 5d. Test Delete Image - confirm deletion of 1st image
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_image_forever)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            // Wait until title updates to "1 / 1" in ViewImageActivity
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1 / 1")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 / 1").assertIsDisplayed()

            // 5e. Navigate back to EditScreen and verify 1 image remains
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            // Wait until back in EditScreen and image count chip "1/2" or "2/2" is gone
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodes(hasText("1/2")).fetchSemanticsNodes().isEmpty() &&
                    composeTestRule.onAllNodes(hasText("2/2")).fetchSemanticsNodes().isEmpty()
            }

            // Verify in DB that only 1 image remains
            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null && note.images.size == 1
            }
            val noteWithOneImage = runBlocking { database.getBaseNoteDao().get(noteId) }
            assertTrue(
                "Note should have exactly 1 image in database",
                noteWithOneImage != null && noteWithOneImage.images.size == 1,
            )

            // 5f. Open ViewImageActivity again on the remaining image
            composeTestRule
                .onNode(hasClickAction() and hasAnyAncestor(hasContentDescription("Image Pager")))
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1 / 1")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 / 1").assertIsDisplayed()

            // Delete the last remaining image
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_image_forever)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            // Since last image was deleted, ViewImageActivity auto-finishes back to EditScreen
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
            }

            // Verify image pager is completely removed from EditScreen UI
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription("Image Pager"))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }

            // Verify in DB that all images are removed
            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null && note.images.isEmpty()
            }

            // 6. Navigate back to MainActivity and verify database and disk cleanup
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(context.getString(R.string.take_note)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule.waitUntil(10_000) {
                val note = runBlocking { database.getBaseNoteDao().get(noteId) }
                note != null &&
                    note.title == noteTitle &&
                    note.images.isEmpty() &&
                    note.files.isEmpty()
            }

            val finalNote = runBlocking { database.getBaseNoteDao().get(noteId) }
            assertTrue(
                "Final note should exist in database",
                finalNote != null,
            )
            assertEquals(
                "Final note should have saved title in database",
                noteTitle,
                finalNote?.title,
            )
            assertTrue(
                "Final note should have empty images in database",
                finalNote!!.images.isEmpty(),
            )
            assertTrue(
                "Final note should have empty files in database",
                finalNote.files.isEmpty(),
            )

            scenario.close()
        } finally {
            Intents.release()
            catFile.delete()
            dogFile.delete()
            textFile.delete()
        }
    }

    @Test
    fun moreOptions() {
        var note = createBaseNote(title = "Options Test Note", body = "Options Test Body")
        runBlocking {
            database.clearAllTables()
            val id = database.getBaseNoteDao().insert(note)
            note = note.copy(id = id)
        }

        Intents.init()
        try {
            val shareResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_SEND),
                        ),
                    )
                )
                .respondWith(shareResult)

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            val appNameLabel = context.getString(R.string.app_name)
            val moreOptionsLabel = context.getString(R.string.tap_for_more_options)
            val saveLabel = context.getString(R.string.save)

            // Open existing note
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Options Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(moreOptionsLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            // 1. Change Color
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.change_color)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(BaseNote.COLOR_NEW))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription(BaseNote.COLOR_NEW).performClick()
            composeTestRule.onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
            composeTestRule.onNodeWithText(saveLabel).performClick()

            // 2. Pin to status bar / Unpin from status bar
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.pin_to_status_bar))
                .performClick()
            waitUntil(10_000) {
                PinnedNotificationManager.getPinnedNotifications(context).any {
                    it?.id?.toLong() == note.id
                }
            }

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.unpin_from_status_bar))
                .performClick()
            waitUntil(10_000) {
                PinnedNotificationManager.getPinnedNotifications(context).none {
                    it?.id?.toLong() == note.id
                }
            }

            // 3. Convert to list / Convert to text
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.convert_to_list_note))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(context.getString(R.string.add_item)))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Options Test Body"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Options Test Body").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.convert_to_text_note))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(context.getString(R.string.note)))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.note))
                .assert(hasText("Options Test Body"))

            // 4. Share
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.share)).performClick()
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        Intent.EXTRA_INTENT,
                        hasAction(Intent.ACTION_SEND),
                    ),
                )
            )

            // 5. Duplicate
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.duplicate)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note (Copy)"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size == 1 &&
                    composeTestRule
                        .onAllNodes(hasText("Options Test Note (Copy)"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .size == 1
            }
            composeTestRule.onNodeWithText("Options Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(moreOptionsLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            // 6. Archive
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.archive)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(appNameLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            composeTestRule.navigateTo(NotallyNavKey.Archived)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Options Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(moreOptionsLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.unarchive)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(appNameLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            composeTestRule.navigateTo(NotallyNavKey.Notes)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Options Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(moreOptionsLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            // 7. Delete
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(appNameLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            composeTestRule.navigateTo(NotallyNavKey.Deleted)
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Options Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Options Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(moreOptionsLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            // 8. Delete Forever
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule
                .onNodeWithText(context.getString(R.string.delete_forever))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(appNameLabel))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty() &&
                    composeTestRule
                        .onAllNodes(hasText("Options Test Note"))
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
            }

            scenario.close()
        } finally {
            Intents.release()
        }
    }

    @Test
    fun export() {
        var note = createBaseNote(title = "Export Test Note", body = "Export Test Body")
        runBlocking {
            database.clearAllTables()
            val id = database.getBaseNoteDao().insert(note)
            note = note.copy(id = id)
        }

        val exportFiles =
            ExportMimeType.entries.associateWith { mimeType ->
                File(
                        context.cacheDir,
                        "export_test_${mimeType.name.lowercase()}.${mimeType.fileExtension}",
                    )
                    .apply { if (exists()) delete() }
            }

        Intents.init()
        try {
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_CREATE_DOCUMENT),
                        ),
                    )
                )
                .respondWithFunction { intent ->
                    val innerIntent =
                        IntentCompat.getParcelableExtra(
                            intent,
                            Intent.EXTRA_INTENT,
                            Intent::class.java,
                        )
                    val mimeType = ExportMimeType.entries.first { it.mimeType == innerIntent?.type }
                    val exportFile = exportFiles.getValue(mimeType)
                    Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                        Intent().apply { data = context.getUriForFile(exportFile) },
                    )
                }

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            // Open existing note
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Export Test Note"))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Export Test Note").performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.tap_for_more_options))
                    )
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }

            for (mimeType in ExportMimeType.entries) {
                exportNote(mimeType)
                verifyExportIntent(note, mimeType)
                assertExportedFile(note, mimeType, exportFiles.getValue(mimeType))
            }

            scenario.close()
        } finally {
            exportFiles.values.forEach { it.delete() }
            Intents.release()
        }
    }

    private fun exportNote(mimeType: ExportMimeType) {
        val moreOptionsLabel = context.getString(R.string.tap_for_more_options)
        composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.export)).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(mimeType.name))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(mimeType.name).performClick()
    }

    private fun verifyExportIntent(note: BaseNote, mimeType: ExportMimeType) {
        intended(
            allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(
                    Intent.EXTRA_INTENT,
                    allOf(
                        hasAction(Intent.ACTION_CREATE_DOCUMENT),
                        hasType(mimeType.mimeType),
                        hasExtra(Intent.EXTRA_TITLE, "${note.title}.${mimeType.fileExtension}"),
                    ),
                ),
            )
        )
    }

    private fun assertExportedFile(note: BaseNote, mimeType: ExportMimeType, file: File) {
        waitUntil(10_000) { file.exists() && file.length() > 0 }
        assertTrue(
            "Exported ${mimeType.name} file should exist and not be empty",
            file.exists() && file.length() > 0,
        )
        when (mimeType) {
            ExportMimeType.TXT -> {
                val content = file.readText()
                assertTrue(
                    "Exported TXT file should contain note body",
                    content.contains(note.body),
                )
            }
            ExportMimeType.MD -> {
                val content = file.readText()
                assertTrue(
                    "Exported MD file should contain note body",
                    content.contains(note.body),
                )
            }
            ExportMimeType.JSON -> {
                val json = JSONObject(file.readText())
                assertEquals(note.title, json.getString("title"))
                assertEquals(note.body, json.getString("body"))
                assertEquals(note.type.name, json.getString("type"))
            }
            ExportMimeType.HTML -> {
                val content = file.readText()
                assertTrue(
                    "Exported HTML file should contain note title heading",
                    content.contains("<h2>${note.title}</h2>"),
                )
                assertTrue(
                    "Exported HTML file should contain note body",
                    content.contains(note.body),
                )
            }
            ExportMimeType.PDF -> {
                val header = file.readBytes().copyOf(4).toString(Charsets.US_ASCII)
                assertEquals("%PDF", header)
            }
        }
    }

    @Test
    fun playAndControlAudioAttachment() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.RECORD_AUDIO,
        )

        runBlocking { database.clearAllTables() }

        val tempAudioFile = context.getTempAudioFile()
        createSampleAudioFile(tempAudioFile, durationSeconds = 6)

        val savedAudioFile =
            File(context.cacheDir, "saved_audio.m4a").apply { if (exists()) delete() }
        val savedAudioUri = context.getUriForFile(savedAudioFile)

        Intents.init()
        try {
            val shareResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(
                                hasAction(Intent.ACTION_SEND),
                                hasType("audio/mp4"),
                            ),
                        ),
                    )
                )
                .respondWith(shareResult)

            val saveAudioResult =
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply { data = savedAudioUri },
                )
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_CREATE_DOCUMENT)),
                    )
                )
                .respondWith(saveAudioResult)

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            // 1. Create a new note and add audio attachment
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.take_note))
                .performClick()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .performTextInput("Audio Playback Test")
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.note))
                .performTextInput("Testing audio player controls and deletion")

            val noteId = runBlocking { database.getBaseNoteDao().getAll().first().id }

            // Attach audio recording
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.record_audio)).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.start)).performClick()
            composeTestRule.waitUntil(15_000) {
                composeTestRule.onAllNodes(hasText("00:06")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.stop)).performClick()

            // Verify audio section and AudioItemRow are rendered in EditScreen
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasText(context.getString(R.string.audio_recordings), substring = true)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithText(context.getString(R.string.audio_recordings), substring = true)
                .assertIsDisplayed()

            // Wait until audio item row is displayed with .m4a
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(".m4a", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Obtain the recorded audio attachment info from DB
            val noteWithAudio = runBlocking { database.getBaseNoteDao().get(noteId) }
            assertTrue(
                "Note should have 1 audio attachment in database",
                noteWithAudio != null && noteWithAudio.audios.size == 1,
            )
            val audioAttachment = noteWithAudio!!.audios.first()
            val audioName = audioAttachment.name

            // Verify audio file exists in audio directory
            val audioDir = context.getCurrentAudioDirectory()
            val audioFileOnDisk = File(audioDir, audioName)
            assertTrue(
                "Audio file should exist in current audio directory",
                audioFileOnDisk.exists(),
            )

            // 2. Click on the AudioItemRow to open PlayAudioActivity
            composeTestRule.onNodeWithText(".m4a", substring = true).performClick()

            // 3. Verify PlayAudioScreen UI components in PlayAudioActivity
            // Wait for audio player service to prepare and display Play button
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.play)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.play)).assertIsDisplayed()
            composeTestRule.onNodeWithText("00:00").assertIsDisplayed()
            composeTestRule.onNodeWithText("00:06").assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.share))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.save_to_device))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .assertIsDisplayed()

            // 4. Test Play interaction
            composeTestRule.onNodeWithText(context.getString(R.string.play)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.pause)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.pause)).assertIsDisplayed()

            // 5. Test Pause interaction
            composeTestRule.onNodeWithText(context.getString(R.string.pause)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.play)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.play)).assertIsDisplayed()

            // 6. Test Seek bar dragging / setting progress
            composeTestRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .performSemanticsAction(SemanticsActions.SetProgress) { it(3000f) }

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("00:03")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("00:03").assertIsDisplayed()

            // Test touch swipe on seek bar
            composeTestRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .performTouchInput { swipeRight() }

            // 7. Test Share Audio
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.share))
                .performClick()
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        Intent.EXTRA_INTENT,
                        allOf(
                            hasAction(Intent.ACTION_SEND),
                            hasType("audio/mp4"),
                        ),
                    ),
                )
            )

            // 8. Test Save to Device
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.save_to_device))
                .performClick()
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_CREATE_DOCUMENT)),
                )
            )
            waitUntil { savedAudioFile.exists() && savedAudioFile.length() > 0 }
            assertTrue(
                "Exported audio file should exist and have data",
                savedAudioFile.exists() && savedAudioFile.length() > 0,
            )

            // 9. Test Delete Audio Attachment
            // 9a. Click delete icon in top bar -> confirm dialog appears
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_audio_recording_forever)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithText(context.getString(R.string.delete_audio_recording_forever))
                .assertIsDisplayed()

            // 9b. Cancel deletion -> dialog dismisses and PlayAudioActivity remains open
            composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_audio_recording_forever)))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            composeTestRule.onNodeWithText(context.getString(R.string.play)).assertIsDisplayed()

            // 9c. Click delete icon again and confirm deletion
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.delete))
                .performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(context.getString(R.string.delete_audio_recording_forever)))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNode(
                    hasText(context.getString(R.string.delete)) and
                        hasAnyAncestor(isComposeDialog())
                )
                .performClick()

            // 10. Verify returning to EditScreen and audio section is removed
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasText(context.getString(R.string.audio_recordings), substring = true)
                    )
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.audio_recordings), substring = true))
                .assertCountEquals(0)

            // 11. Verify audio deletion from Room database
            val noteAfterAudioDelete = runBlocking { database.getBaseNoteDao().get(noteId) }
            assertTrue(
                "Note in database should have no audio attachments after deletion",
                noteAfterAudioDelete != null && noteAfterAudioDelete.audios.isEmpty(),
            )

            // 12. Verify audio file deletion from disk
            waitUntil { !audioFileOnDisk.exists() }
            assertTrue(
                "Audio file should be deleted from disk",
                !audioFileOnDisk.exists(),
            )

            scenario.close()
        } finally {
            Intents.release()
            tempAudioFile.delete()
            if (savedAudioFile.exists()) savedAudioFile.delete()
        }
    }

    @Test
    fun recordAudioControls() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.RECORD_AUDIO,
        )

        val intent =
            Intent(context, MainActivity::class.java).apply { putNavKey(NotallyNavKey.RecordAudio) }
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        // 1. Verify initial UI state: timer at 00:00, Start button displayed, Stop button disabled
        composeTestRule.onNodeWithText("00:00").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.start)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.stop)).assertIsNotEnabled()

        // 2. Click Start to begin recording
        composeTestRule.onNodeWithText(context.getString(R.string.start)).performClick()

        // 3. Status becomes RECORDING: Start changes to Pause, Stop button becomes enabled
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.pause)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(context.getString(R.string.pause)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.stop)).assertIsEnabled()

        // 4. Click Pause: button changes to Resume
        composeTestRule.onNodeWithText(context.getString(R.string.pause)).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.resume)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(context.getString(R.string.resume)).assertIsDisplayed()

        // 5. Click Resume: button changes back to Pause
        composeTestRule.onNodeWithText(context.getString(R.string.resume)).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.pause)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(context.getString(R.string.pause)).assertIsDisplayed()

        // 6. Test back press confirmation dialog while recording is active
        Espresso.pressBack()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.save_recording)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.save_recording))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.discard)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()

        // 7. Cancel dialog: dismisses dialog and remains in RecordAudioActivity
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.save_recording)))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeTestRule.onNodeWithText(context.getString(R.string.stop)).assertIsDisplayed()

        // 8. Click Stop: stops recording and finishes activity
        composeTestRule.onNodeWithText(context.getString(R.string.stop)).performClick()

        waitUntil { scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }

        context.getTempAudioFile().delete()
        scenario.close()
    }

    @Test
    fun autoSaveAndUnsavedChanges() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val titleLabel = context.getString(R.string.title)
        val bodyLabel = context.getString(R.string.note)
        val backLabel = context.getString(R.string.back)

        // =========================================================================
        // Scenario 1: Auto-save on Home press (backgrounding the app)
        // =========================================================================
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        val homeTitle = "AutoSave Home Note"
        val homeBody = "Persisted when home is pressed without clicking save"
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput(homeBody)
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput(homeTitle)

        // Background the app via Home press
        device.pressHome()

        // Wait until note is auto-saved to Room database
        composeTestRule.waitUntil(10_000) {
            runBlocking { database.getBaseNoteDao().getAllNotes().any { it.title == homeTitle } }
        }
        val savedHomeNote = runBlocking {
            database.getBaseNoteDao().getAllNotes().first { it.title == homeTitle }
        }
        assertEquals(homeTitle, savedHomeNote.title)
        assertEquals(homeBody, savedHomeNote.body)

        // Bring app back to foreground
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!.apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(launchIntent)

        // Navigate back to notes list in MainActivity
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(homeTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(homeTitle).assertIsDisplayed()

        // =========================================================================
        // Scenario 2: Auto-save on Back press (New note)
        // =========================================================================
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        val backTitle = "AutoSave Back Note"
        val backBody = "Persisted on back press without clicking save"
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput(backBody)
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput(backTitle)

        // Navigate back via toolbar back button
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Wait until returning to MainActivity
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify returning to MainActivity and note is visible
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(backTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(backTitle).assertIsDisplayed()

        // Verify in Room database
        composeTestRule.waitUntil(10_000) {
            runBlocking { database.getBaseNoteDao().getAllNotes().any { it.title == backTitle } }
        }
        val savedBackNote = runBlocking {
            database.getBaseNoteDao().getAllNotes().first { it.title == backTitle }
        }
        assertEquals(backTitle, savedBackNote.title)
        assertEquals(backBody, savedBackNote.body)

        // =========================================================================
        // Scenario 3: Auto-save on Back press (Editing existing note)
        // =========================================================================
        // Click on the existing note card to reopen in editor
        composeTestRule.onNodeWithText(backTitle).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(backBody)).fetchSemanticsNodes().isNotEmpty()
        }

        // Modify title and body
        composeTestRule.onNodeWithContentDescription(titleLabel).appendText(" Updated")
        composeTestRule.onNodeWithContentDescription(bodyLabel).appendText(" More content")

        // Exit via toolbar back button
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Wait until returning to MainActivity
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val updatedTitle = "$backTitle Updated"
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(updatedTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(updatedTitle).assertIsDisplayed()

        // Verify updated note in database
        composeTestRule.waitUntil(10_000) {
            runBlocking {
                database.getBaseNoteDao().getAllNotes().any {
                    it.id == savedBackNote.id && it.title == updatedTitle
                }
            }
        }
        val reloadedNote = runBlocking {
            database.getBaseNoteDao().getAllNotes().first { it.id == savedBackNote.id }
        }
        assertEquals(updatedTitle, reloadedNote.title)
        assertTrue(reloadedNote.body.contains("More content"))

        // =========================================================================
        // Scenario 4: Discard empty note on back press (no phantom notes saved)
        // =========================================================================
        val noteCountBefore = runBlocking { database.getBaseNoteDao().getAllNotes().size }

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(titleLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Leave fields blank and press back
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val allNotesAfter = runBlocking { database.getBaseNoteDao().getAllNotes() }
        assertEquals(noteCountBefore, allNotesAfter.size)
        assertTrue(allNotesAfter.none { it.title.isEmpty() && it.body.isEmpty() })

        // =========================================================================
        // Scenario 5: Auto-save timer (idle timer) saves changes while remaining on editor screen
        // =========================================================================
        preferences.autoSaveAfterIdleTime.save(1) // 1 second idle timeout

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        val idleTitle = "AutoSave Idle Timer Note"
        val idleBody = "Persisted automatically via idle timer before leaving editor"
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput(idleBody)
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput(idleTitle)

        // Wait without pressing back or home - idle timer should auto-save to Room DB
        composeTestRule.waitUntil(10_000) {
            runBlocking { database.getBaseNoteDao().getAllNotes().any { it.title == idleTitle } }
        }

        val savedIdleNote = runBlocking {
            database.getBaseNoteDao().getAllNotes().first { it.title == idleTitle }
        }
        assertEquals(idleTitle, savedIdleNote.title)
        assertEquals(idleBody, savedIdleNote.body)

        // Now exit back to MainActivity
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(idleTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(idleTitle).assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun openGenericFileAttachment() {
        val testFile =
            File(context.cacheDir, "sample_doc.txt").apply {
                writeText("Sample file attachment content")
            }
        val fileUri = context.getUriForFile(testFile)

        Intents.init()
        try {
            val attachFileResultIntent = Intent().apply { data = fileUri }
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

            val openFileResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(Intent.EXTRA_INTENT, hasAction(Intent.ACTION_VIEW)),
                    )
                )
                .respondWith(openFileResult)

            val scenario = ActivityScenario.launch(MainActivity::class.java)

            val takeNoteLabel = context.getString(R.string.take_note)
            val titleLabel = context.getString(R.string.title)
            val bodyLabel = context.getString(R.string.note)
            val backLabel = context.getString(R.string.back)

            // 1. Create a new note
            composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()
            composeTestRule
                .onNodeWithContentDescription(titleLabel)
                .performTextInput("File Attachment Note")
            composeTestRule
                .onNodeWithContentDescription(bodyLabel)
                .performTextInput("File Attachment Body")

            // 2. Open Add Item bottom sheet and attach file
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.attach_file)).performClick()

            // 3. Wait for attachment to appear in editor
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("sample_doc.txt", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("sample_doc.txt", substring = true).assertIsDisplayed()

            // 4. Tap file attachment chip to trigger ACTION_VIEW
            composeTestRule.onNodeWithText("sample_doc.txt", substring = true).performClick()

            // 5. Verify ACTION_VIEW intent with URI was launched via chooser
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        Intent.EXTRA_INTENT,
                        allOf(
                            hasAction(Intent.ACTION_VIEW),
                            hasType("text/plain"),
                        ),
                    ),
                )
            )

            // 6. Clean up
            composeTestRule.onNodeWithContentDescription(backLabel).performClick()
            scenario.close()
        } finally {
            Intents.release()
            testFile.delete()
        }
    }
}
