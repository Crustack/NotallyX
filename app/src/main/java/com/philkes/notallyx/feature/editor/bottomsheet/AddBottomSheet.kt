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

/** BottomSheet inside note for adding files, recording audio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBottomSheet(
    actionHandler: NoteActionHandler,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    onDismissRequest: () -> Unit,
) {
    val actions = remember(actionHandler) { createAddActions(actionHandler) }
    ActionModalBottomSheet(
        actions = actions,
        backgroundColor = backgroundColor,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    )
}

fun createAddActions(actionHandler: NoteActionHandler): List<ActionItem> =
    listOf(
        ActionItem(
            labelResId = R.string.add_images,
            drawableResId = R.drawable.add_images,
        ) {
            actionHandler.addImages()
        },
        ActionItem(
            labelResId = R.string.attach_file,
            drawableResId = R.drawable.text_file,
        ) {
            actionHandler.attachFiles()
        },
        ActionItem(
            labelResId = R.string.record_audio,
            drawableResId = R.drawable.record_audio,
        ) {
            actionHandler.recordAudio()
        },
    )

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddBottomSheetPreview() {
    NotallyXTheme { AddBottomSheet(actionHandler = object : NoteActionHandler {}) {} }
}
