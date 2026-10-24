package com.philkes.notallyx.activity.edit

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.EditActivity
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.media.audio.RecordAudioActivity
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.assertSubtextHasLink
import com.philkes.notallyx.test.assertSubtextHasNoFormatting
import com.philkes.notallyx.test.assertSubtextHasStrikethrough
import com.philkes.notallyx.test.assertSubtextIsBold
import com.philkes.notallyx.test.assertSubtextIsItalic
import com.philkes.notallyx.test.assertSubtextIsMonospace
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.performClickOnSubtext
import com.philkes.notallyx.test.waitUntil
import java.io.File
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun createAndEditTextNote() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val titleLabel = context.getString(R.string.title)
        val bodyLabel = context.getString(R.string.note)
        val saveLabel = context.getString(R.string.save)
        val addLabel = context.getString(R.string.add_label)
        val backLabel = context.getString(R.string.back)

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("Body")
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Test")

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.pin)).performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.change_color)).performClick()
        composeTestRule.onNodeWithContentDescription(BaseNote.COLOR_NEW).performClick()
        composeTestRule.onNodeWithContentDescription(Color.CORAL.toColorString()).performClick()
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.labels)).performClick()
        composeTestRule.onNodeWithContentDescription(addLabel).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("label")
        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("label").performClick()

        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("label").assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
        composeTestRule.onNodeWithText("Test").assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun textFormatting() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val bodyLabel = context.getString(R.string.note)

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing."
        val bodyNode = composeTestRule.onNodeWithContentDescription(bodyLabel)

        bodyNode.performTextInput(text)

        // Open formatting bar
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.edit))
            .performClick()

        // Select and format "Lorem" as bold
        bodyNode.performTextInputSelection(TextRange(0, 5))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.bold))
            .performClick()

        // Select and format "ipsum" as italic
        bodyNode.performTextInputSelection(TextRange(6, 11))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.italic))
            .performClick()

        // Select and format "dolor" as strikethrough
        bodyNode.performTextInputSelection(TextRange(12, 17))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.strikethrough))
            .performClick()

        // Select and format "sit" as monospace
        bodyNode.performTextInputSelection(TextRange(18, 21))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.monospace))
            .performClick()

        // Select and format "amet" as link
        bodyNode.performTextInputSelection(TextRange(22, 26))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.link))
            .performClick()
        val linkUrl = "https://example.com"
        composeTestRule
            .onAllNodes(hasSetTextAction() and hasAnyAncestor(isComposeDialog()))[1]
            .performTextInput(linkUrl)
        composeTestRule
            .onNode(hasText(context.getString(R.string.save)) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // Select "consectetur", format as bold and then clear formatting
        bodyNode.performTextInputSelection(TextRange(28, 39))
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.bold))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.clear_formatting))
            .performClick()

        // Validate formatting worked
        bodyNode
            .assertSubtextIsBold("Lorem")
            .assertSubtextIsItalic("ipsum")
            .assertSubtextHasStrikethrough("dolor")
            .assertSubtextIsMonospace("sit")
            .assertSubtextHasLink("amet", linkUrl)
            .assertSubtextHasNoFormatting("consectetur")

        // Click on the text that has the link to open options dialog
        bodyNode.performClickOnSubtext("amet")

        // In options dialog, click "Edit Link"
        composeTestRule
            .onNode(
                hasText(context.getString(R.string.edit_link)) and hasAnyAncestor(isComposeDialog())
            )
            .performClick()

        // Edit link and display text
        val updatedText = "amet updated"
        val updatedUrl = "https://updated-example.com"
        composeTestRule
            .onAllNodes(hasSetTextAction() and hasAnyAncestor(isComposeDialog()))[0]
            .performTextReplacement(updatedText)
        composeTestRule
            .onAllNodes(hasSetTextAction() and hasAnyAncestor(isComposeDialog()))[1]
            .performTextReplacement(updatedUrl)
        composeTestRule
            .onNode(hasText(context.getString(R.string.save)) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        // Assert text changed and link is updated
        bodyNode
            .assert(hasText(updatedText, substring = true))
            .assertSubtextHasLink(updatedText, updatedUrl)

        scenario.close()
    }

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
    fun undoAndRedo() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val bodyLabel = context.getString(R.string.note)

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        // Step 1: Perform first edit
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("A")

        // Step 2: Perform second edit
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("B")

        // Step 3: Perform third edit
        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("C")

        // 1. Single tap undo
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.undo))
            .performClick()

        // 2. Single tap redo
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.redo))
            .performClick()

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

            val recordAudioResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(hasComponent(RecordAudioActivity::class.java.name))
                .respondWith(recordAudioResult)

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

    private fun createSampleAudioFile(file: File) {
        file.parentFile?.mkdirs()
        val pcmDataSize = 1600
        val totalDataLen = pcmDataSize + 36

        val header =
            byteArrayOf(
                'R'.code.toByte(),
                'I'.code.toByte(),
                'F'.code.toByte(),
                'F'.code.toByte(),
                (totalDataLen and 0xff).toByte(),
                ((totalDataLen ushr 8) and 0xff).toByte(),
                0,
                0,
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
                1,
                0,
                0x40,
                0x1f,
                0,
                0,
                0x80.toByte(),
                0x3e,
                0,
                0,
                2,
                0,
                16,
                0,
                'd'.code.toByte(),
                'a'.code.toByte(),
                't'.code.toByte(),
                'a'.code.toByte(),
                (pcmDataSize and 0xff).toByte(),
                ((pcmDataSize ushr 8) and 0xff).toByte(),
                0,
                0,
            )

        file.outputStream().use { out ->
            out.write(header)
            out.write(ByteArray(pcmDataSize))
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
        val exportFile = File(context.cacheDir, "export_test.txt").apply { if (exists()) delete() }
        try {
            val exportUri = context.getUriForFile(exportFile)
            val exportResult =
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply { data = exportUri },
                )
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_CREATE_DOCUMENT),
                        ),
                    )
                )
                .respondWith(exportResult)

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

            var currentEditActivity: EditActivity? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val resumedActivities =
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                currentEditActivity =
                    resumedActivities.filterIsInstance<EditActivity>().firstOrNull()
            }
            assertEquals(Color.CORAL.toColorString(), currentEditActivity?.notallyModel?.color)
            assertEquals(
                context.extractColor(Color.CORAL.toColorString()),
                currentEditActivity?.colorInt,
            )

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

            // 4. Export
            composeTestRule.onNodeWithContentDescription(moreOptionsLabel).performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.export)).performClick()
            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText(ExportMimeType.TXT.name))
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(ExportMimeType.TXT.name).performClick()
            intended(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(
                        Intent.EXTRA_INTENT,
                        hasAction(Intent.ACTION_CREATE_DOCUMENT),
                    ),
                )
            )
            waitUntil(10_000) { exportFile.exists() && exportFile.length() > 0 }
            assertTrue(
                "Exported TXT file should exist and contain note body",
                exportFile.exists() && exportFile.readText().contains("Options Test Body"),
            )

            // 5. Share
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

            // 6. Duplicate
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

            // 7. Archive
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

            // 8. Delete
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

            // 9. Delete Forever
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
            exportFile.delete()
            Intents.release()
        }
    }

    @Test
    fun search() {
        val noteTitle = "Search Test Note"
        val padding = "Line of filler text in the note.\n".repeat(25)
        val noteBody =
            "The quick brown fox jumps over the lazy dog.\n" +
                padding +
                "Fox is fast in the middle.\n" +
                padding +
                "Another fox is near at the bottom."
        val note = createBaseNote(title = noteTitle, body = noteBody)
        runBlocking { database.getBaseNoteDao().insert(note) }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val searchLabel = context.getString(R.string.search)
        val nextLabel = context.getString(R.string.next)
        val prevLabel = context.getString(R.string.previous)
        val backLabel = context.getString(R.string.back)

        // Wait for MainActivity to display the note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        // Open existing note
        composeTestRule.onNodeWithText(noteTitle).performClick()

        // Wait for EditActivity to load
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(searchLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Click search action in top bar
        composeTestRule.onNodeWithContentDescription(searchLabel).performClick()

        // Enter search query into search bar
        composeTestRule.onNodeWithContentDescription(searchLabel).performTextInput("fox")

        // Check occurrence indicator displays "1/3" and first match line is displayed
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("1/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("1/3").assertIsDisplayed()
        composeTestRule.onNodeWithText("The quick brown fox", substring = true).assertIsDisplayed()

        // Jump to next occurrence -> "2/3" and verify scrolled to middle match
        composeTestRule.onNodeWithContentDescription(nextLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("2/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Fox is fast in the middle", substring = true)
            .assertIsDisplayed()

        // Jump to next occurrence -> "3/3" and verify scrolled to bottom match
        composeTestRule.onNodeWithContentDescription(nextLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("3/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Another fox is near at the bottom", substring = true)
            .assertIsDisplayed()

        // Jump to previous occurrence -> "2/3" and verify scrolled back to middle match
        composeTestRule.onNodeWithContentDescription(prevLabel).performClick()
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("2/3")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Fox is fast in the middle", substring = true)
            .assertIsDisplayed()

        // Close search mode
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        scenario.close()
    }
}
