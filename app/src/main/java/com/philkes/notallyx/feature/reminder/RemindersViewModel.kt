package com.philkes.notallyx.feature.reminder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.BaseNote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class RemindersViewModel(app: Application, preferences: NotallyXPreferences) :
    BaseViewModel(app, preferences) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminderNotes: StateFlow<List<BaseNote>> =
        databaseStateFlow
            .flatMapLatest { database ->
                database?.getBaseNoteDao()?.getAllWithReminders() ?: flowOf(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
}
