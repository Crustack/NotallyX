package com.philkes.notallyx.feature.editor.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.philkes.notallyx.data.model.ListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
internal fun rememberDragDropState(
    lazyListState: LazyListState,
    items: List<ListItem>,
    onMove: (Int, Int) -> Unit,
    onMoveFinish: (ListItem) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnMoveFinish by rememberUpdatedState(onMoveFinish)

    val state =
        remember(lazyListState) {
            DragDropState(
                lazyListState = lazyListState,
                getItems = { currentItems },
                onMove = { from, to -> currentOnMove(from, to) },
                onMoveFinish = { item -> currentOnMoveFinish(item) },
                scope = scope,
            )
        }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

internal class DragDropState(
    private val lazyListState: LazyListState,
    private val getItems: () -> List<ListItem>,
    private val onMove: (Int, Int) -> Unit,
    private val onMoveFinish: (ListItem) -> Unit,
    private val scope: CoroutineScope,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    internal val draggingItemOffset: Float
        get() =
            draggingItemLayoutInfo?.let { item ->
                draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
            } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() {
            val index = draggingItemIndex ?: return null
            val itemKey = getItems().getOrNull(index)?.id ?: return null
            return lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey }
        }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    internal var previousItemOffset = Animatable(0f)
        private set

    fun onDragStart(index: Int) {
        val itemKey = getItems().getOrNull(index)?.id ?: return
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == itemKey }
            ?.also {
                draggingItemIndex = index
                draggingItemInitialOffset = it.offset
                draggingItemDraggedDelta = 0f
            }
    }

    fun onDragInterrupted() {
        val currentDraggedIndex = draggingItemIndex
        val draggedItem = currentDraggedIndex?.let { getItems().getOrNull(it) }

        if (currentDraggedIndex != null) {
            previousIndexOfDraggedItem = currentDraggedIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f),
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0

        if (draggedItem != null) {
            onMoveFinish(draggedItem)
        }
    }

    fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem =
            lazyListState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                middleOffset.toInt() in itemInfo.offset..(itemInfo.offset + itemInfo.size) &&
                    draggingItem.key != itemInfo.key &&
                    getItems().any { it.id == itemInfo.key }
            }

        if (targetItem != null) {
            val targetIndex = getItems().indexOfFirst { it.id == targetItem.key }
            val currentIndex = draggingItemIndex
            if (targetIndex != -1 && currentIndex != null && targetIndex != currentIndex) {
                if (
                    currentIndex == lazyListState.firstVisibleItemIndex ||
                        targetIndex == lazyListState.firstVisibleItemIndex
                ) {
                    lazyListState.requestScrollToItem(
                        lazyListState.firstVisibleItemIndex,
                        lazyListState.firstVisibleItemScrollOffset,
                    )
                }
                onMove(currentIndex, targetIndex)
                draggingItemIndex = targetIndex
            }
        } else {
            val overscroll =
                when {
                    draggingItemDraggedDelta > 0 ->
                        (endOffset - lazyListState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                    draggingItemDraggedDelta < 0 ->
                        (startOffset - lazyListState.layoutInfo.viewportStartOffset).coerceAtMost(
                            0f
                        )
                    else -> 0f
                }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }
}

@Composable
internal fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dragging = index == dragDropState.draggingItemIndex
    val draggingModifier =
        if (dragging) {
            Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.draggingItemOffset }
        } else if (index == dragDropState.previousIndexOfDraggedItem) {
            Modifier.zIndex(1f).graphicsLayer {
                translationY = dragDropState.previousItemOffset.value
            }
        } else {
            Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
        }
    Box(modifier = modifier.then(draggingModifier)) { content() }
}
