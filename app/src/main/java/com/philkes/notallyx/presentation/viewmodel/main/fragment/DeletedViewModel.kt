package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Attachment
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import com.philkes.notallyx.utils.deleteAttachments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletedViewModel(app: Application) : BaseViewModel(app) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val deletedNotes: StateFlow<List<Item>> =
        databaseStateFlow
            .flatMapLatest { database ->
                database?.getBaseNoteDao()?.getByFolder(Folder.DELETED) ?: flowOf(emptyList())
            }
            .map { list -> app.createItemsFromNotes(list) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun deleteAllTrashedBaseNotes() {
        val dao = baseNoteDao ?: return
        viewModelScope.launch {
            val ids: LongArray
            val images = ArrayList<FileAttachment>()
            val files = ArrayList<FileAttachment>()
            val audios = ArrayList<Audio>()
            withContext(Dispatchers.IO) {
                ids = dao.getDeletedNoteIds()
                val imageStrings = dao.getDeletedNoteImages()
                val fileStrings = dao.getDeletedNoteFiles()
                val audioStrings = dao.getDeletedNoteAudios()
                imageStrings.flatMapTo(images) { json -> Converters.jsonToFiles(json) }
                fileStrings.flatMapTo(files) { json -> Converters.jsonToFiles(json) }
                audioStrings.flatMapTo(audios) { json -> Converters.jsonToAudios(json) }
                dao.deleteFrom(Folder.DELETED)
            }
            val attachments = ArrayList<Attachment>(images.size + files.size + audios.size)
            attachments.addAll(images)
            attachments.addAll(files)
            attachments.addAll(audios)
            withContext(Dispatchers.IO) { app.deleteAttachments(attachments, ids) }
        }
    }
}
