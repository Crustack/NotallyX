package com.philkes.notallyx.presentation.activity.edit

import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.childAtPosition
import com.philkes.notallyx.test.hasBoldSpan
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.setSelection
import com.philkes.notallyx.test.waitUntilSucceeds
import org.hamcrest.Matchers.allOf
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
        val firstWord = "Lorem"

        R.id.EnterBody.byId().perform(replaceText(text), closeSoftKeyboard())
        R.id.EnterBody.byId().perform(setSelection(0, firstWord.length))

        R.string.bold.byContentDescription().perform(click())

        R.id.EnterBody.byId().check(matches(withText(text)))
        R.id.EnterBody.byId().check(matches(hasBoldSpan(0, firstWord.length)))

        scenario.close()
    }
}
