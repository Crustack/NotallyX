package com.philkes.notallyx.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.philkes.notallyx.core.preference.Theme

@Composable
actual fun NotallyXTheme(
    theme: Theme,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme: ColorScheme =
        when (theme) {
            Theme.SUPER_DARK -> SuperDarkColorScheme
            Theme.DARK -> DarkColorScheme
            Theme.LIGHT -> LightColorScheme
            Theme.FOLLOW_SYSTEM -> if (systemInDark) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotallyXTypography,
        content = content,
    )
}

@Composable
actual fun NotallyXTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    NotallyXTheme(
        theme = if (darkTheme) Theme.DARK else Theme.LIGHT,
        dynamicColor = false,
        content = content,
    )
}
