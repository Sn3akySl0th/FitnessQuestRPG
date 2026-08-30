package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.OwnedGear
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemEntity

/**
 * Shared combat-stat comparison for gear tiles and inspect sheets.
 * Uses effective stats (instance rolls + socketed runes).
 */
object GearComparison {

    data class PowerBreakdown(
        val atk: Int,
        val def: Int,
        val hp: Int,
        val upgradeLevel: Int = 0,
    ) {
        val totalPower: Int get() = atk + def + (hp / 4) + upgradeLevel * 2
    }

    data class SocketedRuneBonus(
        val runeId: Long,
        val name: String,
        val emoji: String,
        val atk: Int,
        val def: Int,
        val hp: Int,
    )

    data class UpgradePreview(
        val atkGain: Int,
        val defGain: Int,
        val hpGain: Int,
        val goldCost: Int,
        val materialCost: Int,
        val nextUpgradeLevel: Int,
        val maxUpgradeLevel: Int,
    ) {
        val gainSummary: String
            get() = buildList {
                if (atkGain > 0) add("+$atkGain ATK")
                if (defGain > 0) add("+$defGain DEF")
                if (hpGain > 0) add("+$hpGain HP")
            }.joinToString(", ").ifBlank { "stats" }
    }

    data class DetailedStatBreakdown(
        val baseAtk: Int,
        val baseDef: Int,
        val baseHp: Int,
        val upgradeAtk: Int,
        val upgradeDef: Int,
        val upgradeHp: Int,
        val runeAtk: Int,
        val runeDef: Int,
        val runeHp: Int,
        val upgradeLevel: Int,
        val maxUpgradeLevel: Int,
        val socketedRunes: List<SocketedRuneBonus>,
        val upgradePreview: UpgradePreview?,
    ) {
        val totalAtk: Int get() = baseAtk + upgradeAtk + runeAtk
        val totalDef: Int get() = baseDef + upgradeDef + runeDef
        val totalHp: Int get() = baseHp + upgradeHp + runeHp
        val totalPower: Int get() = PowerBreakdown(totalAtk, totalDef, totalHp, upgradeLevel).totalPower
    }

    enum class Verdict { UPGRADE, DOWNGRADE, SIDEGRADE, NO_BASELINE }

    data class Comparison(
        val candidate: PowerBreakdown,
        val equipped: PowerBreakdown?,
        val atkDelta: Int,
        val defDelta: Int,
        val hpDelta: Int,
    ) {
        val powerDelta: Int =
            candidate.totalPower - (equipped?.totalPower ?: 0)

        val verdict: Verdict = when {
            equipped == null -> if (candidate.totalPower > 0) Verdict.UPGRADE else Verdict.NO_BASELINE
            powerDelta > 0 -> Verdict.UPGRADE
            powerDelta < 0 -> Verdict.DOWNGRADE
            else -> Verdict.SIDEGRADE
        }
    }

    fun breakdownFromOwned(
        gear: OwnedGear,
        runeCatalog: Map<Long, ItemEntity> = emptyMap(),
    ): PowerBreakdown {
        val effective = gear.asEquippedItem(runeCatalog)
        return PowerBreakdown(
            atk = effective.atk,
            def = effective.def,
            hp = effective.hp,
            upgradeLevel = gear.instance.upgradeLevel,
        )
    }

    fun breakdownFromCatalog(item: ItemEntity): PowerBreakdown =
        PowerBreakdown(item.atk, item.def, item.hp, upgradeLevel = 0)

    fun breakdown(
        item: ItemEntity,
        instance: GearInstanceEntity?,
        runeCatalog: Map<Long, ItemEntity> = emptyMap(),
    ): PowerBreakdown = when {
        instance != null -> breakdownFromOwned(OwnedGear(instance, item), runeCatalog)
        else -> breakdownFromCatalog(item)
    }

    fun compare(candidate: PowerBreakdown, equipped: PowerBreakdown?): Comparison {
        val baseline = equipped ?: PowerBreakdown(0, 0, 0)
        return Comparison(
            candidate = candidate,
            equipped = equipped,
            atkDelta = candidate.atk - baseline.atk,
            defDelta = candidate.def - baseline.def,
            hpDelta = candidate.hp - baseline.hp,
        )
    }

    fun compareGear(
        item: ItemEntity,
        instance: GearInstanceEntity?,
        equipped: OwnedGear?,
        equippedCatalog: ItemEntity?,
        runeCatalog: Map<Long, ItemEntity> = emptyMap(),
    ): Comparison {
        val candidate = breakdown(item, instance, runeCatalog)
        val equippedBreakdown = if (equipped != null && equippedCatalog != null) {
            breakdownFromOwned(equipped, runeCatalog)
        } else {
            null
        }
        return compare(candidate, equippedBreakdown)
    }

    /** Per-level combat stat gains from upgrading — mirrors [com.fitnessquest.rpg.data.GameRepository.upgradeGearInstance]. */
    fun upgradeGainsPerLevel(catalog: ItemEntity): Triple<Int, Int, Int> {
        val atkGain = if (catalog.atk > 0) (1 + catalog.tier / 2) else 0
        val defGain = if (catalog.def > 0) (1 + catalog.tier / 2) else 0
        val hpGain = if (catalog.hp > 0 || (atkGain == 0 && defGain == 0)) 3 + catalog.tier * 2 else 0
        return Triple(atkGain, defGain, hpGain)
    }

    fun runeStatBonus(runeId: Long, rune: ItemEntity?): Triple<Int, Int, Int> {
        var atk = Runes.atkBonus(runeId)
        var def = Runes.defBonus(runeId)
        var hp = Runes.hpBonus(runeId)
        rune?.let { row ->
            if (atk == 0) atk = row.atk
            if (def == 0) def = row.def
            if (hp == 0) hp = row.hp
        }
        return Triple(atk, def, hp)
    }

    fun detailedBreakdown(
        item: ItemEntity,
        instance: GearInstanceEntity?,
        runeCatalog: Map<Long, ItemEntity> = emptyMap(),
    ): DetailedStatBreakdown {
        val upgradeLevel = instance?.upgradeLevel ?: 0
        val maxUpgradeLevel = ProgressionRules.maxUpgradeLevel(item.tier)
        val (atkPerLevel, defPerLevel, hpPerLevel) = upgradeGainsPerLevel(item)
        val upgradeAtk = atkPerLevel * upgradeLevel
        val upgradeDef = defPerLevel * upgradeLevel
        val upgradeHp = hpPerLevel * upgradeLevel

        val rolledAtk = instance?.atk ?: item.atk
        val rolledDef = instance?.def ?: item.def
        val rolledHp = instance?.hp ?: item.hp

        val socketedRunes = instance?.runeIds().orEmpty().mapNotNull { runeId ->
            val rune = runeCatalog[runeId]
            val (atk, def, hp) = runeStatBonus(runeId, rune)
            if (rune == null && atk == 0 && def == 0 && hp == 0) return@mapNotNull null
            SocketedRuneBonus(
                runeId = runeId,
                name = rune?.name ?: "Rune",
                emoji = rune?.emoji ?: "✦",
                atk = atk,
                def = def,
                hp = hp,
            )
        }

        val runeAtk = socketedRunes.sumOf { it.atk }
        val runeDef = socketedRunes.sumOf { it.def }
        val runeHp = socketedRunes.sumOf { it.hp }

        val upgradePreview = if (instance != null && upgradeLevel < maxUpgradeLevel) {
            UpgradePreview(
                atkGain = atkPerLevel,
                defGain = defPerLevel,
                hpGain = hpPerLevel,
                goldCost = ProgressionRules.upgradeGoldCost(item.tier, upgradeLevel),
                materialCost = ProgressionRules.upgradeMaterialCost(item.tier, upgradeLevel),
                nextUpgradeLevel = upgradeLevel + 1,
                maxUpgradeLevel = maxUpgradeLevel,
            )
        } else {
            null
        }

        return DetailedStatBreakdown(
            baseAtk = rolledAtk - upgradeAtk,
            baseDef = rolledDef - upgradeDef,
            baseHp = rolledHp - upgradeHp,
            upgradeAtk = upgradeAtk,
            upgradeDef = upgradeDef,
            upgradeHp = upgradeHp,
            runeAtk = runeAtk,
            runeDef = runeDef,
            runeHp = runeHp,
            upgradeLevel = upgradeLevel,
            maxUpgradeLevel = maxUpgradeLevel,
            socketedRunes = socketedRunes,
            upgradePreview = upgradePreview,
        )
    }
}
