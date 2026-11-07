package com.philkes.notallyx.core.platform

import android.content.Context
import android.content.ContextWrapper
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_AUDIOS
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_BACKUPS
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.clearDirectory
import com.philkes.notallyx.core.common.extensions.getExternalMediaDirectory
import com.philkes.notallyx.core.common.extensions.getLogsDir
import com.philkes.notallyx.core.common.extensions.getPrivateAttachmentsRoot
import java.io.File

class AndroidPathProvider(private val context: Context) : PathProvider {

    override fun getDatabaseFile(databaseName: String, dataInPublicFolder: Boolean): File {
        return if (dataInPublicFolder) {
            File(context.getExternalMediaDirectory(), databaseName)
        } else {
            context.getDatabasePath(databaseName)
        }
    }

    override fun getDatabaseFiles(databaseName: String, dataInPublicFolder: Boolean): List<File> {
        return if (dataInPublicFolder) {
            val directory = context.getExternalMediaDirectory()
            listOf(
                    File(directory, databaseName),
                    File(directory, "$databaseName-shm"),
                    File(directory, "$databaseName-wal"),
                    File(directory, "$databaseName-journal"),
                )
                .filter { it.exists() }
        } else {
            val directory = context.getDatabasePath(databaseName).parentFile
            listOf(
                    File(directory, databaseName),
                    File(directory, "$databaseName-shm"),
                    File(directory, "$databaseName-wal"),
                    File(directory, "$databaseName-journal"),
                )
                .filter { it.exists() }
        }
    }

    override fun getMediaDirectory(subFolder: String?, dataInPublicFolder: Boolean): File {
        return if (dataInPublicFolder) {
            context.getExternalMediaDirectory(subFolder ?: "")
        } else {
            val root = context.getPrivateAttachmentsRoot()
            if (subFolder.isNullOrEmpty()) {
                root
            } else {
                File(root, subFolder).apply { if (!exists()) mkdirs() }
            }
        }
    }

    override fun resolveAttachmentFile(
        subFolder: String,
        localName: String,
        dataInPublicFolder: Boolean,
    ): File? {
        if (subFolder !in listOf(SUBFOLDER_IMAGES, SUBFOLDER_FILES, SUBFOLDER_AUDIOS)) {
            return null
        }
        val currentDir = getMediaDirectory(subFolder, dataInPublicFolder)
        val inCurrent = File(currentDir, localName)
        if (inCurrent.exists()) return inCurrent

        val altDir = getMediaDirectory(subFolder, !dataInPublicFolder)
        val inAlt = File(altDir, localName)
        if (inAlt.exists()) return inAlt

        return inCurrent
    }

    override fun getBackupsDirectory(): File {
        return context.getExternalMediaDirectory(SUBFOLDER_BACKUPS)
    }

    override fun getCacheDirectory(subFolder: String?, clearIfExisting: Boolean): File {
        val base = context.cacheDir
        if (subFolder.isNullOrEmpty()) return base
        val folder = File(base, subFolder)
        if (folder.exists()) {
            if (clearIfExisting) folder.clearDirectory()
        } else {
            folder.mkdirs()
        }
        return folder
    }

    override fun getPreferencesDirectory(): File {
        return File(context.filesDir, "datastore").apply { if (!exists()) mkdirs() }
    }

    override fun getLogsDirectory(): File {
        return context.getLogsDir()
    }
}

fun ContextWrapper.toPathProvider(): PathProvider = AndroidPathProvider(this)
