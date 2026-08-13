package com.fitnessquest.rpg

import android.app.Application

/**
 * Dedicated Application class for instrumented tests.
 * Prevents production startup initialization (FirebaseCrashlytics, AppContainer, SharedPreferences)
 * from crashing test runner startup on locked or headless emulators.
 */
class FitQuestTestApp : Application()
