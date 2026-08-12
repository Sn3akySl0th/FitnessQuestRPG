# FitQuest Workspace Guidelines

## Play Store & Play Testing Release Build Rule
Whenever preparing or building a build for Google Play Store upload or Play Testing, ALWAYS execute:
`.\gradlew.bat bumpReleaseVersion :app:bundleRelease :wear:bundleRelease collectReleases`

This command MUST be used whenever the user requests:
- "Build for Play Store"
- "Build for Play Testing"
- "Build for Google Play"
- "Release build"
- Any variation of preparing a production / test upload bundle.

What this command does:
1. Automatically increments the release version code in `version.properties`.
2. Builds signed Android App Bundles (`:app:bundleRelease` and `:wear:bundleRelease`).
3. Collects the `.aab` artifacts into a timestamped `releases/yyyy-MM-dd_HH-mm/` directory.
