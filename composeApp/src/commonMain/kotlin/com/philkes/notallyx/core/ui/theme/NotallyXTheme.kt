package com.philkes.notallyx.core.ui.theme

import androidx.compose.runtime.Composable

@Composable
expect fun NotallyXTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
)
