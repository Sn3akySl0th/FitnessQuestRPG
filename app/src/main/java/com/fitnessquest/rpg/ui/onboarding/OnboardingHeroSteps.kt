package com.fitnessquest.rpg.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitnessquest.rpg.data.auth.UsernameService
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.ui.components.CharacterAvatar
import com.fitnessquest.rpg.ui.components.SectionCard
import com.fitnessquest.rpg.ui.rememberDockContentPadding

/** Full-screen username claim shown after class pick. */
@Composable
fun UsernameSetupStep(
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onClaim: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val preview = UsernameService.MIN_LEN..UsernameService.MAX_LEN
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Choose your username", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(8.dp))
        Text(
            "This is how allies find you on the leaderboard and in parties. It must be unique.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            singleLine = true,
            label = { Text("Username") },
            isError = error != null,
            supportingText = {
                Text(error ?: "${preview.first}–${preview.last} characters · start with a letter")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = { onClaim(name) },
            enabled = !busy && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "Claiming…" else "Claim username")
        }
    }
}

/** Full-screen class selection. */
@Composable
fun ClassPickerStep(isPremium: Boolean, onPick: (CharacterClass) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberDockContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Choose your path", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Every workout makes you stronger. Who will you become?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (!isPremium) {
                    Text(
                        "✦ marks Premium classes — redeem a code in Settings to unlock.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        items(CharacterClass.entries.size) { index ->
            val c = CharacterClass.entries[index]
            val locked = c.requiresPremium && !isPremium
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CharacterAvatar(clazz = c, modifier = Modifier.width(84.dp), showClassOutfit = true)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (c.requiresPremium) "${c.emoji} ${c.label} ✦" else "${c.emoji} ${c.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            c.blurb,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            c.bonusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Button(
                    onClick = { if (!locked) onPick(c) },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            locked -> "Premium required"
                            else -> "Become a ${c.label}"
                        }
                    )
                }
            }
        }
    }
}
