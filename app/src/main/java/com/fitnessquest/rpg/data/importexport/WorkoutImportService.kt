package com.fitnessquest.rpg.data.importexport

import com.fitnessquest.rpg.data.db.ExerciseCategory
import com.fitnessquest.rpg.data.db.SessionEntity
import com.fitnessquest.rpg.data.db.SetLogEntity
import com.fitnessquest.rpg.data.db.WorkoutExerciseEntity
import com.fitnessquest.rpg.domain.ExerciseCategories
import com.fitnessquest.rpg.domain.GameMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ImportedExercise(
    val name: String,
    val category: ExerciseCategory,
    val sets: Int,
    val reps: Int,
    val weightKg: Double = 0.0,
    val notes: String = "",
) {
    fun toEntity(workoutId: Long = 0, sortOrder: Int = 0): WorkoutExerciseEntity =
        WorkoutExerciseEntity(
            workoutId = workoutId,
            exerciseName = name,
            category = category,
            targetSets = sets,
            targetReps = reps,
            sortOrder = sortOrder
        )
}

enum class ImportedWorkoutKind {
    TEMPLATE,
    HISTORY
}

data class ImportedWorkout(
    val title: String,
    val exercises: List<ImportedExercise>,
    val kind: ImportedWorkoutKind = ImportedWorkoutKind.TEMPLATE,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val logs: List<SetLogEntity> = emptyList(),
    val externalId: String? = null
)

data class ImportPersistResult(
    val templatesAdded: Int = 0,
    val sessionsAdded: Int = 0
) {
    val totalAdded: Int get() = templatesAdded + sessionsAdded
}

data class HevyHistoryRepairResult(
    val deletedBadTemplates: Int = 0,
    val importedHistorySessions: Int = 0
)

/**
 * Handles importing workout routines and workout history from external sources:
 * - Direct REST API integration with Hevy (`https://api.hevyapp.com/v1/...`)
 * - CSV file imports (Hevy CSV export, Strong CSV export, or Generic Workout CSV)
 */
object WorkoutImportService {

    private val httpClient = OkHttpClient.Builder().build()
    private const val HEVY_PAGE_SIZE = 10

    /**
     * Fetches routines from Hevy's v1 REST API using a user-provided API key.
     * Endpoint: GET https://api.hevyapp.com/v1/routines
     */
    suspend fun fetchHevyRoutines(apiKey: String): Result<List<ImportedWorkout>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Please enter a valid Hevy API key." }

            val imported = mutableListOf<ImportedWorkout>()
            val seenTitles = HashSet<String>()
            val folderMap = HashMap<String, String>()

            fun extractExerciseTitle(exObj: JSONObject, index: Int): String {
                var title = exObj.optString("title")
                if (title.isBlank()) title = exObj.optString("name")
                if (title.isBlank()) title = exObj.optJSONObject("exercise_template")?.optString("title").orEmpty()
                if (title.isBlank()) title = exObj.optJSONObject("exercise_template")?.optString("name").orEmpty()
                if (title.isBlank()) title = exObj.optString("exercise_title")
                if (title.isBlank()) title = exObj.optString("exercise_template_id")
                if (title.isBlank()) title = "Exercise ${index + 1}"
                return title
            }

            fun formatDateLabel(isoString: String): String {
                if (isoString.isBlank()) return ""
                return try {
                    val parts = isoString.split("T")[0].split("-")
                    if (parts.size == 3) {
                        val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        val m = parts[1].toIntOrNull() ?: 1
                        "${months[m.coerceIn(1, 12)]} ${parts[2].toIntOrNull() ?: 1}, ${parts[0]}"
                    } else isoString
                } catch (_: Exception) {
                    isoString
                }
            }

            fun parseTimeMillis(raw: String): Long? {
                if (raw.isBlank()) return null
                return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
                    ?: runCatching {
                        LocalDate.parse(raw.substringBefore("T")).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
            }

            fun parseWorkoutObject(routineObj: JSONObject, defaultPrefix: String = "Routine", isCompletedSession: Boolean = false) {
                val startTime = routineObj.optString("start_time").ifBlank { routineObj.optString("created_at") }.ifBlank { routineObj.optString("updated_at") }
                val endTime = routineObj.optString("end_time").ifBlank { routineObj.optString("updated_at") }.ifBlank { startTime }
                val dateLabel = formatDateLabel(startTime)
                val defaultTitle = if (dateLabel.isNotBlank()) "Workout ($dateLabel)" else defaultPrefix

                val rawTitle = routineObj.optString("title").ifBlank { routineObj.optString("name", defaultTitle) }
                val folderId = routineObj.optString("folder_id")
                val folderTitle = folderMap[folderId]

                var fullTitle = if (!folderTitle.isNullOrBlank()) "[$folderTitle] $rawTitle" else rawTitle
                if (isCompletedSession && dateLabel.isNotBlank() && !fullTitle.contains(dateLabel)) {
                    fullTitle = "$fullTitle ($dateLabel)"
                }
                if (fullTitle.isBlank()) return

                val exercisesArray = routineObj.optJSONArray("exercises") ?:
                routineObj.optJSONArray("routine_exercises") ?:
                routineObj.optJSONArray("workout_exercises") ?:
                routineObj.optJSONArray("exercises_attributes") ?:
                routineObj.optJSONArray("data")


                val exercisesList = mutableListOf<ImportedExercise>()
                val sessionLogs = mutableListOf<SetLogEntity>()

                if (exercisesArray != null) {
                    for (j in 0 until exercisesArray.length()) {
                        val exObj = exercisesArray.getJSONObject(j)
                        val exTitle = extractExerciseTitle(exObj, j)
                        val setsArray = exObj.optJSONArray("sets") ?: exObj.optJSONArray("routine_sets") ?: exObj.optJSONArray("workout_sets")
                        val category = ExerciseCategories.infer(exTitle)

                        var setNum = setsArray?.length() ?: 3
                        if (setNum <= 0) setNum = 3
                        var firstReps = 10
                        var firstWeight = 0.0

                        if ((setsArray != null) && (setsArray.length() > 0)) {
                            val firstSet = setsArray.getJSONObject(0)
                            firstReps = firstSet.optInt("reps", 10).coerceAtLeast(1)
                            firstWeight = firstSet.optDouble("weight_kg", firstSet.optDouble("weight", 0.0))

                            if (isCompletedSession) {
                                for (setIndex in 0 until setsArray.length()) {
                                    val setObj = setsArray.getJSONObject(setIndex)
                                    val reps = setObj.optInt("reps", 0).coerceAtLeast(0)
                                    val weight = setObj.optDouble("weight_kg", setObj.optDouble("weight", 0.0)).coerceAtLeast(0.0)
                                    val durationMin = (
                                        setObj.optDouble("duration_seconds", 0.0) / 60.0
                                    ).takeIf { it > 0.0 } ?: setObj.optDouble("duration_min", 0.0)
                                    val distanceKm = setObj.optDouble(
                                        "distance_km",
                                        setObj.optDouble("distance_meters", 0.0) / 1000.0
                                    ).coerceAtLeast(0.0)
                                    sessionLogs.add(
                                        SetLogEntity(
                                            sessionId = 0,
                                            exerciseName = exTitle,
                                            category = category,
                                            weightKg = weight,
                                            reps = reps,
                                            durationMin = durationMin.coerceAtLeast(0.0),
                                            distanceKm = distanceKm
                                        )
                                    )
                                }
                            }
                        }

                        exercisesList.add(
                            ImportedExercise(
                                name = exTitle,
                                category = category,
                                sets = setNum,
                                reps = firstReps,
                                weightKg = firstWeight
                            )
                        )
                    }
                }

                if (exercisesList.isNotEmpty()) {
                    var finalTitle = fullTitle
                    var counter = 1
                    while (seenTitles.contains(finalTitle.lowercase())) {
                        finalTitle = "$fullTitle ($counter)"
                        counter++
                    }
                    seenTitles.add(finalTitle.lowercase())
                    imported.add(
                        ImportedWorkout(
                            title = finalTitle,
                            exercises = exercisesList,
                            kind = if (isCompletedSession) ImportedWorkoutKind.HISTORY else ImportedWorkoutKind.TEMPLATE,
                            startedAt = parseTimeMillis(startTime),
                            endedAt = parseTimeMillis(endTime),
                            logs = sessionLogs
                        )
                    )
                }
            }

            // 1. Fetch Routine Folders (Trainer Programs) - and pull nested routines
            try {
                var folderPage = 1
                var totalFolderPages: Int
                do {
                    val request = Request.Builder()
                        .url("https://api.hevyapp.com/v1/routine_folders?page=$folderPage&pageSize=$HEVY_PAGE_SIZE")
                        .header("api-key", key)
                        .header("Accept", "application/json")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val json = JSONObject(responseBody)
                        totalFolderPages = json.optInt("page_count", json.optInt("pageCount", 1))
                        val foldersArray = json.optJSONArray("routine_folders") ?: 
                                          json.optJSONArray("folders") ?: 
                                          json.optJSONArray("data") ?:
                                          json.optJSONArray("programs")
                                          
                        if (foldersArray != null) {
                            for (i in 0 until foldersArray.length()) {
                                val folderObj = foldersArray.getJSONObject(i)
                                val folderId = folderObj.optString("id")
                                val folderTitle = folderObj.optString("title").ifBlank { 
                                    folderObj.optString("name", "Trainer Program ${i + 1}") 
                                }
                                if (folderId.isNotBlank()) {
                                    folderMap[folderId] = folderTitle
                                }

                                // Aggressively pull routines nested inside folders (common for trainer programs)
                                val nestedRoutines = folderObj.optJSONArray("routines") ?: 
                                    folderObj.optJSONArray("data") ?:
                                    folderObj.optJSONArray("routine_templates") ?:
                                    folderObj.optJSONArray("workout_templates") ?:
                                    folderObj.optJSONArray("exercises") // Some API versions nest here
                                    
                                if (nestedRoutines != null) {
                                    for (j in 0 until nestedRoutines.length()) {
                                        val nestedObj = nestedRoutines.getJSONObject(j)
                                        // Ensure this object actually looks like a routine (has an exercises array)
                                        // and is not just a flat list of exercises being mistaken for a workout list.
                                        val hasExercises = nestedObj.has("exercises") || 
                                                          nestedObj.has("routine_exercises") || 
                                                          nestedObj.has("workout_exercises")
                                        
                                        if (hasExercises) {
                                            parseWorkoutObject(nestedObj, "Trainer Routine", isCompletedSession = false)
                                        }
                                    }
                                }
                            }
                        }
                    } else break
                    folderPage++
                } while (folderPage <= totalFolderPages && folderPage <= 20)
            } catch (e: Exception) {
                e.printStackTrace()
            }




            // 2. Fetch User & Trainer Routines (/v1/routines)
            try {
                var page = 1
                var totalPages = 1
                do {
                    val request = Request.Builder()
                        .url("https://api.hevyapp.com/v1/routines?page=$page&pageSize=$HEVY_PAGE_SIZE")
                        .header("api-key", key)
                        .header("Accept", "application/json")
                        .build()



                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val json = JSONObject(responseBody)
                        totalPages = json.optInt("page_count", json.optInt("pageCount", 1))
                        val routinesArray = json.optJSONArray("routines") ?: json.optJSONArray("routine_folders") ?: json.optJSONArray("data")

                        if (routinesArray != null) {
                            for (i in 0 until routinesArray.length()) {
                                parseWorkoutObject(routinesArray.getJSONObject(i), "Hevy Routine ${i + 1}", isCompletedSession = false)
                            }
                        }
                    } else break
                    page++
                } while (page <= totalPages && page <= 50)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Fetch Past Completed Workout Sessions (/v1/workouts)
            try {
                var page = 1
                var totalPages = 1
                do {
                    val request = Request.Builder()
                        .url("https://api.hevyapp.com/v1/workouts?page=$page&pageSize=$HEVY_PAGE_SIZE")
                        .header("api-key", key)
                        .header("Accept", "application/json")
                        .build()


                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val json = JSONObject(responseBody)
                        totalPages = json.optInt("page_count", json.optInt("pageCount", 1))
                        val workoutsArray = json.optJSONArray("workouts") ?: json.optJSONArray("data")

                        if (workoutsArray != null && workoutsArray.length() > 0) {
                            for (i in 0 until workoutsArray.length()) {
                                val wObj = workoutsArray.getJSONObject(i)
                                parseWorkoutObject(wObj, "Completed Workout ${i + 1}", isCompletedSession = true)
                            }
                        } else break
                    } else break
                    page++
                } while (page <= totalPages && page <= 200)
            } catch (e: Exception) {
                e.printStackTrace()
            }


            check(imported.isNotEmpty()) { "No routines, trainer programs, or past completed workouts found in your Hevy account." }
            imported
        }
    }

    suspend fun fetchHevyWorkoutHistory(apiKey: String): Result<List<ImportedWorkout>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Please enter a valid Hevy API key." }

            val imported = mutableListOf<ImportedWorkout>()
            val seenTitles = HashSet<String>()

            fun parseWorkoutHistoryObject(workoutObj: JSONObject, index: Int) {
                val externalId = workoutObj.optString("id").ifBlank {
                    workoutObj.optString("workout_id")
                }.ifBlank { null }
                val startTime = workoutObj.optString("start_time").ifBlank {
                    workoutObj.optString("created_at")
                }
                val endTime = workoutObj.optString("end_time").ifBlank {
                    workoutObj.optString("updated_at")
                }.ifBlank { startTime }
                val dateLabel = formatDateLabel(startTime)
                val rawTitle = workoutObj.optString("title").ifBlank {
                    if (dateLabel.isNotBlank()) "Workout ($dateLabel)" else "Completed Workout ${index + 1}"
                }
                var title = rawTitle
                if (dateLabel.isNotBlank() && !title.contains(dateLabel)) {
                    title = "$title ($dateLabel)"
                }

                val exercisesArray = workoutObj.optJSONArray("exercises") ?: return
                val exercises = mutableListOf<ImportedExercise>()
                val logs = mutableListOf<SetLogEntity>()

                for (exerciseIndex in 0 until exercisesArray.length()) {
                    val exObj = exercisesArray.getJSONObject(exerciseIndex)
                    val exTitle = extractExerciseTitle(exObj, exerciseIndex)
                    val category = ExerciseCategories.infer(exTitle)
                    val setsArray = exObj.optJSONArray("sets")
                    var setCount = setsArray?.length() ?: 0
                    var firstReps = 0
                    var firstWeight = 0.0

                    if (setsArray != null) {
                        for (setIndex in 0 until setsArray.length()) {
                            val setObj = setsArray.getJSONObject(setIndex)
                            val reps = setObj.optInt("reps", 0).coerceAtLeast(0)
                            val weight = setObj.optDouble("weight_kg", setObj.optDouble("weight", 0.0)).coerceAtLeast(0.0)
                            val durationMin = setObj.optDouble("duration_seconds", 0.0).takeIf { it > 0.0 }?.div(60.0)
                                ?: setObj.optDouble("duration_min", 0.0)
                            val distanceKm = setObj.optDouble(
                                "distance_km",
                                setObj.optDouble("distance_meters", 0.0) / 1000.0
                            ).coerceAtLeast(0.0)
                            val rpe = setObj.optDouble("rpe", Double.NaN).takeIf { !it.isNaN() }
                            val rir = rpe?.let { (10.0 - it).toInt().coerceIn(0, 10) }
                                ?: setObj.optInt("rir", -1).takeIf { it >= 0 }

                            if (firstReps == 0 && reps > 0) firstReps = reps
                            if (firstWeight == 0.0 && weight > 0.0) firstWeight = weight
                            logs.add(
                                SetLogEntity(
                                    sessionId = 0,
                                    exerciseName = exTitle,
                                    category = category,
                                    weightKg = weight,
                                    reps = reps,
                                    durationMin = durationMin.coerceAtLeast(0.0),
                                    distanceKm = distanceKm,
                                    rir = rir
                                )
                            )
                        }
                    }
                    if (setCount <= 0) setCount = 1
                    exercises.add(
                        ImportedExercise(
                            name = exTitle,
                            category = category,
                            sets = setCount,
                            reps = firstReps,
                            weightKg = firstWeight
                        )
                    )
                }

                if (logs.isEmpty()) return
                val baseTitle = title.trim().ifBlank { "Completed Workout ${index + 1}" }
                var finalTitle = baseTitle
                var counter = 1
                while (seenTitles.contains(finalTitle.lowercase())) {
                    finalTitle = "$baseTitle ($counter)"
                    counter++
                }
                seenTitles.add(finalTitle.lowercase())
                imported.add(
                    ImportedWorkout(
                        title = finalTitle,
                        exercises = exercises,
                        kind = ImportedWorkoutKind.HISTORY,
                        startedAt = parseTimeMillis(startTime),
                        endedAt = parseTimeMillis(endTime),
                        logs = logs,
                        externalId = externalId
                    )
                )
            }

            var page = 1
            var totalPages = 1
            do {
                val request = Request.Builder()
                    .url("https://api.hevyapp.com/v1/workouts?page=$page&pageSize=$HEVY_PAGE_SIZE")
                    .header("api-key", key)
                    .header("Accept", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) break
                val json = JSONObject(response.body?.string().orEmpty())
                totalPages = json.optInt("page_count", 1)
                val workoutsArray = json.optJSONArray("workouts") ?: json.optJSONArray("data")
                if (workoutsArray == null || workoutsArray.length() <= 0) break
                for (i in 0 until workoutsArray.length()) {
                    parseWorkoutHistoryObject(workoutsArray.getJSONObject(i), i)
                }
                page++
            } while (page <= totalPages && page <= 500)

            check(imported.isNotEmpty()) { "No completed workout history found in your Hevy account." }
            imported
        }
    }

    /**
     * Parses a CSV InputStream (from Hevy CSV export, Strong CSV export, or Generic CSV).
     * Auto-detects columns and groups rows into workouts and exercises.
     */
    suspend fun parseCsvStream(inputStream: InputStream): Result<List<ImportedWorkout>> = withContext(Dispatchers.IO) {
        runCatching {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.use { it.readLines() }.asSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
            require(lines.size >= 2) { "CSV file is empty or missing headers." }

            val headerLine = lines[0]
            val headers = parseCsvRow(headerLine).map { it.trim().lowercase().replace("_", " ") }

            // Precise Disambiguated Column Index Detection
            val titleIdx = headers.indexOfFirst { h ->
                (h.contains("workout name") || h.contains("workout title") || h.contains("routine name") || h.contains("routine title") || h.contains("routine") || h == "title" || h == "workout") &&
                !h.contains("exercise")
            }.let { idx ->
                if (idx >= 0) idx
                else headers.indexOfFirst { h -> (h == "name" || h.contains("title")) && !h.contains("exercise") }.coerceAtLeast(0)
            }

            val exerciseIdx = headers.indexOfFirst { h ->
                (h.contains("exercise name") || h.contains("exercise title") || h.contains("exercise_name") || h.contains("exercise_title") || h.contains("movement")) &&
                !h.contains("workout") && !h.contains("routine")
            }.let { idx ->
                if (idx >= 0) idx
                else headers.indexOfFirst { h ->
                    (h.contains("exercise") || h.contains("name")) && !h.contains("workout") && !h.contains("routine")
                }.let { idx2 ->
                    if (idx2 >= 0 && idx2 != titleIdx) idx2
                    else if (titleIdx == 0) 1.coerceAtMost(headers.size - 1) else 0
                }
            }

            val sessionIdIdx = headers.indexOfFirst { h ->
                (h.contains("workout id") || h.contains("session id") || h.contains("workout_id") || h.contains("session_id")) && !h.contains("exercise")
            }
            val weightIdx = headers.indexOfFirst { it.contains("weight") || it.contains("kg") || it.contains("lbs") }
            val repsIdx = headers.indexOfFirst { it.contains("reps") || it.contains("rep") }
            val notesIdx = headers.indexOfFirst { it.contains("notes") || it.contains("comment") }
            val startIdx = headers.indexOfFirst { it.contains("start time") || it.contains("started") || it == "start" || it.contains("date") }
            val endIdx = headers.indexOfFirst { it.contains("end time") || it.contains("ended") || it == "end" }
            val durationIdx = headers.indexOfFirst { it.contains("duration") || it.contains("seconds") || it.contains("time") }
            val distanceIdx = headers.indexOfFirst { it.contains("distance") || it.contains("km") || it.contains("mile") }
            val csvLooksLikeHistory = startIdx >= 0 || endIdx >= 0 || sessionIdIdx >= 0

            fun parseCsvTime(raw: String?): Long? {
                val value = raw?.trim().orEmpty()
                if (value.isBlank()) return null
                return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
                    ?: runCatching {
                        val cleaned = value.replace(" ", "T")
                        Instant.parse(if (cleaned.contains("T") && !cleaned.endsWith("Z")) "${cleaned}Z" else cleaned).toEpochMilli()
                    }.getOrNull()
                    ?: runCatching {
                        val datePart = value.substringBefore("T").substringBefore(" ")
                        val parts = datePart.split("-", "/")
                        if (parts.size == 3) {
                            val year = if (parts[0].length == 4) parts[0].toInt() else parts[2].toInt()
                            val month = if (parts[0].length == 4) parts[1].toInt() else parts[0].toInt()
                            val day = if (parts[0].length == 4) parts[2].toInt() else parts[1].toInt()
                            LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else null
                    }.getOrNull()
            }

            class CsvSetInfo(
                val exerciseName: String,
                val category: ExerciseCategory,
                val weightKg: Double,
                val reps: Int,
                val durationMin: Double,
                val distanceKm: Double,
                val notes: String
            )

            class CsvSessionInfo(
                val workoutTitle: String,
                val startedAt: Long?,
                val endedAt: Long?,
                val explicitId: String?,
                val rawStart: String?,
                val sets: MutableList<CsvSetInfo> = mutableListOf()
            )

            val sessionList = mutableListOf<CsvSessionInfo>()
            var currentSession: CsvSessionInfo? = null

            for (i in 1 until lines.size) {
                val row = parseCsvRow(lines[i])
                if (row.size <= exerciseIdx) continue

                val workoutTitle = row.getOrNull(titleIdx)?.takeIf { it.isNotBlank() } ?: "Imported Workout"
                val exerciseName = row.getOrNull(exerciseIdx)?.takeIf { it.isNotBlank() }?.let { normalizeExerciseName(it) } ?: continue

                val weightVal = if (weightIdx >= 0 && weightIdx < row.size) row[weightIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val repsVal = if (repsIdx >= 0 && repsIdx < row.size) row[repsIdx].toIntOrNull() ?: 10 else 10
                val notesVal = if (notesIdx >= 0 && notesIdx < row.size) row[notesIdx] else ""
                val category = ExerciseCategories.infer(exerciseName)

                val startRaw = if (startIdx >= 0 && startIdx < row.size) row[startIdx].takeIf { it.isNotBlank() } else null
                val endRaw = if (endIdx >= 0 && endIdx < row.size) row[endIdx].takeIf { it.isNotBlank() } else null
                val explicitSessionId = if (sessionIdIdx >= 0 && sessionIdIdx < row.size) row[sessionIdIdx].takeIf { it.isNotBlank() } else null
                val startedAt = parseCsvTime(startRaw)
                val endedAt = parseCsvTime(endRaw) ?: startedAt

                val durationRaw = if (durationIdx >= 0 && durationIdx < row.size) row[durationIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val durationMin = when {
                    durationRaw <= 0.0 -> 0.0
                    headers.getOrNull(durationIdx)?.contains("second") == true -> durationRaw / 60.0
                    else -> durationRaw
                }
                val distanceRaw = if (distanceIdx >= 0 && distanceIdx < row.size) row[distanceIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val distanceKm = if (headers.getOrNull(distanceIdx)?.contains("mile") == true) distanceRaw * 1.609344 else distanceRaw

                val belongsToCurrent = currentSession != null && (
                    (explicitSessionId != null && explicitSessionId == currentSession.explicitId) ||
                    (startedAt != null && startedAt == currentSession.startedAt && currentSession.workoutTitle.equals(workoutTitle, ignoreCase = true)) ||
                    (startedAt == null && startRaw != null && startRaw == currentSession.rawStart && currentSession.workoutTitle.equals(workoutTitle, ignoreCase = true)) ||
                    (startedAt == null && startRaw == null && currentSession.workoutTitle.equals(workoutTitle, ignoreCase = true))
                )

                if (!belongsToCurrent) {
                    val newSession = CsvSessionInfo(
                        workoutTitle = workoutTitle,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        explicitId = explicitSessionId,
                        rawStart = startRaw
                    )
                    sessionList.add(newSession)
                    currentSession = newSession
                }

                currentSession!!.sets.add(
                    CsvSetInfo(
                        exerciseName = exerciseName,
                        category = category,
                        weightKg = weightVal.coerceAtLeast(0.0),
                        reps = repsVal.coerceAtLeast(0),
                        durationMin = durationMin.coerceAtLeast(0.0),
                        distanceKm = distanceKm.coerceAtLeast(0.0),
                        notes = notesVal
                    )
                )
            }

            val result = mutableListOf<ImportedWorkout>()

            // 1. Create Workout Routine Templates for Training Grounds.
            // We group by Workout Title, but only take the LATEST session's structure to avoid
            // merging multiple versions of the same routine into one giant 29-exercise list.
            val latestSessionByTitle = sessionList.groupBy { it.workoutTitle }
                .mapValues { (_, sessions) ->
                    sessions.maxByOrNull { it.startedAt ?: 0L }
                }

            for ((title, latestSession) in latestSessionByTitle) {
                if (latestSession == null) continue

                // Group sets in the LATEST session by exercise name to get set counts
                val exerciseMap = LinkedHashMap<String, MutableList<CsvSetInfo>>()
                for (setData in latestSession.sets) {
                    exerciseMap.getOrPut(setData.exerciseName) { mutableListOf() }.add(setData)
                }

                val importedExercises = mutableListOf<ImportedExercise>()
                for ((exName, setList) in exerciseMap) {
                    val setsCount = setList.size
                    val avgReps = if (setList.isNotEmpty()) setList.first().reps else 10
                    val weight = if (setList.isNotEmpty()) setList.first().weightKg else 0.0
                    val notes = setList.firstOrNull { it.notes.isNotBlank() }?.notes ?: ""
                    val category = ExerciseCategories.infer(exName)

                    importedExercises.add(
                        ImportedExercise(
                            name = exName,
                            category = category,
                            sets = setsCount.coerceAtLeast(1),
                            reps = avgReps.coerceAtLeast(1),
                            weightKg = weight,
                            notes = notes
                        )
                    )
                }
                if (importedExercises.isNotEmpty()) {
                    result.add(
                        ImportedWorkout(
                            title = title,
                            exercises = importedExercises,
                            kind = ImportedWorkoutKind.TEMPLATE
                        )
                    )
                }
            }

            // 2. If history columns exist, also create History Sessions with set logs
            if (csvLooksLikeHistory) {
                for (session in sessionList) {
                    val setLogs = session.sets.map { s ->
                        SetLogEntity(
                            sessionId = 0,
                            exerciseName = s.exerciseName,
                            category = s.category,
                            weightKg = s.weightKg,
                            reps = s.reps,
                            durationMin = s.durationMin,
                            distanceKm = s.distanceKm
                        )
                    }

                    val byExercise = session.sets.groupBy { it.exerciseName }
                    val importedExercises = byExercise.map { (exName, sets) ->
                        ImportedExercise(
                            name = exName,
                            category = ExerciseCategories.infer(exName),
                            sets = sets.size.coerceAtLeast(1),
                            reps = sets.firstOrNull { it.reps > 0 }?.reps ?: 0,
                            weightKg = sets.firstOrNull { it.weightKg > 0.0 }?.weightKg ?: 0.0,
                            notes = sets.firstOrNull { it.notes.isNotBlank() }?.notes ?: ""
                        )
                    }

                    result.add(
                        ImportedWorkout(
                            title = session.workoutTitle,
                            exercises = importedExercises,
                            kind = ImportedWorkoutKind.HISTORY,
                            startedAt = session.startedAt,
                            endedAt = session.endedAt,
                            logs = setLogs
                        )
                    )
                }
            }

            check(result.isNotEmpty()) { "Could not find any workouts in the selected CSV file." }
            result
        }
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (!inQuotes) {
                        result.add(sb.toString().trim())
                        sb.clear()
                    } else {
                        sb.append(ch)
                    }
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    suspend fun performBackgroundSync(
        apiKey: String,
        userPrefs: com.fitnessquest.rpg.data.UserPrefs,
        gameRepository: com.fitnessquest.rpg.data.GameRepository,
        gemini: com.fitnessquest.rpg.data.ai.GeminiService? = null,
        renameTemplates: Boolean = false
    ): Result<ImportPersistResult> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            if (key.isBlank()) return@runCatching ImportPersistResult()

            val fetchResult = fetchHevyRoutines(key)
            val importedList = fetchResult.getOrNull().orEmpty()
            if (importedList.isEmpty()) return@runCatching ImportPersistResult()

            val result = gameRepository.importExternalWorkouts(
                importedWorkouts = importedList,
                gemini = gemini,
                renameTemplates = renameTemplates
            )
            userPrefs.setHevyLastSyncTimestamp(System.currentTimeMillis())
            result
        }
    }

    suspend fun repairHevyHistoryImport(
        apiKey: String,
        userPrefs: com.fitnessquest.rpg.data.UserPrefs,
        gameRepository: com.fitnessquest.rpg.data.GameRepository
    ): Result<HevyHistoryRepairResult> = withContext(Dispatchers.IO) {
        runCatching {
            val history = fetchHevyWorkoutHistory(apiKey).getOrThrow()
            val deleted = gameRepository.deleteBotchedHevyHistoryTemplates(history)
            val imported = gameRepository.importExternalWorkouts(history)
            userPrefs.setHevyLastSyncTimestamp(System.currentTimeMillis())
            HevyHistoryRepairResult(
                deletedBadTemplates = deleted,
                importedHistorySessions = imported.sessionsAdded
            )
        }
    }

    fun toSessionEntity(imported: ImportedWorkout): SessionEntity {
        val now = System.currentTimeMillis()
        val startedAt = imported.startedAt ?: imported.endedAt ?: now
        val endedAt = imported.endedAt ?: imported.startedAt ?: now
        val logsWithXp = imported.logs.map { log -> log.copy(xp = if (log.xp > 0) log.xp else GameMath.xpForSet(log)) }
        val token = imported.externalId?.let { "hevy_api_$it" }
            ?: "import_${startedAt}_${imported.title.hashCode()}_${logsWithXp.size}"
        return SessionEntity(
            name = imported.title,
            startedAt = startedAt,
            endedAt = endedAt,
            xpEarned = logsWithXp.sumOf { it.xp },
            goldEarned = 0,
            energyEarned = 0,
            setCount = logsWithXp.size,
            completionToken = token
        )
    }

    private fun parseTimeMillis(raw: String): Long? {
        if (raw.isBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(raw.substringBefore("T")).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
    }

    private fun formatDateLabel(isoString: String): String {
        if (isoString.isBlank()) return ""
        return try {
            val parts = isoString.split("T")[0].split("-")
            if (parts.size == 3) {
                val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val month = parts[1].toIntOrNull()?.coerceIn(1, 12) ?: 1
                "${months[month]} ${parts[2].toIntOrNull() ?: 1}, ${parts[0]}"
            } else {
                isoString
            }
        } catch (_: Exception) {
            isoString
        }
    }

    private fun normalizeExerciseName(raw: String): String {
        val n = raw.lowercase()
        return when {
            n.contains("treadmill") || 
            n == "run" || n == "running" || n == "jog" || n == "jogging" || n == "walk" || n == "walking" ||
            n.startsWith("run ") || n.startsWith("jog ") || n.startsWith("walk ") -> "Treadmill"
            else -> raw
        }
    }

    private fun extractExerciseTitle(exObj: JSONObject, index: Int): String {
        var title = exObj.optString("title")
        if (title.isBlank()) title = exObj.optString("name")
        if (title.isBlank()) title = exObj.optJSONObject("exercise_template")?.optString("title").orEmpty()
        if (title.isBlank()) title = exObj.optJSONObject("exercise_template")?.optString("name").orEmpty()
        if (title.isBlank()) title = exObj.optString("exercise_title")
        if (title.isBlank()) title = exObj.optString("exercise_template_id")
        if (title.isBlank()) title = "Exercise ${index + 1}"
        return normalizeExerciseName(title)
    }

    suspend fun inspectHevyApi(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Please enter a valid Hevy API key." }

            val sb = java.lang.StringBuilder()
            sb.appendLine("🔑 Hevy API Diagnostic Report")
            sb.appendLine("----------------------------------")

            // 1. Test /v1/routines
            try {
                val req = Request.Builder()
                    .url("https://api.hevyapp.com/v1/routines?page=1&pageSize=$HEVY_PAGE_SIZE")
                    .header("api-key", key)
                    .header("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                sb.appendLine("1️⃣ GET /v1/routines: HTTP ${resp.code}")
                if (resp.isSuccessful) {
                    val json = JSONObject(body)
                    val pages = json.optInt("page_count", json.optInt("pageCount", 1))
                    val arr = json.optJSONArray("routines") ?: json.optJSONArray("routine_folders") ?: json.optJSONArray("data")
                    val count = arr?.length() ?: 0
                    sb.appendLine("   • Found $count user routines on Page 1 ($pages total pages).")

                    for (i in 0 until minOf(count, 5)) {
                        val obj = arr!!.getJSONObject(i)
                        val t = obj.optString("title").ifBlank { obj.optString("name") }
                        sb.appendLine("     - $t")
                    }
                } else {
                    sb.appendLine("   • Error response: $body")
                }
            } catch (e: Exception) {
                sb.appendLine("1️⃣ GET /v1/routines Exception: ${e.message}")
            }
            sb.appendLine()

            // 2. Test /v1/routine_folders
            try {
                val req = Request.Builder()
                    .url("https://api.hevyapp.com/v1/routine_folders?page=1&pageSize=$HEVY_PAGE_SIZE")
                    .header("api-key", key)
                    .header("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                sb.appendLine("2️⃣ GET /v1/routine_folders: HTTP ${resp.code}")
                if (resp.isSuccessful) {
                    val json = JSONObject(body)
                    val pages = json.optInt("page_count", json.optInt("pageCount", 1))
                    val arr = json.optJSONArray("routine_folders") ?: json.optJSONArray("folders") ?: json.optJSONArray("routines") ?: json.optJSONArray("data")
                    val count = arr?.length() ?: 0
                    sb.appendLine("   • Found $count trainer program folders on Page 1 ($pages total pages).")
                    for (i in 0 until minOf(count, 10)) {
                        val fObj = arr!!.getJSONObject(i)
                        val t = fObj.optString("title").ifBlank { fObj.optString("name", "Unnamed Folder") }
                        val nested = fObj.optJSONArray("routines") ?: 
                                     fObj.optJSONArray("data") ?: 
                                     fObj.optJSONArray("routine_templates")
                        val n = nested?.length() ?: 0
                        sb.appendLine("     - '$t' (ID: ${fObj.optString("id")}) has $n nested routines.")
                    }
                } else {
                    sb.appendLine("   • Error response: $body")
                }
            } catch (e: Exception) {
                sb.appendLine("2️⃣ GET /v1/routine_folders Exception: ${e.message}")
            }


            sb.appendLine()

            // 3. Test /v1/workouts
            try {
                val req = Request.Builder()
                    .url("https://api.hevyapp.com/v1/workouts?page=1&pageSize=$HEVY_PAGE_SIZE")
                    .header("api-key", key)
                    .header("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                sb.appendLine("3️⃣ GET /v1/workouts: HTTP ${resp.code}")
                if (resp.isSuccessful) {
                    val json = JSONObject(body)
                    val totalPages = json.optInt("page_count", json.optInt("pageCount", 1))
                    val arr = json.optJSONArray("workouts") ?: json.optJSONArray("data")

                    val count = arr?.length() ?: 0
                    sb.appendLine("   • Account History: $totalPages Total Pages (~${totalPages * 10} workouts).")
                    sb.appendLine("   • Page 1 Sample ($count workouts):")
                    for (i in 0 until minOf(count, 5)) {
                        val obj = arr!!.getJSONObject(i)
                        val t = obj.optString("title").ifBlank { obj.optString("name") }
                        val time = obj.optString("start_time")
                        sb.appendLine("     - $t ($time)")
                    }
                } else {
                    sb.appendLine("   • Error response: $body")
                }
            } catch (e: Exception) {
                sb.appendLine("3️⃣ GET /v1/workouts Exception: ${e.message}")
            }

            sb.toString()
        }
    }
}
