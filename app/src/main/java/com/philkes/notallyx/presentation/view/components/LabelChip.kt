package com.philkes.notallyx.presentation.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing label.xml layout. */
@Composable
fun LabelChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier =
        if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }

    Text(
        text = text,
        modifier = clickableModifier.padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelChipPreview() {
    NotallyXTheme { LabelChip(text = "Work") }
}
