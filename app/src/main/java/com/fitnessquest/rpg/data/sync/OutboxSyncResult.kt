package com.fitnessquest.rpg.data.sync

enum class OutboxSyncResult {
    /** Successfully processed and delivered to remote target. */
    DELIVERED,
    /** Event was already processed remotely (idempotent duplicate skip). */
    ALREADY_PROCESSED,
    /** User is not in a party/guild or feature is not applicable (complete no-op). */
    NOT_APPLICABLE,
    /** Transient failure (network/server error) requiring retry. */
    RETRYABLE_FAILURE
}
