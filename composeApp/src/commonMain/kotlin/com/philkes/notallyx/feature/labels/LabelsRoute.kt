package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsRoute(
    labelDao: LabelDao,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLabelClick: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val labels by labelDao.getAll().collectAsState(initial = emptyList())
    var newLabelText by remember { mutableStateOf("") }
    var activeDialog by remember { mutableStateOf<LabelDialogState?>(null) }

    fun addLabel() {
        val trimmed = newLabelText.trim()
        if (trimmed.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                labelDao.insert(Label(value = trimmed, order = labels.size))
            }
            newLabelText = ""
            focusManager.clearFocus()
        }
    }

    val labelDataList =
        remember(labels) {
            labels.map { LabelData(value = it.value, visibleInNavigation = true, order = it.order) }
        }

    val state =
        remember(labelDataList, activeDialog) {
            LabelsUiState(
                labels = labelDataList,
                activeDialog = activeDialog,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_LABELS),
        topBar = {
            TopAppBar(
                title = { Text("Labels", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = { newLabelText = it },
                    placeholder = { Text("Create new label…") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.label),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.weight(1f).testTag(TestTags.ADD_LABEL_INPUT),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { addLabel() },
                    modifier = Modifier.testTag(TestTags.ADD_LABEL_BUTTON),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add label")
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                LabelsScreen(
                    state = state,
                    onAction = { action ->
                        when (action) {
                            is LabelsAction.LabelClicked -> onLabelClick(action.label.value)
                            is LabelsAction.RequestAddLabel -> activeDialog = LabelDialogState.Add
                            is LabelsAction.RequestEditLabel ->
                                activeDialog = LabelDialogState.Edit(action.label.value)
                            is LabelsAction.RequestDeleteLabel ->
                                activeDialog = LabelDialogState.Delete(action.label.value)
                            is LabelsAction.DismissDialog -> activeDialog = null
                            is LabelsAction.ConfirmAddLabel -> {
                                scope.launch(Dispatchers.IO) {
                                    labelDao.insert(
                                        Label(value = action.value, order = labels.size)
                                    )
                                }
                                activeDialog = null
                            }
                            is LabelsAction.ConfirmEditLabel -> {
                                scope.launch(Dispatchers.IO) {
                                    labelDao.update(
                                        oldValue = action.oldLabel,
                                        newValue = action.newLabel,
                                    )
                                }
                                activeDialog = null
                            }
                            is LabelsAction.ConfirmDeleteLabel -> {
                                scope.launch(Dispatchers.IO) { labelDao.delete(action.label) }
                                activeDialog = null
                            }
                            is LabelsAction.ReorderLabels -> {
                                scope.launch(Dispatchers.IO) {
                                    val updated =
                                        action.reorderedLabels.mapIndexed { index, labelData ->
                                            Label(value = labelData.value, order = index)
                                        }
                                    labelDao.update(updated)
                                }
                            }
                            is LabelsAction.ToggleVisibility -> {}
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
