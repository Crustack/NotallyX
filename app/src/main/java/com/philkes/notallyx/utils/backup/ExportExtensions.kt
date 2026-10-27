package com.philkes.notallyx.utils.backup

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.print.PdfPrintListener
import android.print.printPdf
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.toHtml
import com.philkes.notallyx.data.model.toJson
import com.philkes.notallyx.data.model.toMarkdown
import com.philkes.notallyx.data.model.toTxt
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.activity.main.fragment.settings.SettingsFragment
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.edit.BackupFile
import com.philkes.notallyx.presentation.viewmodel.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.progress.BackupProgress
import com.philkes.notallyx.utils.MIME_TYPE_ZIP
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import com.philkes.notallyx.utils.ZipVerificationException
import com.philkes.notallyx.utils.copyToLarge
import com.philkes.notallyx.utils.createChannelIfNotExists
import com.philkes.notallyx.utils.createFileSafe
import com.philkes.notallyx.utils.createReportBugIntent
import com.philkes.notallyx.utils.getCurrentAudioDirectory
import com.philkes.notallyx.utils.getCurrentFilesDirectory
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import com.philkes.notallyx.utils.getCurrentMediaRoot
import com.philkes.notallyx.utils.getDocumentFolder
import com.philkes.notallyx.utils.getExportedPath
import com.philkes.notallyx.utils.getExternalCrashesDirectory
import com.philkes.notallyx.utils.getLogFileUri
import com.philkes.notallyx.utils.keepOnlyNewest
import com.philkes.notallyx.utils.listZipFiles
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.md5Hash
import com.philkes.notallyx.utils.openOutputStreamTruncating
import com.philkes.notallyx.utils.resolveAttachmentFile
import com.philkes.notallyx.utils.security.decryptDatabase
import com.philkes.notallyx.utils.security.getInitializedCipherForDecryption
import com.philkes.notallyx.utils.verify
import com.philkes.notallyx.utils.wrapWithChooser
import java.io.File
import java.io.File.createTempFile
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod

private const val TAG = "ExportExtensions"
private const val NOTIFICATION_CHANNEL_ID = "AutoBackups"
private const val NOTIFICATION_ID = 123412
private const val OUTPUT_DATA_BACKUP_URI = "backupUri"

const val AUTO_BACKUP_WORK_NAME = "com.philkes.notallyx.AutoBackupWork"
const val OUTPUT_DATA_EXCEPTION = "exception"

val FILE_TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss_SSS", Locale.ENGLISH)
val LOG_DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

const val ON_SAVE_BACKUP_FILE = "NotallyX_AutoBackup"
const val PERIODIC_BACKUP_FILE_PREFIX = "NotallyX_Backup_"
const val PERIODIC_BACKUP_EMPTY_SUFFIX = "_empty"

private fun DocumentFile.isEmptyBackup() =
    name?.endsWith("$PERIODIC_BACKUP_EMPTY_SUFFIX.zip", ignoreCase = true) == true

private val periodicBackupMutex = Mutex()

suspend fun ContextWrapper.createBackup(): Result {
    return periodicBackupMutex.withLock {
        val app = applicationContext as Application
        val preferences = NotallyXPreferences.getInstance(app)
        val (_, maxBackups) = preferences.periodicBackups.value
        val path = preferences.backupsFolder.value

        if (path != EMPTY_PATH) {
            val uri = path.toUri()
            val folder =
                requireBackupFolder(
                    path,
                    "Periodic Backup failed, because auto-backup path '$path' is invalid",
                ) ?: return@withLock Result.success()
            var zipDocument: DocumentFile? = null
            try {
                val backupFilePrefix = PERIODIC_BACKUP_FILE_PREFIX
                // Leftovers of interrupted backups must not count as backups during rotation
                folder.listZipFiles(backupFilePrefix).forEach {
                    if (it.length() == 0L) {
                        log(TAG, msg = "Deleting empty backup leftover '${it.name}'")
                        it.delete()
                    }
                }
                // Backups without notes are marked in their name, so they can be excluded from
                // the rotation of the backups with notes
                val isEmpty = NotallyDatabase.getDatabase(app).value?.getBaseNoteDao()?.count() == 0
                val name =
                    "$backupFilePrefix${FILE_TIMESTAMP_FORMAT.format(System.currentTimeMillis())}" +
                        if (isEmpty) PERIODIC_BACKUP_EMPTY_SUFFIX else ""
                log(TAG, msg = "Creating '$uri/$name.zip'...")
                zipDocument = folder.createFileSafe(MIME_TYPE_ZIP, name, ".zip")
                val zipUri = zipDocument.uri
                val (exportedNotes, exportedAttachments) =
                    app.exportAsZip(zipUri, password = preferences.backupPassword.value)
                log(TAG, msg = "Exported $exportedNotes notes and $exportedAttachments attachments")
                // Sorted by the timestamp in the name: providers may report lastModified as 0
                val backupFiles =
                    folder.listZipFiles(backupFilePrefix).sortedByDescending { it.name.orEmpty() }
                val (emptyBackups, backupsWithNotes) = backupFiles.partition { it.isEmptyBackup() }
                log(
                    TAG,
                    msg =
                        "Found ${backupsWithNotes.size} backups with notes and ${emptyBackups.size} without",
                )
                // Backups without notes never push out backups with notes: the database might have
                // been emptied by an error and the old backups are the only copy. Of the backups
                // without notes only the newest one is kept.
                val backupsToBeDeleted =
                    emptyBackups.drop(1) +
                        if (exportedNotes == 0) {
                            log(TAG, msg = "Backup contains no notes, keeping all older backups")
                            emptyList()
                        } else backupsWithNotes.drop(maxBackups)
                if (backupsToBeDeleted.isNotEmpty()) {
                    log(
                        TAG,
                        msg =
                            "Deleting ${backupsToBeDeleted.size} oldest backups (maxBackups: $maxBackups): ${backupsToBeDeleted.joinToString { "'${it.name.toString()}'" }}",
                    )
                }
                backupsToBeDeleted.forEach {
                    if (it.exists()) {
                        it.delete()
                    }
                }
                log(TAG, msg = "Finished backup to '$zipUri'")
                preferences.periodicBackupLastExecution.save(Date().time)
                return@withLock Result.success(
                    Data.Builder().putString(OUTPUT_DATA_BACKUP_URI, zipUri.path!!).build()
                )
            } catch (e: Exception) {
                log(TAG, msg = "Failed creating backup to '$path'", throwable = e)
                // Don't leave an empty or incomplete ZIP behind, it would count as a backup
                try {
                    zipDocument?.takeIf { it.exists() }?.delete()
                } catch (deleteException: Exception) {
                    log(
                        TAG,
                        msg = "Failed to delete incomplete backup '${zipDocument?.uri}'",
                        throwable = deleteException,
                    )
                }
                tryPostErrorNotification(e)
                return Result.success(
                    Data.Builder().putString(OUTPUT_DATA_EXCEPTION, e.message).build()
                )
            }
        }
        return@withLock Result.success()
    }
}

fun ContextWrapper.autoBackupOnSaveFileExists(backupPath: String): Boolean {
    val backupFolderFile = getDocumentFolder(backupPath.toUri())
    return backupFolderFile?.let {
        val autoBackupFile = it.findFile("$ON_SAVE_BACKUP_FILE.zip")
        autoBackupFile != null && autoBackupFile.exists()
    } ?: false
}

private val autoBackupOnSaveMutex = Mutex()

/** Name of the ZIP a new auto backup is written to before it replaces [ON_SAVE_BACKUP_FILE]. */
const val ON_SAVE_BACKUP_TEMP_FILE = "${ON_SAVE_BACKUP_FILE}_tmp"

/**
 * Creates or updates the "auto backup on save" ZIP ([ON_SAVE_BACKUP_FILE]).
 *
 * The previous content of the ZIP is kept in its `History` folder (see [BackupHistory]), and the
 * ZIP is only replaced after the new one has been completely written and verified. An interrupted
 * or failing backup therefore never destroys the previous one.
 *
 * @param savedNote if set, only the attachments of this note are added (partial backup), otherwise
 *   all notes and attachments are exported (full backup).
 * @param previousPassword password the existing ZIP was created with, if it differs from
 *   [password].
 */
suspend fun ContextWrapper.autoBackupOnSave(
    backupPath: String,
    password: String,
    savedNote: BaseNote?,
    previousPassword: String = password,
) {
    return autoBackupOnSaveMutex.withLock {
        Log.d(
            TAG,
            "Starting Auto Backup${savedNote?.let { " for Note id: ${it.id} title: ${it.title}" } ?: ""}...",
        )
        val folder =
            requireBackupFolder(
                backupPath,
                "Auto backup on note save (${savedNote?.let { "id: '${savedNote.id}, title: '${savedNote.title}'" }}) failed, because auto-backup path '$backupPath' is invalid",
            ) ?: return@withLock
        val workDir = File(cacheDir, "auto_backup_${UUID.randomUUID()}")
        try {
            val previousDir = File(workDir, "previous").apply { mkdirs() }
            val newDir = File(workDir, "new").apply { mkdirs() }

            recoverInterruptedReplace(folder)
            val existingBackup =
                folder.findFile("$ON_SAVE_BACKUP_FILE.zip")?.takeIf {
                    it.exists() && it.length() > 0
                }
            var previousTimestamp = System.currentTimeMillis()
            val hasPrevious =
                existingBackup != null &&
                    try {
                        extractBackupWithAnyPassword(
                            existingBackup.uri,
                            previousDir,
                            listOf(previousPassword, password).distinct(),
                        )
                        existingBackup
                            .lastModified()
                            .takeIf { it > 0 }
                            ?.let { previousTimestamp = it }
                        true
                    } catch (e: ZipException) {
                        // Corrupt or encrypted with an unknown password: keep it under another
                        // name instead of overwriting it. Other errors (e.g. the file can't be
                        // accessed) abort the backup and leave the existing ZIP untouched.
                        previousDir.deleteRecursively()
                        previousDir.mkdirs()
                        val restoredFromTemp =
                            extractLeftoverTempBackup(
                                    folder,
                                    previousDir,
                                    previousPassword,
                                    password,
                                )
                                ?.also { previousTimestamp = it }
                        preserveUnreadableBackup(folder, existingBackup, e)
                        restoredFromTemp != null
                    }

            if (savedNote == null || !hasPrevious) {
                if (savedNote != null) {
                    log(TAG, msg = "Creating full backup since no readable auto backup ZIP exists")
                }
                Log.d(TAG, "Creating full auto backup")
                val fullBackupZip = File(workDir, "full.zip")
                exportAsZip(Uri.fromFile(fullBackupZip), password = password)
                extractZipToDirectory(fullBackupZip, newDir, password)
                fullBackupZip.delete()
            } else {
                Log.d(TAG, "Creating partial auto backup for Note ${savedNote.id}")
                previousDir.copyRecursively(newDir, overwrite = true)
                val (_, databaseFile) = copyDatabase()
                try {
                    toBackupFiles(savedNote, databaseFile).forEach { backupFile ->
                        if (backupFile.file.exists()) {
                            backupFile.file.copyToLarge(
                                File(newDir, backupFile.targetPath),
                                overwrite = true,
                            )
                        }
                    }
                } finally {
                    try {
                        databaseFile.delete()
                    } catch (e: Exception) {
                        log(
                            TAG,
                            msg =
                                "Failed to delete temp db copy file: '${databaseFile.absolutePath}'",
                            throwable = e,
                        )
                    }
                }
            }

            if (hasPrevious) {
                val newInfo =
                    try {
                        SqliteSnapshotInspector.inspect(File(newDir, DATABASE_NAME))
                    } catch (e: Exception) {
                        throw IOException("Database of the new auto backup can not be read", e)
                    }
                if (newInfo.noteCount == 0) {
                    // Keep the existing ZIP: importing it must not restore an empty database
                    log(TAG, msg = "Database contains no notes, keeping the existing auto backup")
                    return@withLock
                }
            }

            val history =
                BackupHistory.merge(
                    previousDir = previousDir.takeIf { hasPrevious },
                    newDir = newDir,
                    previousTimestamp = previousTimestamp,
                    maxSnapshots =
                        NotallyXPreferences.getInstance(this).periodicBackups.value.maxBackups,
                    inspector = SqliteSnapshotInspector,
                )
            Log.d(TAG, "Auto backup history: $history")
            if (history.skippedEmptySnapshot) {
                log(TAG, msg = "Previous auto backup contained no notes, not added to History")
            }

            val resultZip = File(workDir, "result.zip")
            zipDirectory(newDir, resultZip, password)
            replaceFileSafely(folder, ON_SAVE_BACKUP_FILE, ON_SAVE_BACKUP_TEMP_FILE, resultZip)
            Log.d(TAG, "Finished auto backup")
        } catch (e: Exception) {
            try {
                log(
                    TAG,
                    "Auto backup on note save (${savedNote?.let { "id: '${savedNote.id}, title: '${savedNote.title}'" }}) failed",
                    e,
                )
            } catch (logException: Exception) {
                tryPostErrorNotification(logException)
                return@withLock
            }
            tryPostErrorNotification(e)
        } finally {
            workDir.deleteRecursively()
        }
    }
}

private fun ContextWrapper.toBackupFiles(note: BaseNote, databaseFile: File): List<BackupFile> =
    note.images.map {
        BackupFile(
            "$SUBFOLDER_IMAGES/${it.localName}",
            File(getCurrentImagesDirectory(), it.localName),
        )
    } +
        note.files.map {
            BackupFile(
                "$SUBFOLDER_FILES/${it.localName}",
                File(getCurrentFilesDirectory(), it.localName),
            )
        } +
        note.audios.map {
            BackupFile("$SUBFOLDER_AUDIOS/${it.name}", File(getCurrentAudioDirectory(), it.name))
        } +
        BackupFile(DATABASE_NAME, databaseFile)

/**
 * Extracts the backup ZIP at [uri] into [outputDir], trying each of [passwords]. Several passwords
 * are needed while the backup password is being changed.
 */
private fun ContextWrapper.extractBackupWithAnyPassword(
    uri: Uri,
    outputDir: File,
    passwords: List<String>,
) {
    val tempZipFile = createTempFile("previous", ".zip", outputDir.parentFile ?: cacheDir)
    try {
        val inputStream =
            contentResolver.openInputStream(uri) ?: throw IOException("Can not open '$uri'")
        inputStream.use { input -> tempZipFile.outputStream().use { input.copyToLarge(it) } }
        var lastException: Exception? = null
        for (password in passwords) {
            try {
                extractZipToDirectory(tempZipFile, outputDir, password)
                return
            } catch (e: ZipException) {
                lastException = e
                outputDir.deleteRecursively()
                outputDir.mkdirs()
            }
        }
        throw lastException ?: IOException("No password to extract '$uri'")
    } finally {
        tempZipFile.delete()
    }
}

/**
 * If a previous [replaceFileSafely] was interrupted after the old ZIP was deleted, only the
 * verified temp ZIP exists. Restore it under the regular name before doing anything else.
 */
private fun ContextWrapper.recoverInterruptedReplace(folder: DocumentFile) {
    if (folder.findFile("$ON_SAVE_BACKUP_FILE.zip")?.exists() == true) {
        return
    }
    val temp =
        folder.findFile("$ON_SAVE_BACKUP_TEMP_FILE.zip")?.takeIf { it.exists() && it.length() > 0 }
            ?: return
    log(TAG, msg = "Restoring interrupted auto backup '${temp.name}'")
    if (!temp.renameTo("$ON_SAVE_BACKUP_FILE.zip")) {
        val target = folder.createFileSafe(MIME_TYPE_ZIP, ON_SAVE_BACKUP_FILE, ".zip")
        copyDocumentVerified(temp.uri, target.uri)
        temp.delete()
    }
}

/**
 * If writing the regular ZIP was interrupted in the direct-write fallback of [replaceFileSafely],
 * the regular ZIP is broken while the verified temp ZIP still exists. Extracts that temp ZIP into
 * [outputDir] and returns its timestamp, or `null` if there is no readable temp ZIP. A temp ZIP
 * that is only partially written itself fails to extract and is ignored.
 */
private fun ContextWrapper.extractLeftoverTempBackup(
    folder: DocumentFile,
    outputDir: File,
    vararg passwords: String,
): Long? {
    val temp =
        folder.findFile("$ON_SAVE_BACKUP_TEMP_FILE.zip")?.takeIf { it.exists() && it.length() > 0 }
            ?: return null
    return try {
        extractBackupWithAnyPassword(temp.uri, outputDir, passwords.toList().distinct())
        log(TAG, msg = "Auto backup ZIP is broken, continuing from '${temp.name}'")
        temp.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
    } catch (e: ZipException) {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        null
    }
}

/** Keeps an auto backup ZIP that can not be read under a new name instead of overwriting it. */
private fun ContextWrapper.preserveUnreadableBackup(
    folder: DocumentFile,
    backup: DocumentFile,
    cause: Exception,
) {
    val newName =
        "${ON_SAVE_BACKUP_FILE}_unreadable_${FILE_TIMESTAMP_FORMAT.format(System.currentTimeMillis())}"
    log(
        TAG,
        msg = "Existing auto backup '${backup.name}' can not be read, keeping it as '$newName.zip'",
        throwable = cause,
    )
    if (!backup.renameTo("$newName.zip")) {
        val copy = folder.createFileSafe(MIME_TYPE_ZIP, newName, ".zip")
        copyDocumentVerified(backup.uri, copy.uri)
        backup.delete()
    }
}

/**
 * Replaces `[fileName].zip` in [folder] with [source] so that there is never a moment without a
 * complete backup: [source] is first written to `[tempFileName].zip` and verified, only then the
 * old file is deleted and the temp file renamed.
 */
private fun ContextWrapper.replaceFileSafely(
    folder: DocumentFile,
    fileName: String,
    tempFileName: String,
    source: File,
) {
    folder.findFile("$tempFileName.zip")?.delete()
    val temp = folder.createFileSafe(MIME_TYPE_ZIP, tempFileName, ".zip")
    try {
        writeFileVerified(source, temp.uri)
    } catch (e: Exception) {
        runCatching { temp.delete() }
        throw e
    }
    folder.findFile("$fileName.zip")?.let { old ->
        if (old.exists() && !old.delete()) {
            runCatching { temp.delete() }
            throw IOException("Could not delete old backup '${old.uri}'")
        }
    }
    if (temp.renameTo("$fileName.zip")) {
        return
    }
    // Renaming is not supported by every DocumentsProvider, write the file again instead
    log(TAG, msg = "Renaming '${temp.uri}' failed, writing '$fileName.zip' directly")
    val target = folder.createFileSafe(MIME_TYPE_ZIP, fileName, ".zip")
    try {
        writeFileVerified(source, target.uri)
    } catch (e: Exception) {
        // The verified temp ZIP stays and is picked up by the next backup
        runCatching { target.delete() }
        throw e
    }
    temp.delete()
}

private fun ContextWrapper.writeFileVerified(source: File, target: Uri) {
    openOutputStreamTruncating(target).use { outputStream ->
        FileInputStream(source).use { inputStream -> inputStream.copyToLarge(outputStream) }
        outputStream.flush()
    }
    val targetMd5 = runCatching { md5Hash(target) }.getOrNull()
    if (targetMd5 == null || !targetMd5.contentEquals(source.md5Hash())) {
        throw IOException("Writing '$target' failed, its content does not match '${source.name}'")
    }
}

private fun ContextWrapper.copyDocumentVerified(source: Uri, target: Uri) {
    val tempFile = createTempFile("copy", ".zip", cacheDir)
    try {
        val inputStream =
            contentResolver.openInputStream(source) ?: throw IOException("Can not open '$source'")
        inputStream.use { input -> tempFile.outputStream().use { input.copyToLarge(it) } }
        writeFileVerified(tempFile, target)
    } finally {
        tempFile.delete()
    }
}

private fun zipDirectory(sourceDir: File, zip: File, password: String) {
    val zipFile = ZipFile(zip, if (password != PASSWORD_EMPTY) password.toCharArray() else null)
    val zipParameters =
        ZipParameters().apply {
            isEncryptFiles = password != PASSWORD_EMPTY
            compressionLevel = CompressionLevel.NO_COMPRESSION
            if (isEncryptFiles) {
                encryptionMethod = EncryptionMethod.AES
            }
            isIncludeRootFolder = false
        }
    zipFile.addFolder(sourceDir, zipParameters)
    zipFile.verify(File(sourceDir, DATABASE_NAME))
}

private fun ContextWrapper.requireBackupFolder(path: String, msg: String): DocumentFile? {
    return try {
        val uri = path.toUri()
        val folder = getDocumentFolder(uri)
        if (folder == null || !folder.exists()) {
            log(TAG, msg = msg)
            tryPostErrorNotification(BackupFolderNotExistsException(path))
            return null
        }
        folder
    } catch (e: Exception) {
        log(TAG, msg = msg, throwable = e)
        tryPostErrorNotification(BackupFolderNotExistsException(path, e))
        return null
    }
}

suspend fun ContextWrapper.checkBackupOnSave(
    preferences: NotallyXPreferences,
    note: BaseNote? = null,
    forceFullBackup: Boolean = false,
) {
    if (preferences.backupOnSave.value) {
        val backupPath = preferences.backupsFolder.value
        if (backupPath != EMPTY_PATH) {
            // A full backup keeps the previous state in the ZIP's History, so the existing ZIP must
            // not be deleted beforehand
            val savedNote = if (forceFullBackup) null else note
            MainScope().launch {
                withContext(Dispatchers.IO) {
                    autoBackupOnSave(backupPath, preferences.backupPassword.value, savedNote)
                }
            }
        }
    }
}

fun ContextWrapper.deleteModifiedNoteBackup(backupPath: String) {
    getDocumentFolder(backupPath.toUri())?.findFile("$ON_SAVE_BACKUP_FILE.zip")?.delete()
}

fun ContextWrapper.modifiedNoteBackupExists(backupPath: String): Boolean {
    return getDocumentFolder(backupPath.toUri())?.findFile("$ON_SAVE_BACKUP_FILE.zip")?.exists()
        ?: false
}

typealias NotesAndAttachments = Pair<Int, Int>

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

suspend fun ContextWrapper.exportAsZip(
    fileUri: Uri,
    compress: Boolean = false,
    password: String = PASSWORD_EMPTY,
    backupProgress: MutableStateFlow<Progress?>? = null,
    retryOnFail: Boolean = true,
): NotesAndAttachments {
    backupProgress?.value = BackupProgress(indeterminate = true)
    val tempFile = createTempFile("export", "tmp", cacheDir)
    try {
        val zipFile =
            ZipFile(tempFile, if (password != PASSWORD_EMPTY) password.toCharArray() else null)
        val zipParameters =
            ZipParameters().apply {
                isEncryptFiles = password != PASSWORD_EMPTY
                if (!compress) {
                    compressionLevel = CompressionLevel.NO_COMPRESSION
                }
                if (isEncryptFiles) {
                    encryptionMethod = EncryptionMethod.AES
                }
            }

        val (databaseOriginal, databaseCopy) = copyDatabase()
        zipFile.addFile(databaseCopy, zipParameters.copy(DATABASE_NAME))

        val totalNotes = databaseOriginal.getBaseNoteDao().count()
        val images = databaseOriginal.getBaseNoteDao().getAllImages().toFileAttachments()
        val files = databaseOriginal.getBaseNoteDao().getAllFiles().toFileAttachments()
        val audios = databaseOriginal.getBaseNoteDao().getAllAudios()
        val totalAttachments = images.count() + files.count() + audios.size
        backupProgress?.value =
            BackupProgress(
                0,
                totalAttachments,
                countSuffix = getQuantityString(R.plurals.attachments, totalAttachments),
            )

        val counter = AtomicInteger(0)
        val missingAttachments = ArrayList<String>()
        images.export(
            zipFile,
            zipParameters,
            SUBFOLDER_IMAGES,
            this,
            backupProgress,
            totalAttachments,
            counter,
            missingAttachments,
        )
        files.export(
            zipFile,
            zipParameters,
            SUBFOLDER_FILES,
            this,
            backupProgress,
            totalAttachments,
            counter,
            missingAttachments,
        )
        audios
            .asSequence()
            .flatMap { string -> Converters.jsonToAudios(string) }
            .forEach { audio ->
                try {
                    if (
                        !backupAttachmentFile(
                            this,
                            zipFile,
                            zipParameters,
                            SUBFOLDER_AUDIOS,
                            audio.name,
                        )
                    ) {
                        missingAttachments.add("Audio: ${audio.name}")
                    }
                } catch (exception: Exception) {
                    log(TAG, throwable = exception)
                } finally {
                    backupProgress?.value =
                        BackupProgress(
                            counter.incrementAndGet(),
                            totalAttachments,
                            countSuffix =
                                getQuantityString(R.plurals.attachments, totalAttachments),
                        )
                }
            }
        try {
            zipFile.verify(databaseCopy)
        } catch (e: ZipVerificationException) {
            log(TAG, throwable = e)
            if (retryOnFail) {
                zipFile.file.delete()
                log(TAG, stackTrace = "Retrying to export ZIP to $fileUri...")
                return exportAsZip(fileUri, compress, password, backupProgress, false)
            } else {
                throw IOException(
                    "exportAsZip failed because created '${zipFile.file}' is not a valid ZIP!"
                )
            }
        }
        openOutputStreamTruncating(fileUri).use { outputStream ->
            FileInputStream(zipFile.file).use { inputStream ->
                inputStream.copyToLarge(outputStream)
                outputStream.flush()
            }
        }
        // Guard against null/IO issues when immediately reopening fileUri via SAF
        val sourceMd5 = runCatching { zipFile.file.md5Hash() }.getOrNull()
        val targetMd5 = runCatching { md5Hash(fileUri) }.getOrNull()
        if (sourceMd5 == null || targetMd5 == null || !targetMd5.contentEquals(sourceMd5)) {
            log(TAG, stackTrace = "Exported zipFile '$fileUri' has wrong MD5 hash!")
            if (retryOnFail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        contentResolver.delete(fileUri, null)
                    } catch (e: Exception) {
                        log(TAG, msg = "Deleting $fileUri failed", throwable = e)
                    }
                }
                zipFile.file.delete()
                log(TAG, stackTrace = "Retrying to export ZIP to $fileUri...")
                return exportAsZip(fileUri, compress, password, backupProgress, false)
            } else {
                throw IOException(
                    "exportAsZip failed because created '$fileUri' has wrong or unverifiable MD5 hash!"
                )
            }
        }
        zipFile.file.delete()
        try {
            databaseCopy.delete()
        } catch (e: Exception) {
            log(
                TAG,
                msg = "Failed to delete db copy file: '${databaseCopy.absolutePath}'",
                throwable = e,
            )
        }
        backupProgress?.value = BackupProgress(inProgress = false)
        // Post skipped attachments notification if any were missing
        if (missingAttachments.isNotEmpty()) {
            postSkippedAttachmentsNotification(missingAttachments)
        }
        return Pair(totalNotes, totalAttachments)
    } finally {
        tempFile.delete()
    }
}

private fun extractZipToDirectory(zip: File, outputDir: File, password: String) {
    val zipFile = ZipFile(zip, if (password != PASSWORD_EMPTY) password.toCharArray() else null)
    zipFile.extractAll(outputDir.absolutePath)
}

suspend fun ContextWrapper.copyDatabase(
    decrypt: Boolean = true,
    suffix: String = "",
): Pair<NotallyDatabase, File> {
    val database = NotallyDatabase.getDatabase(this@copyDatabase).value
    database!!.checkpoint()
    val preferences = NotallyXPreferences.getInstance(this)
    val databaseFile = NotallyDatabase.getCurrentDatabaseFile(this)
    return if (decrypt && preferences.biometricLockEncryptsDb.value && preferences.isLockEnabled) {
        val initializationVector =
            requireNotNull(preferences.iv.value) {
                "IV cannot be null when decrypting database for backup export"
            }
        val cipher = getInitializedCipherForDecryption(iv = initializationVector)
        val passphrase = cipher.doFinal(preferences.databaseEncryptionKey.value)
        val decryptedFile =
            withContext(Dispatchers.IO) { createTempFile(DATABASE_NAME, suffix, cacheDir) }
        decryptDatabase(this, passphrase, databaseFile, decryptedFile)
        Pair(database, decryptedFile)
    } else {
        val dbFile = withContext(Dispatchers.IO) { createTempFile(DATABASE_NAME, suffix, cacheDir) }
        databaseFile.copyToLarge(dbFile, overwrite = true)
        Pair(database, dbFile)
    }
}

private fun List<String>.toFileAttachments(): Sequence<FileAttachment> {
    return asSequence().flatMap { string -> Converters.jsonToFiles(string) }
}

private fun Sequence<FileAttachment>.export(
    zipFile: ZipFile,
    zipParameters: ZipParameters,
    subfolder: String,
    context: ContextWrapper,
    backupProgress: MutableStateFlow<Progress?>?,
    total: Int,
    counter: AtomicInteger,
    missingDisplayNames: MutableList<String>,
) {
    forEach { file ->
        try {
            if (!backupAttachmentFile(context, zipFile, zipParameters, subfolder, file.localName)) {
                val typePrefix =
                    when (subfolder) {
                        SUBFOLDER_IMAGES -> "Image"
                        SUBFOLDER_FILES -> "File"
                        else -> subfolder
                    }
                val display = file.originalName.ifBlank { file.localName }
                missingDisplayNames.add("$typePrefix: $display")
            }
        } catch (exception: Exception) {
            context.log(TAG, throwable = exception)
        } finally {
            backupProgress?.value =
                BackupProgress(
                    counter.incrementAndGet(),
                    total,
                    countSuffix = context.getQuantityString(R.plurals.attachments, total),
                )
        }
    }
}

fun WorkInfo.PeriodicityInfo.isEqualTo(value: Long, unit: TimeUnit): Boolean {
    return repeatIntervalMillis == unit.toMillis(value)
}

fun List<WorkInfo>.containsNonCancelled(): Boolean = any { it.state != WorkInfo.State.CANCELLED }

fun WorkManager.cancelAutoBackup() {
    Log.d(TAG, "Cancelling auto backup work")
    cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
}

fun WorkManager.updateAutoBackup(workInfos: List<WorkInfo>, autoBackPeriodInDays: Long) {
    Log.d(TAG, "Updating auto backup schedule for period: $autoBackPeriodInDays days")
    val workInfoId = workInfos.first().id
    val updatedWorkRequest =
        PeriodicWorkRequest.Builder(
                AutoBackupWorker::class.java,
                autoBackPeriodInDays.toLong(),
                TimeUnit.DAYS,
            )
            .setId(workInfoId)
            .build()
    updateWork(updatedWorkRequest)
}

fun WorkManager.scheduleAutoBackup(context: ContextWrapper, periodInDays: Long) {
    Log.d(TAG, "Scheduling auto backup for period: $periodInDays days")
    val request =
        PeriodicWorkRequest.Builder(AutoBackupWorker::class.java, periodInDays, TimeUnit.DAYS)
            .build()
    try {
        enqueueUniquePeriodicWork(AUTO_BACKUP_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    } catch (e: IllegalStateException) {
        // only happens in Unit-Tests
        context.log(TAG, "Scheduling auto backup failed", throwable = e)
    }
}

// Try to backup attachment by resolving it according to current/private/public directories.
// Returns true if the file was found and added to the zip, false if missing.
private fun backupAttachmentFile(
    context: ContextWrapper,
    zipFile: ZipFile,
    zipParameters: ZipParameters,
    folder: String,
    name: String,
): Boolean {
    val file = context.resolveAttachmentFile(folder, name)
    return if (file != null && file.exists()) {
        zipFile.addFile(file, zipParameters.copy("$folder/$name"))
        true
    } else {
        false
    }
}

private fun ContextWrapper.postSkippedAttachmentsNotification(missingAttachments: List<String>) {
    getSystemService<NotificationManager>()?.let { manager ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createChannelIfNotExists(NOTIFICATION_CHANNEL_ID)
        }
        val maxToShow = 10
        val lines = missingAttachments.take(maxToShow)
        val more = missingAttachments.size - lines.size
        val bigText = buildString {
            append(getString(R.string.auto_backup_skipped_files, missingAttachments.size))
            append(" (${getCurrentMediaRoot()})")
            append('\n')
            lines.forEachIndexed { idx, name ->
                if (idx > 0) append('\n')
                append("• ")
                append(name)
            }
            if (more > 0) {
                append('\n')
                append("+")
                append(more)
                append(" …")
            }
        }

        val cleanupIntent =
            Intent(this, CleanupMissingAttachmentsReceiver::class.java).apply {
                action = CleanupMissingAttachmentsReceiver.ACTION_CLEANUP_MISSING_ATTACHMENTS
            }
        val cleanupPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                cleanupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.export)
                .setContentTitle(getString(R.string.backup))
                .setContentText(
                    getString(R.string.auto_backup_skipped_files, missingAttachments.size) +
                        " (${getCurrentMediaRoot()})"
                )
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.delete, getString(R.string.clean_up), cleanupPendingIntent)
                .build()
        manager.notify(NOTIFICATION_ID + 1, notification)
    }
}

private fun ZipParameters.copy(fileNameInZip: String? = this.fileNameInZip): ZipParameters {
    return ZipParameters(this).apply { this@apply.fileNameInZip = fileNameInZip }
}

fun exportPdfFileFolder(
    app: ContextWrapper,
    note: BaseNote,
    folder: DocumentFile,
    fileName: String = note.title,
    pdfPrintListener: PdfPrintListener? = null,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    duplicateFileCount: Int = 1,
) {
    val validFileName = fileName.ifBlank { app.getString(R.string.note) }
    val filePath = "$validFileName.${ExportMimeType.PDF.fileExtension}"
    if (folder.findFile(filePath)?.exists() == true) {
        val duplicateFileName =
            findFreeDuplicateFileName(folder, validFileName, ExportMimeType.PDF.fileExtension)
        return exportPdfFileFolder(
            app,
            note,
            folder,
            duplicateFileName,
            pdfPrintListener,
            progress,
            counter,
            total,
            duplicateFileCount + 1,
        )
    }
    folder
        .createFileSafe(
            ExportMimeType.PDF.mimeType,
            validFileName,
            ".${ExportMimeType.PDF.fileExtension}",
        )
        .let { exportPdfFile(app, note, it, progress, counter, total, pdfPrintListener) }
}

fun exportPdfFile(
    app: ContextWrapper,
    note: BaseNote,
    outputFile: DocumentFile,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    pdfPrintListener: PdfPrintListener? = null,
) {
    val tempFile = DocumentFile.fromFile(File(app.getExportedPath(), "temp.pdf"))
    val html =
        note.toHtml(
            NotallyXPreferences.getInstance(app).showDateCreated(),
            app.getCurrentImagesDirectory(),
        )
    app.printPdf(
        tempFile,
        html,
        object : PdfPrintListener {
            override fun onSuccess(file: DocumentFile) {
                app.contentResolver.openOutputStream(outputFile.uri)?.use { outStream ->
                    app.contentResolver.openInputStream(file.uri)?.copyTo(outStream)
                }
                if (progress != null) {
                    progress.value =
                        BackupProgress(current = counter!!.incrementAndGet(), total = total!!)
                    if (counter.get() == total) {
                        pdfPrintListener?.onSuccess(file)
                    }
                } else {
                    pdfPrintListener?.onSuccess(file)
                }
            }

            override fun onFailure(message: CharSequence?) {
                pdfPrintListener?.onFailure(message)
            }
        },
    )
}

suspend fun exportPlainTextFileFolder(
    app: ContextWrapper,
    note: BaseNote,
    exportType: ExportMimeType,
    folder: DocumentFile,
    fileName: String = note.title,
    progress: MutableStateFlow<Progress?>? = null,
    counter: AtomicInteger? = null,
    total: Int? = null,
    duplicateFileCount: Int = 1,
): DocumentFile? {
    val validFileName = fileName.ifBlank { app.getString(R.string.note) }
    if (folder.findFile("$validFileName.${exportType.fileExtension}")?.exists() == true) {
        val duplicateFileName =
            findFreeDuplicateFileName(folder, validFileName, exportType.fileExtension)
        return exportPlainTextFileFolder(
            app,
            note,
            exportType,
            folder,
            duplicateFileName,
            progress,
            counter,
            total,
            duplicateFileCount + 1,
        )
    }
    return withContext(Dispatchers.IO) {
        val file =
            folder
                .createFileSafe(exportType.mimeType, validFileName, ".${exportType.fileExtension}")
                .let {
                    exportPlainTextFile(app, note, it, exportType)
                    it
                }
        progress?.value = BackupProgress(current = counter!!.incrementAndGet(), total = total!!)
        return@withContext file
    }
}

fun exportPlainTextFile(
    app: ContextWrapper,
    note: BaseNote,
    outputFile: DocumentFile,
    exportType: ExportMimeType,
) {
    app.contentResolver.openOutputStream(outputFile.uri)?.use { stream ->
        OutputStreamWriter(stream).use { writer ->
            writer.write(
                when (exportType) {
                    ExportMimeType.TXT ->
                        note.toTxt(includeTitle = false, includeCreationDate = false)

                    ExportMimeType.JSON -> note.toJson()
                    ExportMimeType.HTML ->
                        note.toHtml(
                            NotallyXPreferences.getInstance(app).showDateCreated(),
                            app.getCurrentImagesDirectory(),
                        )

                    ExportMimeType.MD -> note.toMarkdown()
                    else -> TODO("Unsupported MimeType for Export: $exportType")
                }
            )
        }
    }
}

private fun findFreeDuplicateFileName(
    folder: DocumentFile,
    fileName: String,
    fileExtension: String,
): String {
    val existingNames = folder.listFiles().mapNotNull { it.name }.toSet()
    if ("$fileName.$fileExtension" !in existingNames) return fileName

    var index = 0
    var newName: String

    do {
        index++
        newName = "$fileName ($index).$fileExtension"
    } while (newName in existingNames)

    return "$fileName ($index)"
}

fun Context.exportPreferences(preferences: NotallyXPreferences, uri: Uri): Boolean {
    try {
        contentResolver.openOutputStream(uri)?.use {
            it.write(preferences.toJsonString().toByteArray())
        } ?: return false
        return true
    } catch (e: IOException) {
        if (this is ContextWrapper) {
            log(TAG, throwable = e)
        } else {
            Log.e(TAG, "Export preferences failed", e)
        }
        return false
    }
}

private fun ContextWrapper.tryPostErrorNotification(e: Throwable) {
    fun postErrorNotification(e: Throwable) {
        getSystemService<NotificationManager>()?.let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createChannelIfNotExists(NOTIFICATION_CHANNEL_ID)
            }
            val bugIntent =
                try {
                    createReportBugIntent(
                        e.stackTraceToString(),
                        title = "Auto Backup failed",
                        body = "Error occurred during auto backup, see logs below",
                    )
                } catch (e: IllegalArgumentException) {
                    createReportBugIntent(
                        stackTrace =
                            "PLEASE PASTE YOUR NOTALLYX LOGS FILE CONTENT HERE (Error Notification -> 'View Logs')",
                        title = "Auto Backup failed",
                        body = "Error occurred during auto backup, see logs below.",
                    )
                }
            val notificationBuilder =
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.error)
                    .setContentTitle(getString(R.string.auto_backup_failed))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                getString(
                                    R.string.auto_backup_error_message,
                                    "${e.javaClass.simpleName}: ${e.localizedMessage}",
                                )
                            )
                    )
                    .addAction(
                        R.drawable.settings,
                        getString(R.string.settings),
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, R.id.Settings)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    .addAction(
                        R.drawable.error,
                        getString(R.string.report_bug),
                        PendingIntent.getActivity(
                            this,
                            0,
                            bugIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    .addAction(
                        R.drawable.text_file,
                        getString(R.string.view_logs),
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(getLogFileUri(), "text/plain")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )

            // Add a "Select Folder" button if the error is about a missing folder
            if (e is BackupFolderNotExistsException) {
                notificationBuilder.addAction(
                    R.drawable.settings,
                    getString(R.string.choose_folder),
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, R.id.Settings)
                            putExtra(SettingsFragment.EXTRA_SHOW_IMPORT_BACKUPS_FOLDER, true)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }

            manager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }
    if (
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    ) {
        postErrorNotification(e)
    }
}

fun MainActivity.exportNotes(
    notes: Collection<BaseNote>,
    mimeType: ExportMimeType,
    exportToFileResultLauncher: ActivityResultLauncher<Intent>,
    exportToFolderResultLauncher: ActivityResultLauncher<Intent>,
) {
    if (notes.size == 1) {
        exportNote(notes.first(), mimeType, exportToFileResultLauncher)
    } else {
        lifecycleScope.launch {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .apply { addCategory(Intent.CATEGORY_DEFAULT) }
                    .wrapWithChooser(this@exportNotes)
            exportToFolderResultLauncher.launch(intent)
        }
    }
}

fun Activity.exportNote(
    note: BaseNote,
    mimeType: ExportMimeType,
    exportToFileResultLauncher: ActivityResultLauncher<Intent>,
) {
    val suggestedName =
        (note.title.ifBlank { getString(R.string.note) }) + "." + mimeType.fileExtension
    val intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .apply {
                type = mimeType.mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, suggestedName)
            }
            .wrapWithChooser(this)
    exportToFileResultLauncher.launch(intent)
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
