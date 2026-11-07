package com.philkes.notallyx.activity.note

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.waitUntil
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditTextPlainActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun editAndSavePlainTextFile() {
        val testFile =
            File(context.cacheDir, "sample_plain_file.txt").apply {
                writeText("Original file content")
            }
        val fileUri = Uri.fromFile(testFile)

        val intent =
            Intent(context, MainActivity::class.java).apply {
                component =
                    ComponentName(
                        context,
                        "com.philkes.notallyx.feature.editor.EditTextPlainActivity",
                    )
                action = Intent.ACTION_VIEW
                setDataAndType(fileUri, "text/plain")
            }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        val titleDesc = context.getString(R.string.title)
        val bodyDesc = context.getString(R.string.note)

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(titleDesc))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithContentDescription(titleDesc)
            .assertTextEquals("sample_plain_file.txt")
        composeTestRule
            .onNodeWithContentDescription(bodyDesc)
            .assertTextEquals("Original file content")

        val appendText = " - appended text"
        val expectedContent = "Original file content - appended text"
        composeTestRule.onNodeWithContentDescription(bodyDesc).appendText(appendText)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.save))
            .performClick()

        waitUntil { testFile.readText() == expectedContent }
        assertEquals(expectedContent, testFile.readText())

        scenario.close()
        testFile.delete()
    }

    @Test
    fun editAndSaveAsNewFile() {
        val originalFile =
            File(context.cacheDir, "original_file.txt").apply { writeText("Original content") }
        val originalUri = Uri.fromFile(originalFile)

        val newFile = File(context.cacheDir, "saved_as_new_file.txt")
        if (newFile.exists()) {
            newFile.delete()
        }
        val newUri = Uri.fromFile(newFile)

        val intent =
            Intent(context, MainActivity::class.java).apply {
                component =
                    ComponentName(
                        context,
                        "com.philkes.notallyx.feature.editor.EditTextPlainActivity",
                    )
                action = Intent.ACTION_VIEW
                setDataAndType(originalUri, "text/plain")
            }

        Intents.init()
        try {
            val resultIntent = Intent().apply { data = newUri }
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent)
            intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(result)

            val scenario = ActivityScenario.launch<MainActivity>(intent)

            val titleDesc = context.getString(R.string.title)
            val bodyDesc = context.getString(R.string.note)

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasContentDescription(titleDesc))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(titleDesc)
                .assertTextEquals("original_file.txt")

            val appendText = " - new text"
            val expectedContent = "Original content - new text"
            composeTestRule.onNodeWithContentDescription(bodyDesc).appendText(appendText)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.save_to_device))
                .performClick()

            waitUntil { newFile.exists() && newFile.readText() == expectedContent }
            assertEquals(expectedContent, newFile.readText())
            assertEquals("Original content", originalFile.readText())

            scenario.close()
        } finally {
            Intents.release()
            originalFile.delete()
            if (newFile.exists()) {
                newFile.delete()
            }
        }
    }

    @Test
    fun convertToTextNote() {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                component =
                    ComponentName(
                        context,
                        "com.philkes.notallyx.feature.editor.EditTextPlainActivity",
                    )
            }
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        val noteTitle = "Converted Plain Note"
        val noteBody = "Body text for plain note conversion"

        val titleDesc = context.getString(R.string.title)
        val bodyDesc = context.getString(R.string.note)

        composeTestRule.onNodeWithContentDescription(titleDesc).appendText(noteTitle)
        composeTestRule.onNodeWithContentDescription(bodyDesc).appendText(noteBody)

        Espresso.closeSoftKeyboard()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.convert_to_text_note))
            .performClick()

        waitUntil {
            runBlocking {
                database.getBaseNoteDao().getByTitle("Converted Plain Note").any { note ->
                    (note.title == noteTitle) &&
                        (note.body.equals(noteBody, ignoreCase = true)) &&
                        (note.type == Type.NOTE)
                }
            }
        }

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(titleDesc))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(titleDesc).assertTextEquals(noteTitle)
        composeTestRule.onNodeWithContentDescription(bodyDesc).assertTextEquals(noteBody)

        scenario.close()
    }
}
