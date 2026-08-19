package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.domain.GearRarity

/**
 * Cohesive fantasy rarity color styling and ornate frame definitions.
 */
object RarityVisuals {
    val CommonColor = Color(0xFF94A3B8)       // Slate
    val UncommonColor = Color(0xFF22C55E)     // Emerald
    val RareColor = Color(0xFF06B6D4)         // Cyan
    val EpicColor = Color(0xFFA855F7)         // Mystic Purple
    val LegendaryColor = Color(0xFFF59E0B)    // Radiant Gold/Amber
    val MythicColor = Color(0xFFEF4444)       // Crimson Prismatic

    fun primaryColor(rarity: GearRarity): Color = when (rarity) {
        GearRarity.COMMON -> CommonColor
        GearRarity.UNCOMMON -> UncommonColor
        GearRarity.RARE -> RareColor
        GearRarity.EPIC -> EpicColor
        GearRarity.LEGENDARY -> LegendaryColor
        GearRarity.MYTHIC -> MythicColor
    }

    fun gradient(rarity: GearRarity, alpha: Float = 1f): List<Color> = when (rarity) {
        GearRarity.COMMON -> listOf(CommonColor.copy(alpha = alpha * 0.8f), CommonColor.copy(alpha = alpha * 0.4f))
        GearRarity.UNCOMMON -> listOf(UncommonColor.copy(alpha = alpha), Color(0xFF15803D).copy(alpha = alpha * 0.8f))
        GearRarity.RARE -> listOf(Color(0xFF38BDF8).copy(alpha = alpha), RareColor.copy(alpha = alpha), Color(0xFF1D4ED8).copy(alpha = alpha * 0.8f))
        GearRarity.EPIC -> listOf(Color(0xFFC084FC).copy(alpha = alpha), EpicColor.copy(alpha = alpha), Color(0xFF7E22CE).copy(alpha = alpha * 0.8f))
        GearRarity.LEGENDARY -> listOf(Color(0xFFFDE047).copy(alpha = alpha), LegendaryColor.copy(alpha = alpha), Color(0xFFD97706).copy(alpha = alpha * 0.9f))
        GearRarity.MYTHIC -> listOf(
            Color(0xFFF43F5E).copy(alpha = alpha),
            Color(0xFFEF4444).copy(alpha = alpha),
            Color(0xFF8B5CF6).copy(alpha = alpha * 0.9f),
            Color(0xFFF43F5E).copy(alpha = alpha)
        )
    }

    fun backgroundGlow(rarity: GearRarity): Color = primaryColor(rarity).copy(
        alpha = when (rarity) {
            GearRarity.COMMON -> 0.04f
            GearRarity.UNCOMMON -> 0.07f
            GearRarity.RARE -> 0.10f
            GearRarity.EPIC -> 0.14f
            GearRarity.LEGENDARY -> 0.18f
            GearRarity.MYTHIC -> 0.22f
        }
    )
}

/**
 * Adds an ornate rarity border with tier-scaled ornamentation, shimmer, and selection highlights.
 */
fun Modifier.rarityFrame(
    rarity: GearRarity,
    isSelected: Boolean = false,
    isEquipped: Boolean = false,
    cornerRadius: Dp = 12.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "rarityFrameAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (rarity) {
                    GearRarity.MYTHIC -> 1000
                    GearRarity.LEGENDARY -> 1300
                    GearRarity.EPIC -> 1600
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rarityAlpha"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rarityShimmer"
    )

    val baseColor = RarityVisuals.primaryColor(rarity)
    val cornerRadiusPx = cornerRadius.value

    this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithContent {
            // Draw regular content first
            drawContent()

            val w = size.width
            val h = size.height
            val strokeWidth = when {
                isSelected -> 2.5.dp.toPx()
                rarity >= GearRarity.LEGENDARY -> 2.dp.toPx()
                rarity >= GearRarity.RARE -> 1.6.dp.toPx()
                else -> 1.2.dp.toPx()
            }

            // Draw Tier-Specific Borders
            val borderBrush = when (rarity) {
                GearRarity.COMMON -> Brush.linearGradient(
                    listOf(baseColor.copy(alpha = 0.35f), baseColor.copy(alpha = 0.15f))
                )
                GearRarity.UNCOMMON -> Brush.linearGradient(
                    listOf(baseColor.copy(alpha = 0.8f), baseColor.copy(alpha = 0.4f))
                )
                GearRarity.RARE -> Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8), baseColor, Color(0xFF1D4ED8)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                GearRarity.EPIC -> Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC084FC).copy(alpha = pulseAlpha),
                        baseColor.copy(alpha = pulseAlpha),
                        Color(0xFF7E22CE).copy(alpha = pulseAlpha * 0.8f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                GearRarity.LEGENDARY -> {
                    val angle = shimmerOffset * 2f * Math.PI.toFloat()
                    val startX = (w / 2f) + (w / 2f) * kotlin.math.cos(angle)
                    val startY = (h / 2f) + (h / 2f) * kotlin.math.sin(angle)
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBEB), baseColor, Color(0xFFB45309)),
                        center = Offset(startX, startY),
                        radius = (w + h) / 2f
                    )
                }
                GearRarity.MYTHIC -> {
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFF43F5E),
                            Color(0xFFFB7185),
                            Color(0xFFA855F7),
                            Color(0xFF38BDF8),
                            Color(0xFFF43F5E)
                        )
                    )
                }
            }

            // Outer border
            drawRoundRect(
                brush = borderBrush,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(w - strokeWidth, h - strokeWidth),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // Ornate Corner Accents for Rare, Epic, Legendary, Mythic
            if (rarity >= GearRarity.RARE) {
                val cornerSize = 10.dp.toPx()
                val accentColor = if (rarity >= GearRarity.LEGENDARY) Color.White.copy(alpha = 0.9f) else baseColor
                val cornerStroke = 2.dp.toPx()

                // Top-left notch
                drawLine(accentColor, Offset(0f, cornerSize), Offset(0f, 0f), strokeWidth = cornerStroke)
                drawLine(accentColor, Offset(0f, 0f), Offset(cornerSize, 0f), strokeWidth = cornerStroke)

                // Top-right notch
                drawLine(accentColor, Offset(w - cornerSize, 0f), Offset(w, 0f), strokeWidth = cornerStroke)
                drawLine(accentColor, Offset(w, 0f), Offset(w, cornerSize), strokeWidth = cornerStroke)

                // Bottom-left notch
                drawLine(accentColor, Offset(0f, h - cornerSize), Offset(0f, h), strokeWidth = cornerStroke)
                drawLine(accentColor, Offset(0f, h), Offset(cornerSize, h), strokeWidth = cornerStroke)

                // Bottom-right notch
                drawLine(accentColor, Offset(w - cornerSize, h), Offset(w, h), strokeWidth = cornerStroke)
                drawLine(accentColor, Offset(w, h), Offset(w, h - cornerSize), strokeWidth = cornerStroke)
            }

            // Selected Glow Ring
            if (isSelected) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
}

/**
 * A stylized pill or badge displaying the gear rarity label with matching theme colors.
 */
@Composable
fun RarityBadge(
    rarity: GearRarity,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val color = RarityVisuals.primaryColor(rarity)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 5.dp else 7.dp, vertical = if (compact) 2.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 5.dp else 6.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = rarity.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                ),
                color = color
            )
        }
    }
}
