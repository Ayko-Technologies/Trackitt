package com.ayko.trackitt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TrackittColorScheme = lightColorScheme(
    surface = Surface,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiaryContainer = TertiaryContainer,
    background = Surface,
    onBackground = OnSurface,
)

@Composable
fun TrackittTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrackittColorScheme,
        typography = TrackittTypography,
        shapes = TrackittShapes,
        content = content,
    )
}
