# CESG Manual Test Plan

Use this checklist when validating a release or after large changes. Run in **Creative** for setup speed, then repeat critical paths in **Survival** where noted.

**Environment:** Minecraft 1.21.1 · NeoForge · Create 6.0.10+

**Legend:** `[ ]` not tested · `[x]` pass · `[!]` fail (note in *Notes* column)

---

## Verification status & remaining path (as of 2026-07-02)

**Verified in-game:** §4C.1 config file loads (global `run/config/cesg-server.toml`) · §4A.1 picker opens,
imprint/bind works · §4A.2 basic item flow through Gateway Ports · §4B terminal opens, aggregation,
search, deposit/withdraw basics.

**Changed since last session — RE-TEST:** §4A.1.7–7c partner liveness (3-state, silent when unloaded) ·
§4A.1.8–11 gateway naming + rebind warning · §4A.2 port goggle counts (client sync fix) ·
§4B.6–8b terminal click contract (cursor pickup / shift to inventory / carried deposit) ·
§6.9–6.10 new 3D models (controller frame+core, terminal console tilt).

**Never tested — full remaining path, in recommended order:**

1. §4D Ender Barrel (all rows — newest feature)
2. §4B.11/4B.11b networked-shulker GUI lock
3. §4A.2.5–4A.2.8 port edge cases (unpowered buffering, unloaded partner, facing ports, break-drops)
4. §4E Teleport potions (no prior rows existed — section added below)
5. §4F Ponder scenes (W-key walkthroughs, built but never viewed)
6. §5.4 advancements (craft chain + travel award)
7. §4C.2/4C.3 config hot-reload
8. §5.5–5.6 multiplayer + /reload regression
9. Survival-mode repeat of critical paths + §6 visual pass + Sign-off

> **JEI note:** the dev runtime ships EMI only; the JEI plugin is compiled but needs either a
> `localRuntime` JEI dependency in build.gradle or a real modpack instance to verify (§5.3b).

---

## 0. Smoke test

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 0.1 | `./gradlew runClient` (no `clean` unless needed) | Game launches, mod loads, no crash on title screen |x| |
| 0.2 | Open creative tab **Create: End Storage & Gateways** | All blocks/items appear with textures (no missing purple/black) |x| |
| 0.3 | Create new world, `/gamemode creative` | World loads, JEI/EMI shows CESG recipes |!|EMI works. FIXED: JEI added to dev runtime (localRuntime in build.gradle) — retest with both viewers |

---

## 1. Shulker stations (Loader / Unloader / Belt Loader / Belt Unloader)

### 1.1 Placement & power

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.1.1 | Place stationary loader and unloader | Blocks place, models render correctly |x| |
| 1.1.2 | Place belt loader/unloader **two blocks above** a horizontal Create belt | Placement succeeds; invalid placement blocked |!|FIXED: aiming at the belt (or 1-2 blocks above) auto-places at belt+2; no belt in reach = blocked with action-bar message — retest |
| 1.1.2b | Aim at the belt itself with a belt station item | Station places itself two blocks up automatically | | |
| 1.1.2c | Try placing with no belt below | Blocked; message "Belt stations sit two blocks above a Create belt" | | |
| 1.1.3 | Connect shaft to **back** face, spin up | Goggles show powered; processing works when powered x| |
| 1.1.4 | Remove/stop shaft | Goggles show unpowered; no processing or auto-eject |x| |

### 1.2 Shulker docking

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.2.1 | Right-click empty station with vanilla shulker box | Shulker docks; goggles show contents |x| |
| 1.2.2 | Empty-hand right-click docked station | Shulker removed to player inventory |x| |
| 1.2.3 | Push shulker in via funnel from any side | Shulker docks |x| |
| 1.2.4 | Break station while holding shulker | Shulker drops (not voided) |x| |
| 1.2.5 | Dock **enhanced shulker** (T2/T3/T4) | Correct slot count shown in goggles |x| |
| 1.2.6 | Dock **dyed enhanced shulker** | Works; color preserved |x| |

### 1.3 Item transfer (stationary)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.3.1 | Loader + funnel inserting items | Items enter docked shulker |x| |
| 1.3.2 | Unloader + funnel extracting items | Items leave docked shulker |x| |
| 1.3.2b | UNPOWERED unloader + side funnel | NOTHING moves — no extraction, and especially no duplication (count in box stays exact) |x| |
| 1.3.2c | Powered loader fed a large stack faster than its speed budget | No items voided: source keeps what the station didn't accept |x| |
| 1.3.3 | Loader: funnel cannot extract individual slots | Only whole-box eject exposes shulker item |x| |
| 1.3.4 | Unloader: funnel cannot insert into shulker slots | Insert blocked on capability side |x| |

### 1.4 Belt variants

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.4.1 | Belt loader over moving belt with items | Matching items pulled into docked shulker |x| |
| 1.4.2 | Belt unloader over belt | Items pushed onto belt from shulker |x| |
| 1.4.3 | Set **front filter slot** (item or Create filter) | Only matching items transfer; empty = all non-shulker |x| |
| 1.4.4 | Idle ~3 s after processing | Nozzle/tube animation retracts |x| |
| 1.4.5 | Funnel/hopper on a Belt Unloader side while a box is docked | CANNOT drain contents (tube-only output); docking + finished-box eject still work |x| |

### 1.5 Configuration (wrench or Shift + empty hand)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.5.1 | Open config UI on each station type | Correct title; all controls work |x| |
| 1.5.2 | **Retention: Hold** | Shulker never auto-ejects |x| |
| 1.5.3 | **Retention: Auto Eject** + **All Slots** (loader) | Ejects when every slot has ≥1 item |x| |
| 1.5.4 | **All Slots** (unloader) | Ejects when every slot empty |x | |
| 1.5.5 | **Slot Threshold** loader (N = 5, 27, 54…) | Only first N slots fill; ejects at threshold |x| |
| 1.5.6 | **Slot Threshold** unloader | Ejects when at most N slots still hold items |x| |
| 1.5.7 | Set **station name** | Name stamped on every shulker held at station | |x|
| 1.5.8 | Clear station name | Existing shulker name preserved |x| |

### 1.6 Auto-eject & funnels

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.6.1 | Extracting funnel on ANY side, conditions met | Whole shulker ejected to funnel/belt |x| |
| 1.6.1b | Smart chute below / hopper adjacent, conditions met | Whole shulker pulled out (extractor-agnostic eject) |x| |
| 1.6.2 | Funnel in **insert** mode on a side | Simply never pulls; input stalls until box leaves (no dupes) |x| |
| 1.6.3 | Goggles: **Ready to eject — extract from any side** | Shows when powered + conditions met |x| |
| 1.6.4 | Multi-cycle: eject → new shulker inserted → fill → eject again | Capabilities stay in sync (no stuck funnel) |x| |

### 1.7 Filter goggle hints (Sneak + goggles)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 1.7.1 | Hold item that passes station filter | Goggles: accepts |x| |
| 1.7.2 | Hold item blocked by station filter | Goggles: rejected at station |x| |
| 1.7.3 | Loader with shulker filter upgrade | Chain: station filter then shulker filter |x| |
| 1.7.4 | Hold item blocked by docked shulker filter | Goggles: rejected at shulker |x| |

---

## 2. Enhanced shulker boxes

### 2.1 Crafting & tiers

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 2.1.1 | Tier 2 recipe (vanilla shulker + 8 processed shells) | 54-slot enhanced box; **inventory migrates** |x| |
| 2.1.2 | Tier 3 recipe | 81 slots; upgrades/filter/name preserved |x| |
| 2.1.3 | Tier 4 recipe | 108 slots; data preserved |x| |
| 2.1.4 | Attempt tier recipe in 2×2 inventory grid | Fails (needs 3×3) |x| |
| 2.1.5 | Dye enhanced box in crafting grid | Correct color variant |x| |

### 2.2 GUI & upgrades

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 2.2.1 | Open enhanced shulker GUI | Main grid + sidebar NOTCH only as tall as its slots (grows when filter slot appears) |!|FIXED: sidebar now renders as a notch — retest|
| 2.2.2 | Install **Stack Depth T1/T2/T3** | Stack limits increase (64→128/256/512 etc.); highest tier wins |x| |
| 2.2.2b | Oversized stack (512) in a slot: press a hotbar NUMBER KEY over it | NO crash; one vanilla stack (64) moves to the empty hotbar slot, 448 stay |x| |
| 2.2.2c | Same swap onto an OCCUPIED hotbar slot | No-op (nothing moves, no crash) |x| |
| 2.2.2d | Creative middle-click an oversized stack | Cursor gets a clamped legal stack (64), no crash |x| |
| 2.2.2e | Cursor-pickup / shift-click / Q-throw from an oversized slot | All clamp to 64 per action (vanilla behavior, regression check) |x| |
| 2.2.3 | Install **Filter Upgrade** + set filter slot | Inserts restricted; empty filter = accept all |!|FIXED: instant slot on install, aqua accent + ghost placeholder; GHOST semantics: click with item sets a copy (NOT consumed), empty-hand clears — retest|
| 2.2.3b | Set filter, then REMOVE the Filter Upgrade | No item lost (config is a ghost copy); reinstalling shows a cleared filter |x| |
| 2.2.3c | Shift-click items with an empty filter slot configured | Items go to STORAGE, never hijacked into the filter slot |x| |
| 2.2.4 | Install **Compacting Upgrade** | Partial stacks merge on insert (GUI) |x| |
| 2.2.5 | Shift-tooltip lists installed modules | Correct modules shown |x| |
| 2.2.5b | Install **Smelting Upgrade**, insert iron ore / raw beef (GUI click, shift-click, hotbar-swap number key, AND hopper) | Stored as iron ingot / steak in ALL paths; no-recipe items store unchanged |!|FIXED: all GUI paths incl. hotbar-swap route through the module transform — retest|
| 2.2.5c | Smelting: feed more raw items than fit | Uninserted leftover stays RAW at the source (no free smelting of rejected items) |x| |
| 2.2.5c2 | Fill a box with raw gold FIRST, then install the Smelting Upgrade | Entire existing inventory converts to ingots immediately (no interaction needed) |x| |
| 2.2.5c3 | Same, with Compacting also installed | Converted stacks merge after the smelt pass |x| |
| 2.2.5d | Install **Void Upgrade**, fill box with cobble, keep inserting cobble (GUI + automation) | Overflow accepted and destroyed once every slot is truly full |!|FIXED: GUI paths — retest|
| 2.2.5d2 | Void module's CRIMSON config slot (below the aqua filter slot): click with an item | Void-list set (ghost, nothing consumed); only MATCHING overflow voids; empty = void any stored type | | |
| 2.2.5d3 | Void-list config survives removing/reinstalling the module | Config is stored ON the module item — it travels with it (check tooltip after removal) | | |
| 2.2.5e | Void: full-of-cobble box offered iron | Iron REJECTED normally (only stored types void); box with any empty slot never voids |x| |
| 2.2.5f | Craft both new upgrades | Smelting: blaze powder + blast furnace + shell; Void: obsidian + pearl dust + brass casing |x| |
| 2.2.5g | Install **Magnet Mk I** in a PLACED box, drop items 3 blocks away | Items pulled in and inserted; range ~4 blocks |x| |
| 2.2.5h | Magnet Mk II / Mk III | Range ~7 / ~10 blocks, visibly faster pull; only highest tier applies |x| |
| 2.2.5i | Magnet box FULL (or item filtered out) | Non-accepted items are NOT attracted (no orbiting) |x| |
| 2.2.5j | Magnet + open GUI | Pull pauses while the box is open; resumes on close |x| |
| 2.2.5k | Magnet + Void combo on full box | Items still vacuumed and destroyed (intended trash-vacuum combo) |x| |
| 2.2.5l | Magnet does NOT pull dropped shulker boxes | Shulker box items ignored (no nesting) |x| |
| 2.2.6 | Place as block in world | Opens GUI; contents persist on break |x| |

### 2.3 Automation with upgrades

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 2.3.1 | Stack depth: insert >64 of stackable item via hopper/funnel | Count persists above vanilla max |x| |
| 2.3.2 | Filter: automation insert non-matching item | Rejected; goggle hint if applicable |x| |
| 2.3.3 | Compacting via station automation | Merges at rate limit (64 items/pass) |x| |
| 2.3.4 | Save/load world | Upgrade state and oversized stacks intact |x| |

---

## 3. End farming (Phase 3)

### 3.1 Processed shulker shell pipeline

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 3.1.1 | Crush ender pearl → **Ender Pearl Dust** | Create crushing wheel recipe works |x| |
| 3.1.2 | Mix dust + water in heated basin → **Liquid Ender Pearl** | Fluid appears; bucket fills |x| |
| 3.1.3 | Spout Liquid Ender Pearl onto vanilla shulker shell | **Processed Shulker Shell** output |x| |
| 3.1.4 | Liquid Ender Pearl textures | Still/flow render (no missing texture) |x| |

### 3.2 Liquid Ender Pearl entity behavior

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 3.2.1 | Walk mob into Liquid Ender Pearl pool | Entity teleported to nearby dry ground |x| |
| 3.2.2 | Enderman in fluid | **Not** teleported (exempt) |x| |
| 3.2.3 | Large pool, center entity | Eventually escapes (ring search or fallback) |x| |
| 3.2.4 | Teleport sound plays | Enderman teleport sound at origin |x| |

### 3.3 Shulker Cage

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 3.3.1 | Right-click live shulker with **Shulker Cage** item | Cage placed; shulker trapped |x| |
| 3.3.2 | Empty-hand right-click occupied cage | Shulker released alive |x| |
| 3.3.3 | Break occupied cage | Item drops with shulker data preserved |x| |
| 3.3.4 | Place cage from item with trapped shulker | Shulker still inside |x| |
| 3.3.5 | Goggles: empty cage | Trap hint shown |x| |
| 3.3.6 | Goggles: occupied cage | Trapped status + cooldown in End |x| |

### 3.4 Shell farming (End dimension)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 3.4.1 | Two occupied cages, one powered by redstone, in **End** | Shulker bullet fires; **Processed Shell** drops after cooldown |x| |
| 3.4.2 | Same setup in **Overworld** | No shell drops; goggles note End-only |x| |
| 3.4.3 | Wait through cooldown | ~5 s between drops |x| |
| 3.4.4 | WILD shulker's bullet strikes an occupied cage (End). Repro: trap shulker in cage, lure a free shulker to target you, stand so the cage blocks the bullet's path | Shell drops (same cooldown/End rules as powered farm). Intent: bonus shells from crossfire — the powered paired-cage loop does NOT spawn real bullets, so only wild shulkers trigger this |!|CLARIFIED: wired & intended as flavor bonus; tooltip now documents it — retest with repro|

---

## 4. Gateways (Phase 4)

### 4.1 Fabricated End Gateway

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.1.1 | Place gateway in **Overworld**, right-click | Message: End-only; no teleport |x| |
| 4.1.2 | Place in **Nether**, right-click | End-only message |x| |
| 4.1.3 | Place in **End**, right-click | Teleport to central island; action bar message |x| |
| 4.1.4 | Goggles on End Gateway | Summary tooltip |x| |
| 4.1.5 | Fall damage after teleport | `fallDistance` reset / no lethal fall |x| |

### 4.2 Cross-Dimensional Gateway Core — setup

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.2.1 | Craft core; place with correct facing | Shaft connects to back face only |x| |
| 4.2.2 | Goggles: unpowered, unbound, fuel 0/4000 | Correct hints |x| |
| 4.2.3 | Spin shaft | Goggles no longer show unpowered |x| |

### 4.3 Binding crystal

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.3.1 | **Sneak + use** crystal on gateway A | Crystal imprinted; action bar confirmation |x| |
| 4.3.2 | **Use** empty crystal on gateway B | "Crystal empty" message |x| |
| 4.3.3 | **Use** imprinted crystal on gateway B (different pos/dim) | Both gateways bound; success message |x| |
| 4.3.4 | **Use** imprinted crystal on **same** gateway A | "Cannot bind to itself" |x| |
| 4.3.5 | Break gateway A, rebuild at new position, re-imprint | Old partner reference stale until re-bound |x| |
| 4.3.6 | Bind gateways in **Overworld ↔ Nether** | Cross-dimension partner stored |x| |
| 4.3.7 | Bind **Overworld ↔ End** | Works |x| |
| 4.3.8 | Save/load world | Bindings and fuel persist |x| |

### 4.4 Fueling

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.4.1 | Pour **Teleport Essence** bucket on core | +500 mB; empty bucket returned (Survival) |x| |
| 4.4.2 | Pour **Liquid Eye of Ender** bucket | +1000 mB |x| |
| 4.4.3 | Fill to 4000 mB, pour again | "Fuel tank full"; bucket not consumed |x| |
| 4.4.4 | Creative mode fuel pour | Fuel added; bucket not consumed |x| |
| 4.4.5 | Goggles show fuel level after each pour | Updates correctly |x| |
| 4.4.6 | Craft Teleport Essence / Liquid Eye recipes | Both craftable |x| |

### 4.5 Travel (empty-hand use on core)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.5.1 | Unpowered core, bound, fueled → use | "Requires kinetic power" |x| |
| 4.5.2 | Powered, unbound, fueled → use | "Not bound to a partner" |x| |
| 4.5.3 | Powered, bound, fuel < 250 mB → use | "Requires fuel" |x| |
| 4.5.4 | Powered, bound, fueled ≥250 mB, partner **online** (powered + fueled) | Teleport to partner gateway; −250 mB |x| |
| 4.5.5 | Travel **Overworld → Nether** | Dimension change + safe placement |x| |
| 4.5.6 | Travel **Nether → Overworld** | Return trip works |x| |
| 4.5.7 | Travel **Overworld ↔ End** | Works both directions |x| |
| 4.5.8 | Partner **unpowered** but traveler in **End** | Fallback to central island + fallback message |x| |
| 4.5.9 | Partner **broken/missing** from End | Fallback to central island |x| |
| 4.5.10 | Partner offline from **Overworld** (not End) | Travel denied |x| |
| 4.5.11 | Round-trip A→B→A | Fuel −500 total; position safe both ways |x| |
| 4.5.12 | Partner gateway blocked (unsafe landing) | TeleportResolver finds nearby safe spot |x| |

### 4.6 Emergency Eye Charge

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.6.1 | Use from **Overworld** | Teleport to End central island; item consumed (Survival) |x| |
| 4.6.2 | Use from **Nether** | Same |x| |
| 4.6.3 | Use from **End** (not on island) | Teleport to central island |x| |
| 4.6.4 | Creative use | Teleport; item not consumed |x| |
| 4.6.5 | Craft recipe | Produces charge |x| |

### 4.7 Gateway edge cases

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4.7.1 | Two players travel simultaneously | Both arrive safely; fuel deducted per use |x| |
| 4.7.2 | Travel while riding mount / boat | DEFINED: everything in the portal goes through; each ENTITY pays the travel cost individually (player + boat = 2x); riders dismount on dimension change (vanilla rule); if fuel runs out mid-group, stragglers stay behind |x|DEFINED as tested: "everything goes", per-entity fuel; goggles now read "per entity" |
| 4.7.2b | Throw a stack of dropped items through the portal | Each item ENTITY costs a travel charge — bulk items belong in Gateway Ports (free), not the portal plane | | |
| 4.7.3 | Travel with inventory full at destination | Player teleports; no item loss |x| |
| 4.7.4 | Chunk at partner unloaded | Partner resolves offline → fallback or deny per dimension rules |x| |

---

## 4A. Gateway logistics — channels & ports (Phase 6A)

### 4A.1 Channels & destination picker

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4A.1.1 | Empty-hand click a Gateway Core (not sneaking) | Channel picker opens: 4×4 grid, active channel gold, bound channels green |x| |
| 4A.1.2 | Pick a different channel | Screen closes; goggles show new "Active channel" |x |
| 4A.1.3 | Sneak + empty-hand click | Old status message (ready/unpowered/etc.), no screen |x| |
| 4A.1.4 | Imprint crystal on core A (channel 2 active), apply to core B (channel 5 active) | Message "bound on channel 5 (partner channel 2)"; B ch5 → A, A ch2 → B |x| |
| 4A.1.5 | Bind different gateways on different channels of one core | Switching the active channel switches the travel destination |x| |
| 4A.1.6 | Load a pre-6A save with a bound gateway | Legacy binding appears on channel 1 |x| |
| 4A.1.7 | Goggles on core, partner in LOADED chunks (same dimension nearby) | Green "powered and fueled" while running; red "unpowered or out of fuel" when you stop it (~1s) |x| |
| 4A.1.7b | Goggles on core, partner in another dimension (chunks unloaded) | NO partner-status line at all (not red, not gray) — unknown is silent |x| |
| 4A.1.7c | Travel through, come back | Green line appears (travel verified the partner) until the far side unloads again |x| |
| 4A.1.8 | Type a name ("End Farm") in the picker's name box, close WITHOUT picking a channel | Name saved (reopen to confirm) |x| |
| 4A.1.9 | Imprint a crystal on the named gateway, hover the crystal | Tooltip: "Imprinted: End Farm", location, return channel |x| |
| 4A.1.10 | Bind that crystal at another gateway; hover its channel + goggles | Picker tooltip "Bound: End Farm — dim (x, y, z)"; goggles show "Destination: End Farm" |x| |
| 4A.1.11 | Bind a second crystal onto an already-bound channel | Action-bar message notes "previous binding replaced" |x| |

### 4A.2 Gateway Port transfer

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4A.2.1 | Craft Gateway Port (end stone bricks + brass + hopper + pearl dust) | Recipe works; copper block w/ teal socket |x| |
| 4A.2.2 | Place a port beside a frame of gateway A and one beside gateway B; bind + power + fuel both | Ports found via ring walk | | |
| 4A.2.3 | Funnel/belt items into port A | Items appear in port B within ~0.5s; extractable by funnels/belts from B | | |
| 4A.2.4 | Pump fluid into port A | Fluid arrives in port B's receive tank; never eaten as gateway fuel | | |
| 4A.2.5 | Unpower gateway A (or drain fuel below travel cost) | Transfers stop; port A buffers inserts (no loss) | | |
| 4A.2.6 | Partner chunk unloaded | Port buffers and retries; nothing is lost or duplicated | | |
| 4A.2.7 | Two ports facing each other on the same ring pair | No ping-pong loops (send/receive buffers are separate) | | |
| 4A.2.8 | Break a port with buffered items | Items drop | | |
| 4A.2.9 | Picker: toggle "Keep destination loaded: ON" (Overworld side), fill port, leave the End unloaded | Transfers keep flowing — partner chunk stays loaded and ticking; goggles show gold "keeping loaded" line | | |
| 4A.2.10 | With loading ON, switch the core to a different channel mid-transfer | Tickets move to the NEW destination (old side may unload); next flush delivers to the new partner's ports; old partner keeps items already received | | |
| 4A.2.11 | Toggle OFF / break the core / unbind the channel | Tickets released — destination chunk unloads normally | | |
| 4A.2.12 | Restart the world with loading ON | Force-loading persists (block-owned tickets survive restarts) | | |
| 4A.2.13 | Server config gateway.allowChunkLoading=false | Toggle button hidden; existing toggles force to OFF | | |

---

## 4B. Storage network (Phase 6D)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4B.1 | Place Storage Network Controller; attach enhanced shulkers + stations by adjacency | Right-click controller reports member/inventory/type counts | | |
| 4B.2 | Place Storage Terminal touching the cluster; right-click | Terminal UI opens (search box, 9×6 grid, player inventory) | | |
| 4B.3 | Terminal placed away from any controller | "No Storage Network Controller connected" | | |
| 4B.4 | Items across several enhanced shulkers | Aggregated counts (one row per item type), sorted by count | | |
| 4B.5 | Type in search box | Grid filters by display name; E key while typing does NOT close the screen | | |
| 4B.6 | Left-click an entry (empty cursor) | Picks up a full stack ONTO THE CURSOR | | |
| 4B.6b | Shift + left-click an entry | Sends a stack directly to player inventory | | |
| 4B.7 | Right-click an entry (empty cursor) | Picks up exactly 1 onto the cursor; repeat right-clicks stack onto it | | |
| 4B.7b | With a stack on the cursor, left-click the grid | Deposits the whole carried stack into the network (right-click deposits 1) | | |
| 4B.8 | Shift-click a stack in player inventory slots | Deposits into the network (visible in grid within ~0.5s) | | |
| 4B.8b | Close the terminal with a stack still on the cursor | Stack returns to player inventory (no loss) | | |
| 4B.9 | Second player modifies network contents while terminal open | View refreshes automatically | | |
| 4B.10 | Withdraw from a shulker with stack-depth upgrades (256/slot) | Counts correct; extraction respects per-slot behavior | | |
| 4B.11 | Right-click an enhanced shulker that belongs to a network | GUI does NOT open; action-bar message "part of a storage network — use a Storage Terminal"; box stays visible in the terminal | | |
| 4B.11b | Break the controller (or isolate the box), right-click again | GUI opens normally | | |
| 4B.12 | Place/break a member block next to the cluster | Terminal picks up the membership change within ~1s (20-tick cluster cache) | | |

---

## 4D. Ender Barrel (twinned inventory)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4D.1 | Craft (obsidian + pearls + pearl dust around a barrel) | ONE stack of 2 Ender Barrels; tooltip shows "Pair: #xxxxxxxx" on both |x| |
| 4D.2 | Craft a second pair | Different pair code — pairs are independent |x| |
| 4D.3 | Place both twins apart; put items in one, open the other | Same 27 slots from both ends (vanilla chest UI) |x| |
| 4D.4 | Both GUIs open at once (2 players or 2 windows) | Behaves like one shared chest; no desync |x| |
| 4D.5 | One twin in the Overworld, one in the End | Same inventory across dimensions |x| |
| 4D.6 | Hopper/funnel into one twin, hopper out of the other | Items flow through the pair |x| |
| 4D.7 | Break a barrel | Drops with its pair code intact (tooltip); re-placing rejoins the pool |x| |
| 4D.8 | Break BOTH barrels with items still in the pool | Items persist in the pair; placing either barrel again recovers them |x| |
| 4D.9 | Creative-menu barrel (unpaired) placed from a stack of 2+ | First placement tags the rest of the stack as its twins |x| |
| 4D.10 | Exit world, reload | Pool contents persist (world SavedData) |x| |
| 4D.11 | Barrel touching a storage network cluster | Pool contents appear in the Storage Terminal; withdraw/deposit works |x| |
| 4D.12 | BOTH twins of one pair in the same network | Contents counted ONCE (no doubling); barrel GUI still opens (no lock — shared pool is live) |x| |
| 4D.13 | Two barrels from DIFFERENT pairs in inventory | They never stack (pair id is a component); twins of the same pair do stack |x| |

---

## 4C. Config (Phase 6C)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4C.1 | Config file exists at `run/config/cesg-server.toml` (dev) / `config/cesg-server.toml` (release) — NeoForge 1.21 puts SERVER configs in the global config dir, NOT per-world `saves/*/serverconfig/` | File present with gateway + shulkerCage sections |x| |
| 4C.2 | Edit gateway.travelCostMb (file is hot-watched; no restart needed) | Goggles show new travel cost; fuel drains accordingly |x| |
| 4C.3 | Edit shulkerCage.harvestCooldownTicks | Cage harvest rate changes |x| |

---

## 4E. Teleport & Teleport Resistance potions (Phase 6F)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4E.1 | Brew: awkward potion + Ender Pearl Dust | Teleport potion |x| |
| 4E.2 | Redstone / glowstone on Teleport potion | Extended / amplified variants |x| |
| 4E.3 | Drink Teleport potion | Random chorus-style blinks every few seconds for the duration |x| |
| 4E.4 | Blink near a cliff/void edge | Lands on safe ground (no void deaths from the effect itself) |x| |
| 4E.5 | Brew: Teleport potion + fermented spider eye | Teleport Resistance potion |x| |
| 4E.6 | With Resistance active: throw an ender pearl | Teleport is CANCELLED (pearl consumed or no-op, no teleport) |x| |
| 4E.7 | With Resistance active: walk into a bound gateway portal | Blocked with "resistant" action-bar message; no fuel consumed |x| |
| 4E.8 | With Resistance active: enderman tries to teleport-dodge? (observe) | Only the drinker is protected; mobs unaffected unless they have the effect |x| |
| 4E.9 | Resistance + Teleport potion together | Resistance wins — no blinks while both active |x| |
| 4E.10 | Effect icons in HUD/inventory | Custom textures render (no missing sprite) |x| |

---

## 4F. Ponder scenes (6C integration)

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 4F.1 | Hover Shulker Loader item, hold W | Scene plays: shaft + station + shulker + barrel context, stepwise text |x| |
| 4F.2 | Same for Unloader, Belt Loader, Belt Unloader | Each scene plays with correct schematic and localized text (no raw keys) |x| |
| 4F.3 | Hover Gateway Core, hold W | 5x5 frame-ring scene: assemble → fuel → bind → travel narrative |x| |
| 4F.4 | Scene camera/timing | No missing blocks, z-fighting, or text overlapping geometry |x| |

---

## 5. Integration & regression

| # | Step | Expected | OK | Notes |
|---|------|----------|----|-------|

| 5.1 | Enhanced shulker through full loader → unloader cycle | Contents unchanged; upgrades intact | | |
| 5.2 | Auto-eject enhanced T4 shulker (108 slots) | Eject conditions respect slot count | | |
| 5.3 | EMI: Ender Infusing category, workstation, all recipes render | No broken outputs; fluid amounts shown | | |
| 5.3b | JEI: same category renders (needs `localRuntime` JEI dep in build.gradle, or a real instance — dev runtime ships EMI only) | Category, catalyst, recipes with fluid tooltips | | |
| 5.3c | Ender Barrel pairing recipe visible in recipe viewer | Shaped grid shows; crafted output is 2 barrels | | |
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
| 6.9 | Storage Network Controller 3D model | Brass frame + floating glowing pearl core (no dots), visible through frame; glows in dark | | |
| 6.10 | Storage Terminal 3D model | Console with screen tilted BACK, facing placer; keyboard shelf in front | | |
| 6.11 | Gateway Port / Ender Barrel textures | Copper casing + teal socket; barrel hoops teal, ender-core lid | | |
| 6.12 | Gateway Frame CT (Wave 2 rework) | Frames merge into ONE brass-outlined window; corners connect; straight runs seamless (no dark end spots); inner corners meet flush; no interior faces | | |
| 6.13 | Fueled frame conduit | Active ring: internal pipe with the ACTUAL fuel — lilac essence (same-dim) / green eye (cross-dim); pipe-smart (runs straight, elbows at corners, metal docking collar into the core) | | |
| 6.14 | Pump fuel into an UNLIT/unbound ring | Fluid visibly traces the path to the core; keeps showing while pumping, fades ~1s after stop | | |
| 6.15 | Pump into a ring whose core tank is FULL (or NO core at all) | Fluid parks in frame buffers (250 mB each) and stays visible indefinitely; creeps into adjacent frames; forwards into the core when space frees | | |
| 6.16 | Drain a buffered decorative frame with a pump | Transit fluid comes back out (lossless) | | |
| 6.17 | LIT vs FUEL split | Conduit fluid shows unlit (cosmetic); glass glow + light only when the ring is actually active | | |

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

- **Teleport Essence** is a bucket item, not a placeable fluid block.
- End / cross-dimensional **block models** may still use placeholders until art is finished.
- Repository on **OneDrive** may cause Gradle `clean` / `processResources` file-lock errors.
- Gateway travel charges fuel PER ENTITY (player, mob, vehicle, every dropped item). Bulk item
  transport through the portal plane is intentionally expensive — Gateway Ports carry items free.
- Riders are dismounted during dimension-change travel (vanilla engine rule); mount and rider both
  arrive if fuel covers both.
- A cross-dimensional partner shows no goggle liveness line while its chunks are unloaded —
  intended (travel still works and loads them on demand).
- Ender Barrels in a storage network are indexed once per PAIR, so terminal inventory counts can
  read lower than the number of placed barrels — intended.
