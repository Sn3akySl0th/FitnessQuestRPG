package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CombatStats
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.domain.Monster
import com.fitnessquest.rpg.domain.BossRelics
import com.fitnessquest.rpg.domain.LootRates
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.HealthRed
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.StaminaGreen

@Composable
fun BossProgressCard(
    biome: Biome,
    boss: Monster,
    biomeProgress: BiomeProgressEntity?,
    combat: CombatStats?,
    playerLevel: Int,
    enabled: Boolean,
    onChallenge: (Int) -> Unit,
    onViewLoot: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val points = biomeProgress?.progressPoints ?: 0
    val unlockThreshold = ProgressionRules.bossUnlockPointsFor(biome)
    val isUnlocked = biomeProgress?.bossUnlocked ?: false || (points >= unlockThreshold)
    val isDefeated = biomeProgress?.bossDefeated ?: false
    val firstClearClaimed = biomeProgress?.firstClearRewardClaimed ?: false
    val onCooldown = ProgressionRules.isBossOnCooldown(biomeProgress)
    val remainingMs = ProgressionRules.bossCooldownRemainingMs(biomeProgress)
    val cooldownHours = remainingMs / 3600000
    val cooldownMins = (remainingMs % 3600000) / 60000
    val cooldownLabel = if (cooldownHours > 0) "${cooldownHours}h ${cooldownMins}m" else "${cooldownMins}m"

    val progressFraction = (points.toFloat() / unlockThreshold.toFloat()).coerceIn(0f, 1f)
    val pointsNeeded = (unlockThreshold - points).coerceAtLeast(0)

    val infiniteTransition = rememberInfiniteTransition(label = "bossPulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseBorder",
    )

    val cardBorder = when {
        isUnlocked && !isDefeated -> BorderStroke(
            1.5.dp,
            Brush.sweepGradient(listOf(Gold.copy(alpha = pulseBorderAlpha), HealthRed, Gold.copy(alpha = pulseBorderAlpha))),
        )
        isDefeated -> BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    }

    val cardBg = when {
        isUnlocked && !isDefeated -> Brush.verticalGradient(
            listOf(Color(0xFF2A1515), Color(0xFF190D0D), NightBg),
        )
        isDefeated -> Brush.verticalGradient(
            listOf(Color(0xFF1E231B), Color(0xFF111710), NightBg),
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFF1E1F24), Color(0xFF141519), NightBg),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = cardBorder,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(cardBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Boss Info & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(boss.emoji, fontSize = 28.sp)
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = boss.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HealthRed.copy(alpha = 0.25f),
                                border = BorderStroke(0.5.dp, HealthRed.copy(alpha = 0.6f)),
                            ) {
                                Text(
                                    text = "BOSS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = HealthRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Text(
                            text = "Level ${boss.level} • ${biome.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }

                // State Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    onViewLoot?.let { viewLoot ->
                        LootInfoIconButton(
                            contentDescription = "View boss loot for ${boss.name}",
                            onClick = viewLoot,
                        )
                    }
                    when {
                    isDefeated -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (onCooldown) Color(0xFF37474F).copy(alpha = 0.5f) else Gold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (onCooldown) Color.White.copy(alpha = 0.3f) else Gold),
                        ) {
                            Text(
                                text = if (onCooldown) "⏳ COOLDOWN ($cooldownLabel)" else "🏆 DEFEATED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (onCooldown) Color.White.copy(alpha = 0.8f) else Gold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    isUnlocked -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HealthRed.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, HealthRed),
                        ) {
                            Text(
                                text = "⚔️ READY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = HealthRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                text = "🔒 LOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
            }

            // Description / Lore
            Text(
                text = boss.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = "Drops: ${BossRelics.displayNameFor(biome)} · T${LootRates.biomeBossGearTier(biome)} gear · " +
                    "${LootRates.BOSS_FARM_RELIC_CHANCE}% relic on re-fights",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.85f),
            )

            // State-specific content: Progress Bar or Challenge Options
            if (!isUnlocked && !isDefeated) {
                // Progress Bar Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Unlock Progress",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Text(
                            text = "$points / $unlockThreshold pts (${(progressFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Gold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                    Text(
                        text = "Earn $pointsNeeded more points by winning battles or logging workouts.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // Trait warning & Combat Recommendation
            boss.trait?.let { trait ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(trait.emoji, fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Trait: ${trait.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                            )
                            Text(
                                text = trait.blurb,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            // First clear milestone banner
            if (!firstClearClaimed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Gold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🎁", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "FIRST-CLEAR MILESTONE REWARDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Gold,
                            )
                            Text(
                                text = "Guaranteed High-Tier Gear, Biome Chest & Unlocks the Next Biome!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            // Action Button
            when {
                isUnlocked && !isDefeated -> {
                    Button(
                        onClick = {
                            AudioEffects.playCritHit()
                            HapticEffects.performCritHit(haptic, context)
                            onChallenge(boss.id)
                        },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthRed,
                            disabledContainerColor = HealthRed.copy(alpha = 0.35f),
                        ),
                    ) {
                        Text(
                            text = "⚔️ CHALLENGE BOSS (${GameMath.BATTLE_ENERGY_COST}⚡)",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White,
                        )
                    }
                }
                isDefeated -> {
                    OutlinedButton(
                        onClick = {
                            AudioEffects.playSetLogged()
                            HapticEffects.performSetLogged(haptic, context)
                            onChallenge(boss.id)
                        },
                        enabled = enabled && !onCooldown,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (onCooldown) Color.White.copy(alpha = 0.2f) else Gold.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = if (onCooldown) "⏳ RESPAWNING IN $cooldownLabel" else "🔄 RE-FIGHT BOSS (${GameMath.BATTLE_ENERGY_COST}⚡)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (onCooldown) Color.White.copy(alpha = 0.5f) else Gold,
                        )
                    }
                }
            }
        }
    }
}
