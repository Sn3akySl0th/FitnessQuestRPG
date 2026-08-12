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
