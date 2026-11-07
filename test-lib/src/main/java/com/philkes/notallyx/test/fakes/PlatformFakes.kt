package com.philkes.notallyx.test.fakes

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import com.philkes.notallyx.core.database.DatabaseDriverFactory
import com.philkes.notallyx.core.database.JvmBundledDatabaseDriverFactory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.FileStorage
import com.philkes.notallyx.core.platform.JvmFileStorage
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.platform.PlatformUri
import com.philkes.notallyx.core.platform.TaskScheduler
import com.philkes.notallyx.core.ui.platform.AppPermission
import com.philkes.notallyx.core.ui.platform.FilePicker
import com.philkes.notallyx.core.ui.platform.PermissionChecker
import com.philkes.notallyx.core.ui.platform.ShareHandler
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.feature.media.audio.AudioPlayer
import com.philkes.notallyx.feature.media.audio.AudioRecorder
import com.philkes.notallyx.feature.migration.backup.PdfExporter
import com.philkes.notallyx.feature.reminder.ReminderScheduler
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure-Kotlin platform fakes for running unit tests on JVM without Robolectric or `ContextWrapper`.
 */
class FakePathProvider(
    rootDir: File =
        File(System.getProperty("java.io.tmpdir"), "notallyx-test-${System.nanoTime()}").apply {
            mkdirs()
        }
) : PathProvider by DesktopPathProvider(baseDirOverride = rootDir) {
    val rootDirectory: File = rootDir
}

class FakeFileStorage : FileStorage by JvmFileStorage()

class FakeDatabaseDriverFactory(
    override val pathProvider: PathProvider = FakePathProvider(),
) : DatabaseDriverFactory {
    private val delegate = JvmBundledDatabaseDriverFactory(pathProvider)

    override fun createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<NotallyDatabase> {
        return delegate.createBuilder(databaseName, dataInPublicFolder, inMemory)
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        return delegate.createDriver(databaseName, dataInPublicFolder)
    }
}

class FakeReminderScheduler(var exactAlarmsAllowed: Boolean = true) : ReminderScheduler {
    val scheduledReminders = mutableListOf<Pair<Long, Reminder>>()
    val cancelledReminders = mutableListOf<Pair<Long, Long>>()
    val pinnedNotes = mutableListOf<BaseNote>()
    val unpinnedNoteIds = mutableListOf<Long>()

    override fun scheduleReminder(noteId: Long, reminder: Reminder, forceRepetition: Boolean) {
        scheduledReminders.add(noteId to reminder)
    }

    override fun cancelReminder(noteId: Long, reminderId: Long) {
        cancelledReminders.add(noteId to reminderId)
    }

    override fun cancelAllRemindersForNote(noteId: Long, reminders: List<Reminder>) {
        reminders.forEach { cancelReminder(noteId, it.id) }
    }

    override fun cancelAllRemindersForNotes(notes: Collection<BaseNote>) {
        notes.forEach { cancelAllRemindersForNote(it.id, it.reminders) }
    }

    override fun rescheduleAll(notesWithReminders: List<BaseNote>) {
        notesWithReminders.forEach { note ->
            note.reminders.forEach { scheduleReminder(note.id, it) }
        }
    }

    override fun pinNoteToNotificationBar(note: BaseNote) {
        pinnedNotes.add(note)
    }

    override fun unpinNoteFromNotificationBar(noteId: Long) {
        unpinnedNoteIds.add(noteId)
    }

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed
}

class FakeTaskScheduler : TaskScheduler {
    var periodicBackupsScheduled = false
    var lastPeriodicBackupDays: Long = 0L
    var autoRemoveDeletedNotesScheduled = false
    var cleanupMissingAttachmentsTriggered = false

    override fun schedulePeriodicBackup(periodInDays: Long) {
        periodicBackupsScheduled = true
        lastPeriodicBackupDays = periodInDays
    }

    override fun cancelPeriodicBackup() {
        periodicBackupsScheduled = false
    }

    override fun scheduleAutoRemoveDeletedNotes() {
        autoRemoveDeletedNotesScheduled = true
    }

    override fun cancelAutoRemoveDeletedNotes() {
        autoRemoveDeletedNotesScheduled = false
    }

    override fun triggerMissingAttachmentsCleanup() {
        cleanupMissingAttachmentsTriggered = true
    }
}

class FakePermissionChecker(
    val grantedPermissions: MutableSet<AppPermission> = AppPermission.entries.toMutableSet()
) : PermissionChecker {
    override fun isGranted(permission: AppPermission): Boolean = permission in grantedPermissions
}

class FakeShareHandler : ShareHandler {
    val sharedTextNotes = mutableListOf<Pair<String, String>>()
    val sharedFiles = mutableListOf<Pair<PlatformUri, String>>()
    val openedExternalFiles = mutableListOf<Pair<PlatformUri, String>>()
    val openedUrls = mutableListOf<String>()
    val sentBugReports = mutableListOf<Triple<String, String, String>>()

    override fun shareText(title: String, body: String) {
        sharedTextNotes.add(title to body)
    }

    override fun shareFile(uri: PlatformUri, mimeType: String) {
        sharedFiles.add(uri to mimeType)
    }

    override fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean {
        openedExternalFiles.add(uri to mimeType)
        return true
    }

    override fun openUrl(url: String): Boolean {
        openedUrls.add(url)
        return true
    }

    override fun sendBugReportEmail(recipient: String, subject: String, body: String) {
        sentBugReports.add(Triple(recipient, subject, body))
    }
}

class FakePdfExporter : PdfExporter {
    val exportedHtmls = mutableListOf<Pair<String, PlatformUri>>()

    override suspend fun exportHtmlToPdf(
        htmlContent: String,
        targetUri: PlatformUri,
    ): Result<PlatformUri> {
        exportedHtmls.add(htmlContent to targetUri)
        return Result.success(targetUri)
    }
}

class FakeFilePicker(
    var nextPickedFile: PlatformUri? = null,
    var nextPickedFiles: List<PlatformUri> = emptyList(),
    var nextPickedDirectory: PlatformUri? = null,
    var nextCreatedDocument: PlatformUri? = null,
) : FilePicker {
    override suspend fun pickFile(mimeTypes: List<String>): PlatformUri? = nextPickedFile

    override suspend fun pickMultipleFiles(mimeTypes: List<String>): List<PlatformUri> =
        nextPickedFiles

    override suspend fun pickDirectory(): PlatformUri? = nextPickedDirectory

    override suspend fun createDocument(fileName: String, mimeType: String): PlatformUri? =
        nextCreatedDocument
}

class FakeAudioPlayer : AudioPlayer {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _errorCode = MutableStateFlow<Int?>(null)
    override val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    var loadedFile: File? = null

    override suspend fun loadAudio(file: File) {
        loadedFile = file
    }

    override fun play() {
        _isPlaying.value = true
    }

    override fun pause() {
        _isPlaying.value = false
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }

    override fun seekBy(deltaMs: Long) {
        _currentPositionMs.value = (_currentPositionMs.value + deltaMs).coerceAtLeast(0L)
    }

    override fun release() {
        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }
}

class FakeAudioRecorder(
    override val isSupported: Boolean = true,
    var outputFile: File? = null,
) : AudioRecorder {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedDurationMs = MutableStateFlow(0L)
    override val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs.asStateFlow()

    override fun startRecording() {
        _isRecording.value = true
    }

    override fun stopRecording(): File? {
        _isRecording.value = false
        return outputFile
    }

    override fun release() {
        _isRecording.value = false
    }
}
