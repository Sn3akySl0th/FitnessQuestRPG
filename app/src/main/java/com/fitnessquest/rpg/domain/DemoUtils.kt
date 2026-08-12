package com.fitnessquest.rpg.domain

/**
 * Checks if a hero name belongs to a demo/test account, screenshot profile, or Google reviewer.
 * Demo users should not be published to public leaderboards or create duplicate entries.
 */
fun isDemoUser(name: String?): Boolean {
    if (name.isNullOrBlank()) return false
    val trimmed = name.trim().lowercase()
    return trimmed == "demo" ||
            trimmed.startsWith("demo ") ||
            trimmed.startsWith("demo_") ||
            trimmed.startsWith("demo-") ||
            trimmed.endsWith(" demo") ||
            trimmed == "demo user" ||
            trimmed == "demo hero" ||
            trimmed == "demouser" ||
            trimmed == "demohero" ||
            trimmed == "test user" ||
            trimmed == "test hero" ||
            trimmed == "reviewer" ||
            trimmed.startsWith("test_") ||
            trimmed.startsWith("test-")
}
