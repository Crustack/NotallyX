package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Label

/** MVI Composable replacing activity_label.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectLabelsScreen(
    state: SelectLabelsUiState,
    onAction: (SelectLabelsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.select_labels)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SelectLabelsAction.BackClicked) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(SelectLabelsAction.RequestAddLabel) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = stringResource(R.string.add_label),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (state.labels.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.labels, key = { it.value }) { label ->
                        val isChecked = state.selectedLabels.contains(label.value)
                        SelectableLabelRow(
                            label = label.value,
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                onAction(SelectLabelsAction.ToggleLabel(label.value, checked))
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (state.activeDialog is LabelDialogState.Add) {
        TextInputDialog(
            title = stringResource(R.string.add_label),
            onDismissRequest = { onAction(SelectLabelsAction.DismissDialog) },
            onConfirm = { value ->
                if (value.isNotEmpty()) {
                    onAction(SelectLabelsAction.ConfirmAddLabel(value))
                }
            },
        )
    }
}

/** Backwards-compatible overload for existing preview tests. */
@Composable
internal fun SelectLabelsScreen(
    labels: List<Label>,
    selectedLabels: List<String>,
    modifier: Modifier = Modifier,
    dialogState: LabelDialogState? = null,
    onLabelToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAddLabelClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onConfirmAddLabel: (String) -> Unit = {},
) {
    SelectLabelsScreen(
        state =
            SelectLabelsUiState(
                labels = labels,
                selectedLabels = selectedLabels.toSet(),
                activeDialog = dialogState,
            ),
        onAction = { action ->
            when (action) {
                is SelectLabelsAction.ToggleLabel -> onLabelToggle(action.label, action.isChecked)
                is SelectLabelsAction.RequestAddLabel -> onAddLabelClick()
                is SelectLabelsAction.DismissDialog -> onDismissDialog()
                is SelectLabelsAction.ConfirmAddLabel -> onConfirmAddLabel(action.label)
                is SelectLabelsAction.BackClicked -> onBackClick()
                is SelectLabelsAction.Init -> Unit
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SelectLabelsScreenPreview() {
    NotallyXTheme {
        SelectLabelsScreen(
            state =
                SelectLabelsUiState(
                    labels = listOf(Label("Personal", 0), Label("Work", 1)),
                    selectedLabels = setOf("Work"),
                ),
            onAction = {},
        )
    }
}
