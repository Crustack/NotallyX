package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.philkes.notallyx.Res
import com.philkes.notallyx.add_images
import com.philkes.notallyx.attach_file
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.record_audio
import com.philkes.notallyx.text_file

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
            labelRes = Res.string.add_images,
            drawableRes = Res.drawable.add_images,
        ) {
            actionHandler.addImages()
        },
        ActionItem(
            labelRes = Res.string.attach_file,
            drawableRes = Res.drawable.text_file,
        ) {
            actionHandler.attachFiles()
        },
        ActionItem(
            labelRes = Res.string.record_audio,
            drawableRes = Res.drawable.record_audio,
        ) {
            actionHandler.recordAudio()
        },
    )
