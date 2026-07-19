# Changelog

All notable changes to **Create: End Storage & Gateways** are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/); versions follow
[Semantic Versioning](https://semver.org/) as `MAJOR.MINOR.PATCH`:

- **MAJOR** — breaking changes: world/save format, removed blocks/items, changed
  recipe or fuel economics that invalidate existing builds.
- **MINOR** — new features and content (new blocks, upgrade modules, GUI additions).
- **PATCH** — bug fixes, texture/model polish, performance, tooltips.

Jar naming: `cesg-<minecraft version>-<mod version>.jar`. Each release is tagged
`v<mod version>` in git.

## [Unreleased]

### Changed
- Enhanced / Reinforced / Ultimate Shulker Boxes now show subtle corner-bracket
  trim on the lid top, colored by tier (ender teal / andesite / brass), in world
  and in the item preview; all 17 colors
- Reinforced (tier 3) upgrade recipe reworked to a full 3×3: 2 ender pearls
  (top corners), 5 processed shells, 1 andesite casing (bottom center) around
  the Enhanced box
- Ultimate (tier 4) upgrade recipe reworked to a full 3×3: 2 eyes of ender
  (top corners), 5 processed shells, 1 brass casing (bottom center) around the
  Reinforced box

## [1.0.0] — 2026-07-11

First release. Requires NeoForge 1.21.1 and Create 6.0.10.

### Storage
- Enhanced Shulker Boxes: place-anywhere shulker storage with upgrade slots,
  networked GUI locking, and oversized internal stacks
- Upgrade modules in three tiers (Mk I andesite / Mk II brass / Mk III diamond):
  - **Smelting** — auto-smelts inserted items, chaining multi-step recipes
    (cobblestone → stone → smooth stone) with visible live convergence
  - **Void** — voids overflow, with an independent filter configured on the module
  - **Magnet** — pulls dropped items in, range scaling by tier
  - **Filter** — whitelist ghost-filter for insertions
- Ender Barrels: paired twin barrels sharing one inventory across any distance and
  dimension, networkable by pair ID
- Storage Network: controller + terminal blocks cluster adjacent storage into one
  searchable GUI (crafting-grid terminal screen; brass-casing connected visuals)

### Shulker logistics (Create integration)
- Loader / Unloader stations (kinetic, stationary) and Belt Loader / Belt Unloader
  (belt-fed, tube intake/output) for filling and emptying docked shulkers
- Shulker Cage farming and Ender Infusion processing chain
- Liquid Eye of Ender and Teleport Essence fluids with mixing recipes

### Gateways
- End Gateway blocks bindable by channel with binding items
- Cross-Dimensional Gateway Core: multiblock frame ring, fueled by Teleport
  Essence (same-dimension) or Liquid Eye of Ender (cross-dimension)
- Gateway Port for item/fluid transfer between bound gateways, with optional
  destination chunk loading (Load Destination toggle)
- Frame conduit visuals: connected-texture window glass, pipe-smart fuel conduits
  showing the actual fluid in transit, core docking collars; core porthole shows
  tank contents (Liquid Eye wins over Essence when both are present)
- Warp Resistance effect blocks gateway teleportation (tooltip documents scope)

### Integration & config
- JEI recipe categories, advancements, and a server config
  (gateway travel cost, chunk-loading permissions)
- Goggle overlays on kinetic blocks; Ponder-style tooltips
