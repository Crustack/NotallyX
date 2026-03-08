package com.philkes.notallyx.data.model

import android.content.ContextWrapper
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

class SearchResult(
    context: ContextWrapper,
    scope: CoroutineScope,
    var baseNoteDao: BaseNoteDao,
    private val transform: (List<BaseNote>) -> List<Item>,
) {

    private data class SearchParams(val keyword: String, val folder: Folder, val label: String?)

    private val searchParams = MutableStateFlow<SearchParams?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: LiveData<Boolean> =
        _isLoading.asLiveData(scope.coroutineContext + Dispatchers.Main)

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
                        .getBaseNotesByKeyword(params.keyword, params.folder, params.label)
                        .map { transform(it) }
                        .onEach { _isLoading.value = false }
                        .catch {
                            _isLoading.value = false
                            context.log(TAG, throwable = it)
                            emit(emptyList())
                        }
                        .onCompletion { _isLoading.value = false }
                }
            }
            .flowOn(Dispatchers.IO)

    val results: LiveData<List<Item>> =
        resultFlow.asLiveData(scope.coroutineContext + Dispatchers.Main)

    fun fetch(keyword: String, folder: Folder, label: String?) {
        searchParams.value = SearchParams(keyword, folder, label)
    }

    fun observe(
        owner: androidx.lifecycle.LifecycleOwner,
        observer: androidx.lifecycle.Observer<List<Item>>,
    ) {
        results.observe(owner, observer)
    }

    val value: List<Item>?
        get() = results.value

    companion object {
        private const val TAG = "SearchResult"
    }
}

val SearchResult?.isEmpty: Boolean
    get() = this?.value?.isEmpty() == true
