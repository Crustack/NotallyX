package com.philkes.notallyx.core.common.error

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

data class ErrorScreenUiState(
    val exceptionTitle: String = "",
    val stackTrace: String = "",
    val crashLogsLinkText: String? = null,
    val showExportBackupDialog: Boolean = false,
    val showReimportDialog: Boolean = false,
    val importResultDialogMessage: String? = null,
)

/** Composable replacing activity_error.xml layout. */
@Composable
internal fun ErrorScreen(
    uiState: ErrorScreenUiState,
    modifier: Modifier = Modifier,
    onDismissExportBackupDialog: () -> Unit = {},
    onConfirmExportBackupDialog: () -> Unit = {},
    onDismissReimportDialog: () -> Unit = {},
    onConfirmReimportDialog: () -> Unit = {},
    onDismissImportResultDialog: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onReportBugClick: () -> Unit = {},
    onExportBackupClick: () -> Unit = {},
    onViewLogsClick: () -> Unit = {},
    onCopyStackTraceClick: () -> Unit = {},
    onCrashLogsLinkClick: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.crash_message),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (uiState.exceptionTitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.exceptionTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            StackTraceCard(
                stackTrace = uiState.stackTrace,
                onCopyStackTraceClick = onCopyStackTraceClick,
            )
            if (!uiState.crashLogsLinkText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCrashLogsLinkClick) {
                    Text(
                        text = uiState.crashLogsLinkText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            ErrorActionsSection(
                onRestartClick = onRestartClick,
                onReportBugClick = onReportBugClick,
                onExportBackupClick = onExportBackupClick,
                onViewLogsClick = onViewLogsClick,
            )
        }

        ErrorScreenDialogs(
            uiState = uiState,
            onDismissExportBackupDialog = onDismissExportBackupDialog,
            onConfirmExportBackupDialog = onConfirmExportBackupDialog,
            onDismissReimportDialog = onDismissReimportDialog,
            onConfirmReimportDialog = onConfirmReimportDialog,
            onDismissImportResultDialog = onDismissImportResultDialog,
        )
    }
}

@Composable
private fun StackTraceCard(
    stackTrace: String,
    onCopyStackTraceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(end = 48.dp)
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stackTrace,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
            IconButton(
                onClick = onCopyStackTraceClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.content_copy),
                    contentDescription = stringResource(R.string.copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorActionsSection(
    onRestartClick: () -> Unit,
    onReportBugClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onRestartClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.redo),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.restart_app))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onReportBugClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.link),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.report_bug))
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onExportBackupClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.export_backup),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        TextButton(
            onClick = onViewLogsClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.view_logs),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ErrorScreenDialogs(
    uiState: ErrorScreenUiState,
    onDismissExportBackupDialog: () -> Unit,
    onConfirmExportBackupDialog: () -> Unit,
    onDismissReimportDialog: () -> Unit,
    onConfirmReimportDialog: () -> Unit,
    onDismissImportResultDialog: () -> Unit,
) {
    if (uiState.showExportBackupDialog) {
        ConfirmDialog(
            message =
                stringResource(
                    R.string.crash_export_backup_message,
                    stringResource(R.string.continue_),
                    stringResource(R.string.report_bug),
                ),
            positiveButtonText = stringResource(R.string.continue_),
            onDismissRequest = onDismissExportBackupDialog,
            onConfirm = onConfirmExportBackupDialog,
        )
    }

    if (uiState.showReimportDialog) {
        ConfirmDialog(
            title = stringResource(R.string.database),
            message = stringResource(R.string.reimport_database_message),
            positiveButtonText = stringResource(R.string.reimport_database),
            onDismissRequest = onDismissReimportDialog,
            onConfirm = onConfirmReimportDialog,
        )
    }

    if (uiState.importResultDialogMessage != null) {
        ErrorDialog(
            title = stringResource(R.string.database),
            message = uiState.importResultDialogMessage,
            onDismissRequest = onDismissImportResultDialog,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    NotallyXTheme {
        ErrorScreen(
            uiState =
                ErrorScreenUiState(
                    exceptionTitle =
                        "java.lang.NullPointerException: Attempt to invoke virtual method",
                    stackTrace =
                        "at com.philkes.notallyx.MainActivity.onCreate(MainActivity.kt:90)",
                    crashLogsLinkText = "View Crash Logs",
                )
        )
    }
}
