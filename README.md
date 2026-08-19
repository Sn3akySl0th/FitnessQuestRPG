# FitnessRPG

A fitness tracker that's also an RPG. Every real-world workout earns your hero XP, gold, and
stat gains. Spend energy from training on turn-based monster battles, buy and equip gear,
and let Google Gemini forge workouts and narrate your victories.

## How the game works

| Real world | In game |
| --- | --- |
| Lifting weights (volume = weight x reps) | XP + **STR** (Strength) |
| Cardio (minutes + distance) | XP + **END** (Endurance) |
| Bodyweight / calisthenics (reps) | XP + **AGI** (Agility) |
| Stretching / yoga / mobility (minutes) | XP + **WIL** (Willpower) |

- On first launch you pick a **class** — Warrior, Mage, Thief, or Ranger. Each has a unique
  look, a starting stat bonus, different combat scaling (a Mage's attack grows with Willpower,
  a Thief crits constantly), and a signature battle skill. You can respec later in settings.
- Your hero is rendered as a layered avatar with **7 equipment slots**: weapon, head, chest,
  hands, legs, feet, and trinket. Every piece shows on the avatar — helmets recolor your
  headgear, gauntlets/greaves/boots tint by tier, weapons appear in hand, trinkets glow.
- Each class has themed **armor sets** at three tiers (e.g. Ironbound → Steelwrought →
  Titanforged for Warriors). Wearing all 5 armor pieces of your class grants a
  **set bonus**: +10% ATK and HP, +3 DEF.
- The world has **6 biomes** — Meadowlands, Darkwood, Crystal Caves, Ember Peaks,
  Frozen Wastes, Shadowfen — each with its own monsters and a level requirement.
  **Traveling between biomes takes real cardio**: pick a destination on the Battle tab,
  then run/cycle/row the kilometers (timed cardio counts 1 km per 10 minutes).
- A **rest timer** starts automatically after each logged set, with 60/90/120s presets,
  +30s, and skip.
- **XP** levels up your hero. **Gold** is earned from workouts and battle victories.
- **Energy** is earned only by working out and is spent on battles (10 per fight) —
  you literally cannot grind monsters without training.
- Stats feed combat power: STR → attack, END → HP, AGI → defense/speed/crit,
  WIL → your Focus Strike skill and defensive recovery.
- Battles are interactive turn-based fights against 18 monsters: Attack, class skill
  (cooldown), Defend, or Flee. Victories award gold and XP, with a 20% chance of an
  equipment drop.
- The **Shop** sells 89 pieces of gear across all seven slots, filterable by class.
- **Gear Reforging & Combat Traits**: Reroll procedural traits (e.g. Vampiric, Berserk, Thorns, Executioner) on Rare+ items using gold and materials. Tier 2+ gear supports socketing power runes for custom combat enhancements.
- **Glamour System**: Customize your hero's paper-doll appearance independently of your equipped stat gear from the Hero screen.
- **Hero Objective Tracker**: Context-aware next-objective card directs heroes to unallocated stat points, workout milestones, or boss battles.

## AI features (Cloud Gemini & On-Device Local AI)

- **AI Forge** (Train tab): describe a workout in plain language — "45 min upper body with dumbbells" — and AI generates a complete workout you can save and start.
- **Battle narration**: dramatic epilogues after each fight.
- **Guildmaster praise**: flavor text after finishing a workout.

### AI Setup Options

1. **Cloud Gemini API**: Enter your free Gemini API key in **Settings** (Hero tab → gear icon → "Gemini API key").
2. **On-Device Local AI (100% Offline & Uncapped)**: Download local model weights (Lite Qwen 0.5B, Llama 3.2 1B, Gemma 2B, Qwen 2.5 1.5B) or paste a custom GGUF link in **Settings** → **🤖 On-Device Local AI Engine**.
   - Includes **Screen Wake Locks** and **HTTP Range Resumable Downloads** so screen dimming never aborts downloads.

The app works fully offline and without any API key — automatically falling back to the local AI engine!

For release packaging, runtime diagnostics, and recovery steps for the optional Play-delivered model, see [Local AI and Play Asset Delivery](docs/LOCAL_AI_AND_PLAY_ASSET_DELIVERY.md).

## Layout & Accessibility Features

- **Responsive Landscape Mode**: Converts bottom dock to a sleek **Left Navigation Rail** and transforms Hero Screen into a **2-Column Split View** (Left: Full Avatar & Currency; Right: Stats, Gear & Saga tabs with independent scrolling).
- **Dynamic Username Auto-Scaling**: Automatically scales username font size and line height so long names (e.g. `Sn3akySloth`) render completely without `...` truncation.

## Building and running

Requirements: Android Studio (with Android SDK), device or emulator running Android 8.0+ (API 26).

- **From Android Studio**: open this project folder, let Gradle sync, press Run.
- **From the command line**:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. To install directly to a
plugged-in phone with USB debugging enabled: `.\gradlew.bat :app:installDebug`.

### Running Tests

```powershell
# Run active session unit tests
.\gradlew.bat :app:testDebugUnitTest --tests "com.fitnessquest.rpg.ActiveSessionTest" --no-configuration-cache

# Run database migration matrix & persistence instrumented tests on an attached device
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.fitnessquest.rpg.data.db.AppDatabaseMigrationTest,com.fitnessquest.rpg.data.db.AppDatabasePersistenceTest" --no-configuration-cache
```

## Tech stack

- Kotlin + Jetpack Compose (Material 3, dark fantasy theme), single-activity, Compose Navigation
- Room database (Schema v30, offline-first, no account or server needed)
- Gemini REST API (`gemini-2.5-flash`) via OkHttp
- AGP 9.2 with built-in Kotlin, Gradle 9.4.1

## Project layout

```
app/src/main/java/com/fitquest/app/
├── FitQuestApp.kt          # Application + dependency container
├── MainActivity.kt
├── data/
│   ├── db/                 # Room entities, DAOs, database
│   ├── ai/GeminiService.kt # Gemini REST client + prompt building
│   └── GameRepository.kt   # Single source of truth for all game actions
├── domain/
│   ├── GameMath.kt         # XP curves, stat thresholds, combat stat formulas
│   ├── BattleEngine.kt     # Pure turn-based battle logic
│   └── GameContent.kt      # Exercise catalog, item catalog, monster roster
└── ui/
    ├── Navigation.kt       # Bottom tabs: Hero / Train / Battle / Shop
    ├── components/         # Shared UI pieces
    └── screens/            # One file per screen (+ its ViewModel)
```
