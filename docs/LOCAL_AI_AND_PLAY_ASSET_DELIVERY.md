# Local AI And Play Asset Delivery

FitQuest keeps Local AI optional. The base app remains usable without downloading a model, while Google Play can deliver the official model through the on-demand `local_ai_model` asset pack.

## Current Architecture

- Native Kotlin/Jetpack Compose application.
- MediaPipe GenAI is the current on-device inference runtime for compatible `.bin` or `.task` models.
- `local_ai_model` is an on-demand Play Asset Delivery pack containing the official model.
- `PlayAssetModelProvider` requests the pack, observes state, locates the installed model, and exposes progress/errors to Settings.
- The direct HTTP model downloader remains available for development, sideloaded builds, and recovery.
- A future native llama.cpp/GGUF runtime may broaden model choice without migrating FitQuest to React Native.

## Release Validation Checklist

Play Asset Delivery works only when the exact installed version was published through Google Play with the asset pack attached.

1. Build Play releases with the workspace release command:

   ```powershell
   .\gradlew.bat bumpReleaseVersion :app:bundleRelease :wear:bundleRelease collectReleases
   ```

2. Upload the generated phone `.aab`, not an APK or an older bundle.
3. In Play Console, open **App bundle explorer -> version -> Delivery**.
4. Confirm `local_ai_model` appears under **Asset packs**.
5. Confirm its delivery condition is **On demand** and it is deliverable to supported devices.
6. Install/update from the tester's Google Play listing. Do not sideload an APK over the Play installation.
7. Leave at least 2 GB free and use validated Wi-Fi for the first large-model test.

Version 276 was verified in Play Console with:

- Base download: approximately 126 MB.
- `local_ai_model`: on demand, approximately 994 MB compressed.
- Runtime model transfer: approximately 1.3 GB on device.

## Verified `PACK_UNAVAILABLE` Recovery

On August 12, 2026, a Play-installed internal-test build (version 276) returned `AssetPackErrorCode.PACK_UNAVAILABLE (-2)` even though Play Console correctly listed `local_ai_model` as an on-demand pack for that same version.

The device was verified to have:

- Google Play as installer and initiating package.
- The current internal-test version.
- An enrolled internal tester account.
- More than enough storage.
- Validated unmetered Wi-Fi.

The successful recovery was:

1. Force-stop Google Play Store without clearing account/app data.
2. Open the FitQuest listing directly in Play Store:

   ```powershell
   adb shell am force-stop com.android.vending
   adb shell am start -a android.intent.action.VIEW -d "market://details?id=com.fitnessquest.rpg"
   ```

3. Wait for the listing to refresh and confirm the tester listing shows **Open**.
4. Reopen FitQuest and retry **Settings -> On-Device Local AI Engine -> Install From Play**.

After refreshing the listing, Play changed from error `-2` to status `DOWNLOADING` with error `0`. Progress was verified at 190 MB of 1.3 GB (14%). This indicates stale Play Store delivery metadata can temporarily produce `PACK_UNAVAILABLE` even when the uploaded bundle is correct.

Do not immediately rebuild, clear Play Store data, or uninstall FitQuest when `-2` appears. First verify the Delivery tab, installer, tester enrollment, storage, network, and refresh the Play listing. Uninstall/reinstall is a last resort because it can remove local data that has not synced.

## Runtime Diagnostics

Settings should display:

- Status message.
- Byte-based progress (`bytesDownloaded / totalBytesToDownload`) during download.
- Transfer percentage only during `TRANSFERRING`.
- Downloaded and total file size.
- Play status and error codes.
- A concise recovery message instead of the raw Play SDK exception URL.

Important status codes:

| Status | Meaning |
| --- | --- |
| `1` | Pending |
| `2` | Downloading |
| `3` | Transferring/installing |
| `4` | Completed |
| `5` | Failed |
| `7` | Waiting for Wi-Fi |
| `8` | Not installed |
| `9` | User confirmation required |

Important error codes:

| Error | Meaning / first response |
| --- | --- |
| `-2` | Pack unavailable. Verify Play Delivery tab, then refresh the Play listing. |
| `-6` | Network error. Verify connectivity and retry. |
| `-7` | Access denied. Check foreground state and tester Google account. |
| `-10` | Insufficient storage. Free at least 2 GB. |
| `-11` | App not owned by the current Play account. Install/acquire through Play. |
| `-13` | Unrecognized installation. Remove sideloaded build and install from Play. |

## ADB Troubleshooting

```powershell
adb devices -l
adb shell dumpsys package com.fitnessquest.rpg | Select-String -Pattern "versionCode|versionName|installerPackageName|initiatingPackageName"
adb shell df -h /data
adb shell am force-stop com.android.vending
adb shell am start -a android.intent.action.VIEW -d "market://details?id=com.fitnessquest.rpg"
```

For UI evidence:

```powershell
adb shell screencap -p /sdcard/fitquest-local-ai.png
adb pull /sdcard/fitquest-local-ai.png
```

## Product Guardrails

- Local AI must remain optional and should never block workout logging.
- Deterministic rules remain authoritative for rewards, progression, safety, PRs, equipment, and history.
- AI may suggest, summarize, rename, narrate, or explain, but important state changes require deterministic validation and user confirmation.
- Provide Quiet, Balanced, and Immersive modes plus per-feature controls as AI moments expand.
- Pause or discourage inference during low battery, thermal stress, low storage, or latency-sensitive workout interactions.
