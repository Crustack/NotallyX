package com.philkes.notallyx.presentation.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.presentation.theme.NotallyXTheme

/** Composable replacing recycler_base_note.xml layout. */
@Composable
fun BaseNoteCard(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    dateText: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit = {},
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (!body.isNullOrEmpty()) {
                if (title.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 10,
                )
            }
            if (!dateText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BaseNoteCardPreview() {
    NotallyXTheme {
        BaseNoteCard(
            title = "Meeting Notes",
            body = "Discuss Compose migration timeline and test coverage.",
            dateText = "Created 2 hours ago",
        )
    }
}
