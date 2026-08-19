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
 * learns its own skill line (levels 1, 5, 10, 20, 35, and 50), and has its own avatar look.
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
        emoji = "⚔️",
        blurb = "A frontline bruiser. Hits hard and shrugs off damage. Attack scales heavily with Strength.",
        bonusText = "+3 STR to start · +10% HP",
        skills = listOf(
            ClassSkill("Berserker Slash", "⚔️", "A devastating blow dealing double attack damage.", 1, 3),
            ClassSkill("War Cry", "📢", "A battle roar that boosts your attack by 40% for 3 turns.", 5, 4),
            ClassSkill("Titan Smash", "💥", "A colossal triple-damage blow that stuns the enemy for a turn.", 10, 5),
            ClassSkill("Iron Bastion", "🛡️", "Enters an impenetrable guard: +35 Defense and reflects damage for 3 turns.", 20, 4),
            ClassSkill("Colossus Cleave", "🪓", "A devastating wide sweep that sunders armor for 3.5x Strength damage.", 35, 5),
            ClassSkill("Avatar of War", "⚡", "Awaken the Titan within: increases Attack by +80% and gains stun immunity for 4 turns.", 50, 6)
        )
    ),
    MAGE(
        label = "Mage",
        emoji = "🔮",
        blurb = "A scholar of arcane might. Attack scales with Willpower, and spells pierce armor.",
        bonusText = "+3 WIL to start · spells ignore most defense",
        skills = listOf(
            ClassSkill("Arcane Blast", "🔮", "A surge of raw magic scaling with Willpower that melts through defense.", 1, 3),
            ClassSkill("Frost Lance", "❄️", "An icy spear that damages and chills, sapping the enemy's attack for 2 turns.", 5, 4),
            ClassSkill("Meteor", "☄️", "Calls down a meteor: massive Willpower-scaled damage that ignores all defense.", 10, 5),
            ClassSkill("Chain Lightning", "⚡", "Forks lethal electricity through foes, shocking for heavy piercing damage.", 20, 4),
            ClassSkill("Time Warp", "⏳", "Bends the arcane flow: resets skill cooldowns and boosts Speed by +50.", 35, 6),
            ClassSkill("Supernova", "🌌", "Unleashes a cosmic explosion of stellar Willpower that annihilates target defense.", 50, 6)
        )
    ),
    THIEF(
        label = "Thief",
        emoji = "🗡️",
        blurb = "A shadow in the night. Fast, lucky, and lethal — crits far more often than anyone else.",
        bonusText = "+3 AGI to start · much higher crit chance",
        skills = listOf(
            ClassSkill("Shadow Strike", "🗡️", "A strike from the shadows that always lands a critical hit.", 1, 3),
            ClassSkill("Smoke Bomb", "💨", "Slash and vanish — the enemy's next attack misses you completely.", 5, 4),
            ClassSkill("Assassinate", "☠️", "A lethal critical strike. Executes: +60% damage when the enemy is below 30% HP.", 10, 5),
            ClassSkill("Poisoned Blade", "🧪", "Coats daggers in venom: strikes and deals severe poison damage over 4 turns.", 20, 4),
            ClassSkill("Phantom Dance", "👤", "A flurry of 4 shadow strikes with guaranteed critical hits.", 35, 5),
            ClassSkill("Death's Embrace", "🥷", "Vanishes into pure darkness, executing weakened targets for massive 5x damage.", 50, 6)
        )
    ),
    RANGER(
        label = "Hunter",
        emoji = "🏹",
        blurb = "A master marksman and beastmaster. Fights in perfect harmony with a loyal animal companion.",
        bonusText = "+3 END to start · Loyal Beast Companion",
        skills = listOf(
            ClassSkill("Flanking Shot", "🏹", "Hunter fires a piercing arrow while companion flanks for armor-ignoring damage.", 1, 3),
            ClassSkill("Primal Howl", "🐺", "Companion howls fiercely, granting +20% Crit and inflicting bleed.", 5, 4),
            ClassSkill("Bestial Wrath", "🐾", "Hunter and companion enter synchronized fury, delivering a devastating execute strike.", 10, 5),
            ClassSkill("Eagle Eye", "🦅", "Pins target with focus: +30 Speed and guaranteed critical strikes for 2 turns.", 20, 4),
            ClassSkill("Spirit Bond", "🌿", "Companion bonds with hunter: heals 25% HP and buffs Defense by +25.", 35, 5),
            ClassSkill("Primal Stampede", "🦌", "Calls forth spirits of the wild: a colossal onslaught dealing massive piercing damage.", 50, 6)
        )
    ),
    PALADIN(
        label = "Paladin",
        emoji = "🛡️",
        blurb = "A holy knight. Strength and Willpower in one blade — durable, righteous, hard to kill.",
        bonusText = "+2 STR, +2 WIL · +8% HP",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Holy Strike", "✨", "A radiant blow that scales with Strength and Willpower.", 1, 3),
            ClassSkill("Divine Shield", "🛡️", "Raise a ward: your next hit against you is heavily reduced.", 5, 4),
            ClassSkill("Judgment", "⚡", "Call judgment: heavy piercing damage and a brief stun.", 10, 5),
            ClassSkill("Consecration", "☀️", "Holy ground burns the enemy over 3 turns and heals the Paladin.", 20, 4),
            ClassSkill("Lay on Hands", "🙌", "Calls upon sacred grace to restore 45% Max HP and cleanse debuffs.", 35, 6),
            ClassSkill("Aegis of Heavens", "🕊️", "Summons celestial armor: invulnerable for 1 turn and retaliates with 4x Holy damage.", 50, 6)
        )
    ),
    NECROMANCER(
        label = "Necromancer",
        emoji = "💀",
        blurb = "A master of death magic. Raise undead servants to fight by your side and drain the life of your foes.",
        bonusText = "+3 WIL to start · Undead Summons",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Drain Life", "🩸", "Siphon vitality: pierce damage and heal yourself for a portion.", 1, 3),
            ClassSkill("Raise Skeleton", "🖤", "Summon a skeleton warrior: boosts Defense by +15 and strikes your foe.", 5, 4),
            ClassSkill("Army of the Dead", "☠️", "Summon a horde of skeletons and zombies: boosts Attack and Defense by +25%.", 10, 6),
            ClassSkill("Bone Armor", "🦴", "Surrounds yourself in spinning bones: +30 DEF and damages attackers on contact.", 20, 4),
            ClassSkill("Soul Siphon", "🌀", "Rips soul energy: heavy piercing damage and converts 40% into health.", 35, 5),
            ClassSkill("Lord of the Dead", "👑", "Ascends as Death Lord: summons an Arch-Lich dealing catastrophic necrotic damage.", 50, 6)
        )
    ),
    WHITE_MAGE(
        label = "White Mage",
        emoji = "🕊️",
        blurb = "A holy vessel of light. Focuses Willpower to heal allies, ward against damage, and smite darkness.",
        bonusText = "+3 WIL to start · Holy Healing & Revive",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Holy Light", "✨", "Restores HP and deals holy damage to enemies.", 1, 3),
            ClassSkill("Divine Ward", "🛡️", "Absorbs incoming damage and cleanses debuffs.", 5, 4),
            ClassSkill("Benediction", "🕊️", "Massive group heal and party revival aura.", 10, 5),
            ClassSkill("Seraph Grace", "🪽", "Angelic radiance: heals for 35% HP and increases Speed by +30.", 20, 4),
            ClassSkill("Smite", "⚡", "Calls down righteous light that deals pure Willpower damage.", 35, 5),
            ClassSkill("Apotheosis", "🌟", "Channels supreme sacred power: fully restores HP and reflects 50% damage.", 50, 6)
        )
    ),
    MONK(
        label = "Monk",
        emoji = "🥋",
        blurb = "A master of martial arts. Combines Agility and Willpower to strike with blinding speed.",
        bonusText = "+2 AGI, +2 WIL · Combo Strikes",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Palm Strike", "👊", "A swift blow that generates combo points.", 1, 2),
            ClassSkill("Inner Focus", "🧘", "Doubles critical chance for 2 turns.", 5, 4),
            ClassSkill("Flurry of Blows", "⚡", "Unleashes 5 rapid strikes in a single turn.", 10, 5),
            ClassSkill("Dragon Kick", "🐉", "Flying chi kick: deals 3x Agility damage and stuns the target.", 20, 4),
            ClassSkill("Diamond Body", "💎", "Hardens body like diamond: +40 DEF and immunity to criticals for 3 turns.", 35, 5),
            ClassSkill("Thousand Fists", "💥", "Unleashes 1,000 rapid chi strikes, ignoring all armor and shattering defense.", 50, 6)
        )
    ),
    DRUID(
        label = "Druid",
        emoji = "🐾",
        blurb = "A shapeshifting guardian of nature. Shifts between Bear, Panther, Treant, Moonkin, and Primal Avatar forms.",
        bonusText = "+2 END, +2 AGI · Wild Shape Mastery",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Bear Form", "🐻", "Shapeshifts into a Dire Bear: +100% Max HP & +10 DEF with ironhide protection.", 1, 3),
            ClassSkill("Panther Form", "🐆", "Shapeshifts into a Shadow Panther: +50% Crit & +20 SPD with lethal stalker swipes.", 5, 3),
            ClassSkill("Wrath of Nature", "🌿", "Entangles the foe in crushing roots, dealing nature damage and inflicting bleed.", 10, 5),
            ClassSkill("Treant Form", "🌲", "Shapeshifts into an Ancient Treant: +30% Max HP, +15 DEF, and reflects 20% thorns.", 20, 4),
            ClassSkill("Moonkin Form", "🦉", "Shapeshifts into a Celestial Moonkin: +40% ATK, +15% Crit, and casts defense-piercing Moonfire.", 35, 4),
            ClassSkill("Avatar of the Wilds", "🦅", "Ascends into the Apex Primal Avatar: massive hybrid stat boosts and primal elemental storm.", 50, 6)
        )
    ),
    BERSERKER(
        label = "Berserker",
        emoji = "🪓",
        blurb = "A furious warrior. The lower your health, the higher your attack power.",
        bonusText = "+3 STR to start · Rage scaling",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Frenzy Strike", "🪓", "Heavy physical strike scaling with missing HP.", 1, 3),
            ClassSkill("Blood Rage", "🩸", "Sacrifices 10% HP to boost Attack by +60%.", 5, 4),
            ClassSkill("Ragnarok", "🔥", "A devastating strike that hits all enemies with high crit.", 10, 5),
            ClassSkill("Undying Will", "💀", "Refuses to fall: for 3 turns, attack surges as HP drops.", 20, 5),
            ClassSkill("Skull Splitter", "💥", "A brutal overhead smash dealing 4x Strength damage and guaranteed crit.", 35, 4),
            ClassSkill("God of Wrath", "🌋", "Berserker awakening: +100% Attack and lifesteals 30% of all damage dealt.", 50, 6)
        )
    ),
    BARD(
        label = "Bard",
        emoji = "🎵",
        blurb = "A musical battle maestro. Inspires allies with rhythmic songs that empower team attributes.",
        bonusText = "+2 AGI, +2 WIL · Rhythm Buffs",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Battle Anthem", "🎶", "Empowers party Strength and Willpower by +20%.", 1, 3),
            ClassSkill("Dissonance", "🔊", "Stuns the target with a shattering sonic burst.", 5, 4),
            ClassSkill("Hymn of Valor", "👑", "Grants full team energy and health regen.", 10, 5),
            ClassSkill("Song of Speed", "💨", "Exhilarating tempo: +50 Speed and +25% Dodge chance for 3 turns.", 20, 4),
            ClassSkill("Requiem", "🎻", "Haunting melody that deals heavy piercing damage and saps enemy attack by 40%.", 35, 5),
            ClassSkill("Symphony", "🌌", "Celestial masterwork: full team buff, max energy surge, and massive sonic damage.", 50, 6)
        )
    ),
    SUMMONER(
        label = "Summoner",
        emoji = "🦚",
        blurb = "A caller of primal entities. Summon powerful avatars that fight alongside you.",
        bonusText = "+3 WIL to start · Combat Companions",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Summon Ifrit", "🔥", "Summon the Primal of Fire: +50% ATK, but -20% DEF.", 1, 3),
            ClassSkill("Summon Shiva", "❄️", "Summon the Primal of Ice: +40 SPD and +20% Crit chance.", 5, 3),
            ClassSkill("Astral Flow", "✨", "Unleash your summon's ultimate power for massive piercing damage.", 10, 6),
            ClassSkill("Summon Titan", "🗿", "Summon the Primal of Earth: +50 DEF and absorbs incoming blows.", 20, 4),
            ClassSkill("Summon Bahamut", "🐉", "Call forth King of Dragons: Megaflare incinerates foes for massive pierce damage.", 35, 5),
            ClassSkill("Astral Fusion", "👑", "Fuses with all primals: +60% ATK, +40 SPD, and fires cosmic flares.", 50, 6)
        )
    ),
    DRAGOON(
        label = "Dragoon",
        emoji = "🐲",
        blurb = "A master of aerial combat and dragon-kin. Jumps high above the battlefield with a loyal Wyvern.",
        bonusText = "+2 STR, +2 AGI · Aerial Mastery",
        requiresPremium = true,
        skills = listOf(
            ClassSkill("Call Wyvern", "🐲", "Summon a Wyvern companion: heals you for 5% HP every turn and boosts DEF by +15.", 1, 4),
            ClassSkill("Jump", "✊", "Leap into the air and crash down for 2.5x damage. Ignores 50% defense.", 5, 4),
            ClassSkill("Dragon Dive", "🔥", "A legendary aerial strike that deals massive fire damage to all enemies.", 10, 5),
            ClassSkill("Wyvern Breath", "💨", "Wyvern breathes a stream of flame, searing foe and shredding defense.", 20, 4),
            ClassSkill("High Wind", "🌪️", "Launches into orbit, evading attacks before crashing down for 4x Strength damage.", 35, 5),
            ClassSkill("Stardiver", "🌠", "A celestial dive from the cosmos dealing catastrophic piercing damage.", 50, 6)
        )
    );

    /** The signature (level 1) skill, used in class blurbs. */
    val skillName: String get() = skills.first().name
    val skillBlurb: String get() = skills.first().blurb

    /** Skills available at [level]. */
    fun skillsUnlockedAt(level: Int): List<ClassSkill> = skills.filter { it.unlockLevel <= level }
}
