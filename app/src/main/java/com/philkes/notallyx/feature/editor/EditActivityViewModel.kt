package com.philkes.notallyx.feature.editor

import android.app.Application
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.toReadablePath
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.showSnackbar
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.exportPdfFile
import com.philkes.notallyx.feature.migration.backup.exportPlainTextFile
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    object Loading : UiState

    object Success : UiState
}

class EditActivityViewModel(app: Application, preferences: NotallyXPreferences) :
    NoteModel(app, preferences) {

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    var selectedExportMimeType: ExportMimeType = ExportMimeType.TXT

    fun changeUiState(uiState: UiState) {
        this.uiState = uiState
    }

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

    companion object {
        private const val TAG = "EditActivityViewModel"
    }
}
