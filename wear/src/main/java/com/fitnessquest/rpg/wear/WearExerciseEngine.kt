package com.fitnessquest.rpg.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.AggregateDataType
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.CumulativeDataPoint
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.LocationAvailability
import androidx.health.services.client.data.SampleDataPoint
import com.fitnessquest.shared.wear.HrZone
import com.fitnessquest.shared.wear.WearLiveMetrics
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToLong

/**
 * Collects workout metrics via Health Services when available, with SensorManager fallback.
 * Also tracks HR zone time, rest HR recovery, and simple goals locally.
 */
class WearExerciseEngine(
    context: Context,
    private val onMetrics: (WearLiveMetrics) -> Unit,
    private val onGoal: (kind: String, message: String) -> Unit,
    private val onStatus: (String) -> Unit = {}
) : SensorEventListener {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(app)
    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val exerciseClient: ExerciseClient by lazy {
        HealthServices.getClient(app).exerciseClient
    }

    private var running = false
    private var usingHealthServices = false
    private var sensorsRegistered = false
    private var emitJob: Job? = null

    private var sessionStartedAt = 0L
    private var restStartedAt: Long? = null
    private var accumulatedRestMs = 0L
    private var lastZoneTickAt = 0L
    private var lastZone: HrZone? = null
    private var lastHighHrGoalAt = 0L

    private var bpm: Int? = null
    private var calories: Double? = null
    private var steps: Long? = null
    private var stepBaseline: Long? = null
    private var stepsPerMin: Long? = null
    private var distanceMeters: Double? = null
    private var speedMps: Double? = null
    private var paceSecPerKm: Double? = null
    private var elevationMeters: Double? = null
    private var floors: Double? = null
    private var hsActiveDurationMs: Long? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var zoneWarmupSec = 0
    private var zoneEasySec = 0
    private var zoneWorkSec = 0
    private var zoneHighSec = 0

    private var restHrStart: Int? = null
    private var restHrGoalMet = false
    private var lastStepMilestone = 0L
    private var lastCalorieMilestone = 0
    private var goalLabel: String? = null
    private var lastEmitAt = 0L
    private var maxHr: Int = HrZone.estimatedMaxHr()

    private val callback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            ingestHealthUpdate(update)
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            if (availability is LocationAvailability) {
                Log.i(TAG, "Location availability: $availability")
            }
        }

        override fun onRegistered() {
            Log.i(TAG, "Exercise callback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "Exercise callback registration failed", throwable)
            onStatus("Sensors fallback")
            scope.launch { startSensorFallback() }
        }
    }

    fun hasHrPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, hrPermission()) == PackageManager.PERMISSION_GRANTED

    fun hasActivityPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun setMaxHr(value: Int) {
        maxHr = value.coerceIn(140, 220)
    }

    fun start() {
        if (running) return
        running = true
        resetSessionCounters()
        onStatus("Starting sensors…")
        scope.launch {
            val hsOk = runCatching { startHealthServices() }.onFailure {
                Log.w(TAG, "Health Services start failed", it)
            }.getOrDefault(false)
            if (!hsOk) {
                onStatus(if (hasHrPermission()) "Reading sensors…" else "Allow heart rate")
                startSensorFallback()
            }
            emitJob?.cancel()
            emitJob = scope.launch {
                while (isActive && running) {
                    tickZoneTime()
                    checkLocalGoals()
                    emit(force = false)
                    delay(1_000)
                }
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        emitJob?.cancel()
        emitJob = null
        closeRestWindow()
        scope.launch {
            if (usingHealthServices) {
                runCatching { exerciseClient.endExerciseAsync().await(mainExecutor) }
                runCatching { exerciseClient.clearUpdateCallbackAsync(callback).await(mainExecutor) }
            }
            usingHealthServices = false
        }
        stopSensorFallback()
    }

    fun onRestChanged(restEndsAt: Long?) {
        if (restEndsAt != null && restEndsAt > System.currentTimeMillis()) {
            if (restStartedAt == null) {
                restStartedAt = System.currentTimeMillis()
                restHrStart = bpm
                restHrGoalMet = false
                goalLabel = "Rest until ♥ ≤ $REST_HR_GOAL"
                emit(force = true)
            }
        } else {
            closeRestWindow()
            if (goalLabel?.startsWith("Rest until") == true) goalLabel = null
            emit(force = true)
        }
    }

    private fun closeRestWindow() {
        val started = restStartedAt ?: return
        accumulatedRestMs += (System.currentTimeMillis() - started).coerceAtLeast(0L)
        restStartedAt = null
    }

    private fun resetSessionCounters() {
        sessionStartedAt = System.currentTimeMillis()
        restStartedAt = null
        accumulatedRestMs = 0L
        lastZoneTickAt = sessionStartedAt
        lastZone = null
        bpm = null
        calories = null
        steps = null
        stepBaseline = null
        stepsPerMin = null
        distanceMeters = null
        speedMps = null
        paceSecPerKm = null
        elevationMeters = null
        floors = null
        hsActiveDurationMs = null
        latitude = null
        longitude = null
        zoneWarmupSec = 0
        zoneEasySec = 0
        zoneWorkSec = 0
        zoneHighSec = 0
        restHrStart = null
        restHrGoalMet = false
        lastStepMilestone = 0L
        lastCalorieMilestone = 0
        goalLabel = "Quest sensors live"
    }

    private suspend fun startHealthServices(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        if (!hasHrPermission()) return false
        val capabilities = exerciseClient.getCapabilitiesAsync().await(mainExecutor)
        val type = listOf(ExerciseType.STRENGTH_TRAINING, ExerciseType.WORKOUT, ExerciseType.WALKING)
            .firstOrNull { it in capabilities.supportedExerciseTypes }
            ?: return false
        val typeCaps = capabilities.getExerciseTypeCapabilities(type)
        val supported = typeCaps.supportedDataTypes

        val dataTypes = linkedSetOf<DataType<*, *>>()
        fun addSupported(type: DataType<*, *>) {
            if (type in supported) dataTypes += type
        }
        addSupported(DataType.HEART_RATE_BPM)
        addSupported(DataType.CALORIES_TOTAL)
        addSupported(DataType.STEPS_TOTAL)
        addSupported(DataType.STEPS_PER_MINUTE)
        addSupported(DataType.DISTANCE_TOTAL)
        addSupported(DataType.SPEED)
        addSupported(DataType.PACE)
        addSupported(DataType.ABSOLUTE_ELEVATION)
        addSupported(DataType.ELEVATION_GAIN_TOTAL)
        addSupported(DataType.FLOORS_TOTAL)
        addSupported(DataType.ACTIVE_EXERCISE_DURATION_TOTAL)
        if (hasLocationPermission()) addSupported(DataType.LOCATION)
        if (dataTypes.isEmpty()) return false

        exerciseClient.setUpdateCallback(mainExecutor, callback)

        val goals = mutableListOf<ExerciseGoal<out Number>>()
        if (DataType.STEPS_TOTAL in supported) {
            runCatching {
                goals += ExerciseGoal.createMilestone(
                    DataTypeCondition(
                        dataType = DataType.STEPS_TOTAL,
                        threshold = STEP_GOAL_CHUNK,
                        comparisonType = ComparisonType.GREATER_THAN_OR_EQUAL
                    ),
                    STEP_GOAL_CHUNK
                )
            }.onFailure { Log.w(TAG, "Steps milestone unavailable: ${it.message}") }
        }
        if (DataType.CALORIES_TOTAL in supported) {
            runCatching {
                goals += ExerciseGoal.createMilestone(
                    DataTypeCondition(
                        dataType = DataType.CALORIES_TOTAL,
                        threshold = CAL_GOAL_CHUNK.toDouble(),
                        comparisonType = ComparisonType.GREATER_THAN_OR_EQUAL
                    ),
                    CAL_GOAL_CHUNK.toDouble()
                )
            }.onFailure { Log.w(TAG, "Calorie milestone unavailable: ${it.message}") }
        }

        val config = ExerciseConfig(
            exerciseType = type,
            dataTypes = dataTypes,
            isAutoPauseAndResumeEnabled = false,
            isGpsEnabled = hasLocationPermission() && DataType.LOCATION in dataTypes,
            exerciseGoals = goals
        )
        exerciseClient.startExerciseAsync(config).await(mainExecutor)
        usingHealthServices = true
        onStatus("Health Services live")
        Log.i(TAG, "Started HS exercise $type with ${dataTypes.size} data types")
        if (DataType.STEPS_TOTAL !in dataTypes) {
            startSensorFallback(hrOnly = false, stepsOnly = true)
        }
        return true
    }

    private fun ingestHealthUpdate(update: ExerciseUpdate) {
        val metrics = update.latestMetrics
        latestSample(metrics, DataType.HEART_RATE_BPM)?.roundToLong()?.toInt()?.takeIf { it > 0 }?.let {
            onBpm(it)
        }
        cumulative(metrics, DataType.CALORIES_TOTAL)?.let { calories = it }
        cumulative(metrics, DataType.STEPS_TOTAL)?.let { steps = it }
        latestSample(metrics, DataType.STEPS_PER_MINUTE)?.let { stepsPerMin = it }
        cumulative(metrics, DataType.DISTANCE_TOTAL)?.let { distanceMeters = it }
        latestSample(metrics, DataType.SPEED)?.let { speedMps = it }
        latestSample(metrics, DataType.PACE)?.let { paceSecPerKm = it }
        latestSample(metrics, DataType.ABSOLUTE_ELEVATION)?.let { elevationMeters = it }
        cumulative(metrics, DataType.ELEVATION_GAIN_TOTAL)?.let { elevationMeters = it }
        cumulative(metrics, DataType.FLOORS_TOTAL)?.let { floors = it }
        cumulative(metrics, DataType.ACTIVE_EXERCISE_DURATION_TOTAL)?.let { hsActiveDurationMs = it }

        metrics.getData(DataType.LOCATION).lastOrNull()?.value?.let { loc ->
            latitude = loc.latitude
            longitude = loc.longitude
        }

        update.activeDurationCheckpoint?.activeDuration?.toMillis()?.let { hsActiveDurationMs = it }

        update.latestAchievedGoals.forEach { goal ->
            val dataType = goal.dataTypeCondition.dataType
            val msg = when (dataType) {
                DataType.STEPS_TOTAL -> "Step goal forged"
                DataType.CALORIES_TOTAL -> "Calorie goal forged"
                else -> "Goal met"
            }
            goalLabel = msg
            onGoal("GOAL_MET", msg)
        }
        emit(force = true)
    }

    private fun startSensorFallback(hrOnly: Boolean = false, stepsOnly: Boolean = false) {
        if (!hrOnly && hasHrPermission() && heartSensor != null) {
            sensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_UI)
            sensorsRegistered = true
            onStatus("Reading HR…")
        }
        if (!hrOnly || stepsOnly) {
            if (hasActivityPermission() && stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorsRegistered = true
            }
        }
    }

    private fun stopSensorFallback() {
        if (!sensorsRegistered) return
        sensorManager.unregisterListener(this)
        sensorsRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val value = event.values.firstOrNull()?.toInt() ?: return
                if (value > 0) onBpm(value)
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val total = event.values.firstOrNull()?.toLong() ?: return
                if (stepBaseline == null) stepBaseline = total
                steps = (total - (stepBaseline ?: total)).coerceAtLeast(0L)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onBpm(value: Int) {
        bpm = value
        if (restStartedAt != null && !restHrGoalMet && value <= REST_HR_GOAL) {
            restHrGoalMet = true
            goalLabel = "Rest HR ready (≤$REST_HR_GOAL)"
            onGoal("RECOVERY_READY", goalLabel!!)
        }
        emit(force = true)
    }

    private fun tickZoneTime() {
        val now = System.currentTimeMillis()
        val currentBpm = bpm ?: return
        val zone = HrZone.fromBpm(currentBpm, maxHr)
        val elapsedSec = ((now - lastZoneTickAt).coerceAtLeast(0L) / 1000L).toInt()
        lastZoneTickAt = now
        if (elapsedSec <= 0) {
            lastZone = zone
            return
        }
        if (restStartedAt == null) {
            when (zone) {
                HrZone.WARMUP -> zoneWarmupSec += elapsedSec
                HrZone.EASY -> zoneEasySec += elapsedSec
                HrZone.WORK -> zoneWorkSec += elapsedSec
                HrZone.HIGH -> zoneHighSec += elapsedSec
            }
        } else if (zone == HrZone.WARMUP || zone == HrZone.EASY) {
            when (zone) {
                HrZone.WARMUP -> zoneWarmupSec += elapsedSec
                else -> zoneEasySec += elapsedSec
            }
        }
        if (lastZone != null && lastZone != zone && zone == HrZone.HIGH) {
            val now = System.currentTimeMillis()
            if (now - lastHighHrGoalAt >= HIGH_HR_COOLDOWN_MS) {
                lastHighHrGoalAt = now
                onGoal("ZONE_HIGH", "High heart! Soften the flame.")
            }
        }
        lastZone = zone
    }

    private fun checkLocalGoals() {
        val stepCount = steps
        if (stepCount != null) {
            val milestone = (stepCount / STEP_GOAL_CHUNK) * STEP_GOAL_CHUNK
            if (milestone > 0 && milestone > lastStepMilestone) {
                lastStepMilestone = milestone
                goalLabel = "$milestone steps forged"
                onGoal("GOAL_MET", goalLabel!!)
            }
        }
        val kcal = calories?.toInt()
        if (kcal != null) {
            val calMilestone = (kcal / CAL_GOAL_CHUNK) * CAL_GOAL_CHUNK
            if (calMilestone > 0 && calMilestone > lastCalorieMilestone) {
                lastCalorieMilestone = calMilestone
                goalLabel = "$calMilestone kcal forged"
                onGoal("CALORIES", goalLabel!!)
            }
        }
    }

    private fun emit(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastEmitAt < 800) return
        lastEmitAt = now
        val zone = bpm?.let { HrZone.fromBpm(it, maxHr) }
        val restDrop = if (restStartedAt != null) {
            val start = restHrStart
            val current = bpm
            if (start != null && current != null) (start - current).coerceAtLeast(0) else null
        } else null
        val sessionMs = (now - sessionStartedAt).coerceAtLeast(0L)
        val openRest = restStartedAt?.let { now - it } ?: 0L
        val activeMs = hsActiveDurationMs
            ?: (sessionMs - accumulatedRestMs - openRest).coerceAtLeast(0L)
        onMetrics(
            WearLiveMetrics(
                bpm = bpm,
                zone = zone?.label,
                caloriesKcal = calories,
                steps = steps,
                stepsPerMin = stepsPerMin,
                distanceMeters = distanceMeters,
                speedMps = speedMps,
                paceSecPerKm = paceSecPerKm,
                elevationMeters = elevationMeters,
                floors = floors,
                activeDurationMs = activeMs,
                sessionDurationMs = sessionMs,
                zoneWarmupSec = zoneWarmupSec,
                zoneEasySec = zoneEasySec,
                zoneWorkSec = zoneWorkSec,
                zoneHighSec = zoneHighSec,
                restHrStart = if (restStartedAt != null) restHrStart else null,
                restHrCurrent = if (restStartedAt != null) bpm else null,
                restHrDrop = restDrop,
                restHrGoalBpm = if (restStartedAt != null) REST_HR_GOAL else null,
                restHrGoalMet = restHrGoalMet && restStartedAt != null,
                latitude = latitude,
                longitude = longitude,
                goalLabel = goalLabel,
                atMillis = now
            )
        )
    }

    companion object {
        private const val TAG = "WearExercise"
        const val REST_HR_GOAL = 110
        private const val STEP_GOAL_CHUNK = 500L
        private const val CAL_GOAL_CHUNK = 50
        private const val HIGH_HR_COOLDOWN_MS = 180_000L // 3 minutes debounce to prevent spam

        fun hrPermission(): String =
            if (Build.VERSION.SDK_INT >= 36) {
                "android.permission.health.READ_HEART_RATE"
            } else {
                Manifest.permission.BODY_SENSORS
            }

        fun requiredRuntimePermissions(includeLocation: Boolean): Array<String> = buildList {
            add(hrPermission())
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (includeLocation) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.toTypedArray()
    }
}

private fun <T : Any> latestSample(
    container: DataPointContainer,
    type: DeltaDataType<T, SampleDataPoint<T>>
): T? = container.getData(type).lastOrNull()?.value

private fun <T : Number> cumulative(
    container: DataPointContainer,
    type: AggregateDataType<T, CumulativeDataPoint<T>>
): T? = container.getData(type)?.total

private suspend fun <T> ListenableFuture<T>.await(executor: Executor): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: Exception) {
                    cont.resumeWithException(e.cause ?: e)
                }
            },
            executor
        )
    }
