package com.philkes.notallyx.core.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun IconListDialog(
    title: String,
    items: List<IconListItem>,
    onDismissRequest: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
)
