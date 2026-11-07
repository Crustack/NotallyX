package com.philkes.notallyx.feature.migration.backup

import android.content.ContextWrapper
import android.database.Cursor
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import androidx.core.database.getLongOrNull
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_AUDIOS
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.clearDirectory
import com.philkes.notallyx.core.common.extensions.copyToFile
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentFilesDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.toMessage
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.common.toNotallyXReminder
import com.philkes.notallyx.core.database.ConverterErrorReporter
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.SQLCipherUtils
import com.philkes.notallyx.core.security.decryptDatabase
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.parseToColorString
import com.philkes.notallyx.feature.migration.imports.ImportResult
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import com.philkes.notallyx.feature.reminder.pinAndScheduleReminders
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException

private const val TAG = "ImportExtensions"

private fun getOptionalColumns(db: SQLiteDatabase, tableName: String): Array<String> {
    val existingColumns = mutableSetOf<String>()

    // 1. Get the actual columns currently in the DB
    db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            existingColumns.add(cursor.getString(nameIndex))
        }
    }

    // 2. Map your targets to either the real column or a default value
    return existingColumns
        .mapNotNull { colName ->
            when {
                // Special handling for your body substr logic
                colName == "body" && existingColumns.contains("body") ->
                    "SUBSTR(body, 1, $MAX_BODY_CHAR_LENGTH) AS body"
                else -> colName
            }
        }
        .toTypedArray()
}

suspend fun ContextWrapper.importRawDatabase(
    dbFileUri: Uri,
    checkDuplicates: Boolean,
    importProgress: MutableStateFlow<Progress?>? = null,
): ImportResult {
    val tempDbFile = File(cacheDir, DATABASE_NAME + "_IMPORT")
    try {
        requireNotNull(
                contentResolver.openInputStream(dbFileUri),
                { "InputStream for dbFileUri '$dbFileUri' is null" },
            )
            .use { inputStream ->
                inputStream.copyToFile(tempDbFile)
                val (baseNotes, originalIds, labels, corruptedNotes) =
                    readBaseNotes(tempDbFile, importProgress)
                val import = import(baseNotes, originalIds, labels, corruptedNotes, checkDuplicates)
                importProgress?.value = ImportProgress(inProgress = false)
                return import
            }
    } finally {
        tempDbFile.delete()
    }
}

data class BaseNotesImport(
    val baseNotes: List<BaseNote>,
    val originalIds: List<Long>,
    val labels: List<Label>,
    val corruptedNotes: Int,
)

private fun ContextWrapper.readBaseNotes(
    dbFile: File,
    progress: MutableStateFlow<Progress?>? = null,
): BaseNotesImport {
    val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    try {
        val labelCursor = database.query("Label", null, null, null, null, null, null)

        val safeColumns = getOptionalColumns(database, "BaseNote")

        val baseNoteCursor = database.query("BaseNote", safeColumns, null, null, null, null, null)
        val (labels, _) = labelCursor.toList { cursor -> cursor.toLabel() }

        val total = baseNoteCursor.count
        var counter = 1
        progress?.value = ImportProgress(total = total)
        val originalIds = ArrayList<Long>(baseNoteCursor.count)
        val (baseNotes, corrupted) =
            baseNoteCursor.toList { cursor ->
                val baseNote = cursor.toBaseNote(database)
                val originalId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                originalIds.add(originalId)
                progress?.value = ImportProgress(counter++, total)
                baseNote
            }
        return BaseNotesImport(baseNotes, originalIds, labels, corrupted)
    } finally {
        database.close()
    }
}

/**
 * We only import the images/files referenced in notes. e.g If someone has added garbage to the ZIP
 * file, like a 100 MB image, ignore it.
 */
suspend fun ContextWrapper.importZip(
    zipFileUri: Uri,
    databaseFolder: File,
    zipPassword: String,
    checkDuplicates: Boolean,
    progress: MutableStateFlow<Progress?>? = null,
) {
    progress?.value = ImportProgress(indeterminate = true)
    try {
        val result =
            withContext(Dispatchers.IO) {
                val stream =
                    requireNotNull(
                        contentResolver.openInputStream(zipFileUri),
                        { "InputStream for zipFileUri '$zipFileUri' is null" },
                    )
                val tempZipFile = File(databaseFolder, "TEMP.zip")
                stream.copyToFile(tempZipFile)
                val zipFile = ZipFile(tempZipFile)
                if (zipFile.isEncrypted) {
                    zipFile.setPassword(zipPassword.toCharArray())
                }
                zipFile.extractFile(
                    DATABASE_NAME,
                    databaseFolder.path,
                    DATABASE_NAME,
                )

                var dbFile = File(databaseFolder, DATABASE_NAME)
                val state = SQLCipherUtils.getDatabaseState(this@importZip, dbFile)
                if (state == SQLCipherUtils.State.ENCRYPTED) {
                    val fallbackEncryptionKey =
                        NotallyXPreferences.getInstance(this@importZip)
                            .fallbackDatabaseEncryptionKey
                            .value
                    if (fallbackEncryptionKey != null) {
                        val dbFileDecrypted = File(databaseFolder, "${DATABASE_NAME}-decrypted")
                        decryptDatabase(
                            this@importZip,
                            fallbackEncryptionKey,
                            dbFile,
                            dbFileDecrypted,
                        )
                        dbFile = dbFileDecrypted
                    } else {
                        throw IllegalArgumentException(
                            "Backup contains encrypted database and 'fallbackDatabaseEncryptionKey' has no value!"
                        )
                    }
                }

                delay(1000)
                val (baseNotes, originalIds, labels, corruptedNotes) =
                    readBaseNotes(dbFile, progress)
                val total =
                    baseNotes.fold(0) { acc, baseNote ->
                        acc + baseNote.images.size + baseNote.files.size + baseNote.audios.size
                    }
                progress?.value = ImportProgress(total = total, stage = ImportStage.IMPORT_FILES)

                val current = AtomicInteger(1)
                val imageRoot = getCurrentImagesDirectory()
                val fileRoot = getCurrentFilesDirectory()
                val audioRoot = getCurrentAudioDirectory()
                baseNotes.forEach { baseNote ->
                    importFiles(
                        baseNote.images,
                        SUBFOLDER_IMAGES,
                        imageRoot,
                        zipFile,
                        current,
                        total,
                        progress,
                    )
                    importFiles(
                        baseNote.files,
                        SUBFOLDER_FILES,
                        fileRoot,
                        zipFile,
                        current,
                        total,
                        progress,
                    )
                    baseNote.audios.forEach { audio ->
                        try {
                            val audioFilePath = "$SUBFOLDER_AUDIOS/${audio.name}"
                            val entry = zipFile.getFileHeader(audioFilePath)
                            if (entry != null) {
                                val name = "${UUID.randomUUID()}.m4a"
                                zipFile.extractFile(audioFilePath, audioRoot!!.path, name)
                                audio.name = name
                            }
                        } catch (exception: Exception) {
                            log(TAG, throwable = exception)
                        } finally {
                            progress?.value =
                                ImportProgress(
                                    current.getAndIncrement(),
                                    total,
                                    stage = ImportStage.IMPORT_FILES,
                                )
                        }
                    }
                }
                import(baseNotes, originalIds, labels, corruptedNotes, checkDuplicates)
            }
        databaseFolder.clearDirectory()
        showToast(toMessage(result))
    } catch (e: ZipException) {
        if (e.type == ZipException.Type.WRONG_PASSWORD) {
            log(TAG, throwable = e)
            showToast(R.string.wrong_password)
        } else {
            throw e
        }
    } finally {
        progress?.value = ImportProgress(inProgress = false)
    }
}

private suspend fun ContextWrapper.import(
    baseNotes: List<BaseNote>,
    originalIds: List<Long>,
    labels: List<Label>,
    readCorrupted: Int,
    checkDuplicates: Boolean,
): ImportResult {
    val notallyDatabase = NotallyDatabase.getDatabase(this@import).value
    val importResult =
        notallyDatabase!!
            .getCommonDao()
            .importBackup(baseNotes, originalIds, labels, readCorrupted, checkDuplicates)
    val notesToRemind = notallyDatabase.getBaseNoteDao().getAllWithRemindersOrPinned()
    cancelPinAndReminders(notesToRemind)
    pinAndScheduleReminders(notesToRemind)
    return importResult
}

private fun ContextWrapper.importFiles(
    files: List<FileAttachment>,
    subFolder: String,
    localFolder: File?,
    zipFile: ZipFile,
    current: AtomicInteger,
    total: Int,
    importingBackup: MutableStateFlow<Progress?>? = null,
) {
    files.forEach { file ->
        try {
            val entry = zipFile.getFileHeader("$subFolder/${file.localName}")
            if (entry != null) {
                val extension = file.localName.substringAfterLast(".")
                val name = "${UUID.randomUUID()}.$extension"
                zipFile.extractFile("$subFolder/${file.localName}", localFolder!!.path, name)
                file.localName = name
            }
        } catch (e: Exception) {
            log(TAG, throwable = e)
        } finally {
            importingBackup?.value =
                ImportProgress(current.getAndIncrement(), total, stage = ImportStage.IMPORT_FILES)
        }
    }
}

private fun Cursor.toLabel(): Label {
    val value = this.getString(getColumnIndexOrThrow("value"))
    val orderIndex = getColumnIndex("order")
    val order = if (orderIndex != -1) getInt(orderIndex) else 0
    return Label(value, order)
}

private fun Cursor.toBaseNote(sourceDb: SQLiteDatabase): BaseNote {
    val typeTmp = getString(getColumnIndexOrThrow("type"))
    val folderTmp = getString(getColumnIndexOrThrow("folder"))
    val color =
        getString(getColumnIndexOrThrow("color"))?.parseToColorString() ?: BaseNote.COLOR_DEFAULT
    val title = getString(getColumnIndexOrThrow("title"))
    val pinnedTmp = getInt(getColumnIndexOrThrow("pinned"))
    val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
    val modifiedTimestampIndex = getColumnIndex("modifiedTimestamp")
    val modifiedTimestamp =
        if (modifiedTimestampIndex == -1) {
            timestamp
        } else {
            getLongOrNull(modifiedTimestampIndex) ?: timestamp
        }
    val labelsTmp = getString(getColumnIndexOrThrow("labels"))
    val id = getLong(getColumnIndexOrThrow("id"))
    val body =
        try {
            getString(getColumnIndexOrThrow("body"))
        } catch (_: SQLiteBlobTooBigException) {
            // Fall back to truncated read from source DB to avoid cursor window overflow
            val cursor =
                sourceDb.rawQuery(
                    "SELECT substr(body, 1, ?) AS body FROM BaseNote WHERE id = ?",
                    arrayOf(
                        MAX_BODY_CHAR_LENGTH.toString(),
                        id.toString(),
                    ),
                )
            val value = if (cursor.moveToFirst()) cursor.getString(0) else ""
            cursor.close()
            value
        }
    val spansTmp = getString(getColumnIndexOrThrow("spans"))
    val itemsTmp = getString(getColumnIndexOrThrow("items"))

    val pinned =
        when (pinnedTmp) {
            0 -> false
            1 -> true
            else -> throw IllegalArgumentException("pinned must be 0 or 1")
        }

    val isPinnedToStatusColumn = getColumnIndex("isPinnedToStatus")
    val pinnedToStatusBar =
        isPinnedToStatusColumn != -1 &&
            when (getInt(isPinnedToStatusColumn)) {
                0 -> false
                1 -> true
                else -> false
            }

    val type = Type.valueOfOrDefault(typeTmp)
    val folder = Folder.valueOfOrDefault(folderTmp)

    val labels = Converters.jsonToLabels(labelsTmp)
    val spans = Converters.jsonToSpans(spansTmp).filter { it.isInsideBounds() }
    val items = Converters.jsonToItems(itemsTmp)

    val imagesIndex = getColumnIndex("images")
    val images =
        if (imagesIndex != -1) {
            Converters.jsonToFiles(getString(imagesIndex))
        } else emptyList()

    val filesIndex = getColumnIndex("files")
    val files =
        if (filesIndex != -1) {
            Converters.jsonToFiles(getString(filesIndex))
        } else emptyList()

    val audiosIndex = getColumnIndex("audios")
    val audios =
        if (audiosIndex != -1) {
            Converters.jsonToAudios(getString(audiosIndex))
        } else emptyList()

    val remindersIndex = getColumnIndex("reminders")
    val reminders =
        if (remindersIndex != -1) {
            Converters.jsonToReminders(getString(remindersIndex))
        } else {
            // Notally introduced "reminder" column
            val reminderIndex = getColumnIndex("reminder")
            if (reminderIndex != -1) {
                val reminder = getString(reminderIndex).toNotallyXReminder()
                reminder?.let { listOf(it) } ?: emptyList()
            } else emptyList()
        }

    val viewModeIndex = getColumnIndex("viewMode")
    val viewMode =
        if (viewModeIndex != -1) {
            NoteViewMode.valueOfOrDefault(getString(viewModeIndex))
        } else NoteViewMode.EDIT
    return BaseNote(
        0,
        type,
        folder,
        color,
        title,
        pinned,
        timestamp,
        modifiedTimestamp,
        labels,
        body,
        spans,
        items,
        images,
        files,
        audios,
        reminders,
        viewMode,
        pinnedToStatusBar,
    )
}

private fun <T> Cursor.toList(convert: (cursor: Cursor) -> T): Pair<ArrayList<T>, Int> =
    try {
        ConverterErrorReporter.enabled.set(false)
        val list = ArrayList<T>(count)
        var corrupted = 0
        while (moveToNext()) {
            try {
                list.add(convert(this))
            } catch (e: Exception) {
                Log.e(TAG, "Error while converting DB cursor", e)
                corrupted++
            }
        }
        close()
        Pair(list, corrupted)
    } finally {
        ConverterErrorReporter.enabled.set(true)
    }
