package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R

data class ActionItem(
    val labelResId: Int? = null,
    val drawableResId: Int? = null,
    val label: String? = null,
    val showDividerAbove: Boolean = false,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun ActionBottomSheetContent(
    actions: List<ActionItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (backgroundColor != null) Modifier.background(backgroundColor) else Modifier
                )
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
    ) {
        title?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                onReset?.let { reset ->
                    IconButton(onClick = { reset() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.undo),
                            contentDescription = stringResource(R.string.reset_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        actions.forEach { action ->
            if (action.showDividerAbove) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                )
            }

            val itemLabel = action.label ?: action.labelResId?.let { stringResource(it) } ?: ""

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics { selected = action.isSelected }
                        .clickable {
                            action.onClick()
                            onDismissRequest()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                action.drawableResId?.let { resId ->
                    Icon(
                        painter = painterResource(id = resId),
                        contentDescription = itemLabel,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Text(
                    text = itemLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (action.isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionModalBottomSheet(
    actions: List<ActionItem>,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = backgroundColor ?: MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ActionModalBottomSheet"),
    ) {
        ActionBottomSheetContent(
            actions = actions,
            title = title,
            onReset = onReset,
            onDismissRequest = onDismissRequest,
        )
    }
}
