package com.philkes.notallyx.data.model

import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class SearchResultTest {

    private lateinit var scope: CoroutineScope
    private lateinit var rows: MutableStateFlow<List<BaseNote>>
    private lateinit var result: SearchResult

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        rows = MutableStateFlow(listOf(createBaseNote(title = "First")))
        val dao = mockk<BaseNoteDao>()
        every { dao.getByKeyword(any(), any(), any()) } returns rows
        result =
            SearchResult(ApplicationProvider.getApplicationContext<ContextWrapper>(), scope, dao) {
                it
            }
        scope.launch { result.results.collect {} }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun initialListIsNotReadyBeforeTheFirstQueryCompletes() = runBlocking {
        result.fetch("", Folder.NOTES, "")
        assertFalse(result.isCurrentResult(emptyList()))
        awaitCurrent()
        assertTrue(result.isCurrentResult(result.value))
    }

    @Test
    fun changingKeywordInvalidatesPreviousResultsDuringDebounce() = runBlocking {
        result.fetch("first", Folder.NOTES, "")
        awaitCurrent()
        val submitted = result.value
        result.fetch("second", Folder.NOTES, "")
        assertFalse(result.isCurrentResult(submitted))
        // Equal rows need not trigger a new StateFlow list emission, but become current again.
        awaitCurrent()
        assertTrue(result.isCurrentResult(submitted))
    }

    @Test
    fun newRowsMustBeSubmittedBeforeAnOldAdapterListCanBeUsed() = runBlocking {
        result.fetch("", Folder.NOTES, "")
        awaitCurrent()
        val submitted = result.value
        rows.value = listOf(createBaseNote(title = "Second"))
        withTimeout(5000) { while (result.value == submitted) delay(10) }
        assertFalse(result.isCurrentResult(submitted))
        assertTrue(result.isCurrentResult(result.value))
    }

    @Test
    fun folderAndLabelChangesAlsoInvalidatePreviousResults() = runBlocking {
        result.fetch("", Folder.NOTES, "")
        awaitCurrent()
        val submitted = result.value
        result.fetch("", Folder.ARCHIVED, "Work")
        assertFalse(result.isCurrentResult(submitted))
        awaitCurrent()
        assertTrue(result.isCurrentResult(submitted))
    }

    private suspend fun awaitCurrent() =
        withTimeout(5000) { while (!result.isCurrentResult(result.value)) delay(10) }
}
