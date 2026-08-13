# Crash Reporting

FitQuest uses Firebase Crashlytics for release crash reports.

## Firebase Console

Crash reports appear in:

Firebase Console -> Crashlytics -> `com.fitnessquest.rpg`

Crashlytics is most useful after a build containing the Crashlytics SDK has been installed by testers. Launch crashes from older builds still need ADB or Play Console Android Vitals.

## Capture A Launch Crash With ADB

Use this when a tester cannot open the app after an update.

```powershell
adb logcat -c
adb shell monkey -p com.fitnessquest.rpg 1
adb logcat -d -t 2000 AndroidRuntime:E FirebaseCrashlytics:D ActivityTaskManager:I *:S
```

For a fuller crash file:

```powershell
adb logcat -c
adb shell monkey -p com.fitnessquest.rpg 1
adb logcat -d -t 5000 > fitquest-crash-log.txt
```

Look for:

- `FATAL EXCEPTION`
- `Caused by:`
- `Room cannot verify the data integrity`
- `SQLiteException`
- `ClassNotFoundException`
- `UnsatisfiedLinkError`
- `Resources$NotFoundException`

## Common Launch-Crash Causes After Update

- Room migration mismatch after a database schema change.
- Missing or incompatible native library on a specific device ABI.
- Bad resource reference after shrink/minify.
- Firebase or Google Services config mismatch.
- Startup work throwing before the first screen is shown.

## Database Migration & Restored-Database Crash Recovery

When Android Auto-Backup or Cloud Restore restores a database from an older app release (or a malformed legacy schema), Room may crash on startup with `IllegalStateException: Room cannot verify the data integrity` if schema migrations or expected columns are missing.

### Crash Prevention & Self-Healing Migrations
- `MIGRATION_22_23`, `MIGRATION_23_24`, and `MIGRATION_24_25` implement self-healing column repair checks (`addColumnIfNotExists`) for key tables (`workout_exercises.targetWeightKg`, `set_logs.setType`, `sessions.completionToken`, `sessions.completionReceiptJson`).
- Schema export tracking is committed in `app/schemas/com.fitnessquest.rpg.data.db.AppDatabase/25.json` starting at Version 25.

### Running Migration & Persistence Regression Suite
To test active session unit tests and migration persistence locally:

```powershell
# JVM unit tests for active sessions & outbox
.\gradlew.bat :app:testDebugUnitTest --tests "com.fitnessquest.rpg.ActiveSessionTest" --no-configuration-cache

# Instrumented database migration matrix (V1->V25 matrix, clean V25 creation, malformed restored DB repair)
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.fitnessquest.rpg.data.db.AppDatabaseMigrationTest,com.fitnessquest.rpg.data.db.AppDatabasePersistenceTest" --no-configuration-cache
```
