package com.philkes.notallyx.feature.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun NotesSortDialog(
    currentSort: NotesSort,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirm: (NotesSort) -> Unit,
) {
    var selectedSortBy by remember { mutableStateOf(currentSort.sortedBy) }
    var selectedSortDirection by remember { mutableStateOf(currentSort.sortDirection) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.notes_sorted_by)) },
        text = {
            Column {
                Column(modifier = Modifier.selectableGroup()) {
                    NotesSortBy.entries.forEach { sortBy ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (sortBy == selectedSortBy),
                                        onClick = { selectedSortBy = sortBy },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (sortBy == selectedSortBy),
                                onClick = null,
                            )
                            Text(
                                text = stringResource(sortBy.textResId),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.sort_direction),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    SortDirection.entries.forEach { sortDirection ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(44.dp)
                                    .selectable(
                                        selected = (sortDirection == selectedSortDirection),
                                        onClick = { selectedSortDirection = sortDirection },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (sortDirection == selectedSortDirection),
                                onClick = null,
                            )
                            Text(
                                text = stringResource(sortDirection.textResId),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        NotesSort(sortedBy = selectedSortBy, sortDirection = selectedSortDirection)
                    )
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
private fun NotesSortDialogPreview() {
    NotallyXTheme {
        NotesSortDialog(
            currentSort = NotesSort(),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
