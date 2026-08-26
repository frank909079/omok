package com.frank.omok.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WoodPrimaryDark,
    onPrimary = OnWoodPrimaryDark,
    primaryContainer = WoodPrimaryContainerDark,
    onPrimaryContainer = OnWoodPrimaryContainerDark,
    secondary = StoneAccentDark,
    onSecondary = OnStoneAccentDark,
    secondaryContainer = StoneAccentContainerDark,
    onSecondaryContainer = OnStoneAccentContainerDark,
    background = WoodBackgroundDark,
    onBackground = OnWoodBackgroundDark,
    surface = WoodBackgroundDark,
    onSurface = OnWoodBackgroundDark,
    surfaceVariant = WoodSurfaceVariantDark,
    onSurfaceVariant = OnWoodSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = WoodPrimaryLight,
    onPrimary = OnWoodPrimaryLight,
    primaryContainer = WoodPrimaryContainerLight,
    onPrimaryContainer = OnWoodPrimaryContainerLight,
    secondary = StoneAccentLight,
    onSecondary = OnStoneAccentLight,
    secondaryContainer = StoneAccentContainerLight,
    onSecondaryContainer = OnStoneAccentContainerLight,
    background = WoodBackgroundLight,
    onBackground = OnWoodBackgroundLight,
    surface = WoodBackgroundLight,
    onSurface = OnWoodBackgroundLight,
    surfaceVariant = WoodSurfaceVariantLight,
    onSurfaceVariant = OnWoodSurfaceVariantLight
)

@Composable
fun OmokTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color (Android 12+ wallpaper-derived palette) is intentionally not used here:
    // this is a designed game palette, not something that should shift with the user's wallpaper.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
