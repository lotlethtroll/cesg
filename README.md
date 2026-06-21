# Create: End Storage & Gateways (`cesg`)

NeoForge 1.21.1 add-on for **Create 6.0.10+** — shulker automation, upgradeable storage, End farming support, and dimensional gateways.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Create 6.0.10+

## Storage machines

| Block | Registry ID | Role |
|-------|-------------|------|
| **Shulker Loader** | `shulker_loader` | Stationary: hold shulker, funnel-fill contents, auto-eject whole box |
| **Shulker Unloader** | `shulker_unloader` | Stationary: hold shulker, funnel-empty contents, auto-eject when done |
| **Shulker Belt Loader** | `shulker_belt_loader` | Over-belt: hold shulker, pull matching belt items in, auto-eject filled box |
| **Shulker Belt Unloader** | `shulker_belt_unloader` | Over-belt: hold shulker, push matching contents onto belt, auto-eject emptied box |

### Shared station behavior

All four machines share the same core station logic:

- **Insert a shulker** when empty — right-click with a box, or push one in from any side via funnel/hopper
- **Remove a shulker manually** — empty hand right-click while a box is held (bypasses auto-eject)
- **Kinetic power required** for processing and auto-eject (shaft into the back face)
- **Configure** with a Create wrench, or **Shift + empty hand**
- **Goggle tooltips** show held shulker contents, power state, retention mode, funnel status, and eject readiness

#### Retention modes

| Mode | Behavior |
|------|----------|
| **Hold** | Keeps the shulker until you remove it manually |
| **Auto Eject** | Releases the whole box through an **extracting funnel** on an adjacent side when the configured fullness condition is met (station must be powered) |

#### Fullness conditions (Auto Eject only)

| Mode | Loader / Belt Loader | Unloader / Belt Unloader |
|------|----------------------|--------------------------|
| **All Slots** | Eject when every slot holds at least one item | Eject when every slot is empty |
| **Slot Threshold** | Eject when the first *N* slots (1–54) are full | Eject when at most *N* slots still hold items |

Slot-threshold loaders only accept items into the first *N* slots until eject triggers.

#### Station name

The config screen **Name** field stamps a custom display name onto every shulker held at that station. Leave blank to preserve the box's existing name.

#### Funnel setup for auto-eject

- Attach a funnel facing **into** the station on the eject side
- Funnel must be in **extracting** mode (andesite/brass) or **pushing** (belt funnel)
- Goggles show connected output funnels and **Ready for funnel extraction** when conditions are met
- A funnel in **insert** mode on the eject side blocks release — point output away from the station

### Stationary loader / unloader

- Funnels push **loose items** into (loader) or pull items out of (unloader) the held shulker's contents
- Per-slot extraction from loaders and per-slot insertion into unloaders are blocked on the capability handler — only whole-box eject exposes the shulker item

### Belt loader / unloader

- Place **two blocks above a horizontal belt** (Create's belt-processing placement rule)
- An animated nozzle extends toward the belt while items are processed; retracts after ~3 s idle
- **Front filter slot** limits which items move (empty = all non-shulker items)
- Same retention, fullness, threshold, and name config as stationary machines

### Supported shulker types

- Vanilla shulker boxes (27 slots)
- **Enhanced Shulker (Tier 2)** — 54 slots; works with all station inventory logic via shared `ShulkerInventoryAccess`

---

## Features (by phase)

### Phase 0–1 — Storage core ✅ Complete

- [x] Shulker Loader, Unloader, Belt Loader, and Belt Unloader
- [x] Hold / manual insert & remove / break drops held shulker
- [x] Create wrench + Shift-empty-hand config screen
- [x] Retention modes: Hold, Auto Eject
- [x] Fullness modes: All Slots, Slot Threshold (1–54)
- [x] Station name stamping onto held shulkers
- [x] Funnel insert (any side) and extracting-funnel whole-box eject
- [x] Goggle tooltips (contents, power, funnel wiring, eject readiness)
- [x] Vanilla + Enhanced Shulker T2 inventory integration
- [x] Belt filter slot, tube animation, belt processing hooks

### Phase 2 — Upgrades *(in progress — see roadmap below)*

Foundation already in the repo:

- **Enhanced Shulker (Tier 2)** item — 54-slot GUI with upgrade slot row (slots exist; upgrades not yet functional)
- **Stack Depth**, **Filter**, and **Compacting** upgrade items registered (placeholder items, no behavior yet)
- `EnhancedShulkerContents` data model supports tiers 2–4 (54 / 81 / 108 slots)

### Phase 3 — End farming

- **Shulker Duplication Aid** — holds a shulker for projectile-based shell farming
- **Processed Shulker Shell** — crafting material for machines and tier-ups
- **Teleport Essence / Liquid Eye of Ender buckets** — fuel-chain items

### Phase 4 — Gateways

- **Fabricated End Gateway** — End-only return to central island
- **Cross-Dimensional Gateway Core** — bound, powered, fueled cross-dimension travel with central-island fallback
- **Gateway Binding Crystal** and **Emergency Eye Charge** safety items

---

## Phase 2 roadmap

Goal: make enhanced shulkers a real upgrade path — install modules, feel the effects in-player and in automation.

### Milestone 1 — Upgrade slot plumbing

- [ ] Validate which items can go in upgrade slots (`StackDepthUpgradeItem`, `FilterUpgradeItem`, `CompactingUpgradeItem`)
- [ ] Persist installed upgrades in `EnhancedShulkerContents` and sync to clients
- [ ] Goggle / tooltip lines listing active upgrades on enhanced shulkers
- [ ] Recipes and JEI/EMI entries for T2 shulker + upgrade modules

### Milestone 2 — Stack Depth upgrade

- [ ] Define per-tier stack limits (e.g. 64 → 128 → 256) driven by installed stack-depth modules
- [ ] Apply limits inside `EnhancedShulkerItemStackHandler` insert/extract
- [ ] Ensure loader/unloader/belt machines respect enhanced stack caps via `ShulkerInventoryAccess`
- [ ] In-game test: belt loader fills oversized stacks; unloader extracts them cleanly

### Milestone 3 — Filter upgrade

- [ ] Per-shulker allow/deny list (Create filter UI or simplified slot-based filter)
- [ ] Block inserts that fail the filter in player GUI and in station `ShulkerContentsHandler` paths
- [ ] Belt loader/unloader: clarify interaction between machine front filter vs shulker filter upgrade
- [ ] Goggle hint when an item is rejected by shulker filter

### Milestone 4 — Compacting upgrade

- [ ] Auto-combine partial stacks on insert (player GUI + station funnel/belt paths)
- [ ] Optional compacting recipe table (shapeless merge) without duplicating Create's bulk processing
- [ ] Rate-limit or batch compact ticks to avoid lag on large inventories

### Milestone 5 — Tier progression

- [ ] Crafting / station recipe to upgrade vanilla shulker → Enhanced T2 (uses shells / materials from Phase 3 when ready)
- [ ] Data-driven tier 3 / 4 slot counts (81 / 108) behind additional upgrade modules or shell cost
- [ ] Migration path: `EnhancedShulkerContents.migrateFromVanilla` used in upgrade recipe

### Milestone 6 — Polish & docs

- [ ] Enhanced shulker screen art (replace placeholder models if needed)
- [ ] Update README and in-game guide with upgrade combinations
- [ ] Regression pass: all four station types × vanilla + enhanced × each upgrade combo

**Suggested implementation order:** 1 → 2 → 5 (minimal viable enhanced shulker) → 3 → 4 → 6

---

## Mod ID

`cesg`
