package com.philkes.notallyx.feature.migration.backup

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.print.PdfPrintListener
import android.print.printPdf
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.createFileSafe
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.getExportedPath
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.extensions.toHtml
import com.philkes.notallyx.data.model.extensions.toJson
import com.philkes.notallyx.data.model.extensions.toMarkdown
import com.philkes.notallyx.data.model.extensions.toTxt
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.progress.BackupProgress
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun exportPdfFileFolder(
    app: ContextWrapper,
    note: BaseNote,
    folder: DocumentFile,
    fileName: String = note.title,
    pdfPrintListener: PdfPrintListener? = null,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    duplicateFileCount: Int = 1,
) {
    val validFileName = fileName.ifBlank { app.getString(R.string.note) }
    val filePath = "$validFileName.${ExportMimeType.PDF.fileExtension}"
    if (folder.findFile(filePath)?.exists() == true) {
        val duplicateFileName =
            findFreeDuplicateFileName(folder, validFileName, ExportMimeType.PDF.fileExtension)
        return exportPdfFileFolder(
            app,
            note,
            folder,
            duplicateFileName,
            pdfPrintListener,
            progress,
            counter,
            total,
            duplicateFileCount + 1,
        )
    }
    folder
        .createFileSafe(
            ExportMimeType.PDF.mimeType,
            validFileName,
            ".${ExportMimeType.PDF.fileExtension}",
        )
        .let { exportPdfFile(app, note, it, progress, counter, total, pdfPrintListener) }
}

fun exportPdfFile(
    app: ContextWrapper,
    note: BaseNote,
    outputFile: DocumentFile,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    pdfPrintListener: PdfPrintListener? = null,
) {
    val tempFile = DocumentFile.fromFile(File(app.getExportedPath(), "temp.pdf"))
    val html =
        note.toHtml(
            NotallyXPreferences.getInstance(app).showDateCreated(),
            app.getCurrentImagesDirectory(),
        )
    app.printPdf(
        tempFile,
        html,
        object : PdfPrintListener {
            override fun onSuccess(file: DocumentFile) {
                app.contentResolver.openOutputStream(outputFile.uri)?.use { outStream ->
                    app.contentResolver.openInputStream(file.uri)?.copyTo(outStream)
                }
                if (progress != null) {
                    progress.value =
                        BackupProgress(current = counter!!.incrementAndGet(), total = total!!)
                    if (counter.get() == total) {
                        pdfPrintListener?.onSuccess(file)
                    }
                } else {
                    pdfPrintListener?.onSuccess(file)
                }
            }

            override fun onFailure(message: CharSequence?) {
                pdfPrintListener?.onFailure(message)
            }
        },
    )
}

suspend fun exportPlainTextFileFolder(
    app: ContextWrapper,
    note: BaseNote,
    exportType: ExportMimeType,
    folder: DocumentFile,
    fileName: String = note.title,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    duplicateFileCount: Int = 1,
): DocumentFile? {
    val validFileName = fileName.ifBlank { app.getString(R.string.note) }
    if (folder.findFile("$validFileName.${exportType.fileExtension}")?.exists() == true) {
        val duplicateFileName =
            findFreeDuplicateFileName(folder, validFileName, exportType.fileExtension)
        return exportPlainTextFileFolder(
            app,
            note,
            exportType,
            folder,
            duplicateFileName,
            progress,
            counter,
            total,
            duplicateFileCount + 1,
        )
    }
    return withContext(Dispatchers.IO) {
        val file =
            folder
                .createFileSafe(exportType.mimeType, validFileName, ".${exportType.fileExtension}")
                .let {
                    exportPlainTextFile(app, note, it, exportType)
                    it
                }
        progress?.value = BackupProgress(current = counter!!.incrementAndGet(), total = total!!)
        return@withContext file
    }
}

fun exportPlainTextFile(
    app: ContextWrapper,
    note: BaseNote,
    outputFile: DocumentFile,
    exportType: ExportMimeType,
) {
    app.contentResolver.openOutputStream(outputFile.uri)?.use { stream ->
        OutputStreamWriter(stream).use { writer ->
            writer.write(
                when (exportType) {
                    ExportMimeType.TXT ->
                        note.toTxt(includeTitle = false, includeCreationDate = false)

                    ExportMimeType.JSON -> note.toJson()
                    ExportMimeType.HTML ->
                        note.toHtml(
                            NotallyXPreferences.getInstance(app).showDateCreated(),
                            app.getCurrentImagesDirectory(),
                        )

                    ExportMimeType.MD -> note.toMarkdown()
                    else -> TODO("Unsupported MimeType for Export: $exportType")
                }
            )
        }
    }
}

private fun findFreeDuplicateFileName(
    folder: DocumentFile,
    fileName: String,
    fileExtension: String,
): String {
    val existingNames = folder.listFiles().mapNotNull { it.name }.toSet()
    if ("$fileName.$fileExtension" !in existingNames) return fileName

    var index = 0
    var newName: String

    do {
        index++
        newName = "$fileName ($index).$fileExtension"
    } while (newName in existingNames)

    return "$fileName ($index)"
}

fun MainActivity.exportNotes(
    notes: Collection<BaseNote>,
    mimeType: ExportMimeType,
    exportToFileResultLauncher: ActivityResultLauncher<Intent>,
    exportToFolderResultLauncher: ActivityResultLauncher<Intent>,
) {
    if (notes.size == 1) {
        exportNote(notes.first(), mimeType, exportToFileResultLauncher)
    } else {
        lifecycleScope.launch {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .apply { addCategory(Intent.CATEGORY_DEFAULT) }
                    .wrapWithChooser(this@exportNotes)
            exportToFolderResultLauncher.launch(intent)
        }
    }
}

fun Activity.exportNote(
    note: BaseNote,
    mimeType: ExportMimeType,
    exportToFileResultLauncher: ActivityResultLauncher<Intent>,
) {
    val suggestedName =
        (note.title.ifBlank { getString(R.string.note) }) + "." + mimeType.fileExtension
    val intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .apply {
                type = mimeType.mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, suggestedName)
            }
            .wrapWithChooser(this)
    exportToFileResultLauncher.launch(intent)
}
