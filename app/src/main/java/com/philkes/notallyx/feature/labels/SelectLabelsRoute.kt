package com.philkes.notallyx.feature.labels

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun SelectLabelsRoute(
    initialLabels: List<String>,
    onLabelsSelected: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelsViewModel: LabelsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val labelsState by labelsViewModel.labels.collectAsStateWithLifecycle(emptyList())
    val selectedLabelsState =
        remember(initialLabels) { mutableStateListOf(*initialLabels.toTypedArray()) }
    val dialogState by labelsViewModel.dialogState.collectAsStateWithLifecycle()

    val handleBack: () -> Unit = {
        onLabelsSelected(selectedLabelsState.toList())
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    SelectLabelsScreen(
        labels = labelsState,
        selectedLabels = selectedLabelsState,
        dialogState = dialogState,
        onLabelToggle = { value, checked ->
            if (checked) {
                if (!selectedLabelsState.contains(value)) {
                    selectedLabelsState.add(value)
                }
            } else {
                selectedLabelsState.remove(value)
            }
        },
        onAddLabelClick = { labelsViewModel.showAddDialog() },
        onBackClick = handleBack,
        onDismissDialog = { labelsViewModel.dismissDialog() },
        onConfirmAddLabel = { value ->
            labelsViewModel.insertLabel(value) { success ->
                if (success) {
                    labelsViewModel.dismissDialog()
                } else {
                    Toast.makeText(context, R.string.label_exists, Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = modifier,
    )
}

const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.EXTRA_SELECTED_LABELS"
