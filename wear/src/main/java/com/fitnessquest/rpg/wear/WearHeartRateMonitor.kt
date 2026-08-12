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

/**
 * Streams heart rate via the platform HR sensor while a phone workout is active.
 * Pixel Watch 4 / Wear OS 6 requires [android.permission.health.READ_HEART_RATE].
 */
class WearHeartRateMonitor(
    context: Context,
    private val onBpm: (Int) -> Unit,
    private val onStatus: (String) -> Unit = {}
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            ?: sensorManager.getSensorList(Sensor.TYPE_HEART_RATE).firstOrNull()
    private var running = false
    private var lastEmitAt = 0L

    fun hasPermission(): Boolean {
        val permission = requiredPermission()
        return ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if (running) return
        if (!hasPermission()) {
            onStatus("Allow heart rate access")
            Log.w(TAG, "Missing ${requiredPermission()}")
            return
        }
        val sensor = heartSensor
        if (sensor == null) {
            onStatus("No HR sensor")
            Log.w(TAG, "No TYPE_HEART_RATE sensor")
            return
        }
        running = true
        onStatus("Reading HR…")
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        Log.i(TAG, "HR listener registered (${sensor.name})")
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_HEART_RATE) return
        val bpm = event.values.firstOrNull()?.toInt() ?: return
        if (bpm <= 0) return
        val now = System.currentTimeMillis()
        if (now - lastEmitAt < 800) return
        lastEmitAt = now
        onBpm(bpm)
        onStatus("♥ $bpm")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "WearHR"

        fun requiredPermission(): String =
            if (Build.VERSION.SDK_INT >= 36) {
                "android.permission.health.READ_HEART_RATE"
            } else {
                Manifest.permission.BODY_SENSORS
            }
    }
}
