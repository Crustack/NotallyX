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

/**
 * Resolves application directories across Android (including `dataInPublicFolder` mode) and Desktop
 * OS conventions (`$XDG_DATA_HOME/NotallyX`, `~/Library/Application Support/NotallyX`,
 * `%APPDATA%\NotallyX`).
 */
interface PathProvider {
    fun getDatabaseFile(databaseName: String, dataInPublicFolder: Boolean = false): File

    fun getDatabaseFiles(databaseName: String, dataInPublicFolder: Boolean = false): List<File>

    fun getMediaDirectory(subFolder: String? = null, dataInPublicFolder: Boolean = false): File

    fun getMediaRoot(subFolder: String? = null, dataInPublicFolder: Boolean = false): File =
        getMediaDirectory(subFolder, dataInPublicFolder)

    fun resolveAttachmentFile(
        subFolder: String,
        localName: String,
        dataInPublicFolder: Boolean = false,
    ): File?

    fun getBackupsDirectory(): File

    fun getCacheDirectory(subFolder: String? = null, clearIfExisting: Boolean = false): File

    fun getPreferencesDirectory(): File

    fun getDataStoreFile(fileName: String): File = File(getPreferencesDirectory(), fileName)

    fun getLogsDirectory(): File
}

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

class DesktopPathProvider(
    private val appName: String = "NotallyX",
    private val baseDirOverride: File? = null,
) : PathProvider {

    val appDataDirectory: File by lazy {
        (baseDirOverride ?: resolveStandardOsAppDataDirectory(appName)).apply {
            if (!exists()) mkdirs()
        }
    }

    override fun getDatabaseFile(databaseName: String, dataInPublicFolder: Boolean): File {
        val dbDir = File(appDataDirectory, "databases").apply { if (!exists()) mkdirs() }
        return File(dbDir, databaseName)
    }

    override fun getDatabaseFiles(databaseName: String, dataInPublicFolder: Boolean): List<File> {
        val dbDir = File(appDataDirectory, "databases")
        return listOf(
                File(dbDir, databaseName),
                File(dbDir, "$databaseName-shm"),
                File(dbDir, "$databaseName-wal"),
                File(dbDir, "$databaseName-journal"),
            )
            .filter { it.exists() }
    }

    override fun getMediaDirectory(subFolder: String?, dataInPublicFolder: Boolean): File {
        val root = File(appDataDirectory, "attachments").apply { if (!exists()) mkdirs() }
        return if (subFolder.isNullOrEmpty()) {
            root
        } else {
            File(root, subFolder).apply { if (!exists()) mkdirs() }
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
        return File(getMediaDirectory(subFolder, dataInPublicFolder), localName)
    }

    override fun getBackupsDirectory(): File {
        return File(appDataDirectory, SUBFOLDER_BACKUPS).apply { if (!exists()) mkdirs() }
    }

    override fun getCacheDirectory(subFolder: String?, clearIfExisting: Boolean): File {
        val cacheRoot = File(appDataDirectory, "cache").apply { if (!exists()) mkdirs() }
        if (subFolder.isNullOrEmpty()) return cacheRoot
        val folder = File(cacheRoot, subFolder)
        if (folder.exists()) {
            if (clearIfExisting) folder.clearDirectory()
        } else {
            folder.mkdirs()
        }
        return folder
    }

    override fun getPreferencesDirectory(): File {
        return File(appDataDirectory, "datastore").apply { if (!exists()) mkdirs() }
    }

    override fun getLogsDirectory(): File {
        return File(appDataDirectory, "logs").apply { if (!exists()) mkdirs() }
    }

    companion object {
        fun resolveStandardOsAppDataDirectory(appName: String = "NotallyX"): File {
            val osName = (System.getProperty("os.name") ?: "").lowercase()
            val userHome = System.getProperty("user.home") ?: "."
            return when {
                osName.contains("win") -> {
                    val appData = System.getenv("APPDATA")
                    if (!appData.isNullOrBlank()) {
                        File(appData, appName)
                    } else {
                        File(File(userHome, "AppData/Roaming"), appName)
                    }
                }
                osName.contains("mac") || osName.contains("darwin") -> {
                    File(File(userHome, "Library/Application Support"), appName)
                }
                else -> {
                    val xdgDataHome = System.getenv("XDG_DATA_HOME")
                    if (!xdgDataHome.isNullOrBlank()) {
                        File(xdgDataHome, appName)
                    } else {
                        File(File(userHome, ".local/share"), appName)
                    }
                }
            }
        }
    }
}

fun ContextWrapper.toPathProvider(): PathProvider = AndroidPathProvider(this)
