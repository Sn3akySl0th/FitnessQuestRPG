package com.fitnessquest.rpg.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitnessquest.rpg.FitQuestApp
import org.json.JSONObject

class OutboxWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FitQuestApp ?: return Result.failure()
        val container = app.container
        val dao = container.database.activeSessionDao()

        val pending = dao.getPendingOutboxEvents()
        if (pending.isEmpty()) return Result.success()

        var hasFailures = false

        for (event in pending) {
            try {
                when (event.type) {
                    "PARTY_XP" -> {
                        val json = JSONObject(event.payloadJson)
                        val xp = json.optInt("xp", 0)
                        if (xp > 0) {
                            container.party.reportSessionXp(xp)
                        }
                        dao.markOutboxEventSent(event.eventId)
                    }
                    "GUILD_XP" -> {
                        val json = JSONObject(event.payloadJson)
                        val xp = json.optInt("xp", 0)
                        if (xp > 0) {
                            container.guild.reportSessionXp(xp)
                        }
                        dao.markOutboxEventSent(event.eventId)
                    }
                    else -> {
                        // Unknown event type, mark sent to prevent infinite loop
                        dao.markOutboxEventSent(event.eventId)
                    }
                }
            } catch (e: Exception) {
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "fitquest_outbox_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
