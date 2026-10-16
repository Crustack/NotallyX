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

/** Composable replacing activity_label.xml layout. */
@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.select_labels)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddLabelClick) {
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
            if (labels.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(labels, key = { it.value }) { label ->
                        val isChecked = selectedLabels.contains(label.value)
                        SelectableLabelRow(
                            label = label.value,
                            isChecked = isChecked,
                            onCheckedChange = { checked -> onLabelToggle(label.value, checked) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (dialogState is LabelDialogState.Add) {
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
}

@Preview(showBackground = true)
@Composable
private fun SelectLabelsScreenPreview() {
    NotallyXTheme {
        SelectLabelsScreen(
            labels = listOf(Label("Personal", 0), Label("Work", 1)),
            selectedLabels = listOf("Work"),
        )
    }
}
