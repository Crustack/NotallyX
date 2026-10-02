package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

class UnlabeledViewModel(app: Application) : BaseViewModel(app) {

    val unlabeledNotes: LiveData<List<Item>> = databaseLiveData.switchMapNullSafe { database ->
        database!!
            .getBaseNoteDao()
            .getBaseNotesWithoutLabel(Folder.NOTES)
            .map { list -> app.createItemsFromNotes(list) }
            .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
}
