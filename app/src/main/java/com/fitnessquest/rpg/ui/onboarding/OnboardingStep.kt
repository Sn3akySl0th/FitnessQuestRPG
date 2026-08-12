package com.fitnessquest.rpg.ui.onboarding

/**
 * Ordered Play onboarding steps. Persisted by name in [com.fitnessquest.rpg.data.UserPrefs].
 */
enum class OnboardingStep {
    WELCOME,
    INTRO,
    UNITS,
    HEALTH_CONNECT,
    GENDER,
    DOB,
    HEIGHT,
    WEIGHT,
    GOAL,
    EXPERIENCE,
    FREQUENCY,
    CARDIO,
    DURATION,
    FOCUS,
    GYM,
    EQUIPMENT,
    TERMS,
    PROGRAM,
    FIRST_WORKOUT,
    REWARDS,
    NOTIFICATIONS,
    STEPS,
    WEAR,
    CLASS,
    USERNAME,
    ACCOUNT_PITCH,
    ACCOUNT,
    RETURNING_SIGN_IN;

    companion object {
        fun fromName(name: String?): OnboardingStep? =
            name?.let { n -> entries.find { it.name == n } }
    }
}
