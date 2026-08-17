package com.fitnessquest.rpg.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OutboxWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), OutboxProcessor {

    override suspend fun processEvent(event: PendingSyncEntity): OutboxSyncResult {
        val app = applicationContext as? FitQuestApp ?: return OutboxSyncResult.RETRYABLE_FAILURE
        val container = app.container

        return try {
            when (event.type) {
                "PARTY_XP" -> {
                    val json = JSONObject(event.payloadJson)
                    val xp = json.optInt("xp", 0)
                    val partyId = json.optString("partyId", "")
                    val uid = json.optString("uid", "")
                    if (xp > 0) {
                        container.party.reportSessionXp(
                            xp = xp,
                            eventId = event.eventId,
                            targetPartyId = partyId.ifEmpty { null },
                            targetUid = uid.ifEmpty { null }
                        )
                    } else OutboxSyncResult.NOT_APPLICABLE
                }
                "GUILD_XP" -> {
                    val json = JSONObject(event.payloadJson)
                    val xp = json.optInt("xp", 0)
                    val guildId = json.optString("guildId", "")
                    val uid = json.optString("uid", "")
                    if (xp > 0) {
                        container.guild.reportSessionXp(
                            xp = xp,
                            eventId = event.eventId,
                            targetGuildId = guildId.ifEmpty { null },
                            targetUid = uid.ifEmpty { null }
                        )
                    } else OutboxSyncResult.NOT_APPLICABLE
                }
                else -> OutboxSyncResult.NOT_APPLICABLE
            }
        } catch (e: Exception) {
            OutboxSyncResult.RETRYABLE_FAILURE
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? FitQuestApp ?: return Result.failure()
        val container = app.container
        val dao = container.database.activeSessionDao()

        val pending = dao.getPendingOutboxEvents()
        if (pending.isEmpty()) return Result.success()

        var hasFailures = false

        for (event in pending) {
            val syncResult = processEvent(event)
            when (syncResult) {
                OutboxSyncResult.DELIVERED,
                OutboxSyncResult.ALREADY_PROCESSED,
                OutboxSyncResult.NOT_APPLICABLE -> {
                    dao.markOutboxEventSent(event.eventId)
                }
                OutboxSyncResult.RETRYABLE_FAILURE -> {
                    dao.incrementOutboxEventRetry(event.eventId)
                    hasFailures = true
                }
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
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
