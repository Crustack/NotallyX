package com.philkes.notallyx.presentation.viewmodel.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.ConverterErrorReporter
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel(app: Application, val preferences: NotallyXPreferences) :
    AndroidViewModel(app) {

    private var database: NotallyDatabase? = null
    private lateinit var baseNoteDao: BaseNoteDao
    private lateinit var labelDao: LabelDao

    val progress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val labels: StateFlow<List<Label>> =
        NotallyDatabase.getDatabase(app)
            .flatMapLatest { database -> database?.getLabelDao()?.getAll() ?: flowOf(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch { NotallyDatabase.getDatabase(app).collect { init(it) } }
    }

    fun startObserving() {
        viewModelScope.launch { NotallyDatabase.getDatabase(getApplication()).collect { init(it) } }
    }

    private fun init(database: NotallyDatabase?) {
        if (database == null) return
        this.database = database
        baseNoteDao = database.getBaseNoteDao()
        labelDao = database.getLabelDao()
    }

    fun cleanupDatabase(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            ConverterErrorReporter.enabled.set(false)
            try {
                val allNotes = baseNoteDao.getAll()
                baseNoteDao.updateAll(allNotes)
                withContext(Dispatchers.Main) { onComplete() }
            } finally {
                ConverterErrorReporter.enabled.set(true)
            }
        }
    }
}
