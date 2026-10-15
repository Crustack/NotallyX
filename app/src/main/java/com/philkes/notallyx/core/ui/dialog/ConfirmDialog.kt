package com.philkes.notallyx.core.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.philkes.notallyx.R

@Composable
internal fun ConfirmDialog(
    title: String? = null,
    message: String,
    positiveButtonText: String = stringResource(R.string.delete),
    negativeButtonText: String = stringResource(R.string.cancel),
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title?.let { { Text(it) } },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                },
                modifier = Modifier.testTag("button1"),
            ) {
                Text(positiveButtonText)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(negativeButtonText) } },
    )
}
