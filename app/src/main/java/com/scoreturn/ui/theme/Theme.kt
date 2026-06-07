package com.scoreturn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ScoreTurnColorScheme = darkColorScheme(
    primary = ScoreTurnAccent,
    secondary = ScoreTurnSurface,
    background = ScoreTurnPrimary,
    surface = ScoreTurnSecondary,
    onPrimary = ScoreTurnOnPrimary,
    onBackground = ScoreTurnOnPrimary,
    onSurface = ScoreTurnOnPrimary,
)

@Composable
fun ScoreTurnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScoreTurnColorScheme,
        content = content
    )
}