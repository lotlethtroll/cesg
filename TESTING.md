# CESG Manual Test Plan

Use this checklist when validating a release or after large changes. Run in **Creative** for setup speed, then repeat critical paths in **Survival** where noted.

**Environment:** Minecraft 1.21.1 · NeoForge · Create 6.0.10+

**Legend:** `[ ]` not tested · `[x]` pass · `[!]` fail (note in *Notes* column)

---

## 0. Smoke test

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 0.1 | `./gradlew runClient` (no `clean` unless needed) | Game launches, mod loads, no crash on title screen | | |
| 0.2 | Open creative tab **Create: End Storage & Gateways** | All blocks/items appear with textures (no missing purple/black) | | |
| 0.3 | Create new world, `/gamemode creative` | World loads, JEI/EMI shows CESG recipes | | |

---

## 1. Shulker stations (Loader / Unloader / Belt Loader / Belt Unloader)

### 1.1 Placement & power

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.1.1 | Place stationary loader and unloader | Blocks place, models render correctly | | |
| 1.1.2 | Place belt loader/unloader **two blocks above** a horizontal Create belt | Placement succeeds; invalid placement blocked | | |
| 1.1.3 | Connect shaft to **back** face, spin up | Goggles show powered; processing works when powered | | |
| 1.1.4 | Remove/stop shaft | Goggles show unpowered; no processing or auto-eject | | |

### 1.2 Shulker docking

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.2.1 | Right-click empty station with vanilla shulker box | Shulker docks; goggles show contents | | |
| 1.2.2 | Empty-hand right-click docked station | Shulker removed to player inventory | | |
| 1.2.3 | Push shulker in via funnel from any side | Shulker docks | | |
| 1.2.4 | Break station while holding shulker | Shulker drops (not voided) | | |
| 1.2.5 | Dock **enhanced shulker** (T2/T3/T4) | Correct slot count shown in goggles | | |
| 1.2.6 | Dock **dyed enhanced shulker** | Works; color preserved | | |

### 1.3 Item transfer (stationary)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.3.1 | Loader + funnel inserting items | Items enter docked shulker | | |
| 1.3.2 | Unloader + funnel extracting items | Items leave docked shulker | | |
| 1.3.3 | Loader: funnel cannot extract individual slots | Only whole-box eject exposes shulker item | | |
| 1.3.4 | Unloader: funnel cannot insert into shulker slots | Insert blocked on capability side | | |

### 1.4 Belt variants

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.4.1 | Belt loader over moving belt with items | Matching items pulled into docked shulker | | |
| 1.4.2 | Belt unloader over belt | Items pushed onto belt from shulker | | |
| 1.4.3 | Set **front filter slot** (item or Create filter) | Only matching items transfer; empty = all non-shulker | | |
| 1.4.4 | Idle ~3 s after processing | Nozzle/tube animation retracts | | |

### 1.5 Configuration (wrench or Shift + empty hand)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.5.1 | Open config UI on each station type | Correct title; all controls work | | |
| 1.5.2 | **Retention: Hold** | Shulker never auto-ejects | | |
| 1.5.3 | **Retention: Auto Eject** + **All Slots** (loader) | Ejects when every slot has ≥1 item | | |
| 1.5.4 | **All Slots** (unloader) | Ejects when every slot empty | | |
| 1.5.5 | **Slot Threshold** loader (N = 5, 27, 54…) | Only first N slots fill; ejects at threshold | | |
| 1.5.6 | **Slot Threshold** unloader | Ejects when at most N slots still hold items | | |
| 1.5.7 | Set **station name** | Name stamped on every shulker held at station | | |
| 1.5.8 | Clear station name | Existing shulker name preserved | | |

### 1.6 Auto-eject & funnels

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.6.1 | Extracting funnel on eject side, conditions met | Whole shulker ejected to funnel/belt | | |
| 1.6.2 | Funnel in **insert** mode on eject side | Eject blocked; goggles warn | | |
| 1.6.3 | Goggles: **Ready for funnel extraction** | Shows when powered + conditions met | | |
| 1.6.4 | Multi-cycle: eject → new shulker inserted → fill → eject again | Capabilities stay in sync (no stuck funnel) | | |

### 1.7 Filter goggle hints (Sneak + goggles)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 1.7.1 | Hold item that passes station filter | Goggles: accepts | | |
| 1.7.2 | Hold item blocked by station filter | Goggles: rejected at station | | |
| 1.7.3 | Loader with shulker filter upgrade | Chain: station filter then shulker filter | | |
| 1.7.4 | Hold item blocked by docked shulker filter | Goggles: rejected at shulker | | |

---

## 2. Enhanced shulker boxes

### 2.1 Crafting & tiers

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 2.1.1 | Tier 2 recipe (vanilla shulker + 8 processed shells) | 54-slot enhanced box; **inventory migrates** | | |
| 2.1.2 | Tier 3 recipe | 81 slots; upgrades/filter/name preserved | | |
| 2.1.3 | Tier 4 recipe | 108 slots; data preserved | | |
| 2.1.4 | Attempt tier recipe in 2×2 inventory grid | Fails (needs 3×3) | | |
| 2.1.5 | Dye enhanced box in crafting grid | Correct color variant | | |

### 2.2 GUI & upgrades

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 2.2.1 | Open enhanced shulker GUI | Main grid + sidebar upgrade slots | | |
| 2.2.2 | Install **Stack Depth T1/T2/T3** | Stack limits increase (64→128/256/512 etc.); highest tier wins | | |
| 2.2.3 | Install **Filter Upgrade** + set filter slot | Inserts restricted; empty filter = accept all | | |
| 2.2.4 | Install **Compacting Upgrade** | Partial stacks merge on insert (GUI) | | |
| 2.2.5 | Shift-tooltip lists installed modules | Correct modules shown | | |
| 2.2.6 | Place as block in world | Opens GUI; contents persist on break | | |

### 2.3 Automation with upgrades

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 2.3.1 | Stack depth: insert >64 of stackable item via hopper/funnel | Count persists above vanilla max | | |
| 2.3.2 | Filter: automation insert non-matching item | Rejected; goggle hint if applicable | | |
| 2.3.3 | Compacting via station automation | Merges at rate limit (64 items/pass) | | |
| 2.3.4 | Save/load world | Upgrade state and oversized stacks intact | | |

---

## 3. End farming (Phase 3)

### 3.1 Processed shulker shell pipeline

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 3.1.1 | Crush ender pearl → **Ender Pearl Dust** | Create crushing wheel recipe works | | |
| 3.1.2 | Mix dust + water in heated basin → **Liquid Ender Pearl** | Fluid appears; bucket fills | | |
| 3.1.3 | Spout Liquid Ender Pearl onto vanilla shulker shell | **Processed Shulker Shell** output | | |
| 3.1.4 | Liquid Ender Pearl textures | Still/flow render (no missing texture) | | |

### 3.2 Liquid Ender Pearl entity behavior

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 3.2.1 | Walk mob into Liquid Ender Pearl pool | Entity teleported to nearby dry ground | | |
| 3.2.2 | Enderman in fluid | **Not** teleported (exempt) | | |
| 3.2.3 | Large pool, center entity | Eventually escapes (ring search or fallback) | | |
| 3.2.4 | Teleport sound plays | Enderman teleport sound at origin | | |

### 3.3 Shulker Cage

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 3.3.1 | Right-click live shulker with **Shulker Cage** item | Cage placed; shulker trapped | | |
| 3.3.2 | Empty-hand right-click occupied cage | Shulker released alive | | |
| 3.3.3 | Break occupied cage | Item drops with shulker data preserved | | |
| 3.3.4 | Place cage from item with trapped shulker | Shulker still inside | | |
| 3.3.5 | Goggles: empty cage | Trap hint shown | | |
| 3.3.6 | Goggles: occupied cage | Trapped status + cooldown in End | | |

### 3.4 Shell farming (End dimension)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 3.4.1 | Two occupied cages, one powered by redstone, in **End** | Shulker bullet fires; **Processed Shell** drops after cooldown | | |
| 3.4.2 | Same setup in **Overworld** | No shell drops; goggles note End-only | | |
| 3.4.3 | Wait through cooldown | ~5 s between drops | | |
| 3.4.4 | Hit cage with shulker bullet (projectile) | Cage registers hit (if applicable) | | |

---

## 4. Gateways (Phase 4)

### 4.1 Fabricated End Gateway

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.1.1 | Place gateway in **Overworld**, right-click | Message: End-only; no teleport | | |
| 4.1.2 | Place in **Nether**, right-click | End-only message | | |
| 4.1.3 | Place in **End**, right-click | Teleport to central island; action bar message | | |
| 4.1.4 | Goggles on End Gateway | Summary tooltip | | |
| 4.1.5 | Fall damage after teleport | `fallDistance` reset / no lethal fall | | |

### 4.2 Cross-Dimensional Gateway Core — setup

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.2.1 | Craft core; place with correct facing | Shaft connects to back face only | | |
| 4.2.2 | Goggles: unpowered, unbound, fuel 0/4000 | Correct hints | | |
| 4.2.3 | Spin shaft | Goggles no longer show unpowered | | |

### 4.3 Binding crystal

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.3.1 | **Sneak + use** crystal on gateway A | Crystal imprinted; action bar confirmation | | |
| 4.3.2 | **Use** empty crystal on gateway B | "Crystal empty" message | | |
| 4.3.3 | **Use** imprinted crystal on gateway B (different pos/dim) | Both gateways bound; success message | | |
| 4.3.4 | **Use** imprinted crystal on **same** gateway A | "Cannot bind to itself" | | |
| 4.3.5 | Break gateway A, rebuild at new position, re-imprint | Old partner reference stale until re-bound | | |
| 4.3.6 | Bind gateways in **Overworld ↔ Nether** | Cross-dimension partner stored | | |
| 4.3.7 | Bind **Overworld ↔ End** | Works | | |
| 4.3.8 | Save/load world | Bindings and fuel persist | | |

### 4.4 Fueling

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.4.1 | Pour **Teleport Essence** bucket on core | +500 mB; empty bucket returned (Survival) | | |
| 4.4.2 | Pour **Liquid Eye of Ender** bucket | +1000 mB | | |
| 4.4.3 | Fill to 4000 mB, pour again | "Fuel tank full"; bucket not consumed | | |
| 4.4.4 | Creative mode fuel pour | Fuel added; bucket not consumed | | |
| 4.4.5 | Goggles show fuel level after each pour | Updates correctly | | |
| 4.4.6 | Craft Teleport Essence / Liquid Eye recipes | Both craftable | | |

### 4.5 Travel (empty-hand use on core)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.5.1 | Unpowered core, bound, fueled → use | "Requires kinetic power" | | |
| 4.5.2 | Powered, unbound, fueled → use | "Not bound to a partner" | | |
| 4.5.3 | Powered, bound, fuel < 250 mB → use | "Requires fuel" | | |
| 4.5.4 | Powered, bound, fueled ≥250 mB, partner **online** (powered + fueled) | Teleport to partner gateway; −250 mB | | |
| 4.5.5 | Travel **Overworld → Nether** | Dimension change + safe placement | | |
| 4.5.6 | Travel **Nether → Overworld** | Return trip works | | |
| 4.5.7 | Travel **Overworld ↔ End** | Works both directions | | |
| 4.5.8 | Partner **unpowered** but traveler in **End** | Fallback to central island + fallback message | | |
| 4.5.9 | Partner **broken/missing** from End | Fallback to central island | | |
| 4.5.10 | Partner offline from **Overworld** (not End) | Travel denied | | |
| 4.5.11 | Round-trip A→B→A | Fuel −500 total; position safe both ways | | |
| 4.5.12 | Partner gateway blocked (unsafe landing) | TeleportResolver finds nearby safe spot | | |

### 4.6 Emergency Eye Charge

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.6.1 | Use from **Overworld** | Teleport to End central island; item consumed (Survival) | | |
| 4.6.2 | Use from **Nether** | Same | | |
| 4.6.3 | Use from **End** (not on island) | Teleport to central island | | |
| 4.6.4 | Creative use | Teleport; item not consumed | | |
| 4.6.5 | Craft recipe | Produces charge | | |

### 4.7 Gateway edge cases

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 4.7.1 | Two players travel simultaneously | Both arrive safely; fuel deducted per use | | |
| 4.7.2 | Travel while riding mount / boat | Define expected behavior (player only vs mount) | | |
| 4.7.3 | Travel with inventory full at destination | Player teleports; no item loss | | |
| 4.7.4 | Chunk at partner unloaded | Partner resolves offline → fallback or deny per dimension rules | | |

---

## 5. Integration & regression

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 5.1 | Enhanced shulker through full loader → unloader cycle | Contents unchanged; upgrades intact | | |
| 5.2 | Auto-eject enhanced T4 shulker (108 slots) | Eject conditions respect slot count | | |
| 5.3 | JEI/EMI: all recipes visible and valid | No broken outputs | | |
| 5.4 | Advancements grant on first craft | Gateway, cage, upgrades unlock | | |
| 5.5 | Multiplayer (optional): two clients | Stations, cages, gateways sync | | |
| 5.6 | `/reload` or re-enter world | No duplicate entities; block entities intact | | |

---

## 6. Visual / asset pass

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 6.1 | Gateway binding crystal item texture | Custom texture in inventory/hand | | |
| 6.2 | Emergency eye charge texture | Custom texture | | |
| 6.3 | Teleport essence / liquid eye buckets | Custom textures | | |
| 6.4 | Liquid Ender Pearl in world | Animated flow; correct tint | | |
| 6.5 | End gateway / cross-dimensional core blocks | Block textures (WIP acceptable — note state) | | |
| 6.6 | Station front textures | Loader/unloader/belt variants distinct | | |
| 6.7 | Shulker cage block + trapped shulker render | Cage frame + inner shulker visible | | |
| 6.8 | EN_US lang strings | No raw translation keys in tooltips/messages | | |

---

## 7. Build & repo hygiene

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|
| 7.1 | `./gradlew compileJava` | BUILD SUCCESSFUL | | |
| 7.2 | `./gradlew runData` (if datagen changed) | Generates without error | | |
| 7.3 | Avoid `./gradlew clean` on OneDrive sync path | If clean fails: `gradlew --stop`, delete `build/` manually | | |
| 7.4 | No secrets in staged files | `.env`, credentials not committed | | |

---

## Sign-off

| Field | Value |
|-------|-------|
| Tester | |
| Date | |
| Branch / commit | |
| Minecraft + mod versions | |
| Blocking issues | |
| Follow-ups | |

---

## Known limitations (expected failures)

- Gateway fuel is **bucket pour only** — Create pipes cannot fill the core tank yet.
- **Teleport Essence** is a bucket item, not a placeable fluid block.
- End / cross-dimensional **block models** may still use placeholders until art is finished.
- Repository on **OneDrive** may cause Gradle `clean` / `processResources` file-lock errors.
