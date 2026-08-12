package com.fitnessquest.rpg.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CharacterRace

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarCustomizationDialog(
    character: CharacterEntity,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onClassChange: (CharacterClass) -> Unit = {},
    onSave: (
        skinColor: Long,
        hairColor: Long,
        underwearColor: Long,
        eyeColor: Long,
        hairStyle: String,
        gender: String,
        braColor: Long,
        race: String,
    ) -> Unit
) {
    var skinColor by remember { mutableStateOf(Color(character.skinColor.toInt())) }
    var hairColor by remember { mutableStateOf(Color(character.hairColor.toInt())) }
    var underwearColor by remember { mutableStateOf(Color(character.underwearColor.toInt())) }
    var eyeColor by remember { mutableStateOf(Color(character.eyeColor.toInt())) }
    var hairStyle by remember { mutableStateOf(character.hairStyle) }
    var gender by remember { mutableStateOf(character.gender) }
    var braColor by remember { mutableStateOf(Color(character.braColor.toInt())) }
    var race by remember { mutableStateOf(CharacterRace.fromStored(character.race)) }

    val currentAppearance = AvatarAppearance(
        skinColor, hairColor, underwearColor, eyeColor, hairStyle, gender, braColor, race
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Customize Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    if (isPremium) "Premium active — full RGB, fantasy races & styles unlocked"
                    else "Free color picks below. Premium unlocks RGB sliders, fantasy races & extra hairstyles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CharacterAvatar(
                        clazz = character.characterClass ?: CharacterClass.WARRIOR,
                        gear = emptyMap(),
                        appearance = currentAppearance,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Body Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = gender == "male",
                                onClick = { gender = "male" },
                                label = { Text("Male") }
                            )
                            FilterChip(
                                selected = gender == "female",
                                onClick = { gender = "female" },
                                label = { Text("Female") }
                            )
                        }
                    }

                    Column {
                        Text("Class", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CharacterClass.entries.forEach { option ->
                                val locked = option.requiresPremium && !isPremium
                                FilterChip(
                                    selected = character.characterClass == option,
                                    onClick = { if (!locked) onClassChange(option) },
                                    enabled = !locked,
                                    label = {
                                        Text(
                                            if (locked) "${option.emoji} ${option.label} ✦"
                                            else "${option.emoji} ${option.label}"
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Race", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CharacterRace.entries.forEach { option ->
                                val locked = option.requiresPremium && !isPremium
                                FilterChip(
                                    selected = race == option,
                                    onClick = { if (!locked) race = option },
                                    enabled = !locked,
                                    label = {
                                        Text(
                                            if (locked) "${option.emoji} ${option.label} ✦"
                                            else "${option.emoji} ${option.label}"
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            race.blurb,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isPremium) {
                        ColorSliderGroup("Skin Color", skinColor) { skinColor = it }
                        ColorSliderGroup("Hair Color", hairColor) { hairColor = it }
                        ColorSliderGroup("Underwear Color", underwearColor) { underwearColor = it }
                        if (gender == "female") {
                            ColorSliderGroup("Bra Color", braColor) { braColor = it }
                        }
                        ColorSliderGroup("Eye Color", eyeColor) { eyeColor = it }
                    } else {
                        ColorSwatchGroup(
                            "Skin Color",
                            skinColor,
                            listOf(Color(0xFFE3B187), Color(0xFFC68642), Color(0xFF8D5524), Color(0xFF3C2E28))
                        ) { skinColor = it }
                        ColorSwatchGroup(
                            "Hair Color",
                            hairColor,
                            listOf(Color(0xFF6B4A32), Color(0xFF1E1A26), Color(0xFFD6C4A6), Color(0xFF9E3C3C))
                        ) { hairColor = it }
                        ColorSwatchGroup(
                            "Underwear Color",
                            underwearColor,
                            listOf(Color(0xFF4E4656), Color(0xFF8B2B2B), Color(0xFF2C568B), Color(0xFF336B3C))
                        ) { underwearColor = it }
                        if (gender == "female") {
                            ColorSwatchGroup(
                                "Bra Color",
                                braColor,
                                listOf(Color(0xFF4E4656), Color(0xFF8B2B2B), Color(0xFF2C568B), Color(0xFF336B3C))
                            ) { braColor = it }
                        }
                        ColorSwatchGroup(
                            "Eye Color",
                            eyeColor,
                            listOf(Color(0xFF2A2233), Color(0xFF4A72B2), Color(0xFF4C7B43), Color(0xFF7A4A28))
                        ) { eyeColor = it }
                        Text(
                            "Unlock full RGB sliders with Premium (Settings → Redeem code).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text("Hair Style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val styles = listOf("short", "long", "ponytail", "bald", "afro", "spiky")
                        val freeStyles = listOf("short", "bald")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (style in styles) {
                                val isLocked = !isPremium && (style !in freeStyles)
                                FilterChip(
                                    selected = hairStyle == style,
                                    onClick = { if (!isLocked) hairStyle = style },
                                    label = {
                                        Text(
                                            if (isLocked) {
                                                style.replaceFirstChar { it.uppercase() } + " ✦"
                                            } else {
                                                style.replaceFirstChar { it.uppercase() }
                                            }
                                        )
                                    },
                                    enabled = !isLocked
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                skinColor.toArgb().toLong() and 0xFFFFFFFFL,
                                hairColor.toArgb().toLong() and 0xFFFFFFFFL,
                                underwearColor.toArgb().toLong() and 0xFFFFFFFFL,
                                eyeColor.toArgb().toLong() and 0xFFFFFFFFL,
                                hairStyle,
                                gender,
                                braColor.toArgb().toLong() and 0xFFFFFFFFL,
                                race.name
                            )
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun ColorSliderGroup(label: String, color: Color, onColorChange: (Color) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
            )
        }
        var r by remember(color) { mutableFloatStateOf(color.red) }
        var g by remember(color) { mutableFloatStateOf(color.green) }
        var b by remember(color) { mutableFloatStateOf(color.blue) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "R",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252),
                modifier = Modifier.width(16.dp)
            )
            Slider(
                value = r,
                onValueChange = { r = it; onColorChange(Color(r, g, b)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF5252),
                    activeTrackColor = Color(0xFFFF5252)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "G",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF69F0AE),
                modifier = Modifier.width(16.dp)
            )
            Slider(
                value = g,
                onValueChange = { g = it; onColorChange(Color(r, g, b)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF69F0AE),
                    activeTrackColor = Color(0xFF69F0AE)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "B",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF40C4FF),
                modifier = Modifier.width(16.dp)
            )
            Slider(
                value = b,
                onValueChange = { b = it; onColorChange(Color(r, g, b)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF40C4FF),
                    activeTrackColor = Color(0xFF40C4FF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            )
        }
    }
}

@Composable
fun ColorSwatchGroup(label: String, color: Color, options: List<Color>, onColorChange: (Color) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(option)
                        .border(
                            width = if (color == option) 2.dp else 1.dp,
                            color = if (color == option) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(option) }
                )
            }
        }
    }
}
