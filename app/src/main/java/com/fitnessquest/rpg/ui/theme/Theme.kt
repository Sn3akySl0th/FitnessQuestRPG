package com.fitnessquest.rpg.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fantasy palette: deep night blues with gold and arcane accents
val Gold = Color(0xFFF0A830)
val GoldDim = Color(0xFFB07E24)
val ArcaneBlue = Color(0xFF7BB4E3)
val MysticPurple = Color(0xFF9C7BE3)
val NightBg = Color(0xFF12131F)
val NightSurface = Color(0xFF1C1E30)
val NightSurfaceHigh = Color(0xFF262941)
val Parchment = Color(0xFFEDE6D4)
val HealthRed = Color(0xFFE35B5B)
val StaminaGreen = Color(0xFF6BC96B)

val StatStr = Color(0xFFE35B5B)
val StatEnd = Color(0xFF6BC96B)
val StatAgi = Color(0xFF7BB4E3)
val StatWil = Color(0xFF9C7BE3)

private val FitQuestColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF241A05),
    primaryContainer = GoldDim,
    onPrimaryContainer = Parchment,
    secondary = ArcaneBlue,
    onSecondary = Color(0xFF0A1A28),
    tertiary = MysticPurple,
    onTertiary = Color(0xFF160A28),
    background = NightBg,
    onBackground = Parchment,
    surface = NightSurface,
    onSurface = Parchment,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = Color(0xFFB9B4A6),
    outline = Color(0xFF4A4D6B),
    error = HealthRed,
    onError = Parchment,
    inverseSurface = Parchment,
    inverseOnSurface = NightBg,
    inversePrimary = GoldDim
)

private val FitQuestTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun FitQuestTheme(content: @Composable () -> Unit) {
    // Always dark: the fantasy night theme is the app's identity.
    // Wrap in Surface so LocalContentColor is parchment — Compose defaults it to
    // Color.Black, which made unlabeled Text invisible on night backgrounds.
    MaterialTheme(
        colorScheme = FitQuestColors,
        typography = FitQuestTypography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
