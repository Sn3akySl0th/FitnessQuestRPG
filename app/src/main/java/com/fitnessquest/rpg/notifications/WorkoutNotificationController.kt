package com.fitnessquest.rpg.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fitnessquest.rpg.MainActivity
import com.fitnessquest.rpg.R
import com.fitnessquest.rpg.ui.screens.ActiveSessionUiState
import android.text.format.DateFormat
import java.util.Date

class WorkoutNotificationController(private val context: Context) {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    fun update(state: ActiveSessionUiState, startedAt: Long) {
        if (!canPostNotifications()) return
        if (state.loading || state.finish != null || state.isDemo) {
            cancel()
            return
        }

        ensureChannel()
        val current = state.exercises.getOrNull(
            state.currentExerciseIndex.coerceIn(0, (state.exercises.size - 1).coerceAtLeast(0))
        )
        val plannedSets = state.exercises.sumOf { it.targetSets }.coerceAtLeast(1)
        val totalSets = state.totalSets
        val heart = state.heartRateBpm?.let { bpm ->
            val zone = state.hrZoneLabel?.takeIf { it.isNotBlank() } ?: "Live"
            "$bpm bpm · $zone"
        } ?: if (state.watchLinked) {
            "Waiting for HR"
        } else {
            "Watch offline"
        }
        val restLine = state.restEndsAt?.let { restEnds ->
            val time = DateFormat.getTimeFormat(appContext).format(Date(restEnds))
            "Rest until $time"
        }
        val exerciseLine = current?.let {
            "${it.name} · ${it.loggedSets.size}/${it.targetSets} sets"
        } ?: "Choose your next exercise"
        val detailLines = listOfNotNull(
            exerciseLine,
            heart,
            restLine,
            if (state.heatStreak > 1) "Heat streak x${state.heatStreak}" else null
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_workout)
            .setContentTitle(state.title.ifBlank { "Workout in progress" })
            .setContentText("$exerciseLine · $heart")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailLines.joinToString("\n")))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(startedAt)
            .setUsesChronometer(true)
            .setProgress(plannedSets, totalSets.coerceAtMost(plannedSets), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission can be revoked between the check and this call.
        }
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active workout",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Current workout, set progress, and live heart rate."
            setShowBadge(false)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            20,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postSideQuestNotification(title: String, description: String) {
        if (!canPostNotifications()) return
        ensureSideQuestChannel()
        val builder = NotificationCompat.Builder(appContext, SIDE_QUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚔️ Side Quest: $title")
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())

        try {
            manager.notify(SIDE_QUEST_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun ensureSideQuestChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            SIDE_QUEST_CHANNEL_ID,
            "Side Quests & Recovery Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for random micro side quests and recovery buffs."
            setShowBadge(true)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "active_workout"
        const val SIDE_QUEST_CHANNEL_ID = "side_quests"
        const val NOTIFICATION_ID = 4201
        const val SIDE_QUEST_NOTIFICATION_ID = 4202
    }
}
