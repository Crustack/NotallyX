package com.philkes.notallyx.feature.main.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.main.routes.picknote.PickNoteRoute as PickNoteRouteImpl

@Composable
fun PickNoteRoute(
    excludeNoteId: Long?,
    onNotePicked: (BaseNote) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PickNoteRouteImpl(
        excludeNoteId = excludeNoteId,
        onNotePicked = onNotePicked,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

const val EXTRA_EXCLUDE_NOTE_ID = "notallyx.intent.extra.EXCLUDE_NOTE_ID"
const val EXTRA_PICKED_NOTE_ID = "notallyx.intent.extra.PICKED_NOTE_ID"
const val EXTRA_PICKED_NOTE_TITLE = "notallyx.intent.extra.PICKED_NOTE_TITLE"
const val EXTRA_PICKED_NOTE_TYPE = "notallyx.intent.extra.PICKED_NOTE_TYPE"
