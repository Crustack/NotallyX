package com.philkes.notallyx.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences

abstract class BaseViewModel(protected val app: Application) : AndroidViewModel(app) {

    protected var database: NotallyDatabase? = null
    protected val baseNoteDao
        get() = database?.getBaseNoteDao()

    protected val labelDao
        get() = database?.getLabelDao()

    protected val databaseLiveData = NotallyDatabase.getDatabase(app)

    val preferences: NotallyXPreferences
        get() = NotallyXPreferences.getInstance(app)

    protected open fun initDatabase(database: NotallyDatabase?) {
        this.database = database
    }
}
