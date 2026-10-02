package com.philkes.notallyx.presentation.viewmodel.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.ConverterErrorReporter
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel(app: Application) : AndroidViewModel(app) {

    private var database: NotallyDatabase? = null
    private lateinit var baseNoteDao: BaseNoteDao
    private lateinit var labelDao: LabelDao

    val preferences = NotallyXPreferences.getInstance(app)
    val progress: LiveData<Progress>
        field = MutableLiveData<Progress>()

    val labels: LiveData<List<Label>> =
        NotallyDatabase.getDatabase(app).switchMapNullSafe { database ->
            database!!.getLabelDao().getAll()
        }

    init {
        NotallyDatabase.getDatabase(app).observeForever(::init)
    }

    fun startObserving() {
        NotallyDatabase.getDatabase(getApplication()).observeForever(::init)
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
            val allNotes = baseNoteDao.getAll()
            baseNoteDao.updateAll(allNotes)
            withContext(Dispatchers.Main) {
                onComplete()
                ConverterErrorReporter.enabled.set(true)
            }
        }
    }
}
