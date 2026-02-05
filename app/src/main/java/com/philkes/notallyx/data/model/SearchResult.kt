package com.philkes.notallyx.data.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.philkes.notallyx.data.dao.BaseNoteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_QUERY_DELAY_MS = 2000L

class SearchResult(
    private val scope: CoroutineScope,
    var baseNoteDao: BaseNoteDao,
    private val transform: (List<BaseNote>) -> List<Item>,
) : LiveData<List<Item>>() {

    private var job: Job? = null
    private var liveData: LiveData<List<BaseNote>>? = null
    private val observer =
        Observer<List<BaseNote>> { list ->
            value = transform(list)
            isLoading.value = false
        }

    val isLoading = MutableLiveData<Boolean>()

    init {
        value = emptyList()
        isLoading.value = false
    }

    fun fetch(keyword: String, folder: Folder, label: String?, debounce: Boolean = true) {
        job?.cancel()
        isLoading.value = true
        if (!debounce) {
            liveData?.removeObserver(observer)
        }
        job =
            scope.launch {
                if (debounce) {
                    delay(SEARCH_QUERY_DELAY_MS)
                    liveData?.removeObserver(observer)
                }
                liveData = baseNoteDao.getBaseNotesByKeyword(keyword, folder, label)
                //                    if (keyword.isNotEmpty())
                // baseNoteDao.getBaseNotesByKeyword(keyword, folder, label)
                //                    else baseNoteDao.getFrom(folder)
                liveData?.observeForever(observer)
            }
    }
}
