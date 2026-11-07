package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.Res
import com.philkes.notallyx.export
import com.philkes.notallyx.feature.migration.ExportMimeType
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onExport: (ExportMimeType) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = stringResource(Res.string.export),
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
            labelRes = Res.string.export,
            label = mimeType.name,
        ) {
            onExport(mimeType)
        }
    }
