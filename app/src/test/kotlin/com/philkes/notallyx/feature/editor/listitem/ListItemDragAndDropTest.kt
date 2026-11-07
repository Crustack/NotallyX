package com.philkes.notallyx.feature.editor.listitem

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.screens.DragState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListItemDragAndDropTest {

    private lateinit var lazyListState: LazyListState
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        lazyListState = mockk(relaxed = true)
    }

    private fun createListItem(
        id: Int,
        body: String,
        isChild: Boolean = false,
        children: MutableList<ListItem> = mutableListOf(),
    ): ListItem {
        return ListItem(
            id = id,
            body = body,
            checked = false,
            isChild = isChild,
            order = id,
            children = children,
        )
    }

    private fun mockVisibleItems(items: List<ListItem>, itemHeight: Int = 100) {
        val visibleInfos = items.mapIndexed { idx, _ ->
            val info = mockk<LazyListItemInfo>(relaxed = true)
            every { info.index } returns idx
            every { info.offset } returns idx * itemHeight
            every { info.size } returns itemHeight
            info
        }
        every { lazyListState.layoutInfo.visibleItemsInfo } returns visibleInfos
        every { lazyListState.firstVisibleItemIndex } returns 0
        every { lazyListState.firstVisibleItemScrollOffset } returns 0
    }

    @Test
    fun `test dragging parent over its own children does not call onMove`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val childA2 = createListItem(id = 2, body = "Child A2", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1, childA2),
            )
        val parentB = createListItem(id = 3, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, childA2, parentB)
        initChildren(items)

        var movesCalled = 0
        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> movesCalled++ },
            )

        mockVisibleItems(items)

        // 1. Start drag on Parent A (index 0, y offset 50)
        dragDropState.onDragStart(Offset(0f, 50f))
        assertEquals(0, dragDropState.draggingItemIndex)
        assertEquals("Parent A", dragDropState.draggedOriginalItem?.body)

        // 2. Drag Parent A down over Child A1 (index 1, y = 150)
        dragDropState.onDrag(Offset(0f, 100f))
        assertEquals(0, movesCalled)
        assertEquals(0, dragDropState.draggingItemIndex)

        // 3. Drag Parent A further down over Child A2 (index 2, y = 250)
        dragDropState.onDrag(Offset(0f, 100f))
        assertEquals(0, movesCalled)
        assertEquals(0, dragDropState.draggingItemIndex)

        // 4. Drag Parent A down over Parent B (index 3, y = 350)
        dragDropState.onDrag(Offset(0f, 100f))
        assertEquals(1, movesCalled)
    }

    @Test
    fun `test dragging parent over non-child item triggers onMove and updates list correctly`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1),
            )
        val parentB = createListItem(id = 2, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, parentB)
        initChildren(items)

        val moves = mutableListOf<Triple<Int, Int, ListItem>>()
        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { from, to, orig ->
                    moves.add(Triple(from, to, orig))
                    items.moveWithChildren(from, to, orig)
                },
            )

        mockVisibleItems(items)

        // Start drag on Parent A
        dragDropState.onDragStart(Offset(0f, 50f))

        // Drag Parent A down over Parent B (index 2)
        dragDropState.onDrag(Offset(0f, 200f))

        assertEquals(1, moves.size)
        assertEquals(0, moves[0].first)
        assertEquals(2, moves[0].second)

        // Verify items moved with children: Parent B is now before Parent A
        assertEquals(listOf("Parent B", "Parent A", "Child A1"), items.map { it.body })
    }

    @Test
    fun `test dragging child item over another item calls onMove`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1),
            )
        val parentB = createListItem(id = 2, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, parentB)
        initChildren(items)

        var movesCalled = 0
        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> movesCalled++ },
            )

        mockVisibleItems(items)

        // Start drag on Child A1 (index 1)
        dragDropState.onDragStart(Offset(0f, 150f))
        assertEquals(1, dragDropState.draggingItemIndex)

        // Drag Child A1 up over Parent A (index 0)
        dragDropState.onDrag(Offset(0f, -100f))
        assertEquals(1, movesCalled)
    }

    @Test
    fun `test isItemOrChildDragging is true for parent and all its children when parent is dragged`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val childA2 = createListItem(id = 2, body = "Child A2", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1, childA2),
            )
        val parentB = createListItem(id = 3, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, childA2, parentB)
        initChildren(items)

        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> },
            )

        mockVisibleItems(items)

        // Start drag on Parent A (index 0)
        dragDropState.onDragStart(Offset(0f, 50f))

        assertTrue(dragDropState.isItemOrChildDragging(0)) // Parent A
        assertTrue(dragDropState.isItemOrChildDragging(1)) // Child A1
        assertTrue(dragDropState.isItemOrChildDragging(2)) // Child A2
        assertFalse(dragDropState.isItemOrChildDragging(3)) // Parent B
    }

    @Test
    fun `test isItemOrChildDragging is true only for child when child is dragged`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val childA2 = createListItem(id = 2, body = "Child A2", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1, childA2),
            )

        val items = mutableListOf(parentA, childA1, childA2)
        initChildren(items)

        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> },
            )

        mockVisibleItems(items)

        // Start drag on Child A1 (index 1)
        dragDropState.onDragStart(Offset(0f, 150f))

        assertFalse(dragDropState.isItemOrChildDragging(0)) // Parent A
        assertTrue(dragDropState.isItemOrChildDragging(1)) // Child A1
        assertFalse(dragDropState.isItemOrChildDragging(2)) // Child A2
    }

    private fun getDragStateAndIsLastChild(
        index: Int,
        items: List<ListItem>,
        dragDropState: ListItemDragDropState,
    ): Pair<DragState, Boolean> {
        val draggingIndex = dragDropState.draggingItemIndex
        val isItemInDragGroup = dragDropState.isItemOrChildDragging(index)
        val dragState =
            when {
                !isItemInDragGroup -> DragState.NONE
                index == draggingIndex -> DragState.PARENT
                else -> DragState.CHILD
            }
        val isNextInGroup = index + 1 < items.size && dragDropState.isItemOrChildDragging(index + 1)
        val isLastChild = isItemInDragGroup && !isNextInGroup
        return Pair(dragState, isLastChild)
    }

    @Test
    fun `test DragState and isLastChild when dragging parent with children`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val childA2 = createListItem(id = 2, body = "Child A2", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1, childA2),
            )
        val parentB = createListItem(id = 3, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, childA2, parentB)
        initChildren(items)

        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> },
            )

        mockVisibleItems(items)

        // Drag Parent A (index 0)
        dragDropState.onDragStart(Offset(0f, 50f))

        val (state0, isLast0) = getDragStateAndIsLastChild(0, items, dragDropState)
        assertEquals(DragState.PARENT, state0)
        assertFalse(isLast0)

        val (state1, isLast1) = getDragStateAndIsLastChild(1, items, dragDropState)
        assertEquals(DragState.CHILD, state1)
        assertFalse(isLast1)

        val (state2, isLast2) = getDragStateAndIsLastChild(2, items, dragDropState)
        assertEquals(DragState.CHILD, state2)
        assertTrue(isLast2)

        val (state3, isLast3) = getDragStateAndIsLastChild(3, items, dragDropState)
        assertEquals(DragState.NONE, state3)
        assertFalse(isLast3)
    }

    @Test
    fun `test DragState and isLastChild when dragging parent without children`() {
        val parentA = createListItem(id = 0, body = "Parent A", isChild = false)
        val parentB = createListItem(id = 1, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, parentB)
        initChildren(items)

        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> },
            )

        mockVisibleItems(items)

        // Drag Parent A (index 0)
        dragDropState.onDragStart(Offset(0f, 50f))

        val (state0, isLast0) = getDragStateAndIsLastChild(0, items, dragDropState)
        assertEquals(DragState.PARENT, state0)
        assertTrue(isLast0)

        val (state1, isLast1) = getDragStateAndIsLastChild(1, items, dragDropState)
        assertEquals(DragState.NONE, state1)
        assertFalse(isLast1)
    }

    @Test
    fun `test DragState and isLastChild when dragging child directly`() {
        val childA1 = createListItem(id = 1, body = "Child A1", isChild = true)
        val parentA =
            createListItem(
                id = 0,
                body = "Parent A",
                isChild = false,
                children = mutableListOf(childA1),
            )
        val parentB = createListItem(id = 2, body = "Parent B", isChild = false)

        val items = mutableListOf(parentA, childA1, parentB)
        initChildren(items)

        val dragDropState =
            ListItemDragDropState(
                state = lazyListState,
                scope = testScope,
                getItems = { items },
                onMove = { _, _, _ -> },
            )

        mockVisibleItems(items)

        // Drag Child A1 (index 1) directly
        dragDropState.onDragStart(Offset(0f, 150f))

        val (state0, isLast0) = getDragStateAndIsLastChild(0, items, dragDropState)
        assertEquals(DragState.NONE, state0)
        assertFalse(isLast0)

        val (state1, isLast1) = getDragStateAndIsLastChild(1, items, dragDropState)
        assertEquals(DragState.PARENT, state1)
        assertTrue(isLast1)

        val (state2, isLast2) = getDragStateAndIsLastChild(2, items, dragDropState)
        assertEquals(DragState.NONE, state2)
        assertFalse(isLast2)
    }
}
