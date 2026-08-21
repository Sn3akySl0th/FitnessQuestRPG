# FitQuest UI/UX Sprint Plan

> Source: [Full QA Report](UI_QA_REPORT.md)  
> Total issues: **47** across 4 sprints  
> Estimated total: ~4–6 focused coding sessions

---

## Sprint 1 — Critical & Data Integrity
**Goal:** Fix every issue that causes data loss, hidden information, or layout breakage before the next release.  
**Issues:** 7 | **Effort:** 1 session

| ID | Screen | Issue | File |
|----|--------|-------|------|
| A-2 | Session | Cardio input row: 5 fields crammed → Log button undersized | `ActiveSessionScreen.kt` |
| A-1 | Session | Exercise subtitle silently clips stat label | `ActiveSessionScreen.kt` |
| A-3 | Session | Workout title wraps → expands sticky header | `ActiveSessionScreen.kt` |
| SD-2 | Settings | "Save" only saves API key — misleading CTA | `SettingsDialog.kt` |
| SD-7 | Settings | Raw Play SDK status codes shown to production users | `SettingsDialog.kt` |
| SD-9 | Settings | Hevy error color uses string matching (errors can show green) | `SettingsDialog.kt` |
| W-2 | Wear | Stepper ±buttons are 36dp — below 48dp Wear minimum | `WearApp.kt` |

### Acceptance Criteria
- [ ] Cardio row: Log button moved below input row with `fillMaxWidth()`
- [ ] Exercise subtitle uses `maxLines = 2` or drops the stat label from the single line
- [ ] Workout title in sticky header: `maxLines = 1, overflow = TextOverflow.Ellipsis`
- [ ] Settings "Save" renamed to "Done"; API key saves on `onValueChange` (debounced 800ms)
- [ ] Play diagnostics line guarded by `BuildConfig.DEBUG`
- [ ] Hevy sync error state driven by `isError: Boolean` flag, not string content
- [ ] Stepper ±buttons are `Modifier.size(48.dp)`

---

## Sprint 2 — Settings Restructure + Global Text Overflow Pass [COMPLETED]
**Goal:** Restructure the Settings dialog into a proper full-screen experience and fix all text overflow/truncation issues app-wide in a single sweep.  
**Issues:** 15 | **Effort:** 1–2 sessions

### Part A — Settings Dialog Restructure

| ID | Issue | File |
|----|-------|------|
| SD-1 | Replace `AlertDialog` with fullscreen `Dialog` or `ModalBottomSheet` | `SettingsDialog.kt` |
| SD-3 | Guest auth: collapse email/password behind a "Use email" disclosure | `SettingsDialog.kt` |
| SD-4 | "Repair Gear" button duplicated in both account branches | `SettingsDialog.kt` |
| SD-8 | Watch section: add ● status dot, promote Refresh to `OutlinedButton` | `SettingsDialog.kt` |

### Part B — Global TextOverflow Sweep

| ID | Screen | Text Element | File |
|----|--------|--------------|------|
| RR-1 | Rewards | Reward card label (`maxLines = 2`, no overflow) | `RewardRevealDialog.kt` |
| H-6 | Hero | "Premium" lock label on Job card | `HeroScreen.kt` |
| A-4 | Session | Volume metric value | `ActiveSessionScreen.kt` |
| R-1 | Rivals | Leaderboard player name | `RivalsScreen.kt` |
| Hi-1 | History | Session name in saga/history row | `HeroScreen.kt` + `HistoryScreen.kt` |
| SB-2 | Dock | Tab labels in `DockItem` + `DockItemVertical` | `SceneBanner.kt` |
| W-1 | Wear | Exercise name (`maxLines = 2`, no overflow) | `WearApp.kt` |
| W-4 | Wear | "Find phone" button text | `WearApp.kt` |
| CC-1 | All | Compact buttons: `contentPadding = PaddingValues(0.dp)` → min 8dp | `HeroScreen.kt` |
| A-7 | Session | Remove duplicate import statements (×4) [DONE] | `ActiveSessionScreen.kt` |

### Acceptance Criteria
- [x] Settings opens fullscreen or as bottom sheet — all 11 sections reachable without frustration
- [x] Guest auth shows only Google button by default; email form revealed on tap
- [x] "Repair Gear" appears exactly once, below the Account section header
- [x] Watch status has a ● indicator and Refresh is an `OutlinedButton`
- [x] Every `Text` with `maxLines` has matching `overflow = TextOverflow.Ellipsis`
- [x] All compact Buttons have at minimum `PaddingValues(horizontal = 8.dp)`
- [x] Duplicate imports removed from `ActiveSessionScreen.kt`

---

## Sprint 3 — WearOS UI Safety & Completion UX [COMPLETED]
**Goal:** Ensure the Wear app is safe and usable on round displays, and gives users clear workout progress feedback.  
**Issues:** 6 | **Effort:** 1 session

| ID | Issue | File |
|----|-------|------|
| W-5 | No exercise completion indicator when target sets done | `WearApp.kt` |
| W-3 | Rest screen: recovery text clips on round displays | `WearApp.kt` |
| W-7 | Feedback dialog: flat `12.dp` padding clips on round bezel | `WearApp.kt` |
| W-6 | Metrics row columns: no `weight(1f)` — can overlap | `WearApp.kt` |
| W-8 | Heat streak not shown on watch despite being in `WearSessionState` | `WearApp.kt` |
| SD-6 | Birthday fields: no validation error shown on invalid date | `SettingsDialog.kt` |

### Acceptance Criteria
- [x] When `ex.loggedSets >= ex.targetSets`, row shows `"✓ Done — N sets"` in green
- [x] All `Text` in `RestScreen` has `overflow = TextOverflow.Ellipsis` + `textAlign = Center`
- [x] Feedback dialog uses Wear-safe layout (no corner clipping on round watches)
- [x] Metrics row both Columns have `Modifier.weight(1f)`
- [x] `🔥×N` streak badge appears in metrics row when `heatStreak > 0`
- [x] Invalid birthday shows inline error text below the date fields after Save tap

---

## Sprint 4 — Polish, Alignment & Empty States [COMPLETED]
**Goal:** Address every remaining cosmetic issue, alignment fix, empty state, and minor motivation improvement.  
**Issues:** 19 | **Effort:** 1–2 sessions

### Hero Screen

| ID | Issue | Fix |
|----|-------|-----|
| H-1 | Currency bar scroll: no affordance | Add trailing `Brush` fading edge |
| H-2 | Banner: export buttons + form chips vertically crowded | Collapse export buttons into a `More ⋮` menu icon |
| H-3 | Settings gear aligns center, not top-right of banner | Move into `Box` scope with `Modifier.align(Alignment.TopEnd)` |
| H-4 | Stat allocation buttons: `contentPadding = 0.dp` | `PaddingValues(horizontal = 4.dp)`, font 13.sp |
| H-5 | Saga tab: bottom clearance for gesture nav | Add `WindowInsets.navigationBars` padding to saga Column bottom |
| H-7 | Mastery tab: no empty state | Add "Start training to unlock mastery" empty-state Card |

### Fight Screen

| ID | Issue | Fix |
|----|-------|-----|
| F-1 | Arena fixed `290.dp` clips on short phones | `fillMaxHeight(0.42f)` |
| F-2 | Monster name/trait badge Row alignment | `Modifier.weight(1f)` on name + `TextOverflow.Ellipsis` |
| F-4 | Victory narration unbounded → pushes CTA off screen | `maxLines = 6, overflow = TextOverflow.Ellipsis` |

### Active Session Screen

| ID | Issue | Fix |
|----|-------|-----|
| A-5 | Log button too small for primary action | Extract below field row as `fillMaxWidth()` button |
| A-6 | Heat streak / XP multiplier not shown in sticky header | Add `🔥 ×N.N` chip to header metrics row when `heatStreak > 0` |

### Shop Screen

| ID | Issue | Fix |
|----|-------|-----|
| S-1 | Grid item name truncation | Add `maxLines = 2, overflow = Ellipsis` to grid item name Text |
| S-2 | "Inventory" segment label clips at 1.2× scale | Abbreviate label or switch to icon-only at large scale |
| S-3 | Filter chip row: verify horizontal scroll | Wrap in `horizontalScroll(rememberScrollState())` if missing |

### Rivals Screen

| ID | Issue | Fix |
|----|-------|-----|
| R-2 | Party invite code not copyable | Add copy `IconButton` using `ClipboardManager` |

### Rewards Dialog

| ID | Issue | Fix |
|----|-------|-----|
| RR-2 | Gold/XP tiles show block text instead of emoji | Use `"💰"` / `"⭐"` / `"⚡"` in `rewardPresentation()` |

### Cross-Cutting

| ID | Issue | Fix |
|----|-------|-----|
| CC-2 | Emoji baseline misalignment in mixed Text | Separate emoji into its own `Text` in a `Row` for key stat labels |
| CC-3 | 36dp touch targets on exercise move/delete buttons | Add `Modifier.minimumInteractiveComponentSize()` |
| SD-5 | OutlinedTextField labels at 1.3× scale | Verify on device; fix width guards if confirmed |

### Acceptance Criteria
- [x] Currency bar trailing fade gradient visible when scrollable
- [x] Settings icon anchored to top-right of hero banner
- [x] Battle arena scales proportionally (no fixed height)
- [x] Victory narration bounded — Claim Rewards always visible
- [x] Log button is full-width below input fields in session screen
- [x] `rewardPresentation()` returns emoji for Gold/XP/Energy
- [x] Party invite code has clipboard copy button
- [x] Mastery tab has an empty state card
- [x] Heat streak badge shown in ActiveSession header

---

## Sprint Overview

| Sprint | Focus | Issues | Est. Sessions |
|--------|-------|--------|---------------|
| **Sprint 1** | Critical & data integrity | 7 | 1 |
| **Sprint 2** | Settings restructure + overflow sweep | 15 | 1–2 |
| **Sprint 3** | WearOS safety & completion UX | 6 | 1 |
| **Sprint 4** | Polish, alignment & empty states | 19 | 1–2 |
| **Total** | | **47** | **4–6** |

---

> [!NOTE]
> **Won't fix:** F-3 (disabled skill button explanation) — acceptable UX for v1.  
> **Verify only:** SD-5 (TextField label at 1.3× scale) — fix only if confirmed broken on a physical device.
