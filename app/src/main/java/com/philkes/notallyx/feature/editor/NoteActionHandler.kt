package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.Spanned
import android.text.style.URLSpan
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.openNote
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.isLightColor
import com.philkes.notallyx.core.ui.extensions.showColorSelectDialog
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.createNoteUrl
import com.philkes.notallyx.feature.editor.NoteActionHandler.Companion.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS
import com.philkes.notallyx.feature.editor.bottomsheet.ExportBottomSheet
import com.philkes.notallyx.feature.labels.SelectLabelsActivity
import com.philkes.notallyx.feature.main.PickNoteActivity
import com.philkes.notallyx.feature.main.PickNoteActivity.Companion.EXTRA_EXCLUDE_NOTE_ID
import com.philkes.notallyx.feature.main.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_ID
import com.philkes.notallyx.feature.main.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_TITLE
import com.philkes.notallyx.feature.main.PickNoteActivity.Companion.EXTRA_PICKED_NOTE_TYPE
import com.philkes.notallyx.feature.media.audio.PlayAudioActivity
import com.philkes.notallyx.feature.media.audio.RecordAudioActivity
import com.philkes.notallyx.feature.media.image.ViewImageActivity
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.exportNote
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.feature.reminder.RemindersActivity
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteActionHandler(
    private val activity: EditActivity,
    private val notallyModel: EditActivityViewModel,
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
                    val list =
                        result.data?.getStringArrayListExtra(
                            SelectLabelsActivity.EXTRA_SELECTED_LABELS
                        )
                    if (list != null && list != notallyModel.labels) {
                        notallyModel.setLabels(list)
                        activity.updateLabels(list)
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
                        notallyModel.exportNoteToFile(
                            uri,
                            notallyModel.getBaseNote(),
                            activity.findViewById(android.R.id.content),
                        )
                    }
                }
            }
        pickNoteNewActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == RESULT_OK) {
                    try {
                        val (title, url, _) =
                            requireNotNull(
                                    result.data,
                                    { "result.data is null for pickNoteNewActivityResultLauncher" },
                                )
                                .getPickedNoteData()
                        val start = notallyModel.body.length
                        notallyModel.body.append(title)
                        notallyModel.body.setSpan(
                            URLSpan(url),
                            start,
                            notallyModel.body.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    } catch (_: IllegalArgumentException) {}
                }
            }
        pickNoteUpdateActivityResultLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                if (result.resultCode == RESULT_OK) {
                    try {
                        val (title, url, _) =
                            requireNotNull(
                                    result.data,
                                    {
                                        "result.data is null for pickNoteUpdateActivityResultLauncher"
                                    },
                                )
                                .getPickedNoteData()
                        val newSpan = URLSpan(url)
                        val spanStart = notallyModel.body.getSpanStart(selectedSpan)
                        val spanEnd = notallyModel.body.getSpanEnd(selectedSpan)
                        if (spanStart != -1 && spanEnd != -1) {
                            notallyModel.body.removeSpan(selectedSpan)
                            notallyModel.body.setSpan(
                                newSpan,
                                spanStart,
                                spanEnd,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
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
            EditAction.PIN_TO_STATUS -> pinToStatus()
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
            EditAction.TOGGLE_VIEW_MODE -> {
                notallyModel.toggleViewMode()
            }
            EditAction.CONVERT -> convertTo()
            EditAction.DELETE_FOREVER -> deleteForever()
            EditAction.RESTORE -> restore()
        }
    }

    private fun pin() {
        notallyModel.pinned = !notallyModel.pinned
        activity.bindPinned()
    }

    fun pinToStatus() {
        if (!notallyModel.isPinnedToStatus) {
            activity.checkNotificationPermission(
                REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
                alsoCheckAlarmPermission = false,
            ) {
                notallyModel.isPinnedToStatus = true
                activity.bindPinned()
                activity.refreshStatusBarPin(notallyModel.getBaseNote())
            }
        } else {
            notallyModel.isPinnedToStatus = false
            activity.bindPinned()
            activity.refreshStatusBarPin(notallyModel.getBaseNote())
        }
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
        intent.putStringArrayListExtra(
            SelectLabelsActivity.EXTRA_SELECTED_LABELS,
            notallyModel.labels,
        )
        selectLabelsActivityResultLauncher.launch(intent)
    }

    private fun changeColor() {
        activity.lifecycleScope.launch {
            val database = NotallyDatabase.getDatabase(activity).value ?: return@launch
            val colors: MutableSet<ColorString> =
                withContext(Dispatchers.IO) {
                        database.getBaseNoteDao().getAllColors().toMutableSet()
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
                        notallyModel.changeColor(oldColor, selectedColor)
                    }
                    notallyModel.color = selectedColor
                    activity.setColor()
                    activity.resetIdleTimer()
                },
            ) { colorToDelete, newColor ->
                notallyModel.changeColor(colorToDelete, newColor)
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
            val duplicateId = notallyModel.duplicateNote(notallyModel.getBaseNote())
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
            this.activity.cancelPinAndReminders(notallyModel.id, notallyModel.reminders.value)
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
        val composeView = ComposeView(activity)
        val dialog =
            Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setContentView(composeView)
            }
        composeView.setContent {
            NotallyXTheme(theme = notallyModel.preferences.theme.value) {
                ConfirmDialog(
                    message = stringResource(R.string.delete_note_forever),
                    positiveButtonText = stringResource(R.string.delete),
                    negativeButtonText = stringResource(R.string.cancel),
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = {
                        activity.lifecycleScope.launch {
                            notallyModel.deleteBaseNote()
                            activity.finishAfterDeleteForever()
                        }
                    },
                )
            }
        }
        dialog.show()
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

    fun recordAudio() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                val composeView = ComposeView(activity)
                val dialog =
                    Dialog(activity).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        setContentView(composeView)
                    }
                composeView.setContent {
                    NotallyXTheme(theme = notallyModel.preferences.theme.value) {
                        ConfirmDialog(
                            message = stringResource(R.string.please_grant_notally_audio),
                            positiveButtonText = stringResource(R.string.continue_),
                            negativeButtonText = stringResource(R.string.cancel),
                            onDismissRequest = { dialog.dismiss() },
                            onConfirm = {
                                activity.requestPermissions(
                                    arrayOf(permission),
                                    EditActivity.REQUEST_AUDIO_PERMISSION,
                                )
                            },
                        )
                    }
                }
                dialog.show()
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

    fun viewImage(image: FileAttachment) {
        val position = notallyModel.images.value.indexOf(image)
        val intent =
            Intent(activity, ViewImageActivity::class.java).apply {
                putExtra(EditActivity.EXTRA_SELECTED_BASE_NOTE, notallyModel.id)
                putExtra(ViewImageActivity.EXTRA_POSITION, if (position != -1) position else 0)
            }
        viewImagesActivityResultLauncher.launch(intent)
    }

    fun viewFile(fileAttachment: FileAttachment) {
        val file =
            activity.application.resolveAttachmentFile(SUBFOLDER_FILES, fileAttachment.localName)
        if (file != null && file.exists()) {
            val uri = activity.getUriForFile(file)
            activity.viewFile(uri, fileAttachment.mimeType)
        }
    }

    fun playAudio(audio: Audio) {
        val intent =
            Intent(activity, PlayAudioActivity::class.java).apply {
                putExtra(EditActivity.EXTRA_SELECTED_BASE_NOTE, notallyModel.id)
                putExtra(PlayAudioActivity.EXTRA_AUDIO, audio)
            }
        playAudioActivityResultLauncher.launch(intent)
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

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS = 37
    }
}

fun Activity.refreshStatusBarPin(note: BaseNote) {
    val refresh = {
        if (note.isPinnedToStatus) {
            PinnedNotificationManager.notify(this, note)
        } else {
            PinnedNotificationManager.cancel(this, note.id)
        }
    }
    if (note.isPinnedToStatus) {
        checkNotificationPermission(
            REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
            alsoCheckAlarmPermission = false,
        ) {
            refresh()
        }
    } else {
        refresh()
    }
}

fun Activity.handleRejection(msgResId: Int) {
    val composeView = ComposeView(this)
    val dialog =
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(composeView)
        }
    val preferences = NotallyXPreferences.getInstance(this)
    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            ConfirmDialog(
                message = stringResource(msgResId),
                positiveButtonText = stringResource(R.string.settings),
                negativeButtonText = stringResource(R.string.cancel),
                onDismissRequest = { dialog.dismiss() },
                onConfirm = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${this.packageName}")
                    startActivity(intent)
                },
            )
        }
    }
    dialog.show()
}
