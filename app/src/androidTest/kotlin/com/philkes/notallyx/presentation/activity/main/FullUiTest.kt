package com.philkes.notallyx.presentation.activity.main

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.test.assertLogAppeared
import com.philkes.notallyx.test.assertToastDisplayed
import com.philkes.notallyx.test.assertWorkExecuted
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.childAtPosition
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onLabelItem
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.utils.backup.AUTO_BACKUP_WORK_NAME
import com.philkes.notallyx.utils.backup.ON_SAVE_BACKUP_FILE
import com.philkes.notallyx.utils.backup.PERIODIC_BACKUP_FILE_PREFIX
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import com.philkes.notallyx.utils.getUriForFile
import com.philkes.notallyx.utils.listZipFiles
import java.io.File
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end UI test suite. The individual test methods are logically separated. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class FullUiTest {

    private var intentsInitialized = false

    private val context
        get() = ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext)

    private val database: NotallyDatabase
        get() = NotallyDatabase.getDatabase(context).value!!

    private val preferences: NotallyXPreferences
        get() = NotallyXPreferences.getInstance(ContextWrapper(context))

    private val toolbarBackButton: ViewInteraction
        get() =
            onDisplayView(
                childAtPosition(
                    allOf(
                        withId(R.id.Toolbar),
                        childAtPosition(withId(R.id.main_content_layout), 0),
                    ),
                    0,
                )
            )

    @Before
    fun setup() {
        val packageName = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            uiAutomation.grantRuntimePermission(packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            // Allows your app to set exact alarms without forcing the user to Settings
            device.executeShellCommand("appops set $packageName SCHEDULE_EXACT_ALARM allow")
        }
        preferences.backupsFolder.save(EMPTY_PATH)
        preferences.periodicBackups.save(PeriodicBackup(0, 0))
        preferences.backupOnSave.save(false)
    }

    @After
    fun tearDown() {
        if (intentsInitialized) {
            try {
                Intents.release()
            } finally {
                intentsInitialized = false
            }
        }
    }

    private fun initIntents() {
        Intents.init()
        intentsInitialized = true
    }

    /** Create a text note, pin it, change its color, attach a label and toggle read-only/edit. */
    @Test
    fun createAndEditTextNote() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.TakeNote.byId().perform(click())

        R.id.EnterBody.byId().perform(typeText("Body"), closeSoftKeyboard())
        R.id.EnterTitle.byId().perform(typeText("Test"), closeSoftKeyboard())

        R.string.pin.byContentDescription().perform(click())

        R.string.tap_for_more_options.byContentDescription().perform(click())
        R.string.change_color.byText().perform(click())
        R.id.CardView.byId(withContentDescription("NEW")).perform(click())
        R.id.CardView.byId(withContentDescription("#FAAFA9")).perform(click())
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
        R.id.MainListView.byId()
            .onPositionView(1, withId(R.id.Title))
            .check(matches(withText("Test")))

        scenario.close()
    }

    /** Multi-select notes to unpin them and apply an existing label to a note. */
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
                            items = listOf(createListItem("A"), createListItem("B", isChild = true)),
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

        R.string.save.byText(ViewMatchers.withId(android.R.id.button1)).perform(scrollTo(), click())

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

    /** Navigate to the Labels screen to rename, add, reorder and delete labels. */
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
                            items = listOf(createListItem("A"), createListItem("B", isChild = true)),
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
        //        dragAndDrop(By.text("label2"), By.text("label1"))
        //        R.id.MainListView.byId().checkPositionHasText(1, "label2")
        R.id.MainListView.byId().onPositionView(0, withId(R.id.DeleteButton)).perform(click())

        R.string.delete.byText(withId(android.R.id.button1)).perform(scrollTo(), click())

        scenario.close()
    }

    /** Delete a note to the trash and permanently delete all notes from the Deleted screen. */
    @Test
    fun deleteNotes() {
        // 1. Insert data
        runBlocking {
            val db = database
            db.getLabelDao().insert(listOf(Label("label", 0)))
            db.getBaseNoteDao()
                .insert(
                    listOf(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
                )
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        R.string.delete.byContentDescription().perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Deleted)

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test")))

        R.string.delete_all.byContentDescription().perform(click())

        val confirmDeleteAllButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.delete)))
        confirmDeleteAllButton.perform(scrollTo(), click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        scenario.close()
    }

    /** Archive a note, verify it in Archived, unarchive it and verify it is back in Notes. */
    @Test
    fun archiveNotes() {
        // 1. Insert data
        runBlocking {
            database.getBaseNoteDao().insert(listOf(createBaseNote(title = "Test", body = "Body")))
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))

        openActionBarOverflowOrOptionsMenu(context)
        onDisplayView(withText(R.string.archive)).inRoot(isPlatformPopup()).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Archived)

        R.id.MainListView.byId()
            .onPositionView(1, withId(R.id.Title))
            .check(matches(withText("Test")))

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(1, longClick()))
        R.string.unarchive.byContentDescription().perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Notes)

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test")))

        scenario.close()
    }

    /** Navigate to the Reminders screen and toggle its filter chips. */
    @Test
    fun remindersFilter() {
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
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Reminders)

        R.id.elapsed.byId().perform(click())
        // Only the elapsed reminder note is shown
        onView(withText("ElapsedNote")).check(matches(isDisplayed()))
        onView(withText("FutureNote")).check(doesNotExist())

        R.id.upcoming.byId().perform(click())
        // Only the elapsed reminder note is shown
        onView(withText("ElapsedNote")).check(doesNotExist())
        onView(withText("FutureNote")).check(matches(isDisplayed()))

        R.id.all.byId().perform(click())
        // All reminder notes are shown
        onView(withText("ElapsedNote")).check(matches(isDisplayed()))
        onView(withText("FutureNote")).check(matches(isDisplayed()))

        scenario.close()
    }

    /**
     * Navigate to the Settings screen and change view, search, sort order and security settings.
     */
    @Test
    fun settings() {
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        navigateTo(R.id.Settings)

        onView(withId(R.id.View)).perform(scrollTo(), click())
        val viewSettingOption =
            onData(anything())
                .inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
                .atPosition(1)
        viewSettingOption.perform(click())

        onView(withId(R.id.ShowSearchInTopBar)).perform(scrollTo(), click())
        val showSearchEnabledOption =
            onDisplayView(allOf(withId(R.id.EnabledButton), withText(R.string.enabled)))
        showSearchEnabledOption.perform(click())

        onView(withId(R.id.NotesSortOrder)).perform(scrollTo(), click())
        onDisplayView(withText(R.string.ascending)).perform(click())
        val saveSortOrderButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.save)))
        saveSortOrderButton.perform(scrollTo(), click())

        onView(withId(R.id.BackupPassword)).perform(scrollTo(), click())
        val backupPasswordInput =
            onDisplayView(allOf(withId(R.id.InputText), withContentDescription("Input")))
        backupPasswordInput.perform(replaceText("1234"), closeSoftKeyboard())
        val saveBackupPasswordButton =
            onDisplayView(allOf(withId(android.R.id.button1), withText(R.string.save)))
        saveBackupPasswordButton.perform(scrollTo(), click())

        onView(withId(R.id.SecureFlag)).perform(scrollTo(), click())
        R.id.EnabledButton.byId().perform(click())

        onView(withId(R.id.SecureFlag)).perform(scrollTo(), click())
        R.id.DisabledButton.byId().perform(click())

        onView(withId(R.id.DataInPublicFolder)).perform(scrollTo(), click())
        R.id.EnabledButton.byId().perform(click())

        navigateTo(R.id.Notes)

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test")))
            .perform(click())
        R.id.EnterBody.byId().check(matches(withText("Body")))
        R.id.EnterTitle.byId().perform(typeText(" Foo"), closeSoftKeyboard())

        toolbarBackButton.perform(click())

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test Foo")))

        navigateTo(R.id.Settings)

        onView(withId(R.id.DataInPublicFolder)).perform(scrollTo(), click())
        R.id.DisabledButton.byId().perform(click())

        navigateTo(R.id.Notes)

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test Foo")))
            .perform(click())
        R.id.EnterBody.byId().check(matches(withText("Body")))
        R.id.EnterTitle.byId().perform(typeText(" Bar"), closeSoftKeyboard())

        toolbarBackButton.perform(click())

        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test Foo Bar")))

        scenario.close()
    }

    @Test
    fun periodicBackupCreatedAndImport() {
        initIntents()
        val backupPath = context.getExternalBackupsDirectory().toUri()
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
            preferences.backupsFolder.save(backupPath.toString())
            preferences.periodicBackups.save(PeriodicBackup(1, 1))
            preferences.backupOnSave.save(false)
        }
        intending(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_OPEN_DOCUMENT)),
                )
            )
            .respondWithFunction { intent ->
                val periodicBackup =
                    DocumentFile.fromFile(File(backupPath.path!!))
                        .listZipFiles(PERIODIC_BACKUP_FILE_PREFIX)
                        .firstOrNull()
                        ?: throw IllegalStateException(
                            "No periodic backup zip found in $backupPath"
                        )
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        data = context.getUriForFile(File(periodicBackup.uri.path!!))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertWorkExecuted(AUTO_BACKUP_WORK_NAME)

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        R.string.tap_for_more_options.byContentDescription().perform(click())
        R.string.delete_forever.byText().perform(click())
        R.string.delete.byText(withId(android.R.id.button1)).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Settings)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(scrollTo())
        SystemClock.sleep(3000)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(click())
        R.string.import_backup.byText(withId(android.R.id.button1)).perform(click())
        assertToastDisplayed("Imported 1 Note")

        navigateTo(R.id.Notes)
        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test")))

        scenario.close()
    }

    @Test
    fun autoSaveBackupCreatedAndImport() {
        initIntents()
        val backupPath = context.getExternalBackupsDirectory().toUri()
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Test", body = "Body", labels = listOf("label")))
            preferences.backupsFolder.save(backupPath.toString())
            preferences.periodicBackups.save(PeriodicBackup(0, 0))
            preferences.backupOnSave.save(true)
        }
        intending(
                allOf(
                    hasAction(Intent.ACTION_CHOOSER),
                    hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_OPEN_DOCUMENT)),
                )
            )
            .respondWithFunction { intent ->
                val periodicBackup =
                    DocumentFile.fromFile(File(backupPath.path!!))
                        .listZipFiles(ON_SAVE_BACKUP_FILE)
                        .firstOrNull()
                        ?: throw IllegalStateException(
                            "No auto save backup zip found in $backupPath"
                        )
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        data = context.getUriForFile(File(periodicBackup.uri.path!!))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        R.id.EnterTitle.byId().perform(typeText(" Foo"))
        R.id.EnterBody.byId().perform(typeText(" Foo"), closeSoftKeyboard())
        Espresso.pressBack()
        assertLogAppeared("ExportExtensions", "Finished full backup")

        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
        R.string.delete.byContentDescription().perform(click())
        onView(withText("Test Foo")).check(doesNotExist())
        navigateTo(R.id.Deleted)
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
        R.string.delete_forever.byContentDescription().perform(click())
        R.string.delete.byText(withId(android.R.id.button1)).perform(click())
        "Test".byText(checkDisplayed = false).check(doesNotExist())

        navigateTo(R.id.Settings)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(scrollTo())
        SystemClock.sleep(3000)
        R.id.ImportBackup.byId(checkDisplayed = false).perform(click())
        R.string.import_backup.byText(withId(android.R.id.button1)).perform(click())
        assertToastDisplayed("Imported 1 Note")

        navigateTo(R.id.Notes)
        R.id.MainListView.byId()
            .onPositionView(0, withId(R.id.Title))
            .check(matches(withText("Test Foo")))

        scenario.close()
    }
}
