package com.philkes.notallyx.utils.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.database.getLongOrNull
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.imports.ImportProgress
import com.philkes.notallyx.data.imports.ImportResult
import com.philkes.notallyx.data.imports.ImportStage
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ConverterErrorReporter
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.parseToColorString
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.NotallyModel.FileType
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.FileError
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import com.philkes.notallyx.utils.cancelPinAndReminders
import com.philkes.notallyx.utils.clearDirectory
import com.philkes.notallyx.utils.copyToFile
import com.philkes.notallyx.utils.determineMimeTypeAndExtension
import com.philkes.notallyx.utils.getCurrentAudioDirectory
import com.philkes.notallyx.utils.getCurrentFilesDirectory
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import com.philkes.notallyx.utils.getFileName
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.mimeTypeToFileExtension
import com.philkes.notallyx.utils.pinAndScheduleReminders
import com.philkes.notallyx.utils.rename
import com.philkes.notallyx.utils.security.SQLCipherUtils
import com.philkes.notallyx.utils.security.decryptDatabase
import com.philkes.notallyx.utils.toMessage
import com.philkes.notallyx.utils.toNotallyXReminder
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ImportExtensions"

fun getOptionalColumns(db: SQLiteDatabase, tableName: String): Array<String> {
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
    importProgress: MutableLiveData<Progress>? = null,
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
                importProgress?.postValue(ImportProgress(inProgress = false))
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

fun ContextWrapper.readBaseNotes(
    dbFile: File,
    progress: MutableLiveData<Progress>? = null,
): BaseNotesImport {
    val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    try {
        val labelCursor = database.query("Label", null, null, null, null, null, null)

        val safeColumns = getOptionalColumns(database, "BaseNote")

        val baseNoteCursor = database.query("BaseNote", safeColumns, null, null, null, null, null)
        val (labels, _) = labelCursor.toList { cursor -> cursor.toLabel() }

        val total = baseNoteCursor.count
        var counter = 1
        progress?.postValue(ImportProgress(0, total))
        val originalIds = ArrayList<Long>(baseNoteCursor.count)
        val (baseNotes, corrupted) =
            baseNoteCursor.toList { cursor ->
                val baseNote = cursor.toBaseNote(database)
                val originalId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                originalIds.add(originalId)
                progress?.postValue(ImportProgress(counter++, total))
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
    progress: MutableLiveData<Progress>? = null,
) {
    progress?.postValue(ImportProgress(indeterminate = true))
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
                    NotallyDatabase.DATABASE_NAME,
                    databaseFolder.path,
                    NotallyDatabase.DATABASE_NAME,
                )

                var dbFile = File(databaseFolder, NotallyDatabase.DATABASE_NAME)
                val state = SQLCipherUtils.getDatabaseState(dbFile)
                if (state == SQLCipherUtils.State.ENCRYPTED) {
                    val fallbackEncryptionKey =
                        NotallyXPreferences.getInstance(this@importZip)
                            .fallbackDatabaseEncryptionKey
                            .value
                    if (fallbackEncryptionKey != null) {
                        val dbFileDecrypted =
                            File(databaseFolder, "${NotallyDatabase.DATABASE_NAME}-decrypted")
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
                progress?.postValue(ImportProgress(0, total, stage = ImportStage.IMPORT_FILES))

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
                            progress?.postValue(
                                ImportProgress(
                                    current.getAndIncrement(),
                                    total,
                                    stage = ImportStage.IMPORT_FILES,
                                )
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
    val notallyDatabase = NotallyDatabase.getDatabase(this, observePreferences = false).value
    val importResult =
        notallyDatabase
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
    importingBackup: MutableLiveData<Progress>? = null,
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
            importingBackup?.postValue(
                ImportProgress(current.getAndIncrement(), total, stage = ImportStage.IMPORT_FILES)
            )
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
                        com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
                            .toString(),
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
        if (isPinnedToStatusColumn != -1) {
            when (getInt(isPinnedToStatusColumn)) {
                0 -> false
                1 -> true
                else -> false
            }
        } else false

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

fun Context.importPreferences(jsonFile: Uri, to: SharedPreferences.Editor): Boolean {
    try {
        val inputStream: InputStream? = contentResolver.openInputStream(jsonFile)
        val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: return false
        val jsonObject = JSONObject(jsonString)
        to.clear()
        jsonObject.keys().forEach { key ->
            when (val value = jsonObject.get(key)) {
                is Int -> to.putInt(key, value)
                is Boolean -> to.putBoolean(key, value)
                is Double -> to.putFloat(key, value.toFloat())
                is Long -> to.putLong(key, value)
                is JSONArray -> {
                    val set = (0 until value.length()).map { value.getString(it) }.toSet()
                    to.putStringSet(key, set)
                }

                else -> to.putString(key, value.toString())
            }
        }
        return to.commit()
    } catch (e: Exception) {
        if (this is ContextWrapper) {
            log(TAG, "Import preferences from '$jsonFile' failed", throwable = e)
        } else {
            Log.e(TAG, "Import preferences from '$jsonFile' failed", e)
        }
        return false
    }
}

suspend fun Context.importFile(
    uri: Uri,
    directory: File,
    fileType: FileType,
    errorWhileRenaming: Int = R.string.error_while_renaming_file,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    return withContext(Dispatchers.IO) {
        val document =
            requireNotNull(
                DocumentFile.fromSingleUri(this@importFile, uri),
                { "importFile: could not read file from: '$uri'" },
            )
        val displayName = document.name ?: getString(R.string.unknown_name)
        try {

            /*
            If we have reached this point, an SD card (emulated or real) exists and externalRoot
            is not null. externalRoot.exists() can be false if the folder `Images` has been deleted after
            the previous line, but externalRoot itself can't be null
            */
            val temp = File(directory, "Temp")

            val inputStream =
                requireNotNull(
                    contentResolver.openInputStream(uri),
                    { "importFile: InputStream for '$uri' is null" },
                )
            inputStream.copyToFile(temp)

            val originalName = getFileName(uri)
            when (fileType) {
                FileType.IMAGE -> {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(temp.path, options)
                    val mimeType = options.outMimeType ?: proposedMimeType

                    if (mimeType != null) {
                        val extension = mimeType.mimeTypeToFileExtension()
                        if (extension != null) {
                            val name = "${UUID.randomUUID()}.$extension"
                            if (temp.rename(name)) {
                                return@withContext Pair(
                                    FileAttachment(name, originalName ?: name, mimeType),
                                    null,
                                )
                            } else {
                                // I don't expect this error to ever happen but just in
                                // case
                                return@withContext Pair(
                                    null,
                                    FileError(displayName, getString(errorWhileRenaming), fileType),
                                )
                            }
                        } else
                            return@withContext Pair(
                                null,
                                FileError(
                                    displayName,
                                    getString(R.string.image_format_not_supported),
                                    fileType,
                                ),
                            )
                    } else
                        return@withContext Pair(
                            null,
                            FileError(displayName, getString(R.string.invalid_image), fileType),
                        )
                }

                FileType.ANY -> {
                    val (mimeType, fileExtension) =
                        contentResolver.determineMimeTypeAndExtension(uri, proposedMimeType)
                    val name = "${UUID.randomUUID()}${fileExtension}"
                    if (temp.rename(name)) {
                        return@withContext Pair(
                            FileAttachment(name, originalName ?: name, mimeType),
                            null,
                        )
                    } else {
                        // I don't expect this error to ever happen but just in case
                        return@withContext Pair(
                            null,
                            FileError(displayName, getString(errorWhileRenaming), fileType),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (this is ContextWrapper) {
                log(TAG, throwable = e)
            } else {
                Log.e(TAG, "Import file failed", e)
            }
            return@withContext Pair(
                null,
                FileError(displayName, getString(R.string.unknown_error), fileType),
            )
        }
    }
}

suspend fun ContextWrapper.importFile(
    uri: Uri,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    val filesRoot = getCurrentFilesDirectory()
    requireNotNull(filesRoot) { "filesRoot is null" }
    return importFile(uri, filesRoot, FileType.ANY, proposedMimeType = proposedMimeType)
}

suspend fun ContextWrapper.importImage(
    uri: Uri,
    proposedMimeType: String? = null,
): Pair<FileAttachment?, FileError?> {
    val imagesRoot = getCurrentImagesDirectory()
    requireNotNull(imagesRoot) { "imagesRoot is null" }
    return importFile(uri, imagesRoot, FileType.IMAGE, proposedMimeType = proposedMimeType)
}

suspend fun ContextWrapper.importAudio(original: File, deleteOriginalFile: Boolean): Audio {
    return withContext(Dispatchers.IO) {
        /*
        Regenerate because the directory may have been deleted between the time of activity creation
        and audio recording
        */
        val audioRoot = getCurrentAudioDirectory()
        requireNotNull(audioRoot) { "audioRoot is null" }

        /*
        If we have reached this point, an SD card (emulated or real) exists and audioRoot
        is not null. audioRoot.exists() can be false if the folder `Audio` has been deleted after
        the previous line, but audioRoot itself can't be null
        */
        val name = "${UUID.randomUUID()}.m4a"
        val final = File(audioRoot, name)
        val input = FileInputStream(original)
        input.copyToFile(final)

        if (deleteOriginalFile) {
            original.delete()
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(final.path)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        Audio(name, duration?.toLong(), System.currentTimeMillis())
    }
}
