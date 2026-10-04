package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class RemindersFragmentTest : UiTestBase() {

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

        navigateTo(R.id.Reminders)

        R.id.elapsed.byId().perform(click())
        // Only the elapsed reminder note is shown
        onView(withText("ElapsedNote")).check(matches(isDisplayed()))
        onView(withText("FutureNote")).check(doesNotExist())

        R.id.upcoming.byId().perform(click())
        // Only the elapsed reminder note is shown
        onView(withText("ElapsedNote")).check(doesNotExist())
        onView(withText("FutureNote")).check(matches(isDisplayed()))

        R.id.all.byId().perform(click())
        // All reminder notes are shown
        onView(withText("ElapsedNote")).check(matches(isDisplayed()))
        onView(withText("FutureNote")).check(matches(isDisplayed()))

        scenario.close()
    }
}
