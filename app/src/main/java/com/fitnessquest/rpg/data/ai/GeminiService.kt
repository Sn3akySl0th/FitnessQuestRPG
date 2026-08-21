package com.fitnessquest.rpg.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.TrainingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class AiExercise(
    val name: String,
    val category: ExerciseCategory,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val suggestionReason: String? = null
)

data class AiWorkout(
    val name: String, 
    val exercises: List<AiExercise>,
    val description: String? = null
)


/** One replacement exercise suggested by the AI, with a short justification. */
data class AiSwap(
    val name: String,
    val category: ExerciseCategory,
    val sets: Int,
    val reps: Int,
    val reason: String
)

/** A single adjustment the coach proposes for a not-yet-started exercise. */
data class CoachChange(
    val exercise: String,
    val sets: Int?,
    val reps: Int?,
    val weightKg: Double?,
    val replaceWith: String?,
    val replaceCategory: ExerciseCategory?,
    val reason: String
)

data class CoachAdvice(val message: String, val changes: List<CoachChange>)

/**
 * Thin client for the Gemini REST API. The key comes from the in-app setting if present,
 * otherwise from local.properties via BuildConfig.
 */
class GeminiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("fitquest_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** The user-provided key from preferences, if any. */
    val userApiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() } ?: ""

    var apiKey: String
        get() = userApiKey.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        set(value) {
            prefs.edit().putString(KEY_API, value.trim()).apply()
        }

    val hasKey: Boolean get() = apiKey.isNotBlank()
    val isAvailable: Boolean get() = hasKey || isLocalModelReady()

    /** Explicit check if we should even try the cloud API. */
    fun canUseCloud(): Boolean = hasKey

    fun isLocalModelReady(): Boolean = getReadyModelFile() != null

    private fun getReadyModelFile(): File? {
        (context.applicationContext as? FitQuestApp)?.container?.playAssetModel?.getModelFile()?.let { return it }

        val downloader = LocalModelDownloader(context)


        // Prefer the FitQuest-hosted model when configured, then fall back to larger curated models.
        ModelCatalog.BUILTIN_MODELS
            .sortedWith(compareByDescending<com.fitnessquest.rpg.data.ai.LocalModelSpec> { it.recommended }.thenByDescending { it.approxSizeMb })
            .forEach { spec ->
            if (downloader.isModelReady(spec)) return downloader.getModelFile(spec.id)
        }
        // Check custom model
        val customFile = downloader.getModelFile("custom_model")
        if (customFile.exists() && customFile.length() > 100 * 1024 * 1024) return customFile
        return null
    }

    suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val modelFile = getReadyModelFile()
        val useLocal = prefs.getBoolean("use_local_ai_by_default", false)

        if (useLocal && modelFile != null) {
            Log.d("GeminiService", "Trying local AI...")
            LocalAiEngine.generateTextLocal(context, prompt, modelFile)?.let {
                Log.d("GeminiService", "Local AI success.")
                return@withContext Result.success(it)
            }
            Log.d("GeminiService", "Local AI returned null.")
        }

        Log.d("GeminiService", "Trying cloud AI...")
        val cloudResult = generateContent(listOf(textPart(prompt)))
        
        if (cloudResult.isFailure && modelFile != null) {
            Log.w("GeminiService", "Cloud AI failed, retrying local...", cloudResult.exceptionOrNull())
            LocalAiEngine.generateTextLocal(context, prompt, modelFile)?.let {
                return@withContext Result.success(it)
            }
        }
        
        cloudResult
    }

    private suspend fun generateContent(parts: List<JSONObject>): Result<String> =

        withContext(Dispatchers.IO) {
            val key = apiKey
            if (key.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No Gemini API key configured."))
            }
            var lastError: Throwable? = null
            repeat(3) { attempt ->
                try {
                    return@withContext Result.success(call(parts, key, fastThinking = true))
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    // Older models reject thinkingLevel; retry once without it.
                    if (msg.contains("INVALID_ARGUMENT")) {
                        return@withContext runCatching { call(parts, key, fastThinking = false) }
                    }
                    lastError = e
                    val transient = msg.contains("UNAVAILABLE") ||
                        msg.contains("RESOURCE_EXHAUSTED") ||
                        msg.contains("high demand", ignoreCase = true) ||
                        msg.contains("overloaded", ignoreCase = true)
                    if (!transient) return@withContext Result.failure(e)
                    delay((1500L * (attempt + 1)).milliseconds)
                }
            }
            Result.failure(lastError ?: IllegalStateException("Gemini request failed"))
        }

    /**
     * One generateContent round-trip. [fastThinking] caps the model's reasoning
     * at "minimal" so in-workout requests answer in seconds instead of minutes.
     */
    private fun call(parts: List<JSONObject>, key: String, fastThinking: Boolean): String {
        val partsArray = JSONArray()
        parts.forEach { partsArray.put(it) }
        val payload = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", partsArray))
            )
        // Disable experimental thinkingConfig for now as it may cause timeouts on Flash models
        /*
        if (fastThinking) {
            payload.put(
                "generationConfig",
                JSONObject().put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal"))
            )
        }
        */
        val body = payload.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            .addHeader("x-goog-api-key", key)
            .post(body)
            .build()

        return client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val status = runCatching {
                    JSONObject(text).getJSONObject("error").getString("status")
                }.getOrDefault("")
                val message = runCatching {
                    JSONObject(text).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP ${response.code}")

                val cleanMessage = when (status) {
                    "RESOURCE_EXHAUSTED" -> "Magic reserves depleted (Quota exceeded). Try again in a minute or switch to Local AI."
                    "UNAVAILABLE" -> "The AI is currently overwhelmed by high demand. Please try again soon."
                    "PERMISSION_DENIED" -> "Invalid API key or permission denied. Please check your settings."
                    else -> "The Coach is busy or encountered an error ($status)."
                }
                Log.e("GeminiService", "Raw error from Gemini: $status - $message")
                error(cleanMessage)
            }
            val json = JSONObject(text)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val reason = json.optJSONObject("promptFeedback")?.optString("blockReason")
                error("Gemini returned no answer${reason?.let { " (blocked: $it)" }.orEmpty()}")
            }
            // Thinking models may emit thought parts; concatenate only answer text.
            val responseParts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            val answer = buildString {
                for (i in 0 until responseParts.length()) {
                    val part = responseParts.getJSONObject(i)
                    if (part.optBoolean("thought")) continue
                    append(part.optString("text"))
                }
            }
            if (answer.isBlank()) error("Gemini returned an empty response")
            answer
        }
    }

    private fun textPart(text: String): JSONObject = JSONObject().put("text", text)

    private fun imagePart(bytes: ByteArray, mimeType: String): JSONObject =
        JSONObject().put(
            "inline_data",
            JSONObject()
                .put("mime_type", mimeType)
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
        )

    /** Shrink photos before upload so multimodal requests stay under size limits. */
    fun prepareImageForUpload(rawBytes: ByteArray, mimeHint: String? = null): Pair<ByteArray, String> {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return rawBytes to (mimeHint?.takeIf { it.startsWith("image/") } ?: "image/jpeg")
        val maxEdge = 1280
        val scale = maxOf(decoded.width, decoded.height).toFloat().let { edge ->
            if (edge <= maxEdge) 1f else maxEdge / edge
        }
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return out.toByteArray() to "image/jpeg"
    }

    private fun workoutJsonSchemaBlock(multi: Boolean = false): String {
        val root = if (multi) {
            """{"workouts": [${workoutObjectSchema()}]}"""
        } else {
            workoutObjectSchema()
        }
        return """
            Respond with ONLY valid JSON, no markdown fences, matching exactly this schema:
            $root

            Rules:
            - category STRENGTH is for weighted exercises; CARDIO for treadmill walk/jog/run, cycling, rowing, elliptical, stairs, etc (reps = minutes);
              BODYWEIGHT for calisthenics; FLEXIBILITY for stretching/yoga (reps = minutes).
            - "sets" 1-6, "reps" 1-60.
            - "weightKg" is optional; suggest it ONLY if historical data is provided for that exercise or a similar one.
            - "suggestionReason" is optional; if you suggest a weight, explain why (e.g. "Based on your 3x10 @ 60kg last week").
        """.trimIndent()
    }

    private fun workoutObjectSchema(): String = """
        {"name": "quest name (short, fantasy)", "description": "short blurb", "exercises": [{"name": "exercise name", "category": "STRENGTH|CARDIO|BODYWEIGHT|FLEXIBILITY", "sets": 3, "reps": 10, "weightKg": 60.0, "suggestionReason": "reason"}]}
    """.trimIndent()

    private fun parseWorkoutJson(raw: String, defaultName: String): AiWorkout =
        parseMultiWorkoutJson(raw, defaultName).first()

    private fun parseMultiWorkoutJson(raw: String, defaultName: String): List<AiWorkout> {
        val json = stripFences(raw)
        val workouts = mutableListOf<AiWorkout>()
        
        try {
            val root = JSONObject(json)
            val workoutsArray = root.optJSONArray("workouts")
            if (workoutsArray != null) {
                for (i in 0 until workoutsArray.length()) {
                    workouts.add(parseSingleWorkout(workoutsArray.getJSONObject(i), defaultName))
                }
            } else {
                workouts.add(parseSingleWorkout(root, defaultName))
            }
        } catch (e: Exception) {
            // Fallback for non-nested root if it's an array
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    workouts.add(parseSingleWorkout(arr.getJSONObject(i), defaultName))
                }
            } catch (_: Exception) {
                throw e
            }
        }
        return workouts
    }

    private fun parseSingleWorkout(obj: JSONObject, defaultName: String): AiWorkout {
        val exercises = mutableListOf<AiExercise>()
        val arr = obj.getJSONArray("exercises")
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            exercises += AiExercise(
                name = e.getString("name"),
                category = ExerciseCategories.coerce(e.optString("category"), e.getString("name")),
                sets = e.optInt("sets", 3).coerceIn(1, 6),
                reps = e.optInt("reps", 10).coerceIn(1, 60),
                weightKg = e.optDouble("weightKg").takeIf { !it.isNaN() },
                suggestionReason = e.optString("suggestionReason").takeIf { it.isNotBlank() }
            )
        }
        return AiWorkout(
            name = obj.optString("name", defaultName).ifBlank { defaultName },
            exercises = exercises,
            description = obj.optString("description").takeIf { it.isNotBlank() }
        )
    }


    private fun sanitizeRoutineName(raw: String, fallback: String): String {
        val cleaned = stripFences(raw)
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.trim('"', '\'', '`', '.', ':', '-')
            ?.replace(Regex("""\s+"""), " ")
            .orEmpty()
        return cleaned
            .takeIf { it.length in 3..48 && !it.contains("{") && !it.contains("[") }
            ?: fallback
    }

    suspend fun generateWorkout(
        request: String,
        profile: TrainingProfile? = null,
        history: String? = null
    ): Result<List<AiWorkout>> {
        val profileBlock = profile?.let {
            """

            The player's training profile (respect it strictly):
            ${it.promptSummary()}
            Only include exercises that are possible with the available equipment,
            sized appropriately for the player's fitness level, and consistent with their split.
            """
        }.orEmpty()

        val historyBlock = history?.let {
            """

            Player's recent workout history for context:
            $it
            Use this to avoid repetitive workouts and ensure progress.
            """
        }.orEmpty()

        val prompt = """
            You are a fitness coach inside a fantasy RPG fitness app. Create a workout based on this request:
            "$request"
            $profileBlock
            $historyBlock
            ${workoutJsonSchemaBlock(multi = true)}
            - 4 to 8 exercises, realistic and safe for a general gym-goer.
            - If the request implies multiple workouts (e.g. "a PPL split"), return them in the "workouts" array.
        """.trimIndent()

        val cloudResult = generateText(prompt).mapCatching { parseMultiWorkoutJson(it, "AI Training Quest") }
        if (cloudResult.isSuccess) return cloudResult

        // Hybrid Local AI Fallback when offline or rate limited
        return runCatching {
            val modelFile = getReadyModelFile()
            if (modelFile != null) {
                LocalAiEngine.generateWorkoutLocal(context, prompt, modelFile)?.let { local ->
                    return@runCatching listOf(AiWorkout(
                        name = local.title,
                        exercises = local.exercises.map {
                            AiExercise(it.exerciseName, it.category, it.targetSets, it.targetReps)
                        }
                    ))
                }
            }
            val localHeuristic = LocalAiEngine.generateWorkout(request, null)
            val aiExercises = localHeuristic.exercises.map {
                AiExercise(name = it.exerciseName, category = it.category, sets = it.targetSets, reps = it.targetReps)
            }
            listOf(AiWorkout(name = localHeuristic.title, exercises = aiExercises))
        }
    }





    suspend fun renameImportedRoutine(originalTitle: String, exerciseNames: List<String>): Result<String> {
        val fallback = LocalAiEngine.fantasyRoutineName(originalTitle, exerciseNames)
        val prompt = """
            You name reusable workout routines for FitQuest, a fantasy RPG fitness app.
            Create one immersive training quest name for this imported routine.

            Original routine name: "$originalTitle"
            Exercises: ${exerciseNames.joinToString(", ")}

            Rules:
            - Return only the name, no JSON, no quotes, no explanation.
            - 2 to 5 words.
            - Fantasy RPG flavor, but still understandable as a workout.
            - Avoid dates, emojis, profanity, and copyrighted game names.
            - Do not mention Hevy or import.
        """.trimIndent()

        val cloud = generateText(prompt).mapCatching { raw ->
            sanitizeRoutineName(raw, fallback)
        }
        if (cloud.isSuccess) return cloud
        
        // Try local LLM for renaming
        val modelFile = getReadyModelFile()
        if (modelFile != null) {
            LocalAiEngine.generateTextLocal(context, prompt, modelFile)?.let {
                return Result.success(sanitizeRoutineName(it, fallback))
            }
        }
        
        return Result.success(fallback)
    }


    /** Import workouts pasted from another fitness app / notes. */
    suspend fun importWorkoutFromText(
        raw: String, 
        profile: TrainingProfile? = null,
        history: String? = null
    ): Result<List<AiWorkout>> {
        val profileBlock = profile?.let {
            "\nPlayer profile (prefer exercises they can do):\n${it.promptSummary()}\n"
        }.orEmpty()
        val historyBlock = history?.let {
            "\nPlayer recent history (use to suggest weights):\n$it\n"
        }.orEmpty()
        val prompt = """
            You are importing workouts into FitnessRPG from another app or notes.
            Extract EVERY distinct workout routine from this text. If multiple days or routines are listed, split them into separate workouts.
            Extract exercise names, sets, and reps. 
            $profileBlock
            $historyBlock
            Pasted text:
            ---
            $raw
            ---
            ${workoutJsonSchemaBlock(multi = true)}
            - Preserve exercise names when clear; invent short fantasy-flavored quest names.
            - If sets/reps/minutes are missing, use sensible defaults.
            - If you find multiple routines, return them as a list in the "workouts" array.
            - ONLY include exercises explicitly listed in the text. DO NOT invent extra exercises.
        """.trimIndent()

        return generateText(prompt).mapCatching { parseMultiWorkoutJson(it, "Imported Quest") }
    }

    /** Import workouts from a screenshot / photo of another app's workout screen. */
    suspend fun importWorkoutFromImage(
        imageBytes: ByteArray,
        mimeType: String,
        optionalHint: String? = null,
        profile: TrainingProfile? = null,
        history: String? = null
    ): Result<List<AiWorkout>> {
        val profileBlock = profile?.let {
            "\nPlayer profile (prefer exercises they can do):\n${it.promptSummary()}\n"
        }.orEmpty()
        val historyBlock = history?.let {
            "\nPlayer recent history (use to suggest weights):\n$it\n"
        }.orEmpty()
        val hintBlock = optionalHint?.takeIf { it.isNotBlank() }?.let {
            "\nUser hint: $it\n"
        }.orEmpty()
        val prompt = """
            You are importing workouts into FitnessRPG. The attached image is a screenshot or photo
            of one or more workout routines. 
            Identify EVERY distinct workout routine visible. If the image shows a weekly plan or multiple sessions, split them into separate workouts in the "workouts" array.
            $profileBlock
            $historyBlock
            $hintBlock
            ${workoutJsonSchemaBlock(multi = true)}
            - Extract exercise names, sets, and reps.
            - Suggest starting weights ONLY where history provides context. 
            - Invent short fantasy names for each routine.
            - ONLY include exercises explicitly visible in the image. DO NOT invent or assume extra exercises.
        """.trimIndent()

        val (bytes, mime) = prepareImageForUpload(imageBytes, mimeType)
        return generateContent(listOf(imagePart(bytes, mime), textPart(prompt)))
            .mapCatching { parseMultiWorkoutJson(it, "Imported Quest") }
    }


    /**
     * Mid-session coaching: looks at what was logged so far and proposes tweaks
     * (sets/reps changes or swaps) for exercises that haven't been started yet.
     */
    suspend fun coachSession(
        performedSummary: String,
        remainingPlan: String,
        profile: TrainingProfile?,
        history: String? = null,
        imperial: Boolean = false
    ): Result<CoachAdvice> {
        val profileBlock = profile?.let { "\nPlayer profile:\n${it.promptSummary()}\n" }.orEmpty()
        val historyBlock = history?.let { "\nPlayer history:\n$it\n" }.orEmpty()
        val unitLabel = if (imperial) "lbs" else "kg"

        val prompt = """
            You are a personal trainer coaching a live workout in a fantasy RPG fitness app.
            The player prefers weights in $unitLabel.

            Sets logged so far this session:
            $performedSummary

            Remaining planned exercises (not started yet):
            $remainingPlan
            $profileBlock
            $historyBlock
            Analyze the performance (e.g. reps far above/below target suggest adjusting difficulty,
            fatigue late in a session suggests reducing volume) and adjust the REMAINING
            exercises. If the player is consistently hitting or exceeding targets, you MUST PUSH
            them harder (more weight, sets, or reps). Be a demanding but fair Guildmaster.
            Do not be afraid to substitute exercises for more challenging versions if they are maxing out.
            If you decide NO changes are needed, your "message" must explicitly praise their 
            perfect form and explain why the current plan is still optimal.
            Some sets may include "@N RIR" (reps in reserve): 0 = taken to failure, 4+ = far too easy.
            Use it to judge intensity: consistently high RIR means the player can push harder,
            RIR 0 on early sets means fatigue will build fast.

            Respond with ONLY valid JSON, no markdown fences, exactly this schema:
            {"message": "1-2 sentence coaching note, encouraging, fantasy guildmaster tone",
             "changes": [{"exercise": "exact name from the remaining list", "sets": 3, "reps": 8, "weightKg": 60.5,
                          "replaceWith": null, "category": null, "reason": "short why"}]}

            Rules:
            - "exercise" MUST exactly match a name from the remaining list.
            - Use "replaceWith" (plus "category": "STRENGTH|CARDIO|BODYWEIGHT|FLEXIBILITY") only when
              substituting a different exercise; otherwise keep both null and change sets/reps/weight.
            - "sets" 1-6, "reps" 1-60. "weightKg" should be suggested if the player is finding the exercise too easy.
            - "weightKg" MUST be in Kilograms, even if the player prefers $unitLabel. The app handles the conversion.
            - At most 3 changes.
        """.trimIndent()


        return generateText(prompt).mapCatching { raw ->
            Log.d("GeminiService", "Raw AI response: $raw")
            val stripped = stripFences(raw)
            val obj = try {
                JSONObject(stripped)
            } catch (e: Exception) {
                // If the model output a plain text message instead of JSON, we can still show it.
                val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(raw)
                val msg = msgMatch?.groupValues?.get(1) ?: raw.take(200)
                return@mapCatching CoachAdvice(msg, emptyList())
            }
            val changes = mutableListOf<CoachChange>()
            val arr = obj.optJSONArray("changes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val exerciseName = c.optString("exercise").takeIf { it.isNotBlank() } ?: continue

                val catStr = c.optString("category").takeIf { it.isNotBlank() && it != "null" }
                val category = catStr?.let { cat ->
                    runCatching { ExerciseCategory.valueOf(cat.uppercase()) }
                        .getOrElse { runCatching { ExerciseCategory.valueOf(cat) }.getOrNull() }
                        ?: ExerciseCategories.coerce(cat, exerciseName)
                }

                changes += CoachChange(
                    exercise = exerciseName,
                    sets = c.optInt("sets", -1).takeIf { it in 1..6 },
                    reps = c.optInt("reps", -1).takeIf { it in 1..60 },
                    weightKg = c.optDouble("weightKg").takeIf { !it.isNaN() && it > 0 },
                    replaceWith = c.optString("replaceWith").takeIf { it.isNotBlank() && it != "null" },
                    replaceCategory = category,
                    reason = c.optString("reason", "Suggested tweak")
                )
            }
            CoachAdvice(obj.optString("message", "Keep going, hero!"), changes)
        }
    }

    /** Picks one replacement for an exercise the player cannot or does not want to do. */
    suspend fun suggestSwap(
        exerciseName: String,
        category: ExerciseCategory,
        targetSets: Int,
        targetReps: Int,
        avoid: List<String>,
        profile: TrainingProfile?
    ): Result<AiSwap> {
        val profileBlock = profile?.let { "\nPlayer profile (respect the equipment list strictly):\n${it.promptSummary()}\n" }.orEmpty()
        val prompt = """
            You are a personal trainer in a fantasy RPG fitness app. The player wants to replace
            the exercise "$exerciseName" (${category.name}, planned $targetSets x $targetReps),
            for example because the equipment is unavailable.
            $profileBlock
            Suggest ONE replacement that works the same muscle groups, is possible with the player's
            available equipment, and is NOT one of: ${avoid.joinToString()}.

            Respond with ONLY valid JSON, no markdown fences, exactly this schema:
            {"name": "exercise name", "category": "STRENGTH|CARDIO|BODYWEIGHT|FLEXIBILITY",
             "sets": $targetSets, "reps": $targetReps, "reason": "short why this is a good swap"}
        """.trimIndent()

        return generateText(prompt).mapCatching { raw ->
            val obj = JSONObject(stripFences(raw))
            AiSwap(
                name = obj.getString("name"),
                category = ExerciseCategories.coerce(
                    obj.optString("category").ifBlank { category.name },
                    obj.getString("name")
                ),
                sets = obj.optInt("sets", targetSets).coerceIn(1, 6),
                reps = obj.optInt("reps", targetReps).coerceIn(1, 60),
                reason = obj.optString("reason")
            )
        }
    }

    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            trimmed.removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
        }
    }

    suspend fun battleNarration(monsterName: String, victory: Boolean, playerName: String, level: Int): Result<String> {
        val outcome = if (victory) "defeated" else "was defeated by"
        val prompt = "Write a vivid, dramatic 2-3 sentence fantasy battle epilogue (under 50 words, single short paragraph): the hero \"$playerName\" (level $level) " +
                "$outcome the monster \"$monsterName\" in an RPG where real-world workouts power the hero. " +
                if (victory) "Make it triumphant with a wink of fitness humor."
                else "Make it encouraging: the hero should train harder in the real world and return. " +
                    "Do not write multiple paragraphs. Respond with only the narration text."
        return generateText(prompt)
    }

    suspend fun sessionPraise(summary: String, playerName: String): Result<String> {
        val prompt = "You are a wise RPG guildmaster praising the hero \"$playerName\" who just finished a real workout: $summary. " +
                "Write 1-2 sentences of fantasy-flavored praise. Respond with only the text."
        return generateText(prompt)
    }

    suspend fun lootStory(
        playerName: String,
        classLabel: String,
        level: Int,
        exerciseName: String,
        weightKg: Double,
        reps: Int,
        isPr: Boolean,
        lootLabels: List<String>
    ): Result<String> {
        val lift = if (weightKg > 0) "${"%.1f".format(weightKg)} kg x $reps" else "$reps reps"
        val moment = if (isPr) "a PERSONAL RECORD" else "a mighty lift"
        val loot = lootLabels.joinToString(", ").ifEmpty { "a mysterious blessing" }
        val prompt = "You are the chronicler of FitnessRPG, an RPG powered by real workouts. " +
                "Hero \"$playerName\" (level $level $classLabel) just hit $moment on $exerciseName ($lift) " +
                "and received: $loot. Write 2-3 vivid fantasy sentences tying the lift to the reward. " +
                "No bullet points. Respond with only the story."
        return generateText(prompt)
    }


    companion object {
        // Rolling alias: always points at the newest Flash model, so it never sunsets.
        private const val MODEL = "gemini-flash-latest"
        private const val KEY_API = "gemini_api_key"
    }
}
