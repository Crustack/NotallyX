package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ArchivedViewModel(app: Application) : BaseViewModel(app) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val archivedNotes: StateFlow<List<Item>> =
        databaseStateFlow
            .flatMapLatest { database ->
                database?.getBaseNoteDao()?.getByFolder(Folder.ARCHIVED) ?: flowOf(emptyList())
            }
            .map { list -> app.createItemsFromNotes(list) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
}
