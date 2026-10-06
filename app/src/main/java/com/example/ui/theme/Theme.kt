package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MaxoDarkColorScheme = darkColorScheme(
    primary = MaxoTextWhite,
    onPrimary = MaxoPureBlack,
    primaryContainer = MaxoElevatedBlack,
    onPrimaryContainer = MaxoTextWhite,
    secondary = MaxoTextSecondary,
    onSecondary = MaxoPureBlack,
    secondaryContainer = MaxoSurfaceBlack,
    onSecondaryContainer = MaxoTextSecondary,
    tertiary = MaxoTextMuted,
    onTertiary = MaxoPureBlack,
    background = MaxoDeepBlack,
    onBackground = MaxoTextWhite,
    surface = MaxoSurfaceBlack,
    onSurface = MaxoTextWhite,
    surfaceVariant = MaxoElevatedBlack,
    onSurfaceVariant = MaxoTextSecondary,
    outline = MaxoBorderSubtle,
    outlineVariant = MaxoBorderGlass,
    error = MaxoAccentRed,
    onError = MaxoPureBlack
)

@Composable
fun MaxoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaxoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
