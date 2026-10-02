package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Attachment
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import com.philkes.notallyx.utils.deleteAttachments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletedViewModel(app: Application) : BaseViewModel(app) {

    val deletedNotes: LiveData<List<Item>> = databaseLiveData.switchMapNullSafe { database ->
        database!!.getBaseNoteDao().getFrom(Folder.DELETED).map { list ->
            app.createItemsFromNotes(list)
        }
    }

    init {
        databaseLiveData.observeForever(::initDatabase)
    }

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
