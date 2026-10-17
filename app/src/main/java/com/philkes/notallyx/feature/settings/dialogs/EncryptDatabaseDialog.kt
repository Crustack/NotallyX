package com.philkes.notallyx.feature.settings.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
internal fun EncryptDatabaseDialog(
    onDismissRequest: () -> Unit,
    onContinue: (encryptDatabase: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var checkEncrypt by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.biometric_lock)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.encrypt_database_advice),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.clickable { checkEncrypt = !checkEncrypt }
                            .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = checkEncrypt,
                        onCheckedChange = { checkEncrypt = it },
                    )
                    Text(
                        text = stringResource(R.string.encrypt_database_via_biometric),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onContinue(checkEncrypt)
                }
            ) {
                Text(stringResource(R.string.continue_))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EncryptDatabaseDialogPreview() {
    NotallyXTheme {
        EncryptDatabaseDialog(
            onDismissRequest = {},
            onContinue = {},
        )
    }
}
