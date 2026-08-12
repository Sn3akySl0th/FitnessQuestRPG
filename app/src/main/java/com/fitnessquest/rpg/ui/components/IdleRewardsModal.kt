package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun IdleRewardsModal(
    character: CharacterEntity,
    onClaim: () -> Unit
) {
    var isOpening by remember { mutableStateOf(value = false) }
    var isOpened by remember { mutableStateOf(value = false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val chestScale by animateFloatAsState(
        targetValue = if (isOpening) 1.2f else if (isOpened) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chestScale"
    )

    Dialog(onDismissRequest = { if (isOpened) onClaim() }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NightBg.copy(alpha = 0.95f),
            tonalElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = if (isOpened) "Rewards Claimed!" else "Welcome Back!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "While you were away, your hero patrolled the ${character.currentBiome}...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(Modifier.height(32.dp))

                    if (!isOpened) {
                        // Chest UI
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .scale(chestScale)
                                .screenShake(shakeTrigger)
                                .clickable(enabled = !isOpening) {
                                    isOpening = true
                                    shakeTrigger++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Simple Chest representation with Emojis
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\uD83C\uDF81", fontSize = 100.sp) // Wrapped Gift for now, or Chest if available
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Tap to open!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    LaunchedEffect(isOpening) {
                        if (isOpening) {
                            delay(800.milliseconds)
                            isOpened = true
                            isOpening = false
                        }
                    }

                    AnimatedVisibility(
                        visible = isOpened,
                        enter = fadeIn(tween(500)) + expandVertically(tween(500))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Stats Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RewardRow("\u2694\uFE0F", "Monsters Defeated", character.idleKills.toString())
                                RewardRow("\uD83D\uDCB0", "Gold Found", "+${character.idleGold}", Gold)
                                RewardRow("\u2728", "Experience", "+${character.idleXp}", Color(0xFF6B9CFF))
                                
                                if (character.idleKills > 0) {
                                    RewardRow("\uD83C\uDF81", "Loot", "Reveal on claim", MaterialTheme.colorScheme.tertiary)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = onClaim,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Claim Rewards",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isOpened) {
                    ConfettiOverlay(
                        modifier = Modifier.fillMaxSize(),
                        trigger = isOpened,
                        pieces = 120
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardRow(emoji: String, label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = valueColor)
    }
}
