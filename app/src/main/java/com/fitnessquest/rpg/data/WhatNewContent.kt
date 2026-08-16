package com.fitnessquest.rpg.data

data class ChangeLog(
    val version: String,
    val date: String,
    val changes: List<String>
)

object WhatNewContent {
    /**
     * Recent changelogs. The UI will show the newest one if the user hasn't seen it yet.
     */
    val releases = listOf(
        ChangeLog(
            version = "1.0.6",
            date = "August 2026",
            changes = listOf(
                "Training Quest Sync: Your custom workouts now sync correctly across devices.",
                "History Deletion: You can now delete duplicate or unwanted sessions from your Chronicle. Note: XP and Gold earned from deleted sessions will be reverted.",
                "Orphan Exercise Cleanup: Fixed a bug where workouts would sometimes gain 'random' exercises from deleted or failed syncs.",
                "Smarter CSV Imports: Improved routine template generation when importing from Hevy CSV."
            )
        ),
        ChangeLog(
            version = "1.0.3",
            date = "July 2026",
            changes = listOf(
                "In-App Updates: You can now update the app directly from within the game.",
                "Local AI: Initial support for Local Gemini models for workout generation.",
                "Hero Status Widget: Added a home screen widget to track your hero's progress."
            )
        )
    )

    fun getLatest(): ChangeLog? = releases.firstOrNull()
}
