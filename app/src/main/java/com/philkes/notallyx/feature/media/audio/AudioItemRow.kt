package com.philkes.notallyx.feature.media.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing recycler_audio.xml layout. */
@Composable
internal fun AudioItemRow(
    dateText: String,
    lengthText: String,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current,
    lengthColor: Color = LocalContentColor.current.copy(alpha = 0.7f),
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            text = lengthText,
            style = MaterialTheme.typography.bodyMedium,
            color = lengthColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AudioItemRowPreview() {
    NotallyXTheme {
        AudioItemRow(
            dateText = "Monday, January 1, 2024, 12:00 PM",
            lengthText = "01:23",
        )
    }
}
