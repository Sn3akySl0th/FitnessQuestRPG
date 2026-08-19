package com.fitnessquest.rpg.data.steps

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.edit
import com.fitnessquest.rpg.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Daily step counting via hardware sensor and multi-source reconciliation.
 *
 * Steps passively push biome travel forward (STEPS_PER_KM steps = 1 km)
 * and trigger idle monster battles for loot and gold.
 */
class StepTracker(
    private val app: Application,
    private val repository: GameRepository,
    val engine: StepReconciliationEngine = StepReconciliationEngine(app, repository)
) : SensorEventListener {

    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = app.getSharedPreferences("fitquest_steps", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val stepsToday: StateFlow<Int> = engine.stepsToday

    private val _tracking = MutableStateFlow(value = false)
    val tracking: StateFlow<Boolean> = _tracking

    val hasSensor: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun hasPermission(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ||
            (app.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED)

    /** Idempotent; call at app start and again right after the permission is granted. */
    fun start() {
        // Trigger initial Health Connect sync on startup if permissions are available
        scope.launch {
            engine.syncHealthConnect()
        }

        if (_tracking.value || !hasPermission()) return
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        _tracking.value = registered
    }

    suspend fun syncHealthConnect(): Int = engine.syncHealthConnect()

    suspend fun recordManualSteps(steps: Int, isScan: Boolean = false): Int =
        engine.recordManualSteps(LocalDate.now().toEpochDay(), steps, isScan)

    suspend fun syncWearOsSteps(todayEpochDay: Long, steps: Int): Int =
        engine.syncWearOsSteps(todayEpochDay, steps)

    override fun onSensorChanged(event: SensorEvent) {
        val cumulative = event.values.firstOrNull()?.toInt() ?: return
        val today = LocalDate.now().toEpochDay()
        var day = prefs.getLong(KEY_DAY, -1L)
        var base = prefs.getInt(KEY_BASE, -1)

        // New day, first run, or reboot (cumulative counter restarted below baseline).
        if ((day != today) || (base < 0) || (cumulative < base)) {
            day = today
            base = cumulative
            prefs.edit {
                putLong(KEY_DAY, day)
                putInt(KEY_BASE, base)
            }
        }

        val sensorSteps = (cumulative - base).coerceAtLeast(0)
        scope.launch {
            engine.recordStepsCandidate(today, sensorSteps, StepSource.PHONE_SENSOR)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val KEY_DAY = "sensor_day"
        const val KEY_BASE = "sensor_base"
    }
}
