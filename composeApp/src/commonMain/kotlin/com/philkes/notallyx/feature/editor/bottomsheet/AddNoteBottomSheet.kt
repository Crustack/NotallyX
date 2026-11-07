package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.Res
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.link_note
import com.philkes.notallyx.notebook

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
                labelRes = Res.string.link_note,
                drawableRes = Res.drawable.notebook,
            ) {
                actionHandler.addNoteLink()
            }
        )
