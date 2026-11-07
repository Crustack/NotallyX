package com.philkes.notallyx.core.ui.dialog

import org.jetbrains.compose.resources.DrawableResource

data class IconListItem(
    val iconResId: Int = 0,
    val text: String,
    val icon: DrawableResource? = null,
)
