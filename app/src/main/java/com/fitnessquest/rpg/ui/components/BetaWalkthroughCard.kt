package com.fitnessquest.rpg.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.domain.Consumables
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREF_BETA_FEEDBACK_GIVEN = "pref_beta_feedback_given"
private const val PREF_BETA_REWARD_CLAIMED = "pref_beta_pioneer_reward_claimed"

@Composable
fun BetaWalkthroughCard(
    character: CharacterEntity?,
    onOpenFeedback: () -> Unit,
    onClaimPioneerReward: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("fitnessrpg_user_prefs", Context.MODE_PRIVATE) }

    var expanded by remember { mutableStateOf(true) }
    var rewardClaimed by remember { mutableStateOf(prefs.getBoolean(PREF_BETA_REWARD_CLAIMED, false)) }
    val feedbackGiven by remember { mutableStateOf(prefs.getBoolean(PREF_BETA_FEEDBACK_GIVEN, false)) }

    if (character == null) return

    val m1Completed = character.sessionsCompleted >= 1
    val m2Completed = character.weaponId != null || character.chestId != null || character.headId != null || character.trinketId != null
    val m3Completed = character.battlesWon >= 1 && character.sessionsCompleted >= 3
    val m4Completed = feedbackGiven

    val completedCount = listOf(m1Completed, m2Completed, m3Completed, m4Completed).count { it }
    val allCompleted = completedCount == 4

    val gradientBrush = Brush.horizontalGradient(
        listOf(
            Color(0xFF1E2235),
            Color(0xFF281F3E)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (allCompleted && !rewardClaimed) Gold else Gold.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = NightSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🛡️", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "Closed Beta Tester Quest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                        Text(
                            text = if (rewardClaimed) "All 4 Missions Complete · Pioneer Cache Claimed" else "$completedCount of 4 Missions Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Mission 1
                    MissionRow(
                        title = "1. First Expedition",
                        description = "Log your first workout (Strength or Cardio) & reveal loot",
                        completed = m1Completed
                    )

                    // Mission 2
                    MissionRow(
                        title = "2. Gear Up",
                        description = "Equip rewarded gear onto your Hero avatar",
                        completed = m2Completed
                    )

                    // Mission 3
                    MissionRow(
                        title = "3. Realm Conquest",
                        description = "Slay a Biome Boss & complete 3 workouts (${character.sessionsCompleted.coerceAtMost(3)}/3)",
                        completed = m3Completed
                    )

                    // Mission 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            MissionRow(
                                title = "4. Tester Debrief",
                                description = "Share your impressions in the Feedback Hub",
                                completed = m4Completed
                            )
                        }
                        if (!m4Completed) {
                            OutlinedButton(
                                onClick = onOpenFeedback,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Feedback 💬", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Reward Claim Area
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    if (allCompleted && !rewardClaimed) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Gold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Gold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "🌟 PIONEER'S CACHE UNLOCKED!",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Gold
                                )
                                Text(
                                    "You earned: Pioneer's Amulet (Relic Trinket) + 3x Streak Freezes + 500 Gold & XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            onClaimPioneerReward()
                                            prefs.edit().putBoolean(PREF_BETA_REWARD_CLAIMED, true).apply()
                                            rewardClaimed = true
                                            AudioEffects.playLevelUp()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightSurface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Claim Exclusive Reward 🎁", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (rewardClaimed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("✨", fontSize = 16.sp)
                            Text(
                                "Pioneer's Amulet equipped / added to inventory. Thank you for testing FitQuest!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
                            )
                        }
                    } else {
                        Text(
                            "🎁 Reward upon completion: Exclusive Pioneer's Amulet + Trophy + 3x Streak Freezes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold.copy(alpha = 0.8f)
                        )
                    }

                    // Teaser for upcoming beta chapters & Lifetime Premium reward
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF131722),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("👑", fontSize = 16.sp)
                                Text(
                                    "More Beta Expeditions To Come!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold
                                )
                            }
                            Text(
                                "Your character progress is permanently preserved across all beta updates. Testers who complete the beta campaign will unlock Free Lifetime Premium when FitQuest officially launches!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionRow(
    title: String,
    description: String,
    completed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (completed) "Completed" else "Incomplete",
            tint = if (completed) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (completed) Color.White else Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

fun noteBetaFeedbackSubmitted(context: Context) {
    val prefs = context.getSharedPreferences("fitnessrpg_user_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean(PREF_BETA_FEEDBACK_GIVEN, true).apply()
}
