package com.philkes.notallyx.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.Theme

private val LightColorScheme =
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

private val DarkColorScheme =
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

private val SuperDarkColorScheme =
    darkColorScheme(
        primary = MdThemePrimary,
        onPrimary = ContainerSuperDark,
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
        background = ContainerSuperDark,
        onBackground = MdThemeOnBackgroundDark,
        surface = ContainerSuperDark,
        onSurface = MdThemeOnBackgroundDark,
        surfaceVariant = ContainerSuperDark,
        onSurfaceVariant = MdThemeOnSurfaceVariantDark,
        outline = MdThemeOutline,
        outlineVariant = MdThemeOutlineVariant,
        scrim = ContainerSuperDark,
        inverseSurface = MdThemeInverseSurface,
        inverseOnSurface = MdThemeInverseOnSurface,
        inversePrimary = MdThemeInversePrimary,
    )

/**
 * Main Composable Theme for NotallyX.
 *
 * @param theme Selected theme setting from [NotallyXPreferences] (LIGHT, DARK, SUPER_DARK, or
 *   FOLLOW_SYSTEM).
 * @param content The composable content to render within the theme.
 */
@Composable
fun NotallyXTheme(
    theme: Theme = Theme.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme: ColorScheme =
        when (theme) {
            Theme.LIGHT -> LightColorScheme
            Theme.DARK -> DarkColorScheme
            Theme.SUPER_DARK -> SuperDarkColorScheme
            Theme.FOLLOW_SYSTEM -> if (systemInDark) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotallyXTypography,
        content = content,
    )
}
