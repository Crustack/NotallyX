package com.philkes.notallyx.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.data.model.Attachment
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.isImage
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.progress.DeleteAttachmentProgress
import com.philkes.notallyx.presentation.widget.WidgetProvider
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.CRC32
import net.lingala.zip4j.ZipFile

private const val TAG = "IO"

const val SUBFOLDER_IMAGES = "Images"
const val SUBFOLDER_FILES = "Files"
const val SUBFOLDER_AUDIOS = "Audios"

fun ContextWrapper.getExternalImagesDirectory() = getExternalMediaDirectory(SUBFOLDER_IMAGES)

fun ContextWrapper.getExternalAudioDirectory() = getExternalMediaDirectory(SUBFOLDER_AUDIOS)

fun ContextWrapper.getExternalFilesDirectory() = getExternalMediaDirectory(SUBFOLDER_FILES)

fun ContextWrapper.getExternalMediaDirectory() = getExternalMediaDirectory("")

fun Context.getTempAudioFile(): File {
    return File(externalCacheDir, "Temp.m4a")
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

fun File.isAudioFile(context: Context): Boolean {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, toUri()) // Try to set the file path
        val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        mimeType != null && hasAudio != null ||
            duration != null // If it has audio metadata, it's a valid audio file
    } catch (e: Exception) {
        false // An exception means it’s not a valid audio file
    } finally {
        retriever.release() // Always release retriever to free resources
    }
}

fun File.toRelativePathFrom(baseFolderName: String): String {
    val parentFolderIndex = absolutePath.indexOf("/$baseFolderName/")
    if (parentFolderIndex == -1) {
        return name
    }
    val relativePath = absolutePath.substring(parentFolderIndex + baseFolderName.length + 2)
    return relativePath.trimStart(File.separatorChar)
}

fun File.recreateDir(): File {
    if (exists()) {
        deleteRecursively()
    }
    mkdirs()
    return this
}

private const val BUFFER_SIZE = 512 * 1024

fun File.copyToLarge(
    target: File,
    overwrite: Boolean = false,
    bufferSize: Int = BUFFER_SIZE,
): File {
    return copyTo(target = target, overwrite = overwrite, bufferSize = bufferSize)
}

fun InputStream.copyToLarge(target: OutputStream, bufferSize: Int = BUFFER_SIZE): Long {
    return copyTo(out = target, bufferSize = bufferSize)
}

fun ContextWrapper.deleteAttachments(
    attachments: Collection<Attachment>,
    ids: LongArray? = null,
    progress: MutableLiveData<Progress>? = null,
) {
    if (attachments.isNotEmpty()) {
        progress?.postValue(DeleteAttachmentProgress(0, attachments.size))
        val imageRoot = getExternalImagesDirectory()
        val audioRoot = getExternalAudioDirectory()
        val fileRoot = getExternalFilesDirectory()
        attachments.forEachIndexed { index, attachment ->
            val file =
                when (attachment) {
                    is Audio -> if (audioRoot != null) File(audioRoot, attachment.name) else null

                    is FileAttachment -> {
                        val root = if (attachment.isImage) imageRoot else fileRoot
                        if (root != null) File(root, attachment.localName) else null
                    }
                }
            if (file != null && file.exists()) {
                file.delete()
            }
            progress?.postValue(DeleteAttachmentProgress(index + 1, attachments.size))
        }
    }
    if (ids?.isNotEmpty() == true) {
        WidgetProvider.sendBroadcast(this, ids)
    }
    progress?.postValue(DeleteAttachmentProgress(inProgress = false))
}

fun Context.getBackupDir() = getEmptyFolder("backup")

fun Context.getExportedPath() = getEmptyFolder("exported")

fun ContextWrapper.getLogsDir() =
    getExternalMediaDirectory("logs") ?: File(filesDir, "logs").also { it.mkdir() }

const val APP_LOG_FILE_NAME = "notallyx-logs"

fun ContextWrapper.getLogFile(): File {
    return File(getLogsDir(), "$APP_LOG_FILE_NAME.txt")
}

private fun ContextWrapper.getExternalMediaDirectory(name: String): File? {
    return externalMediaDirs.firstOrNull()?.let { getDirectory(it, name) }
}

private fun getDirectory(dir: File, name: String): File? {
    var file: File? = null
    try {
        file = File(dir, name)
        if (file.exists()) {
            if (!file.isDirectory) {
                file.delete()
                file.createDirectory()
            }
        } else file.createDirectory()
    } catch (exception: Exception) {
        exception.printStackTrace()
    }

    return file
}

private fun File.createDirectory() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Files.createDirectory(toPath())
    } else mkdir()
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

fun File.listFilesRecursive(filter: FileFilter? = null): List<File> {
    val files = mutableListOf<File>()
    files.addAll(walkTopDown().filter { filter?.accept(it) ?: true })
    return files
}

const val MIME_TYPE_ZIP = "application/zip"
const val MIME_TYPE_JSON = "application/json"

class ZipVerificationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

fun ZipFile.verify(databaseFile: File) {
    if (!isValidZipFile) {
        throw ZipVerificationException("ZipFile '${file}' is not a valid ZIP!")
    }
    if (isEncrypted) {
        verifyEncryptedArchive()
    } else {
        verifyCrc(databaseFile)
    }
}

private fun ZipFile.verifyCrc(databaseFile: File) {
    val dbEntry =
        fileHeaders.find { it.fileName == DATABASE_NAME }
            ?: throw ZipVerificationException("Database file missing in ZipFile '${file}'")
    // Compare CRC
    val originalCrc = calculateCrc(File(databaseFile.path).inputStream()).value
    if (dbEntry.crc != originalCrc) {
        throw ZipVerificationException(
            "ZipFile '${file}' contains mismatched CRC for '${DATABASE_NAME}'!"
        )
    }

    for (fileHeader in fileHeaders) {
        // Directories have no CRC
        if (fileHeader.isDirectory || fileHeader == dbEntry) continue
        // Get expected CRC from ZIP metadata
        val expected = fileHeader.crc
        // Compute actual CRC
        val actual = calculateCrc(this.getInputStream(fileHeader)).value
        if (expected != actual) {
            throw ZipVerificationException(
                "ZipFile '${file}' contains mismatched CRC for '${fileHeader.fileName}'!"
            )
        }
    }
}

private fun calculateCrc(inputStream: InputStream): CRC32 =
    CRC32().apply {
        inputStream.use { input ->
            val buffer = ByteArray(1024 * 64)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                update(buffer, 0, read)
            }
        }
    }

private fun ZipFile.verifyEncryptedArchive() {
    for (fh in fileHeaders) {
        if (fh.isDirectory) continue
        try {
            getInputStream(fh).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break // fully read → MAC verified
                }
            }
        } catch (e: Exception) {
            throw ZipVerificationException(
                "ZipFile '${file}' contains corrupt encrypted files!",
                e,
            ) // wrong password or corrupted entry
        }
    }
}

fun Context.md5Hash(uri: Uri): ByteArray? {
    return contentResolver.openInputStream(uri)?.md5Hash()
}

fun File.md5Hash(): ByteArray {
    return inputStream().md5Hash()
}

fun InputStream.md5Hash(): ByteArray {
    val digest = MessageDigest.getInstance("MD5")
    use { fis ->
        val buffer = ByteArray(BUFFER_SIZE / 2)
        var bytes = fis.read(buffer)
        while (bytes != -1) {
            digest.update(buffer, 0, bytes)
            bytes = fis.read(buffer)
        }
    }
    return digest.digest()
}
