package com.fitnessquest.rpg.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.ClassSkill
import com.fitnessquest.rpg.domain.StatGains
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.MysticPurple
import com.fitnessquest.rpg.ui.theme.NightBg

/**
 * High-dopamine Level Up celebration modal.
 * Triggers audio fanfare, haptics, confetti particle burst, and spring animations.
 */
@Composable
fun LevelUpModal(
    newLevel: Int,
    clazz: CharacterClass,
    statGains: StatGains? = null,
    unlockedSkills: List<ClassSkill> = emptyList(),
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val badgeScale = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        AudioEffects.playLevelUp()
        HapticEffects.performLevelUp(haptic, context)
        badgeScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    brush = Brush.verticalGradient(listOf(NightBg, Color(0xFF1E1035))),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(Gold, MysticPurple, Gold)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfettiOverlay(pieces = 120, durationSec = 3.5f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LEVEL UP!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp
                    ),
                    color = Gold
                )

                // Level Badge
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(badgeScale.value),
                    shape = CircleShape,
                    color = MysticPurple.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, Gold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LEVEL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = newLevel.toString(),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                color = Gold
                            )
                        }
                    }
                }

                Text(
                    text = "Your ${clazz.label} grows in power!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (unlockedSkills.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MysticPurple.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .border(1.dp, Gold, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "NEW SKILL UNLOCKED!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Gold
                        )
                        unlockedSkills.forEach { skill ->
                            Text(
                                "${skill.emoji} ${skill.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                skill.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                statGains?.let { gains ->
                    if (gains.any) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "STAT INCREASES",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Gold
                            )
                            if (gains.strength > 0) Text("💪 Strength +${gains.strength}", color = Color.White)
                            if (gains.endurance > 0) Text("🏃 Endurance +${gains.endurance}", color = Color.White)
                            if (gains.agility > 0) Text("⚡ Agility +${gains.agility}", color = Color.White)
                            if (gains.willpower > 0) Text("🧘 Willpower +${gains.willpower}", color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "CLAIM POWER ⚡",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}
