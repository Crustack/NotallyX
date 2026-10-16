package com.philkes.notallyx.feature.main.routes.notes

// import com.philkes.notallyx.createItemsFromNotes
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(app: Application, preferences: NotallyXPreferences) :
    BaseViewModel(app, preferences) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val baseNotes: StateFlow<List<Item>> =
        combine(
                databaseStateFlow,
                preferences.labelsHidden.flow,
            ) { database, labelsHidden ->
                Pair(database, labelsHidden)
            }
            .flatMapLatest { (database, labelsHidden) ->
                database?.getBaseNoteDao()?.getByFolder(Folder.NOTES)?.map { list ->
                    val filtered = list.filter { baseNote ->
                        baseNote.labels.none { labelsHidden.contains(it) }
                    }
                    app.createItemsFromNotes(filtered)
                } ?: flowOf(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
}
