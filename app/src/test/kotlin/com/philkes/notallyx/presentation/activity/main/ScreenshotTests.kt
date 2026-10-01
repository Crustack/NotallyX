package com.philkes.notallyx.presentation.activity.main

import android.content.Intent
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.philkes.notallyx.R
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.presentation.activity.main.MainActivity.Companion.EXTRA_FRAGMENT_TO_OPEN
import com.philkes.notallyx.presentation.activity.note.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.presentation.activity.note.EditNoteActivity
import com.philkes.notallyx.test.FakeAndroidKeyStore
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.context
import com.philkes.notallyx.test.database
import com.philkes.notallyx.test.generateNotes
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.preferences
import com.philkes.notallyx.test.toMillis
import com.philkes.notallyx.test.waitUntilSucceeds
import io.mockk.clearStaticMockk
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    manifest = Config.NONE,
    sdk = [35],
    shadows = [ShadowContextImplMedia::class],
    qualifiers = RobolectricDeviceQualifiers.Pixel5,
)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ScreenshotTests {
    @get:Rule
    val roborazziRule =
        RoborazziRule(
            options =
                RoborazziRule.Options(
                    roborazziOptions =
                        RoborazziOptions(
                            compareOptions =
                                RoborazziOptions.CompareOptions(
                                    changeThreshold = 0.05f // For 1% accepted difference
                                    //                    imageComparator = SimpleImageComparator(
                                    //                        maxDistance = 0.007F, // 0.001F is
                                    // default value from Differ
                                    //                        vShift = 2, // Increasing the shift
                                    // can help resolve antialiasing issues
                                    //                        hShift = 2 // Increasing the shift can
                                    // help resolve antialiasing issues
                                    //                    )
                                )
                        )
                )
        )

    private val fixedTime = Date("2026-01-02 12:00".toMillis())

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FakeAndroidKeyStore.setup
        }
    }

    @After
    fun tearDown() {
        // Clean up static mocks after each test
        clearStaticMockk(EncryptedSharedPreferences::class)
    }

    @Test
    fun editNote() = runTest {
        val noteId =
            withContext(Dispatchers.IO) {
                    database.getBaseNoteDao().insert(generateNotes(1, fixedTime))
                }
                .first()
        val intent =
            Intent(context, EditNoteActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_BASE_NOTE, noteId)
            }
        ActivityScenario.launch<EditNoteActivity>(intent).use { scenario ->
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/00_initial.png"
                )
            onView(withId(R.id.EnterTitle)).perform(typeText("Test Title"))
            onView(withId(R.id.EnterBody)).perform(typeText("Test Body"))
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/01_title_and_body.png"
                )
        }
    }

    @Test
    fun settings() = runTest {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.Settings)
            }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/00_initial.png"
                )
            scenario.scrollAndScreenshotScrollView(
                R.id.NestedScrollView,
                {
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(it)}_settings_scroll.png"
                },
            )
        }
    }

    @Test
    fun navigationDrawer() = runTest {
        withContext(Dispatchers.IO) {
            database.getLabelDao().apply { insert((1..5).map { Label("Label$it", it) }) }
            preferences.labelsHidden.save(setOf("Label1", "Label4"))
        }
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.Labels)
            }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val drawerLayout = activity.findViewById<DrawerLayout>(R.id.DrawerLayout)
                drawerLayout.openDrawer(GravityCompat.START, false) // false = disable animation
            }
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/navigationDrawer/00_initial.png"
                )
        }
    }

    @Test
    fun labels() = runTest {
        withContext(Dispatchers.IO) {
            database.getLabelDao().apply { insert((1..5).map { Label("Label$it", it) }) }
            preferences.labelsHidden.save(setOf("Label1", "Label4"))
        }
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.Labels)
            }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            onView(isRoot())
                .captureRoboImage("${ScreenshotTests::class.java.simpleName}/labels/00_initial.png")

            // Edit Label
            R.id.MainListView.byId().onPositionView(1, withId(R.id.EditButton)).perform(click())
            R.id.EditText.byId()
                .inRoot(isDialog())
                .perform(replaceText(""), typeText("Label Foo"), closeSoftKeyboard())
            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/01_edit_label_dialog.png"
                )
            R.string.save
                .byText(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(scrollTo(), click())
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/02_edit_label_saved.png"
                )

            // Toggle Visibilities
            R.id.MainListView.byId()
                .onPositionView(1, withId(R.id.VisibilityButton))
                .perform(click())
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.VisibilityButton))
                .perform(click())
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/03_label_visibility_toggled.png"
                )

            // Delete Label
            R.id.MainListView.byId().onPositionView(2, withId(R.id.DeleteButton)).perform(click())
            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/04_delete_label_dialog.png"
                )
            R.string.delete
                .byText(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(scrollTo(), click())
            waitUntilSucceeds {
                R.id.MainListView.byId()
                    .onPositionView(2, withId(R.id.LabelText))
                    .check(matches(withText("Label2")))
            }
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/05_deleted_label.png"
                )

            // Add Label
            R.string.add_label.byContentDescription().perform(click())
            R.id.EditText.byId()
                .inRoot(isDialog())
                .perform(typeText("Label New"), closeSoftKeyboard())
            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/06_new_label_dialog.png"
                )
            R.string.save
                .byText(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(scrollTo(), click())
            waitUntilSucceeds {
                R.id.MainListView.byId()
                    .onPositionView(0, withId(R.id.LabelText))
                    .check(matches(withText("Label New")))
            }
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/labels/07_label_added.png"
                )
        }
    }

    @Test
    fun reminders() {
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
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.Reminders)
            }
        ActivityScenario.launch<MainActivity>(intent).use {
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/reminders/00_initial.png"
                )
            R.id.elapsed.byId().perform(click())
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/reminders/01_elapsed.png"
                )

            R.id.upcoming.byId().perform(click())
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/reminders/02_upcoming.png"
                )

            R.id.all.byId().perform(click())
            onView(isRoot())
                .captureRoboImage("${ScreenshotTests::class.java.simpleName}/reminders/03_all.png")
        }
    }

    @Test
    fun notesOverview() = runTest {
        withContext(Dispatchers.IO) {
            database.getBaseNoteDao().apply {
                insert(
                    (1..10).flatMap { idx ->
                        // TODO: add dummy images arbitrarly
                        generateNotes(idx, fixedTime)
                    }
                )
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Get total count from adapter safely on the main thread
            scenario.scrollAndScreenshotRecyclerView(
                recyclerViewId = R.id.MainListView,
                {
                    "${ScreenshotTests::class.java.simpleName}/notesOverview/${"%02d".format(it)}_notes_overview_scroll.png"
                },
            )
        }
    }

    /**
     * Scrolls through a ScrollView or NestedScrollView in step increments equal to a fraction of
     * its visible height and captures a Roborazzi screenshot at each step.
     *
     * @param scrollViewId The resource ID of the ScrollView/NestedScrollView.
     * @param folderPath The directory/file prefix for saved screenshots (e.g.,
     *   "settings/overview").
     * @param stepRatio The percentage of visible height to scroll per step (default 0.8 = 80%).
     * @return The number of screenshots taken.
     */
    fun ActivityScenario<*>.scrollAndScreenshotScrollView(
        @IdRes scrollViewId: Int,
        screenshotNameFunction: (Int) -> String,
        stepRatio: Float = 0.8f,
    ): Int {
        var totalHeight = 0
        var viewportHeight = 0

        // Measure view dimensions on the main thread
        onActivity { activity ->
            val scrollView =
                activity.findViewById<View>(scrollViewId)
                    ?: throw IllegalArgumentException("View with ID $scrollViewId not found")

            val child =
                when (scrollView) {
                    is NestedScrollView -> scrollView.getChildAt(0)
                    is android.widget.ScrollView -> scrollView.getChildAt(0)
                    else ->
                        throw IllegalArgumentException(
                            "View with ID $scrollViewId must be a NestedScrollView or ScrollView"
                        )
                }

            totalHeight = child?.height ?: 0
            viewportHeight = scrollView.height
        }

        // Handle empty or zero-height views
        if (viewportHeight <= 0 || totalHeight <= viewportHeight) {
            onView(isRoot()).captureRoboImage(screenshotNameFunction(0))
            return 1
        }

        val stepPixels = (viewportHeight * stepRatio).toInt().coerceAtLeast(1)
        val maxScrollY = totalHeight - viewportHeight

        var currentY = 0
        var stepIndex = 0

        while (currentY < maxScrollY) {
            val scrollY = currentY
            onActivity { activity ->
                activity.findViewById<View>(scrollViewId)?.scrollTo(0, scrollY)
            }

            onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))

            currentY += stepPixels
            stepIndex++
        }

        // Capture the final bottom state if not reached exactly
        if ((currentY - stepPixels) < maxScrollY) {
            onActivity { activity ->
                activity.findViewById<View>(scrollViewId)?.scrollTo(0, maxScrollY)
            }
            onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))
        }
        return stepIndex + 1
    }

    private fun ActivityScenario<*>.scrollAndScreenshotRecyclerView(
        @IdRes recyclerViewId: Int = R.id.MainListView,
        screenshotNameFunction: (Int) -> String,
        step: Int = 4,
    ): Int {
        var totalItems = 0
        var screenshotCounter = 0

        onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(recyclerViewId)
            totalItems = recyclerView.adapter?.itemCount ?: 0
        }

        // Process positions
        val positionsToCapture = (0 until totalItems step step).toMutableList()
        val lastIndex = totalItems - 1
        if (lastIndex > 0 && lastIndex % step != 0) {
            positionsToCapture.add(lastIndex)
        }

        for (position in positionsToCapture) {
            onActivity { activity ->
                val recyclerView = activity.findViewById<RecyclerView>(recyclerViewId)
                (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                    position,
                    0,
                )
            }
            ShadowLooper.idleMainLooper()
            onView(isRoot()).captureRoboImage(screenshotNameFunction(screenshotCounter++))
        }

        return screenshotCounter
    }
}
