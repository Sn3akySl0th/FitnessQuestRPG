# FitQuest Long-Term Gameplay Plan

## How To Read This Roadmap

Start with the **Current Snapshot** and **Next Best Work** sections when you want to know where the project stands. Use **First Concrete Tickets** when handing work to an AI agent. The detailed **Agent Workstreams** are reference notes for design intent, dependencies, and future expansion.

Status key:
- `[x] Done`: implemented and compiled.
- `[~] Foundation`: first technical foundation exists, but the feature is not player-complete yet.
- `[ ] Todo`: not started or still only planned.
- `[bug]`: user-facing defect/regression to fix before treating related roadmap work as complete.
- `[feature]`: new capability or planned improvement.

## Current Snapshot

### Done Or Foundation Complete

- `[x] Multi-Tier RPG Gear Rarity Scaling System`: Full 5-tier rarity system (Common 1.0×, Uncommon 1.10×, Rare 1.25×, Epic 1.45×, Legendary 1.70×) with deterministic source rolling, stat scaling on ATK/DEF/HP, salvage bonuses, and comprehensive 3-day and 30-day simulation tests.
- `[x] Wear OS Zero-Scroll Redesign & Workout Sync`: Glanceable single-screen workout UI with top status pill (HR, Zone, Streak, Calories), integrated exercise switcher with completion checkmarks, side-by-side steppers, historical weight prefill sync between phone & watch, live wrist cardio stopwatch, and interactive tap-to-edit logged sets on phone.
- `[~] Progression pacing foundation`: gear tier caps now depend on progression rules, boss/biome state, and loot source instead of only player level.
- `[~] Biome progress foundation`: Room now stores biome layer, boss unlock, boss defeated, progress points, and first-clear reward state.
- `[x] Forge foundation`: gear instances support upgrade level, rarity, traits, and origin biome; repository methods exist for salvage and upgrade.
- `[x] Shop, Inventory, Armory browsing MVP`: the old long-list feel has been replaced with tabs, grids, filters, sorting, detail sheets, armory slots, and Forge entry actions.
- `[x] Procedural Avatar & Widget`: full-body layered avatar rendering on the home screen widget and throughout the app.
- `[x] Class Identity Expansion`: added Necromancer summons (Skeletons/Zombies) and unique class auras for all jobs.
- `[x] YT Music Integration`: media session controls on the active workout screen.
- `[x] Daily Body Metrics`: daily weight logging quest and background sync foundation.

- `[x] On-Device Local AI Engine & Downloader`: Settings model downloader (Gemma ~1.3GB / Llama ~850MB), zero rate-limit hybrid AI fallback, and offline workout/battle generation using **MediaPipe GenAI**.
- `[x] Public Model Mirrors`: Integrated public mirrors for Gemma and Llama to bypass Hugging Face gated repo auth (401 errors).
- `[x] Local AI Inference Library`: Integrated `mediapipe-genai` for on-device LLM execution of optimized `.bin` task models.
- `[x] Play Asset Delivery Model Foundation`: Added an on-demand `local_ai_model` asset pack and Settings install flow so Google Play can host the official offline model without bloating the base app.
- `[x] Play Asset Delivery Live Validation`: version 276 was verified in Play Console and on a physical internal-test device; stale `PACK_UNAVAILABLE (-2)` metadata was recovered by refreshing the FitQuest Play Store listing, after which byte-based download progress began normally.

- `[x] AI Workout Renaming`: UI actions to "Fantasy-fy" generic training quest names (e.g., from Hevy imports) into immersive RPG titles using cloud or local AI.

- `[x] Hero's Permissions Consolidator`: consolidated permission request screen for returning users to ensure Passive Travel, Alerts, and Health Sync are active without manual settings hunting.
- `[x] Hevy API & CSV Workout Import System`: direct Hevy API client, Android SAF CSV parser (Hevy, Strong, generic CSVs), routine/history routing, immersive AI quest naming for imported routines, duplicate protection, Settings repair/reimport action for older flooded Hevy imports, and "Archives Restored" import bounty reward (XP, Gold, Wooden Chest, and "Archivist" title).


- `[x] Reward Reveal & Chest Opening`: chest-first opening animation with shake & tap-to-unlock, dynamic reward headers, and unified reveal dialog.
- `[x] Compare-to-equipped & locked-tier messaging`: side-by-side ATK/DEF/HP stat comparison deltas and clear requirement lock messaging in item detail sheets and Hero slot picker.

- `[x] Smart Workout Recommendation & Recovery Engine`: split sequence engine (Upper -> Lower -> Push -> Pull -> Legs), 48-hour muscle recovery decay math, "Today's Recommended Quest" hero card, and soreness penalties.
- `[x] Interactive Hero Muscle Anatomy Heatmap`: Front & Back body views with real-time green/yellow/red recovery overlays, status tooltips, and direct tap-to-flag soreness.
- `[x] Rest Day Active Recovery Quests & "Well Rested" Buff`: Light 10-minute mobility/walking/bodyweight side quests on rest days that award Gold + a temporary +15% XP & Gold boost on your next workout.
- `[x] AI Random Micro Bodyweight Quests & Local Notifications`: local notifications for quick bodyweight side quests (e.g. 10 pushups, 20 jumping jacks).
- `[x] Avatar Wallpaper & Watch Face Exporter`: high-resolution vector avatar bitmap rendering for wallpapers & Wear OS watch face graphics with automatic Wear OS Data Layer sync (`/fitnessrpg/avatar_watch_face`).

### Next Best Work

1. `[ ] Boss-gated biome progression UI/rules`: use the new biome progress data to block or guide travel into later zones.
2. `[ ] Movement Mastery MVP`: make individual exercises level up and unlock passive perks.
3. `[ ] Exercise Catalog Trust MVP`: aliases, import matching, missing exercise reporting, and catalog coverage checks.
4. `[ ] Feedback and submission hub`: collect bugs, feature requests, workout corrections, missing exercises, and missing equipment/machines.
5. `[ ] What's New system`: show users major changes, fixes, new exercises, new gear, and new features after updates.

### Current Build Priority

The shared reward reveal/chest foundation is complete. The best next milestone is now **boss-gated biome progression**, followed by Movement Mastery and exercise catalog trust. These systems establish meaningful gates, long-term training identity, and reliable workout data before skill trees, procedural layers, ascension, and a larger cosmetic economy depend on them.

## Bug Queue

Bugs should be fixed and marked here separately from future feature planning.

### Open Bugs

- `[bug] [ ] Hero gear slot picker still needs full inventory parity`: the Hero > Gear slot picker has been upgraded from a plain list to a compact grid, but it still needs the same filtering/sorting/detail depth as the main Shop/Inventory/Armory surface before the gear system can be considered fully converted away from long lists.
- `[bug] [ ] Idle rewards still have two steps`: the older idle modal now avoids inaccurate gear claims, but the future polished flow should go straight from idle claim into the shared reward reveal or a proper chest-first reveal.
- `[bug] [ ] Hevy trainer-built routines may not be exposed by the public API`: live testing showed `/v1/routines` returns saved user routines, while `/v1/routine_folders` can return an empty `routines` array for trainer/program content. If Hevy does not expose trainer-built plans for an API key, users may need to copy/save those routines into their own routines or import via CSV.

### Fixed Bugs

- `[bug] [x] Play Asset Delivery appeared stuck or unavailable`: download progress now uses downloaded bytes during `DOWNLOADING`, handles Play confirmation/Wi-Fi states, exposes status/error diagnostics, and converts raw SDK exceptions into concise recovery copy. A live version-276 `PACK_UNAVAILABLE (-2)` incident was traced to stale Play Store delivery metadata and recovered by force-stopping Play Store, opening the FitQuest listing, and retrying.
- `[bug] [x] Hevy history import flooded Training with every completed workout`: completed Hevy/API/CSV history now imports into `sessions` and `set_logs`, reusable routines still import as Training quests, background sync dedupes sessions, and Settings includes a Hevy repair action that deletes botched history templates and reimports real completed workout set data into History.
- `[bug] [x] Hevy importer used invalid API page sizes`: Hevy rejects `pageSize` values above 10. Import, history repair, background sync, and diagnostics now use the API-safe page size.
- `[bug] [x] Offline rewards could hide exact item loot`: idle encounter loot is now stored as pending claim loot and applied only when idle rewards are claimed, so materials, gear, gold, energy, and XP boosts can appear in the reward reveal.
- `[bug] [x] Idle rewards estimated gear from monster kills`: the idle modal now says loot will reveal on claim instead of showing a guessed gear count.
- `[bug] [x] HeroScreen had always-true null checks in the gear/combat state mapper`: removed the redundant checks.
- `[bug] [x] HeroScreen used deprecated tab component`: replaced the deprecated `TabRow` call with `PrimaryTabRow`.
- `[bug] [x] Upgrade cost used generic material copy`: item details now name the required upgrade material instead of saying only "materials."
- `[bug] [x] Gear detail did not clearly say who can equip it`: equippable item details now show "Equippable by: All classes" or the required class.
- `[bug] [x] Forge salvage reward was labeled like chest loot`: salvage now uses the `FORGE` reward source.
- `[bug] [x] Reward reveal labels had corrupted emoji text`: reward badges now use readable labels for gold, XP, energy, PRs, levels, maps, titles, and skill points.
- `[bug] [x] Chest reward flattening had a fake zero-gold fallback`: nested chest rewards now flatten through a dedicated reward helper.
- `[bug] [x] Duplicate party/guild members on re-login`: PartyService and GuildService now deduplicate member listings by UID and normalized hero name, and automatically clean up orphaned Firestore member documents from previous guest sessions.
- `[bug] [x] Demo accounts published to public leaderboards`: SyncService, PartyService, GuildService, and RivalsScreen now filter out demo/test accounts (`isDemoUser`) and developer sandbox sessions from public leaderboards and rosters.

## Feature Queue

Feature work belongs here or in the detailed Agent Workstreams below. Bugs above should usually win over new feature polish when they block player understanding or trust.

### Working Features

- `[feature] [~] Reward reveal and chest opening`: shared reward reveal foundation exists; chest-first animation, offline reward accuracy, rarity/tier presentation, and reward action buttons remain.
- `[feature] [~] Shop, Inventory, Armory, and Forge UX`: main market/inventory surface has tabs, grids, filters, sorting, detail sheets, and forge entry actions; Hero gear picker still needs full parity.

### Planned Features

- `[feature] [ ] Compare-to-equipped and locked-tier messaging`.
- `[feature] [ ] Boss-gated biome progression UI/rules`.
- `[feature] [ ] Movement Mastery MVP`.
- `[feature] [ ] Exercise Catalog Trust MVP`.
- `[feature] [ ] Feedback and submission hub`.
- `[feature] [ ] What's New system`.
- `[feature] [ ] Social player cards, guild ranks, party permissions, and player economy`.
- `[feature] [ ] Local AI moments and noise controls`.

## Goal

Make FitQuest rewarding for months or years by connecting real training variety to RPG progression. The app should always give the player:

- A short-term reason to work out today.
- A medium-term goal to chase this week.
- A long-term identity/build/world goal that survives level caps.
- A prestige loop that makes starting over feel powerful, not punishing.

## Design Pillars

1. Real training drives game identity.
2. Every reward should have a use.
3. Build choices should matter more than raw stat totals.
4. Progress should be visible on the hero, map, inventory, and history.
5. Long-term systems should reset carefully and preserve meaningful history.

## Agent Workstreams

### Agent A: Movement Mastery

Purpose: Make specific exercises matter beyond generic STR/END/AGI/WIL gains.

Core feature:
- Track mastery XP per movement, normalized exercise name, and category.
- Level movement mastery independently from hero level.
- Unlock passive perks at mastery milestones.

Example perks:
- Pushup Mastery 10: Iron Grip, physical damage taken -5%.
- Squat Mastery 10: Rooted Stance, stun/knockdown resistance or +defense.
- Running Mastery 10: Marathon Runner, +max energy or faster travel.
- Plank Mastery 10: Core Wall, +max HP.
- Yoga Mastery 10: Centered Mind, +WIL or better recovery.

Implementation tasks:
- Add DB table for movement mastery.
- Update session completion to grant mastery XP from logged exercises.
- Add mastery calculation helpers in domain layer.
- Add movement mastery section to exercise detail and PR screens.
- Add passive perk resolver that feeds `GameMath`.

Dependencies:
- Exercise name normalization from the exercise catalog.
- GameMath support for passive modifiers.

Deliverables:
- `MovementMasteryEntity`
- `MovementMasteryRepository` or repository methods
- `MasteryPerks.kt`
- Mastery UI section
- Tests for mastery XP and perk unlocks

### Agent B: Traits And Combat Effects

Purpose: Add build variety before building full skill trees.

Core feature:
- Introduce traits as reusable combat modifiers.
- Traits can come from skills, mastery perks, gear, runes, biomes, and ascension.
- Battle engine resolves traits during combat.

Trait examples:
- Lightning Damage: adds bonus elemental damage.
- Conductive: lightning jumps to an additional enemy when multi-enemy combat exists.
- Thorns: reflects a percent of incoming physical damage.
- Lifesteal: heals after dealing damage.
- Shielded: starts battle with temporary HP.
- Executioner: bonus damage against enemies below 30% HP.
- Enduring: reduced damage when HR zone is high or stamina is low.

Implementation tasks:
- Add `Trait` enum/value model.
- Add `TraitSource` model for gear, class, mastery, ascension, biome.
- Add trait aggregation from character state and equipped gear.
- Update `BattleEngine` to accept combat traits.
- Keep the first version small: damage bonus, damage reduction, lifesteal, and shield.

Dependencies:
- None required, but should be designed to support skill trees later.

Deliverables:
- `Traits.kt`
- Trait-aware `CombatStats`
- Trait hooks in `BattleEngine`
- Trait chips in gear/hero UI
- Tests for each first-pass trait

### Agent C: Class Skill Trees

Purpose: Move classes from static archetypes into player builds.

Core feature:
- Players earn skill points as they level.
- Each class has branching skill nodes.
- Skill nodes grant traits, stat bonuses, active abilities, or passive effects.

Example trees:
- Warrior:
  - Guardian path: HP, defense, taunt/thorns traits.
  - Berserker path: critical damage, low-HP damage, attack bonuses.
- Mage:
  - Storm path: burst damage, lightning traits.
  - Arcane path: rune power, cooldown/energy efficiency.
- White Mage:
  - Sanctuary path: shields, healing, recovery perks.
  - Judgment path: holy damage and defensive retaliation.
- Monk:
  - Flow path: dodge, speed, combo bonuses.
  - Iron Body path: defense, HP, mastery scaling.

Implementation tasks:
- Add skill tree data definitions.
- Add selected skill node storage.
- Add skill point calculation from level and ascension bonuses.
- Add skill tree UI with locked/unlocked/purchased states.
- Add respec cost using gold/materials.

Dependencies:
- Trait system should exist first.

Deliverables:
- `SkillTree.kt`
- `CharacterSkillEntity`
- Skill tree screen
- Skill point/respec logic
- Starter trees for 3 classes, then expand to all classes

### Agent D: Materials, Forge, And Economy

Purpose: Make materials valuable instead of simple sell fodder.

Core feature:
- Materials upgrade gear, craft gear, salvage duplicates, and manage runes.

Forge services:
- Upgrade gear stats with gold + materials.
- Salvage gear into materials.
- Craft class gear from recipes.
- Unsocket runes using Arcane Dust.
- Refine lower-tier materials into higher-tier materials.

Material identity:
- Scrap Iron: weapons and plate armor.
- Arcane Dust: robes, runes, magic gear, unsocketing.
- Beast Hide: light armor, ranger/druid/monk gear.
- Ember Coal: high-tier upgrades and berserker/fire gear.

Implementation tasks:
- Add forge recipe model.
- Add repository operations for upgrade, salvage, craft, refine, unsocket.
- Add Forge screen.
- Add material cost preview in gear sheet.
- Tune economy so materials feel valuable but not scarce enough to block fun.

Dependencies:
- Current material catalog and gear instances.

Deliverables:
- `ForgeRecipes.kt`
- Forge screen
- Repository methods
- Upgrade/salvage tests

### Agent D2: Shop, Inventory, And Armory UX

Purpose: Replace long item lists with an RPG armory experience that makes gear, materials, runes, chests, and upgrades easier to browse and more satisfying to manage.

Core feature:
- Split the market/inventory/forge surface into clearer modes: Shop, Inventory, Armory, and Forge.
- Use grid-style item cards, slot/category filters, sorting, item detail sheets, and equipped-slot summaries instead of one long list.
- Make item decisions obvious: buy, equip, compare, upgrade, salvage, sell, open, use, or fuse.

Navigation structure:
- Shop: featured/recommended items, class gear, supplies, runes, materials, and locked tier messaging.
- Inventory: owned gear, stackables, chests, runes, materials, consumables, newest/favorite filters later.
- Armory: equipped paper-doll slots, current build summary, slot-tap filtering.
- Forge: upgrade, salvage, socket, refine, and fusion actions.

Filter and sort options:
- Filter by slot, category, class compatibility, stackable type, equipped state, upgradeable state, tier, rarity, and source/biome.
- Sort by tier, power, price, quantity, upgrade level, newest, and slot.
- Add "for my class", "can afford", "owned", "equipped", and "upgradeable" quick filters where useful.

Item detail sheet:
- Show larger item icon/art, slot, tier, rarity, upgrade level, stats, class restriction, source biome, quantity, and description.
- Show contextual actions: buy, equip/unequip, upgrade, salvage, sell, open chest, use consumable, or view possible rewards.
- Include compare-to-equipped when the item is equippable.

Visual direction:
- Rarity borders and glows.
- Slot icons and tier badges.
- Compact grid cards with stable dimensions.
- Useful headers with gold/material counts and current equipped summary.
- Clear empty states for no owned gear, no upgradeable gear, no chests, and locked tiers.

Implementation tasks:
- [x] Add item filter/sort UI state.
- [x] Build reusable compact item grid/card components.
- [x] Build item detail sheet with contextual actions.
- [x] Add armory slot summary/paper-doll component (`HeroPaperDoll.kt`, `PaperDollLayerRenderer.kt`).
- [x] Integrate Mythic rarity, Gear Traits, and merchant tier cap (Tier 1–3).
- [x] Wire forge foundation methods into inventory/forge actions.
- [x] Keep chest opening on the current result path until Agent G4 reward reveal is implemented.

Dependencies:
- Forge repository methods.
- Progression tier rules for locked tier messaging.
- Reward reveal system for chest/opening polish later.
- Gear visual metadata and item icon system.

Deliverables:
- Shop/inventory segmented modes.
- Grid-based item browsing.
- Filter and sort controls.
- Armory equipped-slot summary.
- Item detail sheet.
- Upgrade and salvage actions in UI.
- Locked-tier helper messaging.

### Agent E: Biome Layers And Dynamic World

Purpose: Prevent static biome completion from becoming the end.

Core feature:
- Biomes have mastery, bosses, layers, and scaling.
- Clearing a biome unlocks deeper versions of that biome.

Layer examples:
- Meadow
- Meadow Depth II
- Meadow Depth III
- Meadow: Abyss Layer

Biome mechanics:
- Each biome has resource drops, monster traits, boss unlocks, and travel requirements.
- Layers scale monster level, reward tier, and trait complexity.
- Biome mastery grants passive world bonuses and cosmetic auras.

Implementation tasks:
- Extend biome model with layer, mastery, boss state, and unlock rules.
- Add biome-specific loot tables.
- Add boss encounters per biome/layer.
- Add adaptive monster traits for high-geared players.
- Add map UI for layer progress.

Dependencies:
- Trait system for monster modifiers.
- Forge/economy for biome-specific materials later.

Deliverables:
- `BiomeLayer.kt`
- Layered monster generation
- Biome mastery storage
- Map progress UI
- Boss unlock logic

### Agent F: Ascension

Purpose: Create an infinite progression loop that still respects the user's history.

Core feature:
- At a major milestone, the player can ascend.
- Ascension resets run-level progress but grants permanent account power.

Suggested unlock:
- Reach level 50 or 100.
- Defeat a final boss or clear a biome abyss layer.
- Confirm through a multi-step warning screen.

Reset on ascension:
- Hero level
- Gold
- Equipped gear and gear inventory
- Energy state
- Non-permanent skill selections

Preserve on ascension:
- Account identity and username
- PR history
- Movement mastery records, or keep mastery but reset active mastery perks only if balance requires it
- Cosmetics
- Trophies/achievements
- Unlocked exercises and machines
- Ascension upgrades

Permanent rewards:
- +2% XP gain
- +2% gold gain
- +1 starting stat
- +1 max energy
- +1 skill point per run
- Increased rare loot chance
- Cosmetic aura/title/frame

Implementation tasks:
- Add ascension profile/entity.
- Add ascension point store.
- Add reset transaction with strict preservation rules.
- Add confirmation UI.
- Add ascension reward screen.

Dependencies:
- Skill tree and biome boss systems are ideal but not mandatory.

Deliverables:
- `AscensionEntity`
- Ascension screen
- Reset transaction
- Ascension upgrades
- Tests for preservation/reset rules

### Agent G: Relic Loot And Item Synergies

Purpose: Make loot exciting and build-defining.

Core feature:
- Add relic items with traits and special effects.
- Add item synergies that combine with class skills.

Examples:
- Lightning Dagger: attacks deal lightning damage.
- Conductive Armor: lightning damage jumps to another enemy.
- Dawnshield: starts battle shielded.
- Blood Pact Ring: higher damage at low HP.
- Worldroot Boots: travel gains increased by cardio workouts.

Implementation tasks:
- Add special trait fields or item effect registry.
- Add relic rarity rules.
- Add named relic drops from biome bosses.
- Add visual rarity treatment in item UI.
- Add tooltip text explaining synergies.

Dependencies:
- Trait system.
- Biome bosses for named drops.

Deliverables:
- Relic catalog
- Effect resolver
- UI trait display
- Loot table integration

### Agent G2: Progression Pacing, Boss Gates, And Gear Tiers

Purpose: Slow overall progression so higher-tier gear, zones, and skills feel earned instead of disposable.

Core feature:
- Treat gear tiers, skill growth, and biome unlocks as chapter milestones.
- Gate Tier 3+ gear and new biomes behind bosses, level requirements, quests, or crafted keys.
- Make common workouts reward XP, materials, gold, and mastery consistently, while high-tier gear comes from meaningful achievements.

Progression philosophy:
- Gear should be remembered, upgraded, and visually meaningful.
- Skills should define the player's build more than raw stats.
- Bosses should test the player's training choices before the next zone opens.
- Procedural biome layers should extend play after a fixed biome is cleared.

Gear pacing rules:
- Tier 1: starter gear and early drops.
- Tier 2: first biome progression and starter forge upgrades.
- Tier 3: first boss or first major quest chain reward.
- Tier 4: midgame boss, elite monster, crafted recipe, or weekly campaign reward.
- Tier 5+: rare boss, relic, abyss layer, ascension, or major achievement rewards.

Reward tuning:
- Normal workouts should mostly grant XP, mastery XP, gold, materials, and occasional low-tier gear.
- Elite encounters, bosses, weekly campaigns, and biome layer clears should be the primary source of better gear.
- Duplicate gear should feed the forge through salvage instead of feeling like trash.
- Gear upgrades should require materials so materials remain useful throughout the game.

Boss gates:
- Each biome should have a named boss that must be defeated before the next biome unlocks.
- Boss unlocks can require biome progress, quest steps, energy, a crafted key, or recommended stat readiness.
- Bosses should use mechanics, not only higher HP.
- Boss rewards should include milestone items, class materials, relic chances, skill points, or zone access.

Boss mechanic examples:
- Shielded boss rewards strength or armor-break traits.
- Poison boss rewards endurance, recovery, or cleansing traits.
- Fast boss rewards agility, dodge, or accuracy.
- Undead boss rewards consistency, holy traits, or willpower.
- Armored construct rewards crafted weapons or material-based upgrades.

Procedural zone direction:
- Keep fixed named biomes for identity and story.
- Add procedural layers inside cleared biomes for long-term replay.
- Each layer can modify monster traits, environmental rules, drops, bosses, and recommended training stats.
- Layer depth should increase reward quality slowly, while still letting players feel stronger from upgrades.

Implementation tasks:
- Add gear tier availability rules. `Implemented foundation: ProgressionRules.kt now caps loot tiers by player, biome progress, and source.`
- Add loot table weights by biome, layer, boss state, and player level. `Partial: loot tier caps are wired through current reward paths; detailed biome loot tables still pending.`
- Add boss unlock state to biome progress. `Implemented foundation: biome_progress stores layer, boss unlock, boss defeated, progress, and first-clear reward state.`
- Add zone progression gates before map travel unlocks.
- Add boss reward tables and milestone reward guarantees.
- Tune early gear drop rates so Tier 4 cannot appear after only a few days of normal play. `Implemented foundation: normal workout loot is capped low and Tier 3+ requires boss or equivalent progress.`
- Add difficulty preview for bosses and locked zones.
- Add economy tests for expected gear tier after a sample week of play.

Dependencies:
- Forge/economy for upgrade and salvage sinks.
- Trait system for boss mechanics.
- Biome layer model for procedural zone scaling.
- Reward/quest system for unlock requirements.

Deliverables:
- `ProgressionRules.kt`
- Gear tier gate resolver
- Boss gate resolver
- Tuned loot tables
- Boss reward tables
- Map locked-zone UI states
- Balance tests for early/midgame pacing

### Agent G3: Visual Identity, Avatar Cosmetics, And Reward Presentation

Purpose: Make cosmetics and visual rewards feel exciting by upgrading FitQuest from static/basic art into a layered fantasy avatar and reward presentation system.

Core feature:
- Build heroes from layered visual parts instead of one flat avatar image.
- Separate cosmetic ownership from combat gear stats so players can look how they want without breaking builds.
- Give bosses, mastery milestones, ascension, campaigns, and achievements visible rewards that show on the hero, profile, battle screen, and rewards screens.

Visual style direction:
- Dark fantasy training RPG with high-contrast silhouettes, readable gear shapes, and enchanted equipment details.
- Gear should look forged, powerful, worn, and magical rather than generic.
- Use consistent lighting, shadows, rarity frames, icon crops, and background treatments.
- Replace immersion-breaking stock visuals with FitQuest-styled equipment, movement, biome, and monster art.

Rendering architecture decision:
- Keep the fitness app, Wear OS app, Health Connect, widgets, notifications, Room, and Play integrations native in Kotlin/Jetpack Compose.
- Do not rewrite FitQuest in React Native solely for animation libraries or `llama.rn`; a rewrite would recreate mature Android integrations without improving art quality by itself.
- Use Compose animation and a vector/state-machine runtime such as Rive for responsive UI motion, reward reveals, class effects, animated portraits, and lightweight particles.
- Prototype a dedicated native rendering surface for richer hero, gear, battle, and home-base scenes. Filament is the preferred first 3D/PBR spike because it fits the existing Android app; Godot or Unity can be evaluated later as an embedded game module if housing/battles outgrow a focused renderer.
- Keep gameplay state and progression outside the visual engine so presentation technology can evolve without rewriting workout, inventory, or save logic.
- Support independent effects settings: full, reduced, and minimal motion; sound, haptics, and particles can be controlled separately.

Layered avatar slots:
- Background or scene.
- Aura or elemental effect.
- Base body and class silhouette.
- Hair, face, and expression.
- Armor pieces: helm, chest, gloves, legs, boots.
- Weapon and off-hand item.
- Cape or back item.
- Accessory, trinket, trophy badge, or companion.
- Foreground particles or status effect overlays.

Cosmetic reward types:
- Armor skins.
- Weapon skins.
- Auras.
- Avatar frames.
- Titles.
- Battle entrance effects.
- Crit, spell, slash, heal, and shield effects.
- Biome backgrounds.
- Workout mastery badges.
- Companions or familiars.
- Ascension frames and permanent visual marks.

Reward sources:
- Biome boss clears unlock biome cloak, frame, title, or background.
- Movement mastery milestones unlock exercise-themed badges, auras, or emotes.
- Class questlines unlock class armor skins and weapon poses.
- Weekly campaigns unlock seasonal effects or profile frames.
- Ascension unlocks permanent aura, title, or frame options.
- Abyss layers unlock animated relic cosmetics.

Implementation tasks:
- Add cosmetic catalog models for owned and equipped cosmetics.
- Add `CosmeticSlot`, `CosmeticRarity`, `CosmeticUnlockSource`, and visual asset ids.
- Add visual metadata to gear and weapons, including icon asset, layer asset, pose, rarity effect, and animation tags.
- Build a layered avatar renderer that composes base, equipment, cosmetics, aura, and frame layers.
- Add cosmetic inventory and equip UI.
- Add reward reveal animation for newly unlocked cosmetics.
- Add style guide for generated/imported art so new visuals do not look out of place.
- Add fallback art by category, class, gear slot, and rarity.
- Build a vertical visual-engine spike before committing to 3D: one layered hero, visible equipped gear, idle animation, lighting, a chest opening, rarity particles, audio/haptics, and reduced-effects behavior.
- Measure load time, memory, battery use, frame pacing, download size, and low-end-device behavior before expanding the renderer to battles or housing.

Dependencies:
- Gear and item catalog for equipment visual metadata.
- Achievement, boss, mastery, quest, and ascension systems for unlock sources.
- Visual asset pipeline for consistent images.

Deliverables:
- `Cosmetics.kt`
- `CosmeticCatalog.kt`
- Cosmetic ownership/equipped storage
- Layered avatar renderer
- Cosmetic inventory/equip screen
- Gear visual metadata fields
- Reward reveal component
- FitQuest visual style guide
- First complete themed cosmetic set

### Agent G4: Loot Reveal, Containers, And Reward Feel

Purpose: Make rewards visible, memorable, and satisfying instead of static toasts or vague inventory messages.

Core feature:
- Every reward source should produce a clear reveal moment that shows exactly what the player earned.
- Containers should open through a dedicated chest-opening flow with animation, rarity treatment, item cards, and follow-up actions.
- Offline rewards, workout rewards, boss rewards, quests, campaigns, chests, and idle claims should all use the same reward presentation system.

Current problem examples:
- Offline rewards can say that loot was added without showing the specific item names, icons, rarity, or purpose.
- Inventory chests currently open with a toast-style message instead of a satisfying reveal.
- Reward moments do not yet have enough motion, sound/haptics, rarity treatment, or "what changed" feedback.

Reward reveal principles:
- Show the exact item, quantity, rarity, tier, source, and why it matters.
- Make high-value drops feel different from common drops through animation, color, sound, haptics, and pacing.
- Let users act immediately: equip, compare, salvage, open another, view in inventory, or close.
- Avoid long unskippable animations; allow tap-to-speed-up.
- Keep all reward summaries accessible after the animation for clarity.

Container opening system:
- Add container definitions for wooden chests, biome chests, war caches, boss coffers, campaign crates, relic vaults, and future seasonal containers.
- Each container should define loot table, rarity odds, visual theme, opening animation, and guaranteed rewards.
- Opening flow:
  1. Chest appears with source/theme.
  2. Tap/hold/open animation plays.
  3. Items reveal one at a time with rarity flashes.
  4. Summary grid shows all rewards.
  5. User can equip/compare/salvage/open another.

Offline reward reveal:
- Store the full list of idle/offline rewards before applying or immediately after applying.
- Show a claim modal with:
  - monsters defeated or travel progress
  - gold and XP
  - materials, runes, chests, gear
  - new unlocks, PRs, titles, or cosmetics
- Include "View Inventory" and "Open Chests" actions when relevant.

Dopamine pass across the app:
- Replace plain toast-only reward feedback with reusable reward sheets/modals.
- Add level-up, PR, mastery, boss clear, rare drop, chest open, gear upgrade, and quest-complete moments.
- Add optional haptics and sound hooks using existing effects patterns.
- Add floating numbers, rarity glows, reward trails, and animated progress fills.
- Add satisfying empty-to-filled states for XP bars, mastery bars, boss HP, and forge upgrades.

Navigation and header polish:
- Replace basic bottom navigation icons with stronger FitQuest-specific iconography.
- Use consistent selected/unselected states, readable labels, and fantasy-accented active indicators.
- Build more visually appealing screen headers with contextual art, progress, actions, and current goal summaries.
- Avoid oversized decorative headers on utility screens; make headers useful and beautiful.

Implementation tasks:
- Add a reusable reward model that can represent gear, stackables, gold, XP, energy, titles, cosmetics, mastery XP, skill points, and biome unlocks.
- Add a `RewardRevealController` or UI state holder that receives rewards from repository flows.
- Refactor offline reward claim, chest opening, workout completion, boss victory, quest completion, and campaign completion to use shared reward reveal data.
- Add a dedicated `ChestOpeningScreen` or modal.
- Store recent reward batches if needed so the user can review what was gained.
- Upgrade bottom bar icons and screen headers after the shared reward components exist.

Recommended improvements for MVP:
- **Rarity-Driven Intensity:** revealed items should use their rarity (Common -> Legendary) to scale the intensity of flashes, sounds, and animations.
- **Immediate Action:** allow "Equip" or "Compare" directly from the reveal screen for newly found gear.
- **Consolidated Batching:** ensure the system can handle mixed rewards (e.g. 50 Gold + 1 Epic Sword + 5 Iron Scrap) in a single unified reveal.
- **Skip-to-Summary:** frequent players should be able to tap to skip animations and jump straight to the loot grid.

Dependencies:
- Loot table and progression pacing rules.
- Gear visual metadata and item icon system.
- Forge/salvage for post-reveal actions.
- Haptic/audio effects for optional reward feedback.

Deliverables:
- `RewardGrant` or expanded reward model.
- `RewardBatch` with source, timestamp, and grant list.
- Reward reveal UI component.
- Chest opening flow.
- Offline reward claim modal.
- Inventory chest action integration.
- Bottom navigation icon refresh.
- Header component refresh.

### Agent G5: Home Base, Room Design, And Lifestyle Cosmetics

Purpose: Add a cozy long-term cosmetic layer where real training unlocks furniture, decor, pets/companions, trophies, and biome-themed room upgrades.

Core feature:
- Give the player a personal home base or guild room they can decorate over time.
- Treat room design as a separate cosmetic progression loop from combat gear.
- Use the home as a visual trophy case for achievements, mastery, bosses, ascension, and seasonal campaigns.

Why it fits FitQuest:
- A home base adds emotional attachment and a non-combat reward loop.
- It gives materials, boss drops, quests, and achievements more visible uses.
- It lets players express personality without affecting combat balance.
- It pairs naturally with avatar cosmetics, trophies, companions, and seasonal rewards.

Room systems:
- Furniture slots: bed, desk, rug, wall art, training rack, trophy shelf, window, lighting, floor, wallpaper.
- Decor sets by biome: Meadowlands, Darkwood, Crystal Caves, Ember Peaks, Frozen Wastes, Shadowfen.
- Functional stations: forge corner, trophy vault, wardrobe, training planner, pet bed, quest board.
- Display slots for favorite gear, boss trophies, PR medals, mastery badges, and ascension relics.

Reward sources:
- Boss clears unlock themed furniture or trophies.
- Movement mastery unlocks exercise posters, statues, plaques, or training equipment.
- Weekly campaigns unlock seasonal decorations.
- Ascension unlocks permanent room upgrades, frames, banners, or ambient effects.
- Materials can craft decor so materials have non-combat value too.

Implementation tasks:
- Add home decor catalog model.
- Add owned/equipped room item storage.
- Add a simple room editor with fixed slots first, then drag/drop later if desired.
- Add preview art for the first room theme.
- Add trophy display integration with achievements and PRs.
- Add room reward grants to bosses, campaigns, mastery, and ascension.

Dependencies:
- Cosmetic ownership model.
- Reward reveal system.
- Achievement/trophy expansion.
- Visual style guide for consistent furniture/decor art.

Deliverables:
- `HomeDecor.kt`
- Room ownership/equipped storage
- Home base screen
- Fixed-slot room editor MVP
- First Meadowlands room set
- Trophy/decor reward integration

### Agent H: Rewards, Quests, And Retention

Purpose: Tie all systems together into daily/weekly/monthly play.

Core feature:
- Quest chains and campaigns guide players through mastery, biomes, forge, and skill trees.

Quest types:
- Daily: hydration, steps, one workout, stretch.
- Weekly: complete varied training, defeat bosses, craft/upgrade gear.
- Class: unlock class skills through themed training.
- Biome: travel, defeat monsters, gather materials, clear layer.
- Mastery: train specific movements to unlock perks.

Implementation tasks:
- Extend quest model to support prerequisites and chained steps.
- Add reward types for materials, mastery XP, skill points, cosmetics, and ascension essence.
- Add quest detail screen.
- Add campaign completion celebration.

Dependencies:
- Uses outputs from all other systems.

Deliverables:
- Quest chain model
- Reward model expansion
- Campaign UI updates
- Starter quest chains

### Agent H2: Live Party Workouts

Purpose: Make party training feel alive by showing real-time workout activity, rest status, and shared boss progress while users are actively logging sets.

Core feature:
- Add a compact Live Party Feed inside the active workout screen.
- Show meaningful party activity without turning the workout logger into a noisy chat screen.
- Connect every logged set to the shared raid boss loop.

Live activity fields:
- `isWorkoutActive`
- `activeExercise`
- `liveStatus`
- `lastActivity`
- `lastActivityAt`
- `restEndsAt`
- Optional privacy level: hidden, active only, exercise only, full set summary.

Firestore behavior:
- Write only on meaningful events, not every timer/heart-rate tick.
- Publish when workout starts.
- Publish when exercise changes.
- Publish when a set is logged.
- Publish when rest starts.
- Clear or expire status when workout finishes/abandons.
- Optional heartbeat every 60-90 seconds while active.

UI experience:
- Party Battle Log card near the top of `ActiveSessionScreen`.
- Mini shared boss HP bar.
- Recent teammate actions such as "Paula logged Squats +45 XP".
- Rest countdown labels for active allies.
- Small privacy-aware status chips: training, resting, finished, offline.

Privacy rules:
- Default to a conservative live status.
- Let users choose what party members can see.
- Do not share heart rate, calories, location, bodyweight, screenshots, or detailed health metrics by default.
- Avoid exposing exact weights/reps unless the user opts into full set summaries.

Implementation tasks:
- Extend `PartyMember` live activity model.
- Add `PartyService.reportWorkoutStarted`, `reportExerciseChanged`, `reportSetLogged`, `reportRestStarted`, and `clearLiveActivity`.
- Call the reporting methods from `ActiveSessionViewModel`.
- Add stale-status handling so old live activity disappears automatically.
- Build `PartyLiveFeed` Compose component.
- Add boss HP mini-card inside the active session when the user is in a party.

Dependencies:
- Existing Firestore party service.
- Existing active session state.
- Existing raid boss damage reporting.

Deliverables:
- Live activity fields in party member documents.
- Party live reporting methods.
- `PartyLiveFeed` component.
- Active session integration.
- Privacy setting for live party visibility.
- Firestore rules update if needed.

### Agent H3: Player Cards, Guild Roles, Party Permissions, And Trading

Purpose: Make other players feel like real heroes in the world, and give guilds/parties enough structure to support long-term social play without turning moderation and economy balance into a mess.

Core feature:
- Add player profile cards that can be opened from parties, guilds, rivals, leaderboards, and search.
- Show a player's public hero identity, class/job progress, visible gear/cosmetics, guild/party status, recent achievements, and invite actions.
- Add guild ranks and permissions so guilds can be managed without a single fragile owner-only model.
- Add party ownership transfer and optional party officer permissions.
- Design trading carefully before adding an auction house.

Player card fields:
- Display name, username, avatar, title, current class, level, guild, party, and public status.
- Class/job levels, highest unlocked biome, achievement/trophy highlights, recent PR highlights, and optional "looking for party/guild" status.
- Privacy controls for workout history, body metrics, exact gear stats, online status, and invite availability.
- Actions: invite to party, invite to guild, request friend/follow, inspect public gear, report/block, and message later if chat is ever added.

Guild ranks and permissions:
- Owner: full control, transfer ownership, disband guild.
- Officer: invite/kick lower ranks, approve join requests, start guild events, edit announcement.
- Veteran/Member: participate, donate/contribute, invite if permission is enabled.
- Recruit: limited permissions until promoted.
- Permission flags should be stored separately from rank names so guilds can customize rank labels later.

Party ownership and permissions:
- Party owner can transfer ownership to another member.
- Optional party officers can invite/remove members and start shared activities.
- If owner leaves, transfer to selected successor or longest-active member.
- Add audit events for owner transfer, kicks, invites, and permission changes.

Trading phase plan:
- Phase 1: direct player-to-player trade requests for safe item categories only.
- Phase 2: escrowed trade confirmation screen with both players locking offers before final accept.
- Phase 3: trade history and support/audit trail for disputes.
- Phase 4: auction house only after item binding, rarity/economy sinks, listing fees, taxes, cooldowns, and anti-abuse rules exist.

Recommended trade restrictions:
- No real-money trading.
- No trading equipped items, quest-critical items, ascension currency, account-bound cosmetics, or early onboarding rewards.
- Consider bind-on-equip or bind-on-upgrade for powerful gear.
- Add listing fees/taxes so the auction house does not inflate the economy forever.
- Add minimum account age/level, trade cooldowns, and suspicious-trade logging.

Implementation tasks:
- Add public player profile/card model in Firestore.
- Add profile privacy settings and blocked user list.
- Build `PlayerCardSheet` reusable UI.
- Add entry points from Party, Guild, Rivals, leaderboards, and future search.
- Add guild role model, permission flags, rank management UI, and Firestore rules.
- Add party ownership transfer and permission-aware party actions.
- Prototype direct trading with escrowed confirmation before considering auction listings.
- Add economy telemetry/audit logs for trades, listings, cancellations, and claims.

Dependencies:
- Existing Auth/SyncService identity model.
- Existing PartyService and GuildService.
- Inventory/gear instance model and future item binding rules.
- Firestore security rules and moderation/reporting foundation.

Deliverables:
- `PlayerCardSheet` and public profile model.
- Guild ranks and permission system.
- Party ownership transfer.
- Direct trade design doc plus safe MVP implementation.
- Auction house feasibility gate/checklist before implementation.

### Agent I: Exercise Catalog, Aliases, And Imports

Purpose: Make FitQuest feel familiar to users coming from Hevy, Strong, Apple Fitness, Garmin, spreadsheets, or custom gym routines.

Core feature:
- Every exercise should have a canonical FitQuest name plus aliases.
- Search and imports should match common naming variations, abbreviations, and machine names.
- Missing exercises should never become a dead end: users can create private entries or submit public catalog suggestions.

Catalog model additions:
- Canonical exercise name.
- Aliases, abbreviations, and alternate spellings.
- Equipment and machine station id.
- Primary and secondary muscles.
- Movement pattern.
- Tracking type.
- Source: bundled, curated, cloud approved, private custom, imported.
- Public visibility/moderation status for community entries.

Examples:
- `DB Incline Press` -> `Dumbbell Incline Bench Press`
- `BB RDL` -> `Barbell Romanian Deadlift`
- `Lat Pulldown` / `Wide Grip Pulldown` / `Cable Pulldown` -> related pulldown entries
- `StairMaster` / `StepMill` / `Stair Climber` -> cardio machine tracking
- `Adductor` / `Hip Adduction` / `Inner Thigh Machine` -> Hip Adduction Machine entries

Search/import behavior:
- Normalize case, punctuation, hyphens, slashes, and plural forms.
- Expand abbreviations such as DB, BB, KB, EZ, RDL, OHP, AMRAP, EMOM.
- Search names, aliases, muscles, equipment, machine labels, and tracking type.
- Show "Did you mean?" close matches when exact matching fails.
- Offer "Create custom exercise" and "Submit to public catalog" from empty results.
- Support multi-routine screenshot import for trainer plans that are not exposed through external APIs.
- Split one screenshot into separate training templates when multiple routine headers/cards are detected.
- Use prior workout history to suggest starting weights for imported routines, matching by canonical exercise/alias and recent best working sets.
- Let AI propose weights, sets, reps, and notes, but require a preview/edit step before saving.

Coverage audit:
- Add a local script that checks bundled exercises, custom guides, machine catalog mappings, cloud catalog shape, and alias lists.
- Report machines with no exercises.
- Report exercises with no tracking type.
- Report exercises with no muscles or equipment.
- Report duplicate canonical names.
- Report likely missing aliases.
- Report cardio/flexibility/bodyweight exercises that would be logged with the wrong fields.

Implementation tasks:
- Add alias data model and resolver.
- Add curated alias seed list for common workout-app naming.
- Upgrade exercise search to score aliases and abbreviations.
- Add import-name matching helper for future Hevy CSV/text import.
- Add multi-routine screenshot parser that returns `List<ImportedWorkout>` instead of forcing one workout per image.
- Add historical-weight resolver that looks up recent logged sets, best working sets, estimated 1RM, and tracking type for matched exercises.
- Add AI-assisted weight suggestion prompt that explains the selected historical basis.
- Add import preview UI so users can accept, edit, remove, split, or merge routines before saving.
- Add catalog coverage audit script.
- Add admin/moderation guidance for approved cloud catalog entries.

Dependencies:
- Exercise tracking type system.
- Online/community catalog service.
- Exercise Library screen.

Deliverables:
- `ExerciseAliases.kt`
- `ExerciseImportMatcher.kt`
- `MultiRoutineImageImport`
- `HistoricalWeightSuggestion`
- `scripts/audit_exercise_catalog.*`
- Alias-aware search in picker and library
- Import preview with AI-suggested weights and user approval
- Coverage audit report format
- Initial common alias pack

### Agent J: Feedback, Bugs, And Feature Requests

Purpose: Give users a safe, easy way to tell us what is broken, confusing, missing, or worth building next.

Core feature:
- Add an in-app feedback and submission hub for bugs, feature requests, workout corrections, exercise/catalog corrections, equipment/machine submissions, account issues, watch sync problems, and general comments.
- Attach useful diagnostic context automatically so reports are actionable.
- Keep personal health/workout data private unless the user explicitly includes it.

Feedback categories:
- Bug report.
- Feature request.
- Workout template suggestion.
- Workout logging issue.
- Missing exercise.
- Exercise or machine missing.
- New equipment or machine submission.
- Exercise tracking type wrong.
- Exercise instructions/images wrong.
- Watch / phone sync issue.
- Account / username issue.
- UI/layout problem.
- Balance/reward feedback.
- General feedback.

Submission fields:
- Category.
- Title.
- Description.
- Related exercise id/name when applicable.
- Related equipment/machine id/name when applicable.
- Suggested muscles, tracking type, equipment, and instructions for catalog submissions.
- Suggested workout goal, class/theme, exercises, sets, reps, time, distance, and difficulty for workout submissions.
- Optional screenshot attachment.
- Optional current screen/route.
- Optional device/app diagnostics.
- Optional contact permission for follow-up.
- User id/account id when signed in.
- App version, build type, device model, Android version, Wear OS version when available.
- Status: new, triaged, planned, in progress, fixed, closed, duplicate.

Implementation tasks:
- Add Feedback screen reachable from Settings and possibly Training/Hero help menus.
- Add quick submission entry points from Exercise Library, exercise detail, workout builder, workout logger, equipment/machine screens, and empty search states.
- Add Firestore collection for feedback submissions.
- Add Firestore security rules so users can create feedback and read only their own reports if we expose status.
- Add local fallback queue if offline.
- Add quick feedback prompts from error states, empty search results, failed watch sync, and catalog suggestion failures.
- Add submission templates for bug, feature, workout, exercise, and equipment/machine requests so users do not need to start from a blank form.
- Add moderation statuses for public catalog/workout/equipment submissions before they become visible to all users.
- Add admin/triage data shape for future moderation tools.

Privacy rules:
- Do not auto-send workout logs, health metrics, heart rate, screenshots, email, or location without clear user consent.
- Allow users to submit anonymously if signed out, but prefer account-linked reports when signed in.
- Redact tokens, API keys, auth details, and local file paths.

Deliverables:
- `FeedbackSubmission` model.
- `CatalogSubmission` or typed feedback payload for exercises, workouts, and equipment/machines.
- Feedback screen/dialog.
- Firestore write path and rules.
- Offline retry queue.
- Settings entry point.
- Error-state "Report issue" buttons.
- Exercise/workout/equipment submission entry points.
- Triage status schema.

### Agent K: What's New, Release Notes, And Player Messaging

Purpose: Help users understand what changed after updates so new features, fixes, balance changes, exercises, machines, and rewards do not go unnoticed.

Core feature:
- Add a user-facing What's New screen for major app updates.
- Show concise release cards for new features, improvements, bug fixes, new exercises, new equipment/machines, new gear, new biomes, and balance changes.
- Track which release notes the user has already seen.
- Allow important notes to be highlighted after app launch without becoming annoying.

Content types:
- New feature.
- Improvement.
- Bug fix.
- Balance change.
- New exercise.
- New equipment or machine.
- New workout template.
- New gear/cosmetic/reward.
- Watch app update.
- Known issue.

Implementation tasks:
- Add release note data model with version, title, body, category, date, priority, optional image/icon, and optional route/deep link.
- Store bundled release notes locally first; later support Firestore/Remote Config release notes for server-side announcements.
- Add What's New screen reachable from Settings.
- Add one-time "What's New" modal or sheet for major updates only.
- Add unread/updated indicator in Settings.
- Add optional deep links from release notes to relevant screens.
- Add admin-friendly release note format so future updates can be copied into the public roadmap or Google Site.

Privacy and UX rules:
- Do not show a disruptive modal for every small patch.
- Let users dismiss release notes.
- Keep known issues honest and short.
- Avoid overpromising release dates.

Deliverables:
- `ReleaseNote` model.
- What's New screen.
- Seen-version storage.
- Settings entry point.
- Major-update prompt.
- Optional remote release-note source.

## Recommended Build Order

### Milestone 1: Foundation Loop

Best next build target.

1. Add Forge basics: upgrade, salvage, unsocket.
2. Add Trait model and simple trait aggregation.
3. Add early progression pacing rules so Tier 3+ gear and later biomes require real milestones.
4. Upgrade shop/inventory/armory browsing so gear, materials, chests, and forge actions are not long static lists. `Initial grid/filter/detail implementation complete.`
5. Add shared reward batch/reveal model so every system can show what was earned. `Foundation implemented with RewardBatch, RewardRevealDialog, and initial workout/battle/chest/idle/shop/raid wiring.`
6. Add Movement Mastery DB and passive perk unlocks.
7. Add a small Mastery UI section.
8. Add material rewards to weekly campaigns.
9. Add catalog aliases and coverage audit so exercise discovery stays trustworthy as content grows.
10. Add in-app feedback and bug reporting so testers can report missing exercises, tracking mistakes, and watch sync issues quickly.
11. Add workout/equipment submission templates so users can suggest missing machines, exercises, and routines in a structured way.
12. Add a What's New screen so players can see major new features, fixes, balance changes, exercises, and equipment.

Why first:
- Makes materials useful immediately.
- Makes workouts more meaningful.
- Prevents high-tier gear from arriving before players feel attached to early gear.
- Makes rewards clear and satisfying before adding more reward types.
- Makes gear management readable before adding more loot, traits, cosmetics, and crafting recipes.
- Creates the foundation for skill trees, relics, and ascension.

### Milestone 2: Build Identity

1. Add starter skill trees for Warrior, Mage, Ranger, and one new class.
2. Add skill point earning and respec.
3. Add trait display on Hero and gear screens.
4. Add first relic items.
5. Add Hevy-style import matching for routines and custom exercise names.
6. Add gear set bonuses and item traits that reinforce class builds.
7. Add visual metadata to gear, weapons, relics, and class items.
8. Add the first layered hero avatar renderer.
9. Add chest opening and offline reward reveal flows.
10. Refresh bottom navigation icons and reusable screen headers.

### Milestone 3: World Depth

1. Add biome mastery.
2. Add biome layers.
3. Add boss unlocks.
4. Add biome-specific loot.
5. Add boss-gated zone unlocks and milestone reward guarantees.
6. Add boss and biome cosmetic unlocks.
7. Add boss coffer and biome chest themes.
8. Add first home base biome decor rewards.

### Milestone 4: Infinite Loop

1. Add ascension rules.
2. Add ascension upgrades.
3. Add ascension UI.
4. Add abyss scaling.
5. Add ascension cosmetic frames, auras, and titles.
6. Add ascension home base upgrades and permanent room trophies.

### Milestone 5: Retention Polish

1. Add quest chains.
2. Add seasonal/weekly campaigns.
3. Add more trophies.
4. Add cosmetic inventory and equip polish.
5. Add reward reveal animations.
6. Add more avatar/cosmetic rewards.
7. Add home base editor polish and seasonal decor.

## Parallelization Notes

Agents can work in parallel if they respect these boundaries:

- Agent A owns movement mastery data and perk definitions.
- Agent B owns trait models and combat integration.
- Agent C waits for Agent B before wiring skill effects.
- Agent D can start immediately because materials and gear instances already exist.
- Agent D2 depends on Agent D's forge methods and should stay aligned with Agent G4's future reward reveal/chest opening work.
- Agent E can design biome data while Agent B builds traits.
- Agent F should wait until reset/preserve rules are finalized.
- Agent G waits for Agent B's trait registry.
- Agent G2 should coordinate with Agent D, Agent E, Agent G, and Agent H because pacing touches gear drops, forge sinks, boss gates, biome layers, and campaign rewards.
- Agent G3 should coordinate with Agent G for relic visuals, Agent G2 for milestone rewards, Agent H for quest/campaign unlocks, and Agent F for ascension cosmetics.
- Agent G4 should coordinate with all reward-producing agents because it owns how rewards are represented and revealed to users.
- Agent G5 should wait for Agent G3's cosmetic ownership model and Agent G4's reward reveal model before building the room editor.
- Agent H should integrate after the first versions of mastery, forge, and biomes exist.
- Agent H2 can start from the existing `PartyService` and `ActiveSessionScreen`; it must keep Firestore writes event-based and privacy-aware.
- Agent I can start immediately now that tracking type exists; it should coordinate with Agent A for mastery naming and Agent H for quest/import rewards.
- Agent J can start immediately and should coordinate with Agent I for catalog correction reports and with Wear/HR work for diagnostics.
- Agent K can start immediately with local bundled release notes, then coordinate with Agent J and the public roadmap before adding remote announcements.

## First Concrete Tickets

### 1. Forge MVP `[~] Foundation`

- `[x]` Add gear salvage. `Foundation implemented in GameRepository.salvageGearInstance.`
- `[x]` Add gear upgrade level and improved instance stats. `Foundation implemented in GearInstanceEntity and GameRepository.upgradeGearInstance.`
- `[x]` Spend materials and gold for upgrades.
- `[~]` Build Forge screen. `Initial Forge tab integrated into ShopScreen with upgrade/salvage/fusion entry points.`
- `[ ]` Add unsocket/refine/craft recipe actions.
- `[ ]` Add clear upgrade cost preview and valuable-item confirmation.

### 2. Shop, Inventory, And Armory MVP `[~] Foundation`

- `[x]` Replace long shop/inventory lists with grid-based browsing. `Implemented in ShopScreen.`
- `[x]` Add filters for item type and equipment slot. `Implemented in ShopScreen.`
- `[x]` Add sorting by tier, power, slot, price, and quantity. `Implemented in ShopScreen.`
- `[x]` Add armory equipped-slot summary. `Implemented in ShopScreen.`
- `[x]` Add item detail sheet with buy, equip, use, open, sell, upgrade, and salvage actions. `Implemented in ShopScreen.`
- `[x]` Show required upgrade material names in item detail sheets.
- `[x]` Show who can equip gear in item detail sheets.
- `[~]` Convert Hero gear slot picker away from a long list. `Initial compact grid implemented; still needs filter/sort/detail parity with the main inventory surface.`
- `[ ]` Add compare-to-equipped.
- `[ ]` Add richer locked-tier messaging.

### 3. Reward Reveal And Chest Opening MVP `[~] Foundation`

- `[x]` Add a shared reward batch model. `Implemented with RewardBatch, Reward, RewardSource.`
- `[~]` Show exact item names, quantities, source, and icons. `Implemented for most rewards; rarity/tier/source display still needs richer UI.`
- `[x]` Replace vague offline loot messaging with a detailed claim modal. `Idle loot is now stored pending and revealed on claim; older pre-claim modal still needs visual consolidation.`
- `[~]` Add a chest opening modal with reveal animation and summary grid. `Inventory chest opens now use RewardRevealDialog; a dedicated chest-first animation is still pending.`
- `[ ]` Add equip, compare, salvage, view inventory, and open another actions where practical.
- `[~]` Route workout completion, idle claims, and inventory chest opens through the same reward reveal component. `Initial routing exists for workout, battle, shop chest opens, idle claim, hero, rivals, and guild raid.`
- `[~]` Remove or simplify duplicate idle reward surfaces so offline rewards do not show an estimated chest summary before the exact reveal. `Inaccurate gear estimate removed; one-step polish remains.`
- `[ ]` Add a distinct chest opening source step that shows the chest itself before revealing contents.
- `[ ]` Add tests for Reward/LootResult flattening, including nested chest contents and empty reward batches.

### 4. Progression Pacing MVP `[~] Foundation`

- `[x]` Add Tier 3+ gear lock rules. `Foundation implemented.`
- `[x]` Reduce high-tier random drops in early zones. `Foundation implemented for workout, battle, moment, chest, ambush, and guild reward paths.`
- `[~]` Add boss gate requirements before the next biome unlocks. `Biome boss state implemented; travel lock UI/rules still pending.`
- `[ ]` Add guaranteed milestone rewards for first boss clears.
- `[ ]` Add a balance test for expected gear tier after one week of normal play.

### 5. Biome Layer Prototype `[ ] Todo`

- `[ ]` Add layer value to biome travel/combat.
- `[ ]` Generate stronger monsters per layer.
- `[ ]` Add first boss gate.

### 6. Movement Mastery MVP `[ ] Todo`

- `[ ]` Add mastery table.
- `[ ]` Grant mastery XP from completed workout exercises.
- `[ ]` Unlock 5 starter perks.
- `[ ]` Display mastery on exercise detail.

### 7. Trait MVP `[ ] Todo`

- `[ ]` Add trait enum.
- `[ ]` Add trait list to combat stat calculation.
- `[ ]` Support damage reduction, bonus damage, lifesteal, and starting shield.

### 8. Skill Tree Prototype `[ ] Todo`

- `[ ]` Build one Warrior tree.
- `[ ]` Store unlocked nodes.
- `[ ]` Apply node traits in combat.

### 9. Visual Identity And Cosmetics Foundation `[ ] Todo`

- `[ ]` Add cosmetic slots and equipped cosmetic storage.
- `[ ]` Add visual asset metadata to gear and weapons.
- `[ ]` Build a layered hero preview from base, armor, weapon, aura, and frame layers.
- `[ ]` Create one complete themed cosmetic set.
- `[ ]` Add style rules for future generated/imported art.

### 10. Home Base MVP `[ ] Todo`

- `[ ]` Add home decor catalog and owned/equipped storage.
- `[ ]` Build a fixed-slot room screen with a few furniture/decor slots.
- `[ ]` Add first Meadowlands decor set.
- `[ ]` Let trophies, boss clears, and mastery rewards appear in the room.

### 11. Exercise Catalog Trust MVP `[ ] Todo`

- `[ ]` Add aliases and abbreviation expansion.
- `[ ]` Make search and future imports use canonical matching.
- `[ ]` Add "Did you mean?" fallback.
- `[ ]` Add multi-routine screenshot import that can split one image into several training templates.
- `[ ]` Add imported-routine preview cards with split, merge, edit, and remove controls.
- `[ ]` Add historical-weight suggestions for imported routines using previous logged workouts.
- `[ ]` Show why a weight was suggested, such as "based on your recent 3x10 at 135 lb" or "estimated from best 1RM."
- `[ ]` Require user approval before saving AI-suggested weights.
- `[ ]` Add a catalog audit script for missing tracking types, muscles, equipment, aliases, and machine mappings.

### 12. Feedback MVP `[ ] Todo`

- `[ ]` Add feedback form with category, title, description, and optional diagnostics.
- `[ ]` Write submissions to Firestore.
- `[ ]` Add entry points from Settings and catalog empty states.
- `[ ]` Add privacy copy and a diagnostics opt-in toggle.
- `[ ]` Add bug, feature, workout, exercise, and equipment/machine submission templates.
- `[ ]` Add entry points from Exercise Library, exercise detail, workout builder/logger, and equipment/machine views.
- `[ ]` Add moderation status fields for community-visible workout, exercise, and equipment submissions.

### 13. What's New MVP `[ ] Todo`

- `[ ]` Add release note model with version, date, category, title, summary, priority, and optional destination route.
- `[ ]` Add local bundled release notes source.
- `[ ]` Add What's New screen reachable from Settings.
- `[ ]` Track the latest release note/version the user has seen.
- `[ ]` Show a dismissible major-update sheet only for important releases.
- `[ ]` Add categories for new features, improvements, bug fixes, balance changes, new exercises, new equipment, workout templates, gear/cosmetics, watch updates, and known issues.

- Add categories for new features, improvements, bug fixes, balance changes, new exercises, new equipment, workout templates, gear/cosmetics, watch updates, and known issues.

### 14. Live Party Workout MVP `[ ] Todo`

- `[ ]` Add live activity fields to party members.
- `[ ]` Publish workout started, set logged, rest started, and workout finished events.
- `[ ]` Add a compact Party Battle Log card to active workouts.
- `[ ]` Show shared boss HP during party workouts.
- `[ ]` Add a privacy setting for live workout visibility.

### 15. Player Cards And Social Management MVP `[ ] Todo`

- `[ ]` Add public player card/profile data with privacy controls.
- `[ ]` Open player cards from Party, Guild, Rivals, and leaderboards.
- `[ ]` Show class/job levels, public avatar, guild/party status, achievements, and visible gear/cosmetics.
- `[ ]` Add invite actions from player cards: invite to party, invite to guild, follow/request later.
- `[ ]` Add report/block actions to player cards.
- `[ ]` Add guild ranks: owner, officer, veteran/member, recruit.
- `[ ]` Add permission flags for guild invites, kicks, join approvals, announcements, and event starts.
- `[ ]` Add party ownership transfer and optional party officer permissions.
- `[ ]` Add audit log events for social permission changes.

### 16. Player Trading And Auction House Feasibility `[ ] Todo`

- `[ ]` Define tradable vs account-bound item categories.
- `[ ]` Add item binding rules for equipped/upgraded/high-tier/quest/cosmetic items.
- `[ ]` Design direct trade request and two-sided escrow confirmation flow.
- `[ ]` Add trade history/audit logs.
- `[ ]` Add anti-abuse gates: minimum level/account age, cooldowns, taxes/fees, suspicious-trade logging.
- `[ ]` Prototype direct trading before any auction house work.
- `[ ]` Only build auction house after direct trading, item binding, economy sinks, and moderation tools are proven.

### 17. YT Music / Media Integration MVP `[ ] Todo`

- `[ ]` Implement `MediaSession` listener to detect active YT Music/Spotify sessions.
- `[ ]` Build a compact controller card for the Active Session screen.
- `[ ]` Add transport controls (play/pause/skip).

### 18. Daily Body Metrics & Health Sync `[ ] Todo`

- `[ ]` Implement daily weight entry quest/dialog.
- `[ ]` Add `BodyMetric` history storage.
- `[ ]` Enhance Health Connect sync to automatically pull Fitbit/Health data on app launch.
- `[ ]` Add a Weight History chart to the Progress tab.

### 19. Local AI Moments And Noise Controls `[ ] Todo`

- `[ ]` Add global AI Moments master toggle.
- `[ ]` Add Quiet / Balanced / Immersive AI mode selector.
- `[ ]` Add per-feature toggles for coaching, narration, journal, briefings, swaps, import cleanup, gear flavor, player summaries, and feedback cleanup.
- `[ ]` Add Workout Bard / Session Narrator cards with short, skippable text.
- `[ ]` Add Adaptive Workout Coach suggestions with explicit user approval.
- `[ ]` Add Exercise Substitution Engine using equipment, muscles, and tracking type.
- `[ ]` Expand Immersive Routine Renaming for imports and manual rename actions.
- `[ ]` Add Personalized Daily Quest Briefing on dashboard/app open.
- `[ ]` Add Battle Commentary with cached short lines.
- `[ ]` Add Smart Import Cleanup for exercise normalization and duplicate detection.
- `[ ]` Add AI-assisted weight suggestions during import based on the user's previous workout history.
- `[ ]` Add Lore Journal entries for sessions, PRs, bosses, loot, travel, and streaks.
- `[ ]` Add NPC Coach Personality presets.
- `[ ]` Add Readiness Explanation text after readiness rules exist.
- `[ ]` Add Gear Flavor Text Generator and cached item lore.
- `[ ]` Add Custom Monster/Boss Generation constrained by deterministic game stats.
- `[ ]` Add Player Card AI Summaries using only public/privacy-allowed fields.
- `[ ]` Add Natural Language Workout Builder offline/local path.
- `[ ]` Add Bug/Feedback Summarizer before feedback submission.

## Product Risk Notes

- Avoid too many currencies at once. Use materials, gold, skill points, and ascension essence only.
- Avoid punishing real-world beginners. Mastery should reward consistency, not only heavy weight.
- Ascension must never delete PR history, username, cosmetics, or exercise catalog contributions.
- Skill trees should create interesting choices but not require perfect builds to enjoy workouts.
- Dynamic monsters should scale gently; players should still feel stronger after upgrades.
- Tier 4+ gear should be a chapter reward, not an early random outcome from normal play.
- Boss gates should create anticipation without blocking beginners who are still building the workout habit.
- Procedural biome layers should extend fixed biomes, not replace named zones with generic randomness.
- Cosmetics will feel weak until the avatar renderer supports visible layers, so avoid rewarding cosmetic labels before the art can actually be seen.
- Cosmetic ownership should stay separate from combat power so players can keep a favorite look while changing gear.
- Generated or community-submitted visuals must pass style, aspect ratio, transparency, and moderation checks before becoming public rewards.
- Reward reveals must stay accurate; never show a flashy animation that hides exactly what changed in inventory, XP, gold, gear, or progression.
- Chest opening and reward animations should be skippable or fast-forwardable so frequent players are delighted, not delayed.
- Home base decor should be cosmetic and expressive first; avoid turning it into another required combat optimization grid too early.
- Shop and inventory filters should reduce decision fatigue; avoid adding so many chips that browsing becomes another chore.
- Gear actions must clearly prevent accidental loss; salvage/sell should stay disabled for equipped gear or use confirmation for valuable items.
- Imported exercises should preserve the user's language where possible while still mapping to a clean canonical FitQuest exercise behind the scenes.
- Community catalog approvals must require tracking type, muscles, equipment/machine, and instructions so public entries do not feel out of place.
- Feedback reporting should help debugging without silently collecting sensitive health, account, or location data.
- Live party workout updates should feel motivating, not surveillant; keep write frequency low and give users clear visibility controls.

### Agent L: Media & Immersion

Purpose: Improve the training atmosphere with integrated media controls.

Core feature:
- Add YT Music / Media Session controls directly into the `ActiveSessionScreen`.
- Support play/pause, skip, and current track display without leaving the app.

Implementation tasks:
- Add `MediaSession` listener service.
- Build a compact `MediaControllerCard` for the workout logger.
- Support basic transport controls (next/prev/play).
- Add optional "Auto-start playlist" setting when beginning a workout.

Deliverables:
- `MusicService.kt`
- `MediaControllerCard` component.
- Active Session integration.

### Agent M: Lifestyle, Health, and Body Metrics

Purpose: Deepen the connection between real health data and the hero's physical stats.

Core feature:
- Automate weight/height sync from Health Connect (Fitbit/Google Fit).
- Add a "Weight In" daily quest/interaction.
- Track body metrics history alongside workout history.

Implementation tasks:
- Add `BodyMetricEntity` for history tracking.
- Add "Log Weight" action to the Hero screen or Quest board.
- Implement background Health Connect sync for Weight/Height.
- Add weight-based scaling for bodyweight exercise XP (heavier = slightly more XP for the same reps).

Deliverables:
- `BodyMetricEntity`
- Weight input UI.
- Automated Health Connect sync workers.
- Weight history chart.

### Agent N: On-Device Local AI Inference

Purpose: Replace cloud-based Gemini API with a local LLM model for privacy, offline use, and zero rate limits.

Core feature:
- Download and run small compatible on-device models using the active local inference engine.
- Support a FitQuest-hosted recommended model so normal users can install one blessed model without hunting through external model pages.
- Support a Play Store on-demand asset pack for the official model, with hosted URL downloads retained as a fallback for sideload/dev builds.
- Keep MediaPipe GenAI as the native Android release path for `.bin` / `.task` models, then add a separate native llama.cpp / GGUF runtime later for broader model choice.
- Use local models for workout generation, battle narration, and exercise mapping.

Implementation tasks:
- Integrate a local inference library (e.g., Llama.cpp JNI or MediaPipe GenAI).
- Add a runtime abstraction so FitQuest can choose `MediaPipeLocalEngine` for MediaPipe models and a future `LlamaCppLocalEngine` for `.gguf` models without rewriting AI features.
- Implement background model downloading, resume support, expected-size validation, and SHA-256 integrity checking.
- Add build-time hosted model config through `FITQUEST_LLM_MODEL_URL`, `FITQUEST_LLM_MODEL_VERSION`, `FITQUEST_LLM_MODEL_SIZE_MB`, `FITQUEST_LLM_MODEL_BYTES`, and `FITQUEST_LLM_MODEL_SHA256`.
- Add `local_ai_model` Play Asset Delivery pack and runtime provider that requests, observes, removes, and locates the model file.
- Add model selection and management in Settings.
- Fallback to rule-based generation if no model is downloaded.
- Avoid migrating FitQuest to React Native just to use `llama.rn`; if GGUF support is needed, use native llama.cpp/JNI or a Kotlin-compatible Android wrapper.

Deliverables:
- `LocalAiEngine` implementation using real inference.
- `LocalAiRuntime` interface plus MediaPipe implementation.
- Future `LlamaCppLocalEngine` implementation for GGUF models if MediaPipe model availability remains too limited.
- `LocalModelDownloader` with curated model support, hosted model support, resumable downloads, and integrity checks.
- `PlayAssetModelProvider` for Google Play on-demand model installation.
- AI Model management UI in Settings with a recommended hosted model path when configured.

Runtime decision:
- `[now]` MediaPipe GenAI: best fit for current native Kotlin app and Play Asset Delivery release path.
- `[later]` Native llama.cpp/GGUF engine: broader model ecosystem, better community model availability, higher integration/testing cost.
- `[avoid]` `llama.rn` direct dependency: useful for React Native apps, but adds unnecessary React Native infrastructure to FitQuest.

Play Asset Delivery operational status:
- `[x]` Official `local_ai_model` pack is present in release AABs and verified in Play Console as on-demand.
- `[x]` Physical-device download verified after refreshing stale Play Store listing metadata.
- `[x]` Runtime shows byte-based progress, transferred size, status/error codes, and handles Play confirmation states.
- `[ ]` Add an automated release check that fails collection when `local_ai_model` is missing from the phone AAB.
- `[ ]` Add a small in-app troubleshooting action that opens the FitQuest Play listing when Play reports `PACK_UNAVAILABLE`.
- See `docs/LOCAL_AI_AND_PLAY_ASSET_DELIVERY.md` for the release checklist and verified recovery sequence.

### Agent N2: Local AI Moments, Coaching, And User Noise Controls

Purpose: Use local LLMs for frequent, personal, low-cost AI moments while letting users choose how quiet or expressive the app should be.

Core principle:
- AI should add delight and usefulness without interrupting workouts.
- Any generated text that appears during training should be short, skippable, and optional.
- Deterministic app logic must remain authoritative for XP, rewards, PRs, equipment, safety, and history. AI can explain or suggest, not secretly change important state.

Global controls:
- Add an `AI Moments` settings section.
- Master toggle: `AI Moments On/Off`.
- Mode selector:
  - `Quiet`: only explicit button taps use AI.
  - `Balanced`: post-session summaries, swaps, and occasional quest flavor.
  - `Immersive`: battle commentary, journal entries, daily briefings, and extra flavor.
- Per-feature toggles for coaching, battle narration, journal, quest briefings, gear flavor, player summaries, import cleanup, and feedback cleanup.
- Add "Show less like this" or "Mute this type" actions on generated cards.
- Add a low-power guard so local inference can pause on low battery, hot devices, active workouts, or low storage.

Implementation difficulty:

| Feature | Difficulty | Notes |
| --- | --- | --- |
| Workout Bard / Session Narrator | Low-Medium | Mostly prompt + UI card after sessions/sets. Keep short and skippable. |
| Adaptive Workout Coach | Medium-High | Needs guardrails, set-history analysis, and clear "suggestion only" UI. High player value. |
| Exercise Substitution Engine | Medium | Existing swap path helps. Needs equipment/muscle/tracking constraints. |
| Immersive Routine Renaming | Low | Foundation exists. Expand to batch import and manual rename actions. |
| Personalized Daily Quest Briefing | Medium | Needs recent sessions, goals, readiness, and quiet notification/sheet behavior. |
| Battle Commentary | Medium | Easy generation, but should be templated/cached so battles stay fast. |
| Smart Import Cleanup | Medium-High | High value for Hevy/Strong migration. Needs deterministic matching with AI assistance. |
| Lore Journal | Medium | Store generated summaries by day/session; avoid over-writing history. |
| NPC Coach Personalities | Low-Medium | Mostly prompts + settings; benefits from consistent tone presets. |
| Readiness Explanation | Medium | Needs readiness rules first; AI explains rule output, not invents medical advice. |
| Gear Flavor Text Generator | Low-Medium | Great for loot dopamine. Cache per item instance/template. |
| Custom Monster / Boss Generation | Medium | Needs moderation/style constraints and deterministic stats from game rules. |
| Player Card Summaries | Medium | Depends on public profile/privacy model. AI summarizes allowed public fields only. |
| Natural Language Workout Builder | Medium | Existing AI workout generation path; local model quality and parsing are the challenge. |
| Bug/Feedback Summarizer | Low-Medium | Useful for submissions. Runs locally before sending optional report. |

Feature backlog:
- Workout Bard / Session Narrator: generate brief RPG narration after a set, PR, or completed session.
- Adaptive Workout Coach: suggest next-set weight/reps/volume adjustments based on logged performance, RIR/RPE, and recent history.
- Exercise Substitution Engine: suggest available alternatives when equipment is busy or an exercise hurts/does not fit.
- Immersive Routine Renaming: rename imported or generic routines into FitQuest quest names.
- Personalized Daily Quest Briefing: summarize today's recommended training focus, recovery state, streak, and suggested quest.
- Battle Commentary: generate short turn summaries, crit descriptions, boss taunts, victory/loss epilogues, and class-flavored attacks.
- Smart Import Cleanup: classify imports, normalize exercise names, detect duplicate routines, and flag unknown equipment/machines.
- Import Weight Suggestions: use matched exercise history to propose safe starting weights and explain the source, while requiring user approval before saving.
- Lore Journal: write a private training chronicle from sessions, PRs, bosses, gear, travel, and streaks.
- NPC Coach Personalities: let users choose a coaching style such as Guildmaster, Warrior Captain, Arcane Scholar, Druid Healer, or Quiet Trainer.
- Readiness Explanation: explain "push / normal / deload" using recent training load and recovery rules.
- Gear Flavor Text Generator: generate item lore, rarity callouts, origin text, and class-themed descriptions.
- Custom Monster / Boss Generation: create themed monsters from workout focus, biome, class, and recent progress.
- Player Card Summaries: summarize public hero identity, class/job progress, and teammate fit while respecting privacy.
- Natural Language Workout Builder: build workouts from prompts fully offline when a local model is installed.
- Bug/Feedback Summarizer: turn messy feedback into category, title, repro steps, and optional diagnostics preview.

Implementation tasks:
- Add `AiMomentSettings` to preferences.
- Add `AiMomentType` enum and per-feature toggles.
- Add a shared `AiMomentService` that routes cloud/local/heuristic generation and caches output.
- Add short prompt templates for each feature with strict output length and safety rules.
- Add generated-content cache keys so repeated screens do not regenerate constantly.
- Add "quiet mode" suppression to active workout, battle, reward, and dashboard surfaces.
- Add tests for prompt routing, fallback behavior, and settings suppression.

Dependencies:
- Agent N local inference must use a working model/runtime pair.
- Readiness explanations depend on Recovery/Readiness rules.
- Player card summaries depend on public profile/privacy controls.
- Import cleanup depends on Exercise Catalog aliases and canonical matching.

Deliverables:
- `AiMomentSettings`
- `AiMomentType`
- `AiMomentService`
- AI Moments settings UI
- Toggle-aware cards for narrator, journal, briefings, swaps, coaching, gear lore, battle commentary, and feedback cleanup

### Agent O: Advanced Workout Logging (Supersets & Drop Sets)

Purpose: Support advanced training techniques used by experienced lifters to increase intensity and efficiency.

Core feature:
- Tag sets with types: Normal, Warm-up, Drop Set, Failure, Top Set, Back-off.
- Support Supersets, Giant Sets, and Circuits by grouping exercises together.
- Adjust RPG rewards based on set intensity (e.g., Failure/Drop Sets grant more XP but cost more Energy).

Implementation tasks:
- Add `SetType` enum and update `SetLogEntity`.
- Refactor `WorkoutExerciseEntity` and `SessionExercise` to support exercise grouping (Superset ID).
- Update `ActiveSessionScreen` UI to show grouped exercises and allow set tagging.
- Update `GameMath` to scale rewards based on set type and grouping density.

Deliverables:
- `SetType.kt` domain model.
- Grouping-aware DB schema updates.
- Refined Active Session UI with superset visual indicators.
- Logic for "Density XP" (bonus for short rest/supersets).
