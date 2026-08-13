package com.fitnessquest.rpg

import androidx.room.InvalidationTracker
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.db.ActiveSessionDao
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.BiomeProgressDao
import com.fitnessquest.rpg.data.db.BiomeProgressEntity
import com.fitnessquest.rpg.data.db.BodyMetricDao
import com.fitnessquest.rpg.data.db.CharacterDao
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ClassProgressDao
import com.fitnessquest.rpg.data.db.GearInstanceDao
import com.fitnessquest.rpg.data.db.GearInstanceEntity
import com.fitnessquest.rpg.data.db.ItemDao
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.data.db.SessionDao
import com.fitnessquest.rpg.data.db.WorkoutDao
import com.fitnessquest.rpg.data.db.isEquippable
import com.fitnessquest.rpg.domain.Biome
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.ItemCatalog
import com.fitnessquest.rpg.domain.MonsterCatalog
import com.fitnessquest.rpg.domain.ProgressionRules
import com.fitnessquest.rpg.domain.Reward
import com.fitnessquest.rpg.domain.RewardSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation

private inline fun <reified T> createDummyDao(): T {
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, args ->
        val returnType = method.returnType
        val lastArg = args?.lastOrNull()
        if (lastArg is Continuation<*>) {
            if (method.name.contains("List") || method.name.contains("All") || method.name.startsWith("by")) {
                emptyList<Any>()
            } else {
                null
            }
        } else if (returnType == java.util.List::class.java || returnType.name.contains("List")) {
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

class BossVictoryTest {

    private lateinit var db: FakeDatabase
    private lateinit var repository: GameRepository

    private class FakeDatabase : AppDatabase() {
        var character: CharacterEntity? = null
        val biomeProgressMap = mutableMapOf<String, BiomeProgressEntity>()
        val itemsMap = mutableMapOf<Long, ItemEntity>()
        val gearInstances = mutableMapOf<Long, GearInstanceEntity>()
        var nextGearId = 1L

        private val characterFlow = MutableStateFlow<CharacterEntity?>(null)
        private val allBiomeProgressFlow = MutableStateFlow<List<BiomeProgressEntity>>(emptyList())

        fun updateProgressFlow() {
            allBiomeProgressFlow.value = biomeProgressMap.values.toList()
        }

        val characterDaoProxy = Proxy.newProxyInstance(
            CharacterDao::class.java.classLoader,
            arrayOf(CharacterDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "get" -> character
                "observe", "getCharacterFlow" -> characterFlow
                "upsert", "update" -> {
                    val c = args[0] as CharacterEntity
                    character = c
                    characterFlow.value = c
                    null
                }
                else -> null
            }
        } as CharacterDao

        val biomeProgressDaoProxy = Proxy.newProxyInstance(
            BiomeProgressDao::class.java.classLoader,
            arrayOf(BiomeProgressDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "get" -> biomeProgressMap[args[0] as String]
                "observeAll", "allProgressFlow" -> allBiomeProgressFlow
                "all" -> biomeProgressMap.values.toList()
                "upsert", "insertOrUpdate" -> {
                    val entity = args[0] as BiomeProgressEntity
                    biomeProgressMap[entity.biomeName] = entity
                    updateProgressFlow()
                    null
                }
                else -> null
            }
        } as BiomeProgressDao

        val itemDaoProxy = Proxy.newProxyInstance(
            ItemDao::class.java.classLoader,
            arrayOf(ItemDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "get" -> {
                    val id = args[0] as Long
                    itemsMap[id] ?: ItemCatalog.all.find { it.id == id } ?: ItemEntity(id, "Item $id", "⚔️", ItemSlot.WEAPON, 1, 50)
                }
                "gearTemplatesUpToTier" -> {
                    val maxTier = (args[0] as? Int) ?: 5
                    ItemCatalog.all.filter { it.slot.isEquippable() && it.tier <= maxTier }
                }
                "update", "upsert" -> {
                    val item = args[0] as ItemEntity
                    itemsMap[item.id] = item
                    null
                }
                else -> null
            }
        } as ItemDao

        val gearInstanceDaoProxy = Proxy.newProxyInstance(
            GearInstanceDao::class.java.classLoader,
            arrayOf(GearInstanceDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "get" -> gearInstances[args[0] as Long]
                "insert" -> {
                    val inst = args[0] as GearInstanceEntity
                    val id = if (inst.id == 0L) nextGearId++ else inst.id
                    gearInstances[id] = inst.copy(id = id)
                    id
                }
                "update" -> {
                    val inst = args[0] as GearInstanceEntity
                    gearInstances[inst.id] = inst
                    null
                }
                else -> null
            }
        } as GearInstanceDao

        override fun characterDao(): CharacterDao = characterDaoProxy
        override fun biomeProgressDao(): BiomeProgressDao = biomeProgressDaoProxy
        override fun itemDao(): ItemDao = itemDaoProxy
        override fun gearInstanceDao(): GearInstanceDao = gearInstanceDaoProxy
        override fun activeSessionDao(): ActiveSessionDao = createDummyDao()
        override fun sessionDao(): SessionDao = createDummyDao()
        override fun workoutDao(): WorkoutDao = createDummyDao()
        override fun classProgressDao(): ClassProgressDao = createDummyDao()
        override fun bodyMetricDao(): BodyMetricDao = createDummyDao()

        private val directExecutor = Executor { it.run() }
        override val transactionExecutor: Executor = directExecutor
        override val queryExecutor: Executor = directExecutor

        override fun createInvalidationTracker(): InvalidationTracker = createDummyDao()
        override fun clearAllTables() {}
        @Deprecated("Deprecated in RoomDatabase")
        override fun beginTransaction() {}
        @Deprecated("Deprecated in RoomDatabase")
        override fun setTransactionSuccessful() {}
        @Deprecated("Deprecated in RoomDatabase")
        override fun endTransaction() {}
        override fun runInTransaction(body: Runnable) { body.run() }
        override fun <V> runInTransaction(body: java.util.concurrent.Callable<V>): V = body.call()
    }

    @Before
    fun setUp() {
        db = FakeDatabase()
        repository = GameRepository(db)

        runBlocking {
            db.characterDao().upsert(
                CharacterEntity(
                    id = 1,
                    name = "Champion",
                    characterClass = CharacterClass.WARRIOR,
                    level = 5,
                    currentBiome = Biome.MEADOWLANDS.name,
                    energy = 20,
                    gold = 100,
                ),
            )
            db.biomeProgressDao().upsert(
                BiomeProgressEntity(
                    biomeName = Biome.MEADOWLANDS.name,
                    layer = 1,
                    bossUnlocked = true,
                    bossDefeated = false,
                    progressPoints = 100,
                    firstClearRewardClaimed = false,
                ),
            )
        }
    }

    @Test
    fun `applyVictory - first boss clear grants milestone rewards and marks progress atomically`() = runBlocking {
        val boss = MonsterCatalog.bossForBiome(Biome.MEADOWLANDS)
        val batch = repository.applyVictory(boss)

        assertEquals(RewardSource.BOSS, batch.source)
        
        // Check BiomeProgressEntity state
        val progress = db.biomeProgressDao().get(Biome.MEADOWLANDS.name)
        assertNotNull(progress)
        assertTrue("bossDefeated should be true", progress!!.bossDefeated)
        assertTrue("bossUnlocked should be true", progress.bossUnlocked)
        assertTrue("firstClearRewardClaimed should be true", progress.firstClearRewardClaimed)

        // Check first-clear rewards inclusion
        assertTrue("Must include BiomeUnlocked reward", batch.rewards.any { it is Reward.BiomeUnlocked })
        val unlockReward = batch.rewards.first { it is Reward.BiomeUnlocked } as Reward.BiomeUnlocked
        assertEquals(Biome.DARKWOOD.name, unlockReward.biomeName)

        // Progression rules should now permit entry into Darkwood
        val allProgress = listOf(progress)
        assertTrue("Darkwood should now be accessible", ProgressionRules.canEnterBiome(Biome.DARKWOOD, allProgress))
        val requirement = ProgressionRules.nextBiomeRequirement(Biome.MEADOWLANDS, allProgress)
        assertTrue("Next biome should be Unlocked", requirement is ProgressionRules.BiomeRequirement.Unlocked)
    }

    @Test
    fun `applyVictory - second boss clear does not re-grant first clear rewards`() = runBlocking {
        val boss = MonsterCatalog.bossForBiome(Biome.MEADOWLANDS)
        
        // First victory
        val firstBatch = repository.applyVictory(boss)
        assertTrue(firstBatch.rewards.any { it is Reward.BiomeUnlocked })

        // Second victory (farming boss)
        val secondBatch = repository.applyVictory(boss)
        assertEquals(RewardSource.BOSS, secondBatch.source)
        assertFalse("Second victory should NOT include BiomeUnlocked reward", secondBatch.rewards.any { it is Reward.BiomeUnlocked })

        val progress = db.biomeProgressDao().get(Biome.MEADOWLANDS.name)
        assertNotNull(progress)
        assertTrue(progress!!.bossDefeated)
        assertTrue(progress.firstClearRewardClaimed)
    }

    @Test
    fun `applyVictory - non-boss monster defeat increments progress without marking bossDefeated`() = runBlocking {
        val roamingMonster = MonsterCatalog.byId(1)!! // Couch Slime
        assertFalse(roamingMonster.isBoss)

        // Reset progress points
        db.biomeProgressDao().upsert(
            BiomeProgressEntity(
                biomeName = Biome.MEADOWLANDS.name,
                progressPoints = 10,
                bossUnlocked = false,
                bossDefeated = false,
                firstClearRewardClaimed = false,
            ),
        )

        val batch = repository.applyVictory(roamingMonster)
        assertEquals(RewardSource.BATTLE, batch.source)
        assertFalse(batch.rewards.any { it is Reward.BiomeUnlocked })

        val progress = db.biomeProgressDao().get(Biome.MEADOWLANDS.name)
        assertNotNull(progress)
        assertFalse("bossDefeated must remain false", progress!!.bossDefeated)
        assertFalse("firstClearRewardClaimed must remain false", progress.firstClearRewardClaimed)
        assertTrue("progressPoints must increase", progress.progressPoints > 10)
    }
}
