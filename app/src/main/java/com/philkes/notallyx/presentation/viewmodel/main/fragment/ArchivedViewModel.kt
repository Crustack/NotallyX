package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel

class ArchivedViewModel(app: Application) : BaseViewModel(app) {

    val archivedNotes: LiveData<List<Item>> = databaseLiveData.switchMapNullSafe { database ->
        database!!.getBaseNoteDao().getFrom(Folder.ARCHIVED).map { list ->
            app.createItemsFromNotes(list)
        }
    }
}
