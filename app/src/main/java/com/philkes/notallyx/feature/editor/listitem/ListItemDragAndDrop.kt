package com.philkes.notallyx.feature.editor.listitem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.philkes.notallyx.data.model.ListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun rememberListItemDragDropState(
    lazyListState: LazyListState,
    items: List<ListItem>,
    onMove: (fromIndex: Int, toIndex: Int, originalItem: ListItem) -> Unit,
): ListItemDragDropState {
    val scope = rememberCoroutineScope()
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val state =
        remember(lazyListState) {
            ListItemDragDropState(
                state = lazyListState,
                scope = scope,
                getItems = { currentItems },
                onMove = { from, to, original -> currentOnMove(from, to, original) },
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

class ListItemDragDropState
internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val getItems: () -> List<ListItem>,
    private val onMove: (fromIndex: Int, toIndex: Int, originalItem: ListItem) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    var draggedOriginalItem by mutableStateOf<ListItem?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() =
            draggingItemLayoutInfo?.let { item ->
                (draggingItemInitialOffset + draggingItemDraggedDelta) - item.offset
            } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    internal var previousItemOffset = Animatable(0f)
        private set

    private var internalItems: MutableList<ListItem>? = null

    private fun currentItems(): List<ListItem> {
        return internalItems ?: getItems()
    }

    fun isItemOrChildDragging(index: Int): Boolean {
        return isItemOrChildOf(index, draggingItemIndex)
    }

    internal fun isItemOrChildOf(index: Int, targetParentIndex: Int?): Boolean {
        if (targetParentIndex == null) return false
        if (index == targetParentIndex) return true

        val currentItems = currentItems()
        val targetParentItem = currentItems.getOrNull(targetParentIndex)
        val currentItem = currentItems.getOrNull(index)

        if (targetParentItem == null || currentItem == null || !currentItem.isChild) return false

        if (targetParentItem.children.any { child -> child.id == currentItem.id }) {
            return true
        }

        val original = draggedOriginalItem
        if (original != null && original.id == targetParentItem.id) {
            if (original.children.any { child -> child.id == currentItem.id }) {
                return true
            }
        }

        val parentPair = currentItems.findParent(currentItem)
        return parentPair?.second?.id == targetParentItem.id
    }

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                val snapshot = getItems().map { item -> item.clone() as ListItem }.toMutableList()
                initChildren(snapshot)
                internalItems = snapshot
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
                val item = snapshot.getOrNull(it.index)
                draggedOriginalItem = item?.clone() as? ListItem
            }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != null) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f),
                )
                previousIndexOfDraggedItem = null
                internalItems = null
            }
        } else {
            internalItems = null
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0
        draggedOriginalItem = null
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem =
            state.layoutInfo.visibleItemsInfo.find { item ->
                middleOffset.toInt() in item.offset..item.offsetEnd &&
                    draggingItem.index != item.index
            }
        if (targetItem != null) {
            val currentItems = currentItems()
            val original = draggedOriginalItem
            val targetListItem = currentItems.getOrNull(targetItem.index)

            val isOwnChild = isTargetOwnChild(original, targetListItem, currentItems)

            if (!isOwnChild) {
                if (
                    draggingItem.index == state.firstVisibleItemIndex ||
                        targetItem.index == state.firstVisibleItemIndex
                ) {
                    state.requestScrollToItem(
                        state.firstVisibleItemIndex,
                        state.firstVisibleItemScrollOffset,
                    )
                }
                if (original != null) {
                    onMove(draggingItem.index, targetItem.index, original)
                    internalItems?.moveWithChildren(draggingItem.index, targetItem.index, original)
                }
                val updatedItems = currentItems()
                val draggedId = original?.id
                val updatedIndex =
                    if (draggedId != null) {
                        updatedItems.indexOfFirst { item -> item.id == draggedId }
                    } else {
                        -1
                    }
                val groupSize =
                    if (original != null && !original.isChild) original.children.size + 1 else 1
                val fallbackIndex =
                    if (draggingItem.index < targetItem.index) {
                        (targetItem.index - groupSize + 1).coerceAtLeast(0)
                    } else {
                        targetItem.index
                    }
                draggingItemIndex =
                    if (updatedIndex != -1) {
                        updatedIndex
                    } else {
                        fallbackIndex
                    }
            }
        } else {
            val overscroll =
                when {
                    draggingItemDraggedDelta > 0 ->
                        (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                    draggingItemDraggedDelta < 0 ->
                        (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                    else -> 0f
                }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    private fun isTargetOwnChild(
        original: ListItem?,
        targetListItem: ListItem?,
        currentItems: List<ListItem>,
    ): Boolean {
        if (original == null || targetListItem == null || !targetListItem.isChild) return false

        if (original.children.any { child -> child.id == targetListItem.id }) {
            return true
        }

        val parentPair = currentItems.findParent(targetListItem)
        return parentPair?.second?.id == original.id
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}

fun Modifier.dragContainer(dragDropState: ListItemDragDropState): Modifier {
    return pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset = offset)
            },
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() },
        )
    }
}

@Composable
fun LazyItemScope.draggableModifier(
    dragDropState: ListItemDragDropState,
    index: Int,
): Modifier {
    val isDragging = dragDropState.isItemOrChildOf(index, dragDropState.draggingItemIndex)
    val isPrevious = dragDropState.isItemOrChildOf(index, dragDropState.previousIndexOfDraggedItem)

    return if (isDragging) {
        Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.draggingItemOffset }
    } else if (isPrevious) {
        Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.previousItemOffset.value }
    } else {
        Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
    }
}

@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: ListItemDragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit,
) {
    val isDragging = dragDropState.isItemOrChildOf(index, dragDropState.draggingItemIndex)
    val isPrevious = dragDropState.isItemOrChildOf(index, dragDropState.previousIndexOfDraggedItem)

    val draggingModifier =
        if (isDragging) {
            Modifier.zIndex(1f).graphicsLayer { translationY = dragDropState.draggingItemOffset }
        } else if (isPrevious) {
            Modifier.zIndex(1f).graphicsLayer {
                translationY = dragDropState.previousItemOffset.value
            }
        } else {
            Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
        }
    Column(modifier = modifier.then(draggingModifier)) { content(isDragging) }
}
