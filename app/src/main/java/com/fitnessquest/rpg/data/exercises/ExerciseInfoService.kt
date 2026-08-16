package com.fitnessquest.rpg.data.exercises

import android.content.Context
import com.fitnessquest.rpg.domain.EquipmentCatalog
import com.fitnessquest.rpg.domain.EquipmentFamily
import com.fitnessquest.rpg.domain.EquipmentStation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

/** How-to details for an exercise, sourced from the bundled public-domain database. */
data class ExerciseInfo(
    val name: String,
    val level: String,
    val equipment: String,
    /** free-exercise-db `category` (cardio, strength, stretching, …). */
    val dbCategory: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val imageUrls: List<String>,
    val trackingType: String = "",
)

/**
 * Looks up exercise guides from the bundled free-exercise-db (public domain,
 * ~870 exercises) and demonstration photos under `assets/exercises/`.
 * Lookup is tolerant: exact name, curated alias, then fuzzy token matching so
 * AI-generated names like "Dumbbell Overhead Press" still resolve to a guide.
 * Images are loaded only from the APK — no runtime network fetch.
 */
class ExerciseInfoService(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val mutex = Mutex()
    private var entries: List<ExerciseInfo>? = null
    private var exactIndex: Map<String, ExerciseInfo> = emptyMap()
    private var cloudLoadedAt: Long = 0L
    private var cloudExercises: List<ExerciseInfo> = emptyList()
    private var cloudStations: List<EquipmentStation> = emptyList()

    suspend fun find(name: String): ExerciseInfo? = withContext(Dispatchers.IO) {
        val key = normalize(name)
        onlineExercises().firstOrNull { normalize(it.name) == key }?.let { return@withContext it }
        custom[key]?.let { return@withContext it }
        val list = load()
        exactIndex[key]?.let { return@withContext it }
        aliases[key]?.let { alias -> exactIndex[normalize(alias)]?.let { return@withContext it } }
        fuzzyMatch(key, list)
    }

    /** All DB + custom guides (for equipment browsing). */
    suspend fun allGuides(): List<ExerciseInfo> = withContext(Dispatchers.IO) {
        val db = load()
        val customList = custom.values.toList()
        (onlineExercises() + customList + db).distinctBy { normalize(it.name) }
    }

    suspend fun forStation(station: EquipmentStation): List<ExerciseInfo> = withContext(Dispatchers.IO) {
        allGuides()
            .asSequence()
            .filter { EquipmentCatalog.matches(station, it.name, it.equipment) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    suspend fun allStations(family: EquipmentFamily? = null): List<EquipmentStation> = withContext(Dispatchers.IO) {
        val merged = (onlineStations() + EquipmentCatalog.all).distinctBy { it.id }
        merged
            .asSequence()
            .filter { (family == null) || (it.family == family) }
            .sortedWith(compareBy<EquipmentStation> { it.family.ordinal }.thenBy { it.label.lowercase() })
            .toList()
    }

    suspend fun stationsFor(exerciseName: String, equipmentField: String): List<EquipmentStation> =
        withContext(Dispatchers.IO) {
            val hits = allStations().filter { EquipmentCatalog.matches(it, exerciseName, equipmentField) }
            val specific = hits.filter { it.nameContains.isNotEmpty() }
            (specific.ifEmpty { hits }).distinctBy { it.id }
        }

    suspend fun submitExerciseSuggestion(
        name: String,
        category: String,
        equipment: String,
        primaryMuscles: String,
        instructions: String,
        trackingType: String = ""
    ): Result<Unit> = submitSuggestion(
        type = "exercise",
        payload = mapOf(
            "name" to formatTitle(name),
            "dbCategory" to normalizeToken(category).ifBlank { "strength" },
            "equipment" to normalizeToken(equipment),
            "primaryMuscles" to splitList(primaryMuscles),
            "secondaryMuscles" to emptyList<String>(),
            "instructions" to splitInstructions(instructions),
            "trackingType" to trackingType,
            "level" to "beginner"
        )
    )

    suspend fun submitMachineSuggestion(
        label: String,
        family: EquipmentFamily,
        subtitle: String,
        matches: String
    ): Result<Unit> = submitSuggestion(
        type = "machine",
        payload = mapOf(
            "id" to machineId(label),
            "label" to formatTitle(label),
            "family" to family.name,
            "subtitle" to formatSentence(subtitle),
            "dbTags" to listOf("machine"),
            "nameContains" to splitList(matches).ifEmpty { listOf(label.lowercase()) },
            "nameExcludes" to emptyList<String>()
        )
    )

    private suspend fun submitSuggestion(type: String, payload: Map<String, Any?>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user = auth.currentUser ?: error("Sign in before submitting catalog suggestions.")
                val now = System.currentTimeMillis()
                firestore.collection("userSubmissions").add(
                    mapOf(
                        "type" to type,
                        "status" to "pending",
                        "createdByUid" to user.uid,
                        "createdByName" to user.displayName.orEmpty(),
                        "createdAt" to now,
                        "updatedAt" to now,
                        "schema" to 1,
                        "payload" to payload
                    )
                ).await()
                Unit
            }
        }

    private suspend fun onlineExercises(): List<ExerciseInfo> {
        refreshCloudCatalogIfNeeded()
        return cloudExercises
    }

    private suspend fun onlineStations(): List<EquipmentStation> {
        refreshCloudCatalogIfNeeded()
        return cloudStations
    }

    private suspend fun refreshCloudCatalogIfNeeded() = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now - cloudLoadedAt < CLOUD_TTL_MS) return@withLock
        cloudLoadedAt = now
        runCatching {
            val machineDocs = firestore.collection("catalogMachines")
                .whereEqualTo("status", "approved")
                .get()
                .await()
            cloudStations = machineDocs.documents.mapNotNull { doc ->
                val id = (doc.getString("id") ?: doc.id).sanitizeId()
                val family = doc.getString("family")?.let {
                    runCatching { EquipmentFamily.valueOf(it.uppercase(Locale.US)) }.getOrNull()
                } ?: EquipmentFamily.MACHINES
                EquipmentStation(
                    id = id,
                    label = doc.getString("label")?.takeIf { it.isNotBlank() }?.let(::formatTitle)
                        ?: id.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    family = family,
                    dbTags = doc.getStringList("dbTags").ifEmpty { listOf("machine") }.map(::normalizeToken).toSet(),
                    nameContains = doc.getStringList("nameContains").map(::normalizeToken),
                    nameExcludes = doc.getStringList("nameExcludes").map(::normalizeToken),
                    subtitle = doc.getString("subtitle")?.let(::formatSentence).orEmpty(),
                    remoteImageUrl = doc.getString("imageUrl")?.takeIf { it.startsWith("http", ignoreCase = true) }
                )
            }

            val exerciseDocs = firestore.collection("catalogExercises")
                .whereEqualTo("status", "approved")
                .get()
                .await()
            cloudExercises = exerciseDocs.documents.mapNotNull { doc ->
                val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ExerciseInfo(
                    name = formatTitle(name),
                    level = normalizeToken(doc.getString("level") ?: "beginner").ifBlank { "beginner" },
                    equipment = normalizeToken(doc.getString("equipment") ?: "machine"),
                    dbCategory = normalizeToken(doc.getString("dbCategory") ?: doc.getString("category") ?: "strength"),
                    primaryMuscles = doc.getStringList("primaryMuscles").map(::normalizeToken),
                    secondaryMuscles = doc.getStringList("secondaryMuscles").map(::normalizeToken),
                    instructions = doc.getStringList("instructions").map(::formatSentence).filter { it.isNotBlank() },
                    imageUrls = doc.getStringList("imageUrls").filter { it.startsWith("http", ignoreCase = true) },
                    trackingType = doc.getString("trackingType").orEmpty()
                )
            }
        }
    }

    private suspend fun load(): List<ExerciseInfo> = mutex.withLock {
        entries ?: parse().also { list ->
            entries = list
            exactIndex = list.associateBy { normalize(it.name) }
        }
    }

    private fun parse(): List<ExerciseInfo> {
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    ExerciseInfo(
                        name = o.getString("name"),
                        level = o.optString("level"),
                        equipment = o.optString("equipment").takeIf { it != "null" }.orEmpty(),
                        dbCategory = o.optString("category").takeIf { it != "null" }.orEmpty(),
                        primaryMuscles = o.optJSONArray("primaryMuscles").toStringList(),
                        secondaryMuscles = o.optJSONArray("secondaryMuscles").toStringList(),
                        instructions = o.optJSONArray("instructions").toStringList(),
                        // Bundled under assets/exercises/ — no runtime network dependency.
                        imageUrls = o.optJSONArray("images").toStringList().map { ASSET_IMAGE_PREFIX + it },
                        trackingType = o.optString("trackingType").takeIf { it != "null" }.orEmpty()
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) { for (i in 0 until length()) add(getString(i)) }
    }

    private fun fuzzyMatch(key: String, list: List<ExerciseInfo>): ExerciseInfo? {
        val queryTokens = key.split(' ').filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) return null
        var best: ExerciseInfo? = null
        var bestScore = 0.0
        for (entry in list) {
            val entryTokens = normalize(entry.name).split(' ').filter { it.isNotBlank() }
            val matched = queryTokens.count { q -> entryTokens.any { tokensSimilar(q, it) } }
            // Dice coefficient over token sets.
            val score = 2.0 * matched / (queryTokens.size + entryTokens.size)
            if (score > bestScore || (score == bestScore && best != null && entry.name.length < best.name.length)) {
                bestScore = score
                best = entry
            }
        }
        return if (bestScore >= 0.5) best else null
    }

    /** Equal, or one is a prefix of the other ("walk"/"walking", "row"/"rowing"). */
    private fun tokensSimilar(a: String, b: String): Boolean {
        if (a == b) return true
        val (short, long) = if (a.length <= b.length) a to b else b to a
        return short.length >= 3 && long.startsWith(short)
    }

    private fun normalize(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(' ')
            .joinToString(" ") { it.removeSuffix("s").ifEmpty { it } }

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>).orEmpty().mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }

    private fun splitList(value: String): List<String> =
        value.split(',', '\n', ';')
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }
            .distinct()

    private fun splitInstructions(value: String): List<String> =
        value.split('\n')
            .flatMap { line -> line.split(Regex("""(?<=\.)\s+""")) }
            .map { it.trim().trimStart('-', '*', ' ', '\t') }
            .map(::formatSentence)
            .filter { it.length >= 8 }
            .take(8)

    private fun machineId(label: String): String =
        label.sanitizeId().let { if (it.endsWith("_machine")) it else "${it}_machine" }

    private fun String.sanitizeId(): String =
        lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "custom_machine" }

    private fun normalizeToken(value: String): String =
        value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 /+_-]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun formatTitle(value: String): String =
        normalizeToken(value)
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                when (token) {
                    "and", "or", "of", "the", "to", "with" -> token
                    else -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }
            }

    private fun formatSentence(value: String): String {
        val clean = value.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return ""
        val capped = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        return if (capped.last() in ".!?") capped else "$capped."
    }

    private companion object {
        const val CLOUD_TTL_MS = 5 * 60 * 1000L

        /** Coil loads these from the APK: `app/src/main/assets/exercises/…`. */
        const val ASSET_IMAGE_PREFIX = "file:///android_asset/exercises/"

        /** Curated matches for catalog exercises (keys are normalized names). */
        val aliases = mapOf(
            "bench pres" to "Barbell Bench Press - Medium Grip",
            "squat" to "Barbell Squat",
            "deadlift" to "Barbell Deadlift",
            "overhead pres" to "Standing Military Press",
            "barbell row" to "Bent Over Barbell Row",
            "dumbbell curl" to "Dumbbell Bicep Curl",
            "lat pulldown" to "Wide-Grip Lat Pulldown",
            "tricep pushdown" to "Triceps Pushdown",
            "running" to "Running, Treadmill",
            "cycling" to "Bicycling",
            "rowing machine" to "Rowing, Stationary",
            "jump rope" to "Rope Jumping",
            "stair climber" to "Stairmaster",
            "elliptical" to "Elliptical Trainer",
            "treadmill" to "Running, Treadmill",
            "walking brisk" to "Walking, Treadmill",
            "push up" to "Pushups",
            "pull up" to "Pullups",
            "dip" to "Dips - Triceps Version",
            "bodyweight squat" to "Bodyweight Squat",
            "lunge" to "Bodyweight Walking Lunge",
            "plank rep second" to "Plank",
            "sit up" to "Sit-Up",
            "mountain climber" to "Mountain Climbers",
            "calf press machine" to "Calf Press",
            "seated calf press" to "Calf Press",
            "machine calf press" to "Calf Press",
            "hip adduction" to "Thigh Adductor",
            "hip adduction machine" to "Thigh Adductor",
            "seated hip adduction" to "Thigh Adductor",
            "adductor machine" to "Thigh Adductor",
            "machine adduction" to "Thigh Adductor",
            "vertical crunch" to "Vertical Crunch Machine",
            "vertical crunch machine" to "Vertical Crunch Machine",
            "upright crunch" to "Vertical Crunch Machine",
            "upright ab crunch" to "Vertical Crunch Machine",
            "standing crunch machine" to "Vertical Crunch Machine",
            "captain chair crunch" to "Captain Chair Vertical Crunch",
            "swivel crunch" to "Twisting Vertical Crunch",
            "seated ab crunch" to "Ab Crunch Machine",
            "machine crunch" to "Ab Crunch Machine",
            "ab crunch" to "Ab Crunch Machine"
        )

        /** Exercises the database doesn't cover; hand-written guides, no photos. */
        val custom: Map<String, ExerciseInfo> = listOf(
            ExerciseInfo(
                name = "Burpees",
                level = "intermediate",
                equipment = "body only",
                dbCategory = "plyometrics",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = listOf("chest", "quadriceps", "shoulders"),
                instructions = listOf(
                    "Stand with your feet shoulder-width apart.",
                    "Drop into a squat and place your hands on the floor in front of you.",
                    "Kick your feet back into a plank position, keeping your core tight.",
                    "Optionally perform a push-up.",
                    "Jump your feet back toward your hands.",
                    "Explode upward into a jump, reaching your arms overhead. Land softly and repeat."
                ),
                imageUrls = listOf(ASSET_IMAGE_PREFIX + "burpees.png")
            ),
            ExerciseInfo(
                name = "Swimming",
                level = "beginner",
                equipment = "pool",
                dbCategory = "cardio",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = listOf("lats", "shoulders", "core"),
                instructions = listOf(
                    "Choose a stroke you're comfortable with (freestyle is a great default).",
                    "Keep your body long and level at the surface; exhale steadily underwater.",
                    "Pull with your whole forearm and keep a relaxed, steady kick.",
                    "Swim in intervals: for example 4-8 lengths at a steady effort, short rest, repeat."
                ),
                imageUrls = listOf(ASSET_IMAGE_PREFIX + "swimming.png")
            ),
            ExerciseInfo(
                name = "Yoga Flow",
                level = "beginner",
                equipment = "mat",
                dbCategory = "stretching",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = listOf("core", "hamstrings"),
                instructions = listOf(
                    "Move through a sequence of poses (e.g. sun salutations) at the pace of your breath.",
                    "Inhale on expanding movements, exhale on folding movements.",
                    "Hold each pose 3-5 breaths, keeping your core engaged.",
                    "Finish with a few minutes of stillness or gentle stretching."
                ),
                imageUrls = listOf(ASSET_IMAGE_PREFIX + "yoga.png")
            ),
            ExerciseInfo(
                name = "Static Stretching",
                level = "beginner",
                equipment = "none",
                dbCategory = "stretching",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Pick a muscle group and ease into the stretch until you feel gentle tension, never pain.",
                    "Hold for 20-45 seconds while breathing slowly. Don't bounce.",
                    "Relax deeper into the stretch with each exhale.",
                    "Work through your major muscle groups, especially ones you trained today."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Foam Rolling",
                level = "beginner",
                equipment = "foam roll",
                dbCategory = "stretching",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Place the foam roller under the target muscle and support your weight with your hands and feet.",
                    "Roll slowly (about an inch per second) along the muscle, not over joints.",
                    "When you find a tender spot, pause on it for 20-30 seconds and breathe.",
                    "Spend 1-2 minutes per muscle group."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Mobility Drills",
                level = "beginner",
                equipment = "none",
                dbCategory = "stretching",
                primaryMuscles = listOf("full body"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Work through controlled, full-range movements: arm circles, hip circles, leg swings, cat-cow, deep squat holds.",
                    "Move slowly and deliberately - the goal is control at the end of your range.",
                    "Do 8-12 reps of each drill, focusing on stiff areas.",
                    "Great as a warm-up or an active recovery day."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Pilates",
                level = "beginner",
                equipment = "mat",
                dbCategory = "stretching",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = listOf("lower back", "glutes"),
                instructions = listOf(
                    "Focus on slow, precise movements powered by your core.",
                    "Classic mat sequence: the hundred, roll-ups, leg circles, single-leg stretch, plank variations.",
                    "Keep your lower back gently pressed toward the mat and breathe laterally into your ribs.",
                    "Quality over quantity: 5-10 controlled reps per movement."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Single-Leg Calf Press",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("calves"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Sit on the calf press machine and set the seat so your working knee is only slightly bent.",
                    "Place one foot on the platform with the ball of the foot loaded; keep the other foot clear.",
                    "Hold the handles, unlock the stack, and point your toes up for a full ankle stretch.",
                    "Press through the ball of your foot to full plantar flexion, pause briefly, then lower with control.",
                    "Finish the set, then switch legs."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Seated Hip Adduction Machine",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("adductors"),
                secondaryMuscles = listOf("glutes", "hamstrings"),
                instructions = listOf(
                    "Sit tall with your back against the pad and place your inner thighs against the pads.",
                    "Set the starting width to a comfortable stretch; do not force your hips wider than you can control.",
                    "Grip the handles, brace lightly, and squeeze both pads inward until your thighs come together.",
                    "Pause briefly at the center, then let the pads open slowly under control.",
                    "Keep your hips planted and avoid bouncing at the stretched position."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Single-Leg Hip Adduction Machine",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("adductors"),
                secondaryMuscles = listOf("glutes"),
                instructions = listOf(
                    "Set up on the hip adduction machine as usual, then use one leg to move the pad inward while the other leg stays relaxed.",
                    "Choose a lighter load than your two-leg set so the working hip stays controlled.",
                    "Squeeze the working thigh inward, pause at the center, and return slowly to the open position.",
                    "Keep your pelvis square and your back against the pad.",
                    "Complete all reps on one side, then switch legs."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Pause-Rep Hip Adduction Machine",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("adductors"),
                secondaryMuscles = listOf("glutes", "hamstrings"),
                instructions = listOf(
                    "Sit on the hip adduction machine with a load you can control without momentum.",
                    "Bring the pads inward and hold the fully squeezed position for 2-3 seconds.",
                    "Open the pads halfway, pause for one second, then return to the stretch under control.",
                    "Keep your torso still and avoid using your hands to pull yourself into the rep.",
                    "Use this variation when you want more inner-thigh tension with less weight."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Slow-Tempo Hip Adduction Machine",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("adductors"),
                secondaryMuscles = listOf("glutes"),
                instructions = listOf(
                    "Set the hip adduction machine to a comfortable range and select a moderate load.",
                    "Squeeze the pads inward over three seconds, keeping your knees aligned with the pads.",
                    "Hold for one second at the center.",
                    "Open back to the start over three seconds without letting the stack drop.",
                    "Stop the set when you can no longer keep the tempo smooth."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Toes-In Calf Press",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("calves"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Set up on the calf press machine with both feet on the platform.",
                    "Angle your toes slightly inward (pigeon-toe) while keeping heels free to move.",
                    "Unlock the weight and start from a deep dorsiflexed stretch.",
                    "Press through the balls of your feet to full extension, emphasizing the outer calf.",
                    "Lower slowly and repeat without bouncing at the bottom."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Toes-Out Calf Press",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("calves"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Sit on the calf press machine and place both feet on the platform.",
                    "Turn your toes slightly outward while keeping the knees stable and slightly bent.",
                    "Begin from a full stretch with toes pulled toward you.",
                    "Press down through the balls of your feet to full ankle extension, emphasizing the inner calf.",
                    "Control the return and keep the movement in the ankles only."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Vertical Crunch Machine",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = listOf("obliques"),
                instructions = listOf(
                    "Stand or sit upright on the vertical crunch machine and secure your feet or hips per the station.",
                    "Take the handles with a light grip and brace your core before unlocking the load.",
                    "Curl your ribs toward your hips through the guided arc — use abs, not arm pull.",
                    "Pause briefly at peak contraction, then return with control through a full stretch.",
                    "Keep the path smooth; this upright pattern trains abs with more full-core involvement than a seated crunch."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Oblique Vertical Crunch",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("obliques"),
                secondaryMuscles = listOf("abdominals"),
                instructions = listOf(
                    "Set up on the vertical crunch machine in the upright position with a secure stance.",
                    "Bias the movement toward one side — many vertical units allow a slight swivel or diagonal crunch.",
                    "Crunch diagonally, bringing rib toward opposite hip while keeping hips steady.",
                    "Pause, lower under control, finish the set, then switch sides.",
                    "Use a lighter load than straight vertical crunches so the rotation stays clean."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Twisting Vertical Crunch",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("obliques"),
                secondaryMuscles = listOf("abdominals"),
                instructions = listOf(
                    "Load the vertical crunch machine lightly and set yourself in the upright starting position.",
                    "If the unit has a swivel seat or rotating pad, unlock it so you can turn through the crunch.",
                    "Crunch while rotating toward one side, leading with the shoulder — not yanking the handles.",
                    "Return to center under control, then rotate to the other side on the next rep (or alternate sets).",
                    "Stop if you feel strain in the low back; shorten the range and slow the tempo."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Slow-Tempo Vertical Crunch",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = listOf("obliques"),
                instructions = listOf(
                    "Set up on the vertical crunch machine and choose a load you can control for a full 3-second lift and lower.",
                    "Crunch up for three seconds through the upright guided path, squeeze for one second at the top.",
                    "Lower for three seconds without letting the plates slam.",
                    "Keep breathing steady — exhale on the way up, inhale on the way down.",
                    "End the set when tempo breaks; don't cheat with momentum."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Captain Chair Vertical Crunch",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = listOf("hip flexors", "obliques"),
                instructions = listOf(
                    "Set up on the vertical crunch station with your back supported and forearms or hands secure on the pads or handles.",
                    "Brace your core and keep your shoulders down away from your ears.",
                    "Curl your ribs toward your hips while lifting the knees or driving the crunch arm through its path.",
                    "Pause briefly when the abs are fully shortened.",
                    "Lower slowly until you return to the starting stretch without swinging."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Weighted Vertical Crunch Machine",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = listOf("obliques"),
                instructions = listOf(
                    "Select a load that lets you complete the vertical crunch without pulling through your arms.",
                    "Stand or sit tall in the machine and secure your feet, hips, or forearms based on the station design.",
                    "Exhale as you crunch downward or forward, curling the spine instead of folding only at the hips.",
                    "Hold the squeeze for one second.",
                    "Return under control and stop the set when the movement turns into momentum."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Seated Machine Crunch",
                level = "beginner",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Sit on the standard ab crunch machine, feet under the pads, and rest your triceps or hands on the handles.",
                    "Start upright with a braced core and light resistance.",
                    "Crunch straight forward, curling the torso toward the thighs without twisting.",
                    "Pause at the top, then return slowly to the start — this is pure ab isolation.",
                    "Keep the hips planted; don't pull with your arms or yank the stack."
                ),
                imageUrls = emptyList()
            ),
            ExerciseInfo(
                name = "Pause-Rep Ab Crunch Machine",
                level = "intermediate",
                equipment = "machine",
                dbCategory = "strength",
                primaryMuscles = listOf("abdominals"),
                secondaryMuscles = emptyList(),
                instructions = listOf(
                    "Set up on the seated ab crunch machine with feet locked and a moderate load.",
                    "Crunch forward until the abs are fully shortened, then hold for 2–3 seconds.",
                    "Lower halfway, pause again for one second, then return to the full stretch.",
                    "Repeat with the same pause pattern — no bounce at the bottom.",
                    "Great for a consistent straight-crunch pattern without rotation."
                ),
                imageUrls = emptyList()
            )
        ).associateBy {
            it.name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
                .split(' ').joinToString(" ") { t -> t.removeSuffix("s") }
        }
    }
}
