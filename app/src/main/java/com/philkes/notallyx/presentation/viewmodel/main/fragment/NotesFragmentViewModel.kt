package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.moveBaseNotes
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.SearchResult
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.presentation.activity.note.refreshStatusBarPin
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.showSnackbar
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.preference.BasePreference
import com.philkes.notallyx.presentation.viewmodel.progress.ExportNotesProgress
import com.philkes.notallyx.utils.ActionMode
import com.philkes.notallyx.utils.backup.exportPdfFile
import com.philkes.notallyx.utils.backup.exportPdfFileFolder
import com.philkes.notallyx.utils.backup.exportPlainTextFile
import com.philkes.notallyx.utils.backup.exportPlainTextFileFolder
import com.philkes.notallyx.utils.cancelPinAndReminders
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.toReadablePath
import com.philkes.notallyx.utils.viewFile
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFragmentViewModel(app: Application) : BaseViewModel(app) {

    lateinit var selectedExportMimeType: ExportMimeType

    val imageRoot
        get() = app.getCurrentImagesDirectory()

    val actionMode = ActionMode()
    val progress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    val folder: StateFlow<Folder>
        field = MutableStateFlow(Folder.NOTES)

    fun setFolder(folder: Folder) {
        this.folder.value = folder
    }

    var currentLabel: String? = CURRENT_LABEL_EMPTY

    var keyword = String()
        set(value) {
            if (field != value || searchResults?.value?.isEmpty() == true) {
                field = value
                searchResults?.fetch(keyword, folder.value, currentLabel)
            }
        }

    var searchResults: SearchResult? = null

    init {
        viewModelScope.launch {
            folder.collect { newFolder -> searchResults?.fetch(keyword, newFolder, currentLabel) }
        }
    }

    override fun initDatabase(database: NotallyDatabase?) {
        super.initDatabase(database)
        if (database == null) return
        val dao = baseNoteDao ?: return
        if (searchResults == null) {
            searchResults = SearchResult(app, viewModelScope, dao, app::createItemsFromNotes)
        } else {
            searchResults!!.baseNoteDao = dao
        }
    }

    fun moveBaseNotes(folder: Folder, callable: (() -> Unit)? = null): LongArray {
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(false)
        moveBaseNotes(ids, folder, callable)
        return ids
    }

    fun moveBaseNotes(ids: LongArray, folder: Folder, callable: (() -> Unit)? = null) {
        val dao = baseNoteDao ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                app.moveBaseNotes(dao, ids, folder)
            } finally {
                callable?.invoke()
            }
        }
    }

    fun pinBaseNotes(pinned: Boolean) {
        val dao = baseNoteDao ?: return
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(true)
        viewModelScope.launch(Dispatchers.IO) { dao.updatePinned(ids, pinned) }
    }

    fun pinBaseNotesToStatusBar(activity: Activity, pinnedToStatusBar: Boolean) {
        val dao = baseNoteDao ?: return
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(true)
        viewModelScope.launch {
            val updatedNotes =
                withContext(Dispatchers.IO) {
                    dao.updatePinnedToStatus(ids, pinnedToStatusBar)
                    dao.getByIds(ids)
                }
            updatedNotes.forEach { activity.refreshStatusBarPin(it) }
        }
    }

    fun colorBaseNote(color: String) {
        val dao = baseNoteDao ?: return
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(true)
        viewModelScope.launch(Dispatchers.IO) { dao.updateColor(ids, color) }
    }

    fun changeColor(oldColor: String, newColor: String) {
        val dao = baseNoteDao ?: return
        val defaultColor = preferences.defaultNoteColor.value
        if (oldColor == defaultColor) {
            preferences.defaultNoteColor.save(newColor)
        }
        viewModelScope.launch(Dispatchers.IO) { dao.updateColor(oldColor, newColor) }
    }

    fun updateBaseNoteLabels(labels: List<String>, id: Long) {
        val dao = baseNoteDao ?: return
        actionMode.close(true)
        viewModelScope.launch(Dispatchers.IO) { dao.updateLabels(id, labels) }
    }

    suspend fun deleteSelectedBaseNotes(): Collection<BaseNote> {
        return deleteBaseNotes(actionMode.selectedIds.toLongArray())
    }

    private suspend fun deleteBaseNotes(ids: LongArray): Collection<BaseNote> {
        val dao = baseNoteDao ?: return emptyList()
        val notes = withContext(Dispatchers.IO) { dao.getByIds(ids) }
        actionMode.close(false)
        app.cancelPinAndReminders(notes)
        return withContext(Dispatchers.IO) {
            dao.delete(ids)
            return@withContext notes
        }
    }

    suspend fun duplicateNotes(notes: Collection<BaseNote>): List<Long> {
        val dao = baseNoteDao ?: return emptyList()
        val now = System.currentTimeMillis()
        val copies: List<BaseNote> = notes.map { original ->
            original
                .deepCopy()
                .copy(
                    id = 0L,
                    title =
                        if (original.title.isNotEmpty())
                            "${original.title} (${app.getString(R.string.copy)})"
                        else app.getString(R.string.copy),
                    timestamp = now,
                    modifiedTimestamp = now,
                )
        }
        return withContext(Dispatchers.IO) { dao.insert(copies) }
    }

    fun duplicateSelectedBaseNotes() {
        if (actionMode.isEmpty()) return
        val selected = actionMode.selectedNotes.values.toList()
        viewModelScope.launch {
            duplicateNotes(selected)
            actionMode.close(true)
            app.showToast(app.getQuantityString(R.plurals.duplicates, selected.size))
        }
    }

    suspend fun getAllLabels() =
        withContext(Dispatchers.IO) { labelDao?.getArrayOfAll() ?: emptyArray() }

    fun saveNotes(notes: List<BaseNote>) {
        val dao = baseNoteDao ?: return
        viewModelScope.launch(Dispatchers.IO) { dao.insert(notes) }
    }

    fun <T> savePreference(preference: BasePreference<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) { preference.save(value) }
    }

    fun exportNoteToFile(fileUri: Uri, note: BaseNote, snackbarView: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            actionMode.close(true)
            app.showToast(R.string.something_went_wrong)
        }
        viewModelScope.launch(exceptionHandler) {
            when (selectedExportMimeType) {
                ExportMimeType.PDF -> {
                    exportPdfFile(
                        app,
                        note,
                        DocumentFile.fromSingleUri(app, fileUri)!!,
                        pdfPrintListener =
                            object : PdfPrintListener {
                                override fun onSuccess(file: DocumentFile) {
                                    actionMode.close(true)
                                    val message = app.getQuantityString(R.plurals.exported_notes, 1)
                                    snackbarView.showFileSnackbar(
                                        "$message to '${app.toReadablePath(fileUri)}'",
                                        fileUri,
                                        ExportMimeType.PDF,
                                    )
                                }

                                override fun onFailure(message: CharSequence?) {
                                    app.log(TAG, stackTrace = message as String?)
                                    actionMode.close(true)
                                }
                            },
                    )
                }
                else -> {
                    exportPlainTextFile(
                        app,
                        note,
                        DocumentFile.fromSingleUri(app, fileUri)!!,
                        selectedExportMimeType,
                    )
                    actionMode.close(true)
                    val message = app.getQuantityString(R.plurals.exported_notes, 1)
                    snackbarView.showFileSnackbar(
                        "$message to '${app.toReadablePath(fileUri)}'",
                        fileUri,
                        selectedExportMimeType,
                    )
                }
            }
        }
    }

    fun exportNotesToFolder(folderUri: Uri, notes: Collection<BaseNote>, snackbarView: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            actionMode.close(true)
            progress.value = ExportNotesProgress(inProgress = false)
            app.showToast(R.string.something_went_wrong)
        }
        viewModelScope.launch(exceptionHandler) {
            val counter = AtomicInteger(0)
            progress.value = ExportNotesProgress(total = notes.size)
            when (selectedExportMimeType) {
                ExportMimeType.PDF -> {
                    for (note in notes) {
                        exportPdfFileFolder(
                            app,
                            note,
                            DocumentFile.fromTreeUri(app, folderUri)!!,
                            progress = progress,
                            counter = counter,
                            total = notes.size,
                            pdfPrintListener =
                                object : PdfPrintListener {
                                    override fun onSuccess(file: DocumentFile) {
                                        actionMode.close(true)
                                        progress.value = ExportNotesProgress(inProgress = false)
                                        val message =
                                            app.getQuantityString(
                                                R.plurals.exported_notes,
                                                counter.get(),
                                            )
                                        snackbarView.showSnackbar(
                                            "$message to '${app.toReadablePath(folderUri)}'"
                                        )
                                    }

                                    override fun onFailure(message: CharSequence?) {
                                        app.log(TAG, stackTrace = message as String?)
                                        actionMode.close(true)
                                        progress.value = ExportNotesProgress(inProgress = false)
                                    }
                                },
                        )
                    }
                }
                else -> {
                    for (note in notes) {
                        exportPlainTextFileFolder(
                            app,
                            note,
                            selectedExportMimeType,
                            DocumentFile.fromTreeUri(app, folderUri)!!,
                            progress = progress,
                            counter = counter,
                            total = notes.size,
                        )
                    }
                    actionMode.close(true)
                    progress.value = ExportNotesProgress(inProgress = false)
                    val message = app.getQuantityString(R.plurals.exported_notes, counter.get())
                    snackbarView.showSnackbar("$message to '${app.toReadablePath(folderUri)}'")
                }
            }
        }
    }

    fun exportSelectedNotesToFolder(folderUri: Uri, snackbarView: View) {
        exportNotesToFolder(folderUri, actionMode.selectedNotes.values, snackbarView)
    }

    fun exportSelectedNoteToFile(fileUri: Uri, snackbarView: View) {
        exportNoteToFile(fileUri, actionMode.selectedNotes.values.first(), snackbarView)
    }

    private fun View.showFileSnackbar(msg: String, fileUri: Uri, mimeType: ExportMimeType) {
        showSnackbar(msg, R.string.open_link) { app.viewFile(fileUri, mimeType.mimeType) }
    }

    companion object {
        private const val TAG = "NotallyFragmentViewModel"

        const val CURRENT_LABEL_EMPTY = ""
        val CURRENT_LABEL_NONE: String? = null
    }
}
