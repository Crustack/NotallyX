package com.philkes.notallyx.core.common

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.preference.NotallyXPreferences
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(
    val preferences: NotallyXPreferences,
    protected val databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    protected val pathProvider: PathProvider = DesktopPathProvider(),
) : ViewModel() {

    protected lateinit var app: Application
    protected val hasApp: Boolean
        get() = ::app.isInitialized

    constructor(
        app: Application,
        preferences: NotallyXPreferences,
    ) : this(
        preferences = preferences,
        databaseStateFlow = NotallyDatabase.getDatabase(app),
        pathProvider = AndroidPathProvider(app),
    ) {
        this.app = app
    }

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
