package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.presentation.switchMapNullSafe
import com.philkes.notallyx.presentation.viewmodel.executeAsyncWithCallback
import com.philkes.notallyx.presentation.viewmodel.preference.BasePreference
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LabelsViewModel(app: Application) : AndroidViewModel(app) {

    private var database: NotallyDatabase? = null
    private lateinit var labelDao: LabelDao
    private lateinit var commonDao: CommonDao

    val preferences = NotallyXPreferences.getInstance(app)

    val labels: LiveData<List<Label>> =
        NotallyDatabase.getDatabase(app).switchMapNullSafe { database ->
            database!!.getLabelDao().getAll()
        }

    init {
        NotallyDatabase.getDatabase(app).observeForever(::init)
    }

    private fun init(database: NotallyDatabase?) {
        if (database == null) return
        this.database = database
        labelDao = database.getLabelDao()
        commonDao = database.getCommonDao()
    }

    fun <T> savePreference(preference: BasePreference<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) { preference.save(value) }
    }

    fun insertLabel(label: String, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback(
            { labelDao.insert(Label(label, (labelDao.getMaxOrder() ?: -1) + 1)) },
            onComplete,
        )

    fun updateLabels(labels: List<Label>) {
        viewModelScope.launch(Dispatchers.IO) { labelDao.update(labels) }
    }

    fun deleteLabel(value: String) {
        viewModelScope.launch(Dispatchers.IO) { commonDao.deleteLabel(value) }
        val labelsHiddenPreference = preferences.labelsHidden
        val labelsHidden = labelsHiddenPreference.value.toMutableSet()
        if (labelsHidden.contains(value)) {
            labelsHidden.remove(value)
            savePreference(labelsHiddenPreference, labelsHidden)
        }
        if (preferences.startView.value == value) {
            savePreference(preferences.startView, START_VIEW_DEFAULT)
        }
    }

    fun updateLabel(oldValue: String, newValue: String, onComplete: (success: Boolean) -> Unit) {
        executeAsyncWithCallback({ commonDao.updateLabel(oldValue, newValue) }, onComplete)
        val labelsHiddenPreference = preferences.labelsHidden
        val labelsHidden = labelsHiddenPreference.value.toMutableSet()
        if (labelsHidden.contains(oldValue)) {
            labelsHidden.remove(oldValue)
            labelsHidden.add(newValue)
            savePreference(labelsHiddenPreference, labelsHidden)
        }
    }
}
