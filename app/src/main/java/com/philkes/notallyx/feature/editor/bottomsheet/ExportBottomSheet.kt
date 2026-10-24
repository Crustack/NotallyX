package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.migration.ExportMimeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onExport: (ExportMimeType) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = stringResource(R.string.export),
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    ActionModalBottomSheet(
        actions = createExportActions(onExport),
        modifier = modifier,
        title = title,
        backgroundColor = backgroundColor,
        onDismissRequest = onDismissRequest,
    )
}

fun createExportActions(onExport: (ExportMimeType) -> Unit): List<ActionItem> =
    ExportMimeType.entries.map { mimeType ->
        ActionItem(
            labelResId = EditAction.EXPORT.textResId,
            label = mimeType.name,
        ) {
            onExport(mimeType)
        }
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExportBottomSheetPreview() {
    NotallyXTheme { ExportBottomSheet(onExport = {}) {} }
}
