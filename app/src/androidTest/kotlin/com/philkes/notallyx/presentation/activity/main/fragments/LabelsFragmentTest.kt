package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
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
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onLabelItem
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
class LabelsFragmentTest : UiTestBase() {

    @Test
    fun manageLabels() {
        // 1. Insert data
        runBlocking {
            database.getLabelDao().insert(listOf(Label("label", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Test",
                            body = "Body",
                            pinned = true,
                            labels = listOf("label"),
                        ),
                        createBaseNote(
                            title = "List",
                            pinned = true,
                            items =
                                listOf(createListItem("A"), createListItem("B", isChild = true)),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Labels)

        val labelListItem =
            onDisplayView(
                allOf(
                    withId(R.id.LabelText),
                    withText("label"),
                    withParent(withParent(withId(R.id.MainListView))),
                )
            )
        labelListItem.check(matches(withText("label")))

        R.id.EditButton.byId().perform(click())
        R.id.EditText.byId(withText("label")).perform(replaceText("label1"))
        R.string.save.byText(withId(android.R.id.button1)).perform(scrollTo(), click())
        onLabelItem("label1").check(matches(withText("label1")))

        R.string.add_label.byContentDescription().perform(click())
        R.id.EditText.byId().perform(replaceText("label2"), closeSoftKeyboard())
        R.string.save.byText(withId(android.R.id.button1)).perform(scrollTo(), click())

        // TODO: drag and drop is flaky
        //                dragAndDrop(By.text("label2"), By.text("label1"))
        //        waitUntilSucceeds {    R.id.MainListView.byId().checkPositionHasText(1, "label2")
        // }
        waitUntilSucceeds {
            R.id.MainListView.byId().onPositionView(0, withId(R.id.DeleteButton)).perform(click())
        }

        R.string.delete.byText(withId(android.R.id.button1)).perform(scrollTo(), click())

        scenario.close()
    }

    @Test
    fun unlabeledNotesFilter() {
        // 1. Insert unlabeled and labeled notes
        runBlocking {
            database.getLabelDao().insert(listOf(Label("Work", 0)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Unlabeled Note",
                            body = "Unlabeled Body",
                            labels = emptyList(),
                        ),
                        createBaseNote(
                            title = "Labeled Note",
                            body = "Labeled Body",
                            labels = listOf("Work"),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Unlabeled)

        waitUntilSucceeds { onView(withText("Unlabeled Note")).check(matches(isDisplayed())) }
        onView(withText("Labeled Note")).check(doesNotExist())

        scenario.close()
    }

    @Test
    fun labelsNavigationFilter() {
        // 1. Insert multiple notes with different labels
        runBlocking {
            database.getLabelDao().insert(listOf(Label("Work", 0), Label("Personal", 1)))
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Work Note 1",
                            body = "Work Body 1",
                            labels = listOf("Work"),
                        ),
                        createBaseNote(
                            title = "Work Note 2",
                            body = "Work Body 2",
                            labels = listOf("Work"),
                        ),
                        createBaseNote(
                            title = "Personal Note 1",
                            body = "Personal Body 1",
                            labels = listOf("Personal"),
                        ),
                    )
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Open drawer and check if each label is displayed in navigation view
        onView(withId(R.id.DrawerLayout)).perform(DrawerActions.open())

        onDisplayView(allOf(withText("Work"), isDescendantOfA(withId(R.id.NavigationView))))
            .check(matches(isDisplayed()))

        onDisplayView(allOf(withText("Personal"), isDescendantOfA(withId(R.id.NavigationView))))
            .check(matches(isDisplayed()))

        // Click on "Work" label in navigation view
        onDisplayView(allOf(withText("Work"), isDescendantOfA(withId(R.id.NavigationView))))
            .perform(click())

        waitUntilSucceeds { onView(withText("Work Note 1")).check(matches(isDisplayed())) }
        onView(withText("Work Note 2")).check(matches(isDisplayed()))
        onView(withText("Personal Note 1")).check(doesNotExist())

        // Open drawer again and click on "Personal" label
        onView(withId(R.id.DrawerLayout)).perform(DrawerActions.open())

        onDisplayView(allOf(withText("Personal"), isDescendantOfA(withId(R.id.NavigationView))))
            .perform(click())

        waitUntilSucceeds { onView(withText("Personal Note 1")).check(matches(isDisplayed())) }
        onView(withText("Work Note 1")).check(doesNotExist())
        onView(withText("Work Note 2")).check(doesNotExist())

        scenario.close()
    }
}
