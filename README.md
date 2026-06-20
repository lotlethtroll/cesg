# Create: End Storage & Gateways (`cesg`)

NeoForge 1.21.1 add-on for **Create 6.0.10+** — shulker automation, upgradeable storage, End farming support, and dimensional gateways.

## Requirements

- Minecraft 1.21.1

- NeoForge 21.1.x

- Create 6.0.10+

## Storage machines

| Block | Registry ID | Role |

|-------|-------------|------|

| **Shulker Loader** | `shulker_loader` | Stationary: dock shulker, funnel-fill contents, auto-eject whole box |

| **Shulker Unloader** | `shulker_unloader` | Stationary: dock shulker, funnel-empty contents, auto-eject box when done |

| **Shulker Belt Loader** | `shulker_belt_loader` | Inline: shulker rides belt, items pulled in from side inventory |

| **Shulker Belt Unloader** | `shulker_belt_unloader` | Inline: shulker rides belt, items pushed out to side inventory |

### Stationary loader / unloader

- Insert a **shulker box from any side** when empty (funnel, hopper, etc.)

- Loose items only fill (loader) or empty (unloader) once a shulker is docked

- **Auto Eject** releases the whole shulker through an extracting funnel when the configured condition is met

- Loader threshold = full slots before eject; unloader threshold = slots remaining before eject

## Features (by phase)

### Phase 0–1 — Storage core

- Shulker Loader / Unloader (stationary) and Belt Loader / Belt Unloader

- Enhanced shulker support via shared inventory access

### Phase 2 — Upgrades

- **Enhanced Shulker (Tier 2)** — 54 slots + upgrade slot bank with custom GUI

- **Stack depth, filter, compacting** upgrade modules (foundation in place)

### Phase 3 — End farming

- **Shulker Duplication Aid** — holds a shulker for projectile-based shell farming

- **Processed Shulker Shell** — crafting material for machines and tier-ups

- **Teleport Essence / Liquid Eye of Ender buckets** — fuel-chain items

### Phase 4 — Gateways

- **Fabricated End Gateway** — End-only return to central island

- **Cross-Dimensional Gateway Core** — bound, powered, fueled cross-dimension travel with central-island fallback

- **Gateway Binding Crystal** and **Emergency Eye Charge** safety items

## Mod ID

`cesg`

## Design notes (backlog)

- **Shulker renaming** — anvil-style naming on docked shulkers in automation; not scoped yet.

- **World migration** — `shulker_dock` blocks from earlier builds will be removed; place new `shulker_loader` blocks in existing worlds.
