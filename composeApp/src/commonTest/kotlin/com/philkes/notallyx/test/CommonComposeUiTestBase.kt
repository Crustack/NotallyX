package com.philkes.notallyx.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.philkes.notallyx.core.database.DatabaseDriverFactory
import com.philkes.notallyx.core.database.DesktopDatabaseDriverFactory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Platform-agnostic test base for Compose UI testing in `composeApp/src/commonTest`.
 *
 * Provides:
 * - Pure in-memory [NotallyDatabase] backed by [BundledSQLiteDriver]
 * - Isolated test Koin container without Android Context or Activity dependencies
 * - Helper utilities for note and label seeding
 * - Unified [runUiTest] harness wrapping [runComposeUiTest]
 */
@OptIn(ExperimentalTestApi::class)
abstract class CommonComposeUiTestBase : BaseComposeTest() {

    protected lateinit var tempDir: File
    protected lateinit var pathProvider: PathProvider
    protected lateinit var driverFactory: DatabaseDriverFactory
    protected lateinit var database: NotallyDatabase
    protected val noteDao: BaseNoteDao
        get() = database.getBaseNoteDao()

    protected val labelDao: LabelDao
        get() = database.getLabelDao()

    @Before
    open fun setUp() {
        tempDir =
            File(
                    System.getProperty("java.io.tmpdir"),
                    "notallyx-common-ui-test-${System.nanoTime()}",
                )
                .apply { mkdirs() }

        pathProvider = DesktopPathProvider(baseDirOverride = tempDir)
        driverFactory = DesktopDatabaseDriverFactory(pathProvider)
        database = NotallyDatabase.createDatabase(driverFactory, inMemory = true)
        NotallyDatabase.postInstance(database)

        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }

        val testDatabaseModule = module {
            single<DatabaseDriverFactory> { driverFactory }
            single<PathProvider> { pathProvider }
            single<NotallyDatabase> { database }
            single<StateFlow<NotallyDatabase?>> { NotallyDatabase.instance }
        }

        startKoin { modules(testDatabaseModule) }
    }

    @After
    open fun tearDown() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        NotallyDatabase.clearInstance()
        tempDir.deleteRecursively()
    }

    fun runUiTest(block: ComposeUiTest.() -> Unit) {
        runComposeUiTest(block = block)
    }

    protected fun insertNote(
        title: String = "Test Note",
        body: String = "Test Content",
        folder: Folder = Folder.NOTES,
        type: Type = Type.NOTE,
        color: String = BaseNote.COLOR_DEFAULT,
        pinned: Boolean = false,
        labels: List<String> = emptyList(),
        items: List<com.philkes.notallyx.data.model.ListItem> = emptyList(),
    ): Long = runBlocking {
        val now = System.currentTimeMillis()
        val note =
            BaseNote(
                id = 0L,
                type = type,
                folder = folder,
                color = color,
                title = title,
                pinned = pinned,
                timestamp = now,
                modifiedTimestamp = now,
                labels = labels,
                body = body,
                spans = emptyList(),
                items = items,
                images = emptyList(),
                files = emptyList(),
                audios = emptyList(),
                reminders = emptyList(),
                viewMode = NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        noteDao.insertSafe(note)
    }

    protected fun insertLabel(name: String): Unit = runBlocking {
        labelDao.insert(Label(value = name, order = 0))
    }
}
