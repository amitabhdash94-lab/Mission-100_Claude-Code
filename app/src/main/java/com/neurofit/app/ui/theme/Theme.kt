package com.neurofit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeuroColorScheme = darkColorScheme(
    primary = NeuroColors.NeonCyan,
    onPrimary = NeuroColors.BackgroundVoid,
    secondary = NeuroColors.NeonMagenta,
    onSecondary = NeuroColors.BackgroundVoid,
    tertiary = NeuroColors.AcidGreen,
    onTertiary = NeuroColors.BackgroundVoid,
    background = NeuroColors.BackgroundVoid,
    onBackground = NeuroColors.TextPrimary,
    surface = NeuroColors.SurfaceDeep,
    onSurface = NeuroColors.TextPrimary,
    surfaceVariant = NeuroColors.SurfaceRaised,
    onSurfaceVariant = NeuroColors.TextSecondary,
    outline = NeuroColors.BorderDim,
    error = NeuroColors.DangerRed,
    onError = NeuroColors.BackgroundVoid
)

/** Dark only, no dynamic color. Samsung dark mode is always satisfied. */
@Composable
fun NeuroFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeuroColorScheme,
        typography = NeuroTypography,
        content = content
    )
}
