package com.philkes.notallyx.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.widget.ConfigureWidgetActivity
import com.philkes.notallyx.feature.widget.NoteWidget
import com.philkes.notallyx.feature.widget.NoteWidgetReceiver
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.appendText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.waitUntil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class WidgetTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

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
        val myProvider = ComponentName(context, NoteWidgetReceiver::class.java)

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
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

            // Verify widget preference was saved for Note 1
            waitUntil { preferences.getWidgetData(id) == note1Id }

            assertEquals(note1Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            scenario1.close()

            // Trigger Glance widget update and verify UI content on home screen
            runBlocking { NoteWidget.updateWidgetSuspend(context, id) }
            device.pressHome()

            waitUntil(10_000) { findWidgetOnHomeScreen(device, noteTitle1) }
            assertTrue(
                "Widget UI on home screen should display note title '$noteTitle1'",
                findWidgetOnHomeScreen(device, noteTitle1),
            )
            assertTrue(
                "Widget UI on home screen should display note body 'Body 1'",
                findWidgetOnHomeScreen(device, "Body 1"),
            )

            // Re-open ConfigureWidgetActivity for the same widget to select Note 2
            val intent2 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario2 = ActivityScenario.launch<ConfigureWidgetActivity>(intent2)

            // Select the second note
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            // Verify widget preference was updated to Note 2
            waitUntil { preferences.getWidgetData(id) == note2Id }

            assertEquals(note2Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            scenario2.close()

            // Verify updated widget UI on home screen for Note 2
            runBlocking { NoteWidget.updateWidgetSuspend(context, id) }
            device.pressHome()

            waitUntil(10_000) { findWidgetOnHomeScreen(device, noteTitle2) }
            assertTrue(
                "Widget UI on home screen should display note title '$noteTitle2'",
                findWidgetOnHomeScreen(device, noteTitle2),
            )
            assertTrue(
                "Widget UI on home screen should display note body 'Body 2'",
                findWidgetOnHomeScreen(device, "Body 2"),
            )

            // Open MainActivity, click Note 2, edit its title in EditNoteActivity, and exit
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            val appendedText = " Updated"
            val expectedUpdatedTitle = "$noteTitle2$appendedText"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle2)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText)

            // Exit EditNoteActivity to trigger save & widget update
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            // Verify database contains the updated note title
            waitUntil(10_000) {
                runBlocking {
                    database.getBaseNoteDao().get(note2Id)?.title == expectedUpdatedTitle
                }
            }

            assertEquals(note2Id, preferences.getWidgetData(id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(id))

            mainScenario.close()

            // Verify updated title appears on home screen widget UI
            runBlocking { NoteWidget.updateWidgetSuspend(context, id) }
            device.pressHome()

            waitUntil(10_000) { findWidgetOnHomeScreen(device, expectedUpdatedTitle) }
            assertTrue(
                "Widget UI on home screen should display updated note title '$expectedUpdatedTitle'",
                findWidgetOnHomeScreen(device, expectedUpdatedTitle),
            )
        } else {
            // Fallback for environments without pin widget support
            val testWidgetId = 5555
            val intent1 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidgetId)
                }
            val scenario1 = ActivityScenario.launch<ConfigureWidgetActivity>(intent1)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

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

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            waitUntil { preferences.getWidgetData(testWidgetId) == note2Id }
            assertEquals(note2Id, preferences.getWidgetData(testWidgetId))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidgetId))

            scenario2.close()

            // Open MainActivity, click Note 2, edit its title in EditNoteActivity, and exit
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            val appendedText = " Updated"
            val expectedUpdatedTitle = "$noteTitle2$appendedText"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle2)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            waitUntil(10_000) {
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
    fun configureWidgetActivity_selectsExistingTextNote_updatesPreferencesAndWidget() {
        val noteTitle = "Text Note For Widget"
        val noteBody = "Text content"
        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = noteTitle, body = noteBody, type = Type.NOTE))
        }

        val widgetId = 1234
        val intent =
            Intent(context, ConfigureWidgetActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }

        val scenario = ActivityScenario.launch<ConfigureWidgetActivity>(intent)

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(noteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(noteTitle).performClick()

        waitUntil { preferences.getWidgetData(widgetId) == noteId }
        assertEquals(noteId, preferences.getWidgetData(widgetId))
        assertEquals(Type.NOTE, preferences.getWidgetNoteType(widgetId))

        scenario.close()

        // Trigger widget update and verify database contents
        NoteWidget.updateWidget(context, widgetId)
        val fetchedNote = runBlocking { database.getBaseNoteDao().get(noteId) }
        assertNotNull(fetchedNote)
        assertEquals(noteTitle, fetchedNote?.title)
        assertEquals(noteBody, fetchedNote?.body)
    }

    @Test
    fun configureWidgetActivity_selectsExistingListNote_updatesPreferencesAndWidget() {
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

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(listNoteTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(listNoteTitle).performClick()

        waitUntil { preferences.getWidgetData(widgetId) == listNoteId }
        assertEquals(listNoteId, preferences.getWidgetData(widgetId))
        assertEquals(Type.LIST, preferences.getWidgetNoteType(widgetId))

        scenario.close()

        // Trigger widget update and verify list note contents
        NoteWidget.updateWidget(context, widgetId)
        val fetchedListNote = runBlocking { database.getBaseNoteDao().get(listNoteId) }
        assertNotNull(fetchedListNote)
        assertEquals(listNoteTitle, fetchedListNote?.title)
        assertEquals(2, fetchedListNote?.items?.size)
        assertEquals("Item 1", fetchedListNote?.items?.get(0)?.body)
        assertEquals("Item 2", fetchedListNote?.items?.get(1)?.body)
    }

    private fun findWidgetOnHomeScreen(
        device: UiDevice,
        text: String,
        timeout: Long = 2_000,
    ): Boolean {
        val selector = UiSelector().text(text)
        if (device.findObject(selector).waitForExists(timeout)) return true

        // Try swiping left on the launcher home screen to check page 2/3
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(width * 4 / 5, height / 2, width / 5, height / 2, 10)
        if (device.findObject(selector).waitForExists(1_000)) return true

        device.swipe(width * 4 / 5, height / 2, width / 5, height / 2, 10)
        if (device.findObject(selector).waitForExists(1_000)) return true

        // Reset launcher back to home screen page 1
        device.pressHome()
        return false
    }
}
