package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DisplayLabelViewModel(app: Application) : BaseViewModel(app) {

    private val labelCache = HashMap<String, StateFlow<List<Item>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotesByLabel(label: String): StateFlow<List<Item>> {
        return labelCache.getOrPut(label) {
            databaseStateFlow
                .flatMapLatest { database ->
                    database?.getBaseNoteDao()?.getByLabel(label) ?: flowOf(emptyList())
                }
                .map { list -> app.createItemsFromNotes(list) }
                .flowOn(Dispatchers.IO)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )
        }
    }
}
