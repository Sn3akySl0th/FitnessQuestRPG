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
                nameLower.contains("shield") ||
                styleLower == "cape" ||
                styleLower == "cloak" ||
                styleLower == "quiver" ||
                styleLower == "shield" ||
                styleLower == "wings"

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
 * Character viewing orientation for paper-doll rendering and turnarounds.
 */
enum class AvatarOrientation {
    FRONT,
    BACK;

    val isFacingBack: Boolean get() = this == BACK

    companion object {
        fun fromFacingBack(facingBack: Boolean): AvatarOrientation =
            if (facingBack) BACK else FRONT
    }
}

/**
 * Orientation-specific visibility rule for 2D paper-doll layers.
 */
enum class VisualFacingRule {
    BOTH,
    FRONT_ONLY,
    BACK_ONLY
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

    /**
     * Resolves the effective Z-index according to the current viewing orientation.
     * On back view, GEAR_BACK (capes/wings/shields) drapes in front of the torso/head.
     */
    fun resolvedZIndex(orientation: AvatarOrientation): Int = when (orientation) {
        AvatarOrientation.FRONT -> when (this) {
            BG_PEDESTAL -> 0
            FX_AURA_BACK -> 1
            GEAR_BACK -> 2
            BODY_BASE -> 3
            BODY_HAIR_BACK -> 4
            GEAR_LEGS -> 5
            GEAR_FEET -> 6
            GEAR_TORSO -> 7
            GEAR_HANDS -> 8
            BODY_HAIR_FRONT -> 9
            GEAR_HEAD -> 10
            GEAR_TRINKET -> 11
            GEAR_WEAPON -> 12
            FX_AURA_FRONT -> 13
        }
        AvatarOrientation.BACK -> when (this) {
            BG_PEDESTAL -> 0
            FX_AURA_BACK -> 1
            BODY_BASE -> 3
            BODY_HAIR_BACK -> 4
            GEAR_LEGS -> 5
            GEAR_FEET -> 6
            GEAR_TORSO -> 7
            GEAR_HANDS -> 8
            BODY_HAIR_FRONT -> 9
            GEAR_HEAD -> 10
            GEAR_BACK -> 11
            GEAR_WEAPON -> 12
            GEAR_TRINKET -> 13
            FX_AURA_FRONT -> 14
        }
    }

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
 * Concrete resolution mode for an equipment item in a specific orientation.
 */
sealed interface EquipmentLayerResolution {
    /** Item is completely suppressed in this orientation (e.g. FRONT_ONLY items on back, or hidden by robe/slots). */
    object Hidden : EquipmentLayerResolution

    /** Item has an explicit 2D raster/vector drawable asset for this orientation. */
    data class Drawable(
        @get:DrawableRes val drawableResId: Int,
        val resName: String
    ) : EquipmentLayerResolution

    /** Item is active in this orientation but has no 2D asset, requiring procedural Canvas fallback rendering. */
    object CanvasFallback : EquipmentLayerResolution
}

/**
 * Structural render pass relative to the base avatar body canvas.
 */
enum class PaperDollRenderPass {
    /** Rendered behind the CharacterAvatar canvas (e.g. Front Capes, Wings). */
    BEHIND_BODY,
    /** Rendered on top of the CharacterAvatar canvas (e.g. Torso armor, Helms, Back Capes). */
    FOREGROUND
}

/**
 * Encapsulates a resolved 2D layer for rendering in the paper doll stack.
 */
data class ResolvedPaperDollLayer(
    val order: PaperDollLayerOrder,
    val visualSlot: PaperDollVisualSlot?,
    val spec: EquipmentVisualSpec?,
    @get:DrawableRes val drawableResId: Int,
    val resName: String,
    val item: ItemEntity? = null,
    val isEquipped: Boolean = true,
    val rarity: com.fitnessquest.rpg.domain.GearRarity = com.fitnessquest.rpg.domain.GearRarity.COMMON,
    val dye: EquipmentDye = EquipmentDye.NATURAL,
    val orientation: AvatarOrientation = AvatarOrientation.FRONT
)

/**
 * An individual entry in the strictly ordered paper-doll render stack.
 */
sealed interface PaperDollRenderEntry {
    val zIndex: Int
    val order: PaperDollLayerOrder

    /** Aura, shadow, and class silhouette rendered behind all character gear and body. */
    data class AvatarBackdrop(
        override val zIndex: Int = PaperDollLayerOrder.FX_AURA_BACK.zIndex
    ) : PaperDollRenderEntry {
        override val order: PaperDollLayerOrder get() = PaperDollLayerOrder.FX_AURA_BACK
    }

    /** The base humanoid/druid avatar body (skin, neck, head base, face, hair base, underwear, muscles). */
    data class BodyBase(
        override val zIndex: Int = PaperDollLayerOrder.BODY_BASE.zIndex
    ) : PaperDollRenderEntry {
        override val order: PaperDollLayerOrder get() = PaperDollLayerOrder.BODY_BASE
    }

    /** A 2D raster/vector drawable equipment layer. */
    data class DrawableLayer(
        val layer: ResolvedPaperDollLayer,
        override val zIndex: Int
    ) : PaperDollRenderEntry {
        override val order: PaperDollLayerOrder get() = layer.order
    }

    /** A procedural Canvas-rendered equipment slot (when 2D asset is absent/fallback). */
    data class CanvasFallbackSlot(
        val slot: ItemSlot,
        val item: ItemEntity,
        val spec: EquipmentVisualSpec,
        val visualSlot: PaperDollVisualSlot,
        override val zIndex: Int
    ) : PaperDollRenderEntry {
        override val order: PaperDollLayerOrder get() = spec.layerOrder
    }
}

/**
 * Encapsulates the complete, pure, orientation-resolved plan for the paper doll stack.
 */
data class ResolvedPaperDollPassPlan(
    val entries: List<PaperDollRenderEntry>,
    val hiddenBodyRegions: Set<BodyRegion>,
    val orientation: AvatarOrientation
) {
    val behindBodyLayers: List<ResolvedPaperDollLayer>
        get() = entries.filterIsInstance<PaperDollRenderEntry.DrawableLayer>()
            .map { it.layer }
            .filter { it.order.resolvedZIndex(orientation) < PaperDollLayerOrder.BODY_BASE.resolvedZIndex(orientation) }

    val foregroundLayers: List<ResolvedPaperDollLayer>
        get() = entries.filterIsInstance<PaperDollRenderEntry.DrawableLayer>()
            .map { it.layer }
            .filter { it.order.resolvedZIndex(orientation) >= PaperDollLayerOrder.BODY_BASE.resolvedZIndex(orientation) }

    val canvasFallbackGear: Map<ItemSlot, ItemEntity>
        get() = entries.filterIsInstance<PaperDollRenderEntry.CanvasFallbackSlot>()
            .associate { it.slot to it.item }
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

/** Physical relationship between an equipment visual and the avatar. */
enum class VisualAttachmentMode {
    BODY_WORN,
    HELD,
    SIDE_CARRY,
    BACK_MOUNTED,
    BACK_FEATURE,
    HIDDEN
}

/** Controls whether a non-feature item may compete with capes, wings, or shields. */
enum class BackOcclusionPolicy {
    ALWAYS_VISIBLE,
    HIDE_WHEN_BACK_FEATURE,
    ALWAYS_HIDDEN
}

/** Orientation-specific placement contract shared by drawable and Canvas renderers. */
data class OrientationPresentation(
    val attachment: VisualAttachmentMode,
    val anchor: VisualAnchor,
    val occlusionPolicy: BackOcclusionPolicy = BackOcclusionPolicy.ALWAYS_VISIBLE
)

data class EquipmentOrientationContract(
    val front: OrientationPresentation,
    val back: OrientationPresentation
) {
    fun forOrientation(orientation: AvatarOrientation): OrientationPresentation =
        if (orientation == AvatarOrientation.FRONT) front else back
}

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
    val coverageProfile: CoverageProfile = CoverageProfile.NONE,
    val coveredRegions: Set<BodyRegion> = emptySet(),
    val hiddenVisualSlots: Set<PaperDollVisualSlot> = emptySet(),
    val facingRule: VisualFacingRule = VisualFacingRule.BOTH,
    val backLayerResName: String? = null,
    val orientationContract: EquipmentOrientationContract,
    @get:DrawableRes val layerResId: Int? = null,
    @get:DrawableRes val iconResId: Int? = null,
    @get:DrawableRes val glowResId: Int? = null
)

/**
 * Helpers for default body region coverage and slot suppression per profile.
 */
fun CoverageProfile.defaultCoveredRegions(): Set<BodyRegion> = when (this) {
    CoverageProfile.NONE -> emptySet()
    CoverageProfile.CAP -> setOf(BodyRegion.SCALP, BodyRegion.FOREHEAD)
    CoverageProfile.HOOD -> setOf(BodyRegion.SCALP, BodyRegion.FOREHEAD, BodyRegion.NECK)
    CoverageProfile.CLOSED_HELM -> setOf(BodyRegion.SCALP, BodyRegion.FOREHEAD, BodyRegion.NECK)
    CoverageProfile.SLEEVELESS_VEST -> setOf(BodyRegion.TORSO, BodyRegion.HIPS)
    CoverageProfile.SLEEVED_TUNIC -> setOf(
        BodyRegion.TORSO,
        BodyRegion.HIPS
    )
    CoverageProfile.HEAVY_PLATE -> setOf(
        BodyRegion.TORSO,
        BodyRegion.HIPS
    )
    CoverageProfile.BRACERS -> setOf(BodyRegion.LEFT_FOREARM, BodyRegion.RIGHT_FOREARM)
    CoverageProfile.FULL_GLOVES -> setOf(
        BodyRegion.LEFT_FOREARM,
        BodyRegion.RIGHT_FOREARM,
        BodyRegion.LEFT_HAND,
        BodyRegion.RIGHT_HAND
    )
    CoverageProfile.TROUSERS,
    CoverageProfile.GREAVES -> setOf(
        BodyRegion.HIPS,
        BodyRegion.LEFT_THIGH,
        BodyRegion.RIGHT_THIGH,
        BodyRegion.LEFT_CALF,
        BodyRegion.RIGHT_CALF
    )
    CoverageProfile.BOOTS -> setOf(
        BodyRegion.LEFT_FOOT,
        BodyRegion.RIGHT_FOOT
    )
    CoverageProfile.ROBE -> setOf(
        BodyRegion.TORSO,
        BodyRegion.HIPS,
        BodyRegion.LEFT_UPPER_ARM,
        BodyRegion.RIGHT_UPPER_ARM,
        BodyRegion.LEFT_THIGH,
        BodyRegion.RIGHT_THIGH,
        BodyRegion.LEFT_CALF,
        BodyRegion.RIGHT_CALF
    )
}

fun CoverageProfile.defaultHiddenVisualSlots(): Set<PaperDollVisualSlot> = when (this) {
    CoverageProfile.ROBE -> setOf(PaperDollVisualSlot.LEGS)
    else -> emptySet()
}

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

    private data class SpecCacheKey(
        val id: Long,
        val name: String,
        val slot: ItemSlot,
        val style: String,
        val tier: Int
    )

    private val specCache = java.util.concurrent.ConcurrentHashMap<SpecCacheKey, EquipmentVisualSpec>()
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
        val cacheKey = SpecCacheKey(item.id, item.name, item.slot, item.style, item.tier)
        return specCache.getOrPut(cacheKey) {
            val visualSlot = PaperDollVisualSlot.fromDomainSlot(item.slot, item.style, item.name)
            val layerOrder = PaperDollLayerOrder.forVisualSlot(visualSlot)
            val itemKey = normalizeItemKey(item.name)
            val tierSuffix = "t${item.tier.coerceIn(1, 5)}"

            val slotKey = visualSlot.name.lowercase()
            val layerResName = when (itemKey) {
                // Shields change physical role with orientation: held face-forward in
                // front view, strapped face-out on the back in rear view.
                "iron_shield" -> "layer_trinket_iron_shield_${tierSuffix}"
                else -> "layer_${slotKey}_${itemKey}_${tierSuffix}"
            }
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

            val nameLower = item.name.lowercase()
            val profile = when (visualSlot) {
                PaperDollVisualSlot.HEAD -> when {
                    nameLower.contains("hood") || nameLower.contains("coif") -> CoverageProfile.HOOD
                    nameLower.contains("helm") || nameLower.contains("crown") -> CoverageProfile.CLOSED_HELM
                    else -> CoverageProfile.CAP
                }
                PaperDollVisualSlot.CHEST -> when {
                    item.style == ItemStyle.ROBE || nameLower.contains("robe") || nameLower.contains("vestment") -> CoverageProfile.ROBE
                    item.style == ItemStyle.PLATE || nameLower.contains("plate") || nameLower.contains("aegis") -> CoverageProfile.HEAVY_PLATE
                    item.style == ItemStyle.LIGHT || nameLower.contains("vest") || nameLower.contains("tunic") || nameLower.contains("shirt") -> CoverageProfile.SLEEVED_TUNIC
                    else -> CoverageProfile.SLEEVED_TUNIC
                }
                PaperDollVisualSlot.LEGS -> when {
                    nameLower.contains("greave") || nameLower.contains("guard") -> CoverageProfile.GREAVES
                    else -> CoverageProfile.TROUSERS
                }
                PaperDollVisualSlot.FEET -> CoverageProfile.BOOTS
                PaperDollVisualSlot.HANDS -> when {
                    nameLower.contains("glove") || nameLower.contains("gauntlet") || nameLower.contains("mitt") -> CoverageProfile.FULL_GLOVES
                    else -> CoverageProfile.BRACERS
                }
                PaperDollVisualSlot.WEAPON,
                PaperDollVisualSlot.BACK,
                PaperDollVisualSlot.TRINKET -> CoverageProfile.NONE
            }

            val coveredRegions = profile.defaultCoveredRegions()
            val hiddenVisualSlots = profile.defaultHiddenVisualSlots()

            val facingRule = when (visualSlot) {
                PaperDollVisualSlot.TRINKET -> VisualFacingRule.FRONT_ONLY
                PaperDollVisualSlot.BACK -> VisualFacingRule.BOTH
                else -> VisualFacingRule.BOTH
            }

            val backLayerResName = when {
                itemKey == "iron_shield" -> "layer_back_iron_shield_${tierSuffix}"
                visualSlot == PaperDollVisualSlot.BACK -> layerResName
                else -> null
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
                supportsRarityTint = true,
                coverageProfile = profile,
                coveredRegions = coveredRegions,
                hiddenVisualSlots = hiddenVisualSlots,
                facingRule = facingRule,
                backLayerResName = backLayerResName,
                orientationContract = resolveVisualDescriptor(item).orientationContract
            )
        }
    }

    fun resolvePresentation(
        item: ItemEntity,
        orientation: AvatarOrientation,
        hasFeaturedBackItem: Boolean = false
    ): OrientationPresentation {
        val presentation = resolveVisualDescriptor(item).orientationContract.forOrientation(orientation)
        if (orientation != AvatarOrientation.BACK) return presentation
        return when {
            presentation.attachment == VisualAttachmentMode.HIDDEN ||
                presentation.occlusionPolicy == BackOcclusionPolicy.ALWAYS_HIDDEN ->
                presentation.copy(attachment = VisualAttachmentMode.HIDDEN)
            item.slot == ItemSlot.WEAPON && hasFeaturedBackItem ->
                presentation.copy(
                    attachment = VisualAttachmentMode.HELD,
                    anchor = VisualAnchor(xFraction = 0.715f, yFraction = 0.65f),
                    occlusionPolicy = BackOcclusionPolicy.ALWAYS_VISIBLE
                )
            else -> presentation
        }
    }

    /**
     * Resolves the explicit orientation resolution for an equipment visual specification.
     */
    fun resolveResolution(
        spec: EquipmentVisualSpec,
        orientation: AvatarOrientation,
        context: Context? = null,
        drawableLookup: ((String) -> Int?)? = null
    ): EquipmentLayerResolution {
        if (orientation == AvatarOrientation.BACK && spec.facingRule == VisualFacingRule.FRONT_ONLY) {
            return EquipmentLayerResolution.Hidden
        }
        if (orientation == AvatarOrientation.FRONT && spec.facingRule == VisualFacingRule.BACK_ONLY) {
            return EquipmentLayerResolution.Hidden
        }

        val frontName = spec.layerResName
        val backName = spec.backLayerResName
            ?: if (spec.visualSlot == PaperDollVisualSlot.BACK) {
                frontName
            } else {
                frontName?.replace(Regex("_(t[1-5])$"), "_back_$1")
            }
        val candidateName = if (orientation == AvatarOrientation.BACK) backName else frontName

        if (candidateName.isNullOrBlank()) {
            return EquipmentLayerResolution.CanvasFallback
        }

        fun lookup(name: String?): Int? {
            if (name.isNullOrBlank()) return null
            return when {
                drawableLookup != null -> drawableLookup(name)
                context != null -> findDrawableId(context, name)
                else -> null
            }?.takeIf { it != 0 }
        }

        val resId = lookup(candidateName)

        // Directional authored art is atomic. A unique front must never silently
        // turn into unrelated procedural art on the back (or vice versa). When a
        // pair is incomplete, both orientations use the same procedural descriptor,
        // palette, and item archetype instead. Back features may intentionally share
        // one drawable in both views, and one-sided presentation rules remain one-sided.
        val requiresCompletePair = spec.facingRule == VisualFacingRule.BOTH &&
            !frontName.isNullOrBlank() &&
            !backName.isNullOrBlank() &&
            frontName != backName
        if (requiresCompletePair && (lookup(frontName) == null || lookup(backName) == null)) {
            return EquipmentLayerResolution.CanvasFallback
        }

        return if (resId != null) {
            EquipmentLayerResolution.Drawable(resId, candidateName)
        } else {
            EquipmentLayerResolution.CanvasFallback
        }
    }

    /**
     * Resolves the appropriate 2D drawable resource ID for a specific orientation.
     */
    @DrawableRes
    fun resolveDrawableId(
        context: Context,
        spec: EquipmentVisualSpec,
        orientation: AvatarOrientation
    ): Int? {
        val res = resolveResolution(spec, orientation, context = context)
        return (res as? EquipmentLayerResolution.Drawable)?.drawableResId
    }

    /**
     * Converts item names into snake_case keys (e.g. "Dumbbell Hammer" -> "dumbbell_hammer").
     */
    fun normalizeItemKey(name: String): String {
        val key = name.lowercase()
            .replace("'", "")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return when (key) {
            "rusty_broadsword" -> "rusty_sword"
            "training_bow" -> "training_shortbow"
            "novice_wand" -> "apprentice_wand"
            "worn_dagger" -> "bent_shiv"
            "iron_back_shield", "reinforced_back_shield" -> "iron_shield"
            else -> key
        }
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

    /**
     * Resolves the canonical, orientation-neutral visual descriptor for an equipment item.
     * Consumed uniformly by both 2D drawable pipelines and procedural Canvas renderers.
     */
    fun resolveVisualDescriptor(item: ItemEntity): EquipmentVisualDescriptor {
        val itemKey = normalizeItemKey(item.name)
        val nameLower = item.name.lowercase()
        val isPolearm = listOf("spear", "lance", "pike", "halberd", "harpoon").any(nameLower::contains) ||
            itemKey == "gae_bolg"

        val descriptor = when {
            listOf("handwrap", "palm band", "palm relic").any(nameLower::contains) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.UNARMED_WRAP, androidx.compose.ui.graphics.Color(0xFF8B5E3C),
                androidx.compose.ui.graphics.Color(0xFF4A3525), androidx.compose.ui.graphics.Color(0xFFD97706)
            )
            nameLower.contains("tonfa") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.TONFA, androidx.compose.ui.graphics.Color(0xFF5D4037),
                androidx.compose.ui.graphics.Color(0xFF2C241E), androidx.compose.ui.graphics.Color(0xFF06B6D4)
            )
            listOf("lute", "horn", "flute").any(nameLower::contains) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.INSTRUMENT, androidx.compose.ui.graphics.Color(0xFF8B5A2B),
                androidx.compose.ui.graphics.Color(0xFF4A2F1B), androidx.compose.ui.graphics.Color(0xFFF59E0B)
            )
            nameLower.contains("scythe") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.SCYTHE, androidx.compose.ui.graphics.Color(0xFF5D4037),
                androidx.compose.ui.graphics.Color(0xFFCBD5E1), androidx.compose.ui.graphics.Color(0xFF06B6D4)
            )
            listOf("axe", "cleaver").any(nameLower::contains) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.AXE, androidx.compose.ui.graphics.Color(0xFF5D4037),
                androidx.compose.ui.graphics.Color(0xFF94A3B8), androidx.compose.ui.graphics.Color(0xFFB91C1C)
            )
            nameLower.contains("maul") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.HAMMER, androidx.compose.ui.graphics.Color(0xFF5D4037),
                androidx.compose.ui.graphics.Color(0xFF64748B), androidx.compose.ui.graphics.Color(0xFFB91C1C)
            )
            listOf("club", "bludgeon").any(nameLower::contains) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.CLUB, androidx.compose.ui.graphics.Color(0xFF6B4423),
                androidx.compose.ui.graphics.Color(0xFF3E2723), androidx.compose.ui.graphics.Color(0xFFD97706)
            )
            nameLower.contains("scepter") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.SCEPTER, androidx.compose.ui.graphics.Color(0xFF6B5138),
                androidx.compose.ui.graphics.Color(0xFFD4AF37), androidx.compose.ui.graphics.Color(0xFFC084FC)
            )
            nameLower.contains("focus") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.ARCANE_FOCUS, androidx.compose.ui.graphics.Color(0xFF475569),
                androidx.compose.ui.graphics.Color(0xFF94A3B8), androidx.compose.ui.graphics.Color(0xFF38BDF8)
            )
            isPolearm -> EquipmentVisualDescriptor(
                canonicalKey = itemKey,
                archetype = VisualArchetype.SPEAR_GENERIC,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF6B4423),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                accentColor = androidx.compose.ui.graphics.Color(0xFFB07E24),
                glowColor = if (item.tier >= 4) androidx.compose.ui.graphics.Color(0xFFF97316) else null
            )
            itemKey == "dragonfang_greatsword" || (nameLower.contains("dragonfang") && item.style == ItemStyle.GREATSWORD) -> EquipmentVisualDescriptor(
                canonicalKey = "dragonfang_greatsword",
                archetype = VisualArchetype.GREATSWORD_DRAGON,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF0F172A), // Obsidian blade body
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFF1F5F9), // Dragon bone serrations
                accentColor = androidx.compose.ui.graphics.Color(0xFF991B1B), // Dragon wing crossguard
                glowColor = androidx.compose.ui.graphics.Color(0xFFEF4444) // Crimson fuller & dragon eye gem
            )
            item.style == ItemStyle.GREATSWORD -> EquipmentVisualDescriptor(
                canonicalKey = itemKey,
                archetype = VisualArchetype.GREATSWORD_GENERIC,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF2C241E),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFB07E24),
                accentColor = androidx.compose.ui.graphics.Color(0xFFB07E24)
            )
            itemKey == "knights_blade" || nameLower.contains("knights_blade") || (nameLower.contains("knight") && item.slot == ItemSlot.WEAPON) -> EquipmentVisualDescriptor(
                canonicalKey = "knights_blade",
                archetype = VisualArchetype.SWORD_KNIGHT,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF475569), // Polished steel scabbard
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF94A3B8), // Steel rim & blade
                accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B), // Gold crossguard
                glowColor = androidx.compose.ui.graphics.Color(0xFF06B6D4) // Cyan runic fuller
            )
            item.style == ItemStyle.SWORD -> EquipmentVisualDescriptor(
                canonicalKey = itemKey,
                archetype = VisualArchetype.SWORD_GENERIC,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF475569),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                accentColor = androidx.compose.ui.graphics.Color(0xFFB07E24)
            )
            itemKey == "training_shortbow" || nameLower.contains("shortbow") || nameLower.contains("training bow") -> EquipmentVisualDescriptor(
                canonicalKey = "training_shortbow",
                archetype = VisualArchetype.BOW_SHORT,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF78350F), // Yew wood stave
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFB45309), // Inner curve highlight
                accentColor = androidx.compose.ui.graphics.Color(0xFFE2E8F0), // Bowstring
                glowColor = androidx.compose.ui.graphics.Color(0xFF10B981) // Green arrow fletching
            )
            item.style == ItemStyle.BOW -> EquipmentVisualDescriptor(
                canonicalKey = itemKey,
                archetype = VisualArchetype.BOW_GENERIC,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF78350F),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFB45309),
                accentColor = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
            )
            itemKey == "apprentice_wand" || nameLower.contains("apprentice wand") || nameLower.contains("novice wand") -> EquipmentVisualDescriptor(
                canonicalKey = "apprentice_wand",
                archetype = VisualArchetype.WAND_ARCANE,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF8B6B4A),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF6B5138),
                glowColor = androidx.compose.ui.graphics.Color(0xFF38BDF8) // Cyan mana crystal
            )
            itemKey == "oakheart_staff" || nameLower.contains("oakheart") || item.style == ItemStyle.STAFF -> EquipmentVisualDescriptor(
                canonicalKey = itemKey,
                archetype = VisualArchetype.STAFF_WOODEN,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF5D4037),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF3E2723),
                glowColor = androidx.compose.ui.graphics.Color(0xFFC084FC) // Arcane purple crystal
            )
            item.slot == ItemSlot.CHEST && (nameLower.contains("chainmail") || nameLower.contains("ringmail")) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.ARMOR_MAIL, androidx.compose.ui.graphics.Color(0xFF94A3B8),
                androidx.compose.ui.graphics.Color(0xFF475569), androidx.compose.ui.graphics.Color(0xFFCBD5E1)
            )
            item.slot == ItemSlot.CHEST && listOf("cloak", "shroud", "mantle").any(nameLower::contains) -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.ARMOR_CLOAK, androidx.compose.ui.graphics.Color(0xFF3E5A78),
                androidx.compose.ui.graphics.Color(0xFF283A4E),
                if (item.tier >= 4) androidx.compose.ui.graphics.Color(0xFF9C7BE3) else null
            )
            item.slot == ItemSlot.CHEST && nameLower.contains("pauldron") -> EquipmentVisualDescriptor(
                itemKey, VisualArchetype.ARMOR_PAULDRONS, androidx.compose.ui.graphics.Color(0xFF475569),
                androidx.compose.ui.graphics.Color(0xFF1E293B), androidx.compose.ui.graphics.Color(0xFFF59E0B)
            )
            itemKey == "steel_plate" || nameLower.contains("steel plate") -> EquipmentVisualDescriptor(
                canonicalKey = "steel_plate",
                archetype = VisualArchetype.PLATE_STEEL_CUIRASS,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF475569), // Slate steel
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Dark steel trim
                accentColor = androidx.compose.ui.graphics.Color(0xFF94A3B8), // Ridge
                glowColor = androidx.compose.ui.graphics.Color(0xFF06B6D4) // Cyan rune inlay
            )
            itemKey == "padded_vest" || nameLower.contains("padded vest") -> EquipmentVisualDescriptor(
                canonicalKey = "padded_vest",
                archetype = VisualArchetype.LEATHER_WARM_PADDED,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF78350F), // Warm reddish brown
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF451A03), // Dark brown leather
                accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B) // Gold ties/lacing
            )
            itemKey == "supple_leathers" || nameLower.contains("supple leathers") -> EquipmentVisualDescriptor(
                canonicalKey = "supple_leathers",
                archetype = VisualArchetype.LEATHER_FOREST_SUPPLE,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF0F172A), // Midnight leather, matching authored front
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Charcoal paneling
                accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B) // Gold buckle/trim
            )
            itemKey == "apprentice_robes" || nameLower.contains("apprentice robes") -> EquipmentVisualDescriptor(
                canonicalKey = "apprentice_robes",
                archetype = VisualArchetype.ROBE_COBALT_APPRENTICE,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF2563EB), // Cobalt blue
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF1E3A8A), // Deep navy
                accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B) // Golden stole & runes
            )
            itemKey == "runeweave_robe" || nameLower.contains("runeweave") -> EquipmentVisualDescriptor(
                canonicalKey = "runeweave_robe",
                archetype = VisualArchetype.ROBE_VIOLET_RUNEWEAVE,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF581C87), // Deep violet
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF311042), // Dark shadow violet
                accentColor = androidx.compose.ui.graphics.Color(0xFF7E22CE), // Mantle purple
                glowColor = androidx.compose.ui.graphics.Color(0xFF06B6D4) // Cyan glowing runes
            )
            itemKey == "adventurers_hood" || nameLower.contains("adventurers hood") -> EquipmentVisualDescriptor(
                canonicalKey = "adventurers_hood",
                archetype = VisualArchetype.HOOD_ADVENTURER,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF047857),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF064E3B),
                accentColor = androidx.compose.ui.graphics.Color(0xFFFBBF24)
            )
            itemKey == "iron_greathelm" || nameLower.contains("greathelm") -> EquipmentVisualDescriptor(
                canonicalKey = "iron_greathelm",
                archetype = VisualArchetype.HELM_CLOSED_GREATHELM,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF9AA3AD),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF767E87)
            )
            itemKey == "iron_shield" || nameLower.contains("iron shield") -> EquipmentVisualDescriptor(
                canonicalKey = "iron_shield",
                archetype = VisualArchetype.SHIELD_IRON_HEATER,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF334155),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                accentColor = androidx.compose.ui.graphics.Color(0xFFF59E0B)
            )
            itemKey == "hunters_quiver" || nameLower.contains("quiver") -> EquipmentVisualDescriptor(
                canonicalKey = "hunters_quiver",
                archetype = VisualArchetype.QUIVER_HUNTER,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF5D4037),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF3E2723),
                accentColor = androidx.compose.ui.graphics.Color(0xFFD97706)
            )
            itemKey == "velvet_cape" || nameLower.contains("velvet cape") -> EquipmentVisualDescriptor(
                canonicalKey = "velvet_cape",
                archetype = VisualArchetype.CAPE_VELVET,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF6B459E),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF4C0519),
                accentColor = androidx.compose.ui.graphics.Color(0xFF9333EA)
            )
            itemKey == "celestial_wings" || nameLower.contains("wings") -> EquipmentVisualDescriptor(
                canonicalKey = "celestial_wings",
                archetype = VisualArchetype.WINGS_CELESTIAL,
                primaryColor = androidx.compose.ui.graphics.Color(0xFFF59E0B),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFFFEF08A),
                accentColor = androidx.compose.ui.graphics.Color(0xFFFDE047),
                glowColor = androidx.compose.ui.graphics.Color(0xFF67E8F9)
            )
            else -> {
                val (main, dark, glow) = resolveDefaultPalette(item)
                EquipmentVisualDescriptor(
                    canonicalKey = itemKey,
                    archetype = VisualArchetype.GENERIC,
                    primaryColor = main,
                    secondaryColor = dark,
                    glowColor = glow
                )
            }
        }
        return descriptor.copy(
            proceduralSignature = resolveProceduralSignature(item, descriptor.canonicalKey)
        )
    }

    /**
     * Produces a stable visual signature for procedural equipment. The result is
     * deterministic across processes and orientations: item identity chooses the
     * design family while tier controls the amount and prestige of its detailing.
     */
    fun resolveProceduralSignature(
        item: ItemEntity,
        canonicalKey: String = normalizeItemKey(item.name)
    ): ProceduralVisualSignature {
        val identity = "$canonicalKey|${item.slot.name}|${item.style}|${item.tier.coerceIn(1, 5)}"
        fun bucket(salt: String, size: Int): Int =
            Math.floorMod("$identity|$salt".hashCode(), size)

        val tier = item.tier.coerceIn(1, 5)
        val trims = when (tier) {
            1 -> listOf(ProceduralTrim.PLAIN, ProceduralTrim.BAND)
            2 -> listOf(ProceduralTrim.BAND, ProceduralTrim.SPLIT, ProceduralTrim.CHEVRON)
            else -> ProceduralTrim.entries
        }
        val ornaments = when (tier) {
            1 -> listOf(ProceduralOrnament.NONE, ProceduralOrnament.STUDS)
            2 -> listOf(ProceduralOrnament.STUDS, ProceduralOrnament.GEM)
            3 -> listOf(ProceduralOrnament.GEM, ProceduralOrnament.RUNES, ProceduralOrnament.SPIKES)
            else -> listOf(ProceduralOrnament.GEM, ProceduralOrnament.RUNES, ProceduralOrnament.SPIKES, ProceduralOrnament.CREST)
        }
        val finish = when (tier) {
            1 -> ProceduralMaterialFinish.WORN
            2 -> ProceduralMaterialFinish.CRAFTED
            3 -> ProceduralMaterialFinish.REFINED
            else -> ProceduralMaterialFinish.ASCENDANT
        }

        return ProceduralVisualSignature(
            silhouette = ProceduralSilhouette.entries[bucket("silhouette", ProceduralSilhouette.entries.size)],
            trim = trims[bucket("trim", trims.size)],
            ornament = ornaments[bucket("ornament", ornaments.size)],
            finish = finish,
            accentFamily = bucket("accent", 6),
            detailCount = tier
        )
    }

    /**
     * Internal domain-owned default palette mapping for items without explicit named descriptors.
     */
    fun resolveDefaultPalette(item: ItemEntity): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color?> {
        val name = item.name.lowercase()
        val effectiveStyle = when {
            item.slot == ItemSlot.WEAPON -> item.style
            listOf("cloth", "linen", "silk", "robe", "vestment", "weave").any(name::contains) -> ItemStyle.ROBE
            listOf("leather", "hide").any(name::contains) -> ItemStyle.LIGHT
            listOf("iron", "steel", "plate", "mail", "chain", "sabat", "vambrace", "gauntlet").any(name::contains) -> ItemStyle.PLATE
            else -> item.style
        }
        return when (effectiveStyle) {
        ItemStyle.ROBE -> when (item.tier) {
            1, 2 -> Triple(androidx.compose.ui.graphics.Color(0xFF2563EB), androidx.compose.ui.graphics.Color(0xFF1E3A8A), null)
            3 -> Triple(androidx.compose.ui.graphics.Color(0xFF581C87), androidx.compose.ui.graphics.Color(0xFF311042), androidx.compose.ui.graphics.Color(0xFF06B6D4))
            else -> Triple(androidx.compose.ui.graphics.Color(0xFF8A5CE8), androidx.compose.ui.graphics.Color(0xFF5C36B0), androidx.compose.ui.graphics.Color(0xFFC9A8FF))
        }
        ItemStyle.LIGHT -> when (item.tier) {
            1 -> Triple(androidx.compose.ui.graphics.Color(0xFF78350F), androidx.compose.ui.graphics.Color(0xFF451A03), null)
            2 -> Triple(androidx.compose.ui.graphics.Color(0xFF047857), androidx.compose.ui.graphics.Color(0xFF064E3B), androidx.compose.ui.graphics.Color(0xFFFBBF24))
            3 -> Triple(androidx.compose.ui.graphics.Color(0xFF5A6B78), androidx.compose.ui.graphics.Color(0xFF3E4C58), null)
            else -> Triple(androidx.compose.ui.graphics.Color(0xFF3A3D5C), androidx.compose.ui.graphics.Color(0xFF282A44), androidx.compose.ui.graphics.Color(0xFF9C7BE3))
        }
        ItemStyle.PLATE -> when (item.tier) {
            1 -> Triple(androidx.compose.ui.graphics.Color(0xFF9AA3AD), androidx.compose.ui.graphics.Color(0xFF64748B), null)
            2 -> Triple(androidx.compose.ui.graphics.Color(0xFF94A3B8), androidx.compose.ui.graphics.Color(0xFF475569), null)
            3 -> Triple(androidx.compose.ui.graphics.Color(0xFF475569), androidx.compose.ui.graphics.Color(0xFF1E293B), androidx.compose.ui.graphics.Color(0xFF06B6D4))
            else -> Triple(androidx.compose.ui.graphics.Color(0xFFF0C040), androidx.compose.ui.graphics.Color(0xFFC89B26), androidx.compose.ui.graphics.Color(0xFFF8D24A))
        }
        else -> when (item.tier) {
            1 -> Triple(androidx.compose.ui.graphics.Color(0xFF78350F), androidx.compose.ui.graphics.Color(0xFF451A03), null)
            2 -> Triple(androidx.compose.ui.graphics.Color(0xFF9AA3AD), androidx.compose.ui.graphics.Color(0xFF767E87), null)
            3 -> Triple(androidx.compose.ui.graphics.Color(0xFF475569), androidx.compose.ui.graphics.Color(0xFF1E293B), androidx.compose.ui.graphics.Color(0xFF06B6D4))
            else -> Triple(androidx.compose.ui.graphics.Color(0xFFF0C040), androidx.compose.ui.graphics.Color(0xFFC89B26), androidx.compose.ui.graphics.Color(0xFFF8D24A))
        }
    }
    }
}

/**
 * Standardized equipment visual archetypes for shared 2D/procedural rendering.
 */
enum class VisualArchetype {
    UNARMED_WRAP,
    TONFA,
    INSTRUMENT,
    AXE,
    SCYTHE,
    HAMMER,
    CLUB,
    SCEPTER,
    ARCANE_FOCUS,
    ARMOR_MAIL,
    ARMOR_CLOAK,
    ARMOR_PAULDRONS,
    SPEAR_GENERIC,
    GREATSWORD_DRAGON,
    GREATSWORD_GENERIC,
    SWORD_KNIGHT,
    SWORD_GENERIC,
    BOW_SHORT,
    BOW_GENERIC,
    WAND_ARCANE,
    STAFF_WOODEN,
    ROBE_COBALT_APPRENTICE,
    ROBE_VIOLET_RUNEWEAVE,
    ROBE_GENERIC,
    PLATE_STEEL_CUIRASS,
    PLATE_GENERIC,
    LEATHER_WARM_PADDED,
    LEATHER_FOREST_SUPPLE,
    LEATHER_GENERIC,
    HOOD_ADVENTURER,
    HELM_CLOSED_GREATHELM,
    SHIELD_IRON_HEATER,
    QUIVER_HUNTER,
    CAPE_VELVET,
    WINGS_CELESTIAL,
    GENERIC
}

/**
 * One placement policy per visual archetype. New items inherit these rules by
 * resolving to an archetype; unique artwork does not require combination code.
 */
fun VisualArchetype.defaultOrientationContract(): EquipmentOrientationContract {
    val body = OrientationPresentation(VisualAttachmentMode.BODY_WORN, VisualAnchor())
    val heldRight = OrientationPresentation(
        VisualAttachmentMode.HELD,
        VisualAnchor(xFraction = 0.715f, yFraction = 0.65f)
    )
    return when (this) {
        VisualArchetype.UNARMED_WRAP -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(VisualAttachmentMode.HIDDEN, VisualAnchor())
        )

        VisualArchetype.INSTRUMENT,
        VisualArchetype.TONFA,
        VisualArchetype.SCEPTER,
        VisualArchetype.ARCANE_FOCUS -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.SIDE_CARRY,
                VisualAnchor(xFraction = 0.7f, yFraction = 0.66f),
                BackOcclusionPolicy.HIDE_WHEN_BACK_FEATURE
            )
        )

        VisualArchetype.AXE,
        VisualArchetype.SCYTHE,
        VisualArchetype.HAMMER,
        VisualArchetype.CLUB -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.BACK_MOUNTED,
                VisualAnchor(xFraction = 0.5f, yFraction = 0.5f, rotationDegrees = 35f)
            )
        )

        VisualArchetype.SPEAR_GENERIC -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.BACK_MOUNTED,
                VisualAnchor(xFraction = 0.5f, yFraction = 0.5f, rotationDegrees = -32f),
                BackOcclusionPolicy.HIDE_WHEN_BACK_FEATURE
            )
        )

        VisualArchetype.GREATSWORD_DRAGON,
        VisualArchetype.GREATSWORD_GENERIC -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.BACK_MOUNTED,
                VisualAnchor(xFraction = 0.5f, yFraction = 0.5f, rotationDegrees = 145f)
            )
        )

        VisualArchetype.SWORD_KNIGHT,
        VisualArchetype.SWORD_GENERIC -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.SIDE_CARRY,
                VisualAnchor(xFraction = 0.285f, yFraction = 0.65f),
                BackOcclusionPolicy.HIDE_WHEN_BACK_FEATURE
            )
        )

        VisualArchetype.BOW_SHORT,
        VisualArchetype.BOW_GENERIC -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.SIDE_CARRY,
                VisualAnchor(xFraction = 0.36f, yFraction = 0.475f)
            )
        )

        VisualArchetype.WAND_ARCANE -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.SIDE_CARRY,
                VisualAnchor(xFraction = 0.68f, yFraction = 0.522f)
            )
        )

        VisualArchetype.STAFF_WOODEN -> EquipmentOrientationContract(
            front = heldRight,
            back = OrientationPresentation(
                VisualAttachmentMode.BACK_MOUNTED,
                VisualAnchor(xFraction = 0.5f, yFraction = 0.5f, rotationDegrees = -20f)
            )
        )

        VisualArchetype.SHIELD_IRON_HEATER -> EquipmentOrientationContract(
            front = OrientationPresentation(
                VisualAttachmentMode.HELD,
                VisualAnchor(xFraction = 0.28f, yFraction = 0.61f)
            ),
            back = OrientationPresentation(
                VisualAttachmentMode.BACK_FEATURE,
                VisualAnchor(xFraction = 0.36f, yFraction = 0.52f)
            )
        )

        VisualArchetype.QUIVER_HUNTER,
        VisualArchetype.CAPE_VELVET,
        VisualArchetype.WINGS_CELESTIAL -> EquipmentOrientationContract(
            front = OrientationPresentation(VisualAttachmentMode.BACK_FEATURE, VisualAnchor()),
            back = OrientationPresentation(VisualAttachmentMode.BACK_FEATURE, VisualAnchor())
        )

        VisualArchetype.ROBE_COBALT_APPRENTICE,
        VisualArchetype.ROBE_VIOLET_RUNEWEAVE,
        VisualArchetype.ROBE_GENERIC,
        VisualArchetype.PLATE_STEEL_CUIRASS,
        VisualArchetype.PLATE_GENERIC,
        VisualArchetype.LEATHER_WARM_PADDED,
        VisualArchetype.LEATHER_FOREST_SUPPLE,
        VisualArchetype.LEATHER_GENERIC,
        VisualArchetype.ARMOR_MAIL,
        VisualArchetype.ARMOR_CLOAK,
        VisualArchetype.ARMOR_PAULDRONS,
        VisualArchetype.HOOD_ADVENTURER,
        VisualArchetype.HELM_CLOSED_GREATHELM,
        VisualArchetype.GENERIC -> EquipmentOrientationContract(body, body)
    }
}

/**
 * Canonical visual descriptor defining the shared identity, archetype, and color palette
 * for an equipped item across all viewing orientations.
 */
data class EquipmentVisualDescriptor(
    val canonicalKey: String,
    val archetype: VisualArchetype,
    val primaryColor: androidx.compose.ui.graphics.Color,
    val secondaryColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color? = null,
    val glowColor: androidx.compose.ui.graphics.Color? = null,
    val orientationContract: EquipmentOrientationContract = archetype.defaultOrientationContract(),
    val proceduralSignature: ProceduralVisualSignature = ProceduralVisualSignature.DEFAULT
)

enum class ProceduralSilhouette { BALANCED, ANGULAR, CURVED, FORTIFIED }

enum class ProceduralTrim { PLAIN, BAND, SPLIT, CHEVRON, RUNIC }

enum class ProceduralOrnament { NONE, STUDS, GEM, RUNES, SPIKES, CREST }

enum class ProceduralMaterialFinish { WORN, CRAFTED, REFINED, ASCENDANT }

/** Stable, orientation-neutral parameters consumed by every procedural slot renderer. */
data class ProceduralVisualSignature(
    val silhouette: ProceduralSilhouette,
    val trim: ProceduralTrim,
    val ornament: ProceduralOrnament,
    val finish: ProceduralMaterialFinish,
    val accentFamily: Int,
    val detailCount: Int
) {
    companion object {
        val DEFAULT = ProceduralVisualSignature(
            silhouette = ProceduralSilhouette.BALANCED,
            trim = ProceduralTrim.PLAIN,
            ornament = ProceduralOrnament.NONE,
            finish = ProceduralMaterialFinish.WORN,
            accentFamily = 0,
            detailCount = 1
        )
    }
}

/**
 * Pure, orientation-aware render pass resolver for paper-doll rendering.
 */
object PaperDollPassResolver {

    fun resolvePass(order: PaperDollLayerOrder, orientation: AvatarOrientation): PaperDollRenderPass {
        return if (order.resolvedZIndex(orientation) < PaperDollLayerOrder.BODY_BASE.resolvedZIndex(orientation)) {
            PaperDollRenderPass.BEHIND_BODY
        } else {
            PaperDollRenderPass.FOREGROUND
        }
    }

    fun resolvePlan(
        gear: Map<ItemSlot, ItemEntity>,
        orientation: AvatarOrientation,
        customDyes: Map<ItemSlot, EquipmentDye> = emptyMap(),
        context: Context? = null,
        drawableLookup: ((String) -> Int?)? = null
    ): ResolvedPaperDollPassPlan {
        val entries = mutableListOf<PaperDollRenderEntry>()
        val activeCoveredRegions = mutableSetOf<BodyRegion>()

        data class ItemResolution(
            val slot: ItemSlot,
            val item: ItemEntity,
            val spec: EquipmentVisualSpec,
            val resolution: EquipmentLayerResolution,
            val dye: EquipmentDye
        )

        val resolvedItems = gear.map { (slot, item) ->
            val spec = EquipmentVisualRegistry.resolveSpec(item)
            // Authored drawables already encode their material palette and details.
            // Apply tint only for an explicit player-selected dye.
            val dye = customDyes[slot] ?: EquipmentDye.NATURAL
            val resolution = EquipmentVisualRegistry.resolveResolution(
                spec = spec,
                orientation = orientation,
                context = context,
                drawableLookup = drawableLookup
            )
            ItemResolution(slot, item, spec, resolution, dye)
        }

        // Derive hidden visual slots from all non-hidden active gear (e.g. Robe suppresses LEGS)
        val hiddenSlots = resolvedItems
            .filter { it.resolution !is EquipmentLayerResolution.Hidden }
            .flatMap { it.spec.hiddenVisualSlots }
            .toSet()

        val hasFeaturedBackItem = orientation == AvatarOrientation.BACK && resolvedItems.any {
            it.resolution !is EquipmentLayerResolution.Hidden &&
                it.spec.orientationContract.back.attachment == VisualAttachmentMode.BACK_FEATURE
        }

        // Always include background backdrop (aura, shadow, silhouette) and base humanoid body
        val backdropZ = PaperDollLayerOrder.FX_AURA_BACK.resolvedZIndex(orientation)
        entries.add(PaperDollRenderEntry.AvatarBackdrop(zIndex = backdropZ))

        val bodyZ = PaperDollLayerOrder.BODY_BASE.resolvedZIndex(orientation)
        entries.add(PaperDollRenderEntry.BodyBase(zIndex = bodyZ))

        resolvedItems.forEach { resolved ->
            val presentation = EquipmentVisualRegistry.resolvePresentation(
                item = resolved.item,
                orientation = orientation,
                hasFeaturedBackItem = hasFeaturedBackItem
            )
            val effectiveResolution = if (
                resolved.spec.visualSlot in hiddenSlots ||
                presentation.attachment == VisualAttachmentMode.HIDDEN
            ) {
                EquipmentLayerResolution.Hidden
            } else {
                resolved.resolution
            }

            val effectiveOrder = when (presentation.attachment) {
                // A shield being actively carried belongs with held equipment in
                // front view, not in the behind-body back-wearable pass.
                VisualAttachmentMode.HELD -> PaperDollLayerOrder.GEAR_WEAPON
                else -> resolved.spec.layerOrder
            }
            val zIndex = effectiveOrder.resolvedZIndex(orientation)

            when (effectiveResolution) {
                is EquipmentLayerResolution.Hidden -> {
                    // Excluded completely from render plan
                }
                is EquipmentLayerResolution.CanvasFallback -> {
                    activeCoveredRegions.addAll(resolved.spec.coveredRegions)
                    entries.add(
                        PaperDollRenderEntry.CanvasFallbackSlot(
                            slot = resolved.slot,
                            item = resolved.item,
                            spec = resolved.spec,
                            visualSlot = resolved.spec.visualSlot,
                            zIndex = zIndex
                        )
                    )
                }
                is EquipmentLayerResolution.Drawable -> {
                    activeCoveredRegions.addAll(resolved.spec.coveredRegions)
                    val layer = ResolvedPaperDollLayer(
                        order = effectiveOrder,
                        visualSlot = resolved.spec.visualSlot,
                        spec = resolved.spec,
                        drawableResId = effectiveResolution.drawableResId,
                        resName = effectiveResolution.resName,
                        item = resolved.item,
                        isEquipped = true,
                        dye = resolved.dye,
                        orientation = orientation
                    )
                    entries.add(
                        PaperDollRenderEntry.DrawableLayer(
                            layer = layer,
                            zIndex = zIndex
                        )
                    )
                }
            }
        }

        // Sort strictly by orientation-resolved zIndex so layers and canvas slots interleave perfectly
        entries.sortBy { it.zIndex }

        return ResolvedPaperDollPassPlan(
            entries = entries,
            hiddenBodyRegions = activeCoveredRegions,
            orientation = orientation
        )
    }
}
