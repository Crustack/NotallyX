package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Label
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
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DeletedFragmentTest : UiTestBase() {

    @Test
    fun deleteNotes() {
        // 1. Insert data
        runBlocking {
            database.getLabelDao().insert(listOf(Label("label", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        R.string.delete.byContentDescription().perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Deleted)

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Test")))
        }

        R.string.delete_all.byContentDescription().perform(click())

        val confirmDeleteAllButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
        confirmDeleteAllButton.perform(scrollTo(), click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        scenario.close()
    }
}
