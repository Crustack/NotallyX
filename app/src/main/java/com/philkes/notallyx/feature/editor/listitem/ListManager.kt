package com.philkes.notallyx.feature.editor.listitem

import android.util.Log
import com.philkes.notallyx.core.common.extensions.lastIndex
import com.philkes.notallyx.core.common.extensions.uniqueCurrentMillis
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.autoSortByCheckedEnabled
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.extensions.check
import com.philkes.notallyx.data.model.extensions.findChild
import com.philkes.notallyx.data.model.extensions.plus
import com.philkes.notallyx.data.model.extensions.shouldParentBeChecked
import com.philkes.notallyx.data.model.extensions.shouldParentBeUnchecked
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.ChangeCheckedForAllChange
import com.philkes.notallyx.feature.editor.history.changes.DeleteCheckedChange
import com.philkes.notallyx.feature.editor.history.changes.EditTextState
import com.philkes.notallyx.feature.editor.history.changes.ListAddChange
import com.philkes.notallyx.feature.editor.history.changes.ListBatchChange
import com.philkes.notallyx.feature.editor.history.changes.ListCheckedChange
import com.philkes.notallyx.feature.editor.history.changes.ListDeleteChange
import com.philkes.notallyx.feature.editor.history.changes.ListEditTextChange
import com.philkes.notallyx.feature.editor.history.changes.ListIsChildChange
import com.philkes.notallyx.feature.editor.history.changes.ListMoveChange
import com.philkes.notallyx.feature.editor.listitem.sorting.SortedItemsList

data class ListState(
    val items: MutableList<ListItem>,
    val checkedItems: MutableList<ListItem>?,
    val focusedItemPos: Int? = null,
    val cursorPos: Int? = null,
)

/**
 * Should be used for all changes to the items list. Manages list state and pushes according changes
 * to the [ChangeHistory].
 */
class ListManager(
    val items: MutableList<ListItem> = mutableListOf(),
    var itemsChecked: SortedItemsList? = null,
    private val changeHistory: ChangeHistory = ChangeHistory(),
    private val preferences: NotallyXPreferences,
    private val endSearch: (() -> Unit)? = null,
    val onItemSizeChanged: ((items: Int) -> Unit)? = null,
) {
    private var nextItemId: Int = 0
    private var batchChangeBeforeState: ListState? = null

    init {
        updateNextItemId()
    }

    fun updateNextItemId() {
        val maxUncheckedId = this.items.maxOfOrNull { it.id } ?: -1
        val maxCheckedId = this.itemsChecked?.toMutableList()?.maxOfOrNull { it.id } ?: -1
        nextItemId =
            maxOf(
                0,
                maxUncheckedId + 1,
                maxCheckedId + 1,
                this.items.size + (this.itemsChecked?.size() ?: 0),
            )
    }

    fun init(
        items: List<ListItem>,
        itemsChecked: SortedItemsList? = null,
    ) {
        this.items.clear()
        this.items.addAll(items)
        this.itemsChecked = itemsChecked
        updateNextItemId()
        Log.d(TAG, "initList:\n${this.items.toReadableString()}")
        this.itemsChecked?.let { Log.d(TAG, "itemsChecked:\n${it.toReadableString()}") }
    }

    fun getState(selectedPos: Int? = null, cursorPos: Int? = null): ListState {
        return ListState(
            items.cloneList(),
            itemsChecked?.toMutableList()?.cloneList(),
            selectedPos,
            cursorPos,
        )
    }

    internal fun setState(state: ListState) {
        items.clear()
        items.addAll(state.items)
        state.checkedItems?.let { itemsChecked?.setItems(it) }
        updateNextItemId()
    }

    fun add(
        position: Int = items.size,
        item: ListItem = defaultNewItem(position.coerceAtMost(items.size)),
        pushChange: Boolean = true,
    ) {
        val stateBefore = getState()
        (item + item.children).forEach { setIdIfUnset(it) }

        val insertOrder =
            if (position < 1) {
                0
            } else if (position <= items.lastIndex) {
                items[position - 1].order!! + 1
            } else {
                items.lastOrNull()?.let { it.order!! + 1 } ?: 0
            }
        shiftItemOrdersHigher(insertOrder - 1, 1 + item.children.size)
        item.order = insertOrder
        item.children.forEachIndexed { index, child -> child.order = insertOrder + 1 + index }

        val parentPos =
            if (position <= items.lastIndex && items[position].isChild) {
                findParent(items[position])?.first
            } else null

        val (insertPos, _) = items.addWithChildren(item)
        if (item.isChild) {
            items.addToParent(insertPos)
        } else if (parentPos != null) {
            val childrenBelow = items.removeChildrenBelowPositionFromParent(parentPos, insertPos)
            item.children.addAll(childrenBelow)
        }
        if (pushChange) {
            changeHistory.push(ListAddChange(stateBefore, getState(selectedPos = insertPos), this))
        }

        onItemSizeChanged?.invoke(items.size + (itemsChecked?.size() ?: 0))
    }

    /**
     * Deletes item and its children at given position.
     *
     * @param force if false, deletion can be rejected, e.g. if trying to delete the first item
     * @return true if deletion was successful
     */
    fun delete(
        position: Int = items.lastIndex,
        inCheckedList: Boolean = false,
        force: Boolean = true,
        pushChange: Boolean = true,
    ): Boolean {
        val stateBefore = getState()
        var result = false
        if (position.isValidPosition(forCheckedList = inCheckedList)) {
            return false
        }
        if (force || position > 0) {
            val item = getItem(position, inCheckedList)
            shiftItemOrdersHigher(item.order!! - 1, 1 + item.children.size, items = items)
            if (inCheckedList) {
                itemsChecked!!.removeFromParent(item)
                itemsChecked!!.removeWithChildren(item)
            } else {
                val parent = items.removeFromParent(item)
                parent?.updateParentChecked(items)
                items.removeWithChildren(item)
            }
            result = true
        }
        if (pushChange && result) {
            changeHistory.push(ListDeleteChange(stateBefore, getState(), this))
        }
        onItemSizeChanged?.invoke(items.size + (itemsChecked?.size() ?: 0))
        return result
    }

    /** @return position of the moved item afterwards and the moved item count. */
    fun move(positionFrom: Int, positionTo: Int): Pair<Int, Int> {
        val itemsCheckedBefore = itemsChecked?.toMutableList()?.cloneList()
        val list = items.toMutableList()
        val movedItem = list[positionFrom]
        // Do not allow to move parent into its own children
        if (
            !movedItem.isChild &&
                positionTo in (positionFrom..positionFrom + movedItem.children.size)
        ) {
            return Pair(-1, -1)
        }

        val itemCount = 1 + movedItem.children.size
        val isMoveUpwards = positionFrom < positionTo

        val fromOrder = list[positionFrom].order!!
        val toOrder = list[positionTo].order!!
        val insertOrder = if (isMoveUpwards) toOrder - itemCount + 1 else toOrder
        val (orderRange, valueToAdd) =
            if (isMoveUpwards) {
                Pair(fromOrder + itemCount until toOrder + 1, -itemCount)
            } else {
                Pair(toOrder until fromOrder, itemCount)
            }
        shiftItemOrders(orderRange, valueToAdd, items = list)
        itemsCheckedBefore?.shiftItemOrders(orderRange, valueToAdd)

        list.removeFromParent(movedItem)
        list.removeWithChildren(movedItem)

        (movedItem + movedItem.children).forEachIndexed { index, item ->
            item.order = insertOrder + index
        }
        val (insertIdx, count) = list.addWithChildren(movedItem)
        items.clear()
        items.addAll(list)
        return Pair(insertIdx, count)
    }

    /** Finishes a drag movement by updating [ListItem.isChild] accordingly. */
    fun finishMove(
        positionTo: Int,
        count: Int,
        parentBefore: ListItem?,
        stateBefore: ListState,
        pushChange: Boolean,
    ) {
        val item = items[positionTo]
        val itemBelow = items.getOrNull(positionTo + count)
        val forceIsChild = itemBelow?.isChild == true && !item.isChild
        if (positionTo == 0) {
            item.isChild = false
        } else if (forceIsChild) {
            item.isChild = true
        }

        if (item.isChild) {
            items.refreshParent(positionTo)?.updateParentChecked()
        }
        parentBefore?.updateParentChecked()
        if (pushChange) {
            changeHistory.push(ListMoveChange(stateBefore, getState(), this))
        }
    }

    fun changeText(position: Int, value: EditTextState, pushChange: Boolean = true) {
        changeText(position, value.getEditableText().toString(), pushChange = pushChange)
    }

    fun changeText(
        position: Int,
        text: String,
        inCheckedList: Boolean = false,
        pushChange: Boolean = true,
    ) {
        val stateBefore = getState()
        endSearch?.invoke()
        val item = getItem(position, inCheckedList)
        item.body = text
        if (pushChange) {
            changeHistory.push(ListEditTextChange(stateBefore, getState(), this))
        }
    }

    fun changeChecked(
        position: Int,
        checked: Boolean,
        inCheckedList: Boolean = false,
        pushChange: Boolean = true,
    ) {
        val beforeState = getState()
        val item = getItem(position, inCheckedList)
        if (item.checked == checked) {
            return
        }
        if (item.isChild) {
            changeCheckedChild(item, checked, inCheckedList)
        } else {
            changeCheckedParent(item, checked, changeChildren = true)
        }
        if (pushChange) {
            changeHistory.push(ListCheckedChange(beforeState, getState(), this))
        }
    }

    fun changeCheckedForAll(checked: Boolean, pushChange: Boolean = true) {
        val stateBefore = getState()
        val parents =
            items.findParentsByChecked(!checked) +
                (itemsChecked?.findParentsByChecked(!checked) ?: listOf())
        parents
            .sortedBy { it.order ?: -1 }
            .forEach { parent -> changeCheckedParent(parent, checked, true) }
        if (pushChange) {
            changeHistory.push(ChangeCheckedForAllChange(stateBefore, getState(), this))
        }
    }

    fun changeIsChild(position: Int, isChild: Boolean, pushChange: Boolean = true) {
        val stateBefore = getState()
        items.findParentPosition(position)?.let { parentPos ->
            val nearestParent = items[parentPos]
            val item = items[position]
            item.isChild = isChild
            if (isChild) {
                items.refreshParent(position)
            } else {
                nearestParent.children.apply {
                    val childIndex = indexOf(item)
                    val childrenBelow = filterIndexed { idx, _ -> idx > childIndex }
                    removeAll(childrenBelow)
                    remove(item)
                    item.children = childrenBelow.toMutableList()
                }
            }
            item.updateParentChecked()
            nearestParent.updateParentChecked()
        }

        if (pushChange) {
            changeHistory.push(ListIsChildChange(stateBefore, getState(), this))
        }
    }

    fun deleteCheckedItems(pushChange: Boolean = true) {
        endSearch?.invoke()
        val stateBefore = getState()
        items.deleteCheckedItems()
        itemsChecked?.deleteCheckedItems()
        if (pushChange) {
            changeHistory.push(DeleteCheckedChange(stateBefore, getState(), this))
        }
        onItemSizeChanged?.invoke(items.size + (itemsChecked?.size() ?: 0))
    }

    fun findParent(item: ListItem) = items.findParent(item) ?: itemsChecked?.findParent(item)

    internal fun startBatchChange(cursorPos: Int? = null) {
        batchChangeBeforeState = getState()
        cursorPos?.let { batchChangeBeforeState = batchChangeBeforeState!!.copy(cursorPos = it) }
    }

    internal fun finishBatchChange(focusedItemPos: Int? = null) {
        batchChangeBeforeState?.let {
            val state =
                focusedItemPos?.let {
                    getState().copy(focusedItemPos = focusedItemPos, cursorPos = null)
                } ?: getState()
            changeHistory.push(ListBatchChange(it, state, this))
        }
    }

    fun getItem(position: Int, fromCheckedList: Boolean = false): ListItem {
        return if (fromCheckedList) itemsChecked!![position] else items[position]
    }

    internal fun defaultNewItem(position: Int) =
        ListItem(
            "",
            false,
            items.isNotEmpty() &&
                ((position < items.size && items[position].isChild) ||
                    (position > 0 && items[position - 1].isChild)),
            null,
            mutableListOf(),
            nextItemId++,
        )

    private fun changeCheckedParent(
        parent: ListItem,
        checked: Boolean,
        changeChildren: Boolean,
        items: MutableList<ListItem> = this@ListManager.items,
    ) {
        if (checked) {
            if (preferences.autoSortByCheckedEnabled) {
                checkWithAutoSort(parent, items)
            } else {
                parent.check(true, checkChildren = changeChildren)
            }
        } else {
            if (preferences.autoSortByCheckedEnabled) {
                uncheckWithAutoSort(parent, uncheckChildren = changeChildren)
            } else {
                parent.check(false, checkChildren = changeChildren)
            }
        }
    }

    private fun changeCheckedChild(
        child: ListItem,
        checked: Boolean,
        inCheckedList: Boolean,
    ) {
        if (checked) {
            child.checked = true
            child.checkedTimestamp = uniqueCurrentMillis()
            val (_, parent) = items.findParent(child)!!
            parent.updateParentChecked()
        } else {
            if (inCheckedList) {
                uncheckWithAutoSort(child)
            } else {
                child.checked = false
                child.checkedTimestamp = null
                checkParent(child, false)
            }
        }
    }

    private fun checkWithAutoSort(
        parent: ListItem,
        items: MutableList<ListItem> = this@ListManager.items,
    ) {
        items.removeWithChildren(parent)
        parent.check(true)
        itemsChecked?.addWithChildren(parent)
    }

    private fun uncheckWithAutoSort(
        item: ListItem,
        uncheckChildren: Boolean = true,
        items: MutableList<ListItem> = this@ListManager.items,
    ) {
        if (item.isChild) {
            val (_, parent) = itemsChecked!!.findParent(item)!!
            itemsChecked!!.removeWithChildren(parent)
            parent.findChild(item.id)!!.checked = false
            parent.checked = false
            items.addWithChildren(parent)
        } else {
            itemsChecked!!.removeWithChildren(item)
            item.check(false, uncheckChildren)
            items.addWithChildren(item)
        }
    }

    private fun ListItem.updateParentChecked(
        items: MutableList<ListItem> = this@ListManager.items
    ) {
        if (isChild) {
            return
        }
        if (shouldParentBeChecked()) {
            changeCheckedParent(this, true, changeChildren = true, items = items)
        }
        if (shouldParentBeUnchecked()) {
            changeCheckedParent(this, false, changeChildren = false, items = items)
        }
    }

    private fun checkParent(item: ListItem, checked: Boolean) {
        val (_, parent) = items.findParent(item)!!
        if (parent.checked != checked) {
            parent.checked = checked
            parent.checkedTimestamp = if (checked) uniqueCurrentMillis() else null
        }
    }

    private fun setIdIfUnset(newItem: ListItem) {
        if (newItem.id == -1) {
            newItem.id = nextItemId++
        }
    }

    private fun shiftItemOrdersHigher(
        threshold: Int,
        valueToAdd: Int,
        items: List<ListItem> = this.items,
    ) {
        items.shiftItemOrdersHigher(threshold, valueToAdd)
        itemsChecked?.shiftItemOrdersHigher(threshold, valueToAdd)
    }

    private fun shiftItemOrders(
        orderRange: IntRange,
        valueToAdd: Int,
        items: List<ListItem> = this.items,
    ) {
        items.shiftItemOrders(orderRange, valueToAdd)
        itemsChecked?.shiftItemOrders(orderRange, valueToAdd)
    }

    private fun Int.isValidPosition(forCheckedList: Boolean = false): Boolean {
        return this < 0 ||
            this > (if (forCheckedList) itemsChecked!!.lastIndex else items.lastIndex)
    }

    companion object {
        private const val TAG = "ListManager"
    }
}
