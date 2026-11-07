package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.editor.NoteActionHandler

/** BottomSheet inside text note for adding files, recording audio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteBottomSheet(
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    val actions = remember(actionHandler) { createAddNoteActions(actionHandler) }
    ActionModalBottomSheet(
        actions = actions,
        backgroundColor = backgroundColor,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    )
}

fun createAddNoteActions(actionHandler: NoteActionHandler): List<ActionItem> =
    createAddActions(actionHandler) +
        listOf(
            ActionItem(
                labelResId = R.string.link_note,
                drawableResId = R.drawable.notebook,
            ) {
                actionHandler.addNoteLink()
            }
        )

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddNoteBottomSheetPreview() {
    NotallyXTheme { AddNoteBottomSheet(actionHandler = object : NoteActionHandler {}) {} }
}
