package com.fitnessquest.rpg

import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.db.*
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CharacterRace
import com.fitnessquest.rpg.domain.build
import com.fitnessquest.rpg.domain.visuals.PaperDollLayerOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

private inline fun <reified T> createDummyDao(): T {
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ ->
        val returnType = method.returnType
        if (returnType == java.util.List::class.java || returnType.name.contains("List")) {
            emptyList<Any>()
        } else if (returnType == Boolean::class.javaPrimitiveType) {
            false
        } else if (returnType == Int::class.javaPrimitiveType) {
            0
        } else if (returnType == Long::class.javaPrimitiveType) {
            0L
        } else {
            null
        }
    } as T
}

class GearSanitizationAndVisualsTest {

    private lateinit var db: FakeDatabase
    private lateinit var repository: GameRepository

    private class FakeDatabase : AppDatabase() {
        var character: CharacterEntity = CharacterEntity(id = 1L, name = "Hero")
        val gearInstances = mutableListOf<GearInstanceEntity>()
        val items = mutableListOf<ItemEntity>()
        val classProgress = mutableListOf<ClassProgressEntity>()

        override fun characterDao(): CharacterDao = object : CharacterDao by createDummyDao() {
            override suspend fun get(): CharacterEntity? = character
            override suspend fun upsert(character: CharacterEntity) { this@FakeDatabase.character = character }
            override fun observe() = MutableStateFlow(character)
        }

        override fun gearInstanceDao(): GearInstanceDao = object : GearInstanceDao by createDummyDao() {
            override suspend fun get(id: Long): GearInstanceEntity? = gearInstances.find { it.id == id }
            override suspend fun getAll(): List<GearInstanceEntity> = gearInstances
            override suspend fun insert(instance: GearInstanceEntity): Long {
                val id = (gearInstances.maxOfOrNull { it.id } ?: 0L) + 1L
                val entity = instance.copy(id = id)
                gearInstances.add(entity)
                return id
            }
            override fun observeAll() = MutableStateFlow(gearInstances)
        }

        override fun itemDao(): ItemDao = object : ItemDao by createDummyDao() {
            override suspend fun get(id: Long): ItemEntity? = items.find { it.id == id }
            override suspend fun getAll(): List<ItemEntity> = items
            override fun observeAll() = MutableStateFlow(items)
        }

        override fun classProgressDao(): ClassProgressDao = object : ClassProgressDao by createDummyDao() {
            override suspend fun get(clazz: CharacterClass): ClassProgressEntity? =
                classProgress.find { it.clazz == clazz }
            override suspend fun getAll(): List<ClassProgressEntity> = classProgress
            override suspend fun upsert(progress: ClassProgressEntity) {
                classProgress.removeIf { it.clazz == progress.clazz }
                classProgress.add(progress)
            }
            override fun observeAll() = MutableStateFlow(classProgress)
        }

        override fun workoutDao(): WorkoutDao = createDummyDao()
        override fun sessionDao(): SessionDao = createDummyDao()
        override fun biomeProgressDao(): BiomeProgressDao = createDummyDao()
        override fun movementMasteryDao(): MovementMasteryDao = createDummyDao()
        override fun activeSessionDao(): ActiveSessionDao = createDummyDao()
        override fun bodyMetricDao(): BodyMetricDao = createDummyDao()
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker =
            androidx.room.InvalidationTracker(this, emptyMap(), emptyMap(), "character")
        override fun clearAllTables() {}
    }

    @Before
    fun setUp() {
        db = FakeDatabase()
        repository = GameRepository(db)
    }

    @Test
    fun paperDollLayerOrder_hasCorrectRelativeOrdering() {
        assertTrue("Legs must be below feet", PaperDollLayerOrder.GEAR_LEGS.zIndex < PaperDollLayerOrder.GEAR_FEET.zIndex)
        assertTrue("Feet must be below torso/robes", PaperDollLayerOrder.GEAR_FEET.zIndex < PaperDollLayerOrder.GEAR_TORSO.zIndex)
        assertTrue("Torso must be below trinkets", PaperDollLayerOrder.GEAR_TORSO.zIndex < PaperDollLayerOrder.GEAR_TRINKET.zIndex)
        assertTrue("Head must be below weapons", PaperDollLayerOrder.GEAR_HEAD.zIndex < PaperDollLayerOrder.GEAR_WEAPON.zIndex)
    }

    @Test
    fun raceBuild_providesDistinctProportions() {
        val human = CharacterRace.HUMAN.build()
        assertEquals(1f, human.width, 0.01f)
        assertEquals(1f, human.height, 0.01f)

        val undead = CharacterRace.UNDEAD.build()
        assertEquals(0.86f, undead.width, 0.01f)
        assertEquals(1.08f, undead.height, 0.01f)

        val dwarf = CharacterRace.DWARF.build()
        assertEquals(1.22f, dwarf.width, 0.01f)
        assertEquals(0.76f, dwarf.height, 0.01f)
    }

    @Test
    fun sanitizeEquippedGear_healsMisplacedSlots() = runBlocking {
        // Setup catalog items
        db.items.add(ItemEntity(id = 50, name = "Leather Cap", slot = ItemSlot.HEAD, emoji = "🧢", tier = 1, price = 30))
        db.items.add(ItemEntity(id = 1201, name = "Boar's Slumber Girdle", slot = ItemSlot.TRINKET, emoji = "🐗", tier = 2, price = 350))
        db.items.add(ItemEntity(id = 2005, name = "Bonebound Slippers", slot = ItemSlot.FEET, emoji = "🩰", tier = 2, price = 50))

        // Create instances
        db.gearInstances.add(GearInstanceEntity(id = 101, catalogId = 50))    // Leather Cap (HEAD)
        db.gearInstances.add(GearInstanceEntity(id = 102, catalogId = 1201))  // Boar's Slumber Girdle (TRINKET)
        db.gearInstances.add(GearInstanceEntity(id = 103, catalogId = 2005))  // Bonebound Slippers (FEET)

        // Intentionally create character with scrambled slots (Cap in LEGS, Girdle in HEAD, Slippers in TRINKET)
        val scrambled = CharacterEntity(
            id = 1L,
            name = "Sn3akySl0th",
            headId = 102L,    // Girdle (TRINKET) in Head
            legsId = 101L,    // Leather Cap (HEAD) in Legs
            trinketId = 103L  // Bonebound Slippers (FEET) in Trinket
        )
        db.character = scrambled

        val sanitized = repository.sanitizeEquippedGear(scrambled)

        // Verify items were moved to their correct slots
        assertEquals(101L, sanitized.headId)     // Leather Cap is now in Head
        assertEquals(102L, sanitized.trinketId)  // Boar's Slumber Girdle is now in Trinket
        assertEquals(103L, sanitized.feetId)     // Bonebound Slippers is now in Feet
        assertNull(sanitized.legsId)             // Legs slot is cleanly freed up
    }
}
