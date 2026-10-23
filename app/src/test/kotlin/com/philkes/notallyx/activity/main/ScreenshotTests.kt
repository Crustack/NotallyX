package com.philkes.notallyx.activity.main

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.FakeAndroidKeyStore
import com.philkes.notallyx.test.context
import com.philkes.notallyx.test.database
import com.philkes.notallyx.test.generateNotes
import com.philkes.notallyx.test.preferences
import com.philkes.notallyx.test.toMillis
import com.philkes.notallyx.test.waitUntil
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
            withContext(Dispatchers.IO) {
                database.getBaseNoteDao().deleteFrom(Folder.ARCHIVED)
                database.getBaseNoteDao().deleteFrom(Folder.NOTES)
                database.getBaseNoteDao().deleteFrom(Folder.DELETED)
                database.getLabelDao().deleteAll()
                preferences.reset()
            }
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

            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/00_initial.png"
                )
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .performTextInput("Test Title")
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.note))
                .performTextInput("Test Body")
            ShadowLooper.idleMainLooper()

            composeTestRule
                .onRoot()
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

            composeTestRule
                .onRoot()
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

            composeTestRule
                .onNode(isDialog())
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

            composeTestRule
                .onNode(isDialog())
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

                composeTestRule
                    .onRoot()
                    .captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/editNote/05_added_image.png"
                    )

                composeTestRule
                    .onNodeWithContentDescription(context.getString(R.string.back))
                    .performClick()

                waitUntil(timeoutMs = 10_000) {
                    ShadowLooper.idleMainLooper()
                    runBlocking(Dispatchers.IO) {
                        val savedNote = database.getBaseNoteDao().get(noteId)
                        savedNote?.images?.size == 2 && savedNote.title.contains("Test Title")
                    }
                }
                ShadowLooper.idleMainLooper()
            } finally {
                Intents.release()
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            ShadowLooper.idleMainLooper()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(hasText("Test Title", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            ShadowLooper.idleMainLooper()

            composeTestRule
                .onNode(hasText("Test Title", substring = true))
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/editNote/06_updated_card.png"
                )
        }
    }

    @Test
    fun settings() {
        val intent = Intent(context, MainActivity::class.java).putNavKey(NotallyNavKey.Settings)
        ActivityScenario.launch<MainActivity>(intent).use {
            ShadowLooper.idleMainLooper()

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasText(context.getString(R.string.date_format_note_view), substring = true)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/00_initial.png"
                )

            var imageCount = 1

            // 1. Click Date Time Format Note View setting and capture dialog screenshot
            composeTestRule
                .onNodeWithText(context.getString(R.string.date_format_note_view), substring = true)
                .performClick()
            composeTestRule.mainClock.advanceTimeByFrame()
            ShadowLooper.idleMainLooper()
            composeTestRule
                .onNode(isDialog())
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_dialog_date_time_format.png"
                )

            composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()

            // 2. Scroll to top and touch down on Text Size slider to capture screenshot while
            // dragging
            composeTestRule
                .onNodeWithText(context.getString(R.string.text_size_note_editor))
                .performScrollTo()
            val textSizeSliderThumb =
                composeTestRule.onNode(
                    matcher =
                        hasTestTag(
                            "${TestTags.THUMB}_${context.getString(R.string.text_size_note_editor)}"
                        ),
                    useUnmergedTree = true,
                )
            textSizeSliderThumb.performTouchInput {
                down(center)
                moveBy(Offset(200f, 0f))
            }
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_slider_dragging.png"
                )
            textSizeSliderThumb.performTouchInput { up() }
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText(context.getString(R.string.start_view), substring = true)
                .performScrollTo()
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_start_view_scroll.png"
                )

            composeTestRule
                .onNodeWithText(
                    context.getString(R.string.labels_hidden_in_overview_title),
                    substring = true,
                )
                .performScrollTo()
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_hide_labels_scroll.png"
                )

            composeTestRule
                .onNodeWithText(context.getString(R.string.auto_backup_on_save), substring = true)
                .performScrollTo()
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_backup_on_save_scroll.png"
                )

            composeTestRule
                .onNodeWithText(context.getString(R.string.disallow_screenshots), substring = true)
                .performScrollTo()
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_disallow_screenshots_scroll.png"
                )

            composeTestRule
                .onNodeWithText(context.getString(R.string.send_feedback), substring = true)
                .performScrollTo()
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/settings/${"%02d".format(imageCount++)}_send_feedback_scroll.png"
                )
        }
    }

    @Test
    fun navigationDrawer() = runTest {
        withContext(Dispatchers.IO) {
            database.getLabelDao().apply { insert((1..5).map { Label("Label$it", it) }) }
            preferences.labelsHidden.save(setOf("Label1", "Label4"))
        }
        val intent = Intent(context, MainActivity::class.java).putNavKey(NotallyNavKey.Labels)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.app_name))
                .performClick()
            ShadowLooper.idleMainLooper()
            composeTestRule
                .onRoot()
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
    //                putExtra(EXTRA_DESTINATION_TO_OPEN, NotallyNavKey.Labels.toString())
    //            }
    //        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
    //            composeTestRule.onRoot()
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
    //            composeTestRule.onRoot()
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
    //            composeTestRule.onRoot()
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
    //            composeTestRule.onRoot()
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
    //            composeTestRule.onRoot()
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
        val intent = Intent(context, MainActivity::class.java).putNavKey(NotallyNavKey.Reminders)
        ActivityScenario.launch<MainActivity>(intent).use {
            ShadowLooper.idleMainLooper()
            composeTestRule
                .onRoot()
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

        withContext(Dispatchers.IO) { database.getBaseNoteDao().insert(notes) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()

            composeTestRule.waitUntil(10_000) {
                notes.any { note ->
                    composeTestRule
                        .onAllNodes(hasText(note.title))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            // 1. Capture the entire list view
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/notesOverview/00_notes_overview_list.png"
                )

            // 2. Capture each individual note card
            notes.forEachIndexed { index, note ->
                val noteIndex = "%02d".format(index + 1)
                composeTestRule
                    .onNode(hasScrollToNodeAction())
                    .performScrollToNode(hasText(note.title))
                composeTestRule.waitForIdle()
                ShadowLooper.idleMainLooper()
                composeTestRule
                    .onNode(hasText(note.title))
                    .captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/notesOverview/${noteIndex}_card_$noteIndex.png"
                    )
            }
        }
    }

    @Test
    fun notesOverviewGrid() = runTest {
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

        withContext(Dispatchers.IO) {
            preferences.notesView.save(NotesView.GRID)
            database.getBaseNoteDao().insert(notes)
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()

            composeTestRule.waitUntil(10_000) {
                notes.all { note ->
                    composeTestRule
                        .onAllNodes(hasText(note.title))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            // 1. Capture the entire grid view
            composeTestRule
                .onRoot()
                .captureRoboImage(
                    "${ScreenshotTests::class.java.simpleName}/notesOverviewGrid/00_notes_overview_grid.png"
                )

            // 2. Capture each individual note card
            notes.forEachIndexed { index, note ->
                val noteIndex = "%02d".format(index + 1)
                composeTestRule
                    .onNode(hasText(note.title))
                    .captureRoboImage(
                        "${ScreenshotTests::class.java.simpleName}/notesOverviewGrid/${noteIndex}_card_$noteIndex.png"
                    )
            }
        }
    }
}
