package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Composable replacing fragment_notes.xml layout for LabelsFragment. */
@Composable
internal fun LabelsScreen(
    labels: List<LabelData>,
    modifier: Modifier = Modifier,
    onLabelClick: (LabelData) -> Unit = {},
    onEditClick: (LabelData) -> Unit = {},
    onDeleteClick: (LabelData) -> Unit = {},
    onToggleVisibilityClick: (LabelData) -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (labels.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.label),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(labels, key = { it.value }) { label ->
                    LabelItemRow(
                        label = label.value,
                        isVisibleInNavigation = label.visibleInNavigation,
                        onClick = { onLabelClick(label) },
                        onEdit = { onEditClick(label) },
                        onDelete = { onDeleteClick(label) },
                        onToggleVisibility = { onToggleVisibilityClick(label) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    NotallyXTheme {
        LabelsScreen(
            labels =
                listOf(
                    LabelData("Personal", true, 0),
                    LabelData("Work", false, 1),
                )
        )
    }
}
