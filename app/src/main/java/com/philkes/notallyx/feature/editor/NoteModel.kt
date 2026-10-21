package com.philkes.notallyx.feature.editor

import android.app.Application
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.core.text.getSpans
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.Cache
import com.philkes.notallyx.core.common.extensions.deleteAttachments
import com.philkes.notallyx.core.common.extensions.getCurrentAudioDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentFilesDirectory
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.getTempAudioFile
import com.philkes.notallyx.core.common.result.Event
import com.philkes.notallyx.core.common.result.FileError
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.DatabaseNotConnectedException
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.TextSizeSp
import com.philkes.notallyx.core.ui.extensions.applySpans
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.data.model.extensions.attachmentsDifferFrom
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.migration.backup.checkBackupOnSave
import com.philkes.notallyx.feature.migration.backup.importAudio
import com.philkes.notallyx.feature.migration.backup.importFile
import com.philkes.notallyx.feature.migration.imports.txt.extractListItems
import com.philkes.notallyx.feature.migration.imports.txt.findListSyntaxRegex
import com.philkes.notallyx.feature.migration.progress.AddFilesProgress
import com.philkes.notallyx.feature.reminder.ReminderReceiver
import com.philkes.notallyx.feature.reminder.RemindersActivity.Companion.NEW_REMINDER_ID
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import com.philkes.notallyx.feature.reminder.cancelReminder
import com.philkes.notallyx.feature.reminder.scheduleReminder
import com.philkes.notallyx.feature.widget.NoteWidget
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class BackupFile(val targetPath: String, val file: File)

sealed interface EditorDialogState {
    data class ColorPicker(
        val currentColor: String,
        val colors: Set<String>,
        val onColorSelected: (selectedColor: String) -> Unit,
    ) : EditorDialogState

    data class DeleteForeverConfirm(val onConfirm: () -> Unit) : EditorDialogState

    data class AudioPermissionRationale(val onConfirm: () -> Unit) : EditorDialogState

    data class PermissionRationale(
        val messageResId: Int,
        val onConfirm: () -> Unit,
    ) : EditorDialogState

    data class RejectionConfirm(
        val msgResId: Int,
        val onConfirm: () -> Unit,
    ) : EditorDialogState

    data class FileErrors(val errors: List<FileError>) : EditorDialogState

    data class Error(
        val titleResId: Int,
        val message: String?,
    ) : EditorDialogState

    data class EditLink(
        val urlBefore: String,
        val displayTextBefore: String,
        val isNoteUrl: Boolean,
        val onConfirm: (urlAfter: String?, displayTextAfter: String) -> Unit,
        val onDismiss: () -> Unit = {},
    ) : EditorDialogState
}

/** Core ViewModel for note content editing, attachments, reminders, and auto-save logic. */
open class NoteModel(
    protected val app: Application,
    val preferences: NotallyXPreferences,
) : AndroidViewModel(app) {

    val activeDialog = MutableStateFlow<EditorDialogState?>(null)

    protected val database = NotallyDatabase.getDatabase(app)
    protected var baseNoteDao: BaseNoteDao? = null
    private val attachmentMutex = Mutex()
    val textSize: TextSizeSp = preferences.textSizeNoteEditor.value

    var isNewNote = true

    var type = Type.NOTE

    var id = 0L
    var folder = Folder.NOTES
    var color = preferences.defaultNoteColor.value

    var title = String()
    var pinned = false
    var isPinnedToStatus = false
    var timestamp = System.currentTimeMillis()
    var modifiedTimestamp = System.currentTimeMillis()

    val labels = ArrayList<String>()

    var body: Editable = SpannableStringBuilder()

    val items = ArrayList<ListItem>()

    val images: StateFlow<List<FileAttachment>>
        field = MutableStateFlow<List<FileAttachment>>(emptyList())
    val files: StateFlow<List<FileAttachment>>
        field = MutableStateFlow<List<FileAttachment>>(emptyList())
    val audios: StateFlow<List<Audio>>
        field = MutableStateFlow<List<Audio>>(emptyList())
    val reminders: StateFlow<List<Reminder>>
        field = MutableStateFlow<List<Reminder>>(emptyList())
    val viewMode: StateFlow<NoteViewMode>
        field = MutableStateFlow(NoteViewMode.EDIT)
    val addingFiles: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)
    val eventBus: StateFlow<Event<List<FileError>>?>
        field = MutableStateFlow<Event<List<FileError>>?>(null)

    fun setViewMode(mode: NoteViewMode) {
        viewMode.value = mode
    }

    fun toggleViewMode(): NoteViewMode {
        val newMode =
            when (viewMode.value) {
                NoteViewMode.EDIT -> NoteViewMode.READ_ONLY
                NoteViewMode.READ_ONLY -> NoteViewMode.EDIT
            }
        setViewMode(newMode)
        return newMode
    }

    var imageRoot = app.getCurrentImagesDirectory()
    var audioRoot = app.getCurrentAudioDirectory()
    var filesRoot = app.getCurrentFilesDirectory()

    var originalNote: BaseNote? = null

    init {
        viewModelScope.launch {
            database.collect { db -> db?.let { baseNoteDao = it.getBaseNoteDao() } }
        }
    }

    fun addAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            val audio = app.importAudio(app.getTempAudioFile(), true)
            attachmentMutex.withLock {
                val copy = ArrayList(audios.value)
                copy.add(audio)
                audios.value = copy
                updateAudios()
            }
        }
    }

    fun deleteAudio(audio: Audio) {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentMutex.withLock {
                val copy = ArrayList(audios.value)
                copy.remove(audio)
                audios.value = copy
                updateAudios()
            }
            app.deleteAttachments(arrayListOf(audio))
        }
    }

    fun addImages(uris: Array<Uri>) {
        /*
        Regenerate because the directory may have been deleted between the time of activity creation
        and image addition
         */
        imageRoot = app.getCurrentImagesDirectory()
        requireNotNull(imageRoot) { "imageRoot is null" }
        addFiles(uris, imageRoot!!, FileType.IMAGE)
    }

    fun addFiles(uris: Array<Uri>) {
        /*
        Regenerate because the directory may have been deleted between the time of activity creation
        and image addition
         */
        filesRoot = app.getCurrentFilesDirectory()
        requireNotNull(filesRoot) { "filesRoot is null" }
        addFiles(uris, filesRoot!!, FileType.ANY)
    }

    private fun addFiles(uris: Array<Uri>, directory: File, fileType: FileType) {
        val errorWhileRenaming =
            if (fileType == FileType.IMAGE) {
                R.string.error_while_renaming_image
            } else {
                R.string.error_while_renaming_file
            }
        viewModelScope.launch(Dispatchers.IO) {
            addingFiles.value = AddFilesProgress(0, uris.size)

            val successes = ArrayList<FileAttachment>()
            val errors = ArrayList<FileError>()

            uris.forEachIndexed { index, uri ->
                val (fileAttachment, error) =
                    app.importFile(uri, directory, fileType, errorWhileRenaming)
                fileAttachment?.let { successes.add(it) }
                error?.let { errors.add(it) }
                addingFiles.value = AddFilesProgress(index + 1, uris.size)
            }

            addingFiles.value = AddFilesProgress(inProgress = false)

            if (successes.isNotEmpty()) {
                attachmentMutex.withLock {
                    val copy =
                        when (fileType) {
                            FileType.IMAGE -> ArrayList(images.value)
                            FileType.ANY -> ArrayList(files.value)
                        }
                    copy.addAll(successes)
                    when (fileType) {
                        FileType.IMAGE -> {
                            images.value = copy
                            updateImages()
                        }

                        FileType.ANY -> {
                            files.value = copy
                            updateFiles()
                        }
                    }
                }
            }

            if (errors.isNotEmpty()) {
                eventBus.value = Event(errors)
            }
        }
    }

    fun deleteImages(list: ArrayList<FileAttachment>) {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentMutex.withLock {
                val copy = ArrayList(images.value)
                copy.removeAll(list)
                images.value = copy
                updateImages()
            }
            app.deleteAttachments(list)
        }
    }

    fun deleteFiles(list: ArrayList<FileAttachment>) {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentMutex.withLock {
                val copy = ArrayList(files.value)
                copy.removeAll(list)
                files.value = copy
                updateFiles()
            }
            app.deleteAttachments(list)
        }
    }

    fun setLabels(list: List<String>) {
        labels.clear()
        labels.addAll(list)
    }

    suspend fun setState(id: Long, createInDb: Boolean = true) {
        if (id != 0L) {
            isNewNote = false

            val cachedNote = Cache.list.find { baseNote -> baseNote.id == id }
            val baseNote = cachedNote ?: withContext(Dispatchers.IO) { baseNoteDao?.get(id) }

            if (baseNote != null) {
                originalNote = baseNote.deepCopy()

                this.id = id
                folder = baseNote.folder
                color = baseNote.color

                title = baseNote.title
                pinned = baseNote.pinned
                timestamp = baseNote.timestamp
                modifiedTimestamp = baseNote.modifiedTimestamp

                setLabels(baseNote.labels)

                body = baseNote.body.applySpans(baseNote.spans)

                items.clear()
                items.addAll(baseNote.items.init())

                images.value = baseNote.images
                files.value = baseNote.files
                audios.value = baseNote.audios
                reminders.value = baseNote.reminders
                setViewMode(baseNote.viewMode)
                isPinnedToStatus = baseNote.isPinnedToStatus
            } else {
                originalNote = createBaseNote(createInDb)
                app.showToast(R.string.cant_find_note)
            }
        } else originalNote = createBaseNote(createInDb)
    }

    private suspend fun createBaseNote(createInDb: Boolean = true): BaseNote {
        val baseNote = getBaseNote()
        if (createInDb) {
            id =
                withContext(Dispatchers.IO) {
                    baseNoteDao?.insertSafe(app, baseNote) ?: throw DatabaseNotConnectedException()
                }
        }
        return baseNote.copy(id = id)
    }

    suspend fun deleteBaseNote(checkAutoSave: Boolean = true) {
        app.cancelPinAndReminders(id, reminders.value)
        withContext(Dispatchers.IO) {
            baseNoteDao?.delete(id) ?: throw DatabaseNotConnectedException()
        }
        NoteWidget.sendBroadcast(app, longArrayOf(id))
        val attachments = ArrayList(images.value + files.value + audios.value)
        if (attachments.isNotEmpty()) {
            withContext(Dispatchers.IO) { app.deleteAttachments(attachments) }
        }
        if (checkAutoSave) {
            app.checkBackupOnSave(preferences, forceFullBackup = true)
        }
    }

    fun setItems(items: List<ListItem>) {
        this.items.clear()
        this.items.addAll(items)
    }

    suspend fun saveNote(checkBackupOnSave: Boolean = true): Long {
        return withContext(Dispatchers.IO) {
            val note = getBaseNote()
            val id = baseNoteDao?.insertSafe(app, note) ?: throw DatabaseNotConnectedException()
            if (checkBackupOnSave) {
                checkBackupOnSave(note)
            }
            if (!note.equalContents(originalNote)) {
                app.sendBroadcast(
                    Intent(app, ReminderReceiver::class.java).apply {
                        action = ReminderReceiver.ACTION_UPDATE_NOTIFICATIONS
                        putExtra(ReminderReceiver.EXTRA_NOTE_ID, id)
                    }
                )
            }
            this@NoteModel.id = id
            isNewNote = false
            originalNote = note.copy(id = id).deepCopy()
            return@withContext id
        }
    }

    suspend fun checkBackupOnSave(note: BaseNote = getBaseNote()) {
        app.checkBackupOnSave(
            preferences,
            note = note,
            forceFullBackup = originalNote?.attachmentsDifferFrom(note) == true,
        )
    }

    fun isEmpty(): Boolean {
        return title.isEmpty() &&
            body.isEmpty() &&
            items.none { item -> item.body.isNotEmpty() } &&
            files.value.isEmpty() &&
            images.value.isEmpty() &&
            audios.value.isEmpty()
    }

    fun isModified(): Boolean {
        return getBaseNote() != originalNote
    }

    private suspend fun updateImages() {
        withContext(Dispatchers.IO) {
            baseNoteDao?.updateImages(id, images.value) ?: throw DatabaseNotConnectedException()
        }
    }

    private suspend fun updateFiles() {
        withContext(Dispatchers.IO) {
            baseNoteDao?.updateFiles(id, files.value) ?: throw DatabaseNotConnectedException()
        }
    }

    private suspend fun updateAudios() {
        withContext(Dispatchers.IO) {
            baseNoteDao?.updateAudios(id, audios.value) ?: throw DatabaseNotConnectedException()
        }
    }

    fun getBaseNote(): BaseNote {
        val spans = getFilteredSpans(body)
        val body = this.body.toString()
        val nonEmptyItems = this.items.filter { item -> item.body.isNotEmpty() }
        return BaseNote(
            id,
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
            nonEmptyItems,
            images.value,
            files.value,
            audios.value,
            reminders.value,
            viewMode.value,
            isPinnedToStatus,
        )
    }

    private fun getFilteredSpans(spanned: Spanned): ArrayList<SpanRepresentation> {
        val representations = LinkedHashSet<SpanRepresentation>()
        spanned.getSpans<CharacterStyle>().forEach { span ->
            val end = spanned.getSpanEnd(span)
            val start = spanned.getSpanStart(span)
            val representation =
                SpanRepresentation(start, end, false, false, null, false, false, false)

            when (span) {
                is StyleSpan -> {
                    representation.bold = span.style == Typeface.BOLD
                    representation.italic = span.style == Typeface.ITALIC
                }

                is URLSpan -> {
                    representation.link = true
                    representation.linkData = span.url
                }
                is TypefaceSpan -> representation.monospace = span.family == "monospace"
                is StrikethroughSpan -> representation.strikethrough = true
            }

            if (representation.isNotUseless()) {
                representations.add(representation)
            }
        }
        return getFilteredRepresentations(ArrayList(representations))
    }

    private fun getFilteredRepresentations(
        representations: ArrayList<SpanRepresentation>
    ): ArrayList<SpanRepresentation> {
        representations.forEachIndexed { index, representation ->
            val match = representations.find { spanRepresentation ->
                spanRepresentation.isEqualInSize(representation)
            }
            if (match != null && representations.indexOf(match) != index) {
                if (match.bold) {
                    representation.bold = true
                }
                if (match.link) {
                    representation.link = true
                    representation.linkData = match.linkData
                }
                if (match.italic) {
                    representation.italic = true
                }
                if (match.monospace) {
                    representation.monospace = true
                }
                if (match.strikethrough) {
                    representation.strikethrough = true
                }
                val copy = ArrayList(representations)
                copy[index] = representation
                copy.remove(match)
                return getFilteredRepresentations(copy)
            }
        }
        return representations
    }

    suspend fun removeReminder(reminder: Reminder) {
        app.cancelReminder(this.id, reminder.id)
        val updatedReminders = reminders.value.filter { it.id != reminder.id }
        updateReminders(updatedReminders)
    }

    suspend fun addReminder(reminder: Reminder) {
        val updatedReminders = ArrayList(reminders.value)
        val newReminder = reminder.copy(id = (updatedReminders.maxOfOrNull { it.id } ?: -1) + 1)
        updatedReminders.add(newReminder)
        updateReminders(updatedReminders)
        app.scheduleReminder(id, newReminder)
    }

    suspend fun updateReminder(updatedReminder: Reminder) {
        if (updatedReminder.id != NEW_REMINDER_ID) {
            app.cancelReminder(id, updatedReminder.id)
        }
        val updatedReminders = reminders.value.toMutableList()
        val idx = updatedReminders.indexOfFirst { it.id == updatedReminder.id }
        if (idx != -1) {
            updatedReminders[idx] = updatedReminder
            updateReminders(updatedReminders)
            app.scheduleReminder(id, updatedReminder)
        }
    }

    private suspend fun updateReminders(updatedReminders: List<Reminder>) {
        reminders.value = updatedReminders
        withContext(Dispatchers.IO) {
            baseNoteDao?.updateReminders(id, updatedReminders)
                ?: throw DatabaseNotConnectedException()
        }
    }

    suspend fun convertTo(noteType: Type) {
        when (noteType) {
            Type.NOTE -> {
                body = SpannableStringBuilder(items.joinToString(separator = "\n") { it.body })
                type = Type.NOTE
                setItems(ArrayList())
            }
            Type.LIST -> {
                val text = body.toString()
                val listSyntaxRegex =
                    text.findListSyntaxRegex(checkContains = true, plainNewLineAllowed = true)
                if (listSyntaxRegex != null) {
                    setItems(text.extractListItems(listSyntaxRegex))
                } else {
                    setItems(
                        text.lines().mapIndexed { idx, itemText ->
                            ListItem(itemText, false, false, idx, mutableListOf())
                        }
                    )
                }
                type = Type.LIST
                body = SpannableStringBuilder()
            }
        }
        Cache.list = ArrayList()
        saveNote(checkBackupOnSave = false)
    }

    suspend fun refreshOriginalNote() {
        if (id == 0L) return
        val baseNote =
            withContext(Dispatchers.IO) {
                baseNoteDao?.get(id) ?: throw DatabaseNotConnectedException()
            }
        if (baseNote == null) return
        originalNote = baseNote.deepCopy()
        reminders.value = baseNote.reminders
    }

    enum class FileType {
        IMAGE,
        ANY,
    }
}
