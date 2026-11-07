package com.philkes.notallyx.feature.main.routes.labels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.labels.LabelData
import com.philkes.notallyx.feature.labels.LabelsScreen
import com.philkes.notallyx.feature.labels.LabelsScreenActions
import com.philkes.notallyx.feature.labels.LabelsViewModel
import kotlinx.coroutines.flow.combine
import org.koin.androidx.compose.koinViewModel

@Composable
fun LabelsRoute(
    onNavigateToLabel: (String) -> Unit,
    onShowToast: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelsViewModel: LabelsViewModel = koinViewModel(),
) {
    val labelsFlow =
        remember(labelsViewModel) {
            combine(
                labelsViewModel.labels,
                labelsViewModel.preferences.labelsHidden.flow,
            ) { labels, hiddenLabels ->
                labels.map { label ->
                    LabelData(
                        label.value,
                        !hiddenLabels.contains(label.value),
                        label.order,
                    )
                }
            }
        }
    val labelsState = labelsFlow.collectAsStateWithLifecycle(emptyList())
    val dialogState by labelsViewModel.dialogState.collectAsStateWithLifecycle()

    LabelsScreen(
        labels = labelsState.value,
        dialogState = dialogState,
        actions =
            LabelsScreenActions(
                onLabelClick = { labelData -> onNavigateToLabel(labelData.value) },
                onEditClick = { labelData -> labelsViewModel.showEditDialog(labelData.value) },
                onDeleteClick = { labelData -> labelsViewModel.showDeleteDialog(labelData.value) },
                onToggleVisibilityClick = { labelData ->
                    val hiddenLabels = labelsViewModel.preferences.labelsHidden.value.toMutableSet()
                    if (labelData.visibleInNavigation) {
                        hiddenLabels.add(labelData.value)
                    } else {
                        hiddenLabels.remove(labelData.value)
                    }
                    labelsViewModel.savePreference(
                        labelsViewModel.preferences.labelsHidden,
                        hiddenLabels,
                    )
                },
                onDismissDialog = { labelsViewModel.dismissDialog() },
                onConfirmAddLabel = { value ->
                    labelsViewModel.insertLabel(value) { success ->
                        if (success) {
                            labelsViewModel.dismissDialog()
                        } else {
                            onShowToast(R.string.label_exists)
                        }
                    }
                },
                onConfirmEditLabel = { oldLabel, newLabel ->
                    labelsViewModel.updateLabel(oldLabel, newLabel) { success ->
                        if (success) {
                            labelsViewModel.dismissDialog()
                        } else {
                            onShowToast(R.string.label_exists)
                        }
                    }
                },
                onConfirmDeleteLabel = { label ->
                    labelsViewModel.deleteLabel(label)
                    labelsViewModel.dismissDialog()
                },
            ),
        modifier = modifier,
    )
}
