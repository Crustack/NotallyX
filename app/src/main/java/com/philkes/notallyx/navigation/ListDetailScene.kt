package com.philkes.notallyx.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

val LocalBackButtonVisibility = compositionLocalOf { true }

/**
 * A [Scene] that displays a list and a detail [NavEntry] side-by-side in an adaptive split (42%
 * list, 58% detail) on wide screens (screenWidthDp >= 600).
 */
data class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)

    override val content: @Composable (() -> Unit) = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) { listEntry.Content() }
            VerticalDivider(color = DividerDefaults.color.copy(alpha = 0.5f), thickness = 1.dp)
            CompositionLocalProvider(LocalBackButtonVisibility provides false) {
                Column(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = detailEntry,
                        contentKey = { entry -> entry.contentKey },
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        },
                    ) { entry ->
                        entry.Content()
                    }
                }
            }
        }
    }

    companion object {
        fun listPane(isTopLevel: Boolean = true) = metadata {
            put(ListKey, true)
            if (isTopLevel) put(TopLevelKey, true)
        }

        fun detailPane() = metadata { put(DetailKey, true) }

        fun topLevel() = metadata { put(TopLevelKey, true) }
    }

    object ListKey : NavMetadataKey<Boolean>

    object DetailKey : NavMetadataKey<Boolean>

    object TopLevelKey : NavMetadataKey<Boolean>
}

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val isExpanded =
        remember(windowInfo.containerSize, density) {
            with(density) { windowInfo.containerSize.width.toDp() >= 600.dp }
        }

    return remember(isExpanded) { ListDetailSceneStrategy(isExpanded) }
}

class ListDetailSceneStrategy<T : Any>(val isExpanded: Boolean) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!isExpanded) {
            return null
        }

        val detailEntry =
            entries.lastOrNull()?.takeIf { it.metadata.contains(ListDetailScene.DetailKey) }
                ?: return null
        val listEntry =
            entries.findLast { it.metadata.contains(ListDetailScene.ListKey) } ?: return null

        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.dropLast(1),
            listEntry = listEntry,
            detailEntry = detailEntry,
        )
    }
}
