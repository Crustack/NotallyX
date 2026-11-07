package com.philkes.notallyx.core.platform

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopFileStorage : FileStorage {

    override suspend fun openInputStream(uri: PlatformUri): InputStream? =
        withContext(Dispatchers.IO) {
            val file = uri.toFileOrNull() ?: return@withContext null
            if (file.exists() && file.isFile) {
                FileInputStream(file)
            } else {
                null
            }
        }

    override suspend fun openOutputStream(uri: PlatformUri, append: Boolean): OutputStream? =
        withContext(Dispatchers.IO) {
            val file = uri.toFileOrNull() ?: return@withContext null
            file.parentFile?.mkdirs()
            FileOutputStream(file, append)
        }

    override suspend fun getFileName(uri: PlatformUri): String? =
        withContext(Dispatchers.IO) { uri.toFileOrNull()?.name }

    override suspend fun getMimeType(uri: PlatformUri): String? =
        withContext(Dispatchers.IO) {
            val file = uri.toFileOrNull() ?: return@withContext null
            MimeTypeUtils.getMimeTypeFromFileName(file.name)
        }

    override suspend fun exists(uri: PlatformUri): Boolean =
        withContext(Dispatchers.IO) { uri.toFileOrNull()?.exists() == true }

    override suspend fun delete(uri: PlatformUri): Boolean =
        withContext(Dispatchers.IO) {
            val file = uri.toFileOrNull() ?: return@withContext false
            if (!file.exists()) return@withContext false
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }

    override suspend fun listChildren(directoryUri: PlatformUri): List<PlatformUri> =
        withContext(Dispatchers.IO) {
            val file = directoryUri.toFileOrNull()
            if (file != null && file.isDirectory) {
                file.listFiles()?.map { PlatformUri.fromFile(it) } ?: emptyList()
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
            val dir = directoryUri.toFileOrNull() ?: return@withContext null
            dir.mkdirs()
            val ext =
                if (fileExtension.isEmpty() || fileExtension.startsWith(".")) {
                    fileExtension
                } else {
                    ".$fileExtension"
                }
            val target = File(dir, "$fileNameWithoutExtension$ext")
            if (!target.exists()) {
                target.createNewFile()
            }
            PlatformUri.fromFile(target)
        }

    override suspend fun copyToInternalFile(source: PlatformUri, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            val input = openInputStream(source) ?: return@withContext false
            destination.parentFile?.mkdirs()
            input.use { inStream ->
                destination.outputStream().use { outStream -> inStream.copyTo(outStream) }
            }
            true
        }

    override fun toReadablePath(uri: PlatformUri): String {
        return uri.toFileOrNull()?.absolutePath ?: uri.uriString
    }
}
