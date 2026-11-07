package com.philkes.notallyx.feature.editor.bottomsheet

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class ActionItem(
    val labelResId: Int? = null,
    val drawableResId: Int? = null,
    val labelRes: StringResource? = null,
    val drawableRes: DrawableResource? = null,
    val label: String? = null,
    val showDividerAbove: Boolean = false,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)
