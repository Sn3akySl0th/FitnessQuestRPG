# FitQuest UI/UX QA Report
**Reviewed against:** Current `main` codebase  
**Scope:** All 12 screens + key reusable components  
**Focus:** Truncation, padding, alignment, touch targets, overflow, responsive behavior

---

## Severity Legend
- 🔴 **Critical** — Visible breakage / data hidden / unusable
- 🟠 **High** — Noticeable defect that degrades UX
- 🟡 **Medium** — Cosmetic issue or edge-case defect
- 🟢 **Low** — Polish / nice-to-have improvement

---

## 1. HeroScreen (`HeroScreen.kt`)

### 🔴 [H-1] Currency Bar — Items Clipped on Small Screens  
**Location:** `HeroCurrencyBar` (L819–845)  
The currency Row uses `horizontalScroll` but the Row itself is inside a `padding(horizontal = 16.dp, vertical = 10.dp)` — this is fine, but the tokens (`FantasyToken`) have no `minWidth`, so on small phones (360dp width) when all five tokens are visible (Gold + Energy + Streak + Watch + steps), the scroll container provides no visual affordance that it's scrollable. Users will not know to swipe.  
**Fix:** Add a `FadingEdge` gradient on the trailing side, or reduce token padding to prevent overflow.

### 🟠 [H-2] Header Banner — Name Overflow on 2-Word Long Names  
**Location:** `HeroHeaderBanner` (L643–657)  
The dynamic `nameFontSize` steps are `> 14 chars → 18.sp`, `> 9 → 21.sp`, else `26.sp`. With `maxLines = 2`, a name like "Warrior_King" (12 chars) renders at 21.sp in the 55% width Column. When the Druid form chips row is visible directly beneath (L721–777), the whole column becomes vertically crowded and the "📸 Wallpaper / ⌚ Watch Face" TextButton row (height: 26.dp) may be clipped by the `heightIn(max = 280.dp)` banner constraint on small phones.  
**Fix:** Move the export buttons to a secondary row, or collapse them into a single `More` icon menu.

### 🟠 [H-3] Header Banner — Settings Button Alignment  
**Location:** L793–795  
```kotlin
Box(Modifier.align(Alignment.Top)) { SettingsIconButton() }
```
The `Box` is inside the `Row` at L616, which is `verticalAlignment = Alignment.CenterVertically`. The inner `align(Alignment.Top)` only works inside a `Box` scope — inside a `Row` it has no effect. The settings button actually aligns to **center**, not top-right corner. This means the gear icon appears mid-banner rather than anchored to the status bar area.  
**Fix:** Wrap the outer `Box` in `Modifier.align(Alignment.Top)` within the banner `Box`, not inside the inner Row.

### 🟠 [H-4] Stat Allocation Buttons — Text Too Cramped  
**Location:** L915–922  
```kotlin
Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
    Text("+$stat", fontSize = 12.sp)
}
```
Four buttons split evenly at `contentPadding = 0.dp` with 8.dp spacing → each button is ~(screenWidth − 32 − 24) / 4 ≈ 68–75dp wide. At 12.sp, "+STR" fits, but the button touch target is effectively the Material3 minimum (48dp tall by default), despite being `weight(1f)`. With `contentPadding = PaddingValues(0.dp)`, the text hugs the button edges — visually looks like it might truncate.  
**Fix:** Increase `contentPadding` to at least `PaddingValues(horizontal = 4.dp, vertical = 4.dp)` and raise font to 13.sp.

### 🟡 [H-5] Skills Tab — No Padding at Bottom  
**Location:** `sagaTab` → `SagaTabContent`  
The `LazyColumn` in portrait already adds `Spacer(Modifier.height(80.dp))` at the bottom (L575), but the content inside `SagaTabContent` is itself a `Column`, not a LazyList item. On phones with taller navigation bars (gesture navigation), the last item in `SagaTabContent` (Recent Adventures) can sit behind the floating dock.  
**Fix:** The `Spacer(80.dp)` in the LazyColumn is correctly positioned, but verify `SagaTabContent` receives enough padding when dock + gesture bar = ~100dp total.

### 🟡 [H-6] "Switch Job" Card — Label Overflow  
**Location:** L901  
```kotlin
Text(cls.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
```
Each class card is `width(100.dp)`. Class labels like "Dragoon", "Summoner", "Wanderer" are 7–8 chars and render in `labelSmall` (≈11sp). At 100dp this is fine, but the lock overlay text "Premium" (L902) at `labelMedium` (~12sp) could overflow if the font scale is > 1.0 (accessibility).  
**Fix:** Add `overflow = TextOverflow.Ellipsis` to the "Premium" label.

### 🟡 [H-7] Mastery Tab — No Empty State  
**Location:** `masteryTab` (referenced but not shown in excerpts)  
If a new user has no movement mastery data yet, the tab will render an empty `LazyColumn` with just the dock clearance Spacer, showing a blank white void. There is no "Start training to unlock mastery" placeholder.  
**Fix:** Add an empty state Card with guidance text.

---

## 2. FightScreen (`FightScreen.kt`)

### 🟠 [F-1] Battle Arena Fixed Height — Clips on Short Phones  
**Location:** L276–279  
```kotlin
BattleArena(modifier = Modifier.fillMaxWidth().height(290.dp))
```
The arena is a fixed `290.dp`. The remaining vertical space must accommodate: monster name row, battle log (`weight(1f)`), Attack button (48dp), 3 skill buttons (~48dp), Defend/Flee row (~48dp). On a 640dp logical-height screen (e.g. Pixel 4a with navigation bar), after subtracting statusBars (~24dp) + navBars (~48dp), there is only 568dp available. `290 + 48 + 48 + 48 + spacings + padding = ~500dp` — this is tight. On devices with smaller screens (e.g. 480dp height), the battle log may collapse to zero height.  
**Fix:** Change `height(290.dp)` to `fillMaxHeight(0.42f)` so the arena scales proportionally.

### 🟠 [F-2] Monster Trait Badge — Overflows Next to Long Monster Name  
**Location:** L285–307  
```kotlin
Text("${battle.monster.name}  ·  Lv ${battle.monster.level}",
    modifier = Modifier.weight(1f, fill = false)  // ← NOT fill = true
)
Spacer(Modifier.width(8.dp))
Surface { Text("${trait.emoji} ${trait.label}") }
```
With `fill = false`, the name text will shrink to its intrinsic width, but only up to `weight(1f)`. The trait badge is outside the weight so it always gets its full size. However, if the monster name is short (e.g. "Rat"), the Row centers both elements with lots of space in between — visually unbalanced for `Arrangement.Center`. If the name is long (e.g. "Ancient Stone Golem"), the `weight(1f, fill = false)` means the name does NOT fill the remaining space, so the badge can crowd it.  
**Fix:** Use `Modifier.weight(1f)` (with `fill = true`) on the name Text and add `overflow = TextOverflow.Ellipsis`.

### 🟡 [F-3] Skill Buttons — No Labels When Disabled  
**Location:** L358–368  
`SkillButton` components render with `enabled = fighting`. When the battle ends (outcome != ONGOING), all skill buttons show in a disabled/greyed state, but their content (skill names/icons) is presumably still visible. No visual cue explains *why* they're disabled (e.g. "Battle over"). This is acceptable UX but could confuse first-time players.

### 🟡 [F-4] Victory Overlay — Narration Text Not Bounded  
**Location:** L469–477  
```kotlin
Text(""${state.narration}"",
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center
)
```
AI narration is unbounded — no `maxLines` or `overflow`. An unexpectedly long narration response could push the "✨ Claim Rewards" button below the screen. The Column uses `verticalScroll`, so it won't crash, but the button becomes unreachable until the user discovers they can scroll.  
**Fix:** Add `maxLines = 6, overflow = TextOverflow.Ellipsis` or constrain the narration inside a scrollable `Box` with a max height.

---

## 3. ActiveSessionScreen (`ActiveSessionScreen.kt`)

### 🔴 [A-1] Exercise Name — 1 Line Only in Subtitle  
**Location:** L1810–1816  
```kotlin
Text(
    "${exercise.category.label} • ${targetSummary(exercise)} • ${exercise.category.statLabel}",
    maxLines = 1, overflow = TextOverflow.Ellipsis
)
```
The subtitle concatenates category, target, and statLabel into a single line. With a long category label + target like "Strength • 4 sets × 12 reps • STR/END", this line can reach 45+ characters. At `labelSmall` (≈11sp), this is approximately 45 × 6.5px ≈ 293px, which on a 360dp (980px) screen inside the `weight(1f)` column (minus 40dp icon + 40dp controls = ~280dp ≈ 742px) does technically fit — but at font scale 1.15+ it will truncate. The ellipsis truncation will cut off the stat label silently.  
**Fix:** Either drop the stat label from this line (show it elsewhere) or split onto two lines.

### 🔴 [A-2] Cardio Distance/Time Input Row — Too Many Fields  
**Location:** L1923–1945  
For `CARDIO_MACHINE` / `DISTANCE_TIME` tracking type:
```
[min field] [:] [sec field] [▶ button] [dist field] [Log button]
```
That's **5 elements** in a single `Row(horizontalArrangement = spacedBy(8.dp))` with no explicit widths except `Modifier.weight(1f)` on the number fields. On a 360dp screen: 8dp × 4 gaps + 36dp timer button + 48dp Log button = 100dp fixed → 260dp for 3 weighted fields = ~87dp each. The `NumberField` placeholder labels like "min", "sec", "km" should fit, but the `Log` button only has `height(48.dp)` with `contentPadding = PaddingValues(horizontal = 12.dp)` and the label "Log" — so the Log button is narrower than the fields and looks visually disconnected.  
**Fix:** Give the `Log` button a minimum width of 72dp, or move it below the field row.

### 🟠 [A-3] Session Header — Title Truncation  
**Location:** L1276–1281  
```kotlin
Text(text = state.title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.weight(1f)
)
```
`state.title` has no `maxLines` or `overflow` set. A workout titled "Upper Body Hypertrophy & Cardio Finisher" would wrap onto a second line, pushing the Plate Calculator icon and Finish button down (since they're in the same Row). This could make the sticky header taller than expected and reduce the scroll area.  
**Fix:** Add `maxLines = 1, overflow = TextOverflow.Ellipsis`.

### 🟠 [A-4] Metrics Row — Volume Label Truncation  
**Location:** L1339  
```kotlin
MetricItem("Volume", "${state.totalVolumeKg.toInt()} ${Units.label(imperial)}")
```
At high volume (e.g. 12,500 lbs), this becomes "12500 lb" — 8 characters at `titleMedium`. `MetricItem` uses `horizontalAlignment = Alignment.CenterHorizontally` with no width constraint or ellipsis. In a `Row(Arrangement.SpaceBetween)` with 3 items, each gets ~(screenWidth − 24dp) / 3 ≈ 112dp. "12500 lb" at titleMedium (≈16sp) is ~85dp — fits, but at `sp = 1.3` scale it clips.  
**Fix:** Add `maxLines = 1, overflow = TextOverflow.Ellipsis` to `MetricItem`'s value Text.

### 🟠 [A-5] "Log" Button — Not Visually Prominent Enough  
**Location:** L1988–2025  
The `Log` button is `height(48.dp)` with default colors (Material3 `FilledButton` primary). However it sits at the **trailing end** of the weight/reps input Row, sharing space with 2–5 other fields. On small screens, this button gets no explicit width and shrinks to just "Log" text + padding. For the primary action of the entire screen, this is undersized.  
**Fix:** Extract the Log button below the input Row with `fillMaxWidth()`, like the FightScreen's Attack button.

### 🟡 [A-6] Heat Streak / Ambush XP Multiplier — Not Shown in Header  
The header metrics row shows Duration, Volume, Sets — but not the `heatStreak` or `ambushXpMult` which actively affect XP. Users won't know they're in a "heat streak" unless they notice the floating burst text.  
**Fix:** Add a "🔥 ×N.N" badge to the header metrics row when `heatStreak > 0` or `ambushXpMult > 1`.

### 🟡 [A-7] Duplicate Import Statements  
**Location:** L46–61  
`KeyboardArrowLeft`, `Favorite`, `LocalFireDepartment`, `Timer` are each imported **twice** (L46–49 and L58–61). This is a compilation warning but not a crash risk. Clean these up.

---

## 4. ShopScreen (`ShopScreen.kt`)

### 🟠 [S-1] Item Grid — Item Name Truncation on Grid Cards  
The `LazyVerticalGrid` with `GridCells.Adaptive(minSize = ?)` (not visible in excerpt, check implementation). Grid item cards for gear items often have long names like "Enchanted Dragonscale Breastplate +2". If the item name has no `maxLines` constraint inside its grid cell, the card will expand vertically and break the grid alignment.  
**Action:** Verify that item name Text inside each grid cell card has `maxLines = 2, overflow = TextOverflow.Ellipsis`.

### 🟠 [S-2] Segmented Tab Buttons — Long Labels  
**Location:** `MarketTab` enum (L120–125)  
Tabs: Shop, Inventory, Armory, Forge. "Inventory" is 9 chars — the longest. `SingleChoiceSegmentedButtonRow` divides width equally. On a 360dp screen: (360 − 32 margins) / 4 ≈ 82dp per segment. "Inventory" at `labelMedium` (~12sp) = ~70px. At font scale 1.2+, this will overflow or be cut.  
**Fix:** Use abbreviated labels ("Inv.", "Armory") at larger font scales, or use icon-only with tooltips.

### 🟡 [S-3] Filter Chip Row — No Horizontal Scroll  
The `ItemFilter` has 7 values. If rendered in a non-scrollable Row, they overflow. Confirm the filter chip row uses `horizontalScroll`.

---

## 5. RivalsScreen (`RivalsScreen.kt`)

### 🟠 [R-1] Leaderboard Entry Row — Name Truncation  
**Location:** `RivalEntry` (L94–102)  
Player names on the leaderboard have no constraint in the model. The leaderboard row likely renders: `[rank] [emoji] [name] [XP]`. If a user's display name is long (up to Firebase's unlimited String), the name column will push the XP value off-screen.  
**Fix:** Enforce `maxLines = 1, overflow = TextOverflow.Ellipsis` on the name Text, and give the XP column a fixed trailing width.

### 🟡 [R-2] Party Invite Code — Not Copyable  
The invite code is likely displayed as a plain `Text`. On Android, long-press to select is enabled by default but there's no "Copy" button. For a code that must be shared precisely, a "Copy to clipboard" icon button would significantly improve UX.

---

## 6. HistoryScreen (`HistoryScreen.kt`)

### 🟡 [Hi-1] Session Card — Workout Name Overflow  
**Location:** L1060–1063 (HeroScreen saga, similar in HistoryScreen)  
```kotlin
Text(s.name, fontWeight = FontWeight.Bold)
```
Session names like "Upper Body Hypertrophy Day 2 (Modified)" have no `maxLines` or `overflow`. In a `Row(Arrangement.SpaceBetween)`, the name column will expand and push "+XP" off the right edge.  
**Fix:** Add `maxLines = 1, overflow = TextOverflow.Ellipsis` to session name Text.

---

## 7. RewardRevealDialog (`RewardRevealDialog.kt`)

### 🟠 [RR-1] Reward Label — No `overflow` on Long Names  
**Location:** L248–255  
```kotlin
Text(text = label, maxLines = 2)
```
`maxLines = 2` is set but `overflow` defaults to `Clip` (not `Ellipsis`). This means long labels like "Enchanted Dragon Crest Helmet" will be clipped mid-character at `maxLines = 2`.  
**Fix:** Add `overflow = TextOverflow.Ellipsis`.

### 🟡 [RR-2] Currency Rewards Show Text Labels Instead of Emoji  
**Location:** L260–261  
```kotlin
is Reward.Gold -> Triple("Gold", "+${reward.amount}", Gold)
is Reward.Xp   -> Triple("XP",   "+${reward.amount} XP", ...)
```
The badge field for Gold says "Gold" (text) and for XP says "XP" (text) in `fontSize = 32.sp` when `badge.length <= 2`. This renders large block-caps text inside the square tile instead of an emoji. Competing tiles show emoji (🎁, 🏆) while currency tiles show text — visually inconsistent.  
**Fix:** Use `"💰"` for Gold, `"⭐"` for XP, `"⚡"` for Energy.

---

## 8. SceneBanner / FloatingGameDock (`SceneBanner.kt`)

### 🟠 [SB-1] Tagline — Ellipsis on `maxLines = 2` But No `overflow`  
**Location:** L329–335  
```kotlin
Text(tagline, maxLines = 2, softWrap = true, overflow = TextOverflow.Ellipsis)
```
This is actually correctly coded. ✅ No issue here.

### 🟡 [SB-2] Dock Labels — `maxLines = 1` Without `overflow`  
**Location:** DockItem L251–256, DockItemVertical L187–194  
Both dock label Texts have `maxLines = 1` but no `overflow = TextOverflow.Ellipsis`. Tab labels like "Rivals" and "Allies" are fine, but if the label list is ever extended (or locale-translated), labels may clip.  
**Fix:** Add `overflow = TextOverflow.Ellipsis` to both dock label Texts.

---

## 9. Cross-Cutting Issues

### 🟠 [CC-1] `contentPadding = PaddingValues(0.dp)` on Buttons  
Found in: `HeroScreen` stat allocation buttons (L920), `HeroHeaderBanner` export buttons (L691, L712).  
Zero content padding on Material3 Buttons causes the button text to visually collide with the button edge at large font scales. The minimum recommended Material3 horizontal padding is 12dp.  
**Fix:** Use at minimum `PaddingValues(horizontal = 8.dp, vertical = 2.dp)` for compact buttons.

### 🟡 [CC-2] Emoji in `Text` — Baseline Alignment Issues  
Emoji characters (⚔️, 🛡️, 🔥, 💧) are mixed with non-emoji text in many `Text()` calls without explicit `lineHeight` tuning. On some OEM devices (Samsung), emoji render slightly larger than the specified `sp`, causing the baseline of mixed emoji+text rows to misalign. This is especially visible in the bounty list and the combat stat section.  
**Fix:** Separate emoji into a dedicated `Text` composable alongside the label `Text` in a `Row`, rather than concatenating them.

### 🟡 [CC-3] No `LocalMinimumInteractiveComponentSize` Override at Screen Level  
`ActiveSessionScreen` overrides `LocalMinimumInteractiveComponentSize` to `0.dp` inside `ExerciseLogCard` (L1795, L1868, L1898) to allow compact chips and buttons. This is intentional for information density, but it means small targets like the 36dp `IconButton` for move-up/move-down (L1833–1838) fall below the recommended 48dp touch target. Users with motor impairments may miss these.  
**Fix:** Keep the 36dp icon but expand the touch target using `Modifier.minimumInteractiveComponentSize()` on the individual IconButtons that are user-facing actions (move, delete, swap).

---

## Summary Table

| ID | Screen | Issue | Severity |
|----|--------|-------|----------|
| H-1 | Hero | Currency bar scroll affordance missing | ✅ Fixed |
| H-2 | Hero | Banner header vertical crowding (form chips + export buttons) | 🟠 |
| H-3 | Hero | Settings button alignment off (center vs. top-right) | ✅ Fixed |
| H-4 | Hero | Stat allocation buttons: 0dp contentPadding | 🟡 |
| H-5 | Hero | Saga tab bottom clip risk on gesture nav phones | 🟡 |
| H-6 | Hero | Switch Job "Premium" label: no overflow | 🟡 |
| H-7 | Hero | Mastery tab: no empty state | 🟡 |
| F-1 | Fight | Arena fixed 290dp clips on small phones | 🟠 |
| F-2 | Fight | Monster name/trait badge Row alignment | 🟠 |
| F-3 | Fight | Skill buttons: no explanation when disabled | 🟡 |
| F-4 | Fight | Narration text unbounded — CTA can be pushed off-screen | 🟡 |
| A-1 | Session | Exercise subtitle silently truncates stat label | ✅ Fixed |
| A-2 | Session | Cardio row: 5 elements + Log button on small screens | ✅ Fixed |
| A-3 | Session | Workout title wraps, expands sticky header | ✅ Fixed |
| A-4 | Session | Volume metric value clips at large font scale | ✅ Fixed |
| A-5 | Session | Log button too small for primary action | 🟠 |
| A-6 | Session | Heat streak/XP multiplier not surfaced in header | 🟡 |
| A-7 | Session | Duplicate import statements | ✅ Fixed |
| S-1 | Shop | Item grid name truncation (verify) | 🟠 |
| S-2 | Shop | "Inventory" segment label overflows at 1.2× font scale | 🟠 |
| S-3 | Shop | Filter chip row (verify scroll) | 🟡 |
| R-1 | Rivals | Leaderboard name truncation | 🟠 |
| R-2 | Rivals | Party invite code not copyable | 🟡 |
| Hi-1 | History | Session name overflow | ✅ Fixed |
| RR-1 | Rewards | Label clipped (no TextOverflow.Ellipsis) | ✅ Fixed |
| RR-2 | Rewards | Currency tiles show text instead of emoji | ✅ Fixed |
| SB-2 | Dock | Tab labels missing overflow | 🟡 |
| CC-1 | All | 0dp contentPadding on compact Buttons | 🟠 |
| CC-2 | All | Emoji baseline misalignment in mixed Text | 🟡 |
| CC-3 | All | Small touch targets (36dp) in exercise card actions | 🟡 |

---

## 10. SettingsDialog (`SettingsDialog.kt`)

### 🟠 [SD-1] `AlertDialog` is the Wrong Container — Severely Height-Constrained
**Location:** L477–832  
The entire settings content is placed inside a **Material3 `AlertDialog`** text slot. `AlertDialog` has a maximum height cap on most devices (~85% of screen height), and the `text` slot is not a fullscreen container — it adds its own internal padding. With 10+ sections (Account, Premium, Units, Body Metrics, Watch, Feedback, Graphics, Effort Tracking, Gemini API, Local AI, Hevy), scrolling through the entire dialog takes significant effort and the bottom sections are essentially hidden from casual users.  
**Fix:** Replace with a `ModalBottomSheet` or a dedicated `FullScreenDialog` (using `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`). This is the single biggest structural UX issue in Settings — important items like Local AI and Hevy Sync are effectively buried.

### 🟠 [SD-2] Save Button Only Saves API Key — Misleading CTA
**Location:** L824–828  
```kotlin
confirmButton = {
    TextButton(onClick = {
        viewModel.saveApiKey(key)
        close()
    }) { Text("Save") }
}
```
The dialog's `"Save"` button **only** saves the Gemini API key field. All other settings (units, haptics, sound, body metrics, effort method) are saved immediately on toggle/change, not on this button press. Users who make multiple changes and tap `"Save"` reasonably expect everything to be saved — but if they tap `"Close"` instead, those other settings were already persisted and the API key is **not**. This is directionally inconsistent.  
**Fix:** Either rename `"Save"` to `"Done"`, or save the API key on `onValueChange` (debounced) and make both buttons just dismiss.

### 🟠 [SD-3] Email / Password Fields Always Visible When Guest — Cluttered
**Location:** L605–648  
When a user is not signed in, the dialog shows: Sign in with Google button → email field → password field → Create Account + Sign in buttons — all visible simultaneously. This is a lot of content for a guest who just wants a quick look at settings. If they tap the wrong button ("Create account" vs "Sign in"), they get an error.  
**Fix:** Collapse to just the Google button by default, with an expandable "Use email instead" disclosure.

### 🟠 [SD-4] "Repair Duplicate Gear" Appears **Twice**
**Location:** L594–598 (signed-in path) and L643–648 (guest path)  
The `OutlinedButton` for "Repair duplicate gear" is duplicated in both branches of the `if (account.hasAccount)` block. This means every user sees it, but the code logic suggests it's a recovery tool, not a primary action for all users.  
**Fix:** Move it to a single location (e.g., at the bottom of the Account section), outside the signed-in/guest branch duplication.

### 🟡 [SD-5] `OutlinedTextField` Width Exceeds `AlertDialog` Width
**Location:** L615–621, L622–629, L676–683, L798–811  
All `OutlinedTextField` instances use `Modifier.fillMaxWidth()`. Inside an `AlertDialog`, the text slot has its own internal horizontal padding (~24dp each side). `fillMaxWidth()` fills the text slot's available width — this is fine — but the fields visually feel very wide and can clip their labels on narrow devices if font scale is elevated.  
**Fix:** Verify behavior at 1.3× font scale, specifically that labels like `"Confirm with password"` and `"Hevy API Key"` don't clip in their text field label position.

### 🟡 [SD-6] Birthday Fields — No Validation Feedback
**Location:** L1046–1073  
The MM / DD / YYYY fields accept typed numbers but the year validation at L1081 only accepts `1900..2025` — a user born after 2025 (edge case) or typing a typo year like "20256" will silently fail. There's no inline error below the fields to say "Invalid date — check year".  
**Fix:** Add a `syncMsg`-style error message if the parsed date is null after the Save tap.

### 🟡 [SD-7] Local AI Section — Play Diagnostics Always Visible in Production  
**Location:** L1256–1263  
```kotlin
if (playAssetState.statusCode != AssetPackStatus.UNKNOWN || ...) {
    Text("Play diagnostics: status ${...}, error ${...}")
}
```
This diagnostic line is **always shown** when the asset state is anything other than UNKNOWN — which includes INSTALLED, DOWNLOADING, etc. In production, users will see raw status codes like `"Play diagnostics: status 6, error 0"` with no explanation. This reads as a crash log to end-users.  
**Fix:** Guard this behind `BuildConfig.DEBUG` or remove from the production UI entirely.

### 🟡 [SD-8] Watch Section — Single "Refresh link" TextButton, No Status Icon
**Location:** L707–730  
The Watch status is shown as plain text (`wear.statusText`) with a color (green = linked, grey = not). There's no icon — a green/red indicator dot or a watch icon would make the linked/unlinked state scannable at a glance without reading. The section also has a very minimal `TextButton("Refresh link")` which on grey background looks almost invisible.  
**Fix:** Add a status dot (●) colored green/grey next to the status text, and promote "Refresh link" to an `OutlinedButton`.

### 🟡 [SD-9] Hevy Section — `syncMessage` Error Detection Is Fragile
**Location:** L1373  
```kotlin
color = if (syncMessage!!.contains("Error") || syncMessage!!.contains("Invalid")) error else primary
```
Error color is determined by string-matching the message text. If the sync failure message doesn't contain "Error" or "Invalid" (e.g., network timeout → `"Sync failed: timeout"`), the message shows in the **primary** (success) color even though it's a failure. This is a UX trust issue — error states should never show in green/accent color.  
**Fix:** Drive color from a `isError: Boolean` state variable, not string content.

---

## 11. WearOS (`WearApp.kt` + `WearMainActivity.kt`)

### 🟠 [W-1] Exercise Name — No Overflow Enforcement
**Location:** WearApp.kt L280–288  
```kotlin
Text(
    text = ex.name,
    style = MaterialTheme.typography.title2,
    maxLines = 2,
    modifier = Modifier.fillMaxWidth(0.92f)
)
```
`maxLines = 2` is set but **no `overflow`** is specified (defaults to `TextOverflow.Clip`). Long exercise names like "Incline Dumbbell Fly (Cable Machine Variation)" rendered in `title2` at ~18sp may clip mid-glyph at line 2. On a round 384×384 Wear display, `fillMaxWidth(0.92f)` ≈ 353dp, but with left/right chin insets on round displays the effective horizontal safe area is more like 300dp.  
**Fix:** Add `overflow = TextOverflow.Ellipsis`.

### 🟠 [W-2] `StepperRow` — ±Buttons Too Small for Glanceable Interaction
**Location:** WearApp.kt L495, L505  
```kotlin
Button(onClick = onMinus, modifier = Modifier.size(36.dp)) { Text("-") }
Button(onClick = onPlus, modifier = Modifier.size(36.dp)) { Text("+") }
```
36dp touch targets on a round watch face are below the recommended Wear OS minimum of **48dp**. The stepper is the primary interaction during a workout — the user is sweating, their finger may be imprecise. 36dp buttons will cause frequent mis-taps, logging wrong reps/weight.  
**Fix:** Increase to `Modifier.size(48.dp)` and reduce the label column width correspondingly. Alternatively, use Wear's `CompactChip` style buttons which default to a larger tap area.

### 🟠 [W-3] Rest Screen — Timer Text Has No Bound on Round Display Chin
**Location:** WearApp.kt L536–541  
```kotlin
Text(
    "%d:%02d".format(remaining / 60, remaining % 60),
    style = MaterialTheme.typography.display1,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colors.primary
)
```
The rest timer uses `display1` typography with no width or padding constraint. On round displays, the Wear engine applies its own insets, but the surrounding `Column` only has `padding(horizontal = 14.dp, vertical = 20.dp)`. A value like `"10:00"` in display1 (~36sp) is approximately 100dp wide — fine — but the heart rate drop text directly below (`"Recovery 145 → 112 (−33)"`) at `caption1` is about 200dp wide on one line. On a 320dp-wide round screen with 14dp side padding, that's 292dp max, which is tight for longer recovery strings.  
**Fix:** Add `textAlign = TextAlign.Center` (already present on some but not the recovery text) and `overflow = TextOverflow.Ellipsis` to all Text calls in `RestScreen`.

### 🟠 [W-4] Idle Screen — "Find phone" Button Fixed Width 78%
**Location:** WearApp.kt L168  
```kotlin
Button(onClick = onRetryLink, modifier = Modifier.fillMaxWidth(0.78f)) {
    Text(if (phoneConnected) "Refresh" else "Find phone")
}
```
`fillMaxWidth(0.78f)` is correct for Wear OS pill-button convention, but the text inside switches between `"Refresh"` and `"Find phone"` without any size consideration. On Wear devices with system font scale >1.0, `"Find phone"` in the button may wrap or get cut. Wear OS buttons don't automatically handle this.  
**Fix:** Add `maxLines = 1, overflow = TextOverflow.Ellipsis` to the Text inside the button.

### 🟠 [W-5] `SessionScreen` — No "Exercise Complete" Indicator
**Location:** WearApp.kt L290–296  
```kotlin
Text(text = "Set ${ex.loggedSets + 1} • Goal ${ex.targetReps}")
```
When `ex.loggedSets >= ex.targetSets`, the next-set line still says `"Set N • Goal X reps"` and doesn't communicate that the exercise is done. The user has to know to navigate to the next exercise manually using the `▶` button. There's no visual completion badge or checkmark.  
**Fix:** When `ex.loggedSets >= ex.targetSets`, show `"✓ Done — ${ex.loggedSets} sets"` in a distinct color (green) instead of the set counter.

### 🟡 [W-6] `SessionScreen` — Metrics Row Not Constrained
**Location:** WearApp.kt L211–248  
The metrics row shows HR + zone (left) and kcal + duration (right) in a `Row(Arrangement.SpaceBetween)`. Neither column has a width constraint. If the kcal value reaches 4 digits (e.g., "1450 kcal"), the right column may overflow into the left on very small watch faces (300dp width).  
**Fix:** Give each `Column` a `Modifier.weight(1f)` so they share the width equally.

### 🟡 [W-7] `WearFeedbackDialog` — Fixed `padding(12.dp)` on Round Screen
**Location:** WearApp.kt L98–115  
```kotlin
modifier = Modifier.fillMaxSize().background(...).padding(12.dp)
```
On round watch faces, a flat 12dp padding does not respect the circular safe area. Content in the corners will appear clipped by the screen bezel. Wear OS provides `fillMaxRectangle()` or `curvedComposable` for safe-area-aware layouts.  
**Fix:** Use `Modifier.fillMaxSize().wrapContentSize()` with Wear's `ScreenScaffold` instead of a bare `Column` with flat padding.

### 🟡 [W-8] Heat Streak Not Shown on Watch
**Location:** `WearSessionState` (shared module) includes `heatStreak: Int`, but nothing in `WearApp.kt` renders it.  
The watch shows HR, kcal, reps, weight — but doesn't show the active heat streak multiplier. When a user is on a 5-set heat streak, the watch gives them no feedback about their bonus, which is a missed motivation opportunity.  
**Fix:** Add a small `"🔥×2"` label to the status row when `state.session.heatStreak > 0`.

---

## Updated Summary Table (Settings + WearOS additions)

| ID | Area | Issue | Severity |
|----|------|-------|----------|
| SD-1 | Settings | `AlertDialog` is wrong container — major items buried | 🟠 |
| SD-2 | Settings | "Save" button only saves API key — misleading | 🟠 |
| SD-3 | Settings | Email/password fields cluttered for guest users | 🟠 |
| SD-4 | Settings | "Repair Gear" button duplicated in both account states | 🟠 |
| SD-5 | Settings | `OutlinedTextField` labels may clip at 1.3× font scale | 🟡 |
| SD-6 | Settings | Birthday fields: no validation feedback on bad date | 🟡 |
| SD-7 | Settings | Play diagnostics text shown to production users | 🟡 |
| SD-8 | Settings | Watch section: no status icon, Refresh is invisible | 🟡 |
| SD-9 | Settings | Hevy error color driven by string matching (fragile) | 🟡 |
| W-1 | Wear | Exercise name: no `TextOverflow.Ellipsis` — mid-glyph clip | 🟠 |
| W-2 | Wear | Stepper ±buttons are 36dp — below 48dp Wear minimum | 🟠 |
| W-3 | Wear | Rest screen recovery text: no overflow / center on round face | 🟠 |
| W-4 | Wear | Idle "Find phone" button text may wrap at large font scale | 🟠 |
| W-5 | Wear | No exercise completion indicator after target sets logged | 🟠 |
| W-6 | Wear | Metrics row columns not width-constrained | 🟡 |
| W-7 | Wear | Feedback dialog uses flat padding — clips on round display | 🟡 |
| W-8 | Wear | Heat streak not shown on watch despite being in state | 🟡 |

---

## Recommended Priority Order
1. **A-2** (Cardio input row) — highest risk of actual layout breakage
2. **A-1** (Exercise subtitle truncation) — silently hides data
3. **H-3** (Settings button alignment) — always visible, always wrong
4. **F-1** (Arena fixed height) — affects all battle users on small phones
5. **A-3** (Title wrapping expands header) — affects scroll area for all workouts
6. **RR-1** (Reward label clipping) — affects every chest/battle reward
7. **CC-1** (0dp contentPadding) — global issue, quick sweep fix
8. **R-1** (Leaderboard name truncation)
9. **F-4** (Victory narration pushes CTA off-screen)
10. Remaining 🟡 items as a polish pass
