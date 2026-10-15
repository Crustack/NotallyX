package com.philkes.notallyx.presentation.activity.main

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IdRes
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.philkes.notallyx.R
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.presentation.activity.main.MainActivity.Companion.EXTRA_FRAGMENT_TO_OPEN
import com.philkes.notallyx.presentation.activity.main.fragment.settings.SettingsFragment
import com.philkes.notallyx.presentation.activity.note.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.presentation.activity.note.EditNoteActivity
import com.philkes.notallyx.presentation.view.components.SampleNotes
import com.philkes.notallyx.test.FakeAndroidKeyStore
import com.philkes.notallyx.test.context
import com.philkes.notallyx.test.database
import com.philkes.notallyx.test.generateNotes
import com.philkes.notallyx.test.preferences
import com.philkes.notallyx.test.toMillis
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import io.mockk.clearStaticMockk
import java.io.File
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hamcrest.Matchers.allOf
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
                                    changeThreshold = 0.05f, // For 1% accepted difference
                                    imageComparator =
                                        SimpleImageComparator(
                                            maxDistance = 0.007F // 0.001F is
                                        ),
                                )
                        )
                )
        )

    @get:Rule val composeTestRule = createEmptyComposeRule()

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
            database.getBaseNoteDao().deleteFrom(Folder.ARCHIVED)
            database.getBaseNoteDao().deleteFrom(Folder.NOTES)
            database.getBaseNoteDao().deleteFrom(Folder.DELETED)
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
            waitUntil(timeoutMs = 10_000) {
                ShadowLooper.idleMainLooper()
                var isLoaded = false
                scenario.onActivity { activity ->
                    isLoaded = activity.notallyModel.originalNote != null
                }
                isLoaded
            }
            ShadowLooper.idleMainLooper()

            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/00_initial.png"
                )
            composeTestRule.onNodeWithTag("EnterTitle").performTextInput("Test Title")
            composeTestRule.onNodeWithTag("EnterBody").performTextInput("Test Body")
            ShadowLooper.idleMainLooper()

            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/01_title_and_body.png"
                )

            // Open formatting menu and check bold/italic options
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.edit))
                .performClick()
            ShadowLooper.idleMainLooper()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.bold))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.italic))
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.bold))
                .performClick()
            ShadowLooper.idleMainLooper()

            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/02_text_formatting.png"
                )

            // Close text formatting menu and open lower right more menu
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.cancel))
                .performClick()
            ShadowLooper.idleMainLooper()

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.tap_for_more_options))
                .performClick()
            ShadowLooper.idleMainLooper()

            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/03_more_options.png"
                )

            Espresso.pressBack()
            ShadowLooper.idleMainLooper()

            // Open bottom left (+) menu and capture screenshot
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.add_item))
                .performClick()
            ShadowLooper.idleMainLooper()

            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/04_add_menu.png"
                )

            // Prepare cat.jpg and dog.jpg attachments
            val catFile = File(context.cacheDir, "cat.jpg")
            val dogFile = File(context.cacheDir, "dog.jpg")

            val catInput =
                ScreenshotTests::class.java.classLoader?.getResourceAsStream("attachments/cat.jpg")
                    ?: File("src/androidTest/resources/attachments/cat.jpg")
                        .takeIf { it.exists() }
                        ?.inputStream()
                    ?: File("app/src/androidTest/resources/attachments/cat.jpg")
                        .takeIf { it.exists() }
                        ?.inputStream()

            val dogInput =
                ScreenshotTests::class.java.classLoader?.getResourceAsStream("attachments/dog.jpg")
                    ?: File("src/androidTest/resources/attachments/dog.jpg")
                        .takeIf { it.exists() }
                        ?.inputStream()
                    ?: File("app/src/androidTest/resources/attachments/dog.jpg")
                        .takeIf { it.exists() }
                        ?.inputStream()

            catInput?.use { input -> catFile.outputStream().use { output -> input.copyTo(output) } }
            dogInput?.use { input -> dogFile.outputStream().use { output -> input.copyTo(output) } }

            val catUri = Uri.fromFile(catFile)
            val dogUri = Uri.fromFile(dogFile)

            Intents.init()
            try {
                val addImagesClipData =
                    ClipData.newUri(context.contentResolver, "image", catUri).apply {
                        addItem(ClipData.Item(dogUri))
                    }
                val addImagesResultIntent = Intent().apply { this.clipData = addImagesClipData }
                val addImagesResult =
                    Instrumentation.ActivityResult(Activity.RESULT_OK, addImagesResultIntent)

                intending(
                        allOf(
                            hasAction(Intent.ACTION_CHOOSER),
                            hasExtra(
                                Intent.EXTRA_INTENT,
                                allOf(hasAction(Intent.ACTION_GET_CONTENT), hasType("image/*")),
                            ),
                        )
                    )
                    .respondWith(addImagesResult)

                composeTestRule
                    .onNodeWithText(context.getString(R.string.add_images))
                    .performClick()

                // 1. Wait for background import on Dispatchers.IO to complete and update model
                // images
                waitUntil(timeoutMs = 10_000) {
                    ShadowLooper.idleMainLooper()
                    var isImported = false
                    scenario.onActivity { activity ->
                        isImported = activity.notallyModel.images.value.size >= 2
                    }
                    isImported
                }

                ShadowLooper.idleMainLooper()

                onView(isRoot())
                    .captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/editNote/05_added_image.png"
                    )
            } finally {
                Intents.release()
            }
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
            scenario.onActivity { activity ->
                val navHost =
                    (activity as? androidx.appcompat.app.AppCompatActivity)
                        ?.supportFragmentManager
                        ?.findFragmentById(R.id.NavHostFragment)
                val settingsFragment =
                    navHost?.childFragmentManager?.fragments?.firstOrNull() as? SettingsFragment
                settingsFragment?.showDateTimeFormatNoteViewDialog()
            }
            ShadowLooper.idleMainLooper()
            onView(isRoot())
                .inRoot(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(scrollCount)}_dialog_date_time_format.png"
                )
            Espresso.pressBack()
            ShadowLooper.idleMainLooper()

            // 2. Scroll to top and touch down on Text Size slider to capture screenshot while
            // dragging
            scenario.onActivity { activity ->
                val navHost =
                    (activity as? androidx.appcompat.app.AppCompatActivity)
                        ?.supportFragmentManager
                        ?.findFragmentById(R.id.NavHostFragment)
                val settingsFragment =
                    navHost?.childFragmentManager?.fragments?.firstOrNull() as? SettingsFragment
                runBlocking { settingsFragment?.scrollState?.scrollTo(0) }
                val composeView = settingsFragment?.view as? ComposeView
                if (composeView != null) {
                    val x = composeView.width / 2f
                    val y = 380f // Position of TextSize slider in top section
                    val downTime = SystemClock.uptimeMillis()
                    val event =
                        MotionEvent.obtain(
                            downTime,
                            downTime,
                            MotionEvent.ACTION_DOWN,
                            x,
                            y,
                            0,
                        )
                    composeView.dispatchTouchEvent(event)
                }
            }
            ShadowLooper.idleMainLooper()
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(scrollCount + 1)}_slider_dragging.png"
                )
            scenario.onActivity { activity ->
                val navHost =
                    (activity as? androidx.appcompat.app.AppCompatActivity)
                        ?.supportFragmentManager
                        ?.findFragmentById(R.id.NavHostFragment)
                val settingsFragment =
                    navHost?.childFragmentManager?.fragments?.firstOrNull() as? SettingsFragment
                val composeView = settingsFragment?.view as? ComposeView
                if (composeView != null) {
                    val x = composeView.width / 2f
                    val y = 380f
                    val downTime = SystemClock.uptimeMillis()
                    val event =
                        MotionEvent.obtain(
                            downTime,
                            downTime + 100,
                            MotionEvent.ACTION_UP,
                            x,
                            y,
                            0,
                        )
                    composeView.dispatchTouchEvent(event)
                }
            }
            ShadowLooper.idleMainLooper()
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

    //
    //    @Test
    //    fun labels() = runTest {
    //        withContext(Dispatchers.IO) {
    //            database.getLabelDao().apply { insert((1..5).map { Label("Label$it", it) }) }
    //            preferences.labelsHidden.save(setOf("Label1", "Label4"))
    //        }
    //        val intent =
    //            Intent(context, MainActivity::class.java).apply {
    //                putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.Labels)
    //            }
    //        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
    //            onView(isRoot())
    //
    // .captureRoboImage("${ScreenshotTests::class.java.simpleName}/labels/00_initial.png")
    //
    ////            waitUntilSucceeds {
    ////                "Label1".byText().check(matches(isDisplayed()))
    ////            }
    //            // Edit Label
    //            R.id.MainListView.byId().onPositionView(1,
    // withId(R.id.EditButton)).perform(click())
    //            R.id.EditText.byId()
    //                .inRoot(isDialog())
    //                .perform(replaceText(""), typeText("Label Foo"), closeSoftKeyboard())
    //            onView(isRoot())
    //                .inRoot(isDialog())
    //                .captureRoboImage(
    //
    // "${ScreenshotTests::class.java.simpleName}/labels/01_edit_label_dialog.png"
    //                )
    //            R.string.save
    //                .byText(withId(android.R.id.button1))
    //                .inRoot(isDialog())
    //                .perform(scrollTo(), click())
    //            waitUntilSucceeds {
    //                R.id.MainListView.byId()
    //                    .onPositionView(1, withId(R.id.LabelText))
    //                    .check(matches(withText("Label Foo")))
    //            }
    //            onView(isRoot())
    //                .captureRoboImage(
    //                    "${ScreenshotTests::class.java.simpleName}/labels/02_edit_label_saved.png"
    //                )
    //
    //            // Toggle Visibilities
    //            R.id.MainListView.byId()
    //                .onPositionView(1, withId(R.id.VisibilityButton))
    //                .perform(click())
    //            R.id.MainListView.byId()
    //                .onPositionView(0, withId(R.id.VisibilityButton))
    //                .perform(click())
    //            onView(isRoot())
    //                .captureRoboImage(
    //
    // "${ScreenshotTests::class.java.simpleName}/labels/03_label_visibility_toggled.png"
    //                )
    //
    //            // Delete Label
    //            R.id.MainListView.byId().onPositionView(2,
    // withId(R.id.DeleteButton)).perform(click())
    //            onView(isRoot())
    //                .inRoot(isDialog())
    //                .captureRoboImage(
    //
    // "${ScreenshotTests::class.java.simpleName}/labels/04_delete_label_dialog.png"
    //                )
    //            R.string.delete
    //                .byText(withId(android.R.id.button1))
    //                .inRoot(isDialog())
    //                .perform(scrollTo(), click())
    //            waitUntilSucceeds {
    //                R.id.MainListView.byId()
    //                    .onPositionView(2, withId(R.id.LabelText))
    //                    .check(matches(withText("Label2")))
    //            }
    //            onView(isRoot())
    //                .captureRoboImage(
    //                    "${ScreenshotTests::class.java.simpleName}/labels/05_deleted_label.png"
    //                )
    //
    //            // Add Label
    //            R.string.add_label.byContentDescription().perform(click())
    //            R.id.EditText.byId()
    //                .inRoot(isDialog())
    //                .perform(typeText("Label New"), closeSoftKeyboard())
    //            onView(isRoot())
    //                .inRoot(isDialog())
    //                .captureRoboImage(
    //                    "${ScreenshotTests::class.java.simpleName}/labels/06_new_label_dialog.png"
    //                )
    //            R.string.save
    //                .byText(withId(android.R.id.button1))
    //                .inRoot(isDialog())
    //                .perform(scrollTo(), click())
    //            waitUntilSucceeds {
    //                R.id.MainListView.byId()
    //                    .onPositionView(0, withId(R.id.LabelText))
    //                    .check(matches(withText("Label New")))
    //            }
    //            onView(isRoot())
    //                .captureRoboImage(
    //                    "${ScreenshotTests::class.java.simpleName}/labels/07_label_added.png"
    //                )
    //        }
    //    }

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
                SampleNotes.GENERATED_LIST_NOTE,
            )

        val insertedIds = withContext(Dispatchers.IO) { database.getBaseNoteDao().insert(notes) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()

            composeTestRule.waitUntil(10_000) {
                insertedIds.all { noteId ->
                    composeTestRule
                        .onAllNodes(hasTestTag("NoteCard_$noteId"))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            // 1. Capture the entire list view
            onView(isRoot())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/notesOverview/00_notes_overview_list.png"
                )

            // 2. Capture each individual note card
            insertedIds.forEachIndexed { index, noteId ->
                val cardTag = "NoteCard_$noteId"
                val noteIndex = "%02d".format(index + 1)
                composeTestRule
                    .onNodeWithTag(cardTag)
                    .captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/notesOverview/${noteIndex}_card_$noteIndex.png"
                    )
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
        var composeScrollState: ScrollState? = null

        // Measure view dimensions on the main thread
        onActivity { activity ->
            val navHost =
                (activity as? androidx.appcompat.app.AppCompatActivity)
                    ?.supportFragmentManager
                    ?.findFragmentById(R.id.NavHostFragment)
            val activeFragment = navHost?.childFragmentManager?.fragments?.firstOrNull()
            if (activeFragment is SettingsFragment) {
                composeScrollState = activeFragment.scrollState
            }

            val scrollView =
                activity.findViewById<View>(scrollViewId)
                    ?: activity.findViewById<View>(android.R.id.content)

            val state = composeScrollState
            if (state != null) {
                viewportHeight = scrollView?.height ?: 0
                totalHeight = viewportHeight + state.maxValue
            } else {
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
        }

        ShadowLooper.idleMainLooper()
        val scrollState = composeScrollState
        if (scrollState != null) {
            val maxScrollY = scrollState.maxValue
            if (maxScrollY <= 0 || maxScrollY == Int.MAX_VALUE) {
                onView(isRoot()).captureRoboImage(screenshotNameFunction(0))
                return 1
            }

            val stepPixels =
                (if (viewportHeight > 0) (viewportHeight * stepRatio).toInt() else 800)
                    .coerceAtLeast(1)
            var currentY = 0
            var stepIndex = 0

            while (currentY < maxScrollY) {
                val scrollY = currentY
                onActivity { runBlocking { scrollState.scrollTo(scrollY) } }
                ShadowLooper.idleMainLooper()

                onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))

                currentY += stepPixels
                stepIndex++
            }

            if ((currentY - stepPixels) < maxScrollY) {
                onActivity { runBlocking { scrollState.scrollTo(maxScrollY) } }
                ShadowLooper.idleMainLooper()
                onView(isRoot()).captureRoboImage(screenshotNameFunction(stepIndex))
                stepIndex++
            }

            return stepIndex
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
