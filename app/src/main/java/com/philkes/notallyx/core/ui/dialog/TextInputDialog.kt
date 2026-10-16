package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun TextInputDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    hint: String? = null,
    message: String? = null,
    isPassword: Boolean = false,
    positiveButtonText: String = stringResource(R.string.save),
    negativeButtonText: String = stringResource(R.string.cancel),
) {
    var text by remember { mutableStateOf(initialValue) }

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
                    visualTransformation =
                        if (isPassword) PasswordVisualTransformation()
                        else VisualTransformation.None,
                    keyboardOptions =
                        if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password)
                        else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty(),
            ) {
                Text(positiveButtonText)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(negativeButtonText) } },
    )
}

@Composable
fun DualTextInputDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    initialValue1: String = "",
    initialValue2: String = "",
    hint1: String? = null,
    hint2: String? = null,
    message: String? = null,
    positiveButtonText: String = stringResource(R.string.save),
    negativeButtonText: String = stringResource(R.string.cancel),
) {
    var text1 by remember { mutableStateOf(initialValue1) }
    var text2 by remember { mutableStateOf(initialValue2) }

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
                    value = text1,
                    onValueChange = { text1 = it },
                    label = hint1?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text2,
                    onValueChange = { text2 = it },
                    label = hint2?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text1.trim(), text2.trim()) },
                enabled = text1.trim().isNotEmpty(),
            ) {
                Text(positiveButtonText)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(negativeButtonText) } },
    )
}

@Preview
@Composable
private fun TextInputDialogPreview() {
    NotallyXTheme {
        TextInputDialog(
            title = "Enter Label Name",
            hint = "Label name",
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun DualTextInputDialogPreview() {
    NotallyXTheme {
        DualTextInputDialog(
            title = "Enter Credentials",
            hint1 = "Username",
            hint2 = "Password",
            onDismissRequest = {},
            onConfirm = { _, _ -> },
        )
    }
}
