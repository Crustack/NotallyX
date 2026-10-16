package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun ListDialog(
    title: String,
    items: List<String>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: ((index: Int) -> Unit)? = null,
    positiveButtonText: String? = stringResource(android.R.string.ok),
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items, key = { index, item -> "${index}_$item" }) { index, item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(enabled = onItemClick != null) {
                                    onDismissRequest()
                                    onItemClick?.invoke(index)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (positiveButtonText != null) {
                TextButton(onClick = onDismissRequest) { Text(positiveButtonText) }
            }
        },
    )
}

@Preview
@Composable
private fun ListDialogPreview() {
    NotallyXTheme {
        ListDialog(
            title = "Cannot load file",
            items = listOf("file1.txt: File corrupt", "file2.txt: Permission denied"),
            onDismissRequest = {},
        )
    }
}
