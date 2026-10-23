package com.philkes.notallyx.data.model

import android.content.ContextWrapper
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class SearchResult(
    context: ContextWrapper,
    scope: CoroutineScope,
    var baseNoteDao: BaseNoteDao,
    private val transform: (List<BaseNote>) -> List<Item>,
) {

    private data class SearchParams(val keyword: String, val folder: Folder, val label: String?)

    private val searchParams = MutableStateFlow<SearchParams?>(null)

    private data class CompletedSearch(val params: SearchParams, val items: List<Item>)

    @Volatile private var completedSearch: CompletedSearch? = null

    /**
     * Verify both the request and the list actually submitted to the adapter, including debounce.
     */
    fun isCurrentResult(items: List<Item>): Boolean =
        completedSearch?.let { it.params == searchParams.value && it.items == items } == true

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val resultFlow: Flow<List<Item>> =
        searchParams
            .debounce(100)
            .distinctUntilChanged()
            .flatMapLatest { params ->
                if (params == null) {
                    _isLoading.value = false
                    flowOf(emptyList())
                } else {
                    _isLoading.value = true
                    baseNoteDao
                        .getByKeyword(params.keyword, params.folder, params.label)
                        .map { transform(it) }
                        .onEach {
                            completedSearch = CompletedSearch(params, it)
                            _isLoading.value = false
                        }
                        .catch {
                            completedSearch = CompletedSearch(params, emptyList())
                            _isLoading.value = false
                            context.log(TAG, throwable = it)
                            emit(emptyList())
                        }
                        .onCompletion { _isLoading.value = false }
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

val SearchResult?.isEmpty: Boolean
    get() = this?.value?.isEmpty() == true
