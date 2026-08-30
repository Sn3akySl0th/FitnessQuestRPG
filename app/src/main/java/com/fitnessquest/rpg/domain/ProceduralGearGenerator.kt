package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ArmorSlots
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import kotlin.random.Random

/** Fully rolled gear ready to persist as a [com.fitnessquest.rpg.data.db.GearInstanceEntity]. */
data class GeneratedGear(
    val catalogTemplate: ItemEntity,
    val baseTypeId: String,
    val displayName: String,
    val rarity: GearRarity,
    val traits: List<GearTrait>,
    val atk: Int,
    val def: Int,
    val hp: Int,
    val itemLevel: Int,
    val affixIds: List<String>,
    val setId: String?,
)

object ProceduralGearGenerator {

    fun generate(
        character: CharacterEntity?,
        itemLevel: Int,
        maxTier: Int,
        source: LootSource,
        templatePool: List<ItemEntity>,
        prCount: Int = 0,
        rng: Random = Random.Default,
    ): GeneratedGear? {
        val template = pickTemplate(character, maxTier, templatePool, rng) ?: return null
        val rarity = LootTables.rollRarity(
            source = source,
            characterLevel = itemLevel.coerceAtLeast(1),
            prCount = prCount,
            rng = rng,
        )
        val traits = GearTrait.rollTraitsForRarity(rarity, rng)
        val affixes = GearAffixPool.rollAffixes(rarity, itemLevel, rng)

        val baseStats = ProceduralStatEngine.generateStats(
            catalog = template,
            rarity = rarity,
            itemLevel = itemLevel,
            wideVariance = true,
            rng = rng,
        )
        val affixAtk = affixes.sumOf { it.atk }
        val affixDef = affixes.sumOf { it.def }
        val affixHp = affixes.sumOf { it.hp }

        val setId = when {
            template.slot in ArmorSlots &&
                template.classAffinity != null &&
                rng.nextInt(100) < LootRates.SET_TAG_CHANCE -> GearSetRegistry.setIdFor(template)
            else -> null
        }

        val baseName = baseDisplayName(template, setId)
        val displayName = GearAffixPool.composeDisplayName(baseName, affixes)

        return GeneratedGear(
            catalogTemplate = template,
            baseTypeId = "${template.slot.name.lowercase()}_t${template.tier}_${template.id}",
            displayName = displayName,
            rarity = rarity,
            traits = traits,
            atk = baseStats.atk + affixAtk,
            def = baseStats.def + affixDef,
            hp = baseStats.hp + affixHp,
            itemLevel = itemLevel.coerceAtLeast(1),
            affixIds = affixes.map { it.id },
            setId = setId,
        )
    }

    fun toLootGrant(generated: GeneratedGear): LootGrant.Gear =
        LootGrant.Gear(
            catalog = generated.catalogTemplate,
            rarity = generated.rarity,
            traits = generated.traits,
            displayName = generated.displayName,
            baseTypeId = generated.baseTypeId,
            itemLevel = generated.itemLevel,
            affixIds = generated.affixIds,
            setId = generated.setId,
            rolledAtk = generated.atk,
            rolledDef = generated.def,
            rolledHp = generated.hp,
        )

    private fun pickTemplate(
        character: CharacterEntity?,
        maxTier: Int,
        pool: List<ItemEntity>,
        rng: Random,
    ): ItemEntity? {
        val candidates = pool.filter { it.slot.isEquippable() && it.tier <= maxTier }
        if (candidates.isEmpty()) return null

        val cls = character?.characterClass
        val weighted = candidates.flatMap { item ->
            val archetype = WearArchetype.resolve(item)
            val weight = when {
                cls != null && item.classAffinity == cls -> 4
                item.classAffinity == null -> 2
                archetype == WearArchetype.SHIELD &&
                    (cls == CharacterClass.WARRIOR || cls == CharacterClass.PALADIN) -> 3
                archetype == WearArchetype.SHIELD -> 0
                else -> 1
            }
            if (weight <= 0) emptyList() else List(weight) { item }
        }
        return weighted.randomOrNull(rng)
    }

    private fun baseDisplayName(template: ItemEntity, setId: String?): String {
        if (setId != null && template.slot in ArmorSlots) {
            val setLabel = GearSetRegistry.displayNameFor(setId)
            val piece = when (template.slot) {
                ItemSlot.HEAD -> "Hood"
                ItemSlot.CHEST -> "Vest"
                ItemSlot.HANDS -> "Grips"
                ItemSlot.LEGS -> "Legguards"
                ItemSlot.FEET -> "Boots"
                else -> template.slot.label
            }
            return "$setLabel $piece"
        }
        return template.name
    }
}
