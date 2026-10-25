package com.philkes.notallyx.feature.editor.listitem

import android.util.Log
import com.philkes.notallyx.core.common.extensions.uniqueCurrentMillis
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.extensions.check
import com.philkes.notallyx.data.model.extensions.deepCopy
import com.philkes.notallyx.data.model.extensions.findChild
import com.philkes.notallyx.data.model.extensions.plus
import com.philkes.notallyx.data.model.extensions.shouldParentBeChecked
import com.philkes.notallyx.data.model.extensions.shouldParentBeUnchecked

fun List<ListItem>.cloneList(): MutableList<ListItem> {
    val clone = this.indices.map { this[it].clone() as ListItem }.toMutableList()
    clone.forEach { itemClone ->
        itemClone.children =
            itemClone.children.map { child -> clone.first { it.id == child.id } }.toMutableList()
    }
    return clone
}

fun List<ListItem>.findParent(childItem: ListItem): Pair<Int, ListItem>? {
    this.indices.forEach {
        if (this[it].findChild(childItem.id) != null) {
            return Pair(it, this[it])
        }
    }
    return null
}

fun MutableList<ListItem>.removeWithChildren(item: ListItem): Pair<Int, Int> {
    val index = indexOf(item)
    removeAll(item.children + item)
    return Pair(index, item.children.size + 1)
}

fun MutableList<ListItem>.removeWithChildren(index: Int): List<ListItem> {
    val item = this[index]
    val removed = item + item.children
    removeAll(item.children + item)
    return removed
}

fun MutableList<ListItem>.moveWithChildren(
    fromIndex: Int,
    toIndex: Int,
    originalItem: ListItem? = null,
) {
    if (fromIndex !in indices || toIndex !in 0..size) {
        return
    }

    Log.d("ListItem", "Before move from: $fromIndex to: $toIndex:\n${this.toReadableString()}")

    val item = this[fromIndex]
    val wasOriginallyChild = originalItem?.isChild ?: item.isChild
    val wasOriginallyParent = !wasOriginallyChild

    val removed =
        if (wasOriginallyParent) {
            if (item.children.isNotEmpty()) {
                item + item.children
            } else if (originalItem != null && originalItem.children.isNotEmpty()) {
                val originalChildIds = originalItem.children.map { it.id }.toSet()
                val childrenInList = mutableListOf<ListItem>()
                var idx = fromIndex + 1
                while (idx < size && this[idx].id in originalChildIds) {
                    childrenInList.add(this[idx])
                    idx++
                }
                item + childrenInList
            } else {
                listOf(item)
            }
        } else {
            listOf(item)
        }

    removeAll(removed)

    val targetIndex =
        if (fromIndex < toIndex) {
            (toIndex - removed.size + 1).coerceIn(0, size)
        } else {
            toIndex.coerceIn(0, size)
        }

    addAll(targetIndex, removed)

    val movedItem = removed.first()
    if (targetIndex == 0) {
        movedItem.isChild = false
    } else {
        val prevItem = this[targetIndex - 1]
        val nextItem = this.getOrNull(targetIndex + removed.size)
        if (wasOriginallyParent) {
            val isNextChild = nextItem?.isChild == true
            movedItem.isChild = isNextChild
        } else {
            val hasParentAbove = findParentPosition(targetIndex - 1) != null || !prevItem.isChild
            movedItem.isChild = hasParentAbove
        }
    }

    initOrders(this)
    initChildren(this)

    Log.d("ListItem", "After move from: $fromIndex to: $toIndex:\n${this.toReadableString()}")
}

fun List<ListItem>.splitByChecked(): Pair<List<ListItem>, List<ListItem>> = partition {
    it.checked && (!it.isChild || findParent(it)?.second?.children?.areAllChecked() == true)
}

fun <R> List<R>.getOrNull(index: Int) = if (lastIndex >= index) this[index] else null

fun Collection<ListItem>.init(resetIds: Boolean = true): List<ListItem> {
    val initializedItems = deepCopy()
    initList(initializedItems, resetIds)
    checkBrokenList(initializedItems)
    return initializedItems
}

private fun checkBrokenList(list: List<ListItem>) {
    list.forEach { listItem ->
        if (listItem.shouldParentBeChecked()) {
            listItem.checked = true
        } else if (listItem.shouldParentBeUnchecked()) {
            listItem.checked = false
        }
    }
}

private fun initList(list: List<ListItem>, resetIds: Boolean) {
    if (resetIds) {
        list.forEachIndexed { index, item -> item.id = index }
    }
    initOrders(list)
    initChildren(list)
}

fun initChildren(list: List<ListItem>) {
    list.forEach { it.children.clear() }
    var parent: ListItem? = null
    list.forEach { item ->
        if (item.isChild && parent != null) {
            parent!!.children.add(item)
        } else {
            item.isChild = false
            parent = item
        }
    }
}

fun List<ListItem>.checkItem(
    item: ListItem,
    checked: Boolean,
    now: Long = uniqueCurrentMillis(),
) {
    if (!item.isChild) {
        item.check(checked)
    } else {
        item.checked = checked
        item.checkedTimestamp = if (checked) now else null
        val parent = findParent(item)?.second
        if (parent != null) {
            if (checked) {
                if (parent.children.areAllChecked()) {
                    parent.checked = true
                    parent.checkedTimestamp = now
                }
            } else {
                if (parent.checked) {
                    parent.checked = false
                    parent.checkedTimestamp = null
                }
            }
        }
    }
}

/** Makes sure every [ListItem.order] is valid and correct */
fun initOrders(list: List<ListItem>): Boolean {
    var invalidOrderFound = false
    list.forEachIndexed { idx, item ->
        if (item.order != idx) {
            invalidOrderFound = true
            item.order = idx
        }
    }
    return invalidOrderFound
}

fun List<ListItem>.areAllChecked(except: ListItem? = null): Boolean {
    return this.none { !it.checked && (except == null || it.id != except.id) }
}

fun MutableList<ListItem>.containsId(id: Int): Boolean {
    return this.any { it.id == id }
}

fun Collection<ListItem>.toReadableString(): String {
    return map { "$it order: ${it.order} id: ${it.id}" }.joinToString("\n")
}

fun List<ListItem>.findChildrenPositions(parentPosition: Int): List<Int> {
    val childrenPositions = mutableListOf<Int>()
    for (position in parentPosition + 1 until this.size) {
        if (this[position].isChild) {
            childrenPositions.add(position)
        } else {
            break
        }
    }
    return childrenPositions
}

fun List<ListItem>.findParentPosition(childPosition: Int): Int? {
    for (position in childPosition - 1 downTo 0) {
        if (!this[position].isChild) {
            return position
        }
    }
    return null
}

fun MutableList<ListItem>.deleteCheckedItems(): Set<Int> {
    return mapIndexed { index, listItem -> Pair(index, listItem) }
        .filter { (_, item) -> item.checked }
        .sortedBy { (_, item) -> item.isChild }
        .onEach { (_, item) ->
            removeFromParent(item)
            remove(item)
        }
        .map { (index, _) -> index }
        .toSet()
}

fun List<ListItem>.removeFromParent(child: ListItem): ListItem? {
    if (!child.isChild) {
        return null
    }
    return findParent(child)?.second?.also { it.children.remove(child) }
}
