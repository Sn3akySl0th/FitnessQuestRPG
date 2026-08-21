package com.fitnessquest.rpg

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.MonotonicFrameClock
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.isEquippable
import com.fitnessquest.rpg.domain.ItemStyle
import com.fitnessquest.rpg.domain.visuals.AvatarOrientation
import com.fitnessquest.rpg.domain.visuals.BackOcclusionPolicy
import com.fitnessquest.rpg.domain.visuals.BodyRegion
import com.fitnessquest.rpg.domain.visuals.CoverageProfile
import com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution
import com.fitnessquest.rpg.domain.visuals.EquipmentDye
import com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry
import com.fitnessquest.rpg.domain.visuals.PaperDollLayerOrder
import com.fitnessquest.rpg.domain.visuals.PaperDollPassResolver
import com.fitnessquest.rpg.domain.visuals.PaperDollRenderEntry
import com.fitnessquest.rpg.domain.visuals.PaperDollVisualSlot
import com.fitnessquest.rpg.domain.visuals.ProceduralMaterialFinish
import com.fitnessquest.rpg.domain.visuals.ProceduralOrnament
import com.fitnessquest.rpg.domain.visuals.VisualFacingRule
import com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode
import com.fitnessquest.rpg.domain.visuals.defaultCoveredRegions
import com.fitnessquest.rpg.ui.components.AvatarTurnaroundState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AvatarTurnaroundStateMachineTest {

    private fun drawableLookup(vararg names: String): (String) -> Int? {
        val available = names.toSet()
        return { name -> if (name in available) name.hashCode() else null }
    }

    private class ManualTestClock : MonotonicFrameClock {
        var timeNanos = 0L
            private set
        private val awaiters = mutableListOf<(Long) -> Unit>()

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            return suspendCancellableCoroutine { cont ->
                val callback: (Long) -> Unit = { time ->
                    val res = runCatching { onFrame(time) }
                    cont.resumeWith(res)
                }
                awaiters.add(callback)
                cont.invokeOnCancellation {
                    awaiters.remove(callback)
                }
            }
        }

        suspend fun advanceFrame(millis: Long = 16) {
            kotlinx.coroutines.yield()
            timeNanos += millis * 1_000_000L
            val current = awaiters.toList()
            awaiters.clear()
            current.forEach { it(timeNanos) }
            kotlinx.coroutines.yield()
        }

        suspend fun advanceFrames(count: Int, millisPerFrame: Long = 16) {
            repeat(count) {
                advanceFrame(millisPerFrame)
            }
        }
    }

    private fun runTurnaroundTest(block: suspend kotlinx.coroutines.CoroutineScope.(ManualTestClock) -> Unit) {
        val clock = ManualTestClock()
        runBlocking(clock) {
            block(clock)
        }
    }

    private fun createMockContext(existingDrawables: Set<String>): Context {
        val testResources = object : Resources(null, null, null) {
            override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
                return if (name != null && name in existingDrawables) 1001 else 0
            }

            override fun openRawResource(id: Int): java.io.InputStream {
                if (id == 0) throw Resources.NotFoundException("Resource ID 0")
                return java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3))
            }
        }

        return object : ContextWrapper(null) {
            override fun getPackageName(): String = "com.fitnessquest.rpg"
            override fun getResources(): Resources = testResources
        }
    }

    // =======================================================================
    // 1. Mixed-Pipeline Interleaved Render Plan Tests
    // =======================================================================

    @Test
    fun planOrder_drawableTorsoAndCanvasFallbackWeapon_weaponRendersLater() {
        val torso = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val weapon = ItemEntity(id = 11, name = "Iron Broadsword", slot = ItemSlot.WEAPON, tier = 2, emoji = "⚔️", price = 100)
        val gear = mapOf(ItemSlot.CHEST to torso, ItemSlot.WEAPON to weapon)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = drawableLookup(
                "layer_chest_steel_plate_t3",
                "layer_chest_steel_plate_back_t3"
            )
        )

        val torsoIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.CHEST }
        val weaponIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.WEAPON }
        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }

        assertTrue("BodyBase must exist", bodyIdx >= 0)
        assertTrue("Drawable torso must exist", torsoIdx >= 0)
        assertTrue("Canvas fallback weapon must exist", weaponIdx >= 0)
        assertTrue("Body base must render before torso armor", bodyIdx < torsoIdx)
        assertTrue("Canvas fallback weapon (order 12) must render AFTER drawable torso (order 7)", torsoIdx < weaponIdx)
    }

    @Test
    fun planOrder_drawableTorsoAndCanvasFallbackHeadgear_headgearRendersLater() {
        val torso = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val gear = mapOf(ItemSlot.CHEST to torso, ItemSlot.HEAD to helm)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = drawableLookup(
                "layer_chest_steel_plate_t3",
                "layer_chest_steel_plate_back_t3"
            )
        )

        val torsoIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.CHEST }
        val headIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.HEAD }

        assertTrue("Drawable torso must exist", torsoIdx >= 0)
        assertTrue("Canvas fallback headgear must exist", headIdx >= 0)
        assertTrue("Canvas fallback headgear (order 10) must render AFTER drawable torso (order 7)", torsoIdx < headIdx)
    }

    @Test
    fun planOrder_canvasFallbackTorsoAndDrawableHeadgear_headgearRendersLater() {
        val torso = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val gear = mapOf(ItemSlot.CHEST to torso, ItemSlot.HEAD to helm)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = drawableLookup(
                "layer_head_iron_greathelm_t2",
                "layer_head_iron_greathelm_back_t2"
            )
        )

        val torsoIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.CHEST }
        val headIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.HEAD }

        assertTrue("Canvas fallback torso must exist", torsoIdx >= 0)
        assertTrue("Drawable headgear must exist", headIdx >= 0)
        assertTrue("Drawable headgear (order 10) must render AFTER Canvas fallback torso (order 7)", torsoIdx < headIdx)
    }

    @Test
    fun planOrder_backdropAndFrontDrawableCape_renderInCorrectSequence() {
        val cape = ItemEntity(id = 1195, name = "Novice Cape", slot = ItemSlot.TRINKET, style = "cape", tier = 1, emoji = "🧣", price = 50)
        val gear = mapOf(ItemSlot.TRINKET to cape)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = { name -> if (name == "layer_back_novice_cape_t1") 1003 else null }
        )

        val backdropIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.AvatarBackdrop }
        val capeIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.BACK }
        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }

        assertTrue("AvatarBackdrop must exist in plan", backdropIdx >= 0)
        assertTrue("Front cape must exist in plan", capeIdx >= 0)
        assertTrue("Body base must exist in plan", bodyIdx >= 0)

        assertTrue("AvatarBackdrop (Z=1) must render BEFORE Front Cape (Z=2)", backdropIdx < capeIdx)
        assertTrue("Front Cape (Z=2) must render BEFORE BodyBase (Z=3)", capeIdx < bodyIdx)
    }

    @Test
    fun planOrder_backDrawableCape_rendersInForegroundAtDefinedOrder() {
        val cape = ItemEntity(id = 1195, name = "Novice Cape", slot = ItemSlot.TRINKET, style = "cape", tier = 1, emoji = "🧣", price = 50)
        val torso = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val gear = mapOf(ItemSlot.TRINKET to cape, ItemSlot.CHEST to torso)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { name -> if (name == "layer_back_novice_cape_t1") 1003 else null }
        )

        val backdropIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.AvatarBackdrop }
        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }
        val torsoIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.CHEST }
        val capeIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.BACK }

        assertTrue("AvatarBackdrop must exist", backdropIdx >= 0)
        assertTrue("Body base must exist", bodyIdx >= 0)
        assertTrue("Torso backplate must exist", torsoIdx >= 0)
        assertTrue("Back cape must exist", capeIdx >= 0)
        assertTrue("Backdrop renders before body", backdropIdx < bodyIdx)
        assertTrue("Body base renders first", bodyIdx < torsoIdx)
        assertTrue("Torso backplate renders under back cape", torsoIdx < capeIdx)
        assertEquals("Back cape must resolve to order 11", 11, (plan.entries[capeIdx] as PaperDollRenderEntry.DrawableLayer).zIndex)
    }

    @Test
    fun planOrder_hiddenItemsProduceNoRenderEntry() {
        val sweatband = ItemEntity(id = 9, name = "Lucky Sweatband", slot = ItemSlot.TRINKET, tier = 1, emoji = "🏅", price = 60)
        val gear = mapOf(ItemSlot.TRINKET to sweatband)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { 1001 } // even if lookup returns an ID
        )

        assertEquals("AvatarBackdrop and BodyBase should exist in plan for back view", 2, plan.entries.size)
        assertTrue("First entry must be AvatarBackdrop", plan.entries[0] is PaperDollRenderEntry.AvatarBackdrop)
        assertTrue("Second entry must be BodyBase", plan.entries[1] is PaperDollRenderEntry.BodyBase)
        assertTrue("No Canvas fallback slot created", plan.entries.none { it is PaperDollRenderEntry.CanvasFallbackSlot })
        assertTrue("No Drawable layer created", plan.entries.none { it is PaperDollRenderEntry.DrawableLayer })
    }

    @Test
    fun planOrder_baseAnatomyRendersExactlyOnceInMixedPipeline() {
        val torso = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val weapon = ItemEntity(id = 11, name = "Iron Broadsword", slot = ItemSlot.WEAPON, tier = 2, emoji = "⚔️", price = 100)
        val boots = ItemEntity(id = 13, name = "Iron Greaves", slot = ItemSlot.FEET, tier = 2, emoji = "👢", price = 70)
        val gear = mapOf(ItemSlot.CHEST to torso, ItemSlot.HEAD to helm, ItemSlot.WEAPON to weapon, ItemSlot.FEET to boots)

        // Mixed: torso has drawable, others are CanvasFallback
        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = drawableLookup(
                "layer_chest_steel_plate_t3",
                "layer_chest_steel_plate_back_t3"
            )
        )

        val bodyBaseCount = plan.entries.count { it is PaperDollRenderEntry.BodyBase }
        assertEquals("Base anatomy (BodyBase) must exist exactly once in mixed mode", 1, bodyBaseCount)

        val canvasSlots = plan.entries.filterIsInstance<PaperDollRenderEntry.CanvasFallbackSlot>()
        assertEquals("Should have exactly 3 Canvas fallback slots (HEAD, FEET, WEAPON)", 3, canvasSlots.size)

        // Multiple Canvas fallback slots do NOT create multiple body passes
        assertEquals("Plan must have exactly 6 entries total (1 Backdrop + 1 BodyBase + 1 Drawable + 3 FallbackSlots)", 6, plan.entries.size)

        // Verify sequence strictly monotonic by zIndex
        for (i in 0 until plan.entries.size - 1) {
            val currZ = when (val e = plan.entries[i]) {
                is PaperDollRenderEntry.AvatarBackdrop -> e.zIndex
                is PaperDollRenderEntry.BodyBase -> e.zIndex
                is PaperDollRenderEntry.DrawableLayer -> e.zIndex
                is PaperDollRenderEntry.CanvasFallbackSlot -> e.zIndex
            }
            val nextZ = when (val e = plan.entries[i + 1]) {
                is PaperDollRenderEntry.AvatarBackdrop -> e.zIndex
                is PaperDollRenderEntry.BodyBase -> e.zIndex
                is PaperDollRenderEntry.DrawableLayer -> e.zIndex
                is PaperDollRenderEntry.CanvasFallbackSlot -> e.zIndex
            }
            assertTrue("Plan entries must be sorted strictly in ascending z-order ($currZ <= $nextZ)", currZ <= nextZ)
        }
    }

    @Test
    fun planOrder_canvasWeaponDoesNotCoverOrRedrawDrawableHeadgear() {
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val weapon = ItemEntity(id = 11, name = "Iron Broadsword", slot = ItemSlot.WEAPON, tier = 2, emoji = "⚔️", price = 100)
        val gear = mapOf(ItemSlot.HEAD to helm, ItemSlot.WEAPON to weapon)

        // Headgear has drawable, weapon is canvas fallback
        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = drawableLookup(
                "layer_head_iron_greathelm_t2",
                "layer_head_iron_greathelm_back_t2"
            )
        )

        val headIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.HEAD }
        val weaponIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.WEAPON }

        assertTrue("Drawable headgear must be in plan", headIdx >= 0)
        assertTrue("Canvas weapon must be in plan", weaponIdx >= 0)
        assertTrue("Canvas weapon (order 12) must render AFTER drawable headgear (order 10)", headIdx < weaponIdx)

        val weaponSlot = plan.entries[weaponIdx] as PaperDollRenderEntry.CanvasFallbackSlot
        assertEquals("Canvas weapon slot must only target WEAPON", ItemSlot.WEAPON, weaponSlot.slot)
    }

    @Test
    fun planOrder_canvasHeadgearContainsNoFaceBodyPass() {
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val gear = mapOf(ItemSlot.HEAD to helm)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = { null } // Pure fallback
        )

        val headSlot = plan.entries.filterIsInstance<PaperDollRenderEntry.CanvasFallbackSlot>().single()
        assertEquals("Headgear slot must be HEAD", ItemSlot.HEAD, headSlot.slot)
        assertEquals("Visual slot must be HEAD", PaperDollVisualSlot.HEAD, headSlot.visualSlot)
        assertEquals("Headgear z-index must be 10", 10, headSlot.zIndex)
    }

    @Test
    fun avatarFrame_preservesHelmetAwareHairAndRobeContext() {
        val helm = ItemEntity(id = 12, name = "Iron Greathelm", slot = ItemSlot.HEAD, tier = 2, emoji = "🪖", price = 80)
        val robe = ItemEntity(id = 14, name = "Apprentice Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 1, emoji = "🥋", price = 100)
        val gear = mapOf(ItemSlot.HEAD to helm, ItemSlot.CHEST to robe)

        val frame = com.fitnessquest.rpg.ui.components.AvatarFrame(
            u = 1f,
            cls = com.fitnessquest.rpg.domain.CharacterClass.MAGE,
            look = com.fitnessquest.rpg.ui.components.lookFor(com.fitnessquest.rpg.domain.CharacterClass.MAGE),
            gear = gear,
            costume = false,
            highlightMuscles = emptySet(),
            facingBack = false,
            appearance = com.fitnessquest.rpg.ui.components.AvatarAppearance(),
            expression = com.fitnessquest.rpg.ui.components.AvatarExpression.CALM,
            detail = com.fitnessquest.rpg.ui.components.AvatarDetail.FULL,
            phase = 0f
        )

        assertNotNull("Frame must recognize equipped helmet for hair compression", frame.head)
        assertNotNull("Frame must recognize equipped robe for sleeve/lower-body replacement", frame.robeChest)
    }

    @Test
    fun avatarFrame_preservesWeaponTierForCompanionSelection() {
        val highTierStaff = ItemEntity(id = 99, name = "Staff of the Archmage", slot = ItemSlot.WEAPON, tier = 4, emoji = "🪄", price = 500)
        val gear = mapOf(ItemSlot.WEAPON to highTierStaff)

        val frame = com.fitnessquest.rpg.ui.components.AvatarFrame(
            u = 1f,
            cls = com.fitnessquest.rpg.domain.CharacterClass.SUMMONER,
            look = com.fitnessquest.rpg.ui.components.lookFor(com.fitnessquest.rpg.domain.CharacterClass.SUMMONER),
            gear = gear,
            costume = false,
            highlightMuscles = emptySet(),
            facingBack = false,
            appearance = com.fitnessquest.rpg.ui.components.AvatarAppearance(),
            expression = com.fitnessquest.rpg.ui.components.AvatarExpression.CALM,
            detail = com.fitnessquest.rpg.ui.components.AvatarDetail.FULL,
            phase = 0f
        )

        assertEquals("Frame must preserve weapon tier for Bahamut/Shiva companion threshold checks", 4, frame.weapon?.tier)
    }

    // =======================================================================
    // Checkpoint 2: Representative Combination Pass Plan Tests
    // =======================================================================

    @Test
    fun planOrder_plateAndShield_frontUsesHeldShieldAsset() {
        val plate = ItemEntity(id = 9, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val shield = ItemEntity(id = 1197, name = "Iron Back Shield", slot = ItemSlot.TRINKET, style = "shield", tier = 2, emoji = "🛡️", price = 150)
        val sword = ItemEntity(id = 11, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "⚔️", price = 200)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = mapOf(ItemSlot.WEAPON to sword, ItemSlot.CHEST to plate, ItemSlot.TRINKET to shield),
            orientation = AvatarOrientation.FRONT,
            drawableLookup = { name ->
                when (name) {
                    "layer_trinket_iron_shield_t2" -> 2000
                    "layer_back_iron_shield_t2" -> 2001
                    else -> null
                }
            }
        )

        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }
        val shieldIdx = plan.entries.indexOfFirst {
            it is PaperDollRenderEntry.DrawableLayer &&
                it.layer.resName == "layer_trinket_iron_shield_t2"
        }

        assertTrue("QA1 front must contain the held shield asset", shieldIdx >= 0)
        assertTrue("Held shield must render in front of the body", bodyIdx < shieldIdx)
        assertEquals(
            com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HELD,
            com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
                shield,
                AvatarOrientation.FRONT
            ).attachment
        )
    }

    @Test
    fun planOrder_plateAndShield_backViewSequence() {
        val plate = ItemEntity(id = 10, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val shield = ItemEntity(id = 1196, name = "Iron Shield", slot = ItemSlot.TRINKET, style = "shield", tier = 2, emoji = "🛡️", price = 150)
        val sword = ItemEntity(id = 11, name = "Knight's Blade", slot = ItemSlot.WEAPON, tier = 3, emoji = "⚔️", price = 200)
        val gear = mapOf(ItemSlot.CHEST to plate, ItemSlot.TRINKET to shield, ItemSlot.WEAPON to sword)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { name ->
                when (name) {
                    "layer_trinket_iron_shield_t2" -> 2000
                    "layer_back_iron_shield_t2" -> 2001
                    else -> null
                }
            }
        )

        val backdropIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.AvatarBackdrop }
        val weaponIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.WEAPON }
        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }
        val plateIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.CHEST }
        val shieldIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.BACK }

        assertTrue("Backdrop must exist", backdropIdx >= 0)
        assertTrue("Equipped sword must remain visible beside the featured back shield", weaponIdx >= 0)
        assertTrue("Body base must exist", bodyIdx >= 0)
        assertTrue("Plate backplate must exist", plateIdx >= 0)
        assertTrue("Shield on back must exist in foreground", shieldIdx >= 0)

        assertTrue("BodyBase (Z=3) < Plate (Z=7)", bodyIdx < plateIdx)
        assertTrue("Plate (Z=7) < Shield (Z=11)", plateIdx < shieldIdx)
        assertTrue("Held sword must render after the back-mounted shield", shieldIdx < weaponIdx)
    }

    @Test
    fun planOrder_leatherAndQuiver_backViewSequence() {
        val leather = ItemEntity(id = 20, name = "Supple Leathers", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 2, emoji = "🥋", price = 120)
        val quiver = ItemEntity(id = 1197, name = "Hunter's Quiver", slot = ItemSlot.TRINKET, style = "quiver", tier = 1, emoji = "🏹", price = 90)
        val bow = ItemEntity(id = 21, name = "Training Shortbow", slot = ItemSlot.WEAPON, style = ItemStyle.BOW, tier = 1, emoji = "🏹", price = 100)
        val gear = mapOf(ItemSlot.CHEST to leather, ItemSlot.TRINKET to quiver, ItemSlot.WEAPON to bow)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { name ->
                when (name) {
                    "layer_back_hunters_quiver_t1" -> 2002
                    "layer_chest_supple_leathers_t2" -> 2004
                    "layer_chest_supple_leathers_back_t2" -> 2003
                    else -> null
                }
            }
        )

        val bodyIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.BodyBase }
        val leatherIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.CHEST }
        val quiverIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.BACK }

        assertTrue("Body base must exist in plan", bodyIdx >= 0)
        assertTrue("Leather armor must exist in plan (found: ${plan.entries})", leatherIdx >= 0)
        assertTrue("Quiver must exist in plan (found: ${plan.entries})", quiverIdx >= 0)
        assertTrue("Body base renders before leather armor", bodyIdx < leatherIdx)
        assertTrue("Leather armor renders before quiver on back", leatherIdx < quiverIdx)
    }

    @Test
    fun planOrder_robeAndWings_backViewSequence() {
        val robe = ItemEntity(id = 30, name = "Runeweave Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 3, emoji = "🥋", price = 300)
        val wings = ItemEntity(id = 1198, name = "Celestial Wings", slot = ItemSlot.TRINKET, style = "wings", tier = 4, emoji = "🪽", price = 1000)
        val gear = mapOf(ItemSlot.CHEST to robe, ItemSlot.TRINKET to wings)

        val plan = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { name ->
                when (name) {
                    "layer_back_celestial_wings_t4" -> 2004
                    else -> null
                }
            }
        )

        val wingsIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.DrawableLayer && it.layer.visualSlot == PaperDollVisualSlot.BACK }
        val robeIdx = plan.entries.indexOfFirst { it is PaperDollRenderEntry.CanvasFallbackSlot && it.visualSlot == PaperDollVisualSlot.CHEST }

        assertTrue("Robe back renders before celestial wings on back view", robeIdx < wingsIdx)
        assertEquals("Celestial wings resolve to order 11 in foreground", 11, (plan.entries[wingsIdx] as PaperDollRenderEntry.DrawableLayer).zIndex)
    }

    // =======================================================================
    // 2. Turnaround State Machine & Concurrent Interruption Tests
    // =======================================================================

    @Test
    fun turnaroundState_initializesWithCorrectFrontOrientation() {
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = false)
        assertEquals(0f, state.currentAngle, 0.001f)
        assertEquals(1f, state.scaleX, 0.001f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
        assertFalse(state.renderedFacingBack)
    }

    @Test
    fun turnaroundState_switchesOrientationOnlyAtMidpoint() = runTurnaroundTest { _ ->
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = false)

        // At 0 degrees (Front)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
        assertEquals(1f, state.scaleX, 0.001f)

        // At 45 degrees (still Front, horizontally compressed)
        state.angleAnimatable.snapTo(45f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
        assertEquals(0.707f, state.scaleX, 0.01f)

        // At 90 degrees (Midpoint: edge-on, swaps to Back)
        state.angleAnimatable.snapTo(90f)
        assertEquals(AvatarOrientation.BACK, state.renderedOrientation)
        assertEquals(0.01f, state.scaleX, 0.01f)

        // At 135 degrees (expanding Back)
        state.angleAnimatable.snapTo(135f)
        assertEquals(AvatarOrientation.BACK, state.renderedOrientation)
        assertEquals(0.707f, state.scaleX, 0.01f)

        // At 180 degrees (fully expanded Back)
        state.angleAnimatable.snapTo(180f)
        assertEquals(AvatarOrientation.BACK, state.renderedOrientation)
        assertEquals(1f, state.scaleX, 0.001f)
    }

    @Test
    fun turnaroundState_concurrentInterruptionBeforeMidpoint_reversesCleanly() = runTurnaroundTest { clock ->
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = false)

        val forwardJob = launch {
            state.animateTo(targetFacingBack = true)
        }

        // Advance 5 frames (~80ms into 300ms animation)
        clock.advanceFrames(5)

        val angleAtInterrupt = state.currentAngle
        assertTrue("Animation must advance before interrupt, was $angleAtInterrupt", angleAtInterrupt > 0f)
        assertTrue("Must be before midpoint (90 deg), was $angleAtInterrupt", angleAtInterrupt < 90f)
        assertEquals("Should still be FRONT before midpoint", AvatarOrientation.FRONT, state.renderedOrientation)

        // Cancel forward animation and reverse back to FRONT
        forwardJob.cancel()
        val reverseJob = launch {
            state.animateTo(targetFacingBack = false)
        }

        clock.advanceFrames(25)
        reverseJob.join()

        assertEquals("Should settle at 0 degrees", 0f, state.currentAngle, 0.01f)
        assertEquals("Scale must expand fully back to 1", 1f, state.scaleX, 0.01f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
        assertFalse(state.renderedFacingBack)
    }

    @Test
    fun turnaroundState_concurrentInterruptionAfterMidpoint_flipsAndReversesCleanly() = runTurnaroundTest { clock ->
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = false)

        val forwardJob = launch {
            state.animateTo(targetFacingBack = true)
        }

        // Advance 12 frames (~192ms, past midpoint of 300ms animation)
        clock.advanceFrames(12)

        val angleAtInterrupt = state.currentAngle
        assertTrue("Must be past midpoint (90 deg), was $angleAtInterrupt", angleAtInterrupt >= 90f)
        assertEquals("Should have swapped to BACK after midpoint", AvatarOrientation.BACK, state.renderedOrientation)

        // Cancel forward animation and reverse back to FRONT
        forwardJob.cancel()
        val reverseJob = launch {
            state.animateTo(targetFacingBack = false)
        }

        clock.advanceFrames(25)
        reverseJob.join()

        assertEquals("Should return to 0 degrees", 0f, state.currentAngle, 0.01f)
        assertEquals("Scale must expand fully back to 1", 1f, state.scaleX, 0.01f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
        assertFalse(state.renderedFacingBack)
    }

    @Test
    fun turnaroundState_controlledTargetChangesDuringAnimation() = runTurnaroundTest { clock ->
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = false)

        val forwardJob = launch {
            state.animateTo(targetFacingBack = true)
        }

        // Advance 4 frames
        clock.advanceFrames(4)

        val advancedAngle = state.currentAngle
        assertTrue("Animation must advance before controlled target change, was $advancedAngle", advancedAngle > 0f)

        // Controlled prop changes back to false mid-animation
        forwardJob.cancel()
        val reverseJob = launch {
            state.animateTo(targetFacingBack = false)
        }

        clock.advanceFrames(25)
        reverseJob.join()

        assertEquals("Should settle at 0 degrees", 0f, state.currentAngle, 0.01f)
        assertEquals("Scale must expand fully back to 1", 1f, state.scaleX, 0.01f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
    }

    @Test
    fun turnaroundState_reducedMotionSnapsImmediatelyWithoutCompression() = runTurnaroundTest { _ ->
        val state = AvatarTurnaroundState(initialFacingBack = false, isReducedMotion = true)
        assertEquals(1f, state.scaleX, 0.001f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)

        state.animateTo(targetFacingBack = true)
        assertEquals(180f, state.currentAngle, 0.001f)
        assertEquals(1f, state.scaleX, 0.001f)
        assertEquals(AvatarOrientation.BACK, state.renderedOrientation)

        state.animateTo(targetFacingBack = false)
        assertEquals(0f, state.currentAngle, 0.001f)
        assertEquals(1f, state.scaleX, 0.001f)
        assertEquals(AvatarOrientation.FRONT, state.renderedOrientation)
    }

    // =======================================================================
    // 3. Back-Facing Weapon & Procedural Wearable Animation Tests
    // =======================================================================

    @Test
    fun backFacingWeapon_placesBehindBodyAndSuppressesHandCircles() {
        val sword = ItemEntity(id = 11, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "⚔️", price = 200)
        val gear = mapOf(ItemSlot.WEAPON to sword)

        val planFront = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.FRONT,
            drawableLookup = { null }
        )
        val planBack = PaperDollPassResolver.resolvePlan(
            gear = gear,
            orientation = AvatarOrientation.BACK,
            drawableLookup = { null }
        )

        val weaponEntryFront = planFront.entries.filterIsInstance<PaperDollRenderEntry.CanvasFallbackSlot>().single { it.visualSlot == PaperDollVisualSlot.WEAPON }
        val weaponEntryBack = planBack.entries.filterIsInstance<PaperDollRenderEntry.CanvasFallbackSlot>().single { it.visualSlot == PaperDollVisualSlot.WEAPON }

        assertEquals("Front weapon must render in foreground at zIndex 12", 12, weaponEntryFront.zIndex)
        assertEquals("Back weapon must remain readable over rear armor at zIndex 12", 12, weaponEntryBack.zIndex)
    }

    @Test
    fun proceduralBackWearables_motionDiffersByPhaseAndRigidItemsRemainStatic() {
        val capeSwayPhase0 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeCapeSway(0f, 1f)
        val capeSwayPhase025 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeCapeSway(0.25f, 1f)
        val capeSwayPhase05 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeCapeSway(0.5f, 1f)
        val capeSwayPhase075 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeCapeSway(0.75f, 1f)

        assertEquals("Cape sway at phase 0 should be 0", 0f, capeSwayPhase0, 0.001f)
        assertEquals("Cape sway at phase 0.25 should peak at +3.5", 3.5f, capeSwayPhase025, 0.001f)
        assertEquals("Cape sway at phase 0.5 should cross 0", 0f, capeSwayPhase05, 0.001f)
        assertEquals("Cape sway at phase 0.75 should peak at -3.5", -3.5f, capeSwayPhase075, 0.001f)
        assertNotEquals("Cape sway must differ between phase 0 and 0.25", capeSwayPhase0, capeSwayPhase025)

        val wingFlapPhase0 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeWingFlap(0f, 1f)
        val wingFlapPhase025 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeWingFlap(0.25f, 1f)

        assertEquals("Wing flap at phase 0 should be 0", 0f, wingFlapPhase0, 0.001f)
        assertEquals("Wing flap at phase 0.25 should peak at +5.0", 5f, wingFlapPhase025, 0.001f)
        assertNotEquals("Wing flap must differ between phase 0 and 0.25", wingFlapPhase0, wingFlapPhase025)

        val quiverPhase0 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeQuiverMotion(0f, 1f)
        val quiverPhase025 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeQuiverMotion(0.25f, 1f)
        val shieldPhase0 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeShieldMotion(0f, 1f)
        val shieldPhase025 = com.fitnessquest.rpg.ui.components.ProceduralBackWearableMotion.computeShieldMotion(0.25f, 1f)

        assertEquals("Quiver must remain completely rigid at phase 0", 0f, quiverPhase0, 0.001f)
        assertEquals("Quiver must remain completely rigid at phase 0.25", 0f, quiverPhase025, 0.001f)
        assertEquals("Shield must remain completely rigid at phase 0", 0f, shieldPhase0, 0.001f)
        assertEquals("Shield must remain completely rigid at phase 0.25", 0f, shieldPhase025, 0.001f)
    }

    @Test
    fun explicitBackWearableFlutter_appliesOnlyToFlexibleAssets() {
        fun item(name: String, style: String) = ItemEntity(
            id = name.hashCode().toLong(),
            name = name,
            slot = ItemSlot.TRINKET,
            style = style,
            tier = 2,
            emoji = "",
            price = 1
        )

        assertTrue(com.fitnessquest.rpg.ui.components.shouldFlutterBackWearable(item("Velvet Cape", "cape")))
        assertTrue(com.fitnessquest.rpg.ui.components.shouldFlutterBackWearable(item("Celestial Wings", "wings")))
        assertFalse(com.fitnessquest.rpg.ui.components.shouldFlutterBackWearable(item("Iron Shield", "shield")))
        assertFalse(com.fitnessquest.rpg.ui.components.shouldFlutterBackWearable(item("Hunter's Quiver", "quiver")))
    }

    @Test
    fun headgearShape_classifiesRangerAndThiefAsHood() {
        val rangerHood = ItemEntity(id = 100, name = "Adventurer's Hood", slot = ItemSlot.HEAD, classAffinity = com.fitnessquest.rpg.domain.CharacterClass.RANGER, tier = 1, emoji = "🏹", price = 50)
        val warriorHelm = ItemEntity(id = 101, name = "Iron Greathelm", slot = ItemSlot.HEAD, style = ItemStyle.PLATE, tier = 2, emoji = "🪖", price = 100)
        val mageHat = ItemEntity(id = 102, name = "Wizard's Hat", slot = ItemSlot.HEAD, style = ItemStyle.ROBE, tier = 1, emoji = "🧙", price = 50)

        assertEquals(com.fitnessquest.rpg.ui.components.HeadgearShape.HOOD, com.fitnessquest.rpg.ui.components.GearVisuals.headgearShape(rangerHood))
        assertEquals(com.fitnessquest.rpg.ui.components.HeadgearShape.HELM, com.fitnessquest.rpg.ui.components.GearVisuals.headgearShape(warriorHelm))
        assertEquals(com.fitnessquest.rpg.ui.components.HeadgearShape.HAT, com.fitnessquest.rpg.ui.components.GearVisuals.headgearShape(mageHat))
    }

    @Test
    fun heavyPlateCoverage_preservesAnatomyNotReplacedByTheCuirassAsset() {
        val covered = CoverageProfile.HEAVY_PLATE.defaultCoveredRegions()

        assertTrue(BodyRegion.TORSO in covered)
        assertTrue(BodyRegion.HIPS in covered)
        assertFalse("Plate chest asset has no upper-arm replacement", BodyRegion.LEFT_UPPER_ARM in covered)
        assertFalse("Plate chest asset has no upper-arm replacement", BodyRegion.RIGHT_UPPER_ARM in covered)
        assertFalse("Plate chest asset has no forearm replacement", BodyRegion.LEFT_FOREARM in covered)
        assertFalse("Plate chest asset has no forearm replacement", BodyRegion.RIGHT_FOREARM in covered)
        assertFalse("Plate chest asset must never hide hands", BodyRegion.LEFT_HAND in covered)
        assertFalse("Plate chest asset must never hide hands", BodyRegion.RIGHT_HAND in covered)
    }

    @Test
    fun tunicCoverage_preservesArmsWhenTheAssetOnlyDrawsTheTorso() {
        val covered = CoverageProfile.SLEEVED_TUNIC.defaultCoveredRegions()

        assertEquals(setOf(BodyRegion.TORSO, BodyRegion.HIPS), covered)
    }

    @Test
    fun authoredDrawablePalette_isNaturalUnlessAPlayerDyeIsExplicitlySelected() {
        val plate = ItemEntity(id = 150, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val lookup: (String) -> Int? = { 1001 }

        val naturalPlan = PaperDollPassResolver.resolvePlan(
            gear = mapOf(ItemSlot.CHEST to plate),
            orientation = AvatarOrientation.FRONT,
            drawableLookup = lookup
        )
        val naturalLayer = naturalPlan.entries.filterIsInstance<PaperDollRenderEntry.DrawableLayer>().single()
        assertEquals(EquipmentDye.NATURAL, naturalLayer.layer.dye)

        val dyedPlan = PaperDollPassResolver.resolvePlan(
            gear = mapOf(ItemSlot.CHEST to plate),
            orientation = AvatarOrientation.FRONT,
            customDyes = mapOf(ItemSlot.CHEST to EquipmentDye.GOLD),
            drawableLookup = lookup
        )
        val dyedLayer = dyedPlan.entries.filterIsInstance<PaperDollRenderEntry.DrawableLayer>().single()
        assertEquals(EquipmentDye.GOLD, dyedLayer.layer.dye)
    }

    @Test
    fun robePalette_returnsConsistentColorsForRobeTiers() {
        val apprenticeRobes = ItemEntity(id = 200, name = "Apprentice Robes", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 1, emoji = "🥋", price = 50)
        val runeweaveRobe = ItemEntity(id = 201, name = "Runeweave Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 3, emoji = "🥋", price = 300)

        val palTier1 = com.fitnessquest.rpg.ui.components.GearVisuals.palette(apprenticeRobes)
        val palTier3 = com.fitnessquest.rpg.ui.components.GearVisuals.palette(runeweaveRobe)

        assertEquals("Tier 1/2 Apprentice Robes must match Cobalt Blue", androidx.compose.ui.graphics.Color(0xFF2563EB), palTier1.main)
        assertEquals("Tier 3 Runeweave Robe must match Deep Violet", androidx.compose.ui.graphics.Color(0xFF581C87), palTier3.main)
    }

    @Test
    fun leatherAndPlatePalette_matchesFrontDrawablePalettes() {
        val paddedVest = ItemEntity(id = 300, name = "Padded Vest", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 1, emoji = "🥋", price = 40)
        val suppleLeathers = ItemEntity(id = 301, name = "Supple Leathers", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 2, emoji = "🥋", price = 100)
        val steelPlate = ItemEntity(id = 302, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)

        val palPadded = com.fitnessquest.rpg.ui.components.GearVisuals.palette(paddedVest)
        val palSupple = com.fitnessquest.rpg.ui.components.GearVisuals.palette(suppleLeathers)
        val palSteel = com.fitnessquest.rpg.ui.components.GearVisuals.palette(steelPlate)

        assertEquals("Tier 1 Padded Vest must match warm reddish brown", androidx.compose.ui.graphics.Color(0xFF78350F), palPadded.main)
        assertEquals("Tier 2 Supple Leathers must match authored midnight leather", androidx.compose.ui.graphics.Color(0xFF0F172A), palSupple.main)
        assertEquals("Tier 3 Steel Plate must match slate steel", androidx.compose.ui.graphics.Color(0xFF475569), palSteel.main)
    }

    @Test
    fun sixPreviewItems_resolveToExplicitArchetypesAndMatchingPalettes() {
        // QA 1: Warrior
        val knightsBlade = ItemEntity(id = 401, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "⚔️", price = 300)
        val steelPlate = ItemEntity(id = 402, name = "Steel Plate", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 3, emoji = "🛡️", price = 250)
        val ironShield = ItemEntity(id = 403, name = "Iron Shield", slot = ItemSlot.TRINKET, tier = 2, emoji = "🛡️", price = 150)
        val ironGreathelm = ItemEntity(id = 404, name = "Iron Greathelm", slot = ItemSlot.HEAD, style = ItemStyle.PLATE, tier = 2, emoji = "🪖", price = 100)

        val descSword = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(knightsBlade)
        val descPlate = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(steelPlate)
        val descShield = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(ironShield)
        val descHelm = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(ironGreathelm)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.SWORD_KNIGHT, descSword.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF475569), descSword.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF59E0B), descSword.accentColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF06B6D4), descSword.glowColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.PLATE_STEEL_CUIRASS, descPlate.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF475569), descPlate.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1E293B), descPlate.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF06B6D4), descPlate.glowColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.SHIELD_IRON_HEATER, descShield.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF334155), descShield.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF94A3B8), descShield.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF59E0B), descShield.accentColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.HELM_CLOSED_GREATHELM, descHelm.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF9AA3AD), descHelm.primaryColor)

        // QA 2: Ranger
        val shortbow = ItemEntity(id = 405, name = "Training Shortbow", slot = ItemSlot.WEAPON, style = ItemStyle.BOW, tier = 1, emoji = "🏹", price = 50)
        val suppleLeathers = ItemEntity(id = 406, name = "Supple Leathers", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 2, emoji = "🥋", price = 100)
        val adventurersHood = ItemEntity(id = 407, name = "Adventurer's Hood", slot = ItemSlot.HEAD, classAffinity = com.fitnessquest.rpg.domain.CharacterClass.RANGER, tier = 2, emoji = "🏹", price = 100)
        val huntersQuiver = ItemEntity(id = 408, name = "Hunter's Quiver", slot = ItemSlot.TRINKET, tier = 1, emoji = "🏹", price = 80)

        val descBow = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(shortbow)
        val descLeather = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(suppleLeathers)
        val descHood = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(adventurersHood)
        val descQuiver = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(huntersQuiver)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.BOW_SHORT, descBow.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF78350F), descBow.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFB45309), descBow.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF10B981), descBow.glowColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.LEATHER_FOREST_SUPPLE, descLeather.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0F172A), descLeather.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1E293B), descLeather.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF59E0B), descLeather.accentColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.HOOD_ADVENTURER, descHood.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF047857), descHood.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFBBF24), descHood.accentColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.QUIVER_HUNTER, descQuiver.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF5D4037), descQuiver.primaryColor)

        // QA 3: Mage
        val runeweave = ItemEntity(id = 409, name = "Runeweave Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 3, emoji = "🥋", price = 300)
        val velvetCape = ItemEntity(id = 410, name = "Velvet Cape", slot = ItemSlot.TRINKET, tier = 3, emoji = "🧣", price = 200)
        val wand = ItemEntity(id = 411, name = "Apprentice Wand", slot = ItemSlot.WEAPON, style = ItemStyle.WAND, tier = 1, emoji = "🪄", price = 50)

        val descRobe = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(runeweave)
        val descCape = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(velvetCape)
        val descWand = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(wand)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.ROBE_VIOLET_RUNEWEAVE, descRobe.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF581C87), descRobe.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF06B6D4), descRobe.glowColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.CAPE_VELVET, descCape.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF6B459E), descCape.primaryColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.WAND_ARCANE, descWand.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF38BDF8), descWand.glowColor)

        // QA 4: Paladin
        val apprenticeRobes = ItemEntity(id = 412, name = "Apprentice Robes", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 2, emoji = "🥋", price = 100)
        val celestialWings = ItemEntity(id = 413, name = "Celestial Wings", slot = ItemSlot.TRINKET, tier = 4, emoji = "🪽", price = 500)

        val descApprentice = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(apprenticeRobes)
        val descWings = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(celestialWings)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.ROBE_COBALT_APPRENTICE, descApprentice.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF2563EB), descApprentice.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1E3A8A), descApprentice.secondaryColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.WINGS_CELESTIAL, descWings.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF59E0B), descWings.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFEF08A), descWings.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF67E8F9), descWings.glowColor)

        // QA 5: Barbarian
        val dragonfang = ItemEntity(id = 414, name = "Dragonfang Greatsword", slot = ItemSlot.WEAPON, style = ItemStyle.GREATSWORD, tier = 4, emoji = "🗡️", price = 600)
        val paddedVest = ItemEntity(id = 415, name = "Padded Vest", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 1, emoji = "🥋", price = 40)

        val descDragon = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(dragonfang)
        val descPadded = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(paddedVest)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_DRAGON, descDragon.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0F172A), descDragon.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF1F5F9), descDragon.secondaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF991B1B), descDragon.accentColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFEF4444), descDragon.glowColor)

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualArchetype.LEATHER_WARM_PADDED, descPadded.archetype)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF78350F), descPadded.primaryColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF451A03), descPadded.secondaryColor)
    }

    @Test
    fun greatswordDifferentiation_dragonfangVsGenericTier4() {
        val dragonfang = ItemEntity(id = 501, name = "Dragonfang Greatsword", slot = ItemSlot.WEAPON, style = ItemStyle.GREATSWORD, tier = 4, emoji = "🗡️", price = 600)
        val genericTier4Greatsword = ItemEntity(id = 502, name = "Colossal Claymore", slot = ItemSlot.WEAPON, style = ItemStyle.GREATSWORD, tier = 4, emoji = "🗡️", price = 500)

        val descDragon = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(dragonfang)
        val descGeneric = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveVisualDescriptor(genericTier4Greatsword)

        assertEquals("Dragonfang Greatsword must resolve to dragon archetype", com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_DRAGON, descDragon.archetype)
        assertEquals("Colossal Claymore must resolve to generic greatsword archetype", com.fitnessquest.rpg.domain.visuals.VisualArchetype.GREATSWORD_GENERIC, descGeneric.archetype)
        assertNotEquals("Dragonfang and generic greatswords must have different primary palettes", descDragon.primaryColor, descGeneric.primaryColor)
    }

    @Test
    fun resourceResolution_incompleteDirectionalPairsFallbackAtomically() {
        val dummyLookup: (String) -> Int? = { name ->
            when (name) {
                "layer_weapon_training_shortbow_t1" -> 1001
                "layer_chest_runeweave_robe_t3" -> 1002
                "layer_chest_apprentice_robes_t2" -> 1003
                "layer_back_iron_shield_t2" -> 1004
                "layer_trinket_iron_shield_t2" -> 1006
                "layer_back_celestial_wings_t4" -> 1005
                else -> null
            }
        }

        val shortbow = ItemEntity(id = 601, name = "Training Shortbow", slot = ItemSlot.WEAPON, style = ItemStyle.BOW, tier = 1, emoji = "🏹", price = 50)
        val runeweave = ItemEntity(id = 602, name = "Runeweave Robe", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 3, emoji = "🥋", price = 300)
        val apprentice = ItemEntity(id = 603, name = "Apprentice Robes", slot = ItemSlot.CHEST, style = ItemStyle.ROBE, tier = 2, emoji = "🥋", price = 100)
        val ironShield = ItemEntity(id = 604, name = "Iron Shield", slot = ItemSlot.TRINKET, tier = 2, emoji = "🛡️", price = 150)
        val celestialWings = ItemEntity(id = 605, name = "Celestial Wings", slot = ItemSlot.TRINKET, tier = 4, emoji = "🪽", price = 500)

        val specBow = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(shortbow)
        val specRuneweave = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(runeweave)
        val specApprentice = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(apprentice)
        val specShield = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(ironShield)
        val specWings = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(celestialWings)

        // Incomplete directional art falls back atomically so the same procedural
        // identity is used on both sides.
        val resBowFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specBow, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT, drawableLookup = dummyLookup)
        val resBowBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specBow, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK, drawableLookup = dummyLookup)
        assertTrue("Incomplete shortbow front must use the coherent fallback", resBowFront is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)
        assertTrue("Shortbow back must resolve to CanvasFallback", resBowBack is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)

        val resRobeFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specRuneweave, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT, drawableLookup = dummyLookup)
        val resRobeBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specRuneweave, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK, drawableLookup = dummyLookup)
        assertTrue("Incomplete Runeweave front must use the coherent fallback", resRobeFront is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)
        assertTrue("Runeweave back must resolve to CanvasFallback", resRobeBack is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)

        val resApprenticeFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specApprentice, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT, drawableLookup = dummyLookup)
        val resApprenticeBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specApprentice, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK, drawableLookup = dummyLookup)
        assertTrue("Incomplete apprentice robes front must use the coherent fallback", resApprenticeFront is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)
        assertTrue("Apprentice robes back must resolve to CanvasFallback", resApprenticeBack is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.CanvasFallback)

        // Iron Shield: drawable in both orientations
        val resShieldFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specShield, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT, drawableLookup = dummyLookup)
        val resShieldBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specShield, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK, drawableLookup = dummyLookup)
        assertTrue("Iron Shield front must resolve to Drawable", resShieldFront is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable)
        assertTrue("Iron Shield back must resolve to Drawable", resShieldBack is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable)
        assertEquals("layer_trinket_iron_shield_t2", (resShieldFront as com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable).resName)
        assertEquals("layer_back_iron_shield_t2", (resShieldBack as com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable).resName)

        // Celestial Wings: drawable in both orientations
        val resWingsFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specWings, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT, drawableLookup = dummyLookup)
        val resWingsBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveResolution(specWings, com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK, drawableLookup = dummyLookup)
        assertTrue("Celestial Wings front must resolve to Drawable", resWingsFront is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable)
        assertTrue("Celestial Wings back must resolve to Drawable", resWingsBack is com.fitnessquest.rpg.domain.visuals.EquipmentLayerResolution.Drawable)
    }

    @Test
    fun qa5RearArmor_resolvesMatchingAuthoredBackAssets() {
        val paddedVest = ItemEntity(id = 601, name = "Padded Vest", slot = ItemSlot.CHEST, style = ItemStyle.LIGHT, tier = 1, emoji = "🥋", price = 40)
        val greaves = ItemEntity(id = 602, name = "Veteran's Greaves", slot = ItemSlot.LEGS, style = ItemStyle.PLATE, tier = 3, emoji = "🦿", price = 230)
        val boots = ItemEntity(id = 603, name = "Old Boots", slot = ItemSlot.FEET, style = ItemStyle.LIGHT, tier = 1, emoji = "🥾", price = 25)
        val expected = mapOf(
            paddedVest to Pair("layer_chest_padded_vest_t1", "layer_chest_padded_vest_back_t1"),
            greaves to Pair("layer_legs_veterans_greaves_t3", "layer_legs_veterans_greaves_back_t3"),
            boots to Pair("layer_feet_old_boots_t1", "layer_feet_old_boots_back_t1")
        )
        val authoredNames = expected.values.flatMap { listOf(it.first, it.second) }.toSet()
        val lookup: (String) -> Int? = { name -> if (name in authoredNames) name.hashCode() else null }

        expected.forEach { (item, resourcePair) ->
            val resolution = EquipmentVisualRegistry.resolveResolution(
                spec = EquipmentVisualRegistry.resolveSpec(item),
                orientation = AvatarOrientation.BACK,
                drawableLookup = lookup
            )
            val resourceName = resourcePair.second
            assertTrue("$resourceName must resolve as an authored back drawable", resolution is EquipmentLayerResolution.Drawable)
            assertEquals(resourceName, (resolution as EquipmentLayerResolution.Drawable).resName)
        }
    }

    @Test
    fun starterLeatherRear_resolvesAuthoredWornTrousersBackAsset() {
        val trousers = ItemEntity(
            id = 52,
            name = "Worn Trousers",
            slot = ItemSlot.LEGS,
            style = ItemStyle.LIGHT,
            tier = 1,
            emoji = "👖",
            price = 25
        )
        val frontResource = "layer_legs_worn_trousers_t1"
        val expectedResource = "layer_legs_worn_trousers_back_t1"

        val resolution = EquipmentVisualRegistry.resolveResolution(
            spec = EquipmentVisualRegistry.resolveSpec(trousers),
            orientation = AvatarOrientation.BACK,
            drawableLookup = { name -> if (name == frontResource || name == expectedResource) 1 else null }
        )

        assertTrue(
            "Worn Trousers rear view must not fall back to generic light legs",
            resolution is EquipmentLayerResolution.Drawable
        )
        assertEquals(expectedResource, (resolution as EquipmentLayerResolution.Drawable).resName)
    }

    @Test
    fun orientationContracts_definePlacementOncePerArchetype() {
        val dragonfang = ItemEntity(id = 7001, name = "Dragonfang Greatsword", slot = ItemSlot.WEAPON, style = ItemStyle.GREATSWORD, tier = 4, emoji = "", price = 1)
        val knightBlade = ItemEntity(id = 7002, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "", price = 1)
        val wand = ItemEntity(id = 7003, name = "Apprentice Wand", slot = ItemSlot.WEAPON, style = ItemStyle.WAND, tier = 1, emoji = "", price = 1)
        val wings = ItemEntity(id = 7004, name = "Celestial Wings", slot = ItemSlot.TRINKET, style = "wings", tier = 4, emoji = "", price = 1)
        val shield = ItemEntity(id = 7005, name = "Iron Shield", slot = ItemSlot.TRINKET, style = "shield", tier = 2, emoji = "", price = 1)

        val dragonBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            dragonfang,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK
        )
        val knightBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            knightBlade,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK
        )
        val wandBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            wand,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK
        )
        val wingsBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            wings,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK
        )
        val shieldFront = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            shield,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.FRONT
        )
        val shieldBack = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolvePresentation(
            shield,
            com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK
        )

        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.BACK_MOUNTED, dragonBack.attachment)
        assertEquals(145f, dragonBack.anchor.rotationDegrees)
        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.SIDE_CARRY, knightBack.attachment)
        assertEquals(com.fitnessquest.rpg.domain.visuals.BackOcclusionPolicy.HIDE_WHEN_BACK_FEATURE, knightBack.occlusionPolicy)
        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.SIDE_CARRY, wandBack.attachment)
        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.BACK_FEATURE, wingsBack.attachment)
        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HELD, shieldFront.attachment)
        assertEquals(com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.BACK_FEATURE, shieldBack.attachment)
    }

    @Test
    fun backFeatureMovesWeaponsToHeldPresentationInsteadOfHidingThem() {
        val knightBlade = ItemEntity(id = 7101, name = "Knight's Blade", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "", price = 1)
        val dragonfang = ItemEntity(id = 7102, name = "Dragonfang Greatsword", slot = ItemSlot.WEAPON, style = ItemStyle.GREATSWORD, tier = 4, emoji = "", price = 1)
        val trainingSpear = ItemEntity(id = 7104, name = "Training Spear", slot = ItemSlot.WEAPON, style = "spear", tier = 1, emoji = "", price = 1)
        val wings = ItemEntity(id = 7103, name = "Celestial Wings", slot = ItemSlot.TRINKET, style = "wings", tier = 4, emoji = "", price = 1)

        val knightPlan = PaperDollPassResolver.resolvePlan(
            gear = mapOf(ItemSlot.WEAPON to knightBlade, ItemSlot.TRINKET to wings),
            orientation = com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK,
            drawableLookup = { null }
        )
        val dragonPlan = PaperDollPassResolver.resolvePlan(
            gear = mapOf(ItemSlot.WEAPON to dragonfang, ItemSlot.TRINKET to wings),
            orientation = com.fitnessquest.rpg.domain.visuals.AvatarOrientation.BACK,
            drawableLookup = { null }
        )
        val spearBack = EquipmentVisualRegistry.resolvePresentation(
            trainingSpear,
            AvatarOrientation.BACK
        )
        val spearWithWings = EquipmentVisualRegistry.resolvePresentation(
            trainingSpear,
            AvatarOrientation.BACK,
            hasFeaturedBackItem = true
        )

        assertTrue(knightPlan.entries.any {
            it is PaperDollRenderEntry.CanvasFallbackSlot && it.slot == ItemSlot.WEAPON
        })
        assertTrue(dragonPlan.entries.any {
            it is PaperDollRenderEntry.CanvasFallbackSlot && it.slot == ItemSlot.WEAPON
        })
        assertEquals(VisualAttachmentMode.BACK_MOUNTED, spearBack.attachment)
        assertEquals(-32f, spearBack.anchor.rotationDegrees)
        assertEquals(VisualAttachmentMode.HELD, spearWithWings.attachment)
        assertEquals(BackOcclusionPolicy.ALWAYS_VISIBLE, spearWithWings.occlusionPolicy)
    }

    @Test
    fun catalogItems_allResolveAnOrientationContract() {
        com.fitnessquest.rpg.domain.ItemCatalog.all.forEach { item ->
            val spec = com.fitnessquest.rpg.domain.visuals.EquipmentVisualRegistry.resolveSpec(item)
            assertNotNull("Missing orientation contract for ${item.name}", spec.orientationContract)
            assertNotEquals(
                "Front presentation cannot be hidden for ${item.name}",
                com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HIDDEN,
                spec.orientationContract.front.attachment
            )
        }
    }

    @Test
    fun catalogItems_incompleteDirectionalArtworkNeverMixesDrawableAndFallback() {
        com.fitnessquest.rpg.domain.ItemCatalog.all
            .filter { it.slot.isEquippable() }
            .forEach { item ->
                val spec = EquipmentVisualRegistry.resolveSpec(item)
                if (spec.facingRule != VisualFacingRule.BOTH) return@forEach

                // Model the historically dangerous state: only the authored front
                // name exists. Both orientations must choose the procedural path.
                val frontOnlyLookup: (String) -> Int? = { name ->
                    if (name == spec.layerResName) 1 else null
                }
                val front = EquipmentVisualRegistry.resolveResolution(
                    spec = spec,
                    orientation = AvatarOrientation.FRONT,
                    drawableLookup = frontOnlyLookup
                )
                val back = EquipmentVisualRegistry.resolveResolution(
                    spec = spec,
                    orientation = AvatarOrientation.BACK,
                    drawableLookup = frontOnlyLookup
                )

                val sharesOneAsset = spec.visualSlot == PaperDollVisualSlot.BACK &&
                    (spec.backLayerResName == null || spec.backLayerResName == spec.layerResName)
                if (sharesOneAsset) {
                    assertTrue(
                        "Shared back feature ${item.name} should use its single authored asset",
                        front is EquipmentLayerResolution.Drawable && back is EquipmentLayerResolution.Drawable
                    )
                } else {
                    assertTrue(
                        "Incomplete front for ${item.name} must fall back procedurally",
                        front is EquipmentLayerResolution.CanvasFallback
                    )
                    assertTrue(
                        "Incomplete back for ${item.name} must fall back procedurally",
                        back is EquipmentLayerResolution.CanvasFallback
                    )
                }
            }
    }

    @Test
    fun proceduralSignature_isStableForTheSameCatalogIdentity() {
        val first = ItemEntity(id = 8101, name = "Runebound Sabre", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 3, emoji = "", price = 1)
        val reloaded = first.copy(id = 9999, price = 500)

        val firstSignature = EquipmentVisualRegistry.resolveVisualDescriptor(first).proceduralSignature
        val reloadedSignature = EquipmentVisualRegistry.resolveVisualDescriptor(reloaded).proceduralSignature

        assertEquals("Database row metadata must not reroll an item's appearance", firstSignature, reloadedSignature)
        assertEquals(firstSignature, EquipmentVisualRegistry.resolveVisualDescriptor(first).proceduralSignature)
    }

    @Test
    fun polearmNames_resolveAsSpearsEvenWhenLegacyStyleIsSwordOrGreatsword() {
        val trainingSpear = ItemEntity(id = 8110, name = "Training Spear", slot = ItemSlot.WEAPON, style = ItemStyle.SWORD, tier = 1, emoji = "", price = 1)
        val ironLance = trainingSpear.copy(id = 8111, name = "Iron Lance", tier = 2)
        val mercenaryHalberd = trainingSpear.copy(id = 8112, name = "Mercenary Halberd", style = ItemStyle.GREATSWORD, tier = 3)

        listOf(trainingSpear, ironLance, mercenaryHalberd).forEach { item ->
            val descriptor = EquipmentVisualRegistry.resolveVisualDescriptor(item)
            assertEquals(
                "${item.name} must use a polearm silhouette rather than its legacy ${item.style} fallback",
                com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
                descriptor.archetype
            )
            assertEquals(
                com.fitnessquest.rpg.domain.visuals.VisualAttachmentMode.HELD,
                descriptor.orientationContract.front.attachment
            )
        }
    }

    @Test
    fun catalogWeaponNames_resolveToTheirSemanticSilhouettesInsteadOfLegacyStyles() {
        val expected = mapOf(
            "Worn Harpoon" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Iron Lance" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Wyrmslayer Spear" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Gae Bolg" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Training Spear" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Mercenary Halberd" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Aegis-Breaker Pike" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SPEAR_GENERIC,
            "Chipped War Axe" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
            "Ragecleaver" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
            "Riftbreaker Greataxe" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
            "Woodcutter's Axe" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.AXE,
            "Swiftwind Scythe" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCYTHE,
            "Novice Handwraps" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP,
            "Iron Palm Bands" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP,
            "Dragon Palm Relics" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.UNARMED_WRAP,
            "Stormstep Tonfa" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.TONFA,
            "Practice Lute" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
            "Novice Horn" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
            "Primal Flute" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
            "Apocalypse Horn" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.INSTRUMENT,
            "Blood-Iron Maul" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.HAMMER,
            "Thunderstrike Greatclub" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB,
            "Novice Bludgeon" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.CLUB,
            "Archmage's Scepter" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER,
            "Seraphic Scepter" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.SCEPTER,
            "Crystal Focus" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARCANE_FOCUS,
        )
        val catalogByName = com.fitnessquest.rpg.domain.ItemCatalog.all.associateBy { it.name }

        expected.forEach { (name, archetype) ->
            val item = catalogByName[name]
            assertNotNull("Catalog item missing: $name", item)
            assertEquals("Wrong visual silhouette for $name (${item!!.style})", archetype,
                EquipmentVisualRegistry.resolveVisualDescriptor(item).archetype)
        }
    }

    @Test
    fun armorMaterialNames_overrideLegacyStylePaletteWhenNecessary() {
        val catalogByName = com.fitnessquest.rpg.domain.ItemCatalog.all.associateBy { it.name }
        val cloth = EquipmentVisualRegistry.resolveDefaultPalette(catalogByName.getValue("Cloth Gloves"))
        val leather = EquipmentVisualRegistry.resolveDefaultPalette(catalogByName.getValue("Leather Bracers"))
        val iron = EquipmentVisualRegistry.resolveDefaultPalette(catalogByName.getValue("Iron Vambraces"))

        assertEquals(androidx.compose.ui.graphics.Color(0xFF2563EB), cloth.first)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF78350F), leather.first)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF9AA3AD), iron.first)
        assertNotEquals("Cloth and leather must not collapse to one material palette", cloth.first, leather.first)
        assertNotEquals("Leather and iron must not collapse to one material palette", leather.first, iron.first)
    }

    @Test
    fun catalogArmorNames_resolveToSemanticGarmentAndHeadgearShapes() {
        val catalogByName = com.fitnessquest.rpg.domain.ItemCatalog.all.associateBy { it.name }
        val expectedChest = mapOf(
            "Ringmail Vest" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_MAIL,
            "Chainmail Shirt" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_MAIL,
            "Wildstalker Cloak" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK,
            "Nightveil Shroud" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK,
            "Mantle of the Wilds" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK,
            "Mythic Wayfarer Mantle" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK,
            "Traveler's Cloak" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_CLOAK,
            "Colossus Pauldrons" to com.fitnessquest.rpg.domain.visuals.VisualArchetype.ARMOR_PAULDRONS,
        )
        expectedChest.forEach { (name, archetype) ->
            val item = catalogByName.getValue(name)
            assertEquals("Wrong garment silhouette for $name", archetype,
                EquipmentVisualRegistry.resolveVisualDescriptor(item).archetype)
        }

        assertEquals(
            com.fitnessquest.rpg.ui.components.HeadgearShape.HOOD,
            com.fitnessquest.rpg.ui.components.GearVisuals.headgearShape(catalogByName.getValue("Veteran's Coif"))
        )
        assertEquals(
            com.fitnessquest.rpg.ui.components.HeadgearShape.CROWN,
            com.fitnessquest.rpg.ui.components.GearVisuals.headgearShape(catalogByName.getValue("Mythic Wayfarer Crown"))
        )
    }

    @Test
    fun proceduralSignature_tierControlsPrestigeWithoutSaveMigration() {
        val common = ItemEntity(id = 8201, name = "Vanguard Cuirass", slot = ItemSlot.CHEST, style = ItemStyle.PLATE, tier = 1, emoji = "", price = 1)
        val legendary = common.copy(id = 8202, tier = 4)

        val commonSignature = EquipmentVisualRegistry.resolveVisualDescriptor(common).proceduralSignature
        val legendarySignature = EquipmentVisualRegistry.resolveVisualDescriptor(legendary).proceduralSignature

        assertEquals(ProceduralMaterialFinish.WORN, commonSignature.finish)
        assertEquals(1, commonSignature.detailCount)
        assertTrue(commonSignature.ornament in setOf(ProceduralOrnament.NONE, ProceduralOrnament.STUDS))
        assertEquals(ProceduralMaterialFinish.ASCENDANT, legendarySignature.finish)
        assertEquals(4, legendarySignature.detailCount)
        assertNotEquals(ProceduralOrnament.NONE, legendarySignature.ornament)
    }

    @Test
    fun proceduralSignature_producesCatalogScaleVisualDiversityWithinSafeBounds() {
        val signatures = (1..24).map { index ->
            val item = ItemEntity(
                id = 8300L + index,
                name = "Forgemaster Pattern $index",
                slot = ItemSlot.CHEST,
                style = ItemStyle.PLATE,
                tier = 3,
                emoji = "",
                price = index
            )
            EquipmentVisualRegistry.resolveVisualDescriptor(item).proceduralSignature
        }

        assertTrue("Procedural equipment should not collapse to a handful of identical looks", signatures.toSet().size >= 16)
        signatures.forEach { signature ->
            assertTrue(signature.accentFamily in 0..5)
            assertEquals(3, signature.detailCount)
        }
    }
}
