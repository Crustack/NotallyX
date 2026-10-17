package com.philkes.notallyx.feature.widget

import android.app.Application
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.editor.listitem.findChildrenPositions
import com.philkes.notallyx.feature.editor.listitem.findParentPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToggleChecklistAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val position = parameters[PositionKey] ?: return

        withContext(Dispatchers.IO) {
            val database =
                NotallyDatabase.getDatabase(context.applicationContext as Application).value
                    ?: return@withContext
            val baseNoteDao = database.getBaseNoteDao()
            val note = baseNoteDao.get(noteId) ?: return@withContext
            if (position !in note.items.indices) return@withContext

            val item = note.items[position]
            val checked = !item.checked

            if (item.isChild) {
                changeChildChecked(note, position, checked, baseNoteDao, noteId)
            } else {
                val childrenPositions = note.items.findChildrenPositions(position)
                baseNoteDao.updateChecked(noteId, childrenPositions + position, checked)
            }
        }

        NoteWidget().update(context, glanceId)
    }

    private suspend fun changeChildChecked(
        note: BaseNote,
        childPosition: Int,
        checked: Boolean,
        baseNoteDao: BaseNoteDao,
        noteId: Long,
    ) {
        val parentPosition = note.items.findParentPosition(childPosition) ?: return
        val parent = note.items[parentPosition]
        val childrenPositions = note.items.findChildrenPositions(parentPosition)
        if (parent.checked != checked) {
            if (checked) {
                // If the last unchecked child is being checked also check parent
                if (childrenPositions.none { !note.items[it].checked && it != childPosition }) {
                    baseNoteDao.updateChecked(noteId, listOf(childPosition, parentPosition), true)
                } else {
                    baseNoteDao.updateChecked(noteId, childPosition, true)
                }
            } else {
                if (parent.checked) {
                    // If any child is unchecked the parent is unchecked too
                    baseNoteDao.updateChecked(noteId, listOf(childPosition, parentPosition), false)
                } else {
                    baseNoteDao.updateChecked(noteId, childPosition, false)
                }
            }
        } else {
            baseNoteDao.updateChecked(noteId, childPosition, false)
        }
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<Long>("noteId")
        val PositionKey = ActionParameters.Key<Int>("position")
    }
}
