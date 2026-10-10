package com.philkes.notallyx.presentation.activity.note

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.typeTextAtEnd
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSucceeds
import java.io.File
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditTextPlainActivityTest : UiTestBase() {

    @Test
    fun editAndSavePlainTextFile() {
        val testFile =
            File(context.cacheDir, "sample_plain_file.txt").apply {
                writeText("Original file content")
            }
        val fileUri = Uri.fromFile(testFile)

        val intent =
            Intent(context, EditTextPlainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(fileUri, "text/plain")
            }

        val scenario = ActivityScenario.launch<EditTextPlainActivity>(intent)

        waitUntilSucceeds {
            R.id.EnterTitle.byId().check(matches(withText("sample_plain_file.txt")))
        }
        R.id.EnterTitle.byId().check(matches(not(isEnabled())))
        R.id.EnterBody.byId().check(matches(withText("Original file content")))

        val appendText = " - appended text"
        val expectedContent = "Original file content - appended text"
        R.id.EnterBody.byId().typeTextAtEnd(appendText)

        R.string.save.byContentDescription().perform(click())

        // assertToastDisplayed(R.string.saved_to_device)
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
            Intent(context, EditTextPlainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(originalUri, "text/plain")
            }

        Intents.init()
        try {
            val resultIntent = Intent().apply { data = newUri }
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultIntent)
            intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(result)

            val scenario = ActivityScenario.launch<EditTextPlainActivity>(intent)

            waitUntilSucceeds {
                R.id.EnterTitle.byId().check(matches(withText("original_file.txt")))
            }

            val appendText = " - new text"
            val expectedContent = "Original content - new text"
            R.id.EnterBody.byId().typeTextAtEnd(appendText)

            R.string.save_to_device.byContentDescription().perform(click())

            // assertToastDisplayed(R.string.saved_to_device)
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
        val intent = Intent(context, EditTextPlainActivity::class.java)
        val scenario = ActivityScenario.launch<EditTextPlainActivity>(intent)

        val noteTitle = "Converted Plain Note"
        val noteBody = "Body text for plain note conversion"

        R.id.EnterTitle.byId().typeTextAtEnd(noteTitle)
        R.id.EnterBody.byId().typeTextAtEnd(noteBody)

        androidx.test.espresso.Espresso.closeSoftKeyboard()

        R.string.convert_to_text_note.byContentDescription().perform(click())

        waitUntil {
            database.getBaseNoteDao().getByTitle("Converted Plain Note").any { note ->
                (note.title == noteTitle) &&
                    (note.body.equals(noteBody, ignoreCase = true)) &&
                    (note.type == Type.NOTE)
            }
        }

        waitUntilSucceeds {
            R.id.EnterTitle.byId().check(matches(withText(noteTitle)))
            R.id.EnterBody.byId().check(matches(withText(noteBody)))
        }

        scenario.close()
    }
}
