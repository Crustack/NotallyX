package com.philkes.notallyx.feature.labels

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.model.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LabelsViewModel(app: Application, preferences: NotallyXPreferences) :
    BaseViewModel(app, preferences) {

    private val commonDao: CommonDao?
        get() = database?.getCommonDao()

    private val _activeDialog = MutableStateFlow<LabelDialogState?>(null)
    val dialogState: StateFlow<LabelDialogState?> = _activeDialog

    private val _sideEffects = Channel<LabelsSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<LabelsSideEffect> = _sideEffects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val labels: StateFlow<List<Label>> =
        databaseStateFlow
            .flatMapLatest { database -> database?.getLabelDao()?.getAll() ?: flowOf(emptyList()) }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    private val labelsDataFlow: Flow<List<LabelData>> =
        combine(
                labels,
                preferences.labelsHidden.flow,
            ) { labelList, hiddenLabels ->
                labelList.map { label ->
                    LabelData(
                        label.value,
                        !hiddenLabels.contains(label.value),
                        label.order,
                    )
                }
            }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<LabelsUiState> =
        combine(
                labelsDataFlow,
                _activeDialog,
            ) { labelsData, activeDialog ->
                LabelsUiState(
                    labels = labelsData,
                    activeDialog = activeDialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = LabelsUiState(),
            )

    fun onAction(action: LabelsAction) {
        when (action) {
            is LabelsAction.LabelClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(LabelsSideEffect.NavigateToLabel(action.label.value))
                }
            }

            is LabelsAction.RequestAddLabel -> {
                _activeDialog.update { LabelDialogState.Add }
            }

            is LabelsAction.RequestEditLabel -> {
                _activeDialog.update { LabelDialogState.Edit(action.label.value) }
            }

            is LabelsAction.RequestDeleteLabel -> {
                _activeDialog.update { LabelDialogState.Delete(action.label.value) }
            }

            is LabelsAction.DismissDialog -> {
                _activeDialog.update { null }
            }

            is LabelsAction.ToggleVisibility -> {
                toggleLabelVisibility(action.label)
            }

            is LabelsAction.ConfirmAddLabel -> {
                insertLabel(action.value)
            }

            is LabelsAction.ConfirmEditLabel -> {
                updateLabel(action.oldLabel, action.newLabel)
            }

            is LabelsAction.ConfirmDeleteLabel -> {
                deleteLabel(action.label)
            }

            is LabelsAction.ReorderLabels -> {
                reorderLabels(action.reorderedLabels)
            }
        }
    }

    // Backwards-compatible convenience methods
    fun showAddDialog() = onAction(LabelsAction.RequestAddLabel)

    fun showEditDialog(label: String) =
        onAction(LabelsAction.RequestEditLabel(LabelData(label, true, 0)))

    fun showDeleteDialog(label: String) =
        onAction(LabelsAction.RequestDeleteLabel(LabelData(label, true, 0)))

    fun dismissDialog() = onAction(LabelsAction.DismissDialog)

    private fun toggleLabelVisibility(labelData: LabelData) {
        viewModelScope.launch(Dispatchers.IO) {
            val hiddenLabels = preferences.labelsHidden.value.toMutableSet()
            if (labelData.visibleInNavigation) {
                hiddenLabels.add(labelData.value)
            } else {
                hiddenLabels.remove(labelData.value)
            }
            preferences.labelsHidden.save(hiddenLabels)
        }
    }

    private fun insertLabel(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = labelDao ?: return@launch
            try {
                if (dao.exists(value)) {
                    _sideEffects.send(LabelsSideEffect.ShowToast(R.string.label_exists))
                    return@launch
                }
                val maxOrder = dao.getMaxOrder() ?: -1
                dao.insert(Label(value, maxOrder + 1))
                _activeDialog.update { null }
            } catch (_: SQLiteConstraintException) {
                _sideEffects.send(LabelsSideEffect.ShowToast(R.string.label_exists))
            }
        }
    }

    private fun updateLabel(oldValue: String, newValue: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lDao = labelDao ?: return@launch
            val cDao = commonDao ?: return@launch
            try {
                if (oldValue != newValue && lDao.exists(newValue)) {
                    _sideEffects.send(LabelsSideEffect.ShowToast(R.string.label_exists))
                    return@launch
                }
                cDao.updateLabel(oldValue, newValue)
                val labelsHiddenPreference = preferences.labelsHidden
                val labelsHidden = labelsHiddenPreference.value.toMutableSet()
                if (labelsHidden.contains(oldValue)) {
                    labelsHidden.remove(oldValue)
                    labelsHidden.add(newValue)
                    labelsHiddenPreference.save(labelsHidden)
                }
                _activeDialog.update { null }
            } catch (_: SQLiteConstraintException) {
                _sideEffects.send(LabelsSideEffect.ShowToast(R.string.label_exists))
            }
        }
    }

    private fun deleteLabel(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            commonDao?.deleteLabel(value)
            val labelsHiddenPreference = preferences.labelsHidden
            val labelsHidden = labelsHiddenPreference.value.toMutableSet()
            if (labelsHidden.contains(value)) {
                labelsHidden.remove(value)
                labelsHiddenPreference.save(labelsHidden)
            }
            if (preferences.startView.value == value) {
                preferences.startView.save(START_VIEW_DEFAULT)
            }
            _activeDialog.update { null }
        }
    }

    private fun reorderLabels(reorderedLabels: List<LabelData>) {
        viewModelScope.launch(Dispatchers.IO) {
            val size = reorderedLabels.size
            val updatedLabels = reorderedLabels.mapIndexed { index, label ->
                Label(label.value, size - index)
            }
            labelDao?.update(updatedLabels)
        }
    }
}
