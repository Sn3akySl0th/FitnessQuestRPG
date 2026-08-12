package com.fitnessquest.rpg.domain

/**
 * Fantasy lineage for the hero avatar. Free players stay Human;
 * other races are Premium cosmetics (and unlock matching default looks).
 */
enum class CharacterRace(
    val label: String,
    val emoji: String,
    val blurb: String,
    val requiresPremium: Boolean
) {
    HUMAN(
        label = "Human",
        emoji = "🧑",
        blurb = "Versatile heroes of the realms. The default look.",
        requiresPremium = false
    ),
    ELF(
        label = "Elf",
        emoji = "🧝",
        blurb = "Graceful and sharp-eared. Keen eyes for the long hunt.",
        requiresPremium = true
    ),
    ORC(
        label = "Orc",
        emoji = "👹",
        blurb = "Tusks and muscle. Built for the front line.",
        requiresPremium = true
    ),
    UNDEAD(
        label = "Undead",
        emoji = "💀",
        blurb = "Pale skin and glowing gaze. Death is just another workout.",
        requiresPremium = true
    ),
    DWARF(
        label = "Dwarf",
        emoji = "🧔",
        blurb = "Stout and stubborn. Short in stature, long on grit.",
        requiresPremium = true
    );

    companion object {
        fun fromStored(raw: String?): CharacterRace =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: HUMAN
    }
}
