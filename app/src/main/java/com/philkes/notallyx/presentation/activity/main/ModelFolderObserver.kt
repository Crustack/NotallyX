package com.philkes.notallyx.presentation.activity.main

import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.presentation.activity.note.NoteActionHandler
import com.philkes.notallyx.presentation.add
import com.philkes.notallyx.presentation.checkNotificationPermission
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.movedToResId
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.view.misc.tristatecheckbox.TriStateCheckBox
import com.philkes.notallyx.presentation.view.misc.tristatecheckbox.setMultiChoiceTriStateItems
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.utils.deleteAttachments
import com.philkes.notallyx.utils.shareNote
import com.philkes.notallyx.utils.showColorSelectDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelFolderObserver(
    private val activity: MainActivity,
    private val menu: Menu,
    private val model: BaseNoteModel,
) : Observer<Folder> {

    private val baseModel
        get() = activity.baseModel

    override fun onChanged(value: Folder) {
        menu.clear()
        model.actionMode.count.removeObservers(activity)

        menu.add(
            R.string.select_all,
            R.drawable.select_all,
            showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS,
        ) {
            activity.getCurrentFragmentNotes?.invoke()?.let { model.actionMode.add(it) }
        }
        when (value) {
            Folder.NOTES -> initNotesFolderMenu()
            Folder.ARCHIVED -> initArchivedFolderMenu()
            Folder.DELETED -> initDeletedFolderMenu()
            Folder.HIDDEN -> initHiddenFolderMenu()
        }
    }

    private fun initNotesFolderMenu() {
        val pinned = menu.addPinned(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.addLabels(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.addDelete(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(R.string.duplicate, R.drawable.content_copy) {
            baseModel.duplicateSelectedBaseNotes()
        }
        menu.add(R.string.archive, R.drawable.archive) { moveNotes(Folder.ARCHIVED) }
        menu.add(R.string.hidden, R.drawable.hidden) { moveNotes(Folder.HIDDEN) }
        menu.addChangeColor()
        val pinnedToStatus = menu.addPinnedToStatus()
        val share = menu.addShare()
        menu.addExportMenu()
        model.actionMode.count.observeCountAndPinned(activity, share, pinned, pinnedToStatus)
    }

    private fun initArchivedFolderMenu() {
        menu.add(R.string.unarchive, R.drawable.unarchive, MenuItem.SHOW_AS_ACTION_ALWAYS) {
            moveNotes(Folder.NOTES)
        }
        menu.addDelete(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(R.string.duplicate, R.drawable.content_copy) {
            baseModel.duplicateSelectedBaseNotes()
        }
        menu.addExportMenu(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val pinned = menu.addPinned()
        menu.addLabels()
        menu.addChangeColor()
        val share = menu.addShare()
        model.actionMode.count.observeCountAndPinned(activity, share, pinned, null)
    }

    private fun initDeletedFolderMenu() {
        menu.add(R.string.restore, R.drawable.restore, MenuItem.SHOW_AS_ACTION_ALWAYS) {
            moveNotes(Folder.NOTES)
        }
        menu.add(R.string.delete_forever, R.drawable.delete, MenuItem.SHOW_AS_ACTION_ALWAYS) {
            deleteForever()
        }
        menu.addExportMenu()
        menu.addChangeColor()
        val share = menu.add(R.string.share, R.drawable.share) { share() }
        model.actionMode.count.observeCount(activity, share)
    }

    private fun initHiddenFolderMenu() {
        menu.add(
            R.string.unhidden,
            R.drawable.unhidden,
            MenuItem.SHOW_AS_ACTION_ALWAYS,
        ) {
            moveNotes(Folder.NOTES)
        }
        menu.addDelete(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.addLabels(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.addExportMenu()
        menu.addChangeColor()
        val share = menu.add(R.string.share, R.drawable.share) { share() }
        model.actionMode.count.observeCount(activity, share)

    }

    private fun Menu.addPinned(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.pin, R.drawable.pin, showAsAction) {}
    }

    private fun Menu.addPinnedToStatus(
        showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM
    ): MenuItem {
        return add(R.string.pin_to_status_bar, R.drawable.pinboard, showAsAction) {}
    }

    private fun Menu.addLabels(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.labels, R.drawable.label, showAsAction) { label() }
    }

    private fun Menu.addChangeColor(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.change_color, R.drawable.change_color, showAsAction) {
            activity.lifecycleScope.launch {
                val colors =
                    withContext(Dispatchers.IO) {
                        NotallyDatabase.getDatabase(activity, observePreferences = false)
                            .value
                            .getBaseNoteDao()
                            .getAllColors()
                            .toSet()
                    }
                // Show color as selected only if all selected notes have the same color
                val currentColor =
                    model.actionMode.selectedNotes.values
                        .map { it.color }
                        .distinct()
                        .takeIf { it.size == 1 }
                        ?.firstOrNull()
                activity.showColorSelectDialog(
                    colors,
                    currentColor,
                    null,
                    { selectedColor, oldColor ->
                        if (oldColor != null) {
                            model.changeColor(oldColor, selectedColor)
                        }
                        model.colorBaseNote(selectedColor)
                    },
                ) { colorToDelete, newColor ->
                    model.changeColor(colorToDelete, newColor)
                }
            }
        }
    }

    private fun Menu.addDelete(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.delete, R.drawable.delete, showAsAction) { moveNotes(Folder.DELETED) }
    }

    private fun Menu.addShare(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.share, R.drawable.share, showAsAction) { share() }
    }

    private fun Menu.addExportMenu(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return addSubMenu(R.string.export)
            .apply {
                setIcon(R.drawable.export)
                item.setShowAsAction(showAsAction)
                ExportMimeType.entries.forEach {
                    add(it.name).onClick { activity.exportSelectedNotes(it) }
                }
            }
            .item
    }

    fun MenuItem.onClick(function: () -> Unit) {
        setOnMenuItemClickListener {
            function()
            return@setOnMenuItemClickListener false
        }
    }

    private fun NotNullLiveData<Int>.observeCount(
        lifecycleOwner: LifecycleOwner,
        share: MenuItem,
        onCountChange: ((Int) -> Unit)? = null,
    ) {
        observe(lifecycleOwner) { count ->
            activity.binding.ActionMode.title = count.toString()
            onCountChange?.invoke(count)
            share.setVisible(count == 1)
        }
    }

    private fun NotNullLiveData<Int>.observeCountAndPinned(
        lifecycleOwner: LifecycleOwner,
        share: MenuItem,
        pinned: MenuItem,
        pinnedToStatus: MenuItem?,
    ) {
        observeCount(lifecycleOwner, share) {
            val baseNotes = model.actionMode.selectedNotes.values
            if (baseNotes.any { !it.pinned }) {
                pinned.setTitle(R.string.pin).setIcon(R.drawable.pin).onClick {
                    model.pinBaseNotes(true)
                }
            } else {
                pinned.setTitle(R.string.unpin).setIcon(R.drawable.unpin).onClick {
                    model.pinBaseNotes(false)
                }
            }
            pinnedToStatus?.let {
                if (baseNotes.any { !it.isPinnedToStatus }) {
                    pinnedToStatus
                        .setTitle(R.string.pin_to_status_bar)
                        .setIcon(R.drawable.pinboard)
                        .onClick {
                            activity.checkNotificationPermission(
                                NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
                                alsoCheckAlarmPermission = false,
                            ) {
                                model.pinBaseNotesToStatusBar(activity, true)
                            }
                        }
                } else {
                    pinnedToStatus
                        .setTitle(R.string.unpin_from_status_bar)
                        .setIcon(R.drawable.pinboard_filled)
                        .onClick { model.pinBaseNotesToStatusBar(activity, false) }
                }
            }
        }
    }

    internal fun moveNotes(folderTo: Folder) {
        if (baseModel.actionMode.loading.value || baseModel.actionMode.isEmpty()) {
            return
        }
        try {
            baseModel.actionMode.loading.value = true
            val folderFrom = baseModel.actionMode.getFirstNote().folder
            val ids =
                baseModel.moveBaseNotes(folderTo) { baseModel.actionMode.loading.postValue(false) }
            Snackbar.make(
                activity.findViewById(R.id.DrawerLayout),
                activity.getQuantityString(folderTo.movedToResId(), ids.size),
                Snackbar.LENGTH_SHORT,
            )
                .apply { setAction(R.string.undo) { baseModel.moveBaseNotes(ids, folderFrom) } }
                .show()
        } catch (_: Exception) {
            baseModel.actionMode.loading.postValue(false)
        }
    }

    internal fun share() {
        val baseNote = baseModel.actionMode.getFirstNote()
        activity.shareNote(baseNote)
    }

    internal fun deleteForever() {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.delete_selected_notes)
            .setPositiveButton(R.string.delete) { _, _ ->
                val removedNotes = baseModel.actionMode.selectedNotes.values.toList()
                activity.lifecycleScope.launch {
                    val deletedNotes = baseModel.deleteSelectedBaseNotes()
                    Snackbar.make(
                        activity.findViewById(R.id.DrawerLayout),
                        activity.getQuantityString(
                            R.plurals.deleted_selected_notes,
                            removedNotes.size,
                        ),
                        Snackbar.LENGTH_SHORT,
                    )
                        .apply {
                            setAction(R.string.undo) { baseModel.saveNotes(removedNotes) }
                            addCallback(
                                object : Snackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: Snackbar?,
                                        event: Int,
                                    ) {
                                        if (event != DISMISS_EVENT_ACTION) {
                                            activity.deleteAttachments(
                                                deletedNotes,
                                                baseModel.progress,
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        .show()
                }
            }
            .setCancelButton()
            .show()
    }

    internal fun label() {
        val baseNotes = baseModel.actionMode.selectedNotes.values
        activity.lifecycleScope.launch {
            val labels = baseModel.getAllLabels()
            if (labels.isNotEmpty()) {
                displaySelectLabelsDialog(labels, baseNotes)
            } else {
                baseModel.actionMode.close(true)
                activity.navigateWithAnimation(R.id.Labels)
            }
        }
    }

    private fun displaySelectLabelsDialog(labels: Array<String>, baseNotes: Collection<BaseNote>) {
        val checkedPositions =
            labels
                .map { label ->
                    if (baseNotes.all { it.labels.contains(label) }) {
                        TriStateCheckBox.State.CHECKED
                    } else if (baseNotes.any { it.labels.contains(label) }) {
                        TriStateCheckBox.State.PARTIALLY_CHECKED
                    } else {
                        TriStateCheckBox.State.UNCHECKED
                    }
                }
                .toTypedArray()

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.labels)
            .setCancelButton()
            .setMultiChoiceTriStateItems(activity, labels, checkedPositions) { idx, state ->
                checkedPositions[idx] = state
            }
            .setPositiveButton(R.string.save) { _, _ ->
                val checkedLabels =
                    checkedPositions.mapIndexedNotNull { index, checked ->
                        if (checked == TriStateCheckBox.State.CHECKED) {
                            labels[index]
                        } else null
                    }
                val uncheckedLabels =
                    checkedPositions.mapIndexedNotNull { index, checked ->
                        if (checked == TriStateCheckBox.State.UNCHECKED) {
                            labels[index]
                        } else null
                    }
                val updatedBaseNotesLabels =
                    baseNotes.map { baseNote ->
                        val noteLabels = baseNote.labels.toMutableList()
                        checkedLabels.forEach { checkedLabel ->
                            if (!noteLabels.contains(checkedLabel)) {
                                noteLabels.add(checkedLabel)
                            }
                        }
                        uncheckedLabels.forEach { uncheckedLabel ->
                            if (noteLabels.contains(uncheckedLabel)) {
                                noteLabels.remove(uncheckedLabel)
                            }
                        }
                        noteLabels
                    }
                baseNotes.zip(updatedBaseNotesLabels).forEach { (baseNote, updatedLabels) ->
                    baseModel.updateBaseNoteLabels(updatedLabels, baseNote.id)
                }
            }
            .show()
    }
}
