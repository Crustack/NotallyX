package com.philkes.notallyx.presentation.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing preference.xml layout. */
@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (enabled && onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Column(
        modifier = modifier.fillMaxWidth().then(tagModifier).then(clickableModifier).padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        if (!value.isNullOrEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreferenceRowPreview() {
    NotallyXTheme { PreferenceRow(title = "Theme", value = "Follow System") }
}
