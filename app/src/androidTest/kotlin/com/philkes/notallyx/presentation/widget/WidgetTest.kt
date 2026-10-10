package com.philkes.notallyx.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.presentation.activity.ConfigureWidgetActivity
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.typeTextAtEnd
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSucceeds
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class WidgetTest : UiTestBase() {

    @Test
    fun addWidgetToHomeScreenAndSelectNote() {
        val noteTitle1 = "Widget Note 1"
        val noteTitle2 = "Widget Note 2"

        val (note1Id, note2Id) =
            runBlocking {
                val id1 =
                    database
                        .getBaseNoteDao()
                        .insert(
                            createBaseNote(title = noteTitle1, body = "Body 1", type = Type.NOTE)
                        )
                val id2 =
                    database
                        .getBaseNoteDao()
                        .insert(
                            createBaseNote(title = noteTitle2, body = "Body 2", type = Type.NOTE)
                        )
                Pair(id1, id2)
            }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, WidgetProvider::class.java)

        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                appWidgetManager.isRequestPinAppWidgetSupported
        ) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val initialWidgetIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()

            // Request pinning the app widget
            appWidgetManager.requestPinAppWidget(myProvider, null, null)

            // Look for system "Add automatically" or "Add to Home screen" or "Add" button
            val addWidgetButton =
                device.findObject(
                    UiSelector().textMatches("(?i).*(add automatically|add to home|add).*")
                )
            if (addWidgetButton.waitForExists(5000)) {
                addWidgetButton.click()
            }

            // Wait for the system launcher to assign a new widget ID
            var newWidgetId: Int? = null
            waitUntil(10000) {
                val currentIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                val diff = currentIds - initialWidgetIds
                if (diff.isNotEmpty()) {
                    newWidgetId = diff.first()
                    true
                } else false
            }

            assertNotNull("New widget ID should be assigned by the system", newWidgetId)
            val id = newWidgetId!!

            // Launch ConfigureWidgetActivity for the newly added widget
            val intent1 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario1 = ActivityScenario.launch<ConfigureWidgetActivity>(intent1)

            // Select the first note
            waitUntilSucceeds(10000) { noteTitle1.byText().perform(click()) }

            // Verify widget preference was saved for Note 1
            waitUntil { preferences.getWidgetData(id) == note1Id }

            assertEquals(note1Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            scenario1.close()

            // Re-open ConfigureWidgetActivity for the same widget to select Note 2
            val intent2 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario2 = ActivityScenario.launch<ConfigureWidgetActivity>(intent2)

            // Select the second note
            waitUntilSucceeds(10000) { noteTitle2.byText().perform(click()) }

            // Verify widget preference was updated to Note 2
            waitUntil { preferences.getWidgetData(id) == note2Id }

            assertEquals(note2Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            scenario2.close()

            // Open MainActivity, click Note 2, edit its title in EditNoteActivity, and exit (saves
            // note and updates widget)
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            waitUntilSucceeds { noteTitle2.byText().perform(click()) }

            val appendedText = " Updated"
            val expectedUpdatedTitle = "$noteTitle2$appendedText"

            R.id.EnterTitle.byId().typeTextAtEnd(appendedText).perform(closeSoftKeyboard())

            // Exit EditNoteActivity to trigger save & widget update
            toolbarBackButton.perform(click())

            // Verify database contains the updated note title
            waitUntil {
                runBlocking {
                    database.getBaseNoteDao().get(note2Id)?.title == expectedUpdatedTitle
                }
            }

            assertEquals(note2Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            mainScenario.close()
            device.pressHome()
        } else {
            // Fallback for environments without pin widget support
            val testWidgetId = 5555
            val intent1 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidgetId)
                }
            val scenario1 = ActivityScenario.launch<ConfigureWidgetActivity>(intent1)

            waitUntilSucceeds { noteTitle1.byText().perform(click()) }

            waitUntil { preferences.getWidgetData(testWidgetId) == note1Id }
            assertEquals(note1Id, preferences.getWidgetData(testWidgetId))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidgetId))

            scenario1.close()

            // Re-open ConfigureWidgetActivity for the same widget to select Note 2
            val intent2 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidgetId)
                }
            val scenario2 = ActivityScenario.launch<ConfigureWidgetActivity>(intent2)

            waitUntilSucceeds { noteTitle2.byText().perform(click()) }

            waitUntil { preferences.getWidgetData(testWidgetId) == note2Id }
            assertEquals(note2Id, preferences.getWidgetData(testWidgetId))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidgetId))

            scenario2.close()

            // Open MainActivity, click Note 2, edit its title in EditNoteActivity, and exit
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            waitUntilSucceeds { noteTitle2.byText().perform(click()) }

            val appendedText = " Updated"
            val expectedUpdatedTitle = "$noteTitle2$appendedText"

            R.id.EnterTitle.byId().typeTextAtEnd(appendedText).perform(closeSoftKeyboard())

            toolbarBackButton.perform(click())

            waitUntil {
                runBlocking {
                    database.getBaseNoteDao().get(note2Id)?.title == expectedUpdatedTitle
                }
            }

            assertEquals(note2Id, preferences.getWidgetData(testWidgetId))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidgetId))

            mainScenario.close()
        }
    }

    @Test
    fun configureWidgetActivity_selectsExistingTextNote_updatesPreferences() {
        val noteTitle = "Text Note For Widget"
        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = noteTitle, body = "Text content", type = Type.NOTE))
        }

        val widgetId = 1234
        val intent =
            Intent(context, ConfigureWidgetActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }

        val scenario = ActivityScenario.launch<ConfigureWidgetActivity>(intent)

        waitUntilSucceeds { noteTitle.byText().perform(click()) }

        waitUntil { preferences.getWidgetData(widgetId) == noteId }
        assertEquals(noteId, preferences.getWidgetData(widgetId))
        assertEquals(Type.NOTE, preferences.getWidgetNoteType(widgetId))

        scenario.close()
    }

    @Test
    fun configureWidgetActivity_selectsExistingListNote_updatesPreferences() {
        val listNoteTitle = "List Note For Widget"
        val listNoteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    createBaseNote(
                        title = listNoteTitle,
                        type = Type.LIST,
                        items = listOf(createListItem("Item 1"), createListItem("Item 2")),
                    )
                )
        }

        val widgetId = 5678
        val intent =
            Intent(context, ConfigureWidgetActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }

        val scenario = ActivityScenario.launch<ConfigureWidgetActivity>(intent)

        waitUntilSucceeds { listNoteTitle.byText().perform(click()) }

        waitUntil { preferences.getWidgetData(widgetId) == listNoteId }
        assertEquals(listNoteId, preferences.getWidgetData(widgetId))
        assertEquals(Type.LIST, preferences.getWidgetNoteType(widgetId))

        scenario.close()
    }
}
