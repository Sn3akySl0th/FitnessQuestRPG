package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.fitnessquest.rpg.data.guild.GuildMember
import com.fitnessquest.rpg.data.guild.GuildRaid
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.HealthRed
import com.fitnessquest.rpg.ui.theme.MysticPurple

/**
 * Live Guild Raid Boss Banner with real contributor rankings and claim CTA.
 */
@Composable
fun GuildRaidBossCard(
    raid: GuildRaid,
    members: List<GuildMember>,
    myUid: String?,
    onClaimLoot: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val rankings = remember(raid.damageByUid, members) {
        raid.damageByUid.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (uid, dmg) ->
                val name = members.find { it.uid == uid }?.name ?: "Hero"
                val emoji = members.find { it.uid == uid }?.classEmoji ?: "⚔️"
                Triple(emoji, name, dmg)
            }
    }
    val myDamage = myUid?.let { raid.damageByUid[it] } ?: 0L
    val alreadyClaimed = myUid != null && myUid in raid.claimedBy
    val hoursLeft = (raid.millisLeft / 3_600_000L).coerceAtLeast(0)
    val rewardHint = if (myDamage > 0L) {
        "🎁 CLAIM RAID REWARD (~80–120 gold + loot)"
    } else {
        "Deal damage in workouts to earn a claim"
    }

    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(raid.emoji, style = MaterialTheme.typography.headlineMedium)
                    Column {
                        Text(
                            text = raid.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Gold
                        )
                        Text(
                            text = if (raid.defeated) "VICTORY! Raid Defeated" else "WEEKLY GUILD RAID BOSS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (raid.defeated) Color(0xFF6BC96B) else HealthRed
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MysticPurple.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold)
                ) {
                    Text(
                        text = "⏱️ ${hoursLeft}h left",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White
                    )
                }
            }

            BarMeter(
                label = "Boss Health",
                valueText = "${raid.hp} / ${raid.maxHp} HP",
                progress = (raid.hp.toFloat() / raid.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f),
                color = HealthRed
            )

            if (raid.defeated) {
                if (alreadyClaimed) {
                    Text(
                        "CLAIMED — spoils already collected",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                } else {
                    Button(
                        onClick = {
                            AudioEffects.playLootDrop()
                            HapticEffects.performLevelUp(haptic, context)
                            onClaimLoot()
                        },
                        enabled = myDamage > 0L,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(rewardHint, fontWeight = FontWeight.Black)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Text(
                text = "⚔️ TOP CONTRIBUTORS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Gold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (rankings.isEmpty()) {
                    Text(
                        text = "No damage yet — finish workouts to strike the raid boss.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    rankings.forEachIndexed { index, (emoji, name, dmg) ->
                        Text(
                            text = "#${index + 1} $emoji $name — $dmg dmg",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    if (myUid != null) {
                        Text(
                            text = "You: $myDamage dmg",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Gold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
