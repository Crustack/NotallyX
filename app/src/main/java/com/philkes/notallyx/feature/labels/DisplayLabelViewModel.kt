package com.philkes.notallyx.feature.labels

// import com.philkes.notallyx.createItemsFromNotes
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DisplayLabelViewModel(app: Application, preferences: NotallyXPreferences) :
    BaseViewModel(app, preferences) {

    private val labelCache = HashMap<String, StateFlow<List<Item>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotesByLabel(label: String): StateFlow<List<Item>> {
        return labelCache.getOrPut(label) {
            databaseStateFlow
                .flatMapLatest { database ->
                    database?.getBaseNoteDao()?.getByLabel(label)?.catch { emit(emptyList()) }
                        ?: flowOf(emptyList())
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
