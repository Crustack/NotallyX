package com.philkes.notallyx.feature.migration.backup

import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.getExternalCrashesDirectory
import com.philkes.notallyx.core.common.extensions.keepOnlyNewest
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.database.NotallyDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date

private const val TAG = "DatabaseBackupExtensions"

suspend fun ContextWrapper.exportRawDatabase(fileUri: Uri) {
    val (_, databaseCopy) = copyDatabase()
    val database =
        SQLiteDatabase.openDatabase(databaseCopy.path, null, SQLiteDatabase.OPEN_READONLY)
    database
        .rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf("BaseNote"),
        )
        .use { cursor ->
            if (cursor.count < 1) {
                throw IOException("Database does not contain 'BaseNote' table!")
            }
        }
    val outputStream =
        contentResolver.openOutputStream(fileUri)
            ?: throw IOException("Failed to open output stream for $fileUri")
    outputStream.use { outputStream ->
        (outputStream as? FileOutputStream)?.channel?.truncate(0)
        FileInputStream(databaseCopy).use { inputStream ->
            inputStream.copyToLarge(outputStream)
            outputStream.flush()
        }
    }
    try {
        databaseCopy.delete()
    } catch (e: Exception) {
        log(
            TAG,
            msg = "Failed to delete raw db copy file: '${databaseCopy.absolutePath}'",
            throwable = e,
        )
    }
    contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (cursor.getLong(sizeIndex) <= 0) {
                throw IOException("Exported Raw Database file is empty!")
            }
        }
    }
}

fun ContextWrapper.backupDatabaseFiles(date: Date = Date()): File? {
    val timestamp = FILE_TIMESTAMP_FORMAT.format(date)
    val crashesDir =
        try {
            getExternalCrashesDirectory()
        } catch (_: Exception) {
            File(filesDir, "crashes").apply { mkdirs() }
        }
    val targetDir = File(crashesDir, timestamp).apply { mkdirs() }
    log(TAG, "Backing up raw database files to '$targetDir'")
    val externalCopySuccess =
        copyFiles(
            NotallyDatabase.getExternalDatabaseFiles(this),
            File(targetDir, "external").apply { mkdirs() },
        )
    val internalCopySuccess =
        copyFiles(
            NotallyDatabase.getInternalDatabaseFiles(this),
            File(targetDir, "internal").apply { mkdirs() },
        )
    keepOnlyNewest(crashesDir, 20)
    return if (externalCopySuccess && internalCopySuccess) targetDir else null
}

private fun ContextWrapper.copyFiles(sourceFiles: List<File>, targetDir: File): Boolean {
    var success = true
    sourceFiles.forEach { sourceFile ->
        if (sourceFile.exists()) {
            try {
                val destination = File(targetDir, sourceFile.name)
                log(TAG, msg = "Copying file '${sourceFile.path}' to '${destination.path}'...")
                sourceFile.copyTo(destination, overwrite = true)
            } catch (e: Exception) {
                log(TAG, msg = "Failed to copy file '${sourceFile.path}'", throwable = e)
                success = false
            }
        }
    }
    return success
}
