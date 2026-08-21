package com.fitnessquest.rpg.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.ui.theme.Gold
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class FeedbackCategory(val label: String, val emoji: String) {
    BUG("Bug / Glitch", "🐞"),
    SUGGESTION("Feature Idea", "💡"),
    USABILITY("UI & Controls", "🎨"),
    PRAISE("Praise / Fun", "⭐")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BetaFeedbackDialog(
    character: CharacterEntity?,
    onDismiss: () -> Unit,
    onFeedbackSubmitted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf(FeedbackCategory.BUG) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Closed Beta Feedback",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (!submitting) onDismiss() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                if (submitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🛡️", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Thank you, Vanguard!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your report has been dispatched to the dev team. Your testing directly shapes the future of FitnessQuestRPG!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = {
                                onFeedbackSubmitted?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Return to Quest")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category selection
                        Text(
                            "What would you like to report?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeedbackCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text("${cat.emoji} ${cat.label}") }
                                )
                            }
                        }

                        // Star Rating
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "How is your overall experience so far?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "$star stars",
                                        tint = if (star <= rating) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { rating = star }
                                    )
                                }
                            }
                        }

                        // Feedback details
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Details & Observations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                placeholder = {
                                    Text(
                                        when (category) {
                                            FeedbackCategory.BUG -> "What went wrong? Steps to reproduce, what you expected vs what happened..."
                                            FeedbackCategory.SUGGESTION -> "What would make this feature or workout mechanic more enjoyable?"
                                            FeedbackCategory.USABILITY -> "What screen or button felt awkward, cramped, or hard to use?"
                                            FeedbackCategory.PRAISE -> "What's your favorite part of the game so far?"
                                        }
                                    )
                                },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Screenshot Attachment Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Attach Screenshot (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (selectedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Selected Screenshot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { selectedImageUri = null },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove Screenshot", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pick screenshot from gallery")
                                }
                            }
                        }

                        // Auto-attached metadata preview
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("📋 Auto-attached diagnostic telemetry:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Gold)
                                Text("• Build: FitQuest v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall)
                                Text("• Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.labelSmall)
                                if (character != null) {
                                    Text("• Hero: ${character.name} (Lv ${character.level} ${character.characterClass?.label ?: "Hero"})", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                submitting = true
                                scope.launch {
                                    val ok = submitFeedback(
                                        context = context,
                                        category = category,
                                        rating = rating,
                                        comment = comment,
                                        imageUri = selectedImageUri,
                                        character = character
                                    )
                                    submitting = false
                                    if (ok) {
                                        submitted = true
                                    } else {
                                        Toast.makeText(context, "Dispatched via backup mail handler.", Toast.LENGTH_SHORT).show()
                                        submitted = true
                                    }
                                }
                            },
                            enabled = !submitting && comment.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Submitting...")
                            } else {
                                Text("Submit Beta Feedback")
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun submitFeedback(
    context: Context,
    category: FeedbackCategory,
    rating: Int,
    comment: String,
    imageUri: Uri?,
    character: CharacterEntity?
): Boolean = withContext(Dispatchers.IO) {
    try {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: "anonymous"
        val email = auth.currentUser?.email ?: "guest"

        val payload = hashMapOf<String, Any?>(
            "category" to category.name,
            "rating" to rating,
            "comment" to comment.trim(),
            "uid" to uid,
            "email" to email,
            "heroClass" to (character?.characterClass?.name ?: "UNKNOWN"),
            "heroLevel" to (character?.level ?: 1),
            "deviceManufacturer" to Build.MANUFACTURER,
            "deviceModel" to Build.MODEL,
            "androidRelease" to Build.VERSION.RELEASE,
            "androidSdk" to Build.VERSION.SDK_INT,
            "appVersionName" to BuildConfig.VERSION_NAME,
            "appVersionCode" to BuildConfig.VERSION_CODE,
            "hasScreenshot" to (imageUri != null),
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("beta_feedback")
            .add(payload)
            .await()
        true
    } catch (e: Exception) {
        // Fallback: Launch Send Email intent with diagnostic log if Firestore fails / offline
        withContext(Dispatchers.Main) {
            val mailBody = buildString {
                appendLine("=== FITQUEST BETA FEEDBACK ===")
                appendLine("Category: ${category.name}")
                appendLine("Rating: $rating/5")
                appendLine("Comment: $comment")
                appendLine()
                appendLine("=== DIAGNOSTIC METADATA ===")
                appendLine("App: FitQuest v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
                if (character != null) {
                    appendLine("Hero: ${character.name} Lv ${character.level} ${character.characterClass?.label}")
                }
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (imageUri != null) "image/*" else "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@fitnessquestrpg.com"))
                putExtra(Intent.EXTRA_SUBJECT, "[Closed Beta Feedback] ${category.label}")
                putExtra(Intent.EXTRA_TEXT, mailBody)
                if (imageUri != null) {
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
        }
        false
    }
}
