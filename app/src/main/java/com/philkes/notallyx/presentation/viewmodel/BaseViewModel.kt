package com.philkes.notallyx.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    protected val app: Application,
    val preferences: NotallyXPreferences,
) : AndroidViewModel(app) {

    protected val databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.getDatabase(app)

    protected val database: NotallyDatabase?
        get() = databaseStateFlow.value

    protected val baseNoteDao
        get() = database?.getBaseNoteDao()

    protected val labelDao
        get() = database?.getLabelDao()

    init {
        viewModelScope.launch { databaseStateFlow.collect { db -> initDatabase(db) } }
    }

    protected open fun initDatabase(database: NotallyDatabase?) {}
}
