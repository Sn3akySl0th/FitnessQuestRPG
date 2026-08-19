package com.fitnessquest.rpg.domain

/**
 * First-pass gear traits that roll on high-rarity gear (Rare, Epic, Legendary)
 * and provide tactical combat perks in [BattleEngine].
 */
enum class GearTrait(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String,
    val minRarity: GearRarity = GearRarity.RARE
) {
    VAMPIRIC(
        id = "VAMPIRIC",
        displayName = "Vampiric",
        emoji = "🩸",
        description = "Leeches 12% of attack damage as health recovery.",
        minRarity = GearRarity.RARE
    ),
    THORNS(
        id = "THORNS",
        displayName = "Thorns",
        emoji = "🌵",
        description = "Reflects 15% of unblocked damage back at the attacker.",
        minRarity = GearRarity.RARE
    ),
    SWIFTFOOT(
        id = "SWIFTFOOT",
        displayName = "Swiftfoot",
        emoji = "💨",
        description = "Grants +10% dodge chance in combat.",
        minRarity = GearRarity.RARE
    ),
    BERSERK(
        id = "BERSERK",
        displayName = "Berserk",
        emoji = "🔥",
        description = "Deals +20% damage when hero HP drops below 50%.",
        minRarity = GearRarity.EPIC
    ),
    EXECUTIONER(
        id = "EXECUTIONER",
        displayName = "Executioner",
        emoji = "⚔️",
        description = "Deals +25% bonus damage against enemies below 30% HP.",
        minRarity = GearRarity.EPIC
    ),
    WARDED(
        id = "WARDED",
        displayName = "Warded",
        emoji = "🛡️",
        description = "Reduces incoming critical hit damage by 25%.",
        minRarity = GearRarity.LEGENDARY
    ),
    MOMENTUM(
        id = "MOMENTUM",
        displayName = "Momentum",
        emoji = "⚡",
        description = "Gains +5% damage per consecutive hit landed (up to +25%).",
        minRarity = GearRarity.RARE
    ),
    VENOMOUS(
        id = "VENOMOUS",
        displayName = "Venomous",
        emoji = "🧪",
        description = "Attacks afflict enemies with poison dealing 10% ATK damage for 3 turns.",
        minRarity = GearRarity.RARE
    ),
    COLOSSAL_IMPACT(
        id = "COLOSSAL_IMPACT",
        displayName = "Colossal Impact",
        emoji = "🔨",
        description = "Attacks have a 15% chance to stagger and interrupt the opponent.",
        minRarity = GearRarity.EPIC
    ),
    FORTUNE_SEEKER(
        id = "FORTUNE_SEEKER",
        displayName = "Fortune Seeker",
        emoji = "💰",
        description = "Grants +20% bonus gold and crafting material drops from victories.",
        minRarity = GearRarity.RARE
    ),
    SCHOLAR(
        id = "SCHOLAR",
        displayName = "Scholar",
        emoji = "📜",
        description = "Grants +15% bonus XP from battles and workouts.",
        minRarity = GearRarity.RARE
    ),
    SECOND_WIND(
        id = "SECOND_WIND",
        displayName = "Second Wind",
        emoji = "💫",
        description = "Once per battle, fatal blows leave you at 1 HP and recover 20% max HP.",
        minRarity = GearRarity.LEGENDARY
    );


    companion object {
        fun fromId(id: String): GearTrait? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

        fun parseTraits(csv: String?): List<GearTrait> {
            if (csv.isNullOrBlank()) return emptyList()
            return csv.split(",")
                .mapNotNull { fromId(it) }
        }

        fun rollTraitsForRarity(rarity: GearRarity, rng: kotlin.random.Random = kotlin.random.Random.Default): List<GearTrait> {
            val eligible = entries.filter { rarity.ordinal >= it.minRarity.ordinal }
            if (eligible.isEmpty()) return emptyList()

            val count = when (rarity) {
                GearRarity.COMMON, GearRarity.UNCOMMON -> 0
                GearRarity.RARE -> if (rng.nextInt(100) < 30) 1 else 0
                GearRarity.EPIC -> if (rng.nextInt(100) < 70) 1 else 0
                GearRarity.LEGENDARY -> if (rng.nextInt(100) < 40) 2 else 1
                GearRarity.MYTHIC -> 2
            }

            if (count == 0) return emptyList()
            return eligible.shuffled(rng).take(count)
        }
    }
}
