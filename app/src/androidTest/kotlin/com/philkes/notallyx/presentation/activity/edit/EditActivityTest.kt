package com.philkes.notallyx.presentation.activity.edit

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.activity.note.EditListActivity
import com.philkes.notallyx.presentation.view.note.listitem.adapter.ListItemAdapter
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.childAtPosition
import com.philkes.notallyx.test.hasBoldSpan
import com.philkes.notallyx.test.hasItalicSpan
import com.philkes.notallyx.test.hasMonospaceSpan
import com.philkes.notallyx.test.hasNoSpans
import com.philkes.notallyx.test.hasStrikethroughSpan
import com.philkes.notallyx.test.hasUrlSpan
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.setSelection
import com.philkes.notallyx.test.swipeItem
import com.philkes.notallyx.test.waitUntilSucceeds
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditActivityTest : UiTestBase() {

    @Test
    fun createAndEditTextNote() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.TakeNote.byId().perform(click())

        R.id.EnterBody.byId().perform(typeText("Body"), closeSoftKeyboard())
        R.id.EnterTitle.byId().perform(typeText("Test"), closeSoftKeyboard())

        R.string.pin.byContentDescription().perform(click())

        R.string.tap_for_more_options.byContentDescription().perform(click())
        R.string.change_color.byText().perform(click())
        R.id.CardView.byId(withContentDescription(BaseNote.COLOR_NEW)).perform(click())
        R.id.CardView.byId(withContentDescription(Color.CORAL.toColorString())).perform(click())
        R.string.save.byText(withId(android.R.id.button1)).perform(scrollTo(), click())

        R.string.tap_for_more_options.byContentDescription().perform(click())
        onDisplayView(withText(R.string.labels)).perform(click())
        R.string.add_label.byContentDescription().perform(click())
        R.id.EditText.byId().perform(replaceText("label"))
        android.R.id.button1.byId().perform(click())
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val labelToolbarBackButton =
            onDisplayView(
                childAtPosition(
                    allOf(withId(R.id.Toolbar), childAtPosition(withId(R.id.root_layout), 0)),
                    1,
                )
            )
        labelToolbarBackButton.perform(click())
        val labelChip =
            onDisplayView(
                allOf(
                    withText("label"),
                    withParent(
                        allOf(withId(R.id.LabelGroup), withParent(withId(R.id.ContentLayout)))
                    ),
                )
            )
        labelChip.check(matches(withText("label")))

        R.string.read_only.byContentDescription().perform(click())
        R.id.EnterBody.byId(withText("Body")).perform(click())

        R.string.edit.byContentDescription().perform(click())
        R.string.add_item.byContentDescription().perform(click())
        R.string.add_images.byText().check(matches(isDisplayed()))

        Espresso.pressBack()
        toolbarBackButton.perform(click())
        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(1, withId(R.id.Title))
                .check(matches(withText("Test")))
        }

        scenario.close()
    }

    @Test
    fun textFormatting() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.TakeNote.byId().perform(click())

        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing."
        val word1 = "Lorem"
        val word2 = "ipsum"
        val word3 = "dolor"
        val word4 = "sit"
        val word5 = "amet"

        R.id.EnterBody.byId().perform(replaceText(text), closeSoftKeyboard())

        // 1. Bold on "Lorem" (0..5)
        R.id.EnterBody.byId().perform(setSelection(0, word1.length))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(1))
        R.string.bold.byContentDescription().perform(click())
        R.id.EnterBody.byId().check(matches(hasBoldSpan(0, word1.length)))

        // 2. Italic on "ipsum" (6..11)
        val start2 = text.indexOf(word2)
        val end2 = start2 + word2.length
        R.id.EnterBody.byId().perform(setSelection(start2, end2))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(2))
        R.string.italic.byContentDescription().perform(click())
        R.id.EnterBody.byId().check(matches(hasItalicSpan(start2, end2)))

        // 3. Strikethrough on "dolor" (12..17)
        val start3 = text.indexOf(word3)
        val end3 = start3 + word3.length
        R.id.EnterBody.byId().perform(setSelection(start3, end3))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(3))
        R.string.strikethrough.byContentDescription().perform(click())
        R.id.EnterBody.byId().check(matches(hasStrikethroughSpan(start3, end3)))

        // 4. Monospace on "sit" (18..21)
        val start4 = text.indexOf(word4)
        val end4 = start4 + word4.length
        R.id.EnterBody.byId().perform(setSelection(start4, end4))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(4))
        R.string.monospace.byContentDescription().perform(click())
        R.id.EnterBody.byId().check(matches(hasMonospaceSpan(start4, end4)))

        // 5. Link on "amet" (22..26)
        val start5 = text.indexOf(word5)
        val end5 = start5 + word5.length
        val url = "https://example.com"
        R.id.EnterBody.byId().perform(setSelection(start5, end5))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(0))
        R.string.link.byContentDescription().perform(click())
        R.id.InputText2.byId().perform(replaceText(url))
        R.string.save.byText(withId(android.R.id.button1)).perform(click())
        R.id.EnterBody.byId().check(matches(hasUrlSpan(start5, end5, url)))

        // Verify full text remains intact
        R.id.EnterBody.byId().check(matches(withText(text)))

        // 6. Clear formatting across all text
        R.id.EnterBody.byId().perform(setSelection(0, text.length))
        R.id.MainListView.byId().perform(scrollToPosition<ViewHolder>(5))
        R.string.clear_formatting.byContentDescription().perform(click())
        R.id.EnterBody.byId().check(matches(withText(text)))
        R.id.EnterBody.byId().check(matches(hasNoSpans()))

        scenario.close()
    }

    @Test
    fun listItems() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.MakeList.byId().perform(click())

        // 1. Create list items (Parent and 2 Children)
        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.EditText))
            .perform(click(), typeText("Parent Item"), closeSoftKeyboard())

        R.id.AddItem.byId().perform(click())
        R.id.MainListView.byId()
            .onPositionView(1, withId(R.id.EditText))
            .perform(click(), typeText("Child Item 1"), closeSoftKeyboard())

        R.id.AddItem.byId().perform(click())
        R.id.MainListView.byId()
            .onPositionView(2, withId(R.id.EditText))
            .perform(click(), typeText("Child Item 2"), closeSoftKeyboard())

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.EditText))
            .check(matches(withText("Parent Item")))
        R.id.MainListView.byId()
            .onPositionView(1, withId(R.id.EditText))
            .check(matches(withText("Child Item 1")))
        R.id.MainListView.byId()
            .onPositionView(2, withId(R.id.EditText))
            .check(matches(withText("Child Item 2")))

        // 2. Swipe Child Item 1 and Child Item 2 right to make them child items
        swipeItem(By.text("Child Item 1"), Direction.RIGHT)
        swipeItem(By.text("Child Item 2"), Direction.RIGHT)

        // 3. Verify in the RecyclerView's adapter list that items have correct isChild values
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val currentActivity =
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull() as EditListActivity
            val recyclerView = currentActivity.findViewById<RecyclerView>(R.id.MainListView)
            val adapter = recyclerView.adapter as ListItemAdapter
            val items = adapter.currentList
            assertEquals(3, items.size)
            assertFalse("Parent Item should not be a child", items[0].isChild)
            assertTrue("Child Item 1 should be a child", items[1].isChild)
            assertTrue("Child Item 2 should be a child", items[2].isChild)
        }

        // 4. Check "Child Item 1"
        R.id.MainListView.byId().onPositionView(1, withId(R.id.CheckBox)).perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.EditText))
                .check(matches(withText("Parent Item")))
            R.id.MainListView.byId()
                .onPositionView(1, withId(R.id.EditText))
                .check(matches(withText("Child Item 1")))
            R.id.MainListView.byId()
                .onPositionView(2, withId(R.id.EditText))
                .check(matches(withText("Child Item 2")))
        }

        // 5. Delete "Child Item 2" and assert it is gone
        R.id.MainListView.byId().onPositionView(2, withId(R.id.EditText)).perform(click())
        R.id.MainListView.byId().onPositionView(2, withId(R.id.Delete)).perform(click())

        waitUntilSucceeds { "Child Item 2".byText(checkDisplayed = false).check(doesNotExist()) }

        // With Child Item 1 checked and Child Item 2 deleted, Parent Item automatically moves to
        // CheckedListView
        waitUntilSucceeds {
            R.id.CheckedListView.byId()
                .onPositionView(0, withId(R.id.EditText))
                .check(matches(withText("Parent Item")))
        }

        // 6. Change "Parent Item" text and assert correct text change
        R.id.CheckedListView.byId()
            .onPositionView(0, withId(R.id.EditText))
            .perform(click(), replaceText("Updated Parent Item"), closeSoftKeyboard())

        R.id.CheckedListView.byId()
            .onPositionView(0, withId(R.id.EditText))
            .check(matches(withText("Updated Parent Item")))

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

        R.id.TakeNote.byId().perform(click())
        R.id.EnterTitle.byId().perform(typeText("Reminder Note"), closeSoftKeyboard())

        // Click Reminders button on top toolbar (opens RemindersActivity)
        R.string.reminders.byContentDescription().perform(click())

        // 1. Reminder 1: None repetition
        confirmDateAndTime()
        android.R.id.button1.byId().perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Repetition))
                .check(matches(withText(R.string.reminder_no_repetition)))
        }

        // 2. Reminder 2: Daily repetition
        R.string.add_reminder.byContentDescription().perform(click())
        confirmDateAndTime()
        R.id.Daily.byId().perform(click())
        android.R.id.button1.byId().perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(1, withId(R.id.Repetition))
                .check(matches(withText(R.string.daily)))
        }

        // 3. Reminder 3: Weekly repetition
        R.string.add_reminder.byContentDescription().perform(click())
        confirmDateAndTime()
        R.id.Weekly.byId().perform(click())
        android.R.id.button1.byId().perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(2, withId(R.id.Repetition))
                .check(matches(withText(R.string.weekly)))
        }

        // 4. Reminder 4: Monthly repetition
        R.string.add_reminder.byContentDescription().perform(click())
        confirmDateAndTime()
        R.id.Monthly.byId().perform(click())
        onData(anything()).atPosition(0).perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(3, withId(R.id.Repetition))
                .check(matches(withText(R.string.monthly)))
        }

        // 5. Reminder 5: Yearly repetition
        R.string.add_reminder.byContentDescription().perform(click())
        confirmDateAndTime()
        R.id.Yearly.byId().perform(click())
        android.R.id.button1.byId().perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(4, withId(R.id.Repetition))
                .check(matches(withText(R.string.yearly)))
        }

        // 6. Reminder 6: Custom repetition (every 3rd week)
        R.string.add_reminder.byContentDescription().perform(click())
        confirmDateAndTime()
        R.id.Custom.byId().perform(click())
        R.id.Value.byId().perform(click(), replaceText("3"))
        R.id.Weeks.byId().perform(click())
        android.R.id.button1.byId().perform(click())

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(5, withId(R.id.DateTime))
                .check(matches(withText(containsString("12:"))))
            R.id.MainListView.byId()
                .onPositionView(5, withId(R.id.Repetition))
                .check(matches(isDisplayed()))
        }

        // Go back from RemindersActivity to EditActivity
        Espresso.pressBack()

        // Verify reminder chip is visible and displays reminder time
        waitUntilSucceeds { R.id.EditNoteReminderChip.byId().check(matches(isDisplayed())) }

        scenario.close()
    }
}
