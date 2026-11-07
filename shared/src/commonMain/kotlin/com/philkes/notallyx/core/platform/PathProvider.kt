package com.philkes.notallyx.core.platform

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
        val dbDir =
            if (dataInPublicFolder) {
                appDataDirectory
            } else {
                File(appDataDirectory, "databases").apply { if (!exists()) mkdirs() }
            }
        return File(dbDir, databaseName)
    }

    override fun getDatabaseFiles(databaseName: String, dataInPublicFolder: Boolean): List<File> {
        val dbDir =
            if (dataInPublicFolder) appDataDirectory else File(appDataDirectory, "databases")
        return listOf(
                File(dbDir, databaseName),
                File(dbDir, "$databaseName-shm"),
                File(dbDir, "$databaseName-wal"),
                File(dbDir, "$databaseName-journal"),
            )
            .filter { it.exists() }
    }

    override fun getMediaDirectory(subFolder: String?, dataInPublicFolder: Boolean): File {
        val root =
            if (dataInPublicFolder) {
                appDataDirectory
            } else {
                File(appDataDirectory, "attachments").apply { if (!exists()) mkdirs() }
            }
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
        if (subFolder !in listOf("Images", "Files", "Audios")) {
            return null
        }
        return File(getMediaDirectory(subFolder, dataInPublicFolder), localName)
    }

    override fun getBackupsDirectory(): File {
        return File(appDataDirectory, "Backups").apply { if (!exists()) mkdirs() }
    }

    override fun getCacheDirectory(subFolder: String?, clearIfExisting: Boolean): File {
        val cacheRoot = File(appDataDirectory, "cache").apply { if (!exists()) mkdirs() }
        if (subFolder.isNullOrEmpty()) return cacheRoot
        val folder = File(cacheRoot, subFolder)
        if (folder.exists()) {
            if (clearIfExisting) {
                folder.listFiles()?.forEach { it.delete() }
            }
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
