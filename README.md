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

- **Goggle tooltips** show held shulker contents, tier, upgrades, power state, retention mode, funnel status, and eject readiness



#### Retention modes



| Mode | Behavior |

|------|----------|

| **Hold** | Keeps the shulker until you remove it manually |

| **Auto Eject** | Releases the whole box through an **extracting funnel** on an adjacent side when the configured fullness condition is met (station must be powered) |



#### Fullness conditions (Auto Eject only)



| Mode | Loader / Belt Loader | Unloader / Belt Unloader |

|------|----------------------|--------------------------|

| **All Slots** | Eject when every slot holds at least one item | Eject when every slot is empty |

| **Slot Threshold** | Eject when the first *N* slots (1–108) are full | Eject when at most *N* slots still hold items |



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

- Stable per-side capability handlers dispatch live to holding / fill / eject handlers so Create funnels stay in sync across fill→full transitions



### Belt loader / unloader



- Place **two blocks above a horizontal belt** (Create's belt-processing placement rule)

- An animated nozzle extends toward the belt while items are processed; retracts after ~3 s idle

- **Front filter slot** limits which items move (empty = all non-shulker items); this is separate from a shulker's own **Filter Upgrade**

- Same retention, fullness, threshold, and name config as stationary machines



### Supported shulker types



| Type | Slots | Notes |

|------|-------|-------|

| Vanilla shulker box | 27 | Standard Minecraft boxes |

| Enhanced Shulker **Tier 2** | 54 | 1 upgrade module slot |

| Enhanced Shulker **Tier 3** | 81 | 2 upgrade module slots |

| Enhanced Shulker **Tier 4** | 108 | 3 upgrade module slots |



All enhanced tiers use the same placeable block (17 dye variants). Tier is stored on the item and shown in the name (` - Tier N`). Stations read slot counts and upgrade behavior through shared `ShulkerInventoryAccess`.



---



## Enhanced shulker upgrades



Install modules in the **sidebar upgrade slots** (only items in those slots are active).



| Module | Effect |

|--------|--------|

| **Stack Depth Mk I / II / III** | Raises per-slot stack limits proportionally (e.g. 64→128/256/512; 16→32/64/128). Highest tier wins. |

| **Filter Upgrade** | Restricts inserts using a filter configuration slot (any item or Create filter). Empty filter = accept all. |

| **Compacting Upgrade** | Merges partial stacks on insert (GUI and automation). |



Shift-tooltip on enhanced shulkers lists installed modules. Goggles on stations show held-box tier, upgrades, stack limit, filter, and compacting state.



---



## Tier progression (crafting)



All tier recipes **migrate inventory**, installed upgrades, filter config, and custom name.



### Tier 2 — Vanilla → Enhanced (54 slots)



```

SSS

SBS   B = any vanilla shulker (keeps dye color)

SSS   S = processed shulker shell ×8

```



Recipe ID: `cesg:enhanced_shulker_tier_2`



### Tier 3 — Enhanced T2 → T3 (81 slots)



```

PSP

SBS   B = tier 2 enhanced shulker

SC.   P = ender pearl ×2, C = brass casing, S = shell ×4

```



Recipe ID: `cesg:enhanced_shulker_tier_3`



### Tier 4 — Enhanced T3 → T4 (108 slots)



```

SES

SBS   B = tier 3 enhanced shulker

CC.   E = ender eye, C = brass casing ×2, S = shell ×4

```



Recipe ID: `cesg:enhanced_shulker_tier_4`



Requires a **3×3 crafting grid** (not the 2×2 inventory grid).



---



## Features (by phase)



### Phase 0–1 — Storage core ✅ Complete



- [x] Shulker Loader, Unloader, Belt Loader, and Belt Unloader

- [x] Hold / manual insert & remove / break drops held shulker

- [x] Create wrench + Shift-empty-hand config screen

- [x] Retention modes: Hold, Auto Eject

- [x] Fullness modes: All Slots, Slot Threshold (1–108)

- [x] Station name stamping onto held shulkers

- [x] Funnel insert (any side) and extracting-funnel whole-box eject

- [x] Goggle tooltips (contents, power, funnel wiring, eject readiness)

- [x] Vanilla + enhanced shulker inventory integration

- [x] Belt filter slot, tube animation, belt processing hooks

- [x] Capability dispatch + invalidation for reliable multi-cycle auto-eject



### Phase 2 — Upgrades ✅



| Milestone | Status |

|-----------|--------|

| **1 — Upgrade slot plumbing** | ✅ Done |

| **2 — Stack depth** | ✅ Done |

| **5 — Tier progression (T3/T4)** | ✅ Done — recipes, GUI, creative tab colors |

| **3 — Filter (automation polish)** | ✅ Done — goggle reject hints (sneak + held item probe) |

| **4 — Compacting polish** | ✅ Done — rate-limited automation (64 items/pass), station tick drain, goggle backlog hint |

| **6 — Polish & docs** | ✅ Done — manual regression pass |



### Phase 3 — End farming *(in progress)*



| Item | Registry ID | Role |

|------|-------------|------|

| **Processed Shulker Shell** | `shulker_shell` | Tier upgrades, machines; craft from vanilla shell + iron sheet |

| **Shulker Cage** | `shulker_cage` | Trap a live shulker; shulker bullets drop processed shells in the End |

| **Teleport Essence bucket** | `teleport_essence_bucket` | Gateway fuel precursor (Phase 4 consumer) |

| **Liquid Eye of Ender bucket** | `liquid_eye_of_ender_bucket` | Advanced gateway fuel (Phase 4 consumer) |



#### Shulker Cage



- **Trap** a live shulker by **right-clicking it with a Shulker Cage** (places the cage and captures the mob)

- **Release** with an empty-hand right-click on the cage, or break the block

- **End dimension only** for shell drops — **shulker bullets** (vanilla duplication projectile) drop **Processed Shulker Shells** on a ~5s cooldown

- **Goggles** show trapped shulker, cooldown, and End-dimension status

- **Recipe:** processed shell ×2, end stone ×2, ender pearl (center) in a 3×3 grid



**Next up:** Phase 4 gateways (travel, binding, fuel plumbing).



### Phase 4 — Gateways



- **Fabricated End Gateway** — End-only return to central island

- **Cross-Dimensional Gateway Core** — bound, powered, fueled cross-dimension travel with central-island fallback

- **Gateway Binding Crystal** and **Emergency Eye Charge** safety items



---



## Mod ID



`cesg`


