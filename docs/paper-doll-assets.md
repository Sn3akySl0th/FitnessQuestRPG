# FitnessQuestRPG — 2D Paper-Doll Asset Specification & Integration Guide

This document defines the asset specifications, layer conventions, naming rules, and density requirements for creating replacement 2D artwork for the `HeroPaperDoll` and `EquipmentSlot` visual systems.

---

## 1. Master Canvas & Alignment Rules

To eliminate manual per-item alignment offsets in code, all wearable equipment sprite layers must be rendered against a shared **master 5:6 canvas**.

| Canvas Property | Value |
| :--- | :--- |
| **Master Canvas Ratio** | `5:6` |
| **Logical Resolution (xhdpi)** | `500 × 600 px` |
| **High-Res Master (xxxhdpi)** | `1000 × 1200 px` |
| **Centered Hero Visual Bounds** | Center `400 × 540 px` |
| **Standalone Icon Canvas** | `256 × 256 px` (1:1 square, centered 80% safe zone) |
| **Export Format** | 32-bit PNG (RGBA with alpha) or WebP lossless |

> **Alignment Rule**: Every wearable layer (`layer_*.png`) is exported at the full 500 × 600 px canvas size with transparent padding so that stacking all layers at (0, 0) automatically aligns head, torso, hands, legs, feet, back, and weapon to the hero body.

---

## 2. Standard 14-Layer Z-Index Stack

The paper-doll renderer composites layers in the following strict order:

```
Z-Index  Layer Tag           Target / Contents
--------------------------------------------------------------------------------
00       bg_pedestal         Magical summoning platform & pedestal base
01       fx_aura_back        Radial background aura & high-tier rune glows
02       gear_back           Cape, gym towel cape, hydration cloak, quiver
03       body_base           Hero base body silhouette (torso, arms, legs, face)
04       body_hair_back      Long hair trailing behind neck & shoulders
05       gear_legs           Joggers, shorts, compression tights
06       gear_torso          Gym vest, tank, compression top, cuirass, hoodie
07       gear_feet           High-top training shoes, trail runners, boots
08       gear_hands          Lifting gloves, wrist wraps, straps
09       body_hair_front     Front hair fringe, bangs, facial hair
10       gear_head           Sweatband, headband, cowl, helm visor
11       gear_weapon         Dumbbell hammer, kettlebell mace, resistance bow
12       gear_trinket        Pendant, heart medal, lifting belt, chest badge
13       fx_aura_front       Foreground sparkles, equip flash, flame/lightning FX
```

---

## 3. Slot Clarification — Back vs. Trinket

### Domain vs. Visual Slot Mapping
- **Database / Domain Model**: Uses `ItemSlot.TRINKET` to maintain schema compatibility.
- **Paper-Doll Visual Model**: Distinguishes between `PaperDollVisualSlot.BACK` (Z: 02) and `PaperDollVisualSlot.TRINKET` (Z: 12).

| Item | Domain Slot | Visual Slot | Layer Order | Rendering Position |
| :--- | :---: | :---: | :---: | :--- |
| `gym_towel_cape` | `TRINKET` | `BACK` | `02 gear_back` | Draped behind shoulders and torso |
| `hydration_cloak` | `TRINKET` | `BACK` | `02 gear_back` | Flows behind the body |
| `heart_medal` | `TRINKET` | `TRINKET` | `12 gear_trinket` | Front chest pendant |
| `lifting_belt` | `TRINKET` | `TRINKET` | `12 gear_trinket` | Front waist / core accessory |
| `power_sweatband` | `TRINKET` | `TRINKET` | `12 gear_trinket` | Front wrist/arm accessory |

*Note*: Until the domain model separates Back and Trinket into two equippable inventory slots, a player can equip one TRINKET item at a time, which will dynamically render at either the Back or Trinket layer based on its visual spec.

---

## 4. File Naming Conventions

All asset filenames must use **lowercase snake_case**:

### 4.1 Wearable Layers
```
layer_[slot]_[item_name]_[tier].png
```
*Examples*:
- `layer_weapon_dumbbell_hammer_t4.png`
- `layer_head_heroic_sweatband_t1.png`
- `layer_torso_gym_vest_t2.png`
- `layer_legs_battle_joggers_t3.png`
- `layer_feet_high_top_trainers_t2.png`
- `layer_back_gym_towel_cape_t3.png`
- `layer_trinket_heart_medal_t4.png`

### 4.2 Standalone Inventory & Shop Icons
```
icon_[slot]_[item_name]_[tier].png
icon_[slot]_[item_name]_[tier]_glow.png (optional additive mask)
```
*Examples*:
- `icon_weapon_dumbbell_hammer_t4.png`
- `icon_torso_gym_vest_t2.png`
- `icon_weapon_dumbbell_hammer_t4_glow.png`

### 4.3 Base Body Assets
```
body_[gender]_[race]_[layer].png
```
*Examples*:
- `body_male_human_base.png`
- `body_female_elf_base.png`
- `body_male_orc_base.png`

---

## 5. Android Resource Directory Structure

Place 2D assets in the corresponding density-qualified drawable folders:

```
app/src/main/res/
  ├── drawable-mdpi/          (1.0x  |  Layer: 250×300 px  |  Icon: 128×128 px)
  ├── drawable-hdpi/          (1.5x  |  Layer: 375×450 px  |  Icon: 192×192 px)
  ├── drawable-xhdpi/         (2.0x  |  Layer: 500×600 px  |  Icon: 256×256 px) [BASELINE]
  ├── drawable-xxhdpi/        (3.0x  |  Layer: 750×900 px  |  Icon: 384×384 px)
  └── drawable-xxxhdpi/       (4.0x  |  Layer: 1000×1200 px|  Icon: 512×512 px)
```

---

## 6. Rarity Color Tiers

| Tier | Name | Primary Hex | Palette Character |
| :---: | :--- | :--- | :--- |
| **1** | **Common** | `#94A3B8` | Slate / Matte iron, natural cotton |
| **2** | **Uncommon** | `#22C55E` | Emerald Green / Athletic neon accents |
| **3** | **Rare** | `#06B6D4` | Electric Cyan / Anodized metal & blue power lines |
| **4** | **Epic** | `#A855F7` | Mystic Violet / Arcane energy & dark alloy |
| **5** | **Legendary** | `#F59E0B` | Radiant Gold / Amber flames & gilded trim |
| **6** | **Mythic** | `#EF4444` | Crimson Prismatic / Shifting magenta-to-cyan energy |

---

## 7. Graceful Fallback & Resource Resolution Safety

### 7.1 Current Phase: Placeholder / No-Asset Mode
- If a 2D bitmap layer or icon is not found in `res/drawable`, the system automatically falls back to dynamic Compose `Canvas` rendering (`CharacterAvatar.kt` and `ItemIcon.kt`).
- The resolver `EquipmentVisualRegistry` safely returns `null` for missing drawables and caches resolution results. It will never return `0` or throw an exception.
- The entire application compiles and functions fully with zero PNG/XML artist assets present.

### 7.2 Production Asset Phase (TODO)
- **Important**: Dynamic `resources.getIdentifier()` lookups are **not** automatically safe under Android R8 resource shrinking (`shrinkResources true`) without an explicit keep rule.
- `app/src/main/res/raw/keep.xml` is configured to preserve `@drawable/layer_*`, `@drawable/icon_*`, and `@drawable/body_*`.
- When real production artwork is added to `res/drawable`, teams should either:
  1. Maintain the `tools:keep` entries in `res/raw/keep.xml` for all shipped asset name patterns; OR
  2. Replace dynamic string lookups in `EquipmentVisualRegistry` with a compile-time map of actual existing `R.drawable.*` resource IDs for maximum type-safety and performance.
