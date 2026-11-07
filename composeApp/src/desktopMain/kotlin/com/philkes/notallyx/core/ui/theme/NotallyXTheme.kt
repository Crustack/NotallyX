package com.philkes.notallyx.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val DesktopLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = MdThemePrimary,
        onPrimary = MdThemeOnPrimary,
        primaryContainer = MdThemePrimaryContainer,
        onPrimaryContainer = MdThemeOnPrimaryContainer,
        secondary = MdThemeSecondary,
        onSecondary = MdThemeOnSecondary,
        secondaryContainer = MdThemeSecondaryContainer,
        onSecondaryContainer = MdThemeOnSecondaryContainer,
        tertiary = MdThemeTertiary,
        onTertiary = MdThemeOnTertiary,
        tertiaryContainer = MdThemeTertiaryContainer,
        onTertiaryContainer = MdThemeOnTertiaryContainer,
        error = MdThemeError,
        onError = MdThemeOnError,
        errorContainer = MdThemeErrorContainer,
        onErrorContainer = MdThemeOnErrorContainer,
        background = ContainerLight,
        onBackground = MdThemeOnBackgroundLight,
        surface = ContainerLight,
        onSurface = MdThemeOnBackgroundLight,
        surfaceVariant = MdThemeSurfaceVariantLight,
        onSurfaceVariant = MdThemeOnSurfaceVariantLight,
        outline = MdThemeOutline,
        outlineVariant = MdThemeOutlineVariant,
        scrim = ContainerSuperDark,
        inverseSurface = MdThemeInverseSurface,
        inverseOnSurface = MdThemeInverseOnSurface,
        inversePrimary = MdThemeInversePrimary,
    )

val DesktopDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = MdThemePrimary,
        onPrimary = MdThemeOnPrimary,
        primaryContainer = MdThemePrimaryContainer,
        onPrimaryContainer = MdThemeOnPrimaryContainer,
        secondary = MdThemeSecondary,
        onSecondary = MdThemeOnSecondary,
        secondaryContainer = MdThemeSecondaryContainer,
        onSecondaryContainer = MdThemeOnSecondaryContainer,
        tertiary = MdThemeTertiary,
        onTertiary = MdThemeOnTertiary,
        tertiaryContainer = MdThemeTertiaryContainer,
        onTertiaryContainer = MdThemeOnTertiaryContainer,
        error = MdThemeError,
        onError = MdThemeOnError,
        errorContainer = MdThemeErrorContainer,
        onErrorContainer = MdThemeOnErrorContainer,
        background = MdThemeSurfaceContainerLowDark,
        onBackground = MdThemeOnBackgroundDark,
        surface = MdThemeSurfaceContainerLowDark,
        onSurface = MdThemeOnBackgroundDark,
        surfaceVariant = MdThemeSurfaceContainerDark,
        onSurfaceVariant = MdThemeOnSurfaceVariantDark,
        outline = MdThemeOutline,
        outlineVariant = MdThemeOutlineVariant,
        scrim = ContainerSuperDark,
        inverseSurface = MdThemeInverseSurface,
        inverseOnSurface = MdThemeInverseOnSurface,
        inversePrimary = MdThemeInversePrimary,
    )

@Composable
actual fun NotallyXTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DesktopDarkColorScheme else DesktopLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotallyXTypography,
        content = content,
    )
}
