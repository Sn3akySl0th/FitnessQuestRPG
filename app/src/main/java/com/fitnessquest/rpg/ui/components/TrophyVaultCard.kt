package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.MysticPurple
import kotlin.math.floor

data class Trophy(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val current: Int,
    val target: Int,
    val rewardGold: Int = 100,
    /** When set, overrides current >= target (e.g. distance stored in km). */
    private val unlockedOverride: Boolean? = null
) {
    val unlocked: Boolean get() = unlockedOverride ?: (current >= target)
    val progressFraction: Float get() = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** Lifetime cardio distance required for Wind Walker (stored as km). */
const val WIND_WALKER_TARGET_KM = 10.0

/**
 * RPG Milestone Trophy Vault component displaying unlocked badges,
 * progress bars, and reward claim buttons for major fitness achievements.
 */
@Composable
fun TrophyVaultCard(
    character: CharacterEntity,
    modifier: Modifier = Modifier,
    claimedIds: Set<String> = emptySet(),
    lifetimeCardioKm: Double = 0.0,
    imperial: Boolean = false,
    onClaimReward: (Trophy) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val trophies = remember(character, lifetimeCardioKm, imperial) {
        val cardioTarget = floor(Units.kmToDisplay(WIND_WALKER_TARGET_KM, imperial)).toInt().coerceAtLeast(1)
        listOf(
            Trophy("1", "Iron Titan", "🏋️", "Reach Level 25 Strength & Power", character.strength, 25, 200),
            Trophy(
                id = "2",
                title = "Wind Walker",
                emoji = "🏃",
                description = "Log ${Units.formatDistance(WIND_WALKER_TARGET_KM, imperial)} total cardio",
                current = ((Units.kmToDisplay(lifetimeCardioKm, imperial) /
                    Units.kmToDisplay(WIND_WALKER_TARGET_KM, imperial).coerceAtLeast(0.1)) * cardioTarget)
                    .toInt()
                    .coerceIn(0, cardioTarget),
                target = cardioTarget,
                rewardGold = 200,
                unlockedOverride = lifetimeCardioKm >= WIND_WALKER_TARGET_KM
            ),
            Trophy("3", "Consistency King", "🔥", "Maintain a 7-Day Workout Streak", character.streak, 7, 200),
            Trophy("4", "Slayer of Beasts", "⚔️", "Win 10 Turn-Based Battles", character.battlesWon, 10, 250),
            Trophy("5", "Realm Champion", "👑", "Reach Hero Level 10", character.level, 10, 500)
        )
    }

    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 MILESTONE TROPHY VAULT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Gold
                )
                Text(
                    text = "${trophies.count { it.unlocked }} / ${trophies.size} Unlocked",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            trophies.forEach { trophy ->
                val claimed = claimedIds.contains(trophy.id)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (trophy.unlocked) MysticPurple.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (trophy.unlocked) Gold.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (trophy.unlocked) trophy.emoji else "🔒",
                        fontSize = 28.sp
                    )

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = trophy.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (trophy.unlocked) Gold else Color.Gray
                        )
                        Text(
                            text = trophy.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        BarMeter(
                            label = "${trophy.current} / ${trophy.target}",
                            valueText = "${trophy.current}/${trophy.target}",
                            progress = trophy.progressFraction,
                            color = if (trophy.unlocked) Gold else Color.Gray
                        )
                    }

                    if (trophy.unlocked) {
                        if (claimed) {
                            Text(
                                "CLAIMED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Gray
                            )
                        } else {
                            Button(
                                onClick = {
                                    AudioEffects.playLootDrop()
                                    HapticEffects.performLevelUp(haptic, context)
                                    onClaimReward(trophy)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+${trophy.rewardGold} 💰", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}
