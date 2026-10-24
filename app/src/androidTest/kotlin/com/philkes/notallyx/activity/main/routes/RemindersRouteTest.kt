package com.philkes.notallyx.activity.main.routes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
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
class RemindersRouteTest : UiTestBase() {

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

        composeTestRule.navigateTo(NotallyNavKey.Reminders)

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
