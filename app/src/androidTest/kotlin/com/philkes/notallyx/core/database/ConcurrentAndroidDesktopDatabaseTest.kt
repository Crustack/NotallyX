package com.philkes.notallyx.core.database

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getExternalMediaDirectory
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.waitUntil
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying concurrent access to the same SQLite database file between the
 * Android app (using Framework SQLite driver via [AndroidDatabaseDriverFactory]) and the Desktop
 * app (using [DesktopDatabaseDriverFactory] with
 * [androidx.sqlite.driver.bundled.BundledSQLiteDriver]) when the database is located in the public
 * external media folder in TRUNCATE journal mode.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class ConcurrentAndroidDesktopDatabaseTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private lateinit var desktopDb: NotallyDatabase
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUpConcurrentEnvironment() {
        preferences.dataInPublicFolder.save(value = true)
        preferences.autoSaveAfterIdleTime.save(value = -1)
        waitUntil { preferences.dataInPublicFolder.value }

        NotallyDatabase.clearInstance()

        val publicFolder = context.getExternalMediaDirectory()
        publicFolder.mkdirs()
        publicFolder
            .listFiles()
            ?.filter { it.name.startsWith(NotallyDatabase.DATABASE_NAME) }
            ?.forEach { it.delete() }
    }

    @After
    fun tearDownConcurrentEnvironment() {
        if (::desktopDb.isInitialized) {
            desktopDb.close()
        }
        scenario?.close()
        preferences.dataInPublicFolder.save(value = false)
        NotallyDatabase.clearInstance()

        val publicFolder = context.getExternalMediaDirectory()
        publicFolder
            .listFiles()
            ?.filter { it.name.startsWith(NotallyDatabase.DATABASE_NAME) }
            ?.forEach { it.delete() }
    }

    @Test
    fun testConcurrentAndroidAndDesktopAccessInPublicFolder() {
        val initialNote =
            createBaseNote(
                id = 1L,
                title = "Concurrent Initial Note",
                body = "Initial Body Text",
            )

        val androidDb =
            NotallyDatabase.getFreshDatabase(
                context,
                dataInPublic = true,
                biometricLock = BiometricLock.DISABLED,
            )
        runBlocking { androidDb.getBaseNoteDao().insert(initialNote) }

        val publicFolder = context.getExternalMediaDirectory()
        val publicDbFile = File(publicFolder, NotallyDatabase.DATABASE_NAME)
        assertTrue(
            "Database file must exist in public folder",
            (publicDbFile.exists() && (publicDbFile.length() > 0)),
        )

        // 1. Open Desktop app's database pointing to the exact same public folder concurrently
        val desktopPathProvider = DesktopPathProvider(baseDirOverride = publicFolder)
        val desktopDriverFactory = DesktopDatabaseDriverFactory(desktopPathProvider)
        desktopDb = NotallyDatabase.createDatabase(desktopDriverFactory, dataInPublic = true)

        // Verify desktop app loads initial notes properly
        var desktopInitialNotes = runBlocking {
            desktopDb.getBaseNoteDao().getByFolder(Folder.NOTES).first()
        }
        assertEquals(1, desktopInitialNotes.size)
        assertEquals("Concurrent Initial Note", desktopInitialNotes.first().title)
        assertEquals("Initial Body Text", desktopInitialNotes.first().body)

        // 2. Open Android app (MainActivity) concurrently
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Verify Android app loads initial notes properly
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Concurrent Initial Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Concurrent Initial Note").assertIsDisplayed()

        // 3. In the Android app, enter the note, change its text, and exit it to save it
        composeTestRule.onNodeWithText("Concurrent Initial Note").performClick()

        val titleLabel = context.getString(R.string.title)
        val noteLabel = context.getString(R.string.note)
        val backLabel = context.getString(R.string.back)

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(titleLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        desktopInitialNotes = runBlocking {
            desktopDb.getBaseNoteDao().getByFolder(Folder.NOTES).first()
        }
        assertEquals(1, desktopInitialNotes.size)
        assertEquals("Concurrent Initial Note", desktopInitialNotes.first().title)
        assertEquals("Initial Body Text", desktopInitialNotes.first().body)

        val updatedTitle = "Updated Note Title via Android"
        val updatedBody = "Updated Note Body Content via Android"

        composeTestRule
            .onNodeWithContentDescription(titleLabel)
            .performTextReplacement(updatedTitle)
        composeTestRule.onNodeWithContentDescription(noteLabel).performTextReplacement(updatedBody)
        desktopInitialNotes = runBlocking {
            desktopDb.getBaseNoteDao().getByFolder(Folder.NOTES).first()
        }
        assertEquals(1, desktopInitialNotes.size)
        assertEquals("Concurrent Initial Note", desktopInitialNotes.first().title)
        assertEquals("Initial Body Text", desktopInitialNotes.first().body)
        // Exit note to trigger auto-save
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Verify Android app returns to main screen and displays updated note
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText(updatedTitle)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(updatedTitle).assertIsDisplayed()

        // 4. In the desktop app, check that it shows the changed note properly
        desktopDb.invalidationTracker.refreshAsync()

        val desktopUpdatedNote = runBlocking { desktopDb.getBaseNoteDao().get(1L) }
        assertNotNull("Desktop app should find the updated note", desktopUpdatedNote)
        assertEquals(updatedTitle, desktopUpdatedNote?.title)
        assertEquals(updatedBody, desktopUpdatedNote?.body)

        val desktopNotesAfterSave = runBlocking {
            desktopDb.getBaseNoteDao().getByFolder(Folder.NOTES).first()
        }
        assertEquals(1, desktopNotesAfterSave.size)
        assertEquals(updatedTitle, desktopNotesAfterSave.first().title)
        assertEquals(updatedBody, desktopNotesAfterSave.first().body)
    }
}
