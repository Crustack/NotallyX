package com.philkes.notallyx.feature.migration.imports.google

import android.app.Application
import android.net.Uri
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.listFilesRecursive
import com.philkes.notallyx.core.common.extensions.log
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

class GoogleKeepExternalImporter : GoogleKeepImporter(), ExternalImporter {

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
                throw ImportException(R.string.invalid_google_keep, e)
            }

        if (!dataFolder.exists()) {
            throw ImportException(
                R.string.invalid_google_keep,
                RuntimeException("Extracting Takeout.zip failed"),
            )
        }

        val noteFiles =
            dataFolder
                .listFilesRecursive { file ->
                    file.isFile && file.extension.equals("json", ignoreCase = true)
                }
                .toList()
        val total = noteFiles.size
        progress?.value = ImportProgress(total = total, stage = ImportStage.IMPORT_NOTES)
        var counter = 1
        val baseNotes =
            noteFiles
                .mapNotNull { file ->
                    val baseNote =
                        try {
                            val relativePath = file.parentFile!!.toRelativeString(dataFolder)
                            file.readText().parseToBaseNote(relativePath)
                        } catch (e: Exception) {
                            app.log(
                                TAG,
                                msg =
                                    "Could not parse BaseNote from JSON in file '${file.absolutePath}'",
                                throwable = e,
                            )
                            null
                        }
                    progress?.value =
                        ImportProgress(counter++, total, stage = ImportStage.IMPORT_NOTES)
                    baseNote
                }
                .toList()
        return Pair(baseNotes, dataFolder)
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

    private fun unzip(destinationPath: File, inputStream: InputStream): File {
        val buffer = ByteArray(1024)
        val zis = ZipInputStream(inputStream)
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            val newFile: File = newFile(destinationPath, zipEntry)
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

    companion object {
        private const val TAG = "GoogleKeepImporter"
    }
}
