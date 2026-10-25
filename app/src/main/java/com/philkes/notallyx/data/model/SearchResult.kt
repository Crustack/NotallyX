package com.philkes.notallyx.data.model

import android.content.ContextWrapper
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.data.dao.BaseNoteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SearchResult(
    context: ContextWrapper,
    scope: CoroutineScope,
    var baseNoteDao: BaseNoteDao,
    private val transform: (List<BaseNote>) -> List<Item>,
) {

    private data class SearchParams(val keyword: String, val folder: Folder, val label: String?)

    private val searchParams = MutableStateFlow<SearchParams?>(null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val resultFlow: Flow<List<Item>> =
        searchParams
            .debounce(100)
            .distinctUntilChanged()
            .flatMapLatest { params ->
                if (params == null) {
                    flowOf(emptyList())
                } else {
                    baseNoteDao
                        .getByKeyword(params.keyword, params.folder, params.label)
                        .map { transform(it) }
                        .catch {
                            context.log(TAG, throwable = it)
                            emit(emptyList())
                        }
                }
            }
            .flowOn(Dispatchers.IO)

    val results: StateFlow<List<Item>> =
        resultFlow.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun fetch(keyword: String, folder: Folder, label: String?) {
        searchParams.value = SearchParams(keyword, folder, label)
    }

    val value: List<Item>
        get() = results.value

    companion object {
        private const val TAG = "SearchResult"
    }
}
