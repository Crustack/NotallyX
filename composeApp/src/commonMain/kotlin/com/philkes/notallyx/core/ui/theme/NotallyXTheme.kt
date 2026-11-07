package com.philkes.notallyx.core.ui.theme

import androidx.compose.runtime.Composable
import com.philkes.notallyx.core.preference.Theme

@Composable
expect fun NotallyXTheme(
    theme: Theme = Theme.FOLLOW_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
)

@Composable
expect fun NotallyXTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
)
