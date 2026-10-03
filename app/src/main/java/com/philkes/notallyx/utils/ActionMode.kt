package com.philkes.notallyx.utils

import com.philkes.notallyx.data.model.BaseNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ActionMode {

    val enabled: StateFlow<Boolean>
        field = MutableStateFlow(false)
    val loading: StateFlow<Boolean>
        field = MutableStateFlow(false)
    val count: StateFlow<Int>
        field = MutableStateFlow(0)
    val selectedNotes = HashMap<Long, BaseNote>()
    val selectedIds = selectedNotes.keys
    val closeListener: StateFlow<Event<Set<Long>>?>
        field = MutableStateFlow<Event<Set<Long>>?>(null)
    var addListener: (() -> Unit)? = null

    private fun refresh() {
        count.value = selectedNotes.size
        enabled.value = selectedNotes.isNotEmpty()
    }

    fun add(id: Long, baseNote: BaseNote) {
        selectedNotes[id] = baseNote
        refresh()
    }

    fun add(baseNotes: Collection<BaseNote>) {
        baseNotes.forEach { selectedNotes[it.id] = it }
        refresh()
        addListener?.invoke()
    }

    fun remove(id: Long) {
        selectedNotes.remove(id)
        refresh()
    }

    fun close(notify: Boolean) {
        val previous = HashSet(selectedIds)
        selectedNotes.clear()
        refresh()
        if (notify && selectedNotes.size == 0) {
            closeListener.value = Event(previous)
        }
    }

    fun updateSelected(availableItemIds: List<Long>?) {
        selectedNotes.keys
            .filter { availableItemIds?.contains(it) == false }
            .forEach { selectedNotes.remove(it) }
        refresh()
    }

    fun setLoading(loading: Boolean) {
        this.loading.value = loading
    }

    fun isEnabled() = enabled.value

    fun isLoading() = loading.value

    // We assume selectedNotes.size is 1
    fun getFirstNote() = selectedNotes.values.first()

    fun isEmpty() = selectedNotes.values.isEmpty()
}
