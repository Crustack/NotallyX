package com.philkes.notallyx.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing recycler_toggle.xml layout. */
@Composable
internal fun ToggleOptionItem(
    @DrawableRes iconResId: Int,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    FilledIconToggleButton(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.padding(end = 14.dp),
        colors =
            IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ToggleOptionItemPreview() {
    NotallyXTheme {
        ToggleOptionItem(
            iconResId = R.drawable.pin,
            isChecked = true,
            onCheckedChange = {},
            contentDescription = "Pin Note",
        )
    }
}
