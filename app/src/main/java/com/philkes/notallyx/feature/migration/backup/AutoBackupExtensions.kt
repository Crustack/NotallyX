package com.philkes.notallyx.feature.migration.backup

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_AUDIOS
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.createChannelIfNotExists
import com.philkes.notallyx.core.common.extensions.createFileSafe
import com.philkes.notallyx.core.common.extensions.createReportBugIntent
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentFilesDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.getDocumentFolder
import com.philkes.notallyx.core.common.extensions.getLogFileUri
import com.philkes.notallyx.core.common.extensions.listZipFiles
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.md5Hash
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.core.common.extensions.verify
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.editor.BackupFile
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.settings.SettingsViewModel
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod

private const val TAG = "AutoBackupExtensions"
internal const val NOTIFICATION_CHANNEL_ID = "AutoBackups"
internal const val NOTIFICATION_ID = 123412
private const val OUTPUT_DATA_BACKUP_URI = "backupUri"

const val AUTO_BACKUP_WORK_NAME = "com.philkes.notallyx.AutoBackupWork"
const val OUTPUT_DATA_EXCEPTION = "exception"

const val ON_SAVE_BACKUP_FILE = "NotallyX_AutoBackup"
const val PERIODIC_BACKUP_FILE_PREFIX = "NotallyX_Backup_"
const val ON_SAVE_BACKUP_TEMP_FILE = "${ON_SAVE_BACKUP_FILE}_tmp"
const val ON_SAVE_BACKUP_PREVIOUS_FILE = "${ON_SAVE_BACKUP_FILE}_previous"
private val ON_SAVE_BACKUP_LEFTOVER_FILES =
    listOf(ON_SAVE_BACKUP_TEMP_FILE, ON_SAVE_BACKUP_PREVIOUS_FILE)

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
                folder.listZipFiles(backupFilePrefix).forEach {
                    if (it.length() == 0L) {
                        log(TAG, msg = "Deleting empty backup leftover '${it.name}'")
                        it.delete()
                    }
                }
                val previousBackups = folder.listZipFiles(backupFilePrefix)
                val name =
                    "$backupFilePrefix${FILE_TIMESTAMP_FORMAT.format(System.currentTimeMillis())}"
                log(TAG, msg = "Creating '$uri/$name.zip'...")
                zipDocument = folder.createFileSafe(MIME_TYPE_ZIP, name, ".zip")
                val (exportedNotes, exportedAttachments) =
                    app.exportAsZip(zipDocument.uri, password = preferences.backupPassword.value)
                log(TAG, msg = "Exported $exportedNotes notes and $exportedAttachments attachments")

                if (exportedNotes == 0 && previousBackups.isNotEmpty()) {
                    log(
                        TAG,
                        msg =
                            "Backup contains no notes and previous backups exist, skipping backup",
                    )
                    zipDocument.delete()
                    return@withLock Result.success()
                }

                val backupFiles =
                    folder.listZipFiles(backupFilePrefix).sortedByDescending { it.name.orEmpty() }
                log(TAG, msg = "Found ${backupFiles.size} backups")
                val backupsToBeDeleted = backupFiles.drop(maxBackups)
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
                log(TAG, msg = "Finished backup to '${zipDocument.uri}'")
                preferences.periodicBackupLastExecution.save(Date().time)
                return@withLock Result.success(
                    Data.Builder().putString(OUTPUT_DATA_BACKUP_URI, zipDocument.uri.path!!).build()
                )
            } catch (e: Exception) {
                log(TAG, msg = "Failed creating backup to '$path'", throwable = e)
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
            val hasPrevious =
                existingBackup != null &&
                    try {
                        extractBackupWithAnyPassword(
                            existingBackup.uri,
                            previousDir,
                            listOf(previousPassword, password).distinct(),
                        )
                        true
                    } catch (e: ZipException) {
                        preserveUnreadableBackup(folder, existingBackup, e)
                        previousDir.deleteRecursively()
                        previousDir.mkdirs()
                        false
                    }

            val resultZip = File(workDir, "result.zip")
            if (savedNote == null || !hasPrevious) {
                if (savedNote != null) {
                    log(TAG, msg = "Creating full backup since no readable auto backup ZIP exists")
                }
                Log.d(TAG, "Creating full auto backup")
                val (exportedNotes, _) = exportAsZip(Uri.fromFile(resultZip), password = password)
                if (hasPrevious && exportedNotes == 0) {
                    log(TAG, msg = "Database contains no notes, keeping the existing auto backup")
                    return@withLock
                }
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
                zipDirectory(newDir, resultZip, password)
            }

            replaceFileSafely(
                folder,
                ON_SAVE_BACKUP_FILE,
                ON_SAVE_BACKUP_TEMP_FILE,
                ON_SAVE_BACKUP_PREVIOUS_FILE,
                resultZip,
            )
            Log.d(TAG, "Finished auto backup")
            log(TAG, msg = "Finished backup to '${folder.uri}/$ON_SAVE_BACKUP_FILE.zip'")
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

private fun ContextWrapper.extractBackupWithAnyPassword(
    uri: Uri,
    outputDir: File,
    passwords: List<String>,
) {
    val tempZipFile = File.createTempFile("previous", ".zip", outputDir.parentFile ?: cacheDir)
    try {
        val inputStream =
            contentResolver.openInputStream(uri) ?: throw IOException("Can not open '$uri'")
        inputStream.use { input -> tempZipFile.outputStream().use { input.copyToLarge(it) } }
        val passwordsToTry =
            passwords.filter { isBackupPasswordSet(it) }.distinct() + PASSWORD_EMPTY
        var lastException: Exception? = null
        for (candidate in passwordsToTry) {
            try {
                extractZipToDirectory(tempZipFile, outputDir, candidate)
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

private fun ContextWrapper.recoverInterruptedReplace(folder: DocumentFile) {
    if (folder.findFile("$ON_SAVE_BACKUP_FILE.zip")?.exists() == true) {
        return
    }
    val leftover =
        ON_SAVE_BACKUP_LEFTOVER_FILES.firstNotNullOfOrNull { name ->
            folder.findFile("$name.zip")?.takeIf { it.exists() && it.length() > 0 }
        } ?: return
    log(TAG, msg = "Restoring interrupted auto backup '${leftover.name}'")
    if (!leftover.renameTo("$ON_SAVE_BACKUP_FILE.zip")) {
        val target = folder.createFileSafe(MIME_TYPE_ZIP, ON_SAVE_BACKUP_FILE, ".zip")
        copyDocumentVerified(leftover.uri, target.uri)
        leftover.delete()
    }
}

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

private fun ContextWrapper.replaceFileSafely(
    folder: DocumentFile,
    fileName: String,
    tempFileName: String,
    previousFileName: String,
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
    val old = folder.findFile("$fileName.zip")?.takeIf { it.exists() }
    if (old == null) {
        if (temp.renameTo("$fileName.zip")) {
            return
        }
    } else {
        folder.findFile("$previousFileName.zip")?.delete()
        if (old.renameTo("$previousFileName.zip")) {
            if (temp.renameTo("$fileName.zip")) {
                old.delete()
                return
            }
            if (!old.renameTo("$fileName.zip")) {
                throw IOException(
                    "Replacing '$fileName.zip' failed, the old and new ZIP are kept as '$previousFileName.zip' and '$tempFileName.zip'"
                )
            }
            runCatching { temp.delete() }
            throw IOException("Replacing '$fileName.zip' failed, the old ZIP was kept")
        }
        if (!old.delete()) {
            runCatching { temp.delete() }
            throw IOException("Could not delete old backup '${old.uri}'")
        }
    }
    log(TAG, msg = "Renaming '${temp.uri}' failed, writing '$fileName.zip' directly")
    val target = folder.createFileSafe(MIME_TYPE_ZIP, fileName, ".zip")
    try {
        writeFileVerified(source, target.uri)
    } catch (e: Exception) {
        runCatching { target.delete() }
        throw e
    }
    temp.delete()
}

private fun ContextWrapper.writeFileVerified(source: File, target: Uri) {
    val outputStream =
        contentResolver.openOutputStream(target)
            ?: throw IOException("Could not open output stream for '$target'")
    outputStream.use { out ->
        (out as? FileOutputStream)?.channel?.truncate(0)
        FileInputStream(source).use { input -> input.copyToLarge(out) }
        out.flush()
    }
    val targetMd5 = runCatching { md5Hash(target) }.getOrNull()
    if (targetMd5 == null || !targetMd5.contentEquals(source.md5Hash())) {
        throw IOException("Writing '$target' failed, its content does not match '${source.name}'")
    }
}

private fun ContextWrapper.copyDocumentVerified(source: Uri, target: Uri) {
    val tempFile = File.createTempFile("copy", ".zip", cacheDir)
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
    val isEncrypted = isBackupPasswordSet(password)
    val zipFile = ZipFile(zip, if (isEncrypted) password.toCharArray() else null)
    val zipParameters =
        ZipParameters().apply {
            isEncryptFiles = isEncrypted
            compressionLevel = CompressionLevel.NO_COMPRESSION
            if (isEncryptFiles) {
                encryptionMethod = EncryptionMethod.AES
            }
            isIncludeRootFolder = false
        }
    zipFile.addFolder(sourceDir, zipParameters)
    zipFile.verify(File(sourceDir, DATABASE_NAME))
}

private fun extractZipToDirectory(zip: File, outputDir: File, password: String) {
    val zipFile = ZipFile(zip, if (isBackupPasswordSet(password)) password.toCharArray() else null)
    zipFile.extractAll(outputDir.absolutePath)
}

private fun ContextWrapper.requireBackupFolder(path: String, msg: String): DocumentFile? {
    val uri = path.toUri()
    return try {
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

fun ContextWrapper.checkBackupOnSave(
    preferences: NotallyXPreferences,
    note: BaseNote? = null,
    forceFullBackup: Boolean = false,
) {
    if (preferences.backupOnSave.value) {
        val backupPath = preferences.backupsFolder.value
        if (backupPath != EMPTY_PATH) {
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

internal fun ContextWrapper.tryPostErrorNotification(e: Throwable) {
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
                            Intent(this, MainActivity::class.java)
                                .putNavKey(NotallyNavKey.Settings),
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
            runCatching { getLogFileUri() }
                .getOrNull()
                ?.let { logUri ->
                    notificationBuilder.addAction(
                        R.drawable.text_file,
                        getString(R.string.view_logs),
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(logUri, "text/plain")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }

            // Add a "Select Folder" button if the error is about a missing folder
            if (e is BackupFolderNotExistsException) {
                notificationBuilder.addAction(
                    R.drawable.settings,
                    getString(R.string.choose_folder),
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java).apply {
                            putNavKey(NotallyNavKey.Settings)
                            putExtra(SettingsViewModel.EXTRA_SHOW_IMPORT_BACKUPS_FOLDER, true)
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
        try {
            postErrorNotification(e)
        } catch (notificationException: Exception) {
            log(TAG, msg = "Failed to post error notification", throwable = notificationException)
        }
    }
}
