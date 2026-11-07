package com.philkes.notallyx.core.platform

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Low-level file and content stream I/O abstraction over [PlatformUri], isolating Android's
 * `ContentResolver` and `DocumentFile` from shared domain, import/export, and ViewModel code.
 */
interface FileStorage {
    suspend fun openInputStream(uri: PlatformUri): InputStream?

    suspend fun openOutputStream(uri: PlatformUri, append: Boolean = false): OutputStream?

    suspend fun getFileName(uri: PlatformUri): String?

    suspend fun getMimeType(uri: PlatformUri): String?

    suspend fun exists(uri: PlatformUri): Boolean

    suspend fun delete(uri: PlatformUri): Boolean

    suspend fun listChildren(directoryUri: PlatformUri): List<PlatformUri>

    suspend fun createChildFile(
        directoryUri: PlatformUri,
        mimeType: String,
        fileNameWithoutExtension: String,
        fileExtension: String,
    ): PlatformUri?

    suspend fun copyToInternalFile(source: PlatformUri, destination: File): Boolean

    fun toReadablePath(uri: PlatformUri): String
}

class JvmFileStorage : FileStorage {

    private fun PlatformUri.toFile(): File = toFileOrNull() ?: File(uriString)

    override suspend fun openInputStream(uri: PlatformUri): InputStream? =
        withContext(Dispatchers.IO) {
            val file = uri.toFile()
            if (file.exists() && file.isFile) FileInputStream(file) else null
        }

    override suspend fun openOutputStream(uri: PlatformUri, append: Boolean): OutputStream? =
        withContext(Dispatchers.IO) {
            val file = uri.toFile()
            file.parentFile?.mkdirs()
            FileOutputStream(file, append)
        }

    override suspend fun getFileName(uri: PlatformUri): String? =
        withContext(Dispatchers.IO) { uri.toFile().name }

    override suspend fun getMimeType(uri: PlatformUri): String? =
        withContext(Dispatchers.IO) { MimeTypeUtils.getMimeTypeFromFileName(uri.toFile().name) }

    override suspend fun exists(uri: PlatformUri): Boolean =
        withContext(Dispatchers.IO) { uri.toFile().exists() }

    override suspend fun delete(uri: PlatformUri): Boolean =
        withContext(Dispatchers.IO) { uri.toFile().delete() }

    override suspend fun listChildren(directoryUri: PlatformUri): List<PlatformUri> =
        withContext(Dispatchers.IO) {
            val dir = directoryUri.toFile()
            if (dir.isDirectory) {
                dir.listFiles()?.map { PlatformUri.fromFile(it) } ?: emptyList()
            } else {
                emptyList()
            }
        }

    override suspend fun createChildFile(
        directoryUri: PlatformUri,
        mimeType: String,
        fileNameWithoutExtension: String,
        fileExtension: String,
    ): PlatformUri? =
        withContext(Dispatchers.IO) {
            val dir = directoryUri.toFile()
            dir.mkdirs()
            val ext = if (fileExtension.startsWith(".")) fileExtension else ".$fileExtension"
            val target = File(dir, "$fileNameWithoutExtension$ext")
            if (!target.exists()) target.createNewFile()
            PlatformUri.fromFile(target)
        }

    override suspend fun copyToInternalFile(source: PlatformUri, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            val srcFile = source.toFile()
            if (!srcFile.exists()) return@withContext false
            destination.parentFile?.mkdirs()
            srcFile.copyTo(destination, overwrite = true)
            true
        }

    override fun toReadablePath(uri: PlatformUri): String = uri.toFile().path
}
