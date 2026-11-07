package com.philkes.notallyx.presentation.view.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing recycler_list_item.xml layout. */
@Composable
fun ChecklistItemRow(
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.drag_handle),
            contentDescription = stringResource(R.string.drag_handle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp),
        )
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
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
            modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp),
        )
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            text = "Buy groceries",
            isChecked = false,
            onCheckedChange = {},
        )
    }
}
