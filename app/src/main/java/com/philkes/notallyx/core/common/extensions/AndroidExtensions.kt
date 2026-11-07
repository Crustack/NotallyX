package com.philkes.notallyx.core.common.extensions

// import com.philkes.notallyx.BuildConfig
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.toText
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.feature.editor.EditListActivity
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.imports.ImportResult
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.serialization.json.Json

fun Context.copyToClipBoard(text: CharSequence) {
    ContextCompat.getSystemService(this, ClipboardManager::class.java)?.let {
        val clip = ClipData.newPlainText("label", text)
        it.setPrimaryClip(clip)
    }
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}
    .getOrNull()

fun Context.getFileName(uri: Uri): String? =
    when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let { File(it) }?.name
    }

fun Context.getUriForFile(file: File): Uri =
    FileProvider.getUriForFile(this, "${packageName}.provider", file)

fun Context.getMimeType(uri: Uri) = contentResolver.getType(uri)

fun Activity.openNote(noteId: Long, noteType: Type, clearBackStack: Boolean = false) {
    val activity =
        when (noteType) {
            Type.NOTE -> EditNoteActivity::class.java
            Type.LIST -> EditListActivity::class.java
        }
    val intent = Intent(this, activity)
    if (clearBackStack) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    intent.putExtra(EXTRA_SELECTED_BASE_NOTE, noteId)
    startActivity(intent)
}

fun Context.viewFile(uri: Uri, mimeType: String) {
    val intent =
        Intent(Intent.ACTION_VIEW)
            .apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .wrapWithChooser(this)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    startActivity(intent)
}

fun ContextWrapper.shareNote(note: BaseNote) {
    val body =
        when (note.type) {
            Type.NOTE -> note.body
            Type.LIST -> note.items.toMutableList().toText()
        }
    val filesUris =
        note.images
            .map { File(getCurrentImagesDirectory(), it.localName) }
            .map { getUriForFile(it) }
    shareNote(note.title, body, filesUris)
}

fun ContextWrapper.toMessage(importResult: ImportResult) =
    StringBuilder()
        .apply {
            append(getQuantityString(R.plurals.imported_notes, importResult.inserted))
            if (importResult.duplicates > 0) {
                append("\n")
                append(getQuantityString(R.plurals.duplicates, importResult.duplicates))
            }
            if (importResult.corruptedNotes > 0) {
                append("\n")
                append(
                    getQuantityString(
                        R.plurals.skipped_corrupted_notes,
                        importResult.corruptedNotes,
                    )
                )
            }
        }
        .toString()

private fun Context.shareNote(title: String, body: CharSequence, imageUris: List<Uri>) {
    val text = body.truncate(150_000)
    val intent =
        Intent(if (imageUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND)
            .apply {
                type = if (imageUris.isEmpty()) "text/*" else "image/*"
                putExtra(Intent.EXTRA_TEXT, text.toString())
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                if (imageUris.size > 1) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                } else if (imageUris.isNotEmpty()) {
                    putExtra(Intent.EXTRA_STREAM, imageUris.first())
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .wrapWithChooser(this)
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
    return when (noteType) {
        Type.NOTE -> Intent(this, EditNoteActivity::class.java)
        Type.LIST -> Intent(this, EditListActivity::class.java)
    }.apply {
        putExtra(EXTRA_SELECTED_BASE_NOTE, noteId)
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

fun Context.isSystemInDarkMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}

fun ContentResolver.determineMimeTypeAndExtension(uri: Uri, proposedMimeType: String?) =
    if (proposedMimeType != null && proposedMimeType.contains("/")) {
        Pair(proposedMimeType, ".${uri.lastPathSegment?.substringAfterLast(".")}")
    } else {
        val actualMimeType = getType(uri) ?: "application/octet-stream"
        Pair(
            actualMimeType,
            MimeTypeMap.getSingleton().getExtensionFromMimeType(actualMimeType)?.let { ".${it}" }
                ?: "",
        )
    }

fun Intent.wrapWithChooser(context: Context, titleResId: Int? = null): Intent =
    Intent.createChooser(this, titleResId?.let { context.getText(it) })

internal fun DocumentFile.isLargerThanKb(kilobytes: Long): Boolean {
    return (length() / 1024.0) > kilobytes
}

fun DocumentFile.listZipFiles(prefix: String): List<DocumentFile> {
    if (!this.isDirectory) return emptyList()
    val zipFiles =
        this.listFiles().filter { file ->
            file.isFile &&
                file.name?.endsWith(".zip", ignoreCase = true) == true &&
                file.name?.startsWith(prefix, ignoreCase = true) == true
        }
    return zipFiles.sortedWith(
        compareByDescending<DocumentFile> { it.name.orEmpty() }
            .thenByDescending { it.lastModified() }
    )
}

typealias DocumentFolder = DocumentFile

fun DocumentFolder.createFileSafe(
    mimeType: String,
    fileName: String,
    fileExtension: String,
): DocumentFile {
    var createdFile = createFile(mimeType, fileName)
    if (createdFile == null) {
        createdFile = createFile(mimeType, fileName + fileExtension)
    } else if (createdFile.name?.endsWith(fileExtension) == false) {
        createdFile.delete()
        createdFile = createFile(mimeType, fileName + fileExtension)
    }
    return requireNotNull(createdFile) {
        "Could not create '$fileName$fileExtension' in Folder '$name' (uri: '$uri')"
    }
}

val DocumentFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast(".")

@RequiresApi(Build.VERSION_CODES.O)
fun NotificationManager.createChannelIfNotExists(
    channelId: String,
    importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
) {
    if (getNotificationChannel(channelId) == null) {
        val channel = NotificationChannel(channelId, channelId, importance)
        createNotificationChannel(channel)
    }
}

fun Context.getDocumentFolder(uri: Uri): DocumentFile? {
    return try {
        if (uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == null) {
            val path = uri.path ?: uri.toString()
            DocumentFile.fromFile(File(path))
        } else if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            if (uri.pathSegments.firstOrNull() == "tree") {
                DocumentFile.fromTreeUri(this, uri)
            } else {
                DocumentFile.fromSingleUri(this, uri)
            }
        } else {
            DocumentFile.fromFile(File(uri.toString()))
        }
    } catch (_: Exception) {
        null
    }
}

fun Context.toReadablePath(uri: Uri): String {
    if (uri.authority == "com.android.externalstorage.documents") {
        return toReadable(uri.path!!)
    }

    val documentFile =
        try {
            getDocumentFolder(uri)
        } catch (_: Exception) {
            null
        }
    return if (documentFile != null && !uri.authority.isNullOrEmpty()) {
        "${uri.authority}/${documentFile.name}"
    } else {
        uri.path?.let { toReadable(it) } ?: documentFile?.name ?: uri.toString()
    }
}

private fun toReadable(uriPath: String): String {
    return uriPath
        .replaceFirst("/tree/primary:", "Internal Storage/")
        .replace("^/tree/([^:]+):/?".toRegex(), "External Storage/$1/")
        .replaceFirst("/storage/emulated/0/", "Internal Storage/")
}

private val isBeforeVanillaIceCream
    get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/** Source: https://stackoverflow.com/a/79286411/9748566 */
@Suppress("Deprecation")
fun Activity.changeStatusAndNavigationBarColor(@ColorInt color: Int) {
    window.statusBarColor = color
    window.navigationBarColor = color
    if (!isBeforeVanillaIceCream) {
        window.decorView.setBackgroundColor(color)
        window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        window.insetsController?.show(android.view.WindowInsets.Type.statusBars())
    }
}

fun ContentResolver.readFileContents(uri: Uri) =
    openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader -> reader.readText() }
    } ?: ""

fun Context.getDefaultSharedPreferences(): android.content.SharedPreferences =
    getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

@Composable fun isPreview(): Boolean = LocalInspectionMode.current
