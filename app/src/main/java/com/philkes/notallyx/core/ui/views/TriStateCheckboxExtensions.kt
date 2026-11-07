package com.philkes.notallyx.core.ui.views

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.state.ToggleableState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

fun MaterialAlertDialogBuilder.setMultiChoiceTriStateItems(
    context: Context,
    items: Array<String>,
    checkedStates: Array<ToggleableState>,
    onMultiChoiceClickListener: (index: Int, state: ToggleableState) -> Unit,
): MaterialAlertDialogBuilder {
    val contextWrapper = context as? ContextWrapper ?: ContextWrapper(context)
    val preferences = NotallyXPreferences.getInstance(contextWrapper)
    val composeView = ComposeView(context)
    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            TriStateMultiChoiceContent(
                items = items,
                initialStates = checkedStates,
                onItemClick = onMultiChoiceClickListener,
            )
        }
    }
    setView(composeView)
    return this
}
