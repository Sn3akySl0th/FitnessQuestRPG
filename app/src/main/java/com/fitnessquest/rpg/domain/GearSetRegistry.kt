package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ArmorSlots
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity

data class SetBonusLevel(
    val piecesRequired: Int,
    val atkMultiplier: Float = 1f,
    val hpMultiplier: Float = 1f,
    val flatAtk: Int = 0,
    val flatDef: Int = 0,
    val critBonus: Int = 0,
    val spdBonus: Int = 0,
    val mitigationPercent: Float = 0f,
    val description: String,
    val classRiderDescription: String? = null,
)

data class AppliedSetBonus(
    val setId: String?,
    val setName: String,
    val pieceCount: Int,
    val activeLevel: SetBonusLevel?,
    val nextLevel: SetBonusLevel?,
    val classRiderActive: Boolean,
    val usesNamedSet: Boolean,
)

/** Named armor set ids for procedural drops and set bonuses. */
object GearSetRegistry {
    private val classTierSets: Map<CharacterClass, Map<Int, String>> = mapOf(
        CharacterClass.WARRIOR to mapOf(2 to "ironbound", 3 to "steelwrought", 4 to "titanforged"),
        CharacterClass.MAGE to mapOf(2 to "apprentice", 3 to "runebound", 4 to "cosmic"),
        CharacterClass.THIEF to mapOf(2 to "prowler", 3 to "shadowstep", 4 to "nightveil"),
        CharacterClass.RANGER to mapOf(2 to "scout", 3 to "wildstalker", 4 to "worldroot"),
        CharacterClass.PALADIN to mapOf(2 to "oathbound", 3 to "radiant", 4 to "dawnforged"),
        CharacterClass.NECROMANCER to mapOf(2 to "bonebound", 3 to "cryptweave", 4 to "eclipse"),
        CharacterClass.WHITE_MAGE to mapOf(2 to "vestments_of_light", 3 to "sanctified", 4 to "celestial"),
        CharacterClass.MONK to mapOf(2 to "bandit", 3 to "temple", 4 to "dragon_fist"),
        CharacterClass.DRUID to mapOf(2 to "wildwood", 3 to "sylvan", 4 to "archdruid"),
        CharacterClass.BERSERKER to mapOf(2 to "bloodbound", 3 to "warlord", 4 to "juggernaut"),
        CharacterClass.BARD to mapOf(2 to "minstrel", 3 to "virtuoso", 4 to "harmonic"),
        CharacterClass.SUMMONER to mapOf(2 to "caller", 3 to "primal", 4 to "eldritch"),
        CharacterClass.DRAGOON to mapOf(2 to "lancer", 3 to "skyward", 4 to "stormlance"),
    )

    private val setAffinity: Map<String, CharacterClass> = classTierSets.flatMap { (cls, tiers) ->
        tiers.values.map { it to cls }
    }.toMap()

    private val setTier: Map<String, Int> = classTierSets.flatMap { (_, tiers) ->
        tiers.map { (tier, id) -> id to tier }
    }.toMap()

    private val setFlavors: Map<String, Triple<String, String, String>> = mapOf(
        "ironbound" to Triple("Ironbound Discipline", "Steel Temper", "Titan's Resolve"),
        "steelwrought" to Triple("Forged Frontline", "Battle-Hardened", "Unbreakable Phalanx"),
        "titanforged" to Triple("Colossus Stance", "Warlord's Guard", "Avatar of Iron"),
        "nightveil" to Triple("Shadow Prowl", "Veil Step", "Night's Embrace"),
        "oathbound" to Triple("Sacred Oath", "Radiant Ward", "Dawn's Aegis"),
        "dawnforged" to Triple("Crusader's Light", "Sunforged", "Heaven's Bulwark"),
    )

    fun setIdFor(template: ItemEntity): String? {
        if (template.slot !in ArmorSlots) return null
        val cls = template.classAffinity ?: return null
        return classTierSets[cls]?.get(template.tier)
    }

    fun displayNameFor(setId: String): String =
        setId.split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    fun classAffinityFor(setId: String): CharacterClass? = setAffinity[setId]

    fun gearTierFor(setId: String): Int = setTier[setId] ?: 2

    fun allSetIds(): List<String> = setTier.keys.sortedBy { displayNameFor(it) }

    fun searchSets(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allSetIds()
        return allSetIds().filter {
            displayNameFor(it).lowercase().contains(q) || it.contains(q)
        }
    }

    fun farmHintFor(setId: String): String {
        val tier = gearTierFor(setId)
        val biomes = Biome.entries.filter { LootRates.biomeBossGearTier(it) >= tier.coerceAtMost(4) }
            .take(3)
            .joinToString(", ") { it.label }
        return "Battles & chests in $biomes · ~${LootRates.SET_TAG_CHANCE}% set tag on class armor"
    }

    fun bonusLevelsFor(setId: String): List<SetBonusLevel> {
        val name = displayNameFor(setId)
        val flavor = setFlavors[setId]
        val tier = gearTierFor(setId)
        val isTank = setId in setOf("ironbound", "steelwrought", "titanforged", "oathbound", "radiant", "dawnforged", "juggernaut", "warlord")
        val isCrit = setId in setOf("nightveil", "shadowstep", "prowler", "harmonic", "virtuoso")
        val def2 = if (isTank) 3 else 1
        val def4 = if (isTank) 6 else 3
        val def5 = if (isTank) 10 else 5
        val crit4 = if (isCrit) 8 else 5
        val crit5 = if (isCrit) 12 else 10
        val rider = classAffinityFor(setId)?.let { "+${it.label} rider: extra DEF when class matches" }
        return listOf(
            SetBonusLevel(
                piecesRequired = 2,
                atkMultiplier = 1.05f,
                hpMultiplier = 1.05f,
                flatDef = def2,
                description = flavor?.first ?: "$name (2): +5% ATK/HP, +$def2 DEF",
                classRiderDescription = rider,
            ),
            SetBonusLevel(
                piecesRequired = 4,
                atkMultiplier = 1.10f,
                hpMultiplier = 1.10f,
                flatDef = def4,
                critBonus = crit4,
                mitigationPercent = if (isTank) 0.03f else 0f,
                description = flavor?.second ?: "$name (4): +10% ATK/HP, +$def4 DEF, +$crit4% Crit",
                classRiderDescription = rider,
            ),
            SetBonusLevel(
                piecesRequired = 5,
                atkMultiplier = 1.15f,
                hpMultiplier = 1.15f,
                flatDef = def5,
                critBonus = crit5,
                spdBonus = if (tier >= 4) 5 else 3,
                mitigationPercent = if (isTank) 0.05f else 0.02f,
                description = flavor?.third ?: "$name (5): +15% ATK/HP, +$def5 DEF, full synergy",
                classRiderDescription = rider,
            ),
        )
    }

    fun dominantSetId(
        equipped: List<ItemEntity>,
        instanceById: Map<Long, GearInstanceEntity>,
    ): String? {
        val byCatalog = instancesByCatalogId(instanceById)
        val counts = equipped
            .filter { it.slot in ArmorSlots }
            .mapNotNull { byCatalog[it.id]?.setId }
            .groupingBy { it }
            .eachCount()
        return counts.maxByOrNull { it.value }?.key
    }

    fun resolveAppliedBonus(
        character: CharacterEntity,
        equipped: List<ItemEntity>,
        instanceById: Map<Long, GearInstanceEntity> = emptyMap(),
    ): AppliedSetBonus {
        val armor = equipped.filter { it.slot in ArmorSlots }
        val setId = dominantSetId(equipped, instanceById)
        if (setId != null) {
            val byCatalog = instancesByCatalogId(instanceById)
            val pieceCount = armor.count { byCatalog[it.id]?.setId == setId }
            val levels = bonusLevelsFor(setId)
            val active = levels.lastOrNull { pieceCount >= it.piecesRequired }
            val next = levels.firstOrNull { pieceCount < it.piecesRequired }
            val affinity = classAffinityFor(setId)
            val classPieces = if (affinity != null) {
                armor.count { it.classAffinity == affinity }
            } else {
                0
            }
            val rider = affinity != null && character.characterClass == affinity && pieceCount >= 2 && classPieces >= 2
            return AppliedSetBonus(
                setId = setId,
                setName = displayNameFor(setId),
                pieceCount = pieceCount,
                activeLevel = active,
                nextLevel = next,
                classRiderActive = rider,
                usesNamedSet = true,
            )
        }

        val cls = character.characterClass
        val legacyCount = if (cls != null) armor.count { it.classAffinity == cls } else 0
        val active = legacyBonusForCount(legacyCount)
        val next = legacyBonusForCount(legacyCount)?.let { current ->
            legacyBonusForCount(when (current.piecesRequired) {
                2 -> 3
                4 -> 4
                5 -> 6
                else -> 2
            })
        } ?: legacyBonusForCount(2).takeIf { legacyCount < 2 }
        return AppliedSetBonus(
            setId = null,
            setName = cls?.label?.let { "$it armor" } ?: "Class armor",
            pieceCount = legacyCount,
            activeLevel = active,
            nextLevel = if (legacyCount < 2) legacyBonusForCount(2) else if (legacyCount < 4) legacyBonusForCount(4) else if (legacyCount < 5) legacyBonusForCount(5) else null,
            classRiderActive = false,
            usesNamedSet = false,
        )
    }

    private fun legacyBonusForCount(count: Int): SetBonusLevel? = when {
        count >= 5 -> SetBonusLevel(5, 1.15f, 1.15f, flatDef = 5, critBonus = 10, spdBonus = 5, description = "+15% ATK, +15% HP, +5 DEF, +10% Crit, +5 SPD")
        count >= 4 -> SetBonusLevel(4, 1.10f, 1.10f, flatDef = 3, critBonus = 5, description = "+10% ATK, +10% HP, +3 DEF, +5% Crit")
        count >= 2 -> SetBonusLevel(2, 1.05f, 1.05f, flatDef = 1, description = "+5% ATK, +5% HP, +1 DEF")
        else -> null
    }

    fun instancesByCatalogId(instanceById: Map<Long, GearInstanceEntity>): Map<Long, GearInstanceEntity> =
        instanceById.values.associateBy { it.catalogId }
}
