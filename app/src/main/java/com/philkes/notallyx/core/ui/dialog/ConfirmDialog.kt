package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun ConfirmDialog(
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    positiveButtonText: String = stringResource(R.string.delete),
    negativeButtonText: String = stringResource(R.string.cancel),
    neutralButtonText: String? = null,
    onNeutralButtonClick: (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = title?.let { { Text(it) } },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                }
            ) {
                Text(positiveButtonText)
            }
        },
        dismissButton = {
            if (neutralButtonText != null && onNeutralButtonClick != null) {
                Row {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onNeutralButtonClick()
                        }
                    ) {
                        Text(neutralButtonText)
                    }
                    if (negativeButtonText.isNotEmpty()) {
                        TextButton(onClick = onDismissRequest) { Text(negativeButtonText) }
                    }
                }
            } else if (negativeButtonText.isNotEmpty()) {
                TextButton(onClick = onDismissRequest) { Text(negativeButtonText) }
            }
        },
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    NotallyXTheme {
        ConfirmDialog(
            title = "Title",
            message = "Are you sure you want to proceed?",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
