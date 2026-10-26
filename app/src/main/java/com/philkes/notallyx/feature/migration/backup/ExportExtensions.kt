package com.philkes.notallyx.feature.migration.backup

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_AUDIOS
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.ZipVerificationException
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.createChannelIfNotExists
import com.philkes.notallyx.core.common.extensions.getCurrentMediaRoot
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.md5Hash
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.verify
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.decryptDatabase
import com.philkes.notallyx.core.security.getInitializedCipherForDecryption
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.migration.progress.BackupProgress
import java.io.File
import java.io.File.createTempFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod

private const val TAG = "ExportExtensions"
val FILE_TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss_SSS", Locale.ENGLISH)
val LOG_DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

typealias NotesAndAttachments = Pair<Int, Int>

internal fun isBackupPasswordSet(password: String?): Boolean =
    !password.isNullOrBlank() && password != PASSWORD_EMPTY

suspend fun ContextWrapper.exportAsZip(
    fileUri: Uri,
    compress: Boolean = false,
    password: String = PASSWORD_EMPTY,
    backupProgress: MutableStateFlow<Progress?>? = null,
    retryOnFail: Boolean = true,
): NotesAndAttachments {
    backupProgress?.value = BackupProgress(indeterminate = true)
    val tempFile = createTempFile("export", "tmp", cacheDir)
    var databaseCopyFile: File? = null
    try {
        val isEncrypted = isBackupPasswordSet(password)
        val zipFile = ZipFile(tempFile, if (isEncrypted) password.toCharArray() else null)
        val zipParameters =
            ZipParameters().apply {
                isEncryptFiles = isEncrypted
                if (!compress) {
                    compressionLevel = CompressionLevel.NO_COMPRESSION
                }
                if (isEncryptFiles) {
                    encryptionMethod = EncryptionMethod.AES
                }
            }

        val (databaseOriginal, databaseCopy) = copyDatabase()
        databaseCopyFile = databaseCopy
        zipFile.addFile(databaseCopy, zipParameters.copy(DATABASE_NAME))

        val totalNotes = databaseOriginal.getBaseNoteDao().count()
        val images = databaseOriginal.getBaseNoteDao().getAllImages().toFileAttachments()
        val files = databaseOriginal.getBaseNoteDao().getAllFiles().toFileAttachments()
        val audios = databaseOriginal.getBaseNoteDao().getAllAudios()
        val totalAttachments = images.count() + files.count() + audios.size
        backupProgress?.value =
            BackupProgress(
                total = totalAttachments,
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
        contentResolver.openOutputStream(fileUri)?.use { outputStream ->
            (outputStream as? FileOutputStream)?.channel?.truncate(0)
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
        backupProgress?.value = BackupProgress(inProgress = false)
        // Post skipped attachments notification if any were missing
        if (missingAttachments.isNotEmpty()) {
            postSkippedAttachmentsNotification(missingAttachments)
        }
        return Pair(totalNotes, totalAttachments)
    } finally {
        tempFile.delete()
        try {
            databaseCopyFile?.delete()
        } catch (e: Exception) {
            log(
                TAG,
                msg = "Failed to delete db copy file: '${databaseCopyFile?.absolutePath}'",
                throwable = e,
            )
        }
    }
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
        if (databaseFile.exists()) {
            databaseFile.copyToLarge(dbFile, overwrite = true)
        } else {
            dbFile.delete()
            database.openHelper.writableDatabase.execSQL("VACUUM INTO '${dbFile.absolutePath}'")
        }
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
