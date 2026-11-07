package com.philkes.notallyx.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Primary & Accent Colors
val MdThemePrimary = Color(0xFF7285D0)
val MdThemeOnPrimary = Color(0xFFFFFFFF)
val MdThemePrimaryContainer = Color(0xFFDCE1FF)
val MdThemeOnPrimaryContainer = Color(0xFF05164B)

// Secondary Colors
val MdThemeSecondary = Color(0xFF595D72)
val MdThemeOnSecondary = Color(0xFFFFFFFF)
val MdThemeSecondaryContainer = Color(0xFFDEE1F9)
val MdThemeOnSecondaryContainer = Color(0xFF161B2C)

// Tertiary Colors
val MdThemeTertiary = Color(0xFF75546F)
val MdThemeOnTertiary = Color(0xFFFFFFFF)
val MdThemeTertiaryContainer = Color(0xFFFFD7F5)
val MdThemeOnTertiaryContainer = Color(0xFF2C1229)

// Error Colors
val MdThemeError = Color(0xFFBA1A1A)
val MdThemeOnError = Color(0xFFFFFFFF)
val MdThemeErrorContainer = Color(0xFFFFDAD6)
val MdThemeOnErrorContainer = Color(0xFF410002)

// Surface & Background Colors
val ContainerLight = Color(0xFFFAF8FF)
val ContainerDark = Color(0xFF1A1B21)
val ContainerSuperDark = Color(0xFF000000)

val MdThemeOnBackgroundLight = Color(0xFF1A1B21)
val MdThemeOnBackgroundDark = Color(0xFFF1F0F7)

val MdThemeSurfaceDark = Color(0xFF121318)
val MdThemeSurfaceContainerLowDark = Color(0xFF1E1F25)
val MdThemeSurfaceContainerDark = Color(0xFF23242A)

val MdThemeSurfaceVariantLight = Color(0xFFE2E1EC)
val MdThemeOnSurfaceVariantLight = Color(0xFF45464F)
val MdThemeSurfaceVariantDark = Color(0xFF45464F)
val MdThemeOnSurfaceVariantDark = Color(0xFFC6C5D0)

val MdThemeOutline = Color(0xFF767680)
val MdThemeOutlineVariant = Color(0xFFC6C5D0)

val MdThemeInverseSurface = Color(0xFF2F3036)
val MdThemeInverseOnSurface = Color(0xFFF1F0F7)
val MdThemeInversePrimary = Color(0xFFB7C4FF)

fun contentColorFor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() > 0.5f) {
        MdThemeOnBackgroundLight
    } else {
        MdThemeOnBackgroundDark
    }

val LightColorScheme: androidx.compose.material3.ColorScheme =
    androidx.compose.material3.lightColorScheme(
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

val DarkColorScheme: androidx.compose.material3.ColorScheme =
    androidx.compose.material3.darkColorScheme(
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

val SuperDarkColorScheme: androidx.compose.material3.ColorScheme =
    androidx.compose.material3.darkColorScheme(
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
