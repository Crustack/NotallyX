package com.philkes.notallyx.activity.main.fragments

// import com.philkes.notallyx.R
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class RemindersFragmentTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun remindersFilter() {
        // 1. Insert notes with reminders: one elapsed (past, no repetition), one upcoming (future)
        runBlocking {
            val now = System.currentTimeMillis()
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "ElapsedNote",
                            body = "Body",
                            reminders =
                                listOf(
                                    Reminder(
                                        id = 1L,
                                        dateTime = Date(now - 24 * 60 * 60 * 1000),
                                        repetition = null,
                                    )
                                ),
                        ),
                        createBaseNote(
                            title = "FutureNote",
                            body = "Body",
                            reminders =
                                listOf(
                                    Reminder(
                                        id = 2L,
                                        dateTime = Date(now + 24 * 60 * 60 * 1000),
                                        repetition = null,
                                    )
                                ),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.navigateTo(R.id.Reminders)

        composeTestRule.onNodeWithText(context.getString(R.string.elapsed)).performClick()
        // Only the elapsed reminder note is shown
        composeTestRule.onNodeWithText("ElapsedNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("FutureNote").assertDoesNotExist()

        composeTestRule.onNodeWithText(context.getString(R.string.upcoming)).performClick()
        // Only the future reminder note is shown
        composeTestRule.onNodeWithText("ElapsedNote").assertDoesNotExist()
        composeTestRule.onNodeWithText("FutureNote").assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.all)).performClick()
        // All reminder notes are shown
        composeTestRule.onNodeWithText("ElapsedNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("FutureNote").assertIsDisplayed()

        scenario.close()
    }
}
