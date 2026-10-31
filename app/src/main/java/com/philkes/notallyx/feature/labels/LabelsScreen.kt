package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.editor.listitem.dragContainer
import com.philkes.notallyx.feature.editor.listitem.draggableModifier
import com.philkes.notallyx.feature.editor.listitem.rememberDragDropState

internal data class LabelsScreenActions(
    val onLabelClick: (LabelData) -> Unit = {},
    val onEditClick: (LabelData) -> Unit = {},
    val onDeleteClick: (LabelData) -> Unit = {},
    val onToggleVisibilityClick: (LabelData) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onConfirmAddLabel: (String) -> Unit = {},
    val onConfirmEditLabel: (oldLabel: String, newLabel: String) -> Unit = { _, _ -> },
    val onConfirmDeleteLabel: (String) -> Unit = {},
    val onLabelsReordered: (List<LabelData>) -> Unit = {},
)

/** MVI Composable for LabelsScreen. */
@Composable
internal fun LabelsScreen(
    state: LabelsUiState,
    onAction: (LabelsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentLabels by remember(state.labels) { mutableStateOf(state.labels) }
    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            lazyListState = listState,
            items = currentLabels,
            onMove = { fromIndex, toIndex, _ ->
                currentLabels =
                    currentLabels.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            },
            onDragEnd = { onAction(LabelsAction.ReorderLabels(currentLabels)) },
        )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.labels.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.label),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().dragContainer(dragDropState),
                state = listState,
            ) {
                itemsIndexed(currentLabels, key = { _, label -> label.value }) { index, label ->
                    LabelItemRow(
                        modifier = draggableModifier(dragDropState, index),
                        label = label.value,
                        isVisibleInNavigation = label.visibleInNavigation,
                        onClick = { onAction(LabelsAction.LabelClicked(label)) },
                        onEdit = { onAction(LabelsAction.RequestEditLabel(label)) },
                        onDelete = { onAction(LabelsAction.RequestDeleteLabel(label)) },
                        onToggleVisibility = { onAction(LabelsAction.ToggleVisibility(label)) },
                    )
                }
            }
        }
    }

    when (val dialog = state.activeDialog) {
        is LabelDialogState.Add -> {
            TextInputDialog(
                title = stringResource(R.string.add_label),
                onDismissRequest = { onAction(LabelsAction.DismissDialog) },
                onConfirm = { value ->
                    if (value.isNotEmpty()) {
                        onAction(LabelsAction.ConfirmAddLabel(value))
                    }
                },
            )
        }

        is LabelDialogState.Edit -> {
            TextInputDialog(
                title = stringResource(R.string.edit_label),
                initialValue = dialog.label,
                onDismissRequest = { onAction(LabelsAction.DismissDialog) },
                onConfirm = { value ->
                    if (value.isNotEmpty()) {
                        onAction(LabelsAction.ConfirmEditLabel(dialog.label, value))
                    }
                },
            )
        }

        is LabelDialogState.Delete -> {
            ConfirmDialog(
                title = stringResource(R.string.delete_label),
                message = stringResource(R.string.your_notes_associated),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = { onAction(LabelsAction.DismissDialog) },
                onConfirm = { onAction(LabelsAction.ConfirmDeleteLabel(dialog.label)) },
            )
        }

        null -> {}
    }
}

/** Backwards-compatible overload for existing preview tests. */
@Composable
internal fun LabelsScreen(
    labels: List<LabelData>,
    modifier: Modifier = Modifier,
    dialogState: LabelDialogState? = null,
    actions: LabelsScreenActions = LabelsScreenActions(),
) {
    LabelsScreen(
        state = LabelsUiState(labels = labels, activeDialog = dialogState),
        onAction = { action ->
            when (action) {
                is LabelsAction.LabelClicked -> actions.onLabelClick(action.label)
                is LabelsAction.RequestEditLabel -> actions.onEditClick(action.label)
                is LabelsAction.RequestDeleteLabel -> actions.onDeleteClick(action.label)
                is LabelsAction.ToggleVisibility -> actions.onToggleVisibilityClick(action.label)
                is LabelsAction.DismissDialog -> actions.onDismissDialog()
                is LabelsAction.ConfirmAddLabel -> actions.onConfirmAddLabel(action.value)
                is LabelsAction.ConfirmEditLabel ->
                    actions.onConfirmEditLabel(action.oldLabel, action.newLabel)
                is LabelsAction.ConfirmDeleteLabel -> actions.onConfirmDeleteLabel(action.label)
                is LabelsAction.ReorderLabels -> actions.onLabelsReordered(action.reorderedLabels)
                is LabelsAction.RequestAddLabel -> actions.onConfirmAddLabel("")
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    NotallyXTheme {
        LabelsScreen(
            state =
                LabelsUiState(
                    labels =
                        listOf(
                            LabelData("Personal", true, 0),
                            LabelData("Work", false, 1),
                        )
                ),
            onAction = {},
        )
    }
}
