# FitnessQuestRPG Workspace Guidelines

FitnessQuestRPG is a Kotlin Android fitness RPG. It turns real-world workouts into character progression, quests, loot, battles, and long-term player goals.

## Project Modules

- `app/` — Main Android phone application
- `wear/` — Wear OS companion application
- `shared/` — Shared models, domain logic, utilities, and cross-platform code
- `local_ai_model/` — Local AI/model-related assets or integrations
- `docs/` — Product, technical, and design documentation
- `scripts/` — Development and release automation
- `play-store-assets/` — Store listing and publishing assets
- `version.properties` — Source of truth for versioning

## Core Development Principles

- Prefer small, focused changes that solve the requested problem without unrelated refactors.
- Preserve existing architecture, naming patterns, and module boundaries before introducing a new pattern.
- Put reusable business logic, models, and validation in `shared/` when both phone and Wear OS can use it.
- Keep Android UI, platform APIs, and Android-specific resources inside their respective application modules.
- Do not duplicate domain logic between `app/` and `wear/`.
- Favor explicit, testable Kotlin over clever abstractions.
- Avoid breaking changes to persisted user data, Firebase documents, preferences, or serialized models unless a migration is included.

## Kotlin Standards

- Use Kotlin idioms: immutable `val` by default, data classes for value models, sealed interfaces/classes for finite UI or domain states, and extension functions only when they improve discoverability.
- Avoid `!!`. Handle nullable values explicitly.
- Use coroutines for asynchronous work. Do not block the main thread.
- Keep `ViewModel`s free of `Activity`, `Fragment`, `Context`, and composable references.
- Expose UI state as a single immutable state model when practical.
- Model loading, success, empty, and failure states deliberately; do not leave the UI in an ambiguous state.
- Do not swallow exceptions. Log useful context and surface recoverable errors through UI state or domain results.
- Prefer constructor injection over service locators or hidden global dependencies.

## Jetpack Compose Standards

- Keep composables focused on rendering and user events.
- Hoist state when a child composable does not exclusively own it.
- Pass state and event callbacks into reusable composables rather than passing `ViewModel`s deeply through the UI tree.
- Use stable keys for dynamic lists.
- Avoid expensive calculations, database reads, network calls, and mutation directly in composable bodies.
- Use `remember`, `derivedStateOf`, `LaunchedEffect`, and `DisposableEffect` only when their lifecycle behavior is intentional.
- Preserve accessibility: meaningful content descriptions, readable text scaling, adequate touch targets, and non-color-only status indicators.
- Reuse the app’s established theme, typography, colors, spacing, and component patterns before creating new UI primitives.

## Fitness RPG Domain Rules

- Treat workout history, XP, levels, streaks, inventory, quests, achievements, and battle outcomes as player data that must not be silently lost or reset.
- Make progression calculations deterministic and testable.
- Centralize XP, leveling, rewards, stat calculations, cooldowns, and quest-completion rules. Do not reimplement formulas in UI code.
- Prevent duplicate rewards from repeated taps, recomposition, retries, app restarts, or offline synchronization.
- Validate user-entered workout values, dates, durations, weights, reps, and exercise data.
- When changing a progression formula, identify whether existing player data needs a migration, recalculation, or versioned compatibility behavior.
- Prefer clear user-facing language over game mechanics that obscure the fitness action being recorded.

## Firebase and Data Safety

- Never commit API keys, service-account JSON, tokens, keystores, passwords, or local configuration files.
- Follow the repository’s Firebase configuration and Firestore security rules. Do not weaken rules merely to make a feature work.
- Ensure Firebase reads and writes handle loading, offline, permission-denied, and network-failure states.
- Use stable document identifiers and idempotent write patterns where duplicate processing could grant duplicate rewards.
- Before changing Firestore schemas, search for every reader and writer of the affected fields.
- Preserve backward compatibility for existing documents whenever possible. If not possible, add a migration strategy and document it.

## Wear OS Rules

- Keep Wear experiences concise, glanceable, and useful during workouts.
- Design for small screens, quick interactions, and intermittent connectivity.
- Reuse shared models and domain logic from `shared/`; keep Wear-specific navigation and presentation in `wear/`.
- Verify that changes affecting shared fitness data do not break phone–watch synchronization or create duplicate activity/reward events.

## Testing and Verification

Before marking work complete:

1. Build the module(s) changed.
2. Run relevant unit tests and instrumented/UI tests when available.
3. Verify the affected user flow manually when UI, persistence, Firebase, or reward behavior changes.
4. Confirm compilation after changes to shared APIs, Gradle configuration, dependencies, serialization, or resources.
5. Check for regressions in both `app` and `wear` when modifying `shared/`.

Use the smallest appropriate command first:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :wear:assembleDebug
.\gradlew.bat test
```

For a broad pre-merge verification, use:

```powershell
.\gradlew.bat build
```

Do not claim tests, builds, device verification, Firebase verification, or Play upload succeeded unless they were actually run and their result is known.

## Change Workflow

1. Inspect the relevant files and existing patterns before editing.
2. State the intended approach briefly when the change affects multiple files, architecture, persistence, progression, or Firebase.
3. Implement the smallest coherent change.
4. Add or update tests for business rules, regressions, and bug fixes when practical.
5. Run relevant verification commands.
6. Report:
    - What changed
    - Which files/modules changed
    - Verification performed and results
    - Known limitations, follow-ups, or unverified areas

## Dependency and Gradle Rules

- Do not add a dependency when AndroidX, Kotlin, Compose, Firebase, or an existing dependency already provides the capability.
- Keep dependency versions centralized according to the existing Gradle setup.
- Avoid alpha, beta, RC, or snapshot dependencies unless explicitly requested.
- Do not alter signing configuration, release configuration, application IDs, package names, ProGuard/R8 behavior, or versioning logic without explicit user approval.
- When changing Gradle files, run a relevant Gradle build before completion.

## Play Store & Play Testing Release Build Rule

Whenever preparing or building a bundle for Google Play Store upload, Play Testing, internal testing, closed testing, open testing, production, or any release upload, ALWAYS execute:

```powershell
.\gradlew.bat bumpReleaseVersion :app:bundleRelease :wear:bundleRelease collectReleases
```

This command MUST be used whenever the user requests:

- “Build for Play Store”
- “Build for Play Testing”
- “Build for Google Play”
- “Release build”
- “Production build”
- “Internal testing build”
- Any variation of preparing a signed upload bundle

This command:

1. Automatically increments the release version code in `version.properties`
2. Builds signed Android App Bundles for phone and Wear OS:
    - `:app:bundleRelease`
    - `:wear:bundleRelease`
3. Collects `.aab` artifacts in a timestamped directory:
    - `releases/yyyy-MM-dd_HH-mm/`

Do not substitute debug APKs, individual bundle tasks, or manually edited version codes for this release workflow.

## Publishing for Testing

After the mandatory release command succeeds, Android Studio may be used to upload the generated bundle through **Generate Signed App Bundle** → **Publish for Testing**.

Before any publishing action:

- Confirm which Play Console track is intended: internal, closed, open, or production.
- Confirm the selected bundle is from the newly created timestamped `releases/` directory.
- Verify the generated version code is higher than the version currently active on the target track.
- Review release notes, tester targeting, and rollout settings.
- Treat uploading, rollout changes, and production publishing as explicit user-approved external actions.

## Git and Safety

- Never force-push, rewrite shared history, delete branches, delete releases, or discard uncommitted work unless explicitly instructed.
- Do not commit generated build outputs, keystores, local properties, secrets, or machine-specific files.
- Keep commits narrowly scoped and describe the user-visible or technical purpose.
- Before changing files outside the requested feature area, explain why the change is necessary.