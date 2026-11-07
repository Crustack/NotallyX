package com.philkes.notallyx.core.ui.extensions

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.migration.ExportMimeType

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

fun Activity.exportNotes(
    notes: Collection<BaseNote>,
    mimeType: ExportMimeType,
    exportToFileResultLauncher: ActivityResultLauncher<Intent>,
    exportToFolderResultLauncher: ActivityResultLauncher<Intent>,
) {
    if (notes.size == 1) {
        exportNote(notes.first(), mimeType, exportToFileResultLauncher)
    } else {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .apply { addCategory(Intent.CATEGORY_DEFAULT) }
                .wrapWithChooser(this)
        exportToFolderResultLauncher.launch(intent)
    }
}
