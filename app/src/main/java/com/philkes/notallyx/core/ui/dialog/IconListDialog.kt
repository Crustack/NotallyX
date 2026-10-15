package com.philkes.notallyx.core.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

data class IconListItem(
    val iconResId: Int,
    val text: String,
)

@Composable
internal fun IconListDialog(
    title: String,
    items: List<IconListItem>,
    onDismissRequest: () -> Unit,
    onItemClick: (index: Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items) { index, item ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    onItemClick(index)
                                    onDismissRequest()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .testTag("IconListItem_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("button1"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun IconListDialogPreview() {
    NotallyXTheme {
        IconListDialog(
            title = "Choose app",
            items =
                listOf(
                    IconListItem(R.drawable.icon_notally, "Notally"),
                    IconListItem(R.drawable.database, "Database"),
                ),
            onDismissRequest = {},
            onItemClick = {},
        )
    }
}
