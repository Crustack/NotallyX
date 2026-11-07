package com.philkes.notallyx.feature.editor.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

/** Helper to create [BottomSheetDialogFragment] to display actions using Compose. */
open class ActionBottomSheet(
    private val actions: Collection<Action>,
    @ColorInt private val color: Int? = null,
    private val sheetTitle: String? = null,
    private val sheetOnReset: (() -> Unit)? = null,
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val preferences = NotallyXPreferences.getInstance(requireActivity())

        return ComposeView(requireContext()).apply {
            setContent {
                NotallyXTheme(theme = preferences.theme.value) {
                    ActionBottomSheetContent(
                        actions =
                            actions.map { action ->
                                ActionItem(
                                    labelResId = action.labelResId,
                                    drawableResId = action.drawableResId,
                                    label = action.label,
                                    showDividerAbove = action.showDividerAbove,
                                    isSelected = action.isSelected,
                                    onClick = {
                                        if (action.callback(this@ActionBottomSheet)) {
                                            dismiss()
                                        }
                                    },
                                )
                            },
                        title = sheetTitle,
                        onReset = sheetOnReset,
                        backgroundColor = color?.let { Color(it) },
                        onDismissRequest = { dismiss() },
                    )
                }
            }
        }
    }

    fun hide() {
        dismiss()
    }

    override fun dismiss() {
        if (isAdded) {
            super.dismiss()
        }
    }
}

data class Action(
    val labelResId: Int,
    val drawableResId: Int? = null,
    val label: String? = null,
    val showDividerAbove: Boolean = false,
    val isSelected: Boolean = false,
    /**
     * On click callback.
     *
     * @returns whether or not the BottomSheet should be hidden
     */
    val callback: (actionBottomSheet: ActionBottomSheet) -> Boolean,
)
