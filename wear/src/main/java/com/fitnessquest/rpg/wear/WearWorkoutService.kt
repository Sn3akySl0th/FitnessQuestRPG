package com.fitnessquest.rpg.wear

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fitnessquest.shared.wear.WearLiveMetrics
import com.fitnessquest.shared.wear.WearPaths
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Foreground service that manages the WearExerciseEngine to ensure continuous sensor
 * tracking even when the watch screen is off or the app UI is backgrounded.
 */
class WearWorkoutService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    private var engine: WearExerciseEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val _metrics = MutableStateFlow(WearLiveMetrics())
    val metrics: StateFlow<WearLiveMetrics> = _metrics

    inner class LocalBinder : Binder() {
        fun getService(): WearWorkoutService = this@WearWorkoutService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun startWorkout() {
        if (engine != null) return
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FitQuest:WorkoutWakeLock").apply {
            acquire(120 * 60 * 1000L) // 2 hours max
        }

        engine = WearExerciseEngine(
            context = this,
            onMetrics = { m ->
                _metrics.value = m
                updateNotification(m)
                scope.launch { sendMetricsToPhone(m) }
            },
            onGoal = { kind, msg ->
                // Goals could be broadcast here or handled via state
            },
            onStatus = { status ->
                Log.d("WearWorkoutService", "Engine status: $status")
            }
        )
        
        engine?.start()
        
        val notification = createNotification(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun stopWorkout() {
        engine?.stop()
        engine = null
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    fun onRestChanged(endsAt: Long?) {
        engine?.onRestChanged(endsAt)
    }

    fun setMaxHr(maxHr: Int) {
        engine?.setMaxHr(maxHr)
    }

    private suspend fun sendMetricsToPhone(m: WearLiveMetrics) {
        try {
            val nodeClient = Wearable.getNodeClient(this)
            val messageClient = Wearable.getMessageClient(this)
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, WearPaths.METRICS, m.toJson()).await()
            }
        } catch (e: Exception) {
            Log.e("WearWorkoutService", "Failed to send metrics", e)
        }
    }

    private fun updateNotification(m: WearLiveMetrics) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(m))
    }

    private fun createNotification(m: WearLiveMetrics?): Notification {
        val intent = Intent(this, WearMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (m?.bpm != null) {
            "Heart Rate: ${m.bpm} bpm | ${m.zone ?: "Tracking"}"
        } else {
            "Tracking your quest..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Active Quest")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_media_play)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        engine?.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 888
        private const val CHANNEL_ID = "wear_workout_tracking"
    }
}
