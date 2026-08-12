package com.fitnessquest.rpg.domain

/**
 * A class battle skill. Every class knows its first skill from level 1 and
 * unlocks stronger ones by leveling - the reward for consistent training.
 */
data class ClassSkill(
    val name: String,
    val emoji: String,
    val blurb: String,
    val unlockLevel: Int,
    val cooldown: Int
)

/**
 * Character classes. Each class changes how combat stats scale from attributes,
 * learns its own skill line (levels 1, 5, and 10), and has its own avatar look.
 */
enum class CharacterClass(
    val label: String,
    val emoji: String,
    val blurb: String,
    val bonusText: String,
    val skills: List<ClassSkill>,
    /** Premium-only classes stay locked until [UserPrefs.isPremium]. */
    val requiresPremium: Boolean = false
) {
    WARRIOR(
        label = "Warrior",
        emoji = "\u2694\uFE0F",
        blurb = "A frontline bruiser. Hits hard and shrugs off damage. Attack scales heavily with Strength.",
        bonusText = "+3 STR to start \u00B7 +10% HP",
        skills = listOf(
            ClassSkill(
                name = "Berserker Slash",
                emoji = "\u2694\uFE0F",
                blurb = "A devastating blow dealing double attack damage.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "War Cry",
                emoji = "\uD83D\uDCE3",
                blurb = "A battle roar that boosts your attack by 40% for 3 turns.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Titan Smash",
                emoji = "\uD83D\uDCA5",
                blurb = "A colossal triple-damage blow that stuns the enemy for a turn.",
                unlockLevel = 10,
                cooldown = 5
            )
        )
    ),
    MAGE(
        label = "Mage",
        emoji = "\uD83D\uDD2E",
        blurb = "A scholar of arcane might. Attack scales with Willpower, and spells pierce armor.",
        bonusText = "+3 WIL to start \u00B7 spells ignore most defense",
        skills = listOf(
            ClassSkill(
                name = "Arcane Blast",
                emoji = "\uD83D\uDD2E",
                blurb = "A surge of raw magic scaling with Willpower that melts through defense.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "Frost Lance",
                emoji = "\u2744\uFE0F",
                blurb = "An icy spear that damages and chills, sapping the enemy's attack for 2 turns.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Meteor",
                emoji = "\u2604\uFE0F",
                blurb = "Calls down a meteor: massive Willpower-scaled damage that ignores all defense.",
                unlockLevel = 10,
                cooldown = 5
            )
        )
    ),
    THIEF(
        label = "Thief",
        emoji = "\uD83D\uDDE1\uFE0F",
        blurb = "A shadow in the night. Fast, lucky, and lethal \u2014 crits far more often than anyone else.",
        bonusText = "+3 AGI to start \u00B7 much higher crit chance",
        skills = listOf(
            ClassSkill(
                name = "Shadow Strike",
                emoji = "\uD83D\uDDE1\uFE0F",
                blurb = "A strike from the shadows that always lands a critical hit.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "Smoke Bomb",
                emoji = "\uD83D\uDCA8",
                blurb = "Slash and vanish \u2014 the enemy's next attack misses you completely.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Assassinate",
                emoji = "\u2620\uFE0F",
                blurb = "A lethal critical strike. Executes: +60% damage when the enemy is below 30% HP.",
                unlockLevel = 10,
                cooldown = 5
            )
        )
    ),
    RANGER(
        label = "Ranger",
        emoji = "\uD83C\uDFF9",
        blurb = "A hunter of the wilds. Balanced and precise \u2014 arrows that ignore armor entirely.",
        bonusText = "+3 END to start \u00B7 skill ignores all defense",
        skills = listOf(
            ClassSkill(
                name = "Piercing Arrow",
                emoji = "\uD83C\uDFF9",
                blurb = "A perfectly aimed shot that ignores the enemy's defense.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "Twin Shot",
                emoji = "\uD83C\uDFAF",
                blurb = "Two arrows in one breath \u2014 each can critically strike.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Rain of Arrows",
                emoji = "\uD83C\uDF27\uFE0F",
                blurb = "A volley that deals heavy damage and leaves the enemy bleeding for 3 turns.",
                unlockLevel = 10,
                cooldown = 5
            )
        )
    ),
    PALADIN(
        label = "Paladin",
        emoji = "\uD83D\uDEE1\uFE0F",
        blurb = "A holy knight. Strength and Willpower in one blade \u2014 durable, righteous, hard to kill.",
        bonusText = "+2 STR, +2 WIL \u00B7 +8% HP",
        requiresPremium = true,
        skills = listOf(
            ClassSkill(
                name = "Holy Strike",
                emoji = "\u2728",
                blurb = "A radiant blow that scales with Strength and Willpower.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "Divine Shield",
                emoji = "\uD83D\uDEE1\uFE0F",
                blurb = "Raise a ward: your next hit against you is heavily reduced.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Judgment",
                emoji = "\u26A1",
                blurb = "Call judgment: heavy piercing damage and a brief stun.",
                unlockLevel = 10,
                cooldown = 5
            )
        )
    ),
    NECROMANCER(
        label = "Necromancer",
        emoji = "\uD83D\uDC80",
        blurb = "A master of death magic. Raise undead servants to fight by your side and drain the life of your foes.",
        bonusText = "+3 WIL to start \u00B7 Undead Summons",
        requiresPremium = true,
        skills = listOf(
            ClassSkill(
                name = "Drain Life",
                emoji = "\uD83E\uDE78",
                blurb = "Siphon vitality: pierce damage and heal yourself for a portion.",
                unlockLevel = 1,
                cooldown = 3
            ),
            ClassSkill(
                name = "Raise Skeleton",
                emoji = "\uD83D\uDDA4",
                blurb = "Summon a skeleton warrior: boosts Defense by +15 and strikes your foe.",
                unlockLevel = 5,
                cooldown = 4
            ),
            ClassSkill(
                name = "Army of the Dead",
                emoji = "\u2620\uFE0F",
                blurb = "Summon a horde of skeletons and zombies: boosts Attack and Defense by +25%.",
                unlockLevel = 10,
                cooldown = 6
            )
        )
    ),
    WHITE_MAGE(
        label = "White Mage",
        emoji = "\uD83D\uDD4A\uFE0F",
        blurb = "A holy vessel of light. Focuses Willpower to heal allies, ward against damage, and smite darkness.",
        bonusText = "+3 WIL to start \u00B7 Holy Healing & Revive",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Holy Light", "\u2728", "Restores HP and deals holy damage to enemies.", 1, 3),
            ClassSkill("Divine Ward", "\uD83D\uDEE1\uFE0F", "Absorbs incoming damage and cleanses debuffs.", 5, 4),
            ClassSkill("Benediction", "\uD83D\uDD4A\uFE0F", "Massive group heal and party revival aura.", 10, 5)
        )
    ),
    MONK(
        label = "Monk",
        emoji = "\uD83E\uDD4B",
        blurb = "A master of martial arts. Combines Agility and Willpower to strike with blinding speed.",
        bonusText = "+2 AGI, +2 WIL \u00B7 Combo Strikes",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Palm Strike", "\uD83D\uDC4A", "A swift blow that generates combo points.", 1, 2),
            ClassSkill("Inner Focus", "\uD83E\uDDD8", "Doubles critical chance for 2 turns.", 5, 4),
            ClassSkill("Flurry of Blows", "\u26A1", "Unleashes 5 rapid strikes in a single turn.", 10, 5)
        )
    ),
    DRUID(
        label = "Druid",
        emoji = "\uD83D\uDC3E",
        blurb = "A guardian of nature. Shifts between Bear Form (Tank) and Panther Form (Agile Crit) at will. Each form has a specialized role with unique trade-offs.",
        bonusText = "+2 END, +2 AGI \u00B7 Adaptive Forms",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Bear Form", "\uD83D\uDC3B", "Shapeshifts into a Dire Bear: +100% Max HP & +10 DEF, but -50% ATK.", 1, 3),
            ClassSkill("Panther Form", "\uD83D\uDC06", "Shapeshifts into a Dire Panther: +50% Crit & +20 SPD, but -30% Max HP.", 5, 3),
            ClassSkill("Wrath of Nature", "\uD83C\uDF3F", "Summons roots to entangle and crush foes.", 10, 5)
        )
    ),
    BERSERKER(
        label = "Berserker",
        emoji = "🪓",
        blurb = "A furious warrior. The lower your health, the higher your attack power.",
        bonusText = "+3 STR to start \u00B7 Rage scaling",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Frenzy Strike", "🪓", "Heavy physical strike scaling with missing HP.", 1, 3),
            ClassSkill("Blood Rage", "🩸", "Sacrifices 10% HP to boost Attack by +60%.", 5, 4),
            ClassSkill("Ragnarok", "🔥", "A devastating strike that hits all enemies with high crit.", 10, 5)
        )
    ),
    BARD(
        label = "Bard",
        emoji = "\uD83C\uDFB5",
        blurb = "A musical battle maestro. Inspires allies with rhythmic songs that empower team attributes.",
        bonusText = "+2 AGI, +2 WIL \u00B7 Rhythm Buffs",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Battle Anthem", "\uD83C\uDFB6", "Empowers party Strength and Willpower by +20%.", 1, 3),
            ClassSkill("Dissonance", "\uD83D\uDD0A", "Stuns the target with a shattering sonic burst.", 5, 4),
            ClassSkill("Hymn of Valor", "\uD83D\uDC51", "Grants full team energy and health regen.", 10, 5)
        )
    ),
    SUMMONER(
        label = "Summoner",
        emoji = "\uD83E\uDDA4",
        blurb = "A caller of primal entities. Summon powerful avatars that fight alongside you, providing massive elemental buffs.",
        bonusText = "+3 WIL to start \u00B7 Combat Companions",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Summon Ifrit", "\uD83D\uDD25", "Summon the Primal of Fire: +50% ATK, but -20% DEF.", 1, 3),
            ClassSkill("Summon Shiva", "\u2744\uFE0F", "Summon the Primal of Ice: +40 SPD and +20% Crit chance.", 5, 3),
            ClassSkill("Astral Flow", "\u2728", "Unleash your summon's ultimate power for massive piercing damage.", 10, 6)
        )
    ),
    DRAGOON(
        label = "Dragoon",
        emoji = "\uD83D\uDC32",
        blurb = "A master of aerial combat and dragon-kin. Jumps high above the battlefield and fights with a loyal Wyvern.",
        bonusText = "+2 STR, +2 AGI \u00B7 Aerial Mastery",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Call Wyvern", "\uD83D\uDC32", "Summon a Wyvern companion: heals you for 5% HP every turn and boosts DEF by +15.", 1, 4),
            ClassSkill("Jump", "\u270A", "Leap into the air and crash down for 2.5x damage. Ignores 50% defense.", 5, 4),
            ClassSkill("Dragon Dive", "\uD83D\uDD25", "A legendary aerial strike that deals massive fire damage to all enemies.", 10, 5)
        )
    );

    /** The signature (level 1) skill, used in class blurbs. */
    val skillName: String get() = skills.first().name
    val skillBlurb: String get() = skills.first().blurb

    /** Skills available at [level]. */
    fun skillsUnlockedAt(level: Int): List<ClassSkill> = skills.filter { it.unlockLevel <= level }
}
