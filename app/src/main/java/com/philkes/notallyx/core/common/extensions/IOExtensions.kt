package com.philkes.notallyx.core.common.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.Attachment
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.extensions.isImage
import com.philkes.notallyx.feature.migration.progress.DeleteAttachmentProgress
import com.philkes.notallyx.feature.widget.NoteWidget
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLConnection
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "IO"

const val SUBFOLDER_IMAGES = "Images"
const val SUBFOLDER_FILES = "Files"
const val SUBFOLDER_AUDIOS = "Audios"
const val SUBFOLDER_CRASHES = "Crashes"
const val SUBFOLDER_BACKUPS = "Backups"

private fun Context.getExternalImagesDirectory() = getExternalMediaDirectory(SUBFOLDER_IMAGES)

private fun Context.getExternalAudioDirectory() = getExternalMediaDirectory(SUBFOLDER_AUDIOS)

private fun Context.getExternalFilesDirectory() = getExternalMediaDirectory(SUBFOLDER_FILES)

fun Context.getExternalCrashesDirectory() = getExternalMediaDirectory(SUBFOLDER_CRASHES)

fun Context.getExternalBackupsDirectory() = getExternalMediaDirectory(SUBFOLDER_BACKUPS)

// Private (internal) storage roots for attachments when biometric lock is enabled and
// dataInPublicFolder is disabled.
fun Context.getPrivateAttachmentsRoot(): File {
    val root = File(filesDir, "attachments")
    if (!root.exists()) root.mkdir()
    return root
}

private fun Context.getPrivateImagesDirectory(): File {
    val dir = File(getPrivateAttachmentsRoot(), SUBFOLDER_IMAGES)
    if (!dir.exists()) dir.mkdir()
    return dir
}

private fun Context.getPrivateFilesDirectory(): File {
    val dir = File(getPrivateAttachmentsRoot(), SUBFOLDER_FILES)
    if (!dir.exists()) dir.mkdir()
    return dir
}

private fun Context.getPrivateAudioDirectory(): File {
    val dir = File(getPrivateAttachmentsRoot(), SUBFOLDER_AUDIOS)
    if (!dir.exists()) dir.mkdir()
    return dir
}

private fun Context.isDataInPublicEnabled(): Boolean {
    return runCatching { NotallyXPreferences.getInstance(this).dataInPublicFolder.value }
        .getOrDefault(false)
}

fun Context.getCurrentImagesDirectory(): File {
    return if (isDataInPublicEnabled()) getExternalImagesDirectory()
    else getPrivateImagesDirectory()
}

fun Context.getCurrentFilesDirectory(): File {
    return if (isDataInPublicEnabled()) getExternalFilesDirectory() else getPrivateFilesDirectory()
}

fun Context.getCurrentAudioDirectory(): File {
    return if (isDataInPublicEnabled()) getExternalAudioDirectory() else getPrivateAudioDirectory()
}

fun Context.getCurrentMediaRoot(): File {
    return if (isDataInPublicEnabled()) getExternalMediaDirectory() else getPrivateAttachmentsRoot()
}

private fun Context.getAlternateImagesDirectory(): File {
    return if (isDataInPublicEnabled()) getPrivateImagesDirectory()
    else getExternalImagesDirectory()
}

private fun Context.getAlternateFilesDirectory(): File {
    return if (isDataInPublicEnabled()) getPrivateFilesDirectory() else getExternalFilesDirectory()
}

private fun Context.getAlternateAudioDirectory(): File {
    return if (isDataInPublicEnabled()) getPrivateAudioDirectory() else getExternalAudioDirectory()
}

/**
 * Resolve an attachment file location by checking the current-mode directory first, then fallback.
 */
fun Context.resolveAttachmentFile(subfolder: String, localName: String): File? {
    val current =
        when (subfolder) {
            SUBFOLDER_IMAGES -> getCurrentImagesDirectory()
            SUBFOLDER_FILES -> getCurrentFilesDirectory()
            SUBFOLDER_AUDIOS -> getCurrentAudioDirectory()
            else -> null
        }
    val alt =
        when (subfolder) {
            SUBFOLDER_IMAGES -> getAlternateImagesDirectory()
            SUBFOLDER_FILES -> getAlternateFilesDirectory()
            SUBFOLDER_AUDIOS -> getAlternateAudioDirectory()
            else -> null
        }
    val inCurrent = current?.let { File(it, localName) }
    if (inCurrent != null && inCurrent.exists()) return inCurrent
    val inAlt = alt?.let { File(it, localName) }
    if (inAlt != null && inAlt.exists()) return inAlt
    return inCurrent ?: inAlt
}

/**
 * Move all attachment files between public and private storage to match current mode. If
 * [toPrivate] is true, move from external app media to private dirs; else the opposite.
 *
 * @return Pair of moved and failed file counts
 */
fun Context.migrateAllAttachments(toPrivate: Boolean): Pair<Int, Int> {
    var moved = 0
    var failed = 0
    val sources = listOf(SUBFOLDER_IMAGES, SUBFOLDER_FILES, SUBFOLDER_AUDIOS)
    sources.forEach { sub ->
        val (srcRoot, dstRoot) =
            if (toPrivate) {
                val src =
                    when (sub) {
                        SUBFOLDER_IMAGES -> getExternalImagesDirectory()
                        SUBFOLDER_FILES -> getExternalFilesDirectory()
                        SUBFOLDER_AUDIOS -> getExternalAudioDirectory()
                        else -> null
                    }
                val dst =
                    when (sub) {
                        SUBFOLDER_IMAGES -> getPrivateImagesDirectory()
                        SUBFOLDER_FILES -> getPrivateFilesDirectory()
                        SUBFOLDER_AUDIOS -> getPrivateAudioDirectory()
                        else -> null
                    }
                Pair(src, dst)
            } else {
                val src =
                    when (sub) {
                        SUBFOLDER_IMAGES -> getPrivateImagesDirectory()
                        SUBFOLDER_FILES -> getPrivateFilesDirectory()
                        SUBFOLDER_AUDIOS -> getPrivateAudioDirectory()
                        else -> null
                    }
                val dst =
                    when (sub) {
                        SUBFOLDER_IMAGES -> getExternalImagesDirectory()
                        SUBFOLDER_FILES -> getExternalFilesDirectory()
                        SUBFOLDER_AUDIOS -> getExternalAudioDirectory()
                        else -> null
                    }
                Pair(src, dst)
            }
        if (srcRoot == null || dstRoot == null) return@forEach
        srcRoot.listFiles()?.forEach { file ->
            try {
                val target = File(dstRoot, file.name)
                file.copyTo(target, overwrite = true)
                if (file.delete()) {
                    moved++
                } else {
                    // try overwrite move on legacy devices
                    //                    if (file.renameTo(target)) moved++ else failed++
                    failed++
                }
            } catch (t: Throwable) {
                Log.e(
                    TAG,
                    "Failed to move '${file.absolutePath}' to ${if(toPrivate) "private" else "public"} folder '${dstRoot.absolutePath}'",
                    t,
                )
                failed++
            }
        }
    }
    return Pair(moved, failed)
}

fun Context.getTempAudioFile(): File {
    return File(externalCacheDir, "Temp.m4a")
}

fun Context.openExternalMediaFolder(folderFile: File) {
    if (!folderFile.exists()) {
        folderFile.mkdirs()
    }
    try {
        viewFile(getUriForFile(folderFile), "vnd.android.document/directory")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open external media folder", e)
        showToast(com.philkes.notallyx.R.string.cant_open_link)
    }
}

fun InputStream.copyToFile(destination: File) {
    val output = FileOutputStream(destination)
    copyToLarge(output)
    close()
    output.close()
    Log.d(TAG, "Copied InputStream to '${destination.absolutePath}'")
}

fun File.write(bytes: ByteArray) {
    outputStream().use { outputStream -> outputStream.write(bytes) }
    Log.d(TAG, "Wrote ${bytes.size} bytes to '${this.absolutePath}'")
}

fun File.rename(newName: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val source = toPath()
        val destination = source.resolveSibling(newName)
        Files.move(source, destination)
        true // If move failed, an exception would have been thrown
    } else {
        val destination = resolveSibling(newName)
        renameTo(destination)
    }
}

fun File.clearDirectory() {
    val files = listFiles()
    if (files != null) {
        for (file in files) {
            file.delete()
        }
    }
}

fun File.decodeToBitmap(): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    return BitmapFactory.decodeFile(absolutePath, options)
}

internal const val BUFFER_SIZE = 512 * 1024

fun File.copyToLarge(
    target: File,
    overwrite: Boolean = false,
    bufferSize: Int = BUFFER_SIZE,
    deleteSourceFile: Boolean = false,
): File {
    return copyTo(target = target, overwrite = overwrite, bufferSize = bufferSize).also {
        if (deleteSourceFile) {
            try {
                if (!this.delete()) {
                    Log.w(
                        TAG,
                        "Failed to delete file: '${this.absolutePath}' after copying to '${target.absolutePath}'",
                    )
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Failed to delete file: '${this.absolutePath}' after copying to '${target.absolutePath}'",
                    e,
                )
            }
        }
    }
}

fun File.moveAllFiles(to: File) {
    if (!exists() || !isDirectory) return

    if (!to.exists()) {
        to.mkdirs()
    }

    listFiles()?.forEach { file ->
        val targetFile = File(to, file.name)
        file.renameTo(targetFile)
    }
}

fun InputStream.copyToLarge(target: OutputStream, bufferSize: Int = BUFFER_SIZE): Long {
    return copyTo(out = target, bufferSize = bufferSize)
}

fun Context.deleteAttachments(
    notes: Collection<BaseNote>,
    progress: StateFlow<Progress?>? = null,
) {
    val attachments = ArrayList<Attachment>()
    notes.forEach { note ->
        attachments.addAll(note.images)
        attachments.addAll(note.files)
        attachments.addAll(note.audios)
    }
    deleteAttachments(attachments, notes.map { it.id }.toLongArray(), progress)
}

fun Context.deleteAttachments(
    attachments: Collection<Attachment>,
    ids: LongArray? = null,
    progress: StateFlow<Progress?>? = null,
) {
    if (attachments.isNotEmpty()) {
        (progress as? MutableStateFlow<Progress?>)?.value =
            DeleteAttachmentProgress(total = attachments.size)
        attachments.forEachIndexed { index, attachment ->
            val file =
                when (attachment) {
                    is Audio -> resolveAttachmentFile(SUBFOLDER_AUDIOS, attachment.name)

                    is FileAttachment -> {
                        val subfolder =
                            if (attachment.isImage) SUBFOLDER_IMAGES else SUBFOLDER_FILES
                        resolveAttachmentFile(subfolder, attachment.localName)
                    }
                }
            if (file != null && file.exists()) {
                file.delete()
            }
            (progress as? MutableStateFlow<Progress?>)?.value =
                DeleteAttachmentProgress(index + 1, attachments.size)
        }
    }
    if (ids?.isNotEmpty() == true) {
        NoteWidget.sendBroadcast(this, ids)
    }
    (progress as? MutableStateFlow<Progress?>)?.value = DeleteAttachmentProgress(inProgress = false)
}

fun Context.getBackupDir() = getEmptyFolder("backup")

fun Context.getExportedPath() = getEmptyFolder("exported")

fun Context.getLogsDir() =
    try {
        getExternalMediaDirectory("logs")
    } catch (_: Exception) {
        File(filesDir, "logs").also { it.mkdir() }
    }

const val APP_LOG_FILE_NAME = "notallyx-logs"

fun Context.getLogFile(): File {
    return File(getLogsDir(), "$APP_LOG_FILE_NAME.txt")
}

fun Context.getExternalMediaDirectory(name: String = ""): File {
    return getDirectory(
        requireNotNull(externalMediaDirs.firstOrNull()) {
            "External media directory does not exist"
        },
        name,
    )
}

fun Context.keepOnlyNewest(parentDir: File, keepCount: Int) {
    if (!parentDir.exists() || !parentDir.isDirectory) return
    val folders = parentDir.listFiles()?.sortedByDescending { it.name } ?: return
    folders
        .drop(keepCount)
        .takeIf { it.isNotEmpty() }
        ?.let { foldersToDelete ->
            log(
                "IOExtensions",
                msg =
                    "Keeping only $keepCount latest in '${parentDir.name}', therefore deleting ${foldersToDelete.size} oldest: ${
                        foldersToDelete.joinToString("', '")
                    }",
            )
            foldersToDelete.forEach { it.deleteRecursively() }
        }
}

private fun getDirectory(dir: File, name: String): File {
    val file = File(dir, name)
    if (file.exists()) {
        if (!file.isDirectory) {
            file.delete()
            file.createDirectory()
        }
    } else file.createDirectory()
    return file
}

private fun File.createDirectory() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            Files.createDirectories(toPath())
        } catch (e: FileAlreadyExistsException) {
            if (!isDirectory)
                throw IOException(
                    "Creating directory '${toPath()}' did not work because a file with that name already exists and was not properly deleted",
                    e,
                )
        }
    } else mkdirs()
}

private fun Context.getEmptyFolder(name: String): File {
    val folder = File(cacheDir, name)
    if (folder.exists()) {
        folder.clearDirectory()
    } else folder.mkdir()
    return folder
}

fun String.mimeTypeToFileExtension(): String? {
    return when (this) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        else -> null
    }
}

fun String.getMimeType(): String? {
    return URLConnection.guessContentTypeFromName(this)
}

fun File.listFilesRecursive(filter: FileFilter? = null): List<File> {
    val files = mutableListOf<File>()
    files.addAll(walkTopDown().filter { filter?.accept(it) ?: true })
    return files
}
