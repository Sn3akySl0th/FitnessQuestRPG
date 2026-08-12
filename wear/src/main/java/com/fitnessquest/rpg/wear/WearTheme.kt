package com.fitnessquest.rpg.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WearColors = Colors(
    primary = Color(0xFFF0A830),
    primaryVariant = Color(0xFFB07E24),
    secondary = Color(0xFF7BB4E3),
    secondaryVariant = Color(0xFF5A9AC8),
    background = Color(0xFF12131F),
    surface = Color(0xFF1C1E30),
    error = Color(0xFFE35B5B),
    onPrimary = Color(0xFF241A05),
    onSecondary = Color(0xFF0A1A28),
    onBackground = Color(0xFFEDE6D4),
    onSurface = Color(0xFFEDE6D4),
    onError = Color.White
)

@Composable
fun FitnessRpgWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColors,
        content = content
    )
}
