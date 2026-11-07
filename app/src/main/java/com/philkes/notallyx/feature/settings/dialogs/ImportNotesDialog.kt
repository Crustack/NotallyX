package com.philkes.notallyx.feature.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun ImportNotesDialog(
    title: String,
    modifier: Modifier = Modifier,
    initialText: String = "",
    hint: String? = null,
    message: String? = null,
    initialCheckDuplicates: Boolean = true,
    onDismissRequest: () -> Unit,
    onConfirm: (text: String, checkDuplicates: Boolean) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var checkDuplicates by remember { mutableStateOf(initialCheckDuplicates) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (!message.isNullOrEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = hint?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checkDuplicates,
                        onCheckedChange = { checkDuplicates = it },
                    )
                    Text(
                        text = stringResource(R.string.check_duplicates),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text.trim(), checkDuplicates)
                    onDismissRequest()
                },
                enabled = true,
            ) {
                Text(stringResource(R.string.import_backup))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun ImportNotesDialogPreview() {
    NotallyXTheme {
        ImportNotesDialog(
            title = "Import Backup Password",
            message = "Enter the password used to encrypt the backup file",
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
