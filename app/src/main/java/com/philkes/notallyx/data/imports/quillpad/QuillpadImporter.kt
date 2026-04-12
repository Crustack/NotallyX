package com.philkes.notallyx.data.imports.quillpad

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.philkes.notallyx.R
import com.philkes.notallyx.data.imports.ExternalImporter
import com.philkes.notallyx.data.imports.ImportException
import com.philkes.notallyx.data.imports.ImportProgress
import com.philkes.notallyx.data.imports.ImportStage
import com.philkes.notallyx.data.imports.markdown.parseBodyAndSpansFromMarkdown
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.utils.getMimeType
import com.philkes.notallyx.utils.moveAllFiles
import com.philkes.notallyx.utils.toMillis
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.collections.forEach
import kotlin.collections.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class QuillpadImporter : ExternalImporter {

    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    override fun import(
        app: Application,
        source: Uri,
        destination: File,
        progress: MutableLiveData<ImportProgress>?,
    ): Pair<List<BaseNote>, File> {
        progress?.postValue(ImportProgress(indeterminate = true, stage = ImportStage.EXTRACT_FILES))
        val dataFolder =
            try {
                app.contentResolver.openInputStream(source)!!.use { unzip(destination, it) }
            } catch (e: Exception) {
                throw ImportException(R.string.invalid_quillpad, e)
            }

        val mediaFolder = File(dataFolder, "media")
        if (mediaFolder.exists() && mediaFolder.isDirectory) {
            mediaFolder.moveAllFiles(dataFolder)
        }

        val backupFile = File(dataFolder, "backup.json")
        if (!backupFile.exists()) {
            throw ImportException(
                R.string.invalid_quillpad,
                RuntimeException("backup.json not found in ZIP"),
            )
        }

        val quillpadBackup =
            try {
                json.decodeFromString<QuillpadBackup>(backupFile.readText())
            } catch (e: Exception) {
                throw ImportException(R.string.invalid_quillpad, e)
            }

        val notebookMap = quillpadBackup.notebooks.associate { it.id to it.name }
        val total = quillpadBackup.notes.size
        progress?.postValue(ImportProgress(0, total, stage = ImportStage.IMPORT_NOTES))
        var counter = 1

        val baseNotes =
            quillpadBackup.notes.map { quillpadNote ->
                val result = quillpadNote.toBaseNote(notebookMap)
                progress?.postValue(
                    ImportProgress(counter++, total, stage = ImportStage.IMPORT_NOTES)
                )
                result
            }

        return Pair(baseNotes, dataFolder)
    }

    fun QuillpadNote.toBaseNote(notebookMap: Map<Long, String>): BaseNote {
        val (body, spans) =
            if (!isList && content != null) {
                parseBodyAndSpansFromMarkdown(content)
            } else {
                Pair("", emptyList())
            }

        val items =
            taskList?.mapIndexed { index, task ->
                ListItem(
                    body = task.content,
                    checked = task.isDone,
                    isChild = false,
                    order = index,
                    children = mutableListOf(),
                )
            } ?: emptyList()

        val images = mutableListOf<FileAttachment>()
        val files = mutableListOf<FileAttachment>()
        val audios = mutableListOf<Audio>()

        attachments?.forEach { attachment ->
            when (attachment.type) {
                "AUDIO" ->
                    audios.add(
                        Audio(
                            name = attachment.fileName,
                            duration = null,
                            timestamp = modifiedDate.toMillis(),
                        )
                    )
                else -> {
                    val mimetype = attachment.fileName.getMimeType()
                    if (mimetype?.startsWith("image/") == true) {
                        images.add(
                            FileAttachment(
                                localName = attachment.fileName,
                                originalName = attachment.description ?: attachment.fileName,
                                mimeType = mimetype,
                            )
                        )
                    } else {
                        files.add(
                            FileAttachment(
                                localName = attachment.fileName,
                                originalName = attachment.description ?: attachment.fileName,
                                mimeType = mimetype ?: "application/octet-stream",
                            )
                        )
                    }
                }
            }
        }

        val labels = mutableSetOf<String>()
        notebookId?.let { notebookId -> notebookMap[notebookId]?.let { labels.add(it) } }
        tags?.forEach { labels.add(it.name) }

        val reminders =
            this.reminders.map { Reminder(id = it.id, dateTime = Date(it.date), repetition = null) }

        return BaseNote(
            id = 0L,
            type = if (isList) Type.LIST else Type.NOTE,
            folder =
                when {
                    isDeleted -> Folder.DELETED
                    isArchived -> Folder.ARCHIVED
                    else -> Folder.NOTES
                },
            color = BaseNote.COLOR_DEFAULT,
            title = title ?: "",
            pinned = isPinned,
            timestamp = creationDate.toMillis(),
            modifiedTimestamp = modifiedDate.toMillis(),
            labels = labels.sorted().toList(),
            body = body,
            spans = spans,
            items = items,
            images = images,
            files = files,
            audios = audios,
            reminders = reminders,
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
    }

    private fun unzip(destinationPath: File, inputStream: InputStream): File {
        val buffer = ByteArray(1024)
        val zis = ZipInputStream(inputStream)
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            val newFile = newFile(destinationPath, zipEntry)
            if (zipEntry.isDirectory) {
                if (!newFile.isDirectory && !newFile.mkdirs()) {
                    throw IOException("Failed to create directory $newFile")
                }
            } else {
                val parent = newFile.parentFile
                if (parent != null) {
                    if (!parent.isDirectory && !parent.mkdirs()) {
                        throw IOException("Failed to create directory $parent")
                    }
                }
                FileOutputStream(newFile).use {
                    var len: Int
                    while ((zis.read(buffer).also { length -> len = length }) > 0) {
                        it.write(buffer, 0, len)
                    }
                }
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()
        return destinationPath
    }

    private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: " + zipEntry.name)
        }
        return destFile
    }

    companion object {
        private const val TAG = "QuillpadImporter"
    }
}
