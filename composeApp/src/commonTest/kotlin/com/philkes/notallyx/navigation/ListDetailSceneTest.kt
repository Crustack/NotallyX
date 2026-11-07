package com.philkes.notallyx.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ListDetailSceneTest {

    private val scope = object : SceneStrategyScope<NotallyNavKey>() {}

    @Test
    fun calculateScene_returnsNull_whenNotExpanded() {
        val strategy = ListDetailSceneStrategy<NotallyNavKey>(isExpanded = false)
        val listEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.Notes, metadata = ListDetailScene.listPane()) {}
        val detailEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.EditNote(1L), metadata = ListDetailScene.detailPane()) {}

        val scene = with(strategy) { scope.calculateScene(listOf(listEntry, detailEntry)) }
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_returnsNull_whenOnlyListEntryPresent() {
        val strategy = ListDetailSceneStrategy<NotallyNavKey>(isExpanded = true)
        val listEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.Notes, metadata = ListDetailScene.listPane()) {}

        val scene = with(strategy) { scope.calculateScene(listOf(listEntry)) }
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_returnsNull_whenOnlyDetailEntryPresent() {
        val strategy = ListDetailSceneStrategy<NotallyNavKey>(isExpanded = true)
        val detailEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.EditNote(1L), metadata = ListDetailScene.detailPane()) {}

        val scene = with(strategy) { scope.calculateScene(listOf(detailEntry)) }
        assertThat(scene).isNull()
    }

    @Test
    fun calculateScene_returnsListDetailScene_whenListAndDetailPresent() {
        val strategy = ListDetailSceneStrategy<NotallyNavKey>(isExpanded = true)
        val listEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.Notes, metadata = ListDetailScene.listPane()) {}
        val detailEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.EditNote(42L), metadata = ListDetailScene.detailPane()) {}

        val scene = with(strategy) { scope.calculateScene(listOf(listEntry, detailEntry)) }
        assertThat(scene).isNotNull
        assertThat(scene).isInstanceOf(ListDetailScene::class.java)

        val listDetailScene = scene as ListDetailScene<NotallyNavKey>
        assertThat(listDetailScene.listEntry).isEqualTo(listEntry)
        assertThat(listDetailScene.detailEntry).isEqualTo(detailEntry)
        assertThat(listDetailScene.entries).containsExactly(listEntry, detailEntry)
    }

    @Test
    fun calculateScene_pairsCorrectly_withMultipleEntriesOnStack() {
        val strategy = ListDetailSceneStrategy<NotallyNavKey>(isExpanded = true)
        val notesEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.Notes, metadata = ListDetailScene.listPane()) {}
        val searchEntry: NavEntry<NotallyNavKey> =
            NavEntry(
                NotallyNavKey.Search(),
                metadata = ListDetailScene.listPane(isTopLevel = false),
            ) {}
        val detailEntry: NavEntry<NotallyNavKey> =
            NavEntry(NotallyNavKey.EditList(10L), metadata = ListDetailScene.detailPane()) {}

        val scene =
            with(strategy) { scope.calculateScene(listOf(notesEntry, searchEntry, detailEntry)) }
        assertThat(scene).isNotNull

        val listDetailScene = scene as ListDetailScene<NotallyNavKey>
        assertThat(listDetailScene.listEntry).isEqualTo(searchEntry)
        assertThat(listDetailScene.detailEntry).isEqualTo(detailEntry)
    }
}
