package com.fitnessquest.rpg.domain

/**
 * Explorable regions. Traveling between biomes is powered by real cardio:
 * kilometers logged during workouts fill the travel meter.
 */
enum class Biome(
    val label: String,
    val emoji: String,
    val blurb: String,
    val levelRequired: Int,
    val travelKm: Double,
    val colorA: Long,
    val colorB: Long,
) {
    MEADOWLANDS(
        "Meadowlands", "\uD83C\uDF3E",
        "Rolling golden fields where every hero's journey begins.",
        1, 0.0, 0xFF7A9A3A, 0xFF3E5A24
    ),
    DARKWOOD(
        "Darkwood", "\uD83C\uDF32",
        "An ancient forest where the trees whisper your rep count.",
        3, 3.0, 0xFF2E5A3A, 0xFF16301F
    ),
    CRYSTAL_CAVES(
        "Crystal Caves", "\uD83D\uDC8E",
        "Glittering caverns that echo with the clang of dropped dumbbells.",
        6, 5.0, 0xFF4A6FA8, 0xFF232F5A
    ),
    EMBER_PEAKS(
        "Ember Peaks", "\uD83C\uDF0B",
        "Volcanic crags. The ultimate hot yoga destination.",
        10, 8.0, 0xFFB85A2E, 0xFF6B1F16
    ),
    FROZEN_WASTES(
        "Frozen Wastes", "\u2744\uFE0F",
        "Endless ice. Great for post-workout recovery plunges.",
        14, 12.0, 0xFF6FA8C8, 0xFF2A4A6B
    ),
    SHADOWFEN(
        "Shadowfen", "\uD83C\uDF2B\uFE0F",
        "A haunted marsh where burnout itself lurks. The final frontier.",
        18, 16.0, 0xFF5A4A6B, 0xFF1E1828
    );

    companion object {
        fun fromName(name: String?): Biome =
            entries.find { it.name == name } ?: MEADOWLANDS
    }
}
