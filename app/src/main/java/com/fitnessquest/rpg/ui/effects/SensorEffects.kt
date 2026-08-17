package com.fitnessquest.rpg.ui.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.math.abs

/**
 * Provides the current device tilt as an Offset(x, y) where values typically range from -1f to 1f.
 * Uses the gravity or accelerometer sensor to calculate device pitch and roll,
 * which drives 2.5D parallax effects.
 */
@Composable
fun rememberDeviceTilt(): State<Offset> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(Offset.Zero) }
    val isPreview = LocalInspectionMode.current

    DisposableEffect(context, isPreview) {
        if (isPreview) {
            return@DisposableEffect onDispose {}
        }
        
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            return@DisposableEffect onDispose {}
        }

        // TYPE_GRAVITY is best for tilt without linear acceleration noise, but fallback to ACCELEROMETER.
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastX = 0f
        var lastY = 0f
        val smoothing = 0.15f // Low pass filter for smooth parallax

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                
                // For a device held in portrait:
                // event.values[0] is X-axis (left/right tilt)
                // event.values[1] is Y-axis (up/down tilt)
                // Values typically range from -9.8 to 9.8. Normalize roughly to -1f to 1f.
                
                val rawX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)
                
                lastX = lastX + smoothing * (rawX - lastX)
                lastY = lastY + smoothing * (rawY - lastY)
                
                // X tilt moves left/right. 
                // Y tilt (pitch) naturally rests around 0.5 when holding the phone.
                // We offset Y by ~0.5 to center the neutral holding position.
                val normalizedY = (lastY - 0.5f).coerceIn(-1f, 1f)
                
                tiltState.value = Offset(lastX, normalizedY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            // SENSOR_DELAY_GAME is ~20ms, perfect for UI animations.
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}
