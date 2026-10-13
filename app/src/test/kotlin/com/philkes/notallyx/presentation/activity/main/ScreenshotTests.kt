package com.philkes.notallyx.presentation.activity.main

import android.content.Intent
import android.os.SystemClock
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
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.philkes.notallyx.R
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.activity.main.MainActivity.Companion.EXTRA_FRAGMENT_TO_OPEN
import com.philkes.notallyx.presentation.activity.note.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.presentation.activity.note.EditNoteActivity
import com.philkes.notallyx.test.FakeAndroidKeyStore
import com.philkes.notallyx.test.context
import com.philkes.notallyx.test.database
import com.philkes.notallyx.test.generateNotes
import com.philkes.notallyx.test.preferences
import com.philkes.notallyx.test.toMillis
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import io.mockk.clearStaticMockk
import java.io.File
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

object SampleNotes {
    val LOREM_IPSUM_NOTE =
        BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Lorem Ipsum Note",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = emptyList(),
            body =
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val LIST_NOTE =
        BaseNote(
            id = 0L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = Color.CLAY.toColorString(),
            title = "Shopping List",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = emptyList(),
            body = "",
            spans = emptyList(),
            items =
                listOf(
                    ListItem(
                        "Buy groceries",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Clean the house",
                        checked = true,
                        isChild = false,
                        order = 1,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Call mom",
                        checked = false,
                        isChild = false,
                        order = 2,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Finish project draft",
                        checked = false,
                        isChild = false,
                        order = 3,
                        children = mutableListOf(),
                    ),
                ),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val RICH_TEXT_NOTE =
        BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Note with Everything",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = listOf("Work", "Personal", "Important", "Ideas", "Project"),
            body =
                "This note contains multiple attachments, labels, and a reminder to demonstrate full UI card features.",
            spans = emptyList(),
            items = emptyList(),
            images = listOf(FileAttachment("cat.jpg", "cat.jpg", "image/jpeg")),
            files =
                listOf(
                    FileAttachment("file1.pdf", "Specification.pdf", "application/pdf"),
                    FileAttachment("file2.zip", "Project_Archive.zip", "application/zip"),
                ),
            audios = listOf(Audio("audio1.aac", 60000L, 1767355200000L)),
            reminders =
                listOf(Reminder(id = 1L, dateTime = Date(1767441600000L), repetition = null)),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
}

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
                                    changeThreshold = 0.05f, // For 1% accepted difference
                                    imageComparator =
                                        SimpleImageComparator(
                                            maxDistance = 0.007F // 0.001F is
                                        ),
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
        runBlocking {
            database.getBaseNoteDao().deleteFrom(Folder.NOTES)
            database.getBaseNoteDao().deleteFrom(Folder.DELETED)
            database.getBaseNoteDao().deleteFrom(Folder.ARCHIVED)
        }
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
            val scrollCount =
                scenario.scrollAndScreenshotScrollView(
                    R.id.NestedScrollView,
                    {
                        "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(it)}_settings_scroll.png"
                    },
                )

            // 1. Click Date Time Format Note View setting and capture dialog screenshot
            onView(withId(R.id.DateFormatNoteView)).perform(scrollTo(), click())
            ShadowLooper.idleMainLooper()
            onView(isRoot())
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(scrollCount)}_dialog_date_time_format.png"
                )
            onView(withId(android.R.id.button2))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .perform(click())
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
                drawerLayout?.openDrawer(GravityCompat.START, false)
            }
            ShadowLooper.idleMainLooper()
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/navigationDrawer/00_initial.png"
                )
        }
    }

    @Test
    fun reminders() {
        SystemClock.setCurrentTimeMillis(fixedTime.time)
        // 1. Insert notes with reminders: one elapsed (past, no repetition), one upcoming (future)
        runBlocking {
            val now = fixedTime.time
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
            ShadowLooper.idleMainLooper()
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/reminders/00_initial.png"
                )
        }
    }

    @Test
    fun notesOverview() = runTest {
        // Copy cat.jpg attachment to context.getCurrentImagesDirectory() for image preview
        val mediaDir = context.getCurrentImagesDirectory().apply { if (!exists()) mkdirs() }
        val catFile = File(mediaDir, "cat.jpg")
        val catInput =
            ScreenshotTests::class.java.classLoader?.getResourceAsStream("attachments/cat.jpg")
                ?: File("src/androidTest/resources/attachments/cat.jpg")
                    .takeIf { it.exists() }
                    ?.inputStream()
                ?: File("app/src/androidTest/resources/attachments/cat.jpg")
                    .takeIf { it.exists() }
                    ?.inputStream()

        catInput?.use { input -> catFile.outputStream().use { output -> input.copyTo(output) } }

        val notes =
            listOf(
                SampleNotes.LOREM_IPSUM_NOTE,
                SampleNotes.LIST_NOTE,
                SampleNotes.RICH_TEXT_NOTE,
                generateNotes(1, fixedTime).first(),
            )

        val insertedIds = withContext(Dispatchers.IO) { database.getBaseNoteDao().insert(notes) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()

            // 1. Capture the entire list view
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/notesOverview/00_notes_overview_list.png"
                )

            // 2. Capture each individual note card
            insertedIds.forEachIndexed { index, noteId ->
                scenario.onActivity { activity ->
                    val recyclerView = activity.findViewById<RecyclerView>(R.id.MainListView)
                    (recyclerView?.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(index, 0)
                }
                ShadowLooper.idleMainLooper()

                scenario.onActivity { activity ->
                    val recyclerView = activity.findViewById<RecyclerView>(R.id.MainListView)
                    val cardView = recyclerView?.findViewHolderForAdapterPosition(index)?.itemView
                    cardView?.captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/notesOverview/${"%02d".format(index + 1)}_card_${"%02d".format(index + 1)}.png"
                    )
                }
            }
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
                    ?: activity.findViewById<View>(android.R.id.content)

            val child =
                when (scrollView) {
                    is NestedScrollView -> scrollView.getChildAt(0)
                    is android.widget.ScrollView -> scrollView.getChildAt(0)
                    is android.view.ViewGroup ->
                        if (scrollView.childCount > 0) scrollView.getChildAt(0) else null
                    else -> null
                }

            totalHeight = child?.height ?: 0
            viewportHeight = scrollView?.height ?: 0
        }

        ShadowLooper.idleMainLooper()

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
            ShadowLooper.idleMainLooper()

            onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))

            currentY += stepPixels
            stepIndex++
        }

        // Capture the final bottom state if not reached exactly
        if ((currentY - stepPixels) < maxScrollY) {
            onActivity { activity ->
                activity.findViewById<View>(scrollViewId)?.scrollTo(0, maxScrollY)
            }
            ShadowLooper.idleMainLooper()
            onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))
            stepIndex++
        }
        return stepIndex
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
            totalItems = recyclerView?.adapter?.itemCount ?: 0
        }

        if (totalItems == 0) {
            ShadowLooper.idleMainLooper()
            onView(isRoot()).captureRoboImage(screenshotNameFunction(0))
            return 1
        }

        val positionsToCapture = (0 until totalItems step step).toMutableList()
        val lastIndex = totalItems - 1
        if (lastIndex > 0 && lastIndex % step != 0) {
            positionsToCapture.add(lastIndex)
        }

        for (position in positionsToCapture) {
            onActivity { activity ->
                val recyclerView = activity.findViewById<RecyclerView>(recyclerViewId)
                (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
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
