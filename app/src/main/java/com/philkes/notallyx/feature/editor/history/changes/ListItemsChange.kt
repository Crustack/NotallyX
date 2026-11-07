package com.philkes.notallyx.feature.editor.history.changes

import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.history.Change
import com.philkes.notallyx.feature.editor.listitem.cloneList
import com.philkes.notallyx.feature.editor.listitem.initChildren

class ListItemsChange(
    private val notallyModel: NoteModel,
    private val oldItems: List<ListItem>,
    private val newItems: List<ListItem>,
    private val onItemsUpdated: () -> Unit = {},
) : Change {

    private fun applyState(items: List<ListItem>) {
        val restored = items.cloneList()
        initChildren(restored)
        notallyModel.setItems(restored)
        onItemsUpdated()
    }

    override fun undo() = applyState(oldItems)

    override fun redo() = applyState(newItems)
}
