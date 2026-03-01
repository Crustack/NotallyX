package com.philkes.notallyx.presentation.activity.note

import com.philkes.notallyx.presentation.view.note.listitem.ListManager
import com.philkes.notallyx.presentation.viewmodel.preference.EditListAction

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
