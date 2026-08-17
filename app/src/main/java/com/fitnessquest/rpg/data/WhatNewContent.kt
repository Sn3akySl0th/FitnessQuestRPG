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
            version = "1.0.9",
            date = "August 2026",
            changes = listOf(
                "Attribute Gain Fix: Bonus XP from Elixirs and PRs is now distributed across all active attributes (END, AGI, WIL), not just your Hero Level!",
                "Reliable XP Multipliers: Fixed a bug where Heat Streak and Ambush multipliers were being stripped away during workout completion.",
                "Calorie Estimation: Added a calorie burn estimate to the workout header that works even without a Wear OS connection.",
                "Banner Layout Polish: Fixed a UI issue where cardio distance was being 'squished' and stacked vertically on small screens."
            )
        ),
        ChangeLog(
            version = "1.0.8",
            date = "August 2026",
            changes = listOf(
                "Party Status Effects: PRs now trigger \u26A1 CRIT, high heart rate triggers \uD83D\uDD25 BURN, and rapid sets trigger \uD83E\uDE78 BLEED status symbols for your party to see!",
                "Workout Calorie Tracking: Calories burned are now displayed right next to your heart rate in the session header.",
                "Live Raid Dashboard: See exactly which status effects your teammates have active while you all raid the boss together."
            )
        ),
        ChangeLog(
            version = "1.0.7",
            date = "August 2026",
            changes = listOf(
                "Real-time Party Boss Damage: See 'Ghost Damage' from your party members strike the boss in real-time while they train!",
                "Shared Quest Management: You can now remove quests you've shared with your party.",
                "Redesigned Active Quest Banner: A sleek new look with live tracking for total Volume (kg/lb), Distance, and XP.",
                "Immersive Battle UI: Workout banners now intelligently hide during combat so they never block your Attack buttons.",
                "Localized Units: Quest tracking now correctly respects your preference for Metric or Imperial units."
            )
        ),
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
