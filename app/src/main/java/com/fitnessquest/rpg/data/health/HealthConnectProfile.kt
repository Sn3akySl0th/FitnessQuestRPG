package com.fitnessquest.rpg.data.health

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.fitnessquest.shared.wear.HrZone
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class BodyProfileSource {
    HEALTH_CONNECT,
    MANUAL,
    NONE
}

data class BodyProfile(
    val bodyWeightKg: Double? = null,
    val heightM: Double? = null,
    val dateOfBirthEpoch: Long? = null,
    val weightRecordTimeEpoch: Long? = null,
    val source: BodyProfileSource = BodyProfileSource.NONE,
) {
    val ageYears: Int?
        get() = dateOfBirthEpoch?.let { dob ->
            val zone = ZoneId.systemDefault()
            val birth = Instant.ofEpochMilli(dob).atZone(zone).toLocalDate()
            val now = LocalDate.now(zone)
            Period.between(birth, now).years
        }

    val maxHr: Int
        get() = HrZone.estimatedMaxHr(ageYears ?: 30)

    val isEmpty: Boolean
        get() = (bodyWeightKg == null) && (heightM == null) && (dateOfBirthEpoch == null)
}

object HeightFormat {
    fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
    fun cmToMeters(cm: Double): Double = cm / 100.0
    fun metersToCm(m: Double): Double = m * 100.0
    fun feetInchesToMeters(ft: Int, inc: Double): Double = (ft * 12 + inc) * 0.0254
    data class FtIn(val feet: Int, val inches: Double)
    fun metersToFeetInches(m: Double): FtIn {
        val totalInc = m / 0.0254
        return FtIn((totalInc / 12).toInt(), totalInc % 12)
    }

    fun parseToMeters(text: String, imperial: Boolean): Double? {
        val raw = text.trim()
        if (raw.isBlank()) return null
        return if (imperial) {
            val parts = raw.split(Regex("['\"\\s]+")).filter { it.isNotBlank() }
            if (parts.size == 2) {
                val ft = parts[0].toIntOrNull() ?: return null
                val inc = parts[1].toDoubleOrNull() ?: return null
                feetInchesToMeters(ft, inc)
            } else {
                raw.toDoubleOrNull()?.let { it * 0.0254 }
            }
        } else {
            raw.toDoubleOrNull()?.let { cmToMeters(it) }
        }
    }

    fun parseFeetInchesFields(ftText: String, incText: String): Double? {
        val ft = ftText.toIntOrNull() ?: 0
        val inc = incText.toDoubleOrNull() ?: 0.0
        if (ft == 0 && inc == 0.0) return null
        return feetInchesToMeters(ft, inc)
    }
}

/**
 * High-level wrapper for Health Connect client.
 * Handles permission checks, reading/writing height/weight, and step synchronization.
 */
class HealthConnectProfile(private val context: Context) {

    private val client by lazy {
        runCatching {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        }.getOrNull()
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun sdkStatus(): Int = runCatching {
        HealthConnectClient.getSdkStatus(context)
    }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)

    suspend fun hasCoreReadPermissions(): Boolean = runCatching {
        val granted = client?.permissionController?.getGrantedPermissions() ?: return@runCatching false
        granted.contains(HealthPermission.getReadPermission(HeightRecord::class)) &&
        granted.contains(HealthPermission.getReadPermission(WeightRecord::class))
    }.getOrDefault(false)

    suspend fun hasStepsPermission(): Boolean = runCatching {
        client?.permissionController?.getGrantedPermissions()
            ?.contains(HealthPermission.getReadPermission(StepsRecord::class)) == true
    }.getOrDefault(false)

    suspend fun hasWeightWritePermission(): Boolean = runCatching {
        client?.permissionController?.getGrantedPermissions()
            ?.contains(HealthPermission.getWritePermission(WeightRecord::class)) == true
    }.getOrDefault(false)

    /** Returns true if *all* requested permissions are granted. */
    suspend fun hasAllPermissions(): Boolean = runCatching {
        client?.permissionController?.getGrantedPermissions()
            ?.containsAll(permissions) == true
    }.getOrDefault(false)

    /**
     * Reads total steps recorded in Health Connect for a specific date (local timezone).
     */
    suspend fun readDailySteps(date: LocalDate = LocalDate.now()): Long {
        val c = client ?: return 0L
        if (!hasStepsPermission()) return 0L

        return runCatching {
            val zone = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zone).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()

            val records = c.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            ).records

            records.sumOf { it.count }
        }.getOrDefault(0L)
    }

    /**
     * Writes or updates the player's weight record in Health Connect / Google Fit.
     */
    suspend fun writeWeight(
        weightKg: Double,
        timestamp: Instant = Instant.now()
    ): Result<Unit> {
        val c = client ?: return Result.failure(IllegalStateException("Health Connect unavailable"))
        return runCatching {
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(timestamp)
            val record = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = timestamp,
                zoneOffset = zoneOffset,
                metadata = Metadata.manualEntry()
            )
            c.insertRecords(listOf(record))
            Unit
        }
    }

    suspend fun readLatest(): BodyProfile {
        val c = client ?: return BodyProfile()
        if (!hasCoreReadPermissions()) return BodyProfile()

        val now = Instant.now()
        val monthAgo = now.minus(30, ChronoUnit.DAYS)
        val yearAgo = now.minus(365, ChronoUnit.DAYS)

        // Height changes slowly; look back 1 year.
        val height = c.readRecords(
            ReadRecordsRequest(
                HeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(yearAgo, now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()?.height?.inMeters

        // Weight changes more often; look back 30 days.
        val latestWeightRecord = c.readRecords(
            ReadRecordsRequest(
                WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(monthAgo, now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()

        return BodyProfile(
            bodyWeightKg = latestWeightRecord?.weight?.inKilograms,
            heightM = height,
            dateOfBirthEpoch = null,
            weightRecordTimeEpoch = latestWeightRecord?.time?.toEpochMilli(),
            source = BodyProfileSource.HEALTH_CONNECT
        )
    }

    companion object {
        fun providerInstallIntent() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=com.google.android.apps.healthdata".toUri()
                setPackage("com.android.vending")
            }
        } else null
    }
}
