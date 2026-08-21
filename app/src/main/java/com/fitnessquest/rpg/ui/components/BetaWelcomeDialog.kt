package com.fitnessquest.rpg.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitnessquest.rpg.ui.theme.Gold
import com.fitnessquest.rpg.ui.theme.NightBg
import com.fitnessquest.rpg.ui.theme.NightSurface

const val PREF_BETA_WELCOME_SHOWN = "pref_beta_welcome_shown"

@Composable
fun BetaWelcomeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitnessrpg_user_prefs", Context.MODE_PRIVATE) }
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    fun completeAndDismiss() {
        prefs.edit().putBoolean(PREF_BETA_WELCOME_SHOWN, true).apply()
        onDismiss()
    }

    Dialog(
        onDismissRequest = { completeAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = NightBg,
            border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header with Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLOSED BETA GUIDE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Gold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentStep) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == currentStep) Gold else Color.White.copy(alpha = 0.25f))
                            )
                        }
                    }

                    TextButton(
                        onClick = { completeAndDismiss() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Step Body Animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "BetaWelcomeStep"
                    ) { step ->
                        when (step) {
                            0 -> StepWelcome()
                            1 -> StepCoreLoop()
                            2 -> StepTesterQuest()
                            3 -> StepFeedback()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBg)
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { completeAndDismiss() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBg),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Start Adventure ⚔️", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("🛡️", fontSize = 54.sp)
        Text(
            text = "Welcome to Fitness Quest RPG!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Gold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You are among our elite first cohort of Closed Beta Pioneers.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureBullet(
                    emoji = "🏋️",
                    title = "Real Fitness = RPG Power",
                    description = "Every weight lifted, cardio run logged, or step taken earns real XP, gold, and stat points for your hero."
                )
                FeatureBullet(
                    emoji = "⚔️",
                    title = "Tactical Monster Battles",
                    description = "Level up, equip enchanted loot, and defeat Biome Bosses to unlock new realms."
                )
                FeatureBullet(
                    emoji = "🌟",
                    title = "Exclusive Beta Rewards",
                    description = "Complete the tester quest to earn the Pioneer's Amulet & qualify for Free Lifetime Premium."
                )
            }
        }
    }
}

@Composable
private fun StepCoreLoop() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🧭", fontSize = 48.sp)
        Text(
            text = "How to Play & Progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Gold,
            textAlign = TextAlign.Center
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoopCard(
                stepNum = "1",
                emoji = "🏰",
                title = "Quest Hub (Home)",
                description = "Your command post. Check daily bounties, track your active quest, and jump straight into workouts."
            )
            LoopCard(
                stepNum = "2",
                emoji = "🏋️",
                title = "Train & Log Workouts",
                description = "Pick your routine or generate AI workouts. Log your sets & reps to earn guaranteed gear & XP."
            )
            LoopCard(
                stepNum = "3",
                emoji = "🛡️",
                title = "Hero & Gear Loadout",
                description = "Equip weapons and armor across 7 slots. Look out for the 'Gear Upgrade' notification on home!"
            )
            LoopCard(
                stepNum = "4",
                emoji = "⚔️",
                title = "Battle Bosses",
                description = "Spend Energy in the Battle tab to fight monsters and claim boss chests."
            )
        }
    }
}

@Composable
private fun StepTesterQuest() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("🏆", fontSize = 48.sp)
        Text(
            text = "Closed Beta Tester Quest",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Gold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Check the quest card at the top of your Home screen to complete these 4 milestone missions:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NightSurface,
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestMissionItem(num = "1", title = "First Expedition", desc = "Log your 1st workout & claim guaranteed loot")
                QuestMissionItem(num = "2", title = "Gear Up", desc = "Equip your new armor or weapon in Hero tab")
                QuestMissionItem(num = "3", title = "Realm Conquest", desc = "Slay a Biome Boss & log 3 total workouts")
                QuestMissionItem(num = "4", title = "Tester Debrief", desc = "Submit feedback in the Feedback Hub")
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Gold.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Gold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎁", fontSize = 24.sp)
                Column {
                    Text("Pioneer's Cache Reward", fontWeight = FontWeight.Bold, color = Gold, style = MaterialTheme.typography.labelLarge)
                    Text("Pioneer's Amulet + 3x Streak Freezes + 500 Gold & XP upon completion!", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun StepFeedback() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("💬", fontSize = 48.sp)
        Text(
            text = "Your Feedback Shapes Fitness Quest RPG",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Gold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Encounter a glitch? Have a great balance idea? Want more exercises or monsters?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF192238),
            border = BorderStroke(1.dp, Color(0xFF3F51B5).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📸", fontSize = 20.sp)
                    Text("Screenshot Attachments", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "You can attach screenshots directly to your feedback so we can immediately diagnose and resolve issues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 20.sp)
                    Text("Direct Developer Inbox", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "All feedback reports go directly to our engineering team with anonymized diagnostics for swift turnaround.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun FeatureBullet(emoji: String, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun LoopCard(stepNum: String, emoji: String, title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNum, fontWeight = FontWeight.Black, color = Gold, fontSize = 12.sp)
            }
            Text(emoji, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text(description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun QuestMissionItem(num: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$num.", fontWeight = FontWeight.Black, color = Gold, fontSize = 12.sp)
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.65f))
        }
    }
}
