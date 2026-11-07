package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

data class SelectionItem<T>(
    val value: T,
    val label: String,
)

@Composable
internal fun <T> SingleSelectionDialog(
    title: String,
    items: List<SelectionItem<T>>,
    selectedItem: T,
    onDismissRequest: () -> Unit,
    onConfirm: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(selectedItem) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                items.forEach { item ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(44.dp)
                                .selectable(
                                    selected = (item.value == selected),
                                    onClick = { selected = item.value },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (item.value == selected),
                            onClick = null,
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm(selected)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun SingleSelectionDialogPreview() {
    NotallyXTheme {
        SingleSelectionDialog(
            title = "Select Sort Order",
            items =
                listOf(
                    SelectionItem("title", "Title"),
                    SelectionItem("date", "Date Modified"),
                ),
            selectedItem = "date",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
