package com.philkes.notallyx.feature.main.routes.notes

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.toReadablePath
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.BasePreference
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.showSnackbar
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.dao.moveBaseNotes
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.SearchResult
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.exportPdfFile
import com.philkes.notallyx.feature.migration.backup.exportPdfFileFolder
import com.philkes.notallyx.feature.migration.backup.exportPlainTextFile
import com.philkes.notallyx.feature.migration.backup.exportPlainTextFileFolder
import com.philkes.notallyx.feature.migration.progress.ExportNotesProgress
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesRouteViewModel(app: Application, preferences: NotallyXPreferences) :
    BaseViewModel(app, preferences) {

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

    val keywordFlow = MutableStateFlow("")
    var keyword: String
        get() = keywordFlow.value
        set(value) {
            if (keywordFlow.value != value || searchResults?.value?.isEmpty() == true) {
                keywordFlow.value = value
                searchResults?.fetch(value, folder.value, currentLabel)
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
        @Suppress("UNNECESSARY_SAFE_CALL") val currentFolder = folder?.value ?: Folder.NOTES
        @Suppress("UNNECESSARY_SAFE_CALL") val currentKeyword = keywordFlow?.value ?: ""
        if (searchResults == null) {
            searchResults = SearchResult(app, viewModelScope, dao, app::createItemsFromNotes)
            searchResults!!.fetch(currentKeyword, currentFolder, currentLabel)
        } else {
            searchResults!!.baseNoteDao = dao
            searchResults!!.fetch(currentKeyword, currentFolder, currentLabel)
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

    fun updateBaseNotesLabels(notesLabels: List<Pair<Long, List<String>>>) {
        val dao = baseNoteDao ?: return
        actionMode.close(true)
        viewModelScope.launch(Dispatchers.IO) {
            notesLabels.forEach { (id, labels) -> dao.updateLabels(id, labels) }
        }
    }

    fun updateBaseNoteLabels(labels: List<String>, id: Long) {
        updateBaseNotesLabels(listOf(id to labels))
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

    fun selectAll() {
        val dao = baseNoteDao ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentNotes: List<BaseNote> =
                when {
                    keywordFlow.value.isNotEmpty() ->
                        searchResults?.results?.value?.filterIsInstance<BaseNote>() ?: emptyList()
                    currentLabel == CURRENT_LABEL_NONE -> dao.getWithoutLabel(Folder.NOTES).first()
                    !currentLabel.isNullOrEmpty() -> dao.getByLabel(currentLabel!!).first()
                    else -> dao.getByFolder(folder.value).first()
                }
            withContext(Dispatchers.Main) { actionMode.add(currentNotes) }
        }
    }

    fun togglePinSelectedNotes() {
        val baseNotes = actionMode.selectedNotes.values
        val allPinned = baseNotes.isNotEmpty() && baseNotes.all { it.pinned }
        pinBaseNotes(!allPinned)
    }

    fun onLabelsClick(
        onNavigateToLabels: () -> Unit,
        onShowDialog: (Array<String>, Collection<BaseNote>) -> Unit,
    ) {
        val baseNotes = actionMode.selectedNotes.values.toList()
        viewModelScope.launch {
            val labels = getAllLabels()
            if (labels.isNotEmpty()) {
                onShowDialog(labels, baseNotes)
            } else {
                actionMode.close(true)
                onNavigateToLabels()
            }
        }
    }

    fun onChangeColorClick(onShowDialog: (colors: Set<String>, currentColor: String?) -> Unit) {
        val dao = baseNoteDao ?: return
        viewModelScope.launch {
            val colors = withContext(Dispatchers.IO) { dao.getAllColors().toSet() }
            val currentColor =
                actionMode.selectedNotes.values
                    .map { it.color }
                    .distinct()
                    .takeIf { it.size == 1 }
                    ?.firstOrNull()
            onShowDialog(colors, currentColor)
        }
    }

    fun onPinToStatusClick(activity: Activity, onRequestPermission: () -> Unit) {
        val baseNotes = actionMode.selectedNotes.values
        val anyNotPinned = baseNotes.any { !it.isPinnedToStatus }
        if (anyNotPinned) {
            onRequestPermission()
        } else {
            pinBaseNotesToStatusBar(activity, false)
        }
    }

    private fun View.showFileSnackbar(msg: String, fileUri: Uri, mimeType: ExportMimeType) {
        showSnackbar(msg, R.string.open_link) { app.viewFile(fileUri, mimeType.mimeType) }
    }

    companion object {
        private const val TAG = "NotallyRouteViewModel"

        const val CURRENT_LABEL_EMPTY = ""
        val CURRENT_LABEL_NONE: String? = null
    }
}
