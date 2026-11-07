package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun ProgressDialog(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    progress: Float? = null,
    onDismissRequest: (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismissRequest?.invoke() },
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().testTag("ProgressBar"),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().testTag("ProgressBar")
                    )
                }

                if (!message.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("Count"),
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Preview
@Composable
private fun ProgressDialogPreview() {
    NotallyXTheme {
        ProgressDialog(
            title = "Exporting Database",
            message = "Please wait...",
            progress = 0.5f,
        )
    }
}
