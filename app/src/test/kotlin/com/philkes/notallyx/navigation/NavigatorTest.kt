package com.philkes.notallyx.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.migration.ExportMimeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NavigatorTest {

    private fun createNavigationState(
        startRoute: NotallyNavKey = NotallyNavKey.Notes,
        topLevelRoutes: Set<NotallyNavKey> = DEFAULT_TOP_LEVEL_NAV_KEYS,
    ): NavigationState {
        val topLevelRoute = mutableStateOf(startRoute)
        val backStacks = topLevelRoutes.associateWith { key -> NavBackStack<NavKey>(key) }
        return NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
            resultEventBus = androidx.navigation3.runtime.result.ResultEventBus(),
        )
    }

    @Test
    fun `navigating to top level route switches tab and retains per-tab back stack`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Notes)
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)

        // Push a sub-route on Notes
        val searchRoute = NotallyNavKey.Search(initialFolder = Folder.NOTES)
        navigator.navigate(searchRoute)
        assertThat(navigator.currentRoute).isEqualTo(searchRoute)

        // Switch to Settings tab
        navigator.navigate(NotallyNavKey.Settings)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Settings)
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Settings)

        // Switch back to Notes tab - stack must be retained
        navigator.navigate(NotallyNavKey.Notes)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Notes)
        assertThat(navigator.currentRoute).isEqualTo(searchRoute)
    }

    @Test
    fun `goBack implements exit through home pattern`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        // Navigate Notes -> Settings
        navigator.navigate(NotallyNavKey.Settings)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Settings)

        // First back press returns to startRoute (Notes)
        val handledFirst = navigator.goBack()
        assertThat(handledFirst).isTrue()
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Notes)

        // Second back press exits app (returns false)
        val handledSecond = navigator.goBack()
        assertThat(handledSecond).isFalse()
    }

    @Test
    fun `goBack on sub-route in secondary tab pops sub-route first before home`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        // Navigate Notes -> Labels
        navigator.navigate(NotallyNavKey.Labels)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Labels)

        // Navigate Labels -> DisplayLabel("Recipes")
        val labelRoute = NotallyNavKey.DisplayLabel("Recipes")
        navigator.navigate(labelRoute)
        assertThat(navigator.currentRoute).isEqualTo(labelRoute)

        // First back: pops DisplayLabel, returns to Labels root
        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Labels)

        // Second back: returns to startRoute (Notes)
        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Notes)

        // Third back: returns false to exit
        assertThat(navigator.goBack()).isFalse()
    }

    @Test
    fun `skipStartViewOnBack exits directly when on secondary top level route`() {
        val state = createNavigationState()
        val navigator = Navigator(state, skipStartViewOnBack = true)

        navigator.navigate(NotallyNavKey.Settings)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Settings)

        // Directly returns false because skipStartViewOnBack is enabled
        assertThat(navigator.goBack()).isFalse()
    }

    @Test
    fun `reselecting current top-level route emits reselect event`() = runTest {
        val state = createNavigationState()
        val navigator = Navigator(state)

        var emittedKey: NotallyNavKey? = null
        val job =
            backgroundScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
                emittedKey = navigator.reselectEvents.first()
            }

        // Reselect Notes tab
        navigator.navigate(NotallyNavKey.Notes)

        assertThat(emittedKey).isEqualTo(NotallyNavKey.Notes)
        job.cancel()
    }

    @Test
    fun `reselecting tab with deeper stack pops back to root`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        navigator.navigate(NotallyNavKey.Labels)
        navigator.navigate(NotallyNavKey.DisplayLabel("Work"))
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.DisplayLabel("Work"))

        // Reselect Labels tab
        navigator.navigate(NotallyNavKey.Labels)
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Labels)
    }

    @Test
    fun `dialog destinations push to current stack and pop with goBack`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        val confirmDialog =
            NotallyNavKey.Confirm(
                message = "Are you sure?",
                actionType = "TEST_ACTION",
            )
        navigator.navigate(confirmDialog)
        assertThat(navigator.currentRoute).isEqualTo(confirmDialog)
        assertThat(navigator.currentTopLevelRoute).isEqualTo(NotallyNavKey.Notes)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)
    }

    @Test
    fun `navKeySerializer correctly serializes and deserializes NotallyNavKeys`() {
        val serializer = NavKeySerializer<NavKey>()
        val json = Json

        val keysToTest: List<NotallyNavKey> =
            listOf(
                NotallyNavKey.Notes,
                NotallyNavKey.Labels,
                NotallyNavKey.Deleted,
                NotallyNavKey.Archived,
                NotallyNavKey.Reminders,
                NotallyNavKey.Settings,
                NotallyNavKey.Unlabeled,
                NotallyNavKey.DisplayLabel("Work"),
                NotallyNavKey.Search(initialFolder = Folder.ARCHIVED, initialLabel = "Test"),
                NotallyNavKey.Confirm(
                    title = "Confirm",
                    message = "Delete note?",
                    actionType = "DELETE",
                ),
                NotallyNavKey.ColorPicker(
                    currentColor = "#FF0000",
                    colors = listOf("#FF0000", "#00FF00"),
                ),
                NotallyNavKey.ExportFormat(formats = listOf(ExportMimeType.PDF)),
                NotallyNavKey.PickNote(excludeNoteId = 42L),
                NotallyNavKey.SelectLabels(currentLabels = listOf("Work", "Personal")),
                NotallyNavKey.NoteReminders(noteId = 123L),
                NotallyNavKey.ViewImage(noteId = 456L, initialIndex = 2),
                NotallyNavKey.PlayAudio(
                    audioName = "recording.m4a",
                    duration = 5000L,
                    timestamp = 1700000000L,
                ),
                NotallyNavKey.RecordAudio,
                NotallyNavKey.EditNote(
                    noteId = 42L,
                    initialFolder = Folder.NOTES,
                    initialLabel = "Work",
                ),
                NotallyNavKey.EditList(
                    noteId = 43L,
                    initialFolder = Folder.NOTES,
                    initialLabel = "Shopping",
                ),
            )

        for (key in keysToTest) {
            val encoded = json.encodeToString(serializer, key)
            val decoded = json.decodeFromString(serializer, encoded)
            assertThat(decoded).isEqualTo(key)
        }
    }

    @Test
    fun `navigating to secondary destinations pushes to current stack and pops with goBack`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        val pickNoteKey = NotallyNavKey.PickNote(excludeNoteId = 99L)
        navigator.navigate(pickNoteKey)
        assertThat(navigator.currentRoute).isEqualTo(pickNoteKey)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)

        val noteRemindersKey = NotallyNavKey.NoteReminders(noteId = 42L)
        navigator.navigate(noteRemindersKey)
        assertThat(navigator.currentRoute).isEqualTo(noteRemindersKey)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)
    }

    @Test
    fun `navigating to edit destinations pushes to current stack and pops with goBack`() {
        val state = createNavigationState()
        val navigator = Navigator(state)

        val editNoteKey = NotallyNavKey.EditNote(noteId = 100L)
        navigator.navigate(editNoteKey)
        assertThat(navigator.currentRoute).isEqualTo(editNoteKey)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)

        val editListKey = NotallyNavKey.EditList(noteId = 200L)
        navigator.navigate(editListKey)
        assertThat(navigator.currentRoute).isEqualTo(editListKey)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.currentRoute).isEqualTo(NotallyNavKey.Notes)
    }

    @Test
    fun `resultEventBus sends and removes results without error`() {
        val state = createNavigationState()
        val bus = state.resultEventBus

        val testResult =
            PickedNoteResult(
                noteId = 123L,
                noteTitle = "Meeting notes",
                noteType = com.philkes.notallyx.data.model.Type.NOTE,
            )
        bus.sendResult(testResult)
        bus.removeResult<PickedNoteResult>()
    }

    @Test
    fun `ListDetailSceneStrategy activates when expanded and detail entry is present`() {
        val strategy = ListDetailSceneStrategy<NavKey>(isExpanded = true)
        val scope = androidx.navigation3.scene.SceneStrategyScope<NavKey>()

        val listEntry =
            androidx.navigation3.runtime.NavEntry<NavKey>(
                key = NotallyNavKey.Notes,
                metadata = ListDetailScene.listPane(),
            ) {}
        val detailEntry =
            androidx.navigation3.runtime.NavEntry<NavKey>(
                key = NotallyNavKey.EditNote(1L),
                metadata = ListDetailScene.detailPane(),
            ) {}

        // Single entry (no detail): should be null
        val singleScene = with(strategy) { scope.calculateScene(listOf(listEntry)) }
        assertThat(singleScene).isNull()

        // Two entries (list + detail): should return ListDetailScene
        val scene = with(strategy) { scope.calculateScene(listOf(listEntry, detailEntry)) }
        assertThat(scene).isNotNull
        assertThat(scene).isInstanceOf(ListDetailScene::class.java)
        val listDetailScene = scene as ListDetailScene<NavKey>
        assertThat(listDetailScene.listEntry).isEqualTo(listEntry)
        assertThat(listDetailScene.detailEntry).isEqualTo(detailEntry)

        // If not expanded: should be null even with list and detail
        val compactStrategy = ListDetailSceneStrategy<NavKey>(isExpanded = false)
        val compactScene =
            with(compactStrategy) { scope.calculateScene(listOf(listEntry, detailEntry)) }
        assertThat(compactScene).isNull()
    }
}
