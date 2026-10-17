package com.philkes.notallyx.feature.editor.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.EditTextAutoClearFocus

/** Composable replacing recycler_list_item.xml layout. */
@Composable
internal fun ChecklistItemRow(
    text: String? = null,
    editText: EditTextAutoClearFocus? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isChild: Boolean = false,
    showDragHandle: Boolean = true,
    showDelete: Boolean = true,
    onDragTouch: (MotionEvent) -> Boolean = { false },
    onDelete: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isChild) {
            Spacer(modifier = Modifier.width(20.dp))
        }

        if (showDragHandle) {
            Icon(
                painter = painterResource(id = R.drawable.drag_handle),
                contentDescription = stringResource(R.string.drag_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(8.dp).pointerInteropFilter { event -> onDragTouch(event) },
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = MaterialTheme.colorScheme.surface,
                ),
        )

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (editText != null) {
                AndroidView(
                    factory = { editText },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration =
                                if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                    color =
                        if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            text = "Buy groceries",
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowUncheckedPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            text = "Buy groceries",
            isChecked = false,
            onCheckedChange = {},
        )
    }
}
