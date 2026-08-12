package com.fitnessquest.rpg.ui.effects

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Tactical Haptic Feedback Manager for FitnessRPG.
 * Provides distinct physical sensations for workouts, crits, level-ups, and rewards.
 */
object HapticEffects {

    var hapticsEnabled: Boolean = true

    /** Performs a crisp tap sensation for standard actions like set logging */
    fun performSetLogged(haptic: HapticFeedback?, context: Context? = null) {
        if (!hapticsEnabled) return
        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        vibratePattern(context, longArrayOf(0, 35), intArrayOf(0, 180))
    }

    /** Heavy double-pulse sensation when leveling up */
    fun performLevelUp(haptic: HapticFeedback?, context: Context? = null) {
        if (!hapticsEnabled) return
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        vibratePattern(context, longArrayOf(0, 70, 50, 140), intArrayOf(0, 255, 0, 255))
    }

    /** Powerful thud sensation for critical hits */
    fun performCritHit(haptic: HapticFeedback?, context: Context? = null) {
        if (!hapticsEnabled) return
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        vibratePattern(context, longArrayOf(0, 90), intArrayOf(0, 255))
    }

    /** Rhythmic alert pulse for rest timer completion */
    fun performRestDone(haptic: HapticFeedback?, context: Context? = null) {
        if (!hapticsEnabled) return
        vibratePattern(context, longArrayOf(0, 100, 80, 100), intArrayOf(0, 200, 0, 200))
    }

    private fun vibratePattern(context: Context?, timings: LongArray, amplitudes: IntArray) {
        if (context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(timings, -1)
                }
            }
        } catch (_: Exception) {
            // Ignore missing permissions or unsupported hardware safely
        }
    }
}
