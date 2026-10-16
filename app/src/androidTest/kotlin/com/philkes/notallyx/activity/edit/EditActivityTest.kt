package com.philkes.notallyx.activity.edit

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.media.audio.RecordAudioActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.waitUntil
import java.io.File
import org.hamcrest.Matchers.allOf
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

        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput(text)

        // Open formatting bar
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.edit))
            .performClick()

        // Formatting controls
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.bold))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.italic))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.strikethrough))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.monospace))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.clear_formatting))
            .performClick()

        scenario.close()
    }

    @Test
    fun listItems() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val makeListLabel = context.getString(R.string.make_list)
        val titleLabel = context.getString(R.string.title)

        composeTestRule.onNodeWithContentDescription(makeListLabel).performClick()

        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Shopping List")

        composeTestRule.onNodeWithText(context.getString(R.string.add_item)).performClick()

        composeTestRule.onNodeWithText("Shopping List").assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun reminders() {
        fun confirmDateAndTime() {
            onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
            onView(withId(com.google.android.material.R.id.material_timepicker_ok_button))
                .perform(click())
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

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText(context.getString(R.string.yearly)))
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
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(remindersLabel).assertIsDisplayed()

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
                composeTestRule.onAllNodes(hasText("2")).fetchSemanticsNodes().isNotEmpty()
            }

            // Click image to open ViewImageActivity
            composeTestRule.onNodeWithText("2").performClick()

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("1 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("1 / 2").assertIsDisplayed()

            // Test save image button in ViewImageActivity
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.save_to_device))
                .performClick()
            waitUntil { savedFile.exists() && (savedFile.length() > 0) }
            assertTrue(
                "Saved image file should exist and not be empty",
                savedFile.exists() && (savedFile.length() > 0),
            )

            // Scroll to 2nd image in ViewImageActivity and check 2 / 2
            composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeLeft() }

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText("2 / 2")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("2 / 2").assertIsDisplayed()

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
}
