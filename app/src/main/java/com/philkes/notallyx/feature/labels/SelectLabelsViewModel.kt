package com.philkes.notallyx.feature.labels

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.preference.NotallyXPreferences
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

class SelectLabelsViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : BaseViewModel(app, preferences) {

    private val _selectedLabels = MutableStateFlow<Set<String>>(emptySet())
    private val _activeDialog = MutableStateFlow<LabelDialogState?>(null)
    private var isInitialized = false

    private val _sideEffects = Channel<SelectLabelsSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SelectLabelsSideEffect> = _sideEffects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val labelsFlow: Flow<List<Label>> =
        databaseStateFlow
            .flatMapLatest { db -> db?.getLabelDao()?.getAll() ?: flowOf(emptyList()) }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<SelectLabelsUiState> =
        combine(
                labelsFlow,
                _selectedLabels,
                _activeDialog,
            ) { labels, selected, dialog ->
                SelectLabelsUiState(
                    labels = labels,
                    selectedLabels = selected,
                    activeDialog = dialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SelectLabelsUiState(),
            )

    fun onAction(action: SelectLabelsAction) {
        when (action) {
            is SelectLabelsAction.Init -> {
                if (!isInitialized) {
                    _selectedLabels.value = action.initialLabels.toSet()
                    isInitialized = true
                }
            }

            is SelectLabelsAction.ToggleLabel -> {
                _selectedLabels.update { current ->
                    if (action.isChecked) {
                        current + action.label
                    } else {
                        current - action.label
                    }
                }
            }

            is SelectLabelsAction.RequestAddLabel -> {
                _activeDialog.update { LabelDialogState.Add }
            }

            is SelectLabelsAction.DismissDialog -> {
                _activeDialog.update { null }
            }

            is SelectLabelsAction.ConfirmAddLabel -> {
                insertLabel(action.label)
            }

            is SelectLabelsAction.BackClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        SelectLabelsSideEffect.FinishWithResult(_selectedLabels.value.toList())
                    )
                }
            }
        }
    }

    private fun insertLabel(labelValue: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = labelDao ?: return@launch
            try {
                if (dao.exists(labelValue)) {
                    _sideEffects.send(SelectLabelsSideEffect.ShowToast(R.string.label_exists))
                    return@launch
                }
                val maxOrder = dao.getMaxOrder() ?: -1
                dao.insert(Label(labelValue, maxOrder + 1))
                _activeDialog.update { null }
                _selectedLabels.update { it + labelValue }
            } catch (_: SQLiteConstraintException) {
                _sideEffects.send(SelectLabelsSideEffect.ShowToast(R.string.label_exists))
            }
        }
    }
}
