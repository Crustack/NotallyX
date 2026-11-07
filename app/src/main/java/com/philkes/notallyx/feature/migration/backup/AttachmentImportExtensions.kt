package com.philkes.notallyx.feature.migration.backup

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.copyToFile
import com.philkes.notallyx.core.common.extensions.determineMimeTypeAndExtension
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentFilesDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.getFileName
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.mimeTypeToFileExtension
import com.philkes.notallyx.core.common.extensions.rename
import com.philkes.notallyx.core.common.result.FileError
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.editor.NoteModel.FileType
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AttachmentImportExtensions"

suspend fun Context.importFile(
    uri: Uri,
    directory: File,
    fileType: FileType,
    errorWhileRenaming: Int = R.string.error_while_renaming_file,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    return withContext(Dispatchers.IO) {
        val document =
            requireNotNull(
                DocumentFile.fromSingleUri(this@importFile, uri),
                { "importFile: could not read file from: '$uri'" },
            )
        val displayName = document.name ?: getString(R.string.unknown_name)
        try {
            /*
            If we have reached this point, an SD card (emulated or real) exists and externalRoot
            is not null. externalRoot.exists() can be false if the folder `Images` has been deleted after
            the previous line, but externalRoot itself can't be null
            */
            val temp = File(directory, "Temp")

            val inputStream =
                requireNotNull(
                    contentResolver.openInputStream(uri),
                    { "importFile: InputStream for '$uri' is null" },
                )
            inputStream.copyToFile(temp)

            val originalName = getFileName(uri)
            when (fileType) {
                FileType.IMAGE -> {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(temp.path, options)
                    val mimeType = options.outMimeType ?: proposedMimeType

                    if (mimeType != null) {
                        val extension = mimeType.mimeTypeToFileExtension()
                        if (extension != null) {
                            val name = "${UUID.randomUUID()}.$extension"
                            if (temp.rename(name)) {
                                return@withContext Pair(
                                    FileAttachment(name, originalName ?: name, mimeType),
                                    null,
                                )
                            } else {
                                // I don't expect this error to ever happen but just in case
                                return@withContext Pair(
                                    null,
                                    FileError(displayName, getString(errorWhileRenaming), fileType),
                                )
                            }
                        } else
                            return@withContext Pair(
                                null,
                                FileError(
                                    displayName,
                                    getString(R.string.image_format_not_supported),
                                    fileType,
                                ),
                            )
                    } else
                        return@withContext Pair(
                            null,
                            FileError(displayName, getString(R.string.invalid_image), fileType),
                        )
                }

                FileType.ANY -> {
                    val (mimeType, fileExtension) =
                        contentResolver.determineMimeTypeAndExtension(uri, proposedMimeType)
                    val name = "${UUID.randomUUID()}${fileExtension}"
                    if (temp.rename(name)) {
                        return@withContext Pair(
                            FileAttachment(name, originalName ?: name, mimeType),
                            null,
                        )
                    } else {
                        // I don't expect this error to ever happen but just in case
                        return@withContext Pair(
                            null,
                            FileError(displayName, getString(errorWhileRenaming), fileType),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (this is ContextWrapper) {
                log(TAG, throwable = e)
            } else {
                Log.e(TAG, "Import file failed", e)
            }
            return@withContext Pair(
                null,
                FileError(displayName, getString(R.string.unknown_error), fileType),
            )
        }
    }
}

suspend fun ContextWrapper.importFile(
    uri: Uri,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    val filesRoot = getCurrentFilesDirectory()
    return importFile(uri, filesRoot, FileType.ANY, proposedMimeType = proposedMimeType)
}

suspend fun ContextWrapper.importImage(
    uri: Uri,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    val imagesRoot = getCurrentImagesDirectory()
    return importFile(uri, imagesRoot, FileType.IMAGE, proposedMimeType = proposedMimeType)
}

suspend fun ContextWrapper.importAudio(original: File, deleteOriginalFile: Boolean): Audio {
    return withContext(Dispatchers.IO) {
        /*
        Regenerate because the directory may have been deleted between the time of activity creation
        and audio recording
        */
        val audioRoot = getCurrentAudioDirectory()

        /*
        If we have reached this point, an SD card (emulated or real) exists and audioRoot
        is not null. audioRoot.exists() can be false if the folder `Audio` has been deleted after
        the previous line, but audioRoot itself can't be null
        */
        val name = "${UUID.randomUUID()}.m4a"
        val final = File(audioRoot, name)
        val input = FileInputStream(original)
        input.copyToFile(final)

        if (deleteOriginalFile) {
            original.delete()
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(final.path)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        Audio(name, duration?.toLong(), System.currentTimeMillis())
    }
}
