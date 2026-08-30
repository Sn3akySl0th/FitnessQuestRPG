package com.fitnessquest.rpg.domain

import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot

/** Visual and equip semantics for accessories until OFF_HAND/RING/NECK/BACK slots ship. */
enum class WearArchetype(val label: String) {
    SHIELD("Shield"),
    RING("Ring"),
    AMULET("Amulet"),
    BELT("Belt"),
    BROOCH("Brooch"),
    MEDAL("Medal"),
    CAPE("Cape"),
    WINGS("Wings"),
    QUIVER("Quiver"),
    GENERIC("Trinket"),
    ;

    val isBackWearable: Boolean
        get() = this in setOf(CAPE, WINGS, QUIVER, SHIELD)

    companion object {
        fun resolve(item: ItemEntity): WearArchetype {
            val style = item.style.lowercase()
            val name = item.name.lowercase()
            return when {
                style == "shield" || name.contains("shield") || name.contains("buckler") ||
                    name.contains("bulwark") || name.contains("aegis") && item.slot == ItemSlot.TRINKET -> SHIELD
                style == "cape" || style == "cloak" || name.contains("cape") || name.contains("cloak") ||
                    name.contains("mantle") -> CAPE
                style == "wings" || name.contains("wing") -> WINGS
                style == "quiver" || name.contains("quiver") -> QUIVER
                name.contains("ring") || name.contains("band") && !name.contains("headband") -> RING
                name.contains("amulet") || name.contains("locket") || name.contains("pendant") ||
                    name.contains("talisman") || name.contains("charm") || name.contains("phylactery") -> AMULET
                name.contains("belt") || name.contains("girdle") || name.contains("sash") ||
                    name.contains("torque") -> BELT
                name.contains("brooch") -> BROOCH
                name.contains("medal") || name.contains("sweatband") -> MEDAL
                else -> GENERIC
            }
        }
    }
}

object EquipRules {
    fun isTwoHandedWeapon(item: ItemEntity): Boolean = when (item.style) {
        ItemStyle.GREATSWORD, ItemStyle.BOW, ItemStyle.STAFF -> true
        else -> {
            val name = item.name.lowercase()
            name.contains("greatsword") || name.contains("greataxe") || name.contains("warbow") ||
                name.contains("maul") || name.contains("greatclub") || name.contains("warblade")
        }
    }

    fun isShield(item: ItemEntity): Boolean =
        item.slot == ItemSlot.TRINKET && WearArchetype.resolve(item) == WearArchetype.SHIELD

    fun canEquipTogether(weapon: ItemEntity?, accessory: ItemEntity?): Boolean {
        if (weapon == null || accessory == null) return true
        if (!isShield(accessory)) return true
        return !isTwoHandedWeapon(weapon)
    }

    fun shieldBlockedReason(weapon: ItemEntity?): String? =
        if (weapon != null && isTwoHandedWeapon(weapon)) {
            "Unequip your two-handed weapon to use a shield."
        } else {
            null
        }
}
