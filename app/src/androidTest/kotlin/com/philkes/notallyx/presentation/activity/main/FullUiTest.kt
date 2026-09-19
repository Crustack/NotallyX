package com.philkes.notallyx.presentation.activity.main

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FullUiTest {

    @Rule @JvmField var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule = GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun grantExactAlarmPermission() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName

        // Allows your app to set exact alarms without forcing the user to Settings
        device.executeShellCommand("appops set $packageName SCHEDULE_EXACT_ALARM allow")
    }

    private val mainListView = onView(withId(R.id.MainListView))

    @Test
    fun fullUiTest() {
        val takeNoteButton = onView(allOf(withId(R.id.TakeNote), isDisplayed()))
        takeNoteButton.perform(click())

        val noteBodyInput = onView(allOf(withId(R.id.EnterBody), isDisplayed()))
        noteBodyInput.perform(typeText("Body"), closeSoftKeyboard())

        val noteTitleInput = onView(allOf(withId(R.id.EnterTitle), isDisplayed()))
        noteTitleInput.perform(typeText("Test"), closeSoftKeyboard())

        val pinMenuItem = onView(allOf(withContentDescription("Pin"), isDisplayed()))
        pinMenuItem.perform(click())

        val moreOptionsButton =
            onView(allOf(withContentDescription("Tap for more options"), isDisplayed()))
        moreOptionsButton.perform(click())

        val changeColorMenuItem = onView(allOf(withText("Change Color"), isDisplayed()))
        changeColorMenuItem.perform(click())

        val newColorCard =
            onView(allOf(withId(R.id.CardView), withContentDescription("NEW"), isDisplayed()))
        newColorCard.perform(click())

        val colorCard =
            onView(allOf(withId(R.id.CardView), withContentDescription("#FAAFA9"), isDisplayed()))
        colorCard.perform(click())

        val saveColorButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveColorButton.perform(scrollTo(), click())

        val moreOptionsButton2 =
            onView(allOf(withContentDescription("Tap for more options"), isDisplayed()))
        moreOptionsButton2.perform(click())

        val labelsMenuItem = onView(allOf(withText("Labels"), isDisplayed()))
        labelsMenuItem.perform(click())

        val addLabelMenuItem = onView(allOf(withContentDescription("Add label"), isDisplayed()))
        addLabelMenuItem.perform(click())

        val labelNameInput = onView(allOf(withId(R.id.EditText), isDisplayed()))
        labelNameInput.perform(replaceText("label"))

        onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click())
        waitForRecyclerViewPosition(withId(R.id.MainListView), 0)
        onView(withId(R.id.MainListView)).perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val labelToolbarBackButton =
            onView(
                allOf(
                    childAtPosition(
                        allOf(withId(R.id.Toolbar), childAtPosition(withId(R.id.root_layout), 0)),
                        1,
                    ),
                    isDisplayed(),
                )
            )
        labelToolbarBackButton.perform(click())

        val labelChip =
            onView(
                allOf(
                    withText("label"),
                    withParent(
                        allOf(withId(R.id.LabelGroup), withParent(withId(R.id.ContentLayout)))
                    ),
                    isDisplayed(),
                )
            )
        labelChip.check(matches(withText("label")))

        val readOnlyButton = onView(allOf(withContentDescription("Read Only"), isDisplayed()))
        readOnlyButton.perform(click())

        val readOnlyNoteBody =
            onView(allOf(withId(R.id.EnterBody), withText("Body"), isDisplayed()))
        readOnlyNoteBody.perform(click())

        val editButton = onView(allOf(withContentDescription("Edit"), isDisplayed()))
        editButton.perform(click())
        waitFor(withContentDescription("Add Item"))
        val addItemButton = onView(allOf(withContentDescription("Add Item"), isDisplayed()))
        addItemButton.perform(click())
        Thread.sleep(1000)
        val addImagesLabel = onView(allOf(withText("Add images"), isDisplayed()))
        addImagesLabel.check(matches(isDisplayed()))

        Espresso.pressBack()
        Thread.sleep(1000)
        val noteToolbarBackButton =
            onView(
                allOf(
                    childAtPosition(
                        allOf(
                            withId(R.id.Toolbar),
                            childAtPosition(withId(R.id.main_content_layout), 0),
                        ),
                        0,
                    ),
                    isDisplayed(),
                )
            )
        noteToolbarBackButton.perform(click())
        waitFor(
            allOf(
                withId(R.id.Title),
                withText("Test"),
                withParent(
                    withParent(
                        IsInstanceOf.instanceOf(androidx.cardview.widget.CardView::class.java)
                    )
                ),
            )
        )
        val noteTitleCard =
            onView(
                allOf(
                    withId(R.id.Title),
                    withText("Test"),
                    withParent(
                        withParent(
                            IsInstanceOf.instanceOf(androidx.cardview.widget.CardView::class.java)
                        )
                    ),
                    isDisplayed(),
                )
            )
        noteTitleCard.check(matches(withText("Test")))

        val makeListButton = onView(allOf(withId(R.id.MakeList), isDisplayed()))
        makeListButton.perform(click())

        val firstListItemInput =
            onView(allOf(withId(R.id.EditText), withContentDescription("EditText0"), isDisplayed()))
        firstListItemInput.perform(typeText("A"), closeSoftKeyboard())
        onView(
                allOf(
                    withId(R.id.EditText),
                    withText("A"),
                    withContentDescription("EditText0"),
                    isDisplayed(),
                )
            )
            .perform(pressImeActionButton())

        val secondListItemInput =
            onView(allOf(withId(R.id.EditText), withContentDescription("EditText1"), isDisplayed()))
        secondListItemInput.perform(typeText("B"), closeSoftKeyboard())
        onView(
                allOf(
                    withId(R.id.EditText),
                    withText("B"),
                    withContentDescription("EditText1"),
                    isDisplayed(),
                )
            )
            .perform(pressImeActionButton())

        val thirdListItemInput =
            onView(allOf(withId(R.id.EditText), withContentDescription("EditText2"), isDisplayed()))
        thirdListItemInput.perform(typeText("C"), closeSoftKeyboard())
        onView(
                allOf(
                    withId(R.id.EditText),
                    withText("C"),
                    withContentDescription("EditText2"),
                    isDisplayed(),
                )
            )
            .perform(pressImeActionButton())

        val fourthListItemInput =
            onView(allOf(withId(R.id.EditText), withContentDescription("EditText3"), isDisplayed()))
        fourthListItemInput.perform(typeText("D"), closeSoftKeyboard())

        val thirdListItem =
            onView(
                allOf(
                    withId(R.id.EditText),
                    withText("C"),
                    withContentDescription("EditText2"),
                    isDisplayed(),
                )
            )
        thirdListItem.perform(click())
        thirdListItem.perform(pressImeActionButton())

        val listTitleInput = onView(allOf(withId(R.id.EnterTitle), isDisplayed()))
        listTitleInput.perform(typeText("List"), closeSoftKeyboard())

        val thirdItemCheckBox =
            onView(allOf(withId(R.id.CheckBox), withContentDescription("CheckBox2"), isDisplayed()))
        thirdItemCheckBox.perform(click())

        val secondItemCheckBox =
            onView(allOf(withId(R.id.CheckBox), withContentDescription("CheckBox1"), isDisplayed()))
        secondItemCheckBox.perform(click())

        val firstListItem =
            onView(
                allOf(
                    withId(R.id.EditText),
                    withText("A"),
                    withContentDescription("EditText0"),
                    isDisplayed(),
                )
            )
        firstListItem.check(matches(withText("A")))

        val remindersMenuItem = onView(allOf(withContentDescription("Reminders"), isDisplayed()))
        remindersMenuItem.perform(click())

        val datePickerOkButton = onView(allOf(withText("OK"), isDisplayed()))
        datePickerOkButton.perform(click())

        val timePickerOkButton =
            onView(
                allOf(
                    withId(com.google.android.material.R.id.material_timepicker_ok_button),
                    withText("OK"),
                    isDisplayed(),
                )
            )
        timePickerOkButton.perform(click())

        val customRepetitionOption = onView(allOf(withId(R.id.Custom), withText("Custom")))
        customRepetitionOption.perform(scrollTo(), click())

        val repetitionValueInput = onView(withId(R.id.Value))
        repetitionValueInput.perform(scrollTo(), replaceText("2"), closeSoftKeyboard())

        val daysTimeUnitOption = onView(allOf(withId(R.id.Days), withText("Days")))
        daysTimeUnitOption.perform(scrollTo(), click())

        val saveRepetitionButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveRepetitionButton.perform(scrollTo(), click())

        val repetitionText =
            onView(allOf(withId(R.id.Repetition), withText("Every 2 Days"), isDisplayed()))
        repetitionText.check(matches(withText("Every 2 Days")))

        val reminderToolbarBackButton =
            onView(
                allOf(
                    childAtPosition(
                        allOf(withId(R.id.Toolbar), childAtPosition(withId(R.id.root_layout), 0)),
                        1,
                    ),
                    isDisplayed(),
                )
            )
        reminderToolbarBackButton.perform(click())

        val listToolbarBackButton =
            onView(
                allOf(
                    childAtPosition(
                        allOf(
                            withId(R.id.Toolbar),
                            childAtPosition(withId(R.id.main_content_layout), 0),
                        ),
                        0,
                    ),
                    isDisplayed(),
                )
            )
        listToolbarBackButton.perform(click())

        mainListView.perform(actionOnItemAtPosition<ViewHolder>(1, longClick()))
        mainListView.perform(actionOnItemAtPosition<ViewHolder>(2, longClick()))

        val unpinActionItem = onView(allOf(withContentDescription("Unpin"), isDisplayed()))
        unpinActionItem.perform(click())

        mainListView.perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        val labelsActionItem = onView(allOf(withContentDescription("Labels"), isDisplayed()))
        labelsActionItem.perform(click())

        val labelDialogList = onView(childAtPosition(withId(androidx.appcompat.R.id.custom), 0))
        labelDialogList.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val labelDialogCheckBox =
            onView(
                allOf(
                    withId(R.id.CheckBox),
                    childAtPosition(
                        allOf(
                            withId(R.id.Layout),
                            childAtPosition(
                                withClassName(`is`("androidx.recyclerview.widget.RecyclerView")),
                                0,
                            ),
                        ),
                        0,
                    ),
                    isDisplayed(),
                )
            )
        labelDialogCheckBox.perform(click())

        val saveLabelsButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveLabelsButton.perform(scrollTo(), click())

        val noteLabelChip =
            onView(
                allOf(
                    withText("label"),
                    withParent(
                        allOf(
                            withId(R.id.LabelGroup),
                            withParent(IsInstanceOf.instanceOf(android.view.ViewGroup::class.java)),
                        )
                    ),
                    isDisplayed(),
                )
            )
        noteLabelChip.check(matches(withText("label")))

        val openDrawerButton =
            onView(allOf(withContentDescription("Open navigation drawer"), isDisplayed()))
        openDrawerButton.perform(click())

        val labelsDrawerItem = onView(allOf(withId(R.id.Labels), isDisplayed()))
        labelsDrawerItem.perform(click())

        val labelListItem =
            onView(
                allOf(
                    withId(R.id.LabelText),
                    withText("label"),
                    withParent(withParent(withId(R.id.MainListView))),
                    isDisplayed(),
                )
            )
        labelListItem.check(matches(withText("label")))

        val editLabelButton =
            onView(allOf(withId(R.id.EditButton), withContentDescription("Edit"), isDisplayed()))
        editLabelButton.perform(click())

        val editLabelInput = onView(allOf(withId(R.id.EditText), withText("label"), isDisplayed()))
        editLabelInput.perform(replaceText("label1"))
        onView(allOf(withId(R.id.EditText), withText("label1"), isDisplayed()))
            .perform(closeSoftKeyboard())

        val saveRenamedLabelButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveRenamedLabelButton.perform(scrollTo(), click())

        val renamedLabelListItem =
            onView(
                allOf(
                    withId(R.id.LabelText),
                    withText("label1"),
                    withParent(withParent(withId(R.id.MainListView))),
                    isDisplayed(),
                )
            )
        renamedLabelListItem.check(matches(withText("label1")))

        val addLabelToolbarButton =
            onView(allOf(withContentDescription("Add label"), isDisplayed()))
        addLabelToolbarButton.perform(click())

        val newLabelInput = onView(allOf(withId(R.id.EditText), isDisplayed()))
        newLabelInput.perform(replaceText("label2"), closeSoftKeyboard())
        onView(allOf(withId(R.id.EditText), withText("label2"), isDisplayed())).perform(click())

        val saveNewLabelButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveNewLabelButton.perform(scrollTo(), click())

        val secondLabelListItem =
            onView(
                allOf(
                    withId(R.id.LabelText),
                    withText("label2"),
                    childAtPosition(childAtPosition(withId(R.id.MainListView), 0), 1),
                    isDisplayed(),
                )
            )
        secondLabelListItem.perform(longClick())

        val secondLabelListItemText =
            onView(
                allOf(
                    withId(R.id.LabelText),
                    withText("label2"),
                    withParent(withParent(withId(R.id.MainListView))),
                    isDisplayed(),
                )
            )
        secondLabelListItemText.check(matches(withText("label2")))

        val deleteLabelButton =
            onView(
                allOf(
                    withId(R.id.DeleteButton),
                    withContentDescription("Delete"),
                    childAtPosition(childAtPosition(withId(R.id.MainListView), 0), 4),
                    isDisplayed(),
                )
            )
        deleteLabelButton.perform(click())

        val confirmDeleteLabelButton =
            onView(allOf(withId(android.R.id.button1), withText("Delete")))
        confirmDeleteLabelButton.perform(scrollTo(), click())

        openDrawerButton.perform(click())

        val notesDrawerItem = onView(allOf(withId(R.id.Notes), isDisplayed()))
        notesDrawerItem.perform(click())

        mainListView.perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        val deleteActionItem = onView(allOf(withContentDescription("Delete"), isDisplayed()))
        deleteActionItem.perform(click())

        openDrawerButton.perform(click())

        val deletedDrawerItem = onView(allOf(withId(R.id.Deleted), isDisplayed()))
        deletedDrawerItem.perform(click())

        val deletedNote =
            onView(allOf(withParent(withParent(withId(R.id.MainListView))), isDisplayed()))
        deletedNote.check(matches(isDisplayed()))

        val deleteAllMenuItem = onView(allOf(withContentDescription("Delete all"), isDisplayed()))
        deleteAllMenuItem.perform(click())

        val confirmDeleteAllButton = onView(allOf(withId(android.R.id.button1), withText("Delete")))
        confirmDeleteAllButton.perform(scrollTo(), click())

        openDrawerButton.perform(click())

        val remindersDrawerItem = onView(allOf(withId(R.id.Reminders), isDisplayed()))
        remindersDrawerItem.perform(click())

        val elapsedFilterChip =
            onView(allOf(withId(R.id.elapsed), withText("Elapsed"), isDisplayed()))
        elapsedFilterChip.perform(click())

        val allFilterChip = onView(allOf(withId(R.id.all), withText("All"), isDisplayed()))
        allFilterChip.perform(click())

        openDrawerButton.perform(click())

        val settingsDrawerItem = onView(allOf(withId(R.id.Settings), isDisplayed()))
        settingsDrawerItem.perform(click())

        val viewSettingRow = onView(allOf(withId(R.id.View), isDisplayed()))
        viewSettingRow.perform(click())

        val viewSettingOption =
            onData(anything())
                .inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
                .atPosition(1)
        viewSettingOption.perform(click())

        val showSearchInTopBarRow = onView(allOf(withId(R.id.ShowSearchInTopBar), isDisplayed()))
        showSearchInTopBarRow.perform(click())

        val showSearchEnabledOption =
            onView(allOf(withId(R.id.EnabledButton), withText("Enabled"), isDisplayed()))
        showSearchEnabledOption.perform(click())

        val notesSortOrderRow = onView(allOf(withId(R.id.NotesSortOrder), isDisplayed()))
        notesSortOrderRow.perform(click())

        val sortByModifiedOption = onView(allOf(withText("Modified"), isDisplayed()))
        sortByModifiedOption.perform(click())

        val sortAscendingOption = onView(allOf(withText("Ascending"), isDisplayed()))
        sortAscendingOption.perform(click())

        val saveSortOrderButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveSortOrderButton.perform(scrollTo(), click())

        val backupPasswordRow = onView(withId(R.id.BackupPassword))
        backupPasswordRow.perform(scrollTo(), click())

        val backupPasswordInput =
            onView(allOf(withId(R.id.InputText), withContentDescription("Input"), isDisplayed()))
        backupPasswordInput.perform(replaceText("1234"), closeSoftKeyboard())

        val saveBackupPasswordButton = onView(allOf(withId(android.R.id.button1), withText("Save")))
        saveBackupPasswordButton.perform(scrollTo(), click())

        val secureFlagRow = onView(withId(R.id.SecureFlag))
        secureFlagRow.perform(scrollTo(), click())

        val secureFlagEnabledOption =
            onView(allOf(withId(R.id.EnabledButton), withText("Enabled"), isDisplayed()))
        secureFlagEnabledOption.perform(click())

        secureFlagRow.perform(scrollTo(), click())

        val secureFlagDisabledOption =
            onView(allOf(withId(R.id.DisabledButton), withText("Disabled"), isDisplayed()))
        secureFlagDisabledOption.perform(click())

        openDrawerButton.perform(click())

        val notesDrawerItemFinal = onView(allOf(withId(R.id.Notes), isDisplayed()))
        notesDrawerItemFinal.perform(click())

        noteTitleCard.check(matches(withText("Test")))
        noteTitleCard.perform(click())

        noteBodyInput.check(matches(allOf(isDisplayed(), withText("Body"))))
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup &&
                    parentMatcher.matches(parent) &&
                    view == parent.getChildAt(position)
            }
        }
    }

    fun waitFor(matcher: Matcher<View>, timeoutMs: Long = 5_000) {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (_: NoMatchingViewException) {
                // Keep waiting
            } catch (_: AssertionError) {
                // View exists but isn't displayed yet
            }

            Thread.sleep(50)
        }

        // Let Espresso produce the normal, useful failure message
        onView(matcher).check(matches(isDisplayed()))
    }

    fun waitForRecyclerViewPosition(
        recyclerViewMatcher: Matcher<View>,
        position: Int,
        timeoutMs: Long = 10_000,
    ) {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                onView(recyclerViewMatcher)
                    .perform(
                        RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
                    )

                return
            } catch (_: NoMatchingViewException) {
                // RecyclerView not found yet
            } catch (_: PerformException) {
                // Position doesn't exist yet
            }

            Thread.sleep(50)
        }

        // Produce Espresso's normal failure
        onView(recyclerViewMatcher)
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
    }
}
