package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.createItemsFromNotes
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.BaseViewModel

class RemindersViewModel(app: Application) : BaseViewModel(app) {

    val reminderNotes: LiveData<List<Item>> = databaseLiveData.switchMapNullSafe { database ->
        database!!.getBaseNoteDao().getAllBaseNotesWithReminders().map { list ->
            app.createItemsFromNotes(list)
        }
    }
}
