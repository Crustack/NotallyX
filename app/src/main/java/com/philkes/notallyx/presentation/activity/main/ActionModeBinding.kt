package com.philkes.notallyx.presentation.activity.main

import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialFade
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.databinding.ActivityMainBinding
import com.philkes.notallyx.presentation.activity.main.fragment.NotallyFragment
import com.philkes.notallyx.presentation.add
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortPreference
import com.philkes.notallyx.presentation.viewmodel.preference.isManualSort
import com.philkes.notallyx.utils.shareNote
import com.philkes.notallyx.utils.showColorSelectDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Helper class to manage the action mode menu */
class ActionModeBinding(
    private val baseModel: BaseNoteModel,
    private val binding: ActivityMainBinding,
    private val context: MainActivity,
    notesSorting: NotesSortPreference,
    private val currentFragment: () -> NotallyFragment?,
    private val onExportNotes: (mimeType: ExportMimeType) -> Unit,
) {

    private val actionModeToolbar = binding.ActionMode

    private val actionModeCancelCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                baseModel.actionMode.close(true)
            }
        }

    init {
        actionModeToolbar.setNavigationOnClickListener { baseModel.actionMode.close(true) }
        val transition =
            MaterialFade().apply {
                secondaryAnimatorProvider = null
                excludeTarget(binding.NavHostFragment, true)
                excludeChildren(binding.NavHostFragment, true)
                excludeTarget(binding.TakeNote, true)
                excludeTarget(binding.MakeList, true)
                excludeTarget(binding.NavigationView, true)
            }
        baseModel.actionMode.enabled.observe(context) { enabled ->
            TransitionManager.beginDelayedTransition(binding.RelativeLayout, transition)
            binding.apply {
                if (enabled) {
                    Toolbar.visibility = View.GONE
                    actionModeToolbar.visibility = View.VISIBLE
                } else {
                    Toolbar.visibility = View.VISIBLE
                    actionModeToolbar.visibility = View.GONE
                }
            }
            actionModeCancelCallback.isEnabled = enabled
        }
        baseModel.actionMode.loading.observe(context) { loading ->
            binding.ActionMode.menu.setGroupEnabled(Menu.NONE, !loading)
        }

        notesSorting.observe(context) { refreshActionModeMenu(baseModel.folder.value) }
        baseModel.folder.observe(context, Observer { value -> refreshActionModeMenu(value) })
    }

    fun close() {
        baseModel.actionMode.close(true)
    }

    val enabled
        get() = baseModel.actionMode.enabled

    fun enabled() = enabled.value

    var loading
        get() = baseModel.actionMode.loading.value
        set(value) {
            baseModel.actionMode.loading.value = value
        }

    val mode
        get() = baseModel.actionMode

    fun refreshActionModeMenu(value: Folder) {
        val menu = actionModeToolbar.menu
        menu.clear()
        baseModel.actionMode.count.removeObservers(context)

        menu.add(
            R.string.select_all,
            R.drawable.select_all,
            showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS,
        ) {
            context.getCurrentFragmentNotes?.invoke()?.let { baseModel.actionMode.add(it) }
        }
        when (value) {
            Folder.NOTES -> {
                val pinned = menu.addPinned(MenuItem.SHOW_AS_ACTION_ALWAYS)
                addManualSortActions()
                menu.add(R.string.duplicate, R.drawable.content_copy) {
                    baseModel.duplicateSelectedBaseNotes()
                }
                menu.add(R.string.archive, R.drawable.archive) {
                    context.moveNotes(Folder.ARCHIVED)
                }
                menu.addChangeColor()
                val share = menu.addShare()
                menu.addExportMenu()
                baseModel.actionMode.count.observeCountAndPinned(context, share, pinned)
            }

            Folder.ARCHIVED -> {
                addManualSortActions()
                menu.add(
                    R.string.unarchive,
                    R.drawable.unarchive,
                    MenuItem.SHOW_AS_ACTION_ALWAYS,
                    itemId = Menu.NONE,
                ) {
                    context.moveNotes(Folder.NOTES)
                }
                menu.add(R.string.duplicate, R.drawable.content_copy) {
                    baseModel.duplicateSelectedBaseNotes()
                }
                menu.addExportMenu(MenuItem.SHOW_AS_ACTION_ALWAYS)
                val pinned = menu.addPinned()
                menu.addChangeColor()
                val share = menu.addShare()
                baseModel.actionMode.count.observeCountAndPinned(context, share, pinned)
            }

            Folder.DELETED -> {
                addManualSortActions()
                menu.add(
                    R.string.restore,
                    R.drawable.restore,
                    MenuItem.SHOW_AS_ACTION_ALWAYS,
                    itemId = Menu.NONE,
                ) {
                    context.moveNotes(Folder.NOTES)
                }
                menu.add(
                    R.string.delete_forever,
                    R.drawable.delete,
                    MenuItem.SHOW_AS_ACTION_ALWAYS,
                    itemId = Menu.NONE,
                ) {
                    deleteForever()
                }
                menu.addExportMenu()
                menu.addChangeColor()
                val share = menu.add(R.string.share, R.drawable.share) { share() }
                baseModel.actionMode.count.observeCount(context, share)
            }
        }
    }

    private fun addManualSortActions() {
        if (baseModel.preferences.notesSorting.value.isManualSort) {
            actionModeToolbar.menu.apply {
                removeItem(11)
                removeItem(12)
                add(
                    R.string.previous,
                    R.drawable.arrow_upward,
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS,
                    order = Menu.NONE,
                    itemId = 11,
                ) {
                    currentFragment()?.moveSelectedNotesUp()
                }
                add(
                    R.string.next,
                    R.drawable.arrow_downward,
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS,
                    order = Menu.NONE,
                    itemId = 12,
                ) {
                    currentFragment()?.moveSelectedNotesDown()
                }
            }
        }
    }

    private fun Menu.addPinned(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.pin, R.drawable.pin, showAsAction) {}
    }

    private fun Menu.addChangeColor(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(
            R.string.change_color,
            R.drawable.change_color,
            showAsAction,
            itemId = Menu.NONE,
        ) {
            context.lifecycleScope.launch {
                val colors =
                    withContext(Dispatchers.IO) {
                        NotallyDatabase.getDatabase(context, observePreferences = false)
                            .value
                            .getBaseNoteDao()
                            .getAllColors()
                    }
                // Show color as selected only if all selected notes have the same color
                val currentColor =
                    baseModel.actionMode.selectedNotes.values
                        .map { it.color }
                        .distinct()
                        .takeIf { it.size == 1 }
                        ?.firstOrNull()
                context.showColorSelectDialog(
                    colors,
                    currentColor,
                    null,
                    { selectedColor, oldColor ->
                        if (oldColor != null) {
                            baseModel.changeColor(oldColor, selectedColor)
                        }
                        baseModel.colorBaseNote(selectedColor)
                    },
                ) { colorToDelete, newColor ->
                    baseModel.changeColor(colorToDelete, newColor)
                }
            }
        }
    }

    private fun Menu.addShare(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return add(R.string.share, R.drawable.share, showAsAction) { share() }
    }

    private fun share() {
        val baseNote = baseModel.actionMode.getFirstNote()
        context.shareNote(baseNote)
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.delete_selected_notes)
            .setPositiveButton(R.string.delete) { _, _ ->
                val removedNotes = baseModel.actionMode.selectedNotes.values.toList()
                baseModel.deleteSelectedBaseNotes()
                Snackbar.make(
                        binding.root,
                        context.getQuantityString(
                            R.plurals.deleted_selected_notes,
                            removedNotes.size,
                        ),
                        Snackbar.LENGTH_SHORT,
                    )
                    .apply { setAction(R.string.undo) { baseModel.saveNotes(removedNotes) } }
                    .show()
            }
            .setCancelButton()
            .show()
    }

    private fun Menu.addExportMenu(showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM): MenuItem {
        return addSubMenu(R.string.export)
            .apply {
                setIcon(R.drawable.export)
                item.setShowAsAction(showAsAction)
                ExportMimeType.entries.forEach { add(it.name).onClick { onExportNotes(it) } }
            }
            .item
    }

    private fun MenuItem.onClick(function: () -> Unit) {
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
            actionModeToolbar.title = count.toString()
            onCountChange?.invoke(count)
            share.isVisible = count == 1
        }
    }

    private fun NotNullLiveData<Int>.observeCountAndPinned(
        lifecycleOwner: LifecycleOwner,
        share: MenuItem,
        pinned: MenuItem,
    ) {
        observeCount(lifecycleOwner, share) {
            val baseNotes = baseModel.actionMode.selectedNotes.values
            if (baseNotes.any { !it.pinned }) {
                pinned.setTitle(R.string.pin).setIcon(R.drawable.pin).onClick {
                    baseModel.pinBaseNotes(true)
                }
            } else {
                pinned.setTitle(R.string.unpin).setIcon(R.drawable.unpin).onClick {
                    baseModel.pinBaseNotes(false)
                }
            }
        }
    }
}
