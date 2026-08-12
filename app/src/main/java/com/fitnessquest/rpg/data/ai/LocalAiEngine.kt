package com.fitnessquest.rpg.data.ai

import android.content.Context
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.ItemCatalog
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.random.Random


data class LocalAiWorkout(
    val title: String,
    val exercises: List<WorkoutExerciseEntity>
)

/**
 * On-Device Local AI Engine:
 * Generates structured workouts, exercise selections, and RPG narrative logs locally.
 * Operates 100% offline using MediaPipe GenAI inference when a model is downloaded.
 */
object LocalAiEngine {
    private var llmInference: LlmInference? = null
    private var loadedModelPath: String? = null
    private val mutex = Mutex()

    private fun ensureLoaded(context: Context, modelFile: File?) {
        val path = modelFile?.absolutePath ?: return
        if (!modelFile.exists()) return
        if (llmInference != null && loadedModelPath == path) return
        
        try {
            llmInference?.close()
            // MediaPipe GenAI 0.10.x builder
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(1024)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            loadedModelPath = path
        } catch (e: Exception) {
            e.printStackTrace()
            llmInference = null
        }
    }

    suspend fun generateTextLocal(
        context: Context,
        prompt: String,
        modelFile: File?
    ): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureLoaded(context, modelFile)
            runCatching {
                llmInference?.generateResponse(prompt)
            }.getOrNull()
        }
    }




    private val STRENGTH_EXERCISES = listOf(
        "Bench Press (Barbell)", "Incline Dumbbell Press", "Overhead Shoulder Press",
        "Barbell Squat", "Romanian Deadlift", "Barbell Bent-Over Row",
        "Lat Pulldown", "Dumbbell Bicep Curl", "Tricep Rope Pushdown"
    )

    private val CARDIO_EXERCISES = listOf(
        "Treadmill Running", "Stationary Bike Sprint", "Rowing Machine Intervals",
        "Elliptical Glide", "Stair Climber Power Step", "Jump Rope Burst"
    )

    private val BODYWEIGHT_EXERCISES = listOf(
        "Push-ups", "Pull-ups", "Bodyweight Squats", "Plank Hold",
        "Dips", "Burpees", "Mountain Climbers"
    )

    private val FLEXIBILITY_EXERCISES = listOf(
        "Hamstring Stretch", "Cobra Stretch", "Child's Pose",
        "Hip Flexor Stretch", "Cat-Cow Stretch"
    )

    fun fantasyRoutineName(originalTitle: String, exerciseNames: List<String>): String {
        val text = (originalTitle + " " + exerciseNames.joinToString(" ")).lowercase()
        val focus = when {
            listOf("squat", "deadlift", "leg press", "lunge", "hamstring", "quad", "glute").any { it in text } -> "Ironroot"
            listOf("bench", "press", "chest", "push", "shoulder", "tricep").any { it in text } -> "Sunforge"
            listOf("row", "pulldown", "pull", "lat", "curl", "back", "bicep").any { it in text } -> "Shadowgrip"
            listOf("run", "treadmill", "bike", "rower", "elliptical", "cardio").any { it in text } -> "Windrunner"
            listOf("core", "crunch", "plank", "abs").any { it in text } -> "Runecore"
            listOf("full", "total", "circuit").any { it in text } -> "Adventurer"
            else -> "Training"
        }
        val suffix = when {
            "push" in text -> "Push Trial"
            "pull" in text -> "Pull Trial"
            "leg" in text || "lower" in text -> "Leg Trial"
            "upper" in text -> "Upper Trial"
            "full" in text || "total" in text -> "Full-Body Trial"
            else -> "Quest"
        }
        return "$focus $suffix"
    }

    suspend fun generateWorkoutLocal(
        context: Context,
        prompt: String,
        modelFile: File
    ): LocalAiWorkout? = withContext(Dispatchers.IO) {
        val rawJson = generateTextLocal(context, prompt, modelFile) ?: return@withContext null
        runCatching {
            // Basic extraction of JSON block if the model included markdown or chat text
            val jsonStart = rawJson.indexOf("{")
            val jsonEnd = rawJson.lastIndexOf("}") + 1
            if (jsonStart == -1 || jsonEnd <= jsonStart) return@withContext null
            val cleaned = rawJson.substring(jsonStart, jsonEnd)
            
            val obj = JSONObject(cleaned)
            val title = obj.optString("name", "Local AI Quest")
            val arr = obj.getJSONArray("exercises")
            val exercises = mutableListOf<WorkoutExerciseEntity>()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                val name = e.getString("name")
                val catStr = e.optString("category", "STRENGTH")
                val cat = ExerciseCategories.coerce(catStr, name)
                exercises += WorkoutExerciseEntity(
                    workoutId = 0,
                    exerciseName = name,
                    category = cat,
                    targetSets = e.optInt("sets", 3).coerceIn(1, 6),
                    targetReps = e.optInt("reps", 10).coerceIn(1, 60),
                    sortOrder = i
                )
            }
            LocalAiWorkout(title, exercises)
        }.getOrNull()
    }

    suspend fun generateWorkout(

        prompt: String,
        heroClass: CharacterClass?,
        modelFile: File? = null
    ): LocalAiWorkout = withContext(Dispatchers.Default) {
        val p = prompt.trim().lowercase()

        val isCardio = p.contains("cardio") || p.contains("run") || p.contains("bike") || p.contains("endurance")
        val isFullBody = p.contains("full body") || p.contains("total") || p.contains("circuit")
        val isFlexibility = p.contains("stretch") || p.contains("yoga") || p.contains("flexibility")

        val title = when {
            isCardio -> "Local AI: Endurance Sprint"
            isFlexibility -> "Local AI: Mobility & Recovery"
            isFullBody -> "Local AI: Total Body Forge"
            heroClass != null -> "Local AI: ${heroClass.label} Quest"
            else -> "Local AI: Custom Workout"
        }

        val pool = when {
            isCardio -> CARDIO_EXERCISES
            isFlexibility -> FLEXIBILITY_EXERCISES
            isFullBody -> (STRENGTH_EXERCISES + BODYWEIGHT_EXERCISES).shuffled()
            else -> STRENGTH_EXERCISES
        }

        val selectedNames = pool.shuffled().take(Random.nextInt(4, 6))
        val exercises = selectedNames.mapIndexed { idx, name ->
            val cat = ExerciseCategories.infer(name)
            val sets = if (cat == ExerciseCategory.CARDIO || cat == ExerciseCategory.FLEXIBILITY) 1 else Random.nextInt(3, 5)
            val reps = when (cat) {
                ExerciseCategory.CARDIO -> 15
                ExerciseCategory.FLEXIBILITY -> 5
                ExerciseCategory.BODYWEIGHT -> 15
                else -> 10
            }

            WorkoutExerciseEntity(
                workoutId = 0,
                exerciseName = name,
                category = cat,
                targetSets = sets,
                targetReps = reps,
                sortOrder = idx
            )
        }

        LocalAiWorkout(title = title, exercises = exercises)
    }

    suspend fun generateBattleEpilogue(
        heroClass: CharacterClass,
        monsterName: String,
        victory: Boolean
    ): String = withContext(Dispatchers.Default) {
        if (victory) {
            val victories = listOf(
                "With legendary focus, the ${heroClass.label} strikes down the $monsterName, claiming hard-earned victory!",
                "Channeling maximum effort, you shatter the $monsterName's defense and emerge triumphant!",
                "The $monsterName crumbles under your unrelenting power. Victory is yours!"
            )
            victories.random()
        } else {
            "The $monsterName's fury proves overwhelming. Rest up, train hard, and return stronger!"
        }
    }
}
