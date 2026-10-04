package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntilSucceeds
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class ArchivedFragmentTest : UiTestBase() {

    @Test
    fun archiveNotes() {
        // 1. Insert data
        runBlocking {
            database.getBaseNoteDao().insert(listOf(createBaseNote(title = "Test", body = "Body")))
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        openActionBarOverflowOrOptionsMenu(context)
        onDisplayView(withText(R.string.archive)).inRoot(isPlatformPopup()).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Archived)

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(1, withId(R.id.Title))
                .check(matches(withText("Test")))
        }

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(1, longClick()))
        R.string.unarchive.byContentDescription().perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Notes)

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test")))
        }

        scenario.close()
    }
}
