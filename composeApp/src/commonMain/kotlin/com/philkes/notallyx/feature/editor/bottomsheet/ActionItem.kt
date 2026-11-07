package com.philkes.notallyx.feature.editor.bottomsheet

data class ActionItem(
    val labelResId: Int? = null,
    val drawableResId: Int? = null,
    val label: String? = null,
    val showDividerAbove: Boolean = false,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)
