package com.fitnessquest.rpg.data.steps

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.health.HealthConnectProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class StepSource {
    PHONE_SENSOR,
    HEALTH_CONNECT,
    WEAR_OS,
    MANUAL_ENTRY,
    PEDOMETER_SCAN
}

/**
 * Reconciles step counts across multiple concurrent sources:
 * - Phone hardware step sensor
 * - Health Connect (Fitbit, Garmin, Samsung, Google Fit, etc.)
 * - Wear OS companion watch
 * - Manual Scout Journal / Pedometer OCR scans
 *
 * Implements a high-water mark deduplication model per calendar day:
 * Ensures players are credited for the maximum verified step count seen across
 * all active devices without duplicate travel or monster encounter rewards.
 */
class StepReconciliationEngine(
    private val app: Application,
    private val repository: GameRepository,
    private val healthConnect: HealthConnectProfile? = null
) {
    private val prefs = app.getSharedPreferences("fitquest_steps", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stepsToday = MutableStateFlow(cachedToday())
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    private val _lastSyncSource = MutableStateFlow<StepSource?>(null)
    val lastSyncSource: StateFlow<StepSource?> = _lastSyncSource.asStateFlow()

    /** Maximum realistic step ceiling per day to prevent accidental typos. */
    val maxDailyStepCeiling: Int = 50_000

    /**
     * Records a new step count candidate for a given epoch day.
     * Deduplicates against already credited steps and awards progression for the positive delta.
     *
     * @return Newly credited delta steps (0 if candidate <= already credited).
     */
    suspend fun recordStepsCandidate(
        todayEpochDay: Long,
        candidateTotal: Int,
        source: StepSource
    ): Int {
        if (candidateTotal <= 0) return 0
        val clampedTotal = candidateTotal.coerceAtMost(maxDailyStepCeiling)

        val currentEpochDay = LocalDate.now().toEpochDay()
        val keyDay = "credited_day_$todayEpochDay"
        val keyMax = "max_steps_$todayEpochDay"

        val previouslyCredited = prefs.getInt(keyMax, 0)
        val currentDayStored = prefs.getLong(keyDay, -1L)

        val baseCredited = if (currentDayStored == todayEpochDay) previouslyCredited else 0

        if (clampedTotal > baseCredited) {
            val delta = clampedTotal - baseCredited
            prefs.edit {
                putLong(keyDay, todayEpochDay)
                putInt(keyMax, clampedTotal)
                if (todayEpochDay == currentEpochDay) {
                    putInt("cache", clampedTotal)
                    putLong("day", currentEpochDay)
                }
            }

            if (todayEpochDay == currentEpochDay) {
                _stepsToday.value = clampedTotal
                _lastSyncSource.value = source
            }

            // Push delta to Biome Travel and Idle Encounters
            repository.processIdleSteps(delta)
            return delta
        } else {
            if (todayEpochDay == currentEpochDay && clampedTotal > _stepsToday.value) {
                _stepsToday.value = clampedTotal
            }
            return 0
        }
    }

    /**
     * Polls Health Connect for the current date's total aggregate steps.
     */
    suspend fun syncHealthConnect(date: LocalDate = LocalDate.now()): Int {
        val hc = healthConnect ?: return 0
        val hcSteps = hc.readDailySteps(date).toInt()
        if (hcSteps > 0) {
            return recordStepsCandidate(date.toEpochDay(), hcSteps, StepSource.HEALTH_CONNECT)
        }
        return 0
    }

    /**
     * Handles step reports sent by the Wear OS companion watch.
     */
    suspend fun syncWearOsSteps(todayEpochDay: Long, watchTotalSteps: Int): Int {
        return recordStepsCandidate(todayEpochDay, watchTotalSteps, StepSource.WEAR_OS)
    }

    /**
     * Manually logs steps entered via the Scout Journal or OCR Photo Scan.
     */
    suspend fun recordManualSteps(
        todayEpochDay: Long,
        reportedSteps: Int,
        isScan: Boolean = false
    ): Int {
        val source = if (isScan) StepSource.PEDOMETER_SCAN else StepSource.MANUAL_ENTRY
        return recordStepsCandidate(todayEpochDay, reportedSteps, source)
    }

    /**
     * Refreshes the today step cache from disk / background updates.
     */
    fun refreshTodayCache() {
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong("credited_day_$today", -1L) == today) {
            _stepsToday.value = prefs.getInt("max_steps_$today", 0)
        }
    }

    private fun cachedToday(): Int {
        val today = LocalDate.now().toEpochDay()
        return if (prefs.getLong("credited_day_$today", -1L) == today) {
            prefs.getInt("max_steps_$today", 0)
        } else if (prefs.getLong("day", -1L) == today) {
            prefs.getInt("cache", 0)
        } else {
            0
        }
    }
}
