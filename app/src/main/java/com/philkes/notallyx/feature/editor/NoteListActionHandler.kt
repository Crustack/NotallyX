package com.philkes.notallyx.feature.editor

import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.feature.editor.listitem.ListManager

class NoteListActionHandler(private val listManager: ListManager) {

    fun handleAction(action: EditListAction) {
        when (action) {
            EditListAction.DELETE_CHECKED -> {
                listManager.deleteCheckedItems()
            }
            EditListAction.CHECK_ALL -> {
                listManager.changeCheckedForAll(true)
            }
            EditListAction.UNCHECK_ALL -> {
                listManager.changeCheckedForAll(false)
            }
        }
    }
}
