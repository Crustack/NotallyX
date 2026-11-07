package com.philkes.notallyx.presentation.view.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing menu_item.xml layout. */
@Composable
fun MenuItemRow(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconResId != null) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuItemRowPreview() {
    NotallyXTheme {
        MenuItemRow(
            title = "Edit Note",
            iconResId = R.drawable.edit,
        )
    }
}
