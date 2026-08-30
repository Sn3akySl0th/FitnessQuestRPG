package com.fitnessquest.rpg.domain.visuals

import androidx.compose.ui.graphics.Color
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.ItemStyle

/**
 * Canonical weapon identity table: every catalog weapon resolves to a distinct
 * archetype, palette, and head motif for icon + avatar rendering.
 */
object WeaponVisualCatalog {

    data class Recipe(
        val archetype: VisualArchetype,
        val motif: WeaponHeadMotif = WeaponHeadMotif.NONE,
        val primary: Color? = null,
        val secondary: Color? = null,
        val accent: Color? = null,
        val glow: Color? = null,
    )

    private val recipes: Map<String, Recipe> = mapOf(
        // Warrior
        "rusty_sword" to Recipe(VisualArchetype.SWORD_GENERIC, WeaponHeadMotif.RUST_PATCHES, Color(0xFF8B7355), Color(0xFF5C4A32), Color(0xFFB8860B)),
        "iron_mace" to Recipe(VisualArchetype.MACE_GENERIC, WeaponHeadMotif.SPIKED_STAR, Color(0xFF6B5138), Color(0xFF9AA3AD), Color(0xFF64748B)),
        "knights_blade" to Recipe(VisualArchetype.SWORD_KNIGHT, WeaponHeadMotif.NONE, Color(0xFF475569), Color(0xFF94A3B8), Color(0xFFF59E0B), Color(0xFF06B6D4)),
        "dragonfang_greatsword" to Recipe(VisualArchetype.GREATSWORD_DRAGON, WeaponHeadMotif.DRAGON_SCALE, Color(0xFF0F172A), Color(0xFFF1F5F9), Color(0xFF991B1B), Color(0xFFEF4444)),

        // Mage / caster universal
        "apprentice_wand" to Recipe(VisualArchetype.WAND_ARCANE, WeaponHeadMotif.MANA_CRYSTAL, Color(0xFF8B6B4A), Color(0xFF6B5138), glow = Color(0xFF38BDF8)),
        "oakheart_staff" to Recipe(VisualArchetype.STAFF_WOODEN, WeaponHeadMotif.OAK_LEAVES, Color(0xFF5D4037), Color(0xFF3E2723), glow = Color(0xFF4ADE80)),
        "staff_of_storms" to Recipe(VisualArchetype.STAFF_WOODEN, WeaponHeadMotif.STORM_ORB, Color(0xFF334155), Color(0xFF1E293B), glow = Color(0xFF38BDF8)),
        "archmages_scepter" to Recipe(VisualArchetype.SCEPTER, WeaponHeadMotif.MANA_CRYSTAL, Color(0xFF6B5138), Color(0xFFD4AF37), glow = Color(0xFFC084FC)),

        // Rogue
        "bent_shiv" to Recipe(VisualArchetype.DAGGER_GENERIC, WeaponHeadMotif.RUST_PATCHES, Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF475569)),
        "twin_fangs" to Recipe(VisualArchetype.DAGGER_GENERIC, WeaponHeadMotif.TWIN_BLADES, Color(0xFF475569), Color(0xFFCBD5E1), Color(0xFF06B6D4)),
        "nightpiercer" to Recipe(VisualArchetype.DAGGER_GENERIC, WeaponHeadMotif.NIGHT_CRESCENT, Color(0xFF312E81), Color(0xFF1E1B4B), glow = Color(0xFF818CF8)),
        "whisper_of_the_void" to Recipe(VisualArchetype.DAGGER_GENERIC, WeaponHeadMotif.VOID_TEAR, Color(0xFF1E1B4B), Color(0xFF0F172A), glow = Color(0xFF7C3AED)),

        // Ranger bows
        "training_shortbow" to Recipe(VisualArchetype.BOW_SHORT, WeaponHeadMotif.NONE, Color(0xFF78350F), Color(0xFFB45309), Color(0xFFE2E8F0), Color(0xFF10B981)),
        "hunters_recurve" to Recipe(VisualArchetype.BOW_RECURVE, WeaponHeadMotif.NONE, Color(0xFF92400E), Color(0xFFD97706), Color(0xFFE2E8F0), Color(0xFF22C55E)),
        "hawkeye_longbow" to Recipe(VisualArchetype.BOW_LONGBOW, WeaponHeadMotif.NONE, Color(0xFF365314), Color(0xFF4D7C0F), Color(0xFFE2E8F0), Color(0xFF84CC16)),
        "worldroot_warbow" to Recipe(VisualArchetype.BOW_WARBOW, WeaponHeadMotif.ROOT_CROWN, Color(0xFF14532D), Color(0xFF166534), Color(0xFFE2E8F0), Color(0xFF4ADE80)),

        // Necromancer
        "bone_staff" to Recipe(VisualArchetype.STAFF_NECRO, WeaponHeadMotif.SKULL_BONE, Color(0xFFE8E0D0), Color(0xFF9A9080), glow = Color(0xFF7CF0C0)),
        "graveoak_staff" to Recipe(VisualArchetype.STAFF_NECRO, WeaponHeadMotif.GRAVE_WOOD, Color(0xFF3F3A32), Color(0xFF1F1A14), glow = Color(0xFF4ADE80)),
        "soulflayer" to Recipe(VisualArchetype.STAFF_NECRO, WeaponHeadMotif.SKULL_SOUL, Color(0xFFD8D0C0), Color(0xFF6B5E50), glow = Color(0xFF7CF0C0)),
        "eclipse_phylactery" to Recipe(VisualArchetype.STAFF_NECRO, WeaponHeadMotif.PHYLACTERY, Color(0xFF2A1E38), Color(0xFF120A1C), glow = Color(0xFFA78BFA)),

        // Paladin
        "novice_oathblade" to Recipe(VisualArchetype.SWORD_GENERIC, WeaponHeadMotif.HOLY_CROSS, Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFFDE047), Color(0xFFFBBF24)),
        "tempered_oathblade" to Recipe(VisualArchetype.SWORD_KNIGHT, WeaponHeadMotif.HOLY_CROSS, Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFFF59E0B), Color(0xFFFDE047)),
        "radiant_crusader_blade" to Recipe(VisualArchetype.SWORD_KNIGHT, WeaponHeadMotif.HOLY_CROSS, Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFFFBBF24), Color(0xFFFDE047)),
        "dawnkeeper" to Recipe(VisualArchetype.GREATSWORD_GENERIC, WeaponHeadMotif.RADIANT_SUN, Color(0xFFFFF7ED), Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFFDE047)),

        // War Monk / healer
        "candlelit_wand" to Recipe(VisualArchetype.WAND_ARCANE, WeaponHeadMotif.CANDLE_FLAME, Color(0xFF8B6B4A), Color(0xFF5C4033), glow = Color(0xFFFBBF24)),
        "sanctuary_rod" to Recipe(VisualArchetype.STAFF_WOODEN, WeaponHeadMotif.SANCTUARY_GEM, Color(0xFF6B7280), Color(0xFF374151), glow = Color(0xFF93C5FD)),
        "mercybrand_staff" to Recipe(VisualArchetype.STAFF_WOODEN, WeaponHeadMotif.MERCY_CROSS, Color(0xFFE5E7EB), Color(0xFF9CA3AF), glow = Color(0xFFFDE047)),
        "seraphic_scepter" to Recipe(VisualArchetype.SCEPTER, WeaponHeadMotif.HOLY_CROSS, Color(0xFFF8FAFC), Color(0xFFD4AF37), glow = Color(0xFFFDE047)),

        // Monk
        "novice_handwraps" to Recipe(VisualArchetype.UNARMED_WRAP, WeaponHeadMotif.NONE, Color(0xFF8B5E3C), Color(0xFF4A3525), Color(0xFFD97706)),
        "iron_palm_bands" to Recipe(VisualArchetype.UNARMED_WRAP, WeaponHeadMotif.NONE, Color(0xFF64748B), Color(0xFF334155), Color(0xFF06B6D4)),
        "stormstep_tonfa" to Recipe(VisualArchetype.TONFA, WeaponHeadMotif.NONE, Color(0xFF5D4037), Color(0xFF2C241E), Color(0xFF06B6D4)),
        "dragon_palm_relics" to Recipe(VisualArchetype.UNARMED_WRAP, WeaponHeadMotif.DRAGON_SCALE, Color(0xFF7F1D1D), Color(0xFF450A0A), Color(0xFFF97316), Color(0xFFEF4444)),

        // Druid
        "sproutwood_cane" to Recipe(VisualArchetype.STAFF_DRUID, WeaponHeadMotif.LEAF_SPROUT, Color(0xFF6B8F4E), Color(0xFF3F5F2F), glow = Color(0xFF86EFAC)),
        "briarroot_staff" to Recipe(VisualArchetype.STAFF_DRUID, WeaponHeadMotif.OAK_LEAVES, Color(0xFF4D7C0F), Color(0xFF365314), glow = Color(0xFF4ADE80)),
        "moonwell_crook" to Recipe(VisualArchetype.STAFF_DRUID, WeaponHeadMotif.BRANCH_HOOK, Color(0xFF5B4B8A), Color(0xFF312E81), glow = Color(0xFFC4B5FD)),
        "worldroot_elderstaff" to Recipe(VisualArchetype.STAFF_DRUID, WeaponHeadMotif.ROOT_CROWN, Color(0xFF166534), Color(0xFF14532D), glow = Color(0xFF22C55E)),

        // Berserker
        "chipped_war_axe" to Recipe(VisualArchetype.AXE, WeaponHeadMotif.RUST_PATCHES, Color(0xFF5D4037), Color(0xFF94A3B8), Color(0xFFB91C1C)),
        "ragecleaver" to Recipe(VisualArchetype.AXE, WeaponHeadMotif.NONE, Color(0xFF6B4423), Color(0xFFDC2626), Color(0xFFF97316), Color(0xFFEF4444)),
        "blood_iron_maul" to Recipe(VisualArchetype.HAMMER, WeaponHeadMotif.NONE, Color(0xFF5D4037), Color(0xFF64748B), Color(0xFFB91C1C)),
        "riftbreaker_greataxe" to Recipe(VisualArchetype.AXE, WeaponHeadMotif.STORM_ORB, Color(0xFF1E293B), Color(0xFFDC2626), Color(0xFFF97316), Color(0xFFF59E0B)),

        // Bard
        "practice_lute" to Recipe(VisualArchetype.INSTRUMENT, WeaponHeadMotif.HARP_STRINGS, Color(0xFF8B5A2B), Color(0xFF4A2F1B), Color(0xFFF59E0B)),
        "resonant_baton" to Recipe(VisualArchetype.INSTRUMENT, WeaponHeadMotif.BATON_RINGS, Color(0xFF6B7280), Color(0xFF374151), Color(0xFF06B6D4)),
        "virtuosos_rapier" to Recipe(VisualArchetype.SWORD_RAPIER, WeaponHeadMotif.NONE, Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFD4AF37)),
        "finale_harpblade" to Recipe(VisualArchetype.SWORD_GENERIC, WeaponHeadMotif.HARP_STRINGS, Color(0xFF7C3AED), Color(0xFFCBD5E1), Color(0xFFF59E0B), Color(0xFFC084FC)),

        // Summoner
        "novice_horn" to Recipe(VisualArchetype.INSTRUMENT, WeaponHeadMotif.NONE, Color(0xFF8B5A2B), Color(0xFF4A2F1B), Color(0xFFF59E0B)),
        "primal_flute" to Recipe(VisualArchetype.INSTRUMENT, WeaponHeadMotif.NONE, Color(0xFF0D9488), Color(0xFF134E4A), Color(0xFF5EEAD4)),
        "callers_staff" to Recipe(VisualArchetype.STAFF_WOODEN, WeaponHeadMotif.CALLER_GEM, Color(0xFF6B5138), Color(0xFF3E2723), glow = Color(0xFF38BDF8)),
        "apocalypse_horn" to Recipe(VisualArchetype.INSTRUMENT, WeaponHeadMotif.APOCALYPSE_BELL, Color(0xFF312E81), Color(0xFF1E1B4B), glow = Color(0xFFA78BFA)),

        // Dragoon
        "worn_harpoon" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.RUST_PATCHES, Color(0xFF6B4423), Color(0xFFCBD5E1), Color(0xFF64748B)),
        "iron_lance" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.NONE, Color(0xFF475569), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
        "wyrmslayer_spear" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.DRAGON_SCALE, Color(0xFF334155), Color(0xFF94A3B8), Color(0xFFEF4444), Color(0xFFF97316)),
        "gae_bolg" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.DRAGON_SCALE, Color(0xFF7F1D1D), Color(0xFFDC2626), Color(0xFFF59E0B), Color(0xFFEF4444)),

        // Universal extras
        "training_spear" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.NONE, Color(0xFF6B4423), Color(0xFFCBD5E1), Color(0xFFB07E24)),
        "balanced_shortsword" to Recipe(VisualArchetype.SWORD_GENERIC, WeaponHeadMotif.NONE, Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFFB07E24)),
        "mercenary_halberd" to Recipe(VisualArchetype.SPEAR_GENERIC, WeaponHeadMotif.NONE, Color(0xFF475569), Color(0xFFCBD5E1), Color(0xFFB07E24)),
        "starforged_relic_blade" to Recipe(VisualArchetype.GREATSWORD_GENERIC, WeaponHeadMotif.STAR_FORGE, Color(0xFF312E81), Color(0xFFFDE047), Color(0xFFC084FC), Color(0xFFF8D24A)),
        "travelers_staff" to Recipe(VisualArchetype.STAFF_TRAVELER, WeaponHeadMotif.NONE, Color(0xFF6B4E37), Color(0xFF4A3525), Color(0xFF8A7A68)),
        "novice_bludgeon" to Recipe(VisualArchetype.CLUB, WeaponHeadMotif.NONE, Color(0xFF6B4423), Color(0xFF3E2723), Color(0xFFD97706)),
        "focusing_wand" to Recipe(VisualArchetype.ARCANE_FOCUS, WeaponHeadMotif.GLASS_ORB, Color(0xFF475569), Color(0xFF94A3B8), glow = Color(0xFF38BDF8)),
        "runed_quarterstaff" to Recipe(VisualArchetype.STAFF_RUNED, WeaponHeadMotif.RUNE_BANDS, Color(0xFF5D4037), Color(0xFF3E2723), glow = Color(0xFFC084FC)),
        "composite_longbow" to Recipe(VisualArchetype.BOW_LONGBOW, WeaponHeadMotif.NONE, Color(0xFF78350F), Color(0xFFB45309), Color(0xFFE2E8F0), Color(0xFF60A5FA)),
        "steel_morningstar" to Recipe(VisualArchetype.MACE_GENERIC, WeaponHeadMotif.SPIKED_STAR, Color(0xFF64748B), Color(0xFF334155), Color(0xFF94A3B8)),
        "gilded_wand" to Recipe(VisualArchetype.WAND_ARCANE, WeaponHeadMotif.GOLD_FILIGREE, Color(0xFF8B6B4A), Color(0xFF6B5138), Color(0xFFFBBF24), Color(0xFFFDE047)),
        "duelists_rapier" to Recipe(VisualArchetype.SWORD_RAPIER, WeaponHeadMotif.NONE, Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFD4AF37)),
        "woodcutters_axe" to Recipe(VisualArchetype.AXE, WeaponHeadMotif.NONE, Color(0xFF6B4423), Color(0xFF94A3B8), Color(0xFF78350F)),
    )

    fun resolve(item: ItemEntity): EquipmentVisualDescriptor? {
        if (item.slot != ItemSlot.WEAPON) return null
        val itemKey = EquipmentVisualRegistry.normalizeItemKey(item.name)
        val nameLower = item.name.lowercase()
        val recipe = recipes[itemKey] ?: inferFromName(item, itemKey, nameLower)
        val palette = UniqueGearPalettes.forItem(item, itemKey)
        return EquipmentVisualDescriptor(
            canonicalKey = itemKey,
            archetype = recipe.archetype,
            primaryColor = recipe.primary ?: palette.primary,
            secondaryColor = recipe.secondary ?: palette.secondary,
            accentColor = recipe.accent ?: palette.accent,
            glowColor = recipe.glow ?: palette.glow,
            weaponHeadMotif = recipe.motif,
        )
    }

    private fun inferFromName(item: ItemEntity, itemKey: String, nameLower: String): Recipe {
        val archetype = when {
            listOf("handwrap", "palm band", "palm relic").any(nameLower::contains) ->
                VisualArchetype.UNARMED_WRAP
            nameLower.contains("tonfa") -> VisualArchetype.TONFA
            listOf("lute", "horn", "flute", "baton").any(nameLower::contains) ->
                VisualArchetype.INSTRUMENT
            nameLower.contains("scythe") -> VisualArchetype.SCYTHE
            listOf("axe", "cleaver").any(nameLower::contains) -> VisualArchetype.AXE
            nameLower.contains("maul") -> VisualArchetype.HAMMER
            listOf("club", "bludgeon").any(nameLower::contains) -> VisualArchetype.CLUB
            nameLower.contains("scepter") -> VisualArchetype.SCEPTER
            nameLower.contains("focus") -> VisualArchetype.ARCANE_FOCUS
            listOf("spear", "lance", "pike", "halberd", "harpoon").any(nameLower::contains) ->
                VisualArchetype.SPEAR_GENERIC
            nameLower.contains("rapier") -> VisualArchetype.SWORD_RAPIER
            nameLower.contains("recurve") -> VisualArchetype.BOW_RECURVE
            nameLower.contains("longbow") || nameLower.contains("warbow") -> VisualArchetype.BOW_LONGBOW
            nameLower.contains("shortbow") || nameLower.contains("training bow") -> VisualArchetype.BOW_SHORT
            nameLower.contains("traveler") && nameLower.contains("staff") -> VisualArchetype.STAFF_TRAVELER
            nameLower.contains("apprentice wand") || nameLower.contains("novice wand") ->
                VisualArchetype.WAND_ARCANE
            nameLower.contains("dragonfang") && item.style == ItemStyle.GREATSWORD ->
                VisualArchetype.GREATSWORD_DRAGON
            nameLower.contains("knight") && item.slot == ItemSlot.WEAPON ->
                VisualArchetype.SWORD_KNIGHT
            item.style == ItemStyle.GREATSWORD -> VisualArchetype.GREATSWORD_GENERIC
            item.style == ItemStyle.SWORD -> VisualArchetype.SWORD_GENERIC
            item.style == ItemStyle.BOW -> VisualArchetype.BOW_GENERIC
            item.style == ItemStyle.DAGGER -> VisualArchetype.DAGGER_GENERIC
            item.style == ItemStyle.MACE -> VisualArchetype.MACE_GENERIC
            item.style == ItemStyle.WAND -> VisualArchetype.WAND_ARCANE
            item.style == ItemStyle.STAFF -> VisualArchetype.STAFF_WOODEN
            else -> VisualArchetype.GENERIC
        }
        val motif = when {
            nameLower.contains("skull") || nameLower.contains("bone staff") -> WeaponHeadMotif.SKULL_BONE
            nameLower.contains("phylactery") -> WeaponHeadMotif.PHYLACTERY
            nameLower.contains("storm") -> WeaponHeadMotif.STORM_ORB
            nameLower.contains("gilded") -> WeaponHeadMotif.GOLD_FILIGREE
            nameLower.contains("twin") && nameLower.contains("fang") -> WeaponHeadMotif.TWIN_BLADES
            else -> WeaponHeadMotif.NONE
        }
        return Recipe(archetype, motif)
    }
}
