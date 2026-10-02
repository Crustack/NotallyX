package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel

class NotesViewModel(app: Application) : BaseViewModel(app) {

    val baseNotes: LiveData<List<Item>> = databaseLiveData.switchMapNullSafe { database ->
        preferences.labelsHidden.getData().switchMap { labelsHidden ->
            database!!.getBaseNoteDao().getFrom(Folder.NOTES).map { list ->
                val filtered = list.filter { baseNote ->
                    baseNote.labels.none { labelsHidden.contains(it) }
                }
                app.createItemsFromNotes(filtered)
            }
        }
    }
}
