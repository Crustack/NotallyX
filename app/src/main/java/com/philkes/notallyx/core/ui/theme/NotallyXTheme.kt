package com.philkes.notallyx.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
 * @param dynamicColor Whether to use dynamic system wallpaper colors on Android 12+.
 * @param content The composable content to render within the theme.
 */
@Composable
fun NotallyXTheme(
    theme: Theme = Theme.FOLLOW_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark =
        when (theme) {
            Theme.LIGHT -> false
            Theme.DARK -> true
            Theme.SUPER_DARK -> true
            Theme.FOLLOW_SYSTEM -> systemInDark
        }
    val context = LocalContext.current
    val colorScheme: ColorScheme =
        when {
            theme == Theme.SUPER_DARK -> SuperDarkColorScheme
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            theme == Theme.LIGHT -> LightColorScheme
            theme == Theme.DARK -> DarkColorScheme
            theme == Theme.FOLLOW_SYSTEM -> if (systemInDark) DarkColorScheme else LightColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotallyXTypography,
        content = content,
    )
}
