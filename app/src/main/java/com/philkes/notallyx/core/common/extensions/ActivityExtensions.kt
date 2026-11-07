package com.philkes.notallyx.core.common.extensions

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.FileType
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.SharedNote
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.main.MainActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun Activity.openNote(noteId: Long, noteType: Type, clearBackStack: Boolean = false) {
    val navKey =
        if (noteType == Type.NOTE) {
            NotallyNavKey.EditNote(noteId = noteId)
        } else {
            NotallyNavKey.EditList(noteId = noteId)
        }
    val intent =
        Intent(this, MainActivity::class.java).apply {
            putNavKey(navKey)
            putExtra(EXTRA_SELECTED_BASE_NOTE, noteId)
            putExtra("type", noteType.name)
            if (clearBackStack) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    startActivity(intent)
}

fun Intent.putNavKey(key: NotallyNavKey): Intent = apply {
    putExtra(MainActivity.EXTRA_ROUTE_TO_OPEN, Json.encodeToString(NotallyNavKey.serializer(), key))
}

fun Intent.getNavKey(): NotallyNavKey? {
    val json = getStringExtra(MainActivity.EXTRA_ROUTE_TO_OPEN) ?: return null
    return runCatching { Json.decodeFromString(NotallyNavKey.serializer(), json) }
        .getOrDefault(NotallyNavKey.Notes)
}

fun Context.getOpenNotePendingIntent(note: BaseNote) = getOpenNotePendingIntent(note.id, note.type)

fun Context.getOpenNoteIntent(noteId: Long, noteType: Type): Intent {
    val navKey =
        if (noteType == Type.NOTE) {
            NotallyNavKey.EditNote(noteId = noteId)
        } else {
            NotallyNavKey.EditList(noteId = noteId)
        }
    return Intent(this, MainActivity::class.java).apply {
        putNavKey(navKey)
        putExtra(EXTRA_SELECTED_BASE_NOTE, noteId)
        putExtra("type", noteType.name)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
    }
}

fun Context.getOpenNotePendingIntent(noteId: Long, noteType: Type): PendingIntent {
    return PendingIntent.getActivity(
        this,
        noteId.hashCode(),
        getOpenNoteIntent(noteId, noteType),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}

@Composable fun isPreview(): Boolean = LocalInspectionMode.current

fun Intent.generateBaseNote(context: ContextWrapper): SharedNote {
    val title = getStringExtra(Intent.EXTRA_SUBJECT) ?: data?.let { context.getFileName(it) } ?: ""
    val intentFiles =
        IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            ?: IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                listOf(it)
            }
    val text =
        (data?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    try {
                        inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        context.log(
                            tag = "",
                            msg = "Reading text contents from intent failed",
                            throwable = e,
                        )
                        null
                    }
                }
                    ?: run {
                        context.showToast(R.string.cant_load_file)
                        null
                    }
            } ?: getStringExtra(Intent.EXTRA_TEXT) ?: "")
            .let {
                if (it.length > MAX_BODY_CHAR_LENGTH) {
                    context.showToast(
                        context.getString(
                            R.string.note_text_too_long_truncated,
                            MAX_BODY_CHAR_LENGTH,
                        )
                    )
                }
                it.take(MAX_BODY_CHAR_LENGTH)
            }
    val (images, files) =
        intentFiles?.let {
            val filesByType = it.groupBy { uri ->
                context.getMimeType(uri)?.let { mimeType ->
                    if (mimeType.startsWith("image/")) {
                        FileType.IMAGE
                    } else {
                        FileType.ANY
                    }
                } ?: FileType.ANY
            }
            val images =
                filesByType[FileType.IMAGE]?.let { images ->
                    images.map { FileAttachment("", it.toString(), "") }
                } ?: listOf()
            val files =
                filesByType[FileType.ANY]?.let { otherFiles ->
                    otherFiles.map { FileAttachment("", it.toString(), "") }
                } ?: listOf()
            Pair(images, files)
        } ?: Pair(listOf(), listOf())
    return SharedNote(title, text, images, files = files)
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
