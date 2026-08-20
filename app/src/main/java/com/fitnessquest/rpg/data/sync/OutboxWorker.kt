package com.fitnessquest.rpg.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.data.db.PendingSyncEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OutboxWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), OutboxProcessor {

    override suspend fun processEvent(event: PendingSyncEntity): OutboxSyncResult {
        val app = applicationContext as? FitQuestApp ?: return OutboxSyncResult.RETRYABLE_FAILURE
        return processSingleEvent(app.container, event)
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? FitQuestApp ?: return Result.failure()
        val success = syncNow(app)
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "OutboxWorker"
        private const val WORK_NAME = "fitquest_outbox_sync"

        suspend fun processSingleEvent(container: AppContainer, event: PendingSyncEntity): OutboxSyncResult {
            return try {
                when (event.type) {
                    "PARTY_XP" -> {
                        val json = JSONObject(event.payloadJson)
                        val xp = json.optInt("xp", 0)
                        val partyId = json.optString("partyId", "")
                        val uid = json.optString("uid", "")
                        val isCrit = json.optBoolean("isCrit", false)
                        val authorName = json.optString("authorName", "").ifEmpty { null }
                        if (xp > 0) {
                            container.party.reportSessionXp(
                                xp = xp,
                                eventId = event.eventId,
                                targetPartyId = partyId.ifEmpty { null },
                                targetUid = uid.ifEmpty { null },
                                isCrit = isCrit,
                                authorName = authorName
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
                Log.e(TAG, "Failed to process outbox event ${event.eventId}", e)
                OutboxSyncResult.RETRYABLE_FAILURE
            }
        }

        /**
         * Flushes the pending outbox queue immediately without waiting for WorkManager schedule delays.
         */
        suspend fun syncNow(context: Context): Boolean = withContext(Dispatchers.IO) {
            val app = context.applicationContext as? FitQuestApp ?: return@withContext false
            val container = app.container
            val dao = container.database.activeSessionDao()

            val pending = dao.getPendingOutboxEvents()
            if (pending.isEmpty()) return@withContext true

            Log.d(TAG, "syncNow: Processing ${pending.size} pending outbox event(s)")
            var hasFailures = false

            for (event in pending) {
                val syncResult = processSingleEvent(container, event)
                when (syncResult) {
                    OutboxSyncResult.DELIVERED,
                    OutboxSyncResult.ALREADY_PROCESSED,
                    OutboxSyncResult.NOT_APPLICABLE -> {
                        dao.markOutboxEventSent(event.eventId)
                        Log.d(TAG, "Event ${event.eventId} marked SENT ($syncResult)")
                    }
                    OutboxSyncResult.RETRYABLE_FAILURE -> {
                        dao.incrementOutboxEventRetry(event.eventId)
                        Log.w(TAG, "Event ${event.eventId} failed, will retry later")
                        hasFailures = true
                    }
                }
            }

            !hasFailures
        }

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
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
