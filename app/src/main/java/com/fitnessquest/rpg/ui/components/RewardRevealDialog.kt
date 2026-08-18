package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitnessquest.rpg.domain.Reward
import com.fitnessquest.rpg.domain.RewardBatch
import com.fitnessquest.rpg.domain.RewardSource
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.delay

@Composable
fun RewardRevealDialog(
    batch: RewardBatch,
    onDismiss: () -> Unit
) {
    val isChest = batch.source == RewardSource.CHEST_OPENING
    var chestUnlocked by remember(batch.timestamp, batch.source) { androidx.compose.runtime.mutableStateOf(!isChest) }
    var revealedIndex by remember(batch.timestamp, batch.source, batch.rewards.size) { mutableIntStateOf(if (isChest) -1 else 0) }
    val isFullyRevealed = chestUnlocked && revealedIndex >= batch.rewards.size - 1

    // Chest shake animation
    val chestScale = remember { Animatable(0.8f) }
    val chestRotation = remember { Animatable(0f) }

    LaunchedEffect(batch.timestamp, batch.source, batch.rewards.size, chestUnlocked) {
        if (!chestUnlocked) {
            // Chest opening sequence
            chestScale.animateTo(1.1f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
            repeat(3) {
                chestRotation.animateTo(8f, spring(stiffness = Spring.StiffnessHigh))
                chestRotation.animateTo(-8f, spring(stiffness = Spring.StiffnessHigh))
            }
            chestRotation.animateTo(0f)
            delay(250)
            chestUnlocked = true
        }

        if (chestUnlocked) {
            delay(200)
            batch.rewards.indices.forEach { index ->
                revealedIndex = index
                delay(350)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (isFullyRevealed) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(enabled = !isFullyRevealed) {
                    if (!chestUnlocked) {
                        chestUnlocked = true
                    }
                    revealedIndex = batch.rewards.size - 1
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!chestUnlocked) "Unlocking Chest..." else rewardTitle(batch.source),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Gold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                if (!chestUnlocked) {
                    // Animated Chest Container
                    Box(
                        modifier = Modifier
                            .scale(chestScale.value)
                            .padding(24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(2.dp, Gold, RoundedCornerShape(24.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.CardGiftcard,
                                contentDescription = "Locked Treasure Chest",
                                tint = Gold,
                                modifier = Modifier
                                    .size(72.dp)
                                    .scale(chestScale.value)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tap to open",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gold.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(batch.rewards.take((revealedIndex + 1).coerceAtLeast(0))) { reward ->
                            RewardItemCard(reward)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (isFullyRevealed) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text(
                            "Collect All",
                            color = NightBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            if (isFullyRevealed) {
                ConfettiOverlay(
                    modifier = Modifier.fillMaxSize(),
                    trigger = isFullyRevealed
                )
            }
        }
    }
}

private fun rewardTitle(source: RewardSource): String = when (source) {
    RewardSource.WORKOUT -> "Quest Completed!"
    RewardSource.BATTLE -> "Victory Spoils!"
    RewardSource.BOSS -> "Boss Conquered!"
    RewardSource.CHEST_OPENING -> "Loot Found!"
    RewardSource.OFFLINE_IDLE -> "Patrol Rewards"
    RewardSource.FORGE -> "Forge Results!"
    RewardSource.GUILD_RAID -> "Guild Spoils!"
    RewardSource.CAMPAIGN_GOAL -> "Campaign Rewards!"
    RewardSource.ACHIEVEMENT -> "Achievement Unlocked!"
    RewardSource.DATA_IMPORT -> "Archives Restored!"
}

@Composable
private fun RewardItemCard(reward: Reward) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(reward) {
        scale.snapTo(0f)
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    val (badge, label, color) = rewardPresentation(reward)

    Box(
        modifier = Modifier
            .scale(scale.value)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = badge,
                fontSize = if (badge.length <= 2 || reward is Reward.Gear || reward is Reward.Stackable) 32.sp else 18.sp,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun rewardPresentation(reward: Reward): Triple<String, String, Color> = when (reward) {
    is Reward.Gold -> Triple("\uD83D\uDCB0", "+${reward.amount}", Gold)
    is Reward.Xp -> Triple("\u2B50", "+${reward.amount} XP", Color(0xFF9C7BE3))
    is Reward.Energy -> Triple("\u26A1", "+${reward.amount}", Color(0xFF4ADE80))
    is Reward.XpBoost -> Triple("\u2728", "+${reward.amount} XP Boost", Color(0xFF4A6FD8))
    is Reward.Gear -> Triple(
        reward.item.emoji,
        if (reward.rarity != com.fitnessquest.rpg.domain.GearRarity.COMMON) "[${reward.rarity.displayName}] ${reward.item.name}" else reward.item.name,
        Color(reward.rarity.colorHex)
    )
    is Reward.Stackable -> Triple(reward.item.emoji, "${reward.item.name} x${reward.quantity}", Color.White)
    is Reward.LevelUp -> Triple("LV", "Level ${reward.newLevel}!", Gold)
    is Reward.NewPr -> Triple("PR", "New Record!", Color(0xFFF6AD55))
    is Reward.BiomeUnlocked -> Triple("MAP", reward.biomeLabel, Color(0xFF6BC96B))
    is Reward.TitleUnlocked -> Triple("TITLE", reward.title, Gold)
    is Reward.SkillPoint -> Triple("SP", "Skill Point", Color(0xFF9C7BE3))
}
