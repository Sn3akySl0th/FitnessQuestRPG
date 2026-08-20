package com.fitnessquest.rpg.domain.visuals

import android.content.Context
import androidx.annotation.DrawableRes
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.ItemStyle

/**
 * Distinct visual targets for paper-doll rendering.
 * Note: While the domain model currently exposes [ItemSlot.TRINKET], visual rendering
 * distinguishes between back-layer wearables (capes, towels, cloaks) and front-layer
 * accessories (medals, belts, pendants).
 */
enum class PaperDollVisualSlot {
    WEAPON,
    HEAD,
    CHEST,
    HANDS,
    LEGS,
    FEET,
    BACK,
    TRINKET;

    companion object {
        fun fromDomainSlot(slot: ItemSlot, itemStyle: String = "", itemName: String = ""): PaperDollVisualSlot {
            val nameLower = itemName.lowercase()
            val styleLower = itemStyle.lowercase()
            val isBackWearable = nameLower.contains("cape") ||
                nameLower.contains("cloak") ||
                nameLower.contains("towel") ||
                nameLower.contains("quiver") ||
                nameLower.contains("wings") ||
                nameLower.contains("mantle") ||
                styleLower == "cape" ||
                styleLower == "cloak"

            return when (slot) {
                ItemSlot.WEAPON -> WEAPON
                ItemSlot.HEAD -> HEAD
                ItemSlot.CHEST -> if (isBackWearable) BACK else CHEST
                ItemSlot.HANDS -> HANDS
                ItemSlot.LEGS -> LEGS
                ItemSlot.FEET -> FEET
                ItemSlot.TRINKET -> if (isBackWearable) BACK else TRINKET
                else -> TRINKET
            }
        }
    }
}

/**
 * Procedural material and rarity dyes for Diablo-style visual customization.
 */
enum class EquipmentDye(
    val label: String,
    val tintColor: androidx.compose.ui.graphics.Color?
) {
    NATURAL("Original", null),
    IRON("Polished Iron", androidx.compose.ui.graphics.Color(0xFF94A3B8)),
    GOLD("Burnished Gold", androidx.compose.ui.graphics.Color(0xFFF59E0B)),
    BLOOD_STEEL("Blood Steel", androidx.compose.ui.graphics.Color(0xFFE11D48)),
    GLACIAL("Glacial Frost", androidx.compose.ui.graphics.Color(0xFF38BDF8)),
    VOID("Void Shadow", androidx.compose.ui.graphics.Color(0xFFA855F7)),
    VERDANT("Emerald Wood", androidx.compose.ui.graphics.Color(0xFF10B981)),
    CELESTIAL("Celestial Dawn", androidx.compose.ui.graphics.Color(0xFFFEF08A));

    companion object {
        fun fromItem(item: ItemEntity): EquipmentDye {
            val nameLower = item.name.lowercase()
            return when {
                nameLower.contains("gold") || nameLower.contains("dawn") || nameLower.contains("solar") -> GOLD
                nameLower.contains("blood") || nameLower.contains("rage") || nameLower.contains("ember") || nameLower.contains("fury") -> BLOOD_STEEL
                nameLower.contains("frost") || nameLower.contains("glacial") || nameLower.contains("freeze") || nameLower.contains("storm") -> GLACIAL
                nameLower.contains("void") || nameLower.contains("shadow") || nameLower.contains("soul") || nameLower.contains("eclipse") -> VOID
                nameLower.contains("emerald") || nameLower.contains("wild") || nameLower.contains("oak") || nameLower.contains("briar") -> VERDANT
                nameLower.contains("celestial") || nameLower.contains("seraph") || nameLower.contains("radiant") -> CELESTIAL
                nameLower.contains("iron") || nameLower.contains("steel") || nameLower.contains("plate") -> IRON
                else -> NATURAL
            }
        }
    }
}

/**
 * Standardized 14-layer Z-index stack for 2D paper-doll rendering.
 */
enum class PaperDollLayerOrder(val zIndex: Int, val layerTag: String) {
    BG_PEDESTAL(0, "bg_pedestal"),
    FX_AURA_BACK(1, "fx_aura_back"),
    GEAR_BACK(2, "gear_back"),
    BODY_BASE(3, "body_base"),
    BODY_HAIR_BACK(4, "body_hair_back"),
    GEAR_LEGS(5, "gear_legs"),
    GEAR_FEET(6, "gear_feet"),
    GEAR_TORSO(7, "gear_torso"),
    GEAR_HANDS(8, "gear_hands"),
    BODY_HAIR_FRONT(9, "body_hair_front"),
    GEAR_HEAD(10, "gear_head"),
    GEAR_TRINKET(11, "gear_trinket"),
    GEAR_WEAPON(12, "gear_weapon"),
    FX_AURA_FRONT(13, "fx_aura_front");

    companion object {
        fun forVisualSlot(slot: PaperDollVisualSlot): PaperDollLayerOrder = when (slot) {
            PaperDollVisualSlot.BACK -> GEAR_BACK
            PaperDollVisualSlot.LEGS -> GEAR_LEGS
            PaperDollVisualSlot.CHEST -> GEAR_TORSO
            PaperDollVisualSlot.FEET -> GEAR_FEET
            PaperDollVisualSlot.HANDS -> GEAR_HANDS
            PaperDollVisualSlot.HEAD -> GEAR_HEAD
            PaperDollVisualSlot.WEAPON -> GEAR_WEAPON
            PaperDollVisualSlot.TRINKET -> GEAR_TRINKET
        }
    }
}

/**
 * Positioning and alignment anchor for visual items.
 */
data class VisualAnchor(
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val rotationDegrees: Float = 0f,
    val scale: Float = 1f
)

/**
 * Metadata mapping a game item to its 2D sprite layers and icon assets.
 */
data class EquipmentVisualSpec(
    val itemKey: String,
    val domainSlot: ItemSlot,
    val visualSlot: PaperDollVisualSlot,
    val layerResName: String?,
    val iconResName: String?,
    val glowResName: String? = null,
    val layerOrder: PaperDollLayerOrder,
    val anchor: VisualAnchor? = null,
    val supportsRarityTint: Boolean = true,
    @get:DrawableRes val layerResId: Int? = null,
    @get:DrawableRes val iconResId: Int? = null,
    @get:DrawableRes val glowResId: Int? = null
)

/**
 * Central registry that resolves 2D asset specs and drawable resources for equipment.
 *
 * Current Phase: Placeholder / No-Asset Mode
 * - Centralizes dynamic name resolution and caches resolved IDs.
 * - Missing asset names safely resolve to null.
 * - Zero ID is never returned or passed to painterResource.
 * - Entire app builds and runs without any PNG/XML artist assets present.
 *
 * TODO (Production Asset Phase):
 * Dynamic `resources.getIdentifier()` is NOT guaranteed safe with Android R8 resource shrinking
 * unless an explicit `tools:keep` rule is maintained in `res/raw/keep.xml`.
 * When real 2D artwork is checked into `res/drawable`, replace dynamic name lookup with a
 * compile-time map of actual existing `R.drawable` IDs (or maintain `res/raw/keep.xml`)
 * to ensure release build stability and prevent resource stripping.
 */
object EquipmentVisualRegistry {

    private val specCache = java.util.concurrent.ConcurrentHashMap<Long, EquipmentVisualSpec>()
    private val drawableIdCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    // Known anchor presets for fitness-fantasy gear
    private val WeaponAnchor = VisualAnchor(xFraction = 0.76f, yFraction = 0.48f, rotationDegrees = 0f)
    private val HeadAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.17f)
    private val TorsoAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.37f)
    private val HandsAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.50f)
    private val LegsAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.62f)
    private val FeetAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.87f)
    private val BackAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.40f)
    private val TrinketAnchor = VisualAnchor(xFraction = 0.50f, yFraction = 0.42f)

    /**
     * Resolves and caches the visual specification for a given [ItemEntity].
     */
    fun resolveSpec(item: ItemEntity): EquipmentVisualSpec {
        return specCache.getOrPut(item.id) {
            val visualSlot = PaperDollVisualSlot.fromDomainSlot(item.slot, item.style, item.name)
            val layerOrder = PaperDollLayerOrder.forVisualSlot(visualSlot)
            val itemKey = normalizeItemKey(item.name)
            val tierSuffix = "t${item.tier.coerceIn(1, 5)}"

            val slotKey = visualSlot.name.lowercase()
            val layerResName = "layer_${slotKey}_${itemKey}_${tierSuffix}"
            val iconResName = "icon_${slotKey}_${itemKey}_${tierSuffix}"
            val glowResName = "icon_${slotKey}_${itemKey}_${tierSuffix}_glow"

            val anchor = when (visualSlot) {
                PaperDollVisualSlot.WEAPON -> WeaponAnchor
                PaperDollVisualSlot.HEAD -> HeadAnchor
                PaperDollVisualSlot.CHEST -> TorsoAnchor
                PaperDollVisualSlot.HANDS -> HandsAnchor
                PaperDollVisualSlot.LEGS -> LegsAnchor
                PaperDollVisualSlot.FEET -> FeetAnchor
                PaperDollVisualSlot.BACK -> BackAnchor
                PaperDollVisualSlot.TRINKET -> TrinketAnchor
            }

            EquipmentVisualSpec(
                itemKey = itemKey,
                domainSlot = item.slot,
                visualSlot = visualSlot,
                layerResName = layerResName,
                iconResName = iconResName,
                glowResName = glowResName,
                layerOrder = layerOrder,
                anchor = anchor,
                supportsRarityTint = true
            )
        }
    }

    /**
     * Converts item names into snake_case keys (e.g. "Dumbbell Hammer" -> "dumbbell_hammer").
     */
    fun normalizeItemKey(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    /**
     * Safely attempts to resolve and cache a drawable resource ID by name without crashing if absent.
     * Returns null if the resource does not exist. Never returns 0.
     */
    @DrawableRes
    fun findDrawableId(context: Context, resName: String?): Int? {
        if (resName.isNullOrBlank()) return null
        val cached = drawableIdCache[resName]
        if (cached != null) {
            return if (cached != 0) cached else null
        }
        val resolvedId = try {
            val id = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (id != 0) {
                // Verify the resource is actually loadable (Compose Preview / Layoutlib returns synthetic IDs for missing assets)
                context.resources.openRawResource(id).use { }
                id
            } else {
                0
            }
        } catch (_: Throwable) {
            0
        }
        drawableIdCache[resName] = resolvedId
        return if (resolvedId != 0) resolvedId else null
    }
}
