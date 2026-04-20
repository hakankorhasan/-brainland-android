package com.example.brain_land.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BrainlandColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentCyan,
    tertiary = AccentIndigo,
    background = BgDark,
    surface = BgCard,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun BrainlandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrainlandColorScheme,
        typography = Typography,
        content = content
    )
}