package com.fitnessquest.rpg.data.db

import com.fitnessquest.rpg.domain.*
import org.json.JSONArray
import org.json.JSONObject

object SessionReceiptCodec {
    fun serialize(res: SessionResult): String {
        val obj = JSONObject()
        obj.put("xp", res.xp)
        obj.put("gold", res.gold)
        obj.put("energy", res.energy)
        obj.put("levelsGained", res.levelsGained)

        obj.put("statGains", JSONObject().apply {
            put("strength", res.statGains.strength)
            put("endurance", res.statGains.endurance)
            put("agility", res.statGains.agility)
            put("willpower", res.statGains.willpower)
        })

        obj.put("volumeKg", res.volumeKg)
        obj.put("durationMs", res.durationMs)

        val muscles = JSONArray()
        res.musclesWorked.forEach { muscles.put(it) }
        obj.put("musclesWorked", muscles)

        obj.put("weeklyWorkoutsDone", res.weeklyWorkoutsDone)
        obj.put("weeklyWorkoutsGoal", res.weeklyWorkoutsGoal)
        obj.put("travelKm", res.travelKm)
        obj.put("arrivedAt", res.arrivedAt ?: JSONObject.NULL)
        obj.put("streak", res.streak)
        obj.put("streakSaved", res.streakSaved)
        obj.put("xpBoostApplied", res.xpBoostApplied)

        val prArray = JSONArray()
        res.prs.forEach { prArray.put(serializePr(it)) }
        obj.put("prs", prArray)

        val lootArray = JSONArray()
        res.lootLabels.forEach { lootArray.put(it) }
        obj.put("lootLabels", lootArray)

        obj.put("rewardBatch", res.rewardBatch?.let { serializeRewardBatch(it) } ?: JSONObject.NULL)

        // Extra metrics
        obj.put("caloriesKcal", res.caloriesKcal ?: JSONObject.NULL)
        obj.put("avgHr", res.avgHr ?: JSONObject.NULL)
        obj.put("maxHr", res.maxHr ?: JSONObject.NULL)
        obj.put("steps", res.steps ?: JSONObject.NULL)
        obj.put("distanceMeters", res.distanceMeters ?: JSONObject.NULL)
        obj.put("activeDurationMs", res.activeDurationMs ?: JSONObject.NULL)

        return obj.toString()
    }

    private fun serializePr(pr: SessionPr): JSONObject {
        return JSONObject().apply {
            put("exerciseName", pr.exerciseName)
            put("kind", pr.kind.name)
            put("value", pr.value)
            put("reps", pr.reps)
            put("isNew", pr.isNew)
        }
    }

    private fun serializeRewardBatch(batch: RewardBatch): JSONObject {
        return JSONObject().apply {
            put("source", batch.source.name)
            put("timestamp", batch.timestamp)
            val rewards = JSONArray()
            batch.rewards.forEach { rewards.put(serializeReward(it)) }
            put("rewards", rewards)
        }
    }

    private fun serializeReward(reward: Reward): JSONObject {
        val obj = JSONObject()
        when (reward) {
            is Reward.Xp -> {
                obj.put("type", "XP")
                obj.put("amount", reward.amount)
            }
            is Reward.Gold -> {
                obj.put("type", "GOLD")
                obj.put("amount", reward.amount)
            }
            is Reward.Energy -> {
                obj.put("type", "ENERGY")
                obj.put("amount", reward.amount)
            }
            is Reward.Gear -> {
                obj.put("type", "GEAR")
                obj.put("item", serializeItem(reward.item))
                obj.put("rarity", reward.rarity.name)
                val traitsArr = JSONArray()
                reward.traits.forEach { traitsArr.put(it.id) }
                obj.put("traits", traitsArr)
            }
            is Reward.Stackable -> {
                obj.put("type", "STACKABLE")
                obj.put("item", serializeItem(reward.item))
                obj.put("quantity", reward.quantity)
            }
            is Reward.LevelUp -> {
                obj.put("type", "LEVEL_UP")
                obj.put("newLevel", reward.newLevel)
            }
            is Reward.NewPr -> {
                obj.put("type", "NEW_PR")
                obj.put("pr", serializePr(reward.pr))
            }
            is Reward.BiomeUnlocked -> {
                obj.put("type", "BIOME_UNLOCKED")
                obj.put("biomeName", reward.biomeName)
                obj.put("biomeLabel", reward.biomeLabel)
            }
            is Reward.TitleUnlocked -> {
                obj.put("type", "TITLE_UNLOCKED")
                obj.put("title", reward.title)
            }
            is Reward.SkillPoint -> {
                obj.put("type", "SKILL_POINT")
                obj.put("amount", reward.amount)
            }
            is Reward.XpBoost -> {
                obj.put("type", "XP_BOOST")
                obj.put("amount", reward.amount)
            }
        }
        return obj
    }

    private fun serializeItem(item: ItemEntity): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("emoji", item.emoji)
            put("slot", item.slot.name)
            put("tier", item.tier)
            put("price", item.price)
            put("atk", item.atk)
            put("def", item.def)
            put("hp", item.hp)
            put("description", item.description)
            put("owned", item.owned)
            put("classAffinity", item.classAffinity?.name ?: JSONObject.NULL)
            put("style", item.style)
            put("quantity", item.quantity)
        }
    }

    fun deserialize(json: String, character: CharacterEntity): SessionResult {
        val obj = JSONObject(json)
        
        val xp = obj.optInt("xp", 0)
        val gold = obj.optInt("gold", 0)
        val energy = obj.optInt("energy", 0)
        val levelsGained = obj.optInt("levelsGained", 0)
        
        val statGains = obj.optJSONObject("statGains")?.let {
            StatGains(
                strength = it.optInt("strength", 0),
                endurance = it.optInt("endurance", 0),
                agility = it.optInt("agility", 0),
                willpower = it.optInt("willpower", 0)
            )
        } ?: StatGains()

        val volumeKg = obj.optDouble("volumeKg", 0.0)
        val durationMs = obj.optLong("durationMs", 0)
        
        val musclesWorked = mutableSetOf<String>()
        obj.optJSONArray("musclesWorked")?.let { arr ->
            for (i in 0 until arr.length()) {
                musclesWorked.add(arr.getString(i))
            }
        }

        val weeklyWorkoutsDone = obj.optInt("weeklyWorkoutsDone", 1)
        val weeklyWorkoutsGoal = obj.optInt("weeklyWorkoutsGoal", 3)
        val travelKm = obj.optDouble("travelKm", 0.0)
        val arrivedAt = if (obj.isNull("arrivedAt")) null else obj.optString("arrivedAt").takeIf { it.isNotBlank() }
        val streak = obj.optInt("streak", character.streak)
        val streakSaved = obj.optBoolean("streakSaved", false)
        val xpBoostApplied = obj.optInt("xpBoostApplied", 0)

        val prs = mutableListOf<SessionPr>()
        obj.optJSONArray("prs")?.let { arr ->
            for (i in 0 until arr.length()) {
                prs.add(deserializePr(arr.getJSONObject(i)))
            }
        }

        val lootLabels = mutableListOf<String>()
        obj.optJSONArray("lootLabels")?.let { arr ->
            for (i in 0 until arr.length()) {
                lootLabels.add(arr.getString(i))
            }
        }

        val rewardBatchObject = obj.optJSONObject("rewardBatch")
        val rewardBatch = when {
            rewardBatchObject != null -> deserializeRewardBatch(rewardBatchObject)
            obj.has("rewardBatch") && obj.isNull("rewardBatch") -> null
            else -> legacyRewardBatch(xp, gold, energy, levelsGained, arrivedAt, character)
        }

        val caloriesKcal = if (obj.isNull("caloriesKcal")) null else obj.optInt("caloriesKcal")
        val avgHr = if (obj.isNull("avgHr")) null else obj.optInt("avgHr")
        val maxHr = if (obj.isNull("maxHr")) null else obj.optInt("maxHr")
        val steps = if (obj.isNull("steps")) null else obj.optLong("steps")
        val distanceMeters = if (obj.isNull("distanceMeters")) null else obj.optDouble("distanceMeters")
        val activeDurationMs = if (obj.isNull("activeDurationMs")) null else obj.optLong("activeDurationMs")

        return SessionResult(
            xp = xp,
            gold = gold,
            energy = energy,
            levelsGained = levelsGained,
            statGains = statGains,
            updatedCharacter = character,
            volumeKg = volumeKg,
            durationMs = durationMs,
            musclesWorked = musclesWorked,
            weeklyWorkoutsDone = weeklyWorkoutsDone,
            weeklyWorkoutsGoal = weeklyWorkoutsGoal,
            travelKm = travelKm,
            arrivedAt = arrivedAt,
            streak = streak,
            streakSaved = streakSaved,
            xpBoostApplied = xpBoostApplied,
            prs = prs,
            lootLabels = lootLabels,
            rewardBatch = rewardBatch,
            caloriesKcal = caloriesKcal,
            avgHr = avgHr,
            maxHr = maxHr,
            steps = steps,
            distanceMeters = distanceMeters,
            activeDurationMs = activeDurationMs
        )
    }

    private fun deserializePr(obj: JSONObject): SessionPr {
        return SessionPr(
            exerciseName = obj.getString("exerciseName"),
            kind = PrKind.valueOf(obj.getString("kind")),
            value = obj.getDouble("value"),
            reps = obj.optInt("reps", 0),
            isNew = obj.optBoolean("isNew", true)
        )
    }

    private fun legacyRewardBatch(
        xp: Int,
        gold: Int,
        energy: Int,
        levelsGained: Int,
        arrivedAt: String?,
        character: CharacterEntity
    ): RewardBatch {
        val rewards = buildList {
            if (xp > 0) add(Reward.Xp(xp))
            if (gold > 0) add(Reward.Gold(gold))
            if (energy > 0) add(Reward.Energy(energy))
            if (levelsGained > 0) add(Reward.LevelUp(character.level))
            if (arrivedAt != null) {
                add(Reward.BiomeUnlocked(character.currentBiome, arrivedAt))
            }
        }
        return RewardBatch(RewardSource.WORKOUT, rewards)
    }

    private fun deserializeRewardBatch(obj: JSONObject): RewardBatch {
        val source = RewardSource.valueOf(obj.getString("source"))
        val timestamp = obj.getLong("timestamp")
        val rewardsArr = obj.getJSONArray("rewards")
        val rewards = mutableListOf<Reward>()
        for (i in 0 until rewardsArr.length()) {
            rewards.add(deserializeReward(rewardsArr.getJSONObject(i)))
        }
        return RewardBatch(source, rewards, timestamp)
    }

    private fun deserializeReward(obj: JSONObject): Reward {
        return when (val type = obj.getString("type")) {
            "XP" -> Reward.Xp(obj.getInt("amount"))
            "GOLD" -> Reward.Gold(obj.getInt("amount"))
            "ENERGY" -> Reward.Energy(obj.getInt("amount"))
            "GEAR" -> {
                val item = deserializeItem(obj.getJSONObject("item"))
                val rarity = obj.optString("rarity").takeIf { it.isNotBlank() }?.let { GearRarity.fromName(it) } ?: GearRarity.COMMON
                val traits = mutableListOf<GearTrait>()
                obj.optJSONArray("traits")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        GearTrait.fromId(arr.getString(i))?.let { traits.add(it) }
                    }
                }
                Reward.Gear(item, rarity, traits)
            }
            "STACKABLE" -> Reward.Stackable(deserializeItem(obj.getJSONObject("item")), obj.getInt("quantity"))
            "LEVEL_UP" -> Reward.LevelUp(obj.getInt("newLevel"))
            "NEW_PR" -> Reward.NewPr(deserializePr(obj.getJSONObject("pr")))
            "BIOME_UNLOCKED" -> Reward.BiomeUnlocked(obj.getString("biomeName"), obj.getString("biomeLabel"))
            "TITLE_UNLOCKED" -> Reward.TitleUnlocked(obj.getString("title"))
            "SKILL_POINT" -> Reward.SkillPoint(obj.getInt("amount"))
            "XP_BOOST" -> Reward.XpBoost(obj.getInt("amount"))
            else -> throw IllegalArgumentException("Unknown reward type: $type")
        }
    }

    private fun deserializeItem(obj: JSONObject): ItemEntity {
        return ItemEntity(
            id = obj.getLong("id"),
            name = obj.getString("name"),
            emoji = obj.getString("emoji"),
            slot = ItemSlot.valueOf(obj.getString("slot")),
            tier = obj.getInt("tier"),
            price = obj.getInt("price"),
            atk = obj.optInt("atk", 0),
            def = obj.optInt("def", 0),
            hp = obj.optInt("hp", 0),
            description = obj.optString("description", ""),
            owned = obj.optBoolean("owned", false),
            classAffinity = if (obj.isNull("classAffinity")) null else CharacterClass.valueOf(obj.getString("classAffinity")),
            style = obj.optString("style", ""),
            quantity = obj.optInt("quantity", 0)
        )
    }
}
