package com.philkes.notallyx.presentation.activity.main

import android.app.Notification
import android.util.Log
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.presentation.activity.note.refreshStatusBarPin
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.childAtPosition
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSucceeds
import com.philkes.notallyx.utils.PinnedNotificationManager
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class MainActivityTest : UiTestBase() {

    @Test
    fun multiSelectAndApplyLabel() {
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
        // 2. Recreate Activity so it reads the newly inserted data
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(1, longClick()))
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(2, longClick()))

        "Unpin".byContentDescription().perform(click())

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        "Labels".byContentDescription().perform(click())

        val labelDialogList =
            onDisplayView(childAtPosition(withId(androidx.appcompat.R.id.custom), 0))
        labelDialogList.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val labelDialogCheckBox =
            onDisplayView(
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
                )
            )
        labelDialogCheckBox.perform(click())

        R.string.save.byText(withId(android.R.id.button1)).perform(scrollTo(), click())

        val noteLabelChip =
            onDisplayView(
                allOf(
                    withText("label"),
                    withParent(
                        allOf(
                            withId(R.id.LabelGroup),
                            withParent(IsInstanceOf.instanceOf(android.view.ViewGroup::class.java)),
                        )
                    ),
                )
            )
        noteLabelChip.check(matches(withText("label")))

        scenario.close()
    }

    @Test
    fun restorePinnedNotifications() {
        var pinned1 =
            createBaseNote(
                title = "Pinned 1",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        var pinned2 =
            createBaseNote(
                title = "Pinned 2",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        runBlocking {
            withContext(Dispatchers.Main) {
                val baseNoteDao = database.getBaseNoteDao()
                pinned1 = pinned1.copy(id = baseNoteDao.insert(pinned1))
                pinned2 = pinned2.copy(id = baseNoteDao.insert(pinned2))
                baseNoteDao.insert(
                    createBaseNote(title = "Not Pinned", body = "Body", labels = listOf("label"))
                )
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.refreshStatusBarPin(pinned1)
            activity.refreshStatusBarPin(pinned2)
        }

        waitUntil(10_000) { PinnedNotificationManager.getPinnedNotifications(context).size == 2 }
        val pinnedNotifications = PinnedNotificationManager.getPinnedNotifications(context)
        assertTrue(
            "Notification for every pinned Note shown",
            setOf(pinned1, pinned2).all { pinnedNote ->
                pinnedNotifications.any {
                    it!!.id.toLong() == pinnedNote.id &&
                        pinnedNote.title.contains(
                            it.notification.extras.getString(Notification.EXTRA_TITLE)!!
                        ) &&
                        pinnedNote.body.contains(
                            it.notification.extras.getString(Notification.EXTRA_TEXT)!!
                        )
                }
            },
        )

        scenario.close()
    }

    @Test
    fun databaseParallel() {
        runBlocking {
            withContext(Dispatchers.Main) {
                database
                    .getBaseNoteDao()
                    .insert(
                        createBaseNote(title = "Note -1", body = "Body", labels = listOf("label"))
                    )
            }
        }

        val application = ApplicationProvider.getApplicationContext<NotallyXApplication>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val numTasks = 25
        runBlocking(Dispatchers.IO) {
            val baseNoteDao = database.getBaseNoteDao()
            val insertTasks =
                (1..numTasks).map {
                    async(Dispatchers.IO) {
                        baseNoteDao.insert(
                            createBaseNote(title = "Note $it", isPinnedToStatus = it % 2 == 0)
                        )
                    }
                }
            val restoreTasks =
                (1..numTasks).map {
                    async(Dispatchers.Main) { application.restorePinnedNotifications() }
                }
            val readTasks =
                (1..numTasks * 2).map {
                    async(Dispatchers.IO) {
                        val allNotes = baseNoteDao.getAll()
                        Log.d("FullUiTest", "Total notes: ${allNotes.size}")
                        delay(100.milliseconds)
                    }
                }
            (insertTasks + restoreTasks + readTasks).awaitAll()
        }

        waitUntil(10_000) {
            runBlocking { database.getBaseNoteDao().getAll().size == numTasks + 1 }
        }

        scenario.close()
    }

    @Test
    fun searchNotes() {
        // 1. Insert test data for normal, archived, and deleted notes
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    listOf(
                        createBaseNote(
                            title = "Normal Note",
                            body = "Grocery list",
                            folder = Folder.NOTES,
                        ),
                        createBaseNote(
                            title = "Archived Note",
                            body = "Grocery history",
                            folder = Folder.ARCHIVED,
                        ),
                        createBaseNote(
                            title = "Deleted Note",
                            body = "Grocery trash",
                            folder = Folder.DELETED,
                        ),
                        createBaseNote(
                            title = "Work Meeting",
                            body = "Project status",
                            folder = Folder.NOTES,
                        ),
                    )
                )
        }

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 2. Open Search fragment
        R.string.search.byContentDescription().perform(click())

        // 3. Type search keyword matching "Grocery"
        R.id.EnterSearchKeyword.byId().perform(typeText("Grocery"), closeSoftKeyboard())

        // 4. Verify default "Notes" folder chip only shows normal notes
        waitUntilSucceeds { "Normal Note".byText().check(matches(isDisplayed())) }
        "Work Meeting".byText(checkDisplayed = false).check(doesNotExist())
        "Archived Note".byText(checkDisplayed = false).check(doesNotExist())
        "Deleted Note".byText(checkDisplayed = false).check(doesNotExist())

        // 5. Switch filter to "Archived" folder chip
        R.id.Archived.byId().perform(click())
        waitUntilSucceeds { "Archived Note".byText().check(matches(isDisplayed())) }
        "Normal Note".byText(checkDisplayed = false).check(doesNotExist())
        "Deleted Note".byText(checkDisplayed = false).check(doesNotExist())

        // 6. Switch filter to "Deleted" folder chip
        R.id.Deleted.byId().perform(click())
        waitUntilSucceeds { "Deleted Note".byText().check(matches(isDisplayed())) }
        "Normal Note".byText(checkDisplayed = false).check(doesNotExist())
        "Archived Note".byText(checkDisplayed = false).check(doesNotExist())

        // 7. Switch filter back to "Notes" folder chip
        R.id.Notes.byId().perform(click())
        waitUntilSucceeds { "Normal Note".byText().check(matches(isDisplayed())) }

        // 8. Search for keyword with no matches
        R.id.EnterSearchKeyword.byId()
            .perform(replaceText("NonExistentKeyword"), closeSoftKeyboard())
        waitUntilSucceeds { "Normal Note".byText(checkDisplayed = false).check(doesNotExist()) }
        "Archived Note".byText(checkDisplayed = false).check(doesNotExist())
        "Deleted Note".byText(checkDisplayed = false).check(doesNotExist())

        // 9. Cancel search to return to main notes list
        R.string.cancel.byContentDescription().perform(click())
        waitUntilSucceeds { "Work Meeting".byText().check(matches(isDisplayed())) }

        scenario.close()
    }
}
