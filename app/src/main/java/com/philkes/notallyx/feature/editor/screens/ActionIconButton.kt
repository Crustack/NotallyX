package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun ActionIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.Companion,
) {
    val clickModifier =
        if (onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        } else {
            Modifier.clickable(enabled = enabled, onClick = onClick)
        }

    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint =
                if (enabled) LocalContentColor.current
                else LocalContentColor.current.copy(alpha = 0.38f),
        )
    }
}
