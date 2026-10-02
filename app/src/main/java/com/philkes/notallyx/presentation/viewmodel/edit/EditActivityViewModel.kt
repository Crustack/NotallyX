package com.philkes.notallyx.presentation.viewmodel.edit

import android.app.Application
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.showSnackbar
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.executeAsyncWithCallback
import com.philkes.notallyx.utils.backup.exportPdfFile
import com.philkes.notallyx.utils.backup.exportPlainTextFile
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.toReadablePath
import com.philkes.notallyx.utils.viewFile
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditActivityViewModel(app: Application) : NoteModel(app) {

    private val labelDao: LabelDao by lazy { database.value!!.getLabelDao() }
    private val commonDao: CommonDao by lazy { database.value!!.getCommonDao() }

    var selectedExportMimeType: ExportMimeType = ExportMimeType.TXT

    val allLabels: LiveData<List<Label>> by lazy { labelDao.getAll() }

    fun exportNoteToFile(fileUri: Uri, note: BaseNote, snackbarView: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
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
                                    val message = app.getQuantityString(R.plurals.exported_notes, 1)
                                    snackbarView.showFileSnackbar(
                                        "$message to '${app.toReadablePath(fileUri)}'",
                                        fileUri,
                                        ExportMimeType.PDF,
                                    )
                                }

                                override fun onFailure(message: CharSequence?) {
                                    app.log(TAG, stackTrace = message as String?)
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

    private fun View.showFileSnackbar(msg: String, fileUri: Uri, mimeType: ExportMimeType) {
        showSnackbar(msg, R.string.open_link) { app.viewFile(fileUri, mimeType.mimeType) }
    }

    fun changeColor(oldColor: String, newColor: String) {
        val defaultColor = preferences.defaultNoteColor.value
        if (oldColor == defaultColor) {
            preferences.defaultNoteColor.save(newColor)
        }
        viewModelScope.launch(Dispatchers.IO) { baseNoteDao?.updateColor(oldColor, newColor) }
    }

    suspend fun duplicateNote(note: BaseNote) = duplicateNotes(listOf(note)).first()

    suspend fun duplicateNotes(notes: Collection<BaseNote>): List<Long> {
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
        return withContext(Dispatchers.IO) { baseNoteDao!!.insert(copies) }
    }

    suspend fun getAllLabels(): Array<String> =
        withContext(Dispatchers.IO) { labelDao.getArrayOfAll() }

    fun insertLabel(label: String, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback(
            { labelDao.insert(Label(label, (labelDao.getMaxOrder() ?: -1) + 1)) },
            onComplete,
        )

    fun updateLabel(oldValue: String, newValue: String, onComplete: (success: Boolean) -> Unit) {
        executeAsyncWithCallback({ commonDao.updateLabel(oldValue, newValue) }, onComplete)
        val labelsHiddenPreference = preferences.labelsHidden
        val labelsHidden = labelsHiddenPreference.value.toMutableSet()
        if (labelsHidden.contains(oldValue)) {
            labelsHidden.remove(oldValue)
            labelsHidden.add(newValue)
            viewModelScope.launch(Dispatchers.IO) { labelsHiddenPreference.save(labelsHidden) }
        }
    }

    companion object {
        private const val TAG = "EditActivityViewModel"
    }
}
