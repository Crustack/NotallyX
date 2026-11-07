package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
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

/** Composable replacing fragment_notes.xml layout for LabelsFragment. */
@Composable
internal fun LabelsScreen(
    labels: List<LabelData>,
    modifier: Modifier = Modifier,
    dialogState: LabelDialogState? = null,
    onLabelClick: (LabelData) -> Unit = {},
    onEditClick: (LabelData) -> Unit = {},
    onDeleteClick: (LabelData) -> Unit = {},
    onToggleVisibilityClick: (LabelData) -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onConfirmAddLabel: (String) -> Unit = {},
    onConfirmEditLabel: (oldLabel: String, newLabel: String) -> Unit = { _, _ -> },
    onConfirmDeleteLabel: (String) -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (labels.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.label),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(labels, key = { it.value }) { label ->
                    LabelItemRow(
                        label = label.value,
                        isVisibleInNavigation = label.visibleInNavigation,
                        onClick = { onLabelClick(label) },
                        onEdit = { onEditClick(label) },
                        onDelete = { onDeleteClick(label) },
                        onToggleVisibility = { onToggleVisibilityClick(label) },
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        is LabelDialogState.Add -> {
            TextInputDialog(
                title = stringResource(R.string.add_label),
                initialValue = "",
                onDismissRequest = onDismissDialog,
                onConfirm = { value ->
                    if (value.isNotEmpty()) {
                        onConfirmAddLabel(value)
                    }
                },
            )
        }
        is LabelDialogState.Edit -> {
            TextInputDialog(
                title = stringResource(R.string.edit_label),
                initialValue = state.label,
                onDismissRequest = onDismissDialog,
                onConfirm = { value ->
                    if (value.isNotEmpty()) {
                        onConfirmEditLabel(state.label, value)
                    }
                },
            )
        }
        is LabelDialogState.Delete -> {
            ConfirmDialog(
                title = stringResource(R.string.delete_label),
                message = stringResource(R.string.your_notes_associated),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = onDismissDialog,
                onConfirm = { onConfirmDeleteLabel(state.label) },
            )
        }
        null -> {}
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    NotallyXTheme {
        LabelsScreen(
            labels =
                listOf(
                    LabelData("Personal", true, 0),
                    LabelData("Work", false, 1),
                )
        )
    }
}
