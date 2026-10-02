package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

class DisplayLabelViewModel(app: Application) : BaseViewModel(app) {

    private val labelCache = HashMap<String, LiveData<List<Item>>>()

    fun getNotesByLabel(label: String): LiveData<List<Item>> {
        return labelCache.getOrPut(label) {
            databaseLiveData.switchMapNullSafe { database ->
                database!!
                    .getBaseNoteDao()
                    .getBaseNotesByLabel(label)
                    .map { list -> app.createItemsFromNotes(list) }
                    .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
            }
        }
    }
}
