package com.fitnessquest.rpg.data.sync

import com.fitnessquest.rpg.data.db.PendingSyncEntity

interface OutboxProcessor {
    suspend fun processEvent(event: PendingSyncEntity): OutboxSyncResult
}
