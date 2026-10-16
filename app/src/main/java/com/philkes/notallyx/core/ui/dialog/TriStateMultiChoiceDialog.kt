package com.philkes.notallyx.core.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TriStateMultiChoiceContent

@Composable
internal fun TriStateMultiChoiceDialog(
    title: String,
    items: Array<String>,
    initialStates: Array<ToggleableState>,
    onDismissRequest: () -> Unit,
    onConfirm: (states: Array<ToggleableState>) -> Unit,
    modifier: Modifier = Modifier,
    positiveButtonText: String = stringResource(R.string.save),
    negativeButtonText: String = stringResource(R.string.cancel),
) {
    val states = remember { initialStates.clone() }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            TriStateMultiChoiceContent(
                items = items,
                initialStates = initialStates,
                onItemClick = { index, state -> states[index] = state },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(states)
                    onDismissRequest()
                }
            ) {
                Text(positiveButtonText)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(negativeButtonText) } },
    )
}

@Preview
@Composable
private fun TriStateMultiChoiceDialogPreview() {
    NotallyXTheme {
        TriStateMultiChoiceDialog(
            title = "Select Labels",
            items = arrayOf("Label 1", "Label 2", "Label 3"),
            initialStates =
                arrayOf(ToggleableState.On, ToggleableState.Off, ToggleableState.Indeterminate),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
