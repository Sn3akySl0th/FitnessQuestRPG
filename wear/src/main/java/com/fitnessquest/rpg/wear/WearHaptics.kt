package com.fitnessquest.rpg.wear

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.fitnessquest.shared.wear.WearFeedbackKind

object WearHaptics {
    enum class Pattern { SET_LOGGED, REST_END, HIGH, HEAT }

    fun forKind(context: Context, kind: WearFeedbackKind) {
        when (kind) {
            WearFeedbackKind.SET_LOGGED, WearFeedbackKind.PR -> pulse(context, Pattern.SET_LOGGED)
            WearFeedbackKind.REST_END, WearFeedbackKind.RECOVERY_READY, WearFeedbackKind.GOAL_MET,
            WearFeedbackKind.CALORIES -> pulse(context, Pattern.REST_END)
            WearFeedbackKind.ZONE_HIGH, WearFeedbackKind.ZONE_RECOVER -> pulse(context, Pattern.HIGH)
            WearFeedbackKind.HEAT, WearFeedbackKind.AMBUSH -> pulse(context, Pattern.HEAT)
        }
    }

    fun pulse(context: Context, pattern: Pattern) {
        val vibrator = vibrator(context) ?: return
        val timings = when (pattern) {
            Pattern.SET_LOGGED -> longArrayOf(0, 40, 40, 40)
            Pattern.REST_END -> longArrayOf(0, 80, 60, 80)
            Pattern.HIGH -> longArrayOf(0, 120, 40, 120)
            Pattern.HEAT -> longArrayOf(0, 50, 30, 50, 30, 80)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
