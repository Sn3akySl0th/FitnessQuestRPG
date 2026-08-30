# FitQuest Public Roadmap

Last updated: August 29, 2026

FitQuest turns real workouts into RPG progress. This roadmap shows what is already in progress, what is planned next, and the larger systems we want to build over time.

## Status Key

- **Finished**: built into the app.
- **Foundation Built**: the technical base exists, but more UI or polish is still needed.
- **Working Next**: the next priority.
- **Planned**: designed, but not started yet.

## Current Focus

The next major focus is boss-gated biome progression, followed by Movement Mastery and exercise catalog trust. Reward reveals and chest opening now have a finished shared foundation; future visual-engine work will deepen their animation, sound, haptics, and rarity spectacle without blocking progression work.

## Known Issues And Fixes

### Fixed Recently

- **Procedural Loot, Loot Atlas & Named Armor Sets (v0.14.0)**: Battles, bosses, and chests now drop rolled gear with affixes, item level, and named set tags. Added in-app Loot Atlas drop guides (biome, boss, monster) with searchable set farming hints. Named 2/4/5-piece set bonuses and class affinity riders apply in combat. Warrior and Paladin shields pair with one-handed weapons; two-handed weapons block shields. Merchant shop focuses on stackables while equippable gear comes from gameplay drops.
- **Avatar V2 Overhaul (5-Piece Sets, Dyes, 3D Parallax & Flutter Physics)**: Full 5-piece head-to-toe gear visualization with custom high-definition vector layers for Legs and Boots across all tiers. Added Back slot cloaks, capes, and celestial wings with idle breathing flutter physics. Implemented 3D gyroscopic motion parallax (holographic card depth displacement) across the 14-layer Z-stack, procedural Diablo-style material dye engine (`IRON`, `GOLD`, `BLOOD_STEEL`, `GLACIAL`, `VOID`, `VERDANT`, `CELESTIAL`), and tactile spring squash-and-stretch tap recoil.
- **Paper-Doll Avatar Showcase & 14-Layer Visual Compositor**: Centered hero paper doll character showcase with dynamic pedestal backdrop, equip animation burst effects, and seamless fallback to vector avatar rendering. Full 14-layer Z-order compositing pipeline ready for custom 2D gear assets with automatic rarity tinting and visual anchors.
- **Mythic Gear Rarity Tier & High-Tier Auras**: Added Mythic tier (2.00× stats multiplier) with radiant crimson energy shimmers, glowing rune pedestals, animated gradient frames, and high-tier equipment effects.
- **Gear Traits & Combat Affixes**: Procedural combat affixes (Vampiric, Berserk, Thorns, Executioner, Swiftness, Fortified) rolled on loot drops and active during battles.
- **Merchant Shop Tier Capping**: Capped merchant store items to Tiers 1–3, ensuring Tier 4+ gear is exclusively earned through endgame boss encounters, raid caches, and forge crafting.
- **Phone vs Watch Prefilled Weight Sync & Live Cardio Stopwatch**: Resolved historical performance weight prefill desync between phone and watch so watch immediately displays the correct previous workout weight (e.g. 115 lb). Added wrist live cardio stopwatch with interactive Start/Pause/Reset controls and auto-duration capture on log.
- **Interactive Tap-to-Edit Logged Sets on Phone**: Any completed set (logged on watch or phone) can now be tapped on phone to open `EditSetDialog` and modify weight, reps, RIR/RPE, cardio distance/duration/incline/speed/program, or set type with automatic XP recalculation.
- **Wear OS Zero-Scroll Compact UI**: Redesigned Wear OS quest screen into a single glanceable layout with top status pill (Heart rate, Zone, Streak, Calories), integrated exercise switcher with completion checkmark indicators (`✓ Done — N sets`), side-by-side steppers, and single-screen reachable `LOG SET` button.
- **Ambient Hero Avatar Workout Backdrop**: Integrated watermarked Hero Avatar backdrop into phone and Wear OS active workout screens via DataLayer bitmap transfer.
- **Multi-Tier RPG Gear Rarity Scaling System**: Introduced 5 rarity tiers (Common 1.0×, Uncommon 1.10×, Rare 1.25×, Epic 1.45×, Legendary 1.70×) across drops, crafting, salvage bonuses, and reward reveals with 30-day simulation tests.
- **Birthday Validation Feedback**: Added inline calendar date validation in settings to prevent silent failures on invalid date entries.
- Google Play Automatic In-App Updates: Integrated Google Play `AppUpdateManager` with non-blocking Flexible background downloads, lifecycle hooks in `MainActivity`, and a themed floating restart banner to apply updates seamlessly without interrupting workouts.
- Boss Progression UI & First-Clear Milestone Reward Flow: Added dynamic boss gates (`BossProgressCard`), canonical biome boss assignments, readiness & trait previews, transactional `GameRepository.applyVictory` atomicity, and guaranteed milestone loot (high-tier gear, Biome Chest, crafting mats, gold/energy).
- Hevy CSV Import Fix: Fixed CSV column index disambiguation (so workout_name and exercise_name columns are detected separately) and session grouping so all exercises (e.g. Bench Press, Bent Over Row, Shoulder Press, Bicep Curl) from the same Hevy workout are correctly grouped into a single routine template & history session.
- Responsive Landscape Mode: converts bottom dock to a left side **NavigationRail** and main Hero Screen into a **2-Column Split View** (Left: Avatar & Currency; Right: Stats, Gear, and Saga tabs with independent scrolling).
- Dynamic Username Auto-Scaling: scales username font size and line height so long names (e.g. `Sn3akySloth`) render completely on-screen without `...` truncation.
- Resumable Local Model Downloader & Wake Locks: HTTP Range download resuming and screen wake lock (`FLAG_KEEP_SCREEN_ON`) prevent screen dimming from aborting model downloads.
- Public Model Mirror for Gemma 2B: un-gated mirror allows Gemma 2B to download 100% offline with zero HF tokens or API keys required.
- Google Play on-demand Local AI model was validated on a physical internal-test device with real byte/size progress and clearer recovery messages.
- Upgrade details now name the required material instead of saying only "materials."
- Gear details now clearly show which class can equip an item, or whether all classes can use it.
- Forge salvage rewards now use a forge reward label instead of looking like chest rewards.
- **Movement Mastery Perks & Dynamic Energy Caps**: Reaching level 10, 25, or 50 in any movement pattern (e.g., Squat, Running) now unlocks permanent passive perks (ATK, DEF, Max HP). Added dynamic energy caps that expand as users master low-intensity movements like walking or cycling.
- **Boss-Gated travel & Quest Hub Chapter Milestones**: Finalized backend enforcement for biome travel, requiring the previous biome boss to be defeated. Integrated "Chapter Milestones" into the Home screen Quest Hub to guide players toward boss encounters.
- **Account Isolation & Multi-UID Party Cleanup**: Implemented automatic local data wipe on logout and a background Firestore cleanup tool to remove "ghost" member entries from party/guild rosters.
- Reward reveal badges were cleaned up so labels are readable.
- Offline battle loot is now stored until claim, then applied through the reward reveal so item rewards are not silently added.
- Idle rewards no longer estimate gear from monster kills before the exact reveal.
- Party and Guild member lists now deduplicate member cards by UID and hero name, preventing duplicate roster entries across re-logins or guest account transitions.
- Member card refreshes automatically clean up old orphaned member documents from previous guest sessions.
- Demo and test accounts (e.g., "demo", "demo user", "reviewer") and developer sandbox sessions are filtered from public leaderboards and public party/guild displays.

### Known Issues

- The Hero gear slot picker has been improved from a plain list to a compact grid, but it still needs the same filtering, sorting, and detail depth as the main inventory/armory screen.
- Offline rewards still need a cleaner one-step presentation so the old pre-claim modal and exact reveal feel like one polished flow.
- Chest opening still needs a dedicated chest-first animation instead of only using the shared reward reveal.

## Recently Finished

### Smart Workout Recommendation & Recovery Engine

**Status: Finished**

FitQuest now dynamically analyzes your training split history and muscle recovery decay to recommend the optimal next workout routine.

What changed:
- **Split Sequence Engine**: Detects split history (Upper -> Lower -> Push -> Pull -> Legs) to guide your training rotation.
- **48-Hour Recovery Heatmap**: Calculates muscle freshness across all 10 major muscle groups.
- **"Today's Recommended Quest" Hero Card**: Featured at the top of the Training Grounds with readiness % and rationale.
- **Interactive Hero Anatomy Heatmap**: Front and back body views with 🟢 Green (Primed), 🟡 Yellow (Rebuilding), and 🔴 Red (Fatigued/Sore) overlays, complete with direct tap-to-flag soreness.
- **Rest Day Active Recovery Quests & "Well Rested" Buff**: Light 10-minute mobility/walking/bodyweight side quests on rest days that award Gold + a +15% XP & Gold boost on your next workout!
- **AI Random Micro Bodyweight Quests & Local Notifications**: Local phone notifications for quick bodyweight challenges on the go.
- **Avatar Wallpaper & Watch Face Direct Sync**: Full vector avatar bitmap canvas drawing with automatic Wear OS Data Layer watch face sync (`/fitnessrpg/avatar_watch_face`).

### Shop, Inventory, And Armory Refresh

**Status: Finished**

The shop and inventory have been redesigned so they feel more like an RPG armory and less like long static lists.

What changed:
- Shop, Inventory, Armory, and Forge modes.
- Grid-style item browsing.
- Filters for item type and gear slot.
- Sorting by tier, power, slot, price, and quantity.
- Equipped gear summary.
- Item detail sheets with actions like buy, equip, use, open, sell, upgrade, salvage, and fuse.

### YT Music & Media Control

**Status: Finished**

You can now control your music directly from the Active Session screen. Support for YT Music, Spotify, and other major music apps is built-in.

### Daily Weight In Quest

**Status: Finished**

A new daily bounty has been added! Log your weight each day to keep your hero's physical stats in sync and earn gold and energy rewards.

## Foundations Already Built

### Progression Pacing

**Status: Foundation Built**

Gear tiers are now limited by player progress, biome progress, boss progress, and reward source. This prevents high-tier gear from appearing too early through normal workouts.

Why it matters:
- Early gear has more time to matter.
- Tier 3+ gear becomes a milestone instead of random noise.
- Bosses and biomes can become meaningful gates for stronger rewards.

### Biome Progress

**Status: Foundation Built**

The app can now track biome progress, boss unlock state, boss defeat state, biome layer, and first-clear reward state.

Why it matters:
- Biomes can become long-term zones instead of one-time stops.
- Bosses can unlock new areas and better loot.
- Future procedural biome layers can build on this data.

### Forge And Gear Upgrades

**Status: Foundation Built**

Gear now supports upgrade levels, rarity, traits, origin biome, salvage, and upgrade actions.

Why it matters:
- Materials have a purpose.
- Duplicate gear can become useful.
- Players can invest in favorite gear instead of constantly replacing it.

## Working Next

### On-Device Local AI Model Downloader & Engine

**Status: Foundation Built**

What changed:
- Built-in **Local AI Model Manager** in Settings allowing on-demand downloads of compatible local LLM models without bloating the base APK size.
- Background chunked downloader with live progress percentage, downloaded MB tracker, resume support, wake-lock protection, and model storage management.
- FitQuest-hosted recommended model support, so future builds can point users to one official model URL with expected-size and SHA-256 validation.
- Google Play on-demand asset pack foundation, so the official offline model can be hosted by Play Store and installed only by users who want Local AI.
- Seamless 3-tier hybrid AI fallback: Cloud Gemini API -> On-Device Local Model -> Smart Heuristic Engine.
- Offline generation path to avoid API rate-limit caps when a compatible model is installed.
- Official `local_ai_model` asset pack verified in Play Console as on-demand and deliverable to supported devices.
- Physical-device download verified with actual transferred bytes and total size instead of a misleading transfer-only percentage.
- Clearer Play Store diagnostics and recovery for tester access, storage, network, installation source, and stale delivery metadata.

Next polish:
- Keep MediaPipe as the current native Android model path, then research native llama.cpp / GGUF support for a larger future model catalog.
- Tune prompts and output parsing for small on-device models.
- Add automatic release validation and an in-app shortcut to refresh the Play listing when delivery metadata is stale.

### Optional AI Moments And Coaching

**Status: Planned**

Local AI makes it possible to add more personal moments without relying on cloud quotas. These features will be optional so players can keep the app quiet if they prefer.

Planned improvements:
- AI Moments setting with Quiet, Balanced, and Immersive modes.
- Per-feature toggles for coaching, battle narration, session stories, journal entries, daily briefings, and import cleanup.
- Workout Bard / Session Narrator for short RPG summaries after sets, PRs, and sessions.
- Adaptive Workout Coach for suggested next-set adjustments.
- Exercise Substitution when equipment is busy or an exercise needs replacing.
- Personalized Daily Quest Briefing based on recent workouts and goals.
- Battle Commentary and victory/loss epilogues.
- Smart Import Cleanup for exercise matching, duplicate detection, and routine naming.
- AI-assisted import weight suggestions based on previous logged workouts, with review before saving.
- Lore Journal that turns real training milestones into a private chronicle.
- NPC Coach Personalities with different tones.
- Readiness explanations for push, normal, or deload recommendations.
- Gear flavor text, custom monster/boss generation, player card summaries, natural-language workout building, and feedback cleanup.

### Hevy API & CSV Workout Import System

**Status: Finished**

What changed:
- Direct REST API integration with Hevy (`https://api.hevyapp.com/v1/routines`) using Hevy API Keys.
- Native Android SAF CSV File Picker for importing workout CSVs exported from Hevy, Strong, or generic formats.
- Automatic exercise name mapping and RPG category inference (Strength, Cardio, Bodyweight, Flexibility).
- Planned screenshot import support for trainer routines that are not exposed by external APIs, including splitting one screenshot into multiple training templates.
- Planned starting-weight suggestions that use the player's prior workout history and explain the source before saving.
- Smart duplicate protection: preview dialog detects existing routines, tags them `(Already Logged)`, auto-deselects duplicates, and appends `(Imported)` if forced to prevent name clobbering.
- **"Archives Restored"** import bounty reward: importing routines awards bonus XP, Gold, a Wooden Loot Chest, and unlocks the **"Archivist"** title with full confetti reward reveal presentation.
- **AI Quest Renaming**: Existing Training Quests with generic names (like "Ironroot Pull Trial 1") can be "Fantasy-fied" using AI to generate immersive, lore-appropriate titles based on the workout's exercises.
- **Hero's Permissions Consolidator**: A new one-time flow for returning users (or those who skip onboarding) that requests all essential permissions (Passive Travel, Battle Alerts, Health Sanctuary) in a single, immersive RPG-themed screen.



### Reward Reveal And Chest Opening

**Status: Finished**

What changed:
- Dedicated animated chest opening sequence with shake and tap-to-unlock effects.
- Dynamic reward headers for chest loot, victory spoils, forge results, and patrol rewards.
- Unified reward reveal presentation showing exact items, quantities, tiers, and icons.

### Gear Comparison And Locked Tier Messaging

**Status: Finished**

What changed:
- Item detail sheets compare selected gear side-by-side against currently equipped items.
- Color-coded stat delta pills (Green for ATK/DEF/HP upgrades, Red for downgrades).
- Hero equipment slot picker grid displays live stat comparison deltas before equipping.
- Explicit requirement lock banners explaining why gear is locked (class affinity or tier level requirements).
- Show whether an item is better for attack, defense, HP, rarity, tier, or upgrade level.
- Explain why locked gear tiers are unavailable and what milestone unlocks them.

### Boss-Gated Biome Progression

**Status: Working Next**

Planned improvements:
- Progress visualization for boss unlocks.
- First-clear boss rewards.
- Biome advancement as a major chapter milestone.

## Planned Gameplay Systems

### Movement Mastery

**Status: Planned**

Specific exercises will level up over time. Instead of only gaining broad stats like strength or endurance, players will also build mastery in movements like squats, pushups, running, planks, rows, presses, and more.

Example rewards:
- Pushup Mastery: defensive or physical resilience perks.
- Squat Mastery: stronger stance, HP, or defense perks.
- Running Mastery: travel, stamina, or endurance perks.
- Plank Mastery: core defense and max HP perks.

### Traits And Combat Effects

**Status: Planned**

Traits will make gear, skills, mastery perks, runes, and classes interact in more interesting ways.

Example traits:
- Lifesteal.
- Thorns.
- Lightning damage.
- Damage reduction.
- Starting shield.
- Bonus damage against weakened enemies.

### Class Skill Trees

**Status: Planned**

Classes will become build choices, not just starting labels. Players will earn skill points and choose branches that change how they fight and progress.

Example paths:
- Warrior Guardian: defense, HP, thorns.
- Warrior Berserker: higher damage with riskier play.
- Mage Storm: burst damage and lightning effects.
- White Mage Sanctuary: shields, healing, recovery.

### Biome Layers And Dynamic Zones

**Status: Planned**

After clearing a biome, players should be able to return to harder layered versions of that biome with stronger enemies, better loot, bosses, and unique rewards.

Why it matters:
- The world can grow without needing endless static zones.
- Favorite biomes stay relevant.
- Long-term players always have a deeper challenge.

### Ascension

**Status: Planned**

Ascension is a long-term prestige loop. When players reach a major milestone, they can reset some progress in exchange for permanent power.

The goal:
- Keep long-term progression alive.
- Preserve meaningful history, cosmetics, records, usernames, and achievements.
- Make each new run feel faster and more powerful.

## Planned Reward And Cosmetic Systems

### Better Loot And Containers

**Status: Planned**

Loot should feel exciting from the moment it drops to the moment it is used.

Planned improvements:
- Better chest opening.
- Better offline reward summaries.
- Reward rarity animations.
- Themed boss coffers.
- Biome-specific chest visuals.
- More meaningful item sources.

### Visual Identity And Cosmetics

**Status: Planned**

FitQuest needs stronger hero visuals so cosmetics feel rewarding. The long-term goal is a layered avatar system where armor, weapons, auras, titles, frames, and cosmetic sets visibly change the hero.

Planned improvements:
- Layered hero avatar renderer.
- Gear and weapon visual metadata.
- Cosmetic slots separate from combat gear.
- Boss, biome, mastery, and ascension cosmetic rewards.
- More vibrant and consistent fantasy art direction.
- Keep the reliable native workout/watch foundation while adding a focused animation and rendering layer for hero, gear, battles, rewards, and housing.
- Prototype animated layered 2D first, then evaluate a native 3D renderer or embedded game module only where it materially improves customization and reward feel.
- Add reduced-effects controls so players can choose full spectacle or a quieter, battery-friendly experience.

### Home Base And Room Design

**Status: Planned**

A future home base system could let players decorate a room with trophies, furniture, biome decor, and personal achievements.

Why it fits:
- Gives cosmetics another place to matter.
- Creates a cozy long-term collection layer.
- Lets achievements become visible instead of just text.

## Planned Fitness And Catalog Improvements

### Exercise Catalog Trust

**Status: Planned**

Players coming from apps like Hevy should be able to find the exercises they already know.

Planned improvements:
- Exercise aliases and abbreviation matching.
- Better search.
- "Did you mean?" results.
- Support for imported workout names.
- Missing exercise and machine reporting.
- Tracking type checks so distance/time exercises do not show weight/reps logging.

### Richer Exercise Detail Pages

**Status: Planned**

Exercise pages should feel useful and immersive.

Planned improvements:
- Better exercise visuals that match FitQuest style.
- Muscles worked.
- Equipment and machine details.
- Instructions and tips.
- Personal records.
- Recent history.
- Timeframe graphs for week, month, year, and all time.

### Feedback, Bugs, And Feature Requests

**Status: Planned**

Players need an easy way to report bugs, suggest features, flag missing or incorrect exercises, submit missing equipment/machines, and suggest workouts.

Planned improvements:
- Feedback form.
- Bug report form.
- Feature request form.
- Workout suggestion form.
- Missing exercise submission form.
- Missing equipment or machine submission form.
- Exercise correction form for wrong muscles, tracking type, equipment, instructions, or images.
- Optional diagnostics.
- Privacy-friendly reporting.
- Submission statuses so users can see whether something is new, reviewed, planned, fixed, declined, duplicate, or added.

### What's New And Release Notes

**Status: Planned**

Users should be able to see what changed after major updates instead of discovering new features by accident.

Planned improvements:
- What's New screen in Settings.
- Major update cards for new features, improvements, fixes, balance changes, new exercises, new equipment, new workout templates, gear, cosmetics, and watch app updates.
- A dismissible update sheet for important releases.
- Seen/unseen tracking so users are not shown the same update repeatedly.
- Optional links from release notes to the feature or screen that changed.

## Planned Social And Live Systems

### Live Party Workouts

**Status: Planned**

Party members should be able to see workout activity in a motivating way without feeling watched.

Planned improvements:
- Workout started events.
- Set logged events.
- Rest started events.
- Workout completed events.
- Party battle log.
- Shared boss HP during group workouts.
- Privacy controls.

### Player Cards And Social Invites

**Status: Planned**

Players should be able to tap another hero and understand who they are, what they have leveled, and how to team up with them.

Planned improvements:
- Public player cards from parties, guilds, rivals, leaderboards, and search.
- Hero avatar, title, current class, class/job levels, guild/party status, achievements, and visible gear/cosmetics.
- Invite actions for parties and guilds.
- Privacy settings for what other players can inspect.
- Report and block actions for safety.

### Guild Ranks And Party Permissions

**Status: Planned**

Guilds and parties need better management tools as communities grow.

Planned improvements:
- Guild owner, officer, member/veteran, and recruit ranks.
- Permission controls for invites, kicks, join approvals, announcements, and events.
- Party ownership transfer.
- Optional party officer permissions.
- Audit logs for major social changes.

### Trading And Auction House

**Status: Planned / Feasibility-Gated**

Trading could make loot and crafting feel more social, but it needs careful economy and safety rules before it is added.

Planned approach:
- Start with direct player-to-player trading.
- Use a two-sided confirmation/escrow flow.
- Restrict account-bound, equipped, upgraded, quest-critical, and special cosmetic items.
- Add trade history, cooldowns, listing fees/taxes, and anti-abuse checks.
- Consider an auction house only after direct trading and economy safeguards are proven.

## Roadmap Summary

| Area | Status | Next Step |
| --- | --- | --- |
| Shop, Inventory, Armory | Foundation Built | Finish Hero gear picker parity and add compare-to-equipped polish |
| Forge | Foundation Built | Add crafting, refining, unsocketing, and better cost previews |
| Gear Progression Pacing | Foundation Built | Add boss-gated biome UI and reward guarantees |
| Reward Reveals And Chests | Foundation Built | Finish chest-first animation, idle reward presentation polish, and action buttons |
| Movement Mastery | Planned | Add mastery database and XP rules |
| Traits And Combat Effects | Planned | Add first trait model and BattleEngine hooks |
| Skill Trees | Planned | Build first Warrior prototype |
| Biome Layers | Planned | Add layered zones and boss gates |
| Cosmetics | Planned | Build layered avatar visuals |
| Home Base | Planned | Add cosmetic room/decor storage |
| Exercise Catalog | Planned | Add aliases, imports, and catalog audits |
| Feedback And Submissions | Planned | Add bugs, features, workouts, exercises, and equipment submissions |
| What's New | Planned | Add user-facing release notes and major update cards |
| Optional AI Moments And Coaching | Planned | Add local AI features with Quiet/Balanced/Immersive controls |
| Live Party Workouts | Planned | Add privacy-aware activity events |
| Player Cards And Social Invites | Planned | Add inspectable player profiles and party/guild invite actions |
| Guild Ranks And Party Permissions | Planned | Add roles, permission flags, ownership transfer, and audit logs |
| Trading And Auction House | Planned | Prototype safe direct trading before auction listings |

## Suggested Google Sites Layout

Use these page sections:

1. **Hero Section**: FitQuest Public Roadmap.
2. **Current Focus**: Reward reveals and chest opening.
3. **Recently Finished**: Shop, Inventory, and Armory Refresh.
4. **Working Next**: reward reveals, gear comparison, boss-gated biomes.
5. **Planned Gameplay Systems**: mastery, traits, skill trees, biome layers, ascension.
6. **Planned Reward And Cosmetic Systems**: loot, cosmetics, home base.
7. **Fitness And Catalog Improvements**: exercise search, details, feedback, submissions.
8. **Optional AI Moments**: local AI coaching, narration, import cleanup, and user noise controls.
9. **What's New And Release Notes**: how users will learn about updates.
10. **Social Systems**: player cards, guild ranks, party permissions, and trading plans.
11. **Roadmap Summary Table**: quick scan for visitors.
