package com.philkes.notallyx.presentation.activity.note

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.NoteIdReminder
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.createNoteUrl
import com.philkes.notallyx.presentation.activity.note.PickNoteActivity.Companion.EXTRA_EXCLUDE_NOTE_ID
import com.philkes.notallyx.presentation.activity.note.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_ID
import com.philkes.notallyx.presentation.activity.note.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_TITLE
import com.philkes.notallyx.presentation.activity.note.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_TYPE
import com.philkes.notallyx.presentation.activity.note.SelectLabelsActivity.Companion.EXTRA_SELECTED_LABELS
import com.philkes.notallyx.presentation.activity.note.reminders.RemindersActivity
import com.philkes.notallyx.presentation.bindLabels
import com.philkes.notallyx.presentation.isLightColor
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.note.action.ExportBottomSheet
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.NotallyModel
import com.philkes.notallyx.presentation.viewmodel.preference.EditAction
import com.philkes.notallyx.utils.backup.exportNote
import com.philkes.notallyx.utils.cancelNoteReminders
import com.philkes.notallyx.utils.openNote
import com.philkes.notallyx.utils.shareNote
import com.philkes.notallyx.utils.showColorSelectDialog
import com.philkes.notallyx.utils.wrapWithChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteActionHandler(
    private val activity: EditActivity,
    private val notallyModel: NotallyModel,
) {
    lateinit var recordAudioActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var addImagesActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var viewImagesActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var selectLabelsActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var playAudioActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var attachFilesActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var pickNoteNewActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var pickNoteUpdateActivityResultLauncher: ActivityResultLauncher<Intent>

    lateinit var selectedSpan: URLSpan

    fun setupActivityResultLaunchers() {
        recordAudioActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    notallyModel.addAudio()
                }
            }
        addImagesActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val uri = result.data?.data
                    val clipData = result.data?.clipData
                    if (uri != null) {
                        val uris = arrayOf(uri)
                        notallyModel.addImages(uris)
                    } else if (clipData != null) {
                        val uris =
                            Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
                        notallyModel.addImages(uris)
                    }
                }
            }
        viewImagesActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val list =
                        result.data?.let {
                            IntentCompat.getParcelableArrayListExtra(
                                it,
                                ViewImageActivity.EXTRA_DELETED_IMAGES,
                                FileAttachment::class.java,
                            )
                        }
                    if (!list.isNullOrEmpty()) {
                        notallyModel.deleteImages(list)
                    }
                }
            }
        selectLabelsActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val list = result.data?.getStringArrayListExtra(EXTRA_SELECTED_LABELS)
                    if (list != null && list != notallyModel.labels) {
                        notallyModel.setLabels(list)
                        activity.binding.LabelGroup.bindLabels(
                            notallyModel.labels,
                            notallyModel.textSize,
                            paddingTop = true,
                            activity.colorInt,
                        )
                        activity.resetIdleTimer()
                    }
                }
            }
        playAudioActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val audio =
                        result.data?.let {
                            IntentCompat.getParcelableExtra(
                                it,
                                PlayAudioActivity.EXTRA_AUDIO,
                                Audio::class.java,
                            )
                        }
                    if (audio != null) {
                        notallyModel.deleteAudio(audio)
                    }
                }
            }
        attachFilesActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val uri = result.data?.data
                    val clipData = result.data?.clipData
                    if (uri != null) {
                        val uris = arrayOf(uri)
                        notallyModel.addFiles(uris)
                    } else if (clipData != null) {
                        val uris =
                            Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
                        notallyModel.addFiles(uris)
                    }
                }
            }
        exportFileActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        activity.baseModel.exportNoteToFile(
                            uri,
                            notallyModel.getBaseNote(),
                            activity.binding.root,
                        )
                    }
                }
            }
        pickNoteNewActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == RESULT_OK) {
                    try {
                        val (title, url, emptyTitle) =
                            requireNotNull(
                                    result.data,
                                    { "result.data is null for pickNoteNewActivityResultLauncher" },
                                )
                                .getPickedNoteData()
                        if (emptyTitle) {
                            activity.binding.EnterBody.showAddLinkDialog(
                                activity,
                                presetDisplayText = title,
                                presetUrl = url,
                                isNewUnnamedLink = true,
                            )
                        } else {
                            activity.binding.EnterBody.addSpans(
                                title,
                                listOf(UnderlineSpan(), URLSpan(url)),
                            )
                        }
                    } catch (_: IllegalArgumentException) {}
                }
            }
        pickNoteUpdateActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == RESULT_OK) {
                    try {
                        val (title, url, emptyTitle) =
                            requireNotNull(
                                    result.data,
                                    {
                                        "result.data is null for pickNoteUpdateActivityResultLauncher"
                                    },
                                )
                                .getPickedNoteData()
                        val newSpan = URLSpan(url)
                        activity.binding.EnterBody.updateSpan(selectedSpan, newSpan, title)
                        if (emptyTitle) {
                            activity.binding.EnterBody.showEditDialog(
                                newSpan,
                                isNewUnnamedLink = true,
                            )
                        }
                    } catch (_: IllegalArgumentException) {}
                }
            }
    }

    fun handleAction(action: EditAction) {
        when (action) {
            EditAction.SEARCH -> activity.startSearch()
            EditAction.PIN -> pin()
            EditAction.REMINDERS -> changeReminders()
            EditAction.LABELS -> changeLabels()
            EditAction.CHANGE_COLOR -> changeColor()
            EditAction.DUPLICATE -> duplicate()
            EditAction.EXPORT -> {
                ExportBottomSheet(activity.colorInt, ::export)
                    .show(activity.supportFragmentManager, ExportBottomSheet.TAG)
            }
            EditAction.SHARE -> share()
            EditAction.DELETE -> delete()
            EditAction.ARCHIVE -> archive()
            EditAction.TOGGLE_VIEW_MODE -> toggleViewMode()
            EditAction.CONVERT -> convertTo()
            EditAction.DELETE_FOREVER -> deleteForever()
            EditAction.RESTORE -> restore()
        }
    }

    private fun pin() {
        notallyModel.pinned = !notallyModel.pinned
        activity.bindPinned()
    }

    private fun changeReminders() {
        activity.lifecycleScope.launch {
            val noteId =
                if (notallyModel.id != 0L) {
                    notallyModel.id
                } else {
                    notallyModel.id = activity.saveNote(false)
                    notallyModel.id
                }
            val intent = Intent(activity, RemindersActivity::class.java)
            intent.putExtra(RemindersActivity.NOTE_ID, noteId)
            activity.startActivity(intent)
        }
    }

    private fun changeLabels() {
        val intent = Intent(activity, SelectLabelsActivity::class.java)
        intent.putStringArrayListExtra(EditActivity.EXTRA_SELECTED_LABELS, notallyModel.labels)
        selectLabelsActivityResultLauncher.launch(intent)
    }

    private fun changeColor() {
        activity.lifecycleScope.launch {
            val colors: MutableSet<ColorString> =
                withContext(Dispatchers.IO) {
                        NotallyDatabase.getDatabase(activity, observePreferences = false)
                            .value
                            .getBaseNoteDao()
                            .getAllColors()
                            .toMutableSet()
                    }
                    .toMutableSet()
            if (colors.none { it == notallyModel.color }) {
                colors.add(notallyModel.color)
            }
            activity.showColorSelectDialog(
                colors,
                notallyModel.color,
                activity.colorInt.isLightColor(),
                { selectedColor, oldColor ->
                    if (oldColor != null) {
                        activity.baseModel.changeColor(oldColor, selectedColor)
                    }
                    notallyModel.color = selectedColor
                    activity.setColor()
                    activity.resetIdleTimer()
                },
            ) { colorToDelete, newColor ->
                activity.baseModel.changeColor(colorToDelete, newColor)
                if (colorToDelete == notallyModel.color) {
                    notallyModel.color = newColor
                    activity.setColor()
                }
                activity.resetIdleTimer()
            }
        }
    }

    private fun duplicate() {
        activity.lifecycleScope.launch {
            activity.saveNote(true)
            val duplicateId = activity.baseModel.duplicateNote(notallyModel.getBaseNote())
            activity.openNote(duplicateId, notallyModel.type, clearBackStack = true)
        }
    }

    private fun share() {
        activity.shareNote(notallyModel.getBaseNote())
    }

    private fun export(mimeType: ExportMimeType) {
        activity.exportNote(notallyModel.getBaseNote(), mimeType, exportFileActivityResultLauncher)
    }

    private fun delete() {
        moveNote(Folder.DELETED)
    }

    private fun restore() {
        moveNote(Folder.NOTES)
    }

    private fun archive() {
        if (notallyModel.folder == Folder.ARCHIVED) {
            restore()
        } else {
            moveNote(Folder.ARCHIVED)
        }
    }

    private fun moveNote(toFolder: Folder) {
        if (toFolder != Folder.NOTES) {
            val noteIdReminders: List<NoteIdReminder> =
                listOf(NoteIdReminder(notallyModel.id, notallyModel.reminders.value))
            this.activity.cancelNoteReminders(noteIdReminders)
        }
        val resultIntent =
            Intent().apply {
                putExtra(EditActivity.EXTRA_NOTE_ID, notallyModel.id)
                putExtra(EditActivity.EXTRA_FOLDER_FROM, notallyModel.folder.name)
                putExtra(EditActivity.EXTRA_FOLDER_TO, toFolder.name)
            }
        notallyModel.folder = toFolder
        activity.setResult(AppCompatActivity.RESULT_OK, resultIntent)
        activity.finish()
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { _, _ ->
                activity.lifecycleScope.launch {
                    notallyModel.deleteBaseNote()
                    activity.finishAfterDeleteForever()
                }
            }
            .setCancelButton()
            .show()
    }

    private fun toggleViewMode() {
        notallyModel.viewMode.value =
            when (notallyModel.viewMode.value) {
                NoteViewMode.EDIT -> NoteViewMode.READ_ONLY
                NoteViewMode.READ_ONLY -> NoteViewMode.EDIT
            }
    }

    private fun convertTo() {
        activity.updateModel()
        activity.lifecycleScope.launch {
            val type = if (notallyModel.type == Type.LIST) Type.NOTE else Type.LIST
            notallyModel.convertTo(type)
            val intent =
                Intent(
                    activity,
                    when (type) {
                        Type.NOTE -> EditNoteActivity::class.java
                        Type.LIST -> EditListActivity::class.java
                    },
                )
            intent.putExtra(EditActivity.EXTRA_SELECTED_BASE_NOTE, notallyModel.id)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    @RequiresApi(24)
    fun recordAudio() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                MaterialAlertDialogBuilder(activity)
                    .setMessage(R.string.please_grant_notally_audio)
                    .setCancelButton()
                    .setPositiveButton(R.string.continue_) { _, _ ->
                        activity.requestPermissions(
                            arrayOf(permission),
                            EditActivity.REQUEST_AUDIO_PERMISSION,
                        )
                    }
                    .show()
            } else
                activity.requestPermissions(
                    arrayOf(permission),
                    EditActivity.REQUEST_AUDIO_PERMISSION,
                )
        } else startRecordAudioActivity()
    }

    fun startRecordAudioActivity() {
        if (notallyModel.audioRoot != null) {
            val intent = Intent(activity, RecordAudioActivity::class.java)
            recordAudioActivityResultLauncher.launch(intent)
        } else activity.showToast(R.string.insert_an_sd_card_audio)
    }

    fun handleRejection() {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.to_record_audio)
            .setCancelButton()
            .setPositiveButton(R.string.settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
            .show()
    }

    fun addImages() {
        if (notallyModel.imageRoot != null) {
            val intent =
                Intent(Intent.ACTION_GET_CONTENT)
                    .apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    .wrapWithChooser(activity)
            addImagesActivityResultLauncher.launch(intent)
        } else activity.showToast(R.string.insert_an_sd_card_images)
    }

    fun attachFiles() {
        if (notallyModel.filesRoot != null) {
            val intent =
                Intent(Intent.ACTION_GET_CONTENT)
                    .apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    .wrapWithChooser(activity)
            attachFilesActivityResultLauncher.launch(intent)
        } else activity.showToast(R.string.insert_an_sd_card_files)
    }

    fun addNoteLink() {
        linkNote(pickNoteNewActivityResultLauncher)
    }

    fun updateNoteLink(span: URLSpan) {
        selectedSpan = span
        linkNote(pickNoteUpdateActivityResultLauncher)
    }

    private fun linkNote(activityResultLauncher: ActivityResultLauncher<Intent>) {
        val intent =
            Intent(activity, PickNoteActivity::class.java).apply {
                putExtra(EXTRA_EXCLUDE_NOTE_ID, notallyModel.id)
            }
        activityResultLauncher.launch(intent)
    }

    private fun Intent.getPickedNoteData(): Triple<String, String, Boolean> {
        val noteId = getLongExtra(EXTRA_PICKED_NOTE_ID, -1L)
        if (noteId == -1L) {
            throw IllegalArgumentException("Invalid note picked!")
        }
        var emptyTitle = false
        val noteTitle =
            (getStringExtra(EXTRA_PICKED_NOTE_TITLE) ?: "").ifEmpty {
                emptyTitle = true
                activity.getString(R.string.note)
            }
        val noteType = Type.valueOf(getStringExtra(EXTRA_PICKED_NOTE_TYPE)!!)
        val noteUrl = noteId.createNoteUrl(noteType)
        return Triple(noteTitle, noteUrl, emptyTitle)
    }
}
