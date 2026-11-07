package com.philkes.notallyx.feature.migration.imports.quillpad

import android.app.Application
import android.net.Uri
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.moveAllFiles
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.migration.imports.ExternalImporter
import com.philkes.notallyx.feature.migration.imports.ImportException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.flow.MutableStateFlow

class QuillpadExternalImporter : QuillpadImporter(), ExternalImporter {

    override fun import(
        app: Application,
        source: Uri,
        destination: File,
        progress: MutableStateFlow<Progress?>?,
    ): Pair<List<BaseNote>, File> {
        progress?.value = ImportProgress(indeterminate = true, stage = ImportStage.EXTRACT_FILES)
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
        progress?.value = ImportProgress(total = total, stage = ImportStage.IMPORT_NOTES)
        var counter = 1

        val baseNotes =
            quillpadBackup.notes.map { quillpadNote ->
                val result = quillpadNote.toBaseNote(notebookMap)
                progress?.value = ImportProgress(counter++, total, stage = ImportStage.IMPORT_NOTES)
                result
            }

        return Pair(baseNotes, dataFolder)
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
