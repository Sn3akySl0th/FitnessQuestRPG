package com.fitnessquest.rpg.data.health

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fitnessquest.shared.wear.HrZone
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
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
            // Support 5'10 or 5 10 or 70
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
 * Handles permission checks and reading the latest height/weight.
 */
class HealthConnectProfile(private val context: Context) {

    private val client by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun sdkStatus() = HealthConnectClient.getSdkStatus(context)

    suspend fun hasCoreReadPermissions(): Boolean {
        return client?.permissionController?.getGrantedPermissions()
            ?.containsAll(permissions) == true
    }

    /** Returns true if *all* requested permissions are granted. */
    suspend fun hasAllPermissions(): Boolean {
        return client?.permissionController?.getGrantedPermissions()
            ?.containsAll(permissions) == true
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
        val weight = c.readRecords(
            ReadRecordsRequest(
                WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(monthAgo, now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()?.weight?.inKilograms

        return BodyProfile(
            bodyWeightKg = weight,
            heightM = height,
            dateOfBirthEpoch = null,
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
