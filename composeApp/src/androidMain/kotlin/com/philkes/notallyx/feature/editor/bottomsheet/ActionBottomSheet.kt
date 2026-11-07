package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Helper composable to display actions using Jetpack Compose [ActionModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBottomSheet(
    actions: List<ActionItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    onReset: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
) {
    ActionModalBottomSheet(
        actions = actions,
        modifier = modifier,
        title = title,
        onReset = onReset,
        backgroundColor = backgroundColor,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    )
}

typealias Action = ActionItem

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ActionBottomSheetPreview() {
    NotallyXTheme {
        ActionBottomSheet(
            title = "Sample Actions",
            actions =
                listOf(
                    ActionItem(
                        label = "Option 1",
                        drawableResId = R.drawable.add,
                    ) {},
                    ActionItem(
                        label = "Option 2",
                        drawableResId = R.drawable.delete,
                        showDividerAbove = true,
                    ) {},
                ),
        ) {}
    }
}
