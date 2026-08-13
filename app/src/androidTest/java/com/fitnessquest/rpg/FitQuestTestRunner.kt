package com.fitnessquest.rpg

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Dedicated [AndroidJUnitRunner] that initializes [FitQuestTestApp] instead of standard [FitQuestApp].
 *
 * PURPOSE & ISOLATION:
 * By running tests under [FitQuestTestApp], background services, Firebase Crashlytics, and credential-protected
 * SharedPreferences storage initialization in [FitQuestApp.onCreate] are bypassed, ensuring database and unit
 * instrumented tests run cleanly without storage lock exceptions on locked or headless emulators.
 *
 * FUTURE UI TEST ARCHITECTURE NOTE:
 * Because [testInstrumentationRunner] in `build.gradle.kts` points to [FitQuestTestRunner], all instrumentation
 * tests will execute under [FitQuestTestApp] (where `FitQuestApp.container` is not initialized by default).
 * If future UI tests require `FitQuestApp.container` or full Application DI container initialization, they must
 * either explicitly initialize an [AppContainer] instance in `@Before` setup or configure a dedicated UI test runner / rule.
 */
class FitQuestTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, FitQuestTestApp::class.java.name, context)
    }
}
