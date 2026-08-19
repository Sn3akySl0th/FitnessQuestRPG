package com.fitnessquest.rpg.domain.visuals

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentVisualRegistryTest {

    private fun createTestContext(
        pkg: String = "com.fitnessquest.rpg",
        resolver: (name: String?, defType: String?, defPackage: String?) -> Int
    ): Context {
        val testResources = object : Resources(null, null, null) {
            override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
                return resolver(name, defType, defPackage)
            }

            override fun openRawResource(id: Int): java.io.InputStream {
                if (id == 0) throw Resources.NotFoundException("Resource ID 0")
                return java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3))
            }
        }

        return object : ContextWrapper(null) {
            override fun getPackageName(): String = pkg
            override fun getResources(): Resources = testResources
        }
    }

    @Test
    fun `findDrawableId returns null for blank or null resource name`() {
        val context = createTestContext { _, _, _ -> 0 }
        assertNull(EquipmentVisualRegistry.findDrawableId(context, null))
        assertNull(EquipmentVisualRegistry.findDrawableId(context, ""))
        assertNull(EquipmentVisualRegistry.findDrawableId(context, "   "))
    }

    @Test
    fun `findDrawableId returns null without throwing when drawable is missing`() {
        var lookupCount = 0
        val context = createTestContext { name, defType, pkg ->
            lookupCount++
            assertEquals("drawable", defType)
            assertEquals("com.fitnessquest.rpg", pkg)
            0 // not found
        }

        val missingName = "layer_weapon_missing_item_t1"
        val result = EquipmentVisualRegistry.findDrawableId(context, missingName)

        assertNull(result)
        assertEquals(1, lookupCount)
    }

    @Test
    fun `findDrawableId repeatedly queried for missing drawable returns null from cache without throwing`() {
        var lookupCount = 0
        val context = createTestContext { _, _, _ ->
            lookupCount++
            0
        }

        val missingName = "layer_head_uninstalled_helmet_t2"
        val result1 = EquipmentVisualRegistry.findDrawableId(context, missingName)
        val result2 = EquipmentVisualRegistry.findDrawableId(context, missingName)
        val result3 = EquipmentVisualRegistry.findDrawableId(context, missingName)

        assertNull(result1)
        assertNull(result2)
        assertNull(result3)
        // Verified lookup occurs once and caches 0 (sentinel) in ConcurrentHashMap
        assertEquals(1, lookupCount)
    }

    @Test
    fun `findDrawableId returns resource ID when drawable exists and caches it`() {
        var lookupCount = 0
        val expectedResId = 2131230845
        val context = createTestContext { name, _, _ ->
            lookupCount++
            if (name == "layer_weapon_real_sword_t3") expectedResId else 0
        }

        val realName = "layer_weapon_real_sword_t3"
        val result1 = EquipmentVisualRegistry.findDrawableId(context, realName)
        val result2 = EquipmentVisualRegistry.findDrawableId(context, realName)

        assertNotNull(result1)
        assertEquals(expectedResId, result1)
        assertEquals(expectedResId, result2)
        assertEquals(1, lookupCount)
    }

    @Test
    fun `findDrawableId safely catches openRawResource NotFoundException from synthetic preview IDs and returns null`() {
        val testResources = object : Resources(null, null, null) {
            override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
                return 0x7F08FFFD // Synthetic preview ID
            }

            override fun openRawResource(id: Int): java.io.InputStream {
                throw Resources.NotFoundException("Could not find drawable resource matching value 0x7F08FFFD")
            }
        }

        val context = object : ContextWrapper(null) {
            override fun getPackageName(): String = "com.fitnessquest.rpg"
            override fun getResources(): Resources = testResources
        }

        val missingPreviewAssetName = "layer_chest_aegis_of_the_titan_t4"
        val result = EquipmentVisualRegistry.findDrawableId(context, missingPreviewAssetName)

        assertNull(result)
    }
}
