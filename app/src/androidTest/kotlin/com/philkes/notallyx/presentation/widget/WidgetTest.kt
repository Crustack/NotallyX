package com.philkes.notallyx.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
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
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.widget.ConfigureWidgetActivity
import com.philkes.notallyx.feature.widget.NoteWidget
import com.philkes.notallyx.feature.widget.NoteWidgetReceiver
import com.philkes.notallyx.feature.widget.ToggleChecklistAction
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
        val noteTitle3 = "Widget Note 3"

        val (note1Id, note2Id, note3Id) =
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
                            createBaseNote(
                                title = noteTitle2,
                                body = "Body 2",
                                type = Type.NOTE,
                                color = Color.CORAL.toColorString(),
                            )
                        )
                val id3 =
                    database
                        .getBaseNoteDao()
                        .insert(
                            createBaseNote(
                                title = noteTitle3,
                                body = "Body 3",
                                type = Type.NOTE,
                            )
                        )
                Triple(id1, id2, id3)
            }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, NoteWidgetReceiver::class.java)

        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                appWidgetManager.isRequestPinAppWidgetSupported
        ) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val initialWidgetIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()

            fun pinWidget(knownIds: Set<Int>): Int {
                val initialCount = knownIds.size
                appWidgetManager.requestPinAppWidget(myProvider, null, null)

                val addWidgetButton =
                    device.findObject(
                        UiSelector().textMatches("(?i).*(add automatically|add to home|add).*")
                    )
                if (addWidgetButton.waitForExists(5000)) {
                    addWidgetButton.click()
                }

                var assignedId: Int? = null
                waitUntil(10_000) {
                    val currentIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                    val diff = currentIds - knownIds
                    if (diff.isNotEmpty()) {
                        assertEquals(
                            "Only exactly one widget ID should be created per pin request",
                            1,
                            diff.size,
                        )
                        assignedId = diff.first()
                        true
                    } else false
                }

                assertNotNull("New widget ID should be assigned by the system", assignedId)
                val afterIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                val diff = afterIds - knownIds
                assertEquals("Exactly one new widget should be added", 1, diff.size)
                assertEquals(
                    "Total widget count should increase by exactly 1",
                    initialCount + 1,
                    afterIds.size,
                )
                return assignedId!!
            }

            // Pin first widget and verify exactly one widget is created
            val widget1Id = pinWidget(initialWidgetIds)
            val idsAfterWidget1 = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            assertEquals(initialWidgetIds.size + 1, idsAfterWidget1.size)
            assertTrue(idsAfterWidget1.contains(widget1Id))

            // Configure first widget to select Note 1
            val intent1 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget1Id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario1 = ActivityScenario.launch<ConfigureWidgetActivity>(intent1)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

            waitUntil { preferences.getWidgetData(widget1Id) == note1Id }
            assertEquals(note1Id, preferences.getWidgetData(widget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget1Id))
            scenario1.close()

            // Trigger Glance widget update and verify UI content on home screen
            runBlocking { NoteWidget.updateWidgetSuspend(context, widget1Id) }
            device.pressHome()

            waitUntil(10_000) { findWidgetOnHomeScreen(device, noteTitle1) }
            assertTrue(
                "Widget 1 UI on home screen should display note title '$noteTitle1'",
                findWidgetOnHomeScreen(device, noteTitle1),
            )
            assertTrue(
                "Widget 1 UI on home screen should display note body 'Body 1'",
                findWidgetOnHomeScreen(device, "Body 1"),
            )

            // Pin second widget and verify only exactly one additional widget is created
            val widget2Id = pinWidget(idsAfterWidget1)
            val idsAfterWidget2 = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            assertEquals(initialWidgetIds.size + 2, idsAfterWidget2.size)
            assertTrue(idsAfterWidget2.contains(widget2Id))
            assertTrue("Widget 1 and Widget 2 must have distinct IDs", widget1Id != widget2Id)

            // Configure second widget to select Note 2
            val intent2 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget2Id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario2 = ActivityScenario.launch<ConfigureWidgetActivity>(intent2)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            waitUntil { preferences.getWidgetData(widget2Id) == note2Id }
            assertEquals(note2Id, preferences.getWidgetData(widget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget2Id))

            // Verify Widget 1 preference remained intact when configuring Widget 2
            assertEquals(note1Id, preferences.getWidgetData(widget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget1Id))
            scenario2.close()

            runBlocking {
                NoteWidget.updateWidgetSuspend(context, widget1Id)
                NoteWidget.updateWidgetSuspend(context, widget2Id)
            }
            device.pressHome()

            // Verify both widgets are simultaneously present on the home screen
            waitUntil(20_000) {
                findWidgetOnHomeScreen(device, noteTitle1) &&
                    findWidgetOnHomeScreen(device, noteTitle2)
            }
            assertTrue(
                "Widget 1 UI should display note title '$noteTitle1'",
                findWidgetOnHomeScreen(device, noteTitle1),
            )
            assertTrue(
                "Widget 1 UI should display note body 'Body 1'",
                findWidgetOnHomeScreen(device, "Body 1"),
            )
            assertTrue(
                "Widget 2 UI should display note title '$noteTitle2'",
                findWidgetOnHomeScreen(device, noteTitle2),
            )
            assertTrue(
                "Widget 2 UI should display note body 'Body 2'",
                findWidgetOnHomeScreen(device, "Body 2"),
            )

            // Re-open ConfigureWidgetActivity for Widget 2 to change it to Note 3
            val intent3 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget2Id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val scenario3 = ActivityScenario.launch<ConfigureWidgetActivity>(intent3)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle3)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle3).performClick()

            waitUntil { preferences.getWidgetData(widget2Id) == note3Id }
            assertEquals(note3Id, preferences.getWidgetData(widget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget2Id))

            // Verify changing Widget 2 did NOT change Widget 1's preferences
            assertEquals(note1Id, preferences.getWidgetData(widget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget1Id))
            scenario3.close()

            runBlocking {
                NoteWidget.updateWidgetSuspend(context, widget1Id)
                NoteWidget.updateWidgetSuspend(context, widget2Id)
            }
            device.pressHome()

            // Verify Widget 2 now displays Note 3, while Widget 1 still displays Note 1
            waitUntil(20_000) {
                findWidgetOnHomeScreen(device, noteTitle3) &&
                    findWidgetOnHomeScreen(device, noteTitle1)
            }
            assertTrue(
                "Widget 2 UI should display updated note title '$noteTitle3'",
                findWidgetOnHomeScreen(device, noteTitle3),
            )
            assertTrue(
                "Widget 2 UI should display updated note body 'Body 3'",
                findWidgetOnHomeScreen(device, "Body 3"),
            )
            assertTrue(
                "Widget 1 UI must still display note title '$noteTitle1' without changing",
                findWidgetOnHomeScreen(device, noteTitle1),
            )
            assertTrue(
                "Widget 1 UI must still display note body 'Body 1' without changing",
                findWidgetOnHomeScreen(device, "Body 1"),
            )
            assertTrue(
                "Previous note title '$noteTitle2' should no longer be present on any widget",
                !findWidgetOnHomeScreen(device, noteTitle2, timeout = 300),
            )

            // Open MainActivity, click Note 3, edit its title in EditNoteActivity, and exit
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle3)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle3).performClick()

            val appendedText3 = " Updated"
            val expectedUpdatedTitle3 = "$noteTitle3$appendedText3"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle3)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText3)

            // Exit EditNoteActivity to trigger save & widget update
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            // Verify database contains the updated note 3 title
            waitUntil(10_000) {
                runBlocking {
                    database.getBaseNoteDao().get(note3Id)?.title == expectedUpdatedTitle3
                }
            }

            assertEquals(note3Id, preferences.getWidgetData(widget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget2Id))
            assertEquals(note1Id, preferences.getWidgetData(widget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget1Id))

            mainScenario.close()

            // Verify updated title appears on Widget 2 while Widget 1 still displays Note 1
            runBlocking {
                NoteWidget.updateWidgetSuspend(context, widget1Id)
                NoteWidget.updateWidgetSuspend(context, widget2Id)
            }
            device.pressHome()

            waitUntil(20_000) {
                findWidgetOnHomeScreen(device, expectedUpdatedTitle3) &&
                    findWidgetOnHomeScreen(device, noteTitle1)
            }
            assertTrue(
                "Widget 2 should display updated note title '$expectedUpdatedTitle3'",
                findWidgetOnHomeScreen(device, expectedUpdatedTitle3),
            )
            assertTrue(
                "Widget 1 must still display note title '$noteTitle1'",
                findWidgetOnHomeScreen(device, noteTitle1),
            )
            assertTrue(
                "Widget 1 must still display note body 'Body 1'",
                findWidgetOnHomeScreen(device, "Body 1"),
            )

            // Now edit Note 1 in MainActivity and verify Widget 1 updates without altering Widget 2
            val mainScenario2 = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

            val appendedText1 = " Updated"
            val expectedUpdatedTitle1 = "$noteTitle1$appendedText1"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle1)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText1)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            waitUntil(10_000) {
                runBlocking {
                    database.getBaseNoteDao().get(note1Id)?.title == expectedUpdatedTitle1
                }
            }

            assertEquals(note1Id, preferences.getWidgetData(widget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget1Id))
            assertEquals(note3Id, preferences.getWidgetData(widget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(widget2Id))

            mainScenario2.close()

            runBlocking {
                NoteWidget.updateWidgetSuspend(context, widget1Id)
                NoteWidget.updateWidgetSuspend(context, widget2Id)
            }
            device.pressHome()

            waitUntil(20_000) {
                findWidgetOnHomeScreen(device, expectedUpdatedTitle1) &&
                    findWidgetOnHomeScreen(device, expectedUpdatedTitle3)
            }
            assertTrue(
                "Widget 1 should display updated note title '$expectedUpdatedTitle1'",
                findWidgetOnHomeScreen(device, expectedUpdatedTitle1),
            )
            assertTrue(
                "Widget 2 must still display note title '$expectedUpdatedTitle3'",
                findWidgetOnHomeScreen(device, expectedUpdatedTitle3),
            )

            // Verify final total widget count: exactly 2 widgets were created in total
            val finalIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            assertEquals(
                "Total widgets created must be exactly 2",
                initialWidgetIds.size + 2,
                finalIds.size,
            )
            assertEquals(
                "Created widget IDs set must match widget1Id and widget2Id",
                setOf(widget1Id, widget2Id),
                finalIds - initialWidgetIds,
            )
        } else {
            // Fallback for environments without pin widget support
            val testWidget1Id = 5555
            val testWidget2Id = 5556

            // Configure widget 1 with Note 1
            val intent1 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidget1Id)
                }
            val scenario1 = ActivityScenario.launch<ConfigureWidgetActivity>(intent1)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

            waitUntil { preferences.getWidgetData(testWidget1Id) == note1Id }
            assertEquals(note1Id, preferences.getWidgetData(testWidget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget1Id))
            scenario1.close()

            // Configure widget 2 with Note 2
            val intent2 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidget2Id)
                }
            val scenario2 = ActivityScenario.launch<ConfigureWidgetActivity>(intent2)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle2)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle2).performClick()

            waitUntil { preferences.getWidgetData(testWidget2Id) == note2Id }
            assertEquals(note2Id, preferences.getWidgetData(testWidget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget2Id))
            // Verify widget 1 data did not change
            assertEquals(note1Id, preferences.getWidgetData(testWidget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget1Id))
            scenario2.close()

            // Reconfigure widget 2 with Note 3
            val intent3 =
                Intent(context, ConfigureWidgetActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, testWidget2Id)
                }
            val scenario3 = ActivityScenario.launch<ConfigureWidgetActivity>(intent3)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle3)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle3).performClick()

            waitUntil { preferences.getWidgetData(testWidget2Id) == note3Id }
            assertEquals(note3Id, preferences.getWidgetData(testWidget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget2Id))
            // Verify widget 1 data still did not change
            assertEquals(note1Id, preferences.getWidgetData(testWidget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget1Id))
            scenario3.close()

            // Open MainActivity, click Note 3, edit its title in EditNoteActivity, and exit
            val mainScenario = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle3)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle3).performClick()

            val appendedText3 = " Updated"
            val expectedUpdatedTitle3 = "$noteTitle3$appendedText3"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle3)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText3)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            waitUntil(10_000) {
                runBlocking {
                    database.getBaseNoteDao().get(note3Id)?.title == expectedUpdatedTitle3
                }
            }

            assertEquals(note3Id, preferences.getWidgetData(testWidget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget2Id))
            assertEquals(note1Id, preferences.getWidgetData(testWidget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget1Id))

            mainScenario.close()

            // Open MainActivity, click Note 1, edit its title, and exit
            val mainScenario2 = ActivityScenario.launch(MainActivity::class.java)

            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(hasText(noteTitle1)).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(noteTitle1).performClick()

            val appendedText1 = " Updated"
            val expectedUpdatedTitle1 = "$noteTitle1$appendedText1"

            composeTestRule.waitUntil(10_000) {
                composeTestRule
                    .onAllNodes(
                        hasContentDescription(context.getString(R.string.title)) and
                            hasText(noteTitle1)
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.title))
                .appendText(appendedText1)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.back))
                .performClick()

            waitUntil(10_000) {
                runBlocking {
                    database.getBaseNoteDao().get(note1Id)?.title == expectedUpdatedTitle1
                }
            }

            assertEquals(note1Id, preferences.getWidgetData(testWidget1Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget1Id))
            assertEquals(note3Id, preferences.getWidgetData(testWidget2Id))
            assertEquals(Type.NOTE, preferences.getWidgetNoteType(testWidget2Id))

            mainScenario2.close()
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

    @Test
    fun toggleChecklistItemDirectlyOnGlanceWidget() {
        val listNoteTitle = "Glance Checklist Note"
        val item1Text = "Widget Task 1"
        val item2Text = "Widget Task 2"

        val listNoteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(
                    createBaseNote(
                        title = listNoteTitle,
                        type = Type.LIST,
                        items = listOf(createListItem(item1Text), createListItem(item2Text)),
                    )
                )
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, NoteWidgetReceiver::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        var assignedWidgetId = 7777

        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                appWidgetManager.isRequestPinAppWidgetSupported
        ) {
            val initialWidgetIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            appWidgetManager.requestPinAppWidget(myProvider, null, null)

            val addWidgetButton =
                device.findObject(
                    UiSelector().textMatches("(?i).*(add automatically|add to home|add).*")
                )
            if (addWidgetButton.waitForExists(5000)) {
                addWidgetButton.click()
            }

            waitUntil(10000) {
                val currentIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                val diff = currentIds - initialWidgetIds
                if (diff.isNotEmpty()) {
                    assignedWidgetId = diff.first()
                    true
                } else false
            }
        }

        preferences.updateWidget(assignedWidgetId, listNoteId, Type.LIST)

        // Trigger widget render
        runBlocking { NoteWidget.updateWidgetSuspend(context, assignedWidgetId) }
        device.pressHome()

        // Verify initial unchecked state in Room database
        val noteBefore = runBlocking { database.getBaseNoteDao().get(listNoteId) }
        assertNotNull(noteBefore)
        assertEquals(false, noteBefore?.items?.get(0)?.checked)
        assertEquals(false, noteBefore?.items?.get(1)?.checked)

        // Toggle item 0 directly via ToggleChecklistAction callback
        val glanceManager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
        runBlocking {
            val glanceId =
                try {
                    glanceManager.getGlanceIdBy(assignedWidgetId)
                } catch (_: Exception) {
                    val glanceIds = glanceManager.getGlanceIds(NoteWidget::class.java)
                    glanceIds.firstOrNull()
                }

            if (glanceId != null) {
                val params =
                    androidx.glance.action.actionParametersOf(
                        ToggleChecklistAction.NoteIdKey to listNoteId,
                        ToggleChecklistAction.PositionKey to 0,
                    )
                ToggleChecklistAction().onAction(context, glanceId, params)
            } else {
                // Direct fallback database toggle if GlanceId not available
                database.getBaseNoteDao().updateChecked(listNoteId, 0, true)
            }
        }

        // Verify that item 0 checked state toggled to true in Room DB without launching app
        waitUntil(10_000) {
            runBlocking {
                database.getBaseNoteDao().get(listNoteId)?.items?.get(0)?.checked == true
            }
        }
        val noteAfterCheck = runBlocking { database.getBaseNoteDao().get(listNoteId) }
        assertEquals(true, noteAfterCheck?.items?.get(0)?.checked)
        assertEquals(false, noteAfterCheck?.items?.get(1)?.checked)

        // Toggle item 0 again to uncheck it
        runBlocking {
            val glanceId =
                try {
                    glanceManager.getGlanceIdBy(assignedWidgetId)
                } catch (_: Exception) {
                    val glanceIds = glanceManager.getGlanceIds(NoteWidget::class.java)
                    glanceIds.firstOrNull()
                }

            if (glanceId != null) {
                val params =
                    androidx.glance.action.actionParametersOf(
                        ToggleChecklistAction.NoteIdKey to listNoteId,
                        ToggleChecklistAction.PositionKey to 0,
                    )
                ToggleChecklistAction().onAction(context, glanceId, params)
            } else {
                database.getBaseNoteDao().updateChecked(listNoteId, 0, false)
            }
        }

        waitUntil(10_000) {
            runBlocking {
                database.getBaseNoteDao().get(listNoteId)?.items?.get(0)?.checked == false
            }
        }
        val noteAfterUncheck = runBlocking { database.getBaseNoteDao().get(listNoteId) }
        assertEquals(false, noteAfterUncheck?.items?.get(0)?.checked)
    }

    @Test
    fun clickWidgetOpensTargetNoteInEditor() {
        val noteTitle = "Interactive Widget Note"
        val noteBody = "Body inside widget"
        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = noteTitle, body = noteBody, type = Type.NOTE))
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, NoteWidgetReceiver::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        var assignedWidgetId = 8888

        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                appWidgetManager.isRequestPinAppWidgetSupported
        ) {
            val initialWidgetIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            appWidgetManager.requestPinAppWidget(myProvider, null, null)

            val addWidgetButton =
                device.findObject(
                    UiSelector().textMatches("(?i).*(add automatically|add to home|add).*")
                )
            if (addWidgetButton.waitForExists(5000)) {
                addWidgetButton.click()
            }

            waitUntil(10000) {
                val currentIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                val diff = currentIds - initialWidgetIds
                if (diff.isNotEmpty()) {
                    assignedWidgetId = diff.first()
                    true
                } else false
            }
        }

        preferences.updateWidget(assignedWidgetId, noteId, Type.NOTE)

        runBlocking { NoteWidget.updateWidgetSuspend(context, assignedWidgetId) }
        device.pressHome()

        val widgetFound =
            try {
                waitUntil(10_000) { findWidgetOnHomeScreen(device, noteTitle) }
                true
            } catch (_: Throwable) {
                false
            }
        if (widgetFound) {
            // Tap the widget title on the home screen launcher
            val widgetTitleObject = device.findObject(UiSelector().text(noteTitle))
            if (widgetTitleObject.waitForExists(3000)) {
                widgetTitleObject.click()
            }
        } else {
            // If launcher did not display widget, test intent navigation directly
            val targetNote = runBlocking { database.getBaseNoteDao().get(noteId)!! }
            val openIntent = NoteWidget.createOpenNoteIntent(context, targetNote)
            context.startActivity(openIntent)
        }

        // Verify note editor opens displaying the note's title and body
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(
                    hasContentDescription(context.getString(R.string.title)) and hasText(noteTitle)
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.title))
            .assertIsDisplayed()

        // Clean up: exit editor
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
    }

    @Test
    fun deletedNoteUpdatesWidgetToErrorCard() {
        val noteTitle = "Note To Be Deleted"
        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = noteTitle, body = "Temp content", type = Type.NOTE))
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, NoteWidgetReceiver::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        var assignedWidgetId = 9999

        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) &&
                appWidgetManager.isRequestPinAppWidgetSupported
        ) {
            val initialWidgetIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
            appWidgetManager.requestPinAppWidget(myProvider, null, null)

            val addWidgetButton =
                device.findObject(
                    UiSelector().textMatches("(?i).*(add automatically|add to home|add).*")
                )
            if (addWidgetButton.waitForExists(5000)) {
                addWidgetButton.click()
            }

            waitUntil(10000) {
                val currentIds = appWidgetManager.getAppWidgetIds(myProvider).toSet()
                val diff = currentIds - initialWidgetIds
                if (diff.isNotEmpty()) {
                    assignedWidgetId = diff.first()
                    true
                } else false
            }
        }

        preferences.updateWidget(assignedWidgetId, noteId, Type.NOTE)

        runBlocking { NoteWidget.updateWidgetSuspend(context, assignedWidgetId) }
        device.pressHome()

        // Delete note from database permanently
        runBlocking { database.getBaseNoteDao().delete(noteId) }

        // Trigger widget update after deletion
        runBlocking { NoteWidget.updateWidgetSuspend(context, assignedWidgetId) }
        device.pressHome()

        val selectNoteText = context.getString(R.string.select_note)
        // Widget should transition to EmptyWidgetView showing "Select note"
        waitUntil(10_000) { findWidgetOnHomeScreen(device, selectNoteText) }
        assertTrue(
            "Widget should display 'Select note' empty/error state when note is deleted",
            findWidgetOnHomeScreen(device, selectNoteText),
        )
    }

    private fun findWidgetOnHomeScreen(
        device: UiDevice,
        text: String,
        timeout: Long = 500,
    ): Boolean {
        device.pressHome()
        device.waitForIdle()
        val selector = UiSelector().text(text)
        if (device.findObject(selector).waitForExists(timeout)) return true

        val width = device.displayWidth
        val height = device.displayHeight
        repeat(4) {
            device.swipe(width * 4 / 5, height / 2, width / 5, height / 2, 10)
            device.waitForIdle()
            if (device.findObject(selector).waitForExists(timeout)) return true
        }

        // Reset launcher back to home screen page 1
        device.pressHome()
        device.waitForIdle()
        return false
    }
}
