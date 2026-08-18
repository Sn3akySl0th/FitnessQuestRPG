package com.fitnessquest.shared.wear

import org.json.JSONArray
import org.json.JSONObject

/** Capability names for phone ↔ watch discovery. */
object WearCapabilities {
    const val PHONE = "fitnessrpg_phone"
    const val WEAR = "fitnessrpg_wear"
}

/** Message / Data Layer path constants. */
object WearPaths {
    const val SESSION_STATE = "/fitnessrpg/session_state"
    const val LOG_SET = "/fitnessrpg/log_set"
    const val REST = "/fitnessrpg/rest"
    const val HR_SAMPLE = "/fitnessrpg/hr_sample"
    const val METRICS = "/fitnessrpg/metrics"
    const val FEEDBACK = "/fitnessrpg/feedback"
    const val SESSION_ENDED = "/fitnessrpg/session_ended"
    const val OPEN_APP = "/fitnessrpg/open_app"
    const val HELLO = "/fitnessrpg/hello"
    const val HELLO_ACK = "/fitnessrpg/hello_ack"
    const val AVATAR_WATCH_FACE = "/fitnessrpg/avatar_watch_face"
    const val START_WORKOUT = "/fitnessrpg/start_workout"
    const val ROUTINES_LIST = "/fitnessrpg/routines_list"
}

enum class WearRestAction { SKIP, EXTEND, SET_DURATION }

enum class HrZone(val label: String, val minPct: Double, val maxPct: Double) {
    WARMUP("Warmup", 0.0, 0.50),
    EASY("Easy", 0.50, 0.70),
    WORK("Work", 0.70, 0.85),
    HIGH("High", 0.85, 1.01);

    companion object {
        fun fromBpm(bpm: Int, maxHr: Int): HrZone {
            val pct = bpm.toDouble() / maxHr.coerceAtLeast(1)
            return entries.firstOrNull { pct >= it.minPct && pct < it.maxPct } ?: HIGH
        }

        fun estimatedMaxHr(age: Int = 30): Int = (220 - age).coerceIn(140, 200)
    }
}

enum class WearFeedbackKind {
    SET_LOGGED,
    REST_END,
    ZONE_HIGH,
    ZONE_RECOVER,
    HEAT,
    AMBUSH,
    PR,
    RECOVERY_READY,
    GOAL_MET,
    CALORIES
}

data class WearExerciseState(
    val name: String,
    val category: String,
    val targetSets: Int,
    val targetReps: Int,
    val loggedSets: Int,
    val trackingType: String = "",
    val lastWeightDisplay: Double = 0.0,
    val lastReps: Int = 0,
    val suggestedWeightDisplay: Double? = null,
    val suggestedReps: Int? = null,
    val suggestionReason: String? = null
)

data class WearSessionState(
    val active: Boolean,
    val title: String,
    val imperial: Boolean,
    val heatStreak: Int,
    val totalSets: Int,
    val totalXp: Int,
    val restEndsAt: Long?,
    val restDurationSec: Int,
    val currentIndex: Int,
    val exercises: List<WearExerciseState>,
    val heartRateBpm: Int? = null,
    val hrZone: String? = null,
    val watchLinked: Boolean = false,
    val bodyWeightKg: Double? = null,
    val ageYears: Int? = null,
    val maxHr: Int? = null
) {
    fun toJson(): ByteArray = JSONObject().apply {
        put("active", active)
        put("title", title)
        put("imperial", imperial)
        put("heatStreak", heatStreak)
        put("totalSets", totalSets)
        put("totalXp", totalXp)
        put("restEndsAt", restEndsAt ?: JSONObject.NULL)
        put("restDurationSec", restDurationSec)
        put("currentIndex", currentIndex)
        put("heartRateBpm", heartRateBpm ?: JSONObject.NULL)
        put("hrZone", hrZone ?: JSONObject.NULL)
        put("watchLinked", watchLinked)
        put("bodyWeightKg", bodyWeightKg ?: JSONObject.NULL)
        put("ageYears", ageYears ?: JSONObject.NULL)
        put("maxHr", maxHr ?: JSONObject.NULL)
        put("exercises", JSONArray().also { arr ->
            exercises.forEach { ex ->
                arr.put(
                    JSONObject().apply {
                        put("name", ex.name)
                        put("category", ex.category)
                        put("targetSets", ex.targetSets)
                        put("targetReps", ex.targetReps)
                        put("loggedSets", ex.loggedSets)
                        put("trackingType", ex.trackingType)
                        put("lastWeightDisplay", ex.lastWeightDisplay)
                        put("lastReps", ex.lastReps)
                    }
                )
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJson(bytes: ByteArray): WearSessionState {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            val arr = o.optJSONArray("exercises") ?: JSONArray()
            val exercises = buildList {
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    add(
                        WearExerciseState(
                            name = e.getString("name"),
                            category = e.getString("category"),
                            targetSets = e.getInt("targetSets"),
                            targetReps = e.getInt("targetReps"),
                            loggedSets = e.getInt("loggedSets"),
                            trackingType = e.optString("trackingType"),
                            lastWeightDisplay = e.optDouble("lastWeightDisplay", 0.0),
                            lastReps = e.optInt("lastReps", 0)
                        )
                    )
                }
            }
            return WearSessionState(
                active = o.optBoolean("active", false),
                title = o.optString("title", "Session"),
                imperial = o.optBoolean("imperial", false),
                heatStreak = o.optInt("heatStreak", 0),
                totalSets = o.optInt("totalSets", 0),
                totalXp = o.optInt("totalXp", 0),
                restEndsAt = if (o.isNull("restEndsAt")) null else o.optLong("restEndsAt"),
                restDurationSec = o.optInt("restDurationSec", 90),
                currentIndex = o.optInt("currentIndex", 0),
                exercises = exercises,
                heartRateBpm = if (o.isNull("heartRateBpm")) null else o.optInt("heartRateBpm"),
                hrZone = if (o.isNull("hrZone")) null else o.optString("hrZone"),
                watchLinked = o.optBoolean("watchLinked", false),
                bodyWeightKg = if (o.isNull("bodyWeightKg")) null else o.optDouble("bodyWeightKg"),
                ageYears = if (o.isNull("ageYears")) null else o.optInt("ageYears"),
                maxHr = if (o.isNull("maxHr")) null else o.optInt("maxHr")
            )
        }
    }
}

data class WearLogSetCommand(
    val exerciseIndex: Int,
    val weightDisplay: Double,
    val reps: Int,
    val durationMin: Double = 0.0,
    val distanceDisplay: Double = 0.0,
    val rir: Int? = null,
    val avgHr: Int? = null,
    val maxHr: Int? = null
) {
    fun toJson(): ByteArray = JSONObject().apply {
        put("exerciseIndex", exerciseIndex)
        put("weightDisplay", weightDisplay)
        put("reps", reps)
        put("durationMin", durationMin)
        put("distanceDisplay", distanceDisplay)
        put("rir", rir ?: JSONObject.NULL)
        put("avgHr", avgHr ?: JSONObject.NULL)
        put("maxHr", maxHr ?: JSONObject.NULL)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJson(bytes: ByteArray): WearLogSetCommand {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            return WearLogSetCommand(
                exerciseIndex = o.getInt("exerciseIndex"),
                weightDisplay = o.getDouble("weightDisplay"),
                reps = o.getInt("reps"),
                durationMin = o.optDouble("durationMin", 0.0),
                distanceDisplay = o.optDouble("distanceDisplay", 0.0),
                rir = if (o.isNull("rir")) null else o.optInt("rir"),
                avgHr = if (o.isNull("avgHr")) null else o.optInt("avgHr"),
                maxHr = if (o.isNull("maxHr")) null else o.optInt("maxHr")
            )
        }
    }
}

data class WearRestCommand(
    val action: WearRestAction,
    val seconds: Int = 30
) {
    fun toJson(): ByteArray = JSONObject().apply {
        put("action", action.name)
        put("seconds", seconds)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJson(bytes: ByteArray): WearRestCommand {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            return WearRestCommand(
                action = WearRestAction.valueOf(o.getString("action")),
                seconds = o.optInt("seconds", 30)
            )
        }
    }
}

/** Live biometric + activity metrics streamed from the watch during a quest. */
data class WearLiveMetrics(
    val bpm: Int? = null,
    val zone: String? = null,
    val caloriesKcal: Double? = null,
    val steps: Long? = null,
    val stepsPerMin: Long? = null,
    val distanceMeters: Double? = null,
    val speedMps: Double? = null,
    val paceSecPerKm: Double? = null,
    val elevationMeters: Double? = null,
    val floors: Double? = null,
    /** Pause-aware work time (excludes rest windows tracked on watch). */
    val activeDurationMs: Long? = null,
    val sessionDurationMs: Long? = null,
    val zoneWarmupSec: Int = 0,
    val zoneEasySec: Int = 0,
    val zoneWorkSec: Int = 0,
    val zoneHighSec: Int = 0,
    val restHrStart: Int? = null,
    val restHrCurrent: Int? = null,
    val restHrDrop: Int? = null,
    val restHrGoalBpm: Int? = null,
    val restHrGoalMet: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val goalLabel: String? = null,
    val atMillis: Long = System.currentTimeMillis()
) {
    fun toJson(): ByteArray = JSONObject().apply {
        putOpt("bpm", bpm)
        putOpt("zone", zone)
        putOpt("caloriesKcal", caloriesKcal)
        putOpt("steps", steps)
        putOpt("stepsPerMin", stepsPerMin)
        putOpt("distanceMeters", distanceMeters)
        putOpt("speedMps", speedMps)
        putOpt("paceSecPerKm", paceSecPerKm)
        putOpt("elevationMeters", elevationMeters)
        putOpt("floors", floors)
        putOpt("activeDurationMs", activeDurationMs)
        putOpt("sessionDurationMs", sessionDurationMs)
        put("zoneWarmupSec", zoneWarmupSec)
        put("zoneEasySec", zoneEasySec)
        put("zoneWorkSec", zoneWorkSec)
        put("zoneHighSec", zoneHighSec)
        putOpt("restHrStart", restHrStart)
        putOpt("restHrCurrent", restHrCurrent)
        putOpt("restHrDrop", restHrDrop)
        putOpt("restHrGoalBpm", restHrGoalBpm)
        put("restHrGoalMet", restHrGoalMet)
        putOpt("latitude", latitude)
        putOpt("longitude", longitude)
        putOpt("goalLabel", goalLabel)
        put("atMillis", atMillis)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJson(bytes: ByteArray): WearLiveMetrics {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            return WearLiveMetrics(
                bpm = o.optNullableInt("bpm"),
                zone = o.optNullableString("zone"),
                caloriesKcal = o.optNullableDouble("caloriesKcal"),
                steps = o.optNullableLong("steps"),
                stepsPerMin = o.optNullableLong("stepsPerMin"),
                distanceMeters = o.optNullableDouble("distanceMeters"),
                speedMps = o.optNullableDouble("speedMps"),
                paceSecPerKm = o.optNullableDouble("paceSecPerKm"),
                elevationMeters = o.optNullableDouble("elevationMeters"),
                floors = o.optNullableDouble("floors"),
                activeDurationMs = o.optNullableLong("activeDurationMs"),
                sessionDurationMs = o.optNullableLong("sessionDurationMs"),
                zoneWarmupSec = o.optInt("zoneWarmupSec", 0),
                zoneEasySec = o.optInt("zoneEasySec", 0),
                zoneWorkSec = o.optInt("zoneWorkSec", 0),
                zoneHighSec = o.optInt("zoneHighSec", 0),
                restHrStart = o.optNullableInt("restHrStart"),
                restHrCurrent = o.optNullableInt("restHrCurrent"),
                restHrDrop = o.optNullableInt("restHrDrop"),
                restHrGoalBpm = o.optNullableInt("restHrGoalBpm"),
                restHrGoalMet = o.optBoolean("restHrGoalMet", false),
                latitude = o.optNullableDouble("latitude"),
                longitude = o.optNullableDouble("longitude"),
                goalLabel = o.optNullableString("goalLabel"),
                atMillis = o.optLong("atMillis", System.currentTimeMillis())
            )
        }

        private fun JSONObject.putOpt(key: String, value: Any?) {
            if (value == null) put(key, JSONObject.NULL) else put(key, value)
        }

        private fun JSONObject.optNullableInt(key: String): Int? =
            if (isNull(key) || !has(key)) null else optInt(key)

        private fun JSONObject.optNullableLong(key: String): Long? =
            if (isNull(key) || !has(key)) null else optLong(key)

        private fun JSONObject.optNullableDouble(key: String): Double? =
            if (isNull(key) || !has(key)) null else optDouble(key)

        private fun JSONObject.optNullableString(key: String): String? =
            if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() }
    }
}

@Deprecated("Use WearLiveMetrics")
data class WearHrSample(val bpm: Int, val atMillis: Long = System.currentTimeMillis()) {
    fun toJson(): ByteArray = WearLiveMetrics(bpm = bpm, atMillis = atMillis).toJson()

    companion object {
        fun fromJson(bytes: ByteArray): WearHrSample {
            val m = WearLiveMetrics.fromJson(bytes)
            return WearHrSample(m.bpm ?: 0, m.atMillis)
        }
    }
}

data class WearFeedbackEvent(
    val kind: WearFeedbackKind,
    val message: String
) {
    fun toJson(): ByteArray = JSONObject().apply {
        put("kind", kind.name)
        put("message", message)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJson(bytes: ByteArray): WearFeedbackEvent {
            val o = JSONObject(String(bytes, Charsets.UTF_8))
            return WearFeedbackEvent(
                kind = WearFeedbackKind.valueOf(o.getString("kind")),
                message = o.optString("message", "")
            )
        }
    }
}

data class WearRoutineSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val isRecommended: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("exerciseCount", exerciseCount)
        put("isRecommended", isRecommended)
    }

    companion object {
        fun fromJson(json: JSONObject): WearRoutineSummary = WearRoutineSummary(
            id = json.optLong("id", -1L),
            name = json.optString("name", "Quest"),
            exerciseCount = json.optInt("exerciseCount", 0),
            isRecommended = json.optBoolean("isRecommended", false)
        )

        fun listToJson(list: List<WearRoutineSummary>): ByteArray {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return JSONObject().put("routines", arr).toString().toByteArray(Charsets.UTF_8)
        }

        fun listFromJson(bytes: ByteArray): List<WearRoutineSummary> = runCatching {
            val obj = JSONObject(String(bytes, Charsets.UTF_8))
            val arr = obj.optJSONArray("routines") ?: JSONArray()
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}
