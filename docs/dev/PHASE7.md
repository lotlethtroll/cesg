# Phase 7 — "Logistics & Power" (ships as CESG 1.1.0)

Big themed content drop that **deepens the systems shipped in 1.0** rather than
opening new frontiers (deliberately no worldgen / combat this cycle). Three
pillars, chosen 2026-07-22:

1. **Deepen logistics** — the Cross-Dimensional Storage Bridge (headline),
   gateway routing, a crafting terminal, Create-synergy upgrade modules.
2. **Power & economy** — a fuel/energy battery to buffer bursty gateway demand,
   plus config-exposed economics.
3. **Polish & UX** — finish the Phase 6 ponder scaffold, sounds/particles,
   recipe-viewer coverage, and release docs. (Phase 6 cosmetic carryovers are
   WONTFIX for 1.1.0.)

Version: MINOR (`1.1.0`) — new content, no save/recipe breakage intended. Any
change that invalidates a 1.0 world or recipe must be flagged and re-scoped as
MAJOR before it lands.

## Content workflow (applies to every track)

Two standing rules for new content this phase:

- **Recipes need sign-off.** Propose any new crafting/processing recipe
  (ingredients, pattern, output, cost/progression rationale) and get the user's
  input BEFORE authoring the JSON / `CESGRecipeProvider` entry. Recipes are
  balance decisions the user owns — do not finalize them unilaterally.
- **Art is a tracked step.** Every new block/item gets an explicit
  modelling/texture pass. Placeholder models are fine to get a functional build
  under test, but a track is not "done" until its art is finished or the user has
  explicitly deferred it. Follow the palette/PIL workflow in the texture art
  direction notes. Each PHASE7-QA track section carries an **art-pass** item.

---

## Track map

| Track | Name | Headline? | Build order |
|-------|------|-----------|-------------|
| 7A | Cross-Dimensional Storage Bridge | ⭐ tentpole | 3 |
| 7B | Gateway Routing (filters + fan-out) | | 4 |
| 7C | Crafting Terminal — ✅ shipped in 1.0.0; JEI/EMI "+" → 7F (code done) | | — |
| 7D | Create-synergy upgrade modules (Crushing, Washing) | | 5 |
| 7E | Gateway Battery (fuel/energy buffer) | | 1 |
| 7G | End Cultivation (native-Create farming recipes) | | anytime (orthogonal) |
| 7F | Polish & UX pass | | 6 (last) |
| 7H | Art Pass (models/textures for every new block/item) | | last — collects deferred art |

### Status snapshot (code audit 2026-07-26, branch `1.1-dev`, `1.1.0-dev`)

| Track | Feature code | Art | In-game QA | Notes |
|-------|--------------|-----|------------|-------|
| **7G** | ✅ | N/A | ✅ green | Only fully verified track |
| **7C** | ✅ (in 1.0) | N/A | N/A | JEI/EMI "+" → 7F ✅ code |
| **7E** | ✅ | ✅ | ❌ open | Recipe signed off 2026-07-27; fluid-on-break deferred |
| **7A** | ✅ | ✅ (uncommitted) | ❌ open | Recipe signed off + authored 2026-07-27 |
| **7B** | ✅ | N/A (UI) | ❌ open | |
| **7D** | ✅ | ✅ textures present | ❌ open | Recipes signed off |
| **7F** | partial | — | ❌ open | See 7F remaining list |
| **7H** | — | near-done | — | Battery ✅; Bridge + Crushing/Washing pending user sign-off |

**Truly open for 1.1.0 ship:** in-game QA sweep (7A/B/D/E + 7F transfer), ponder
W-key verify, release docs (README / LISTING / CHANGELOG), confirm 7H Bridge +
module icons in-game. Both outstanding recipes (Storage Bridge, Flux Battery)
were signed off and authored 2026-07-27. Optional polish gaps
(looping bridge hum, battery-discharge FX, continuous LIVE ambience) are
documented under 7F — not treated as ship blockers unless re-scoped. Phase 6
cosmetics = **WONTFIX**.

---

## Design decisions

### Locked (dictated by existing patterns)
- **L1 — Bridge topology reuses the Port model.** The Storage Bridge is a
  BE-backed logistics endpoint modeled on `GatewayPortBlockEntity`: send/receive
  buffers, timed flush, cross-dimension `level.isLoaded(next)` guard, ring-scan
  to find its Core. No new networking primitive.
- **L2 — Bridge binds via the existing channel system.** It routes over the
  Core's `bindings` (channel → `GatewayPartner`), same as Ports. No parallel
  addressing scheme.
- **L3 — Network membership stays block-adjacency, on-demand BFS** (`StorageNetwork`,
  `SCAN_LIMIT = 256`). The Bridge is just another network member type; the
  remote side is reached over the gateway, not by extending the BFS across
  dimensions.
- **L4 — Enhanced-shulker snapshot invariant holds.** Any Bridge/Terminal write
  path must skip `isViewed()` boxes and build handlers fresh per operation
  (never cache them). This is the Phase 6 hardening rule; violating it corrupts
  boxes.
- **L5 — Config-first economics.** Every new cost (bridge upkeep, routing,
  battery throughput) lands as a `CESGConfig` knob with the current behaviour as
  its default, so an untouched config changes nothing.
- **L6 — New upgrade modules follow `ShulkerUpgradeItem` + per-tier Mk I/II/III**
  and are applied in `EnhancedShulkerBoxBlockEntity`'s upgrade tick, mirroring
  Smelting/Void/Magnet.

### Locked 2026-07-22 (were open; now settled)

- **D1 (7E) — Battery = Gateway Flux Battery (fuel buffer).** Stores Teleport
  Essence / Liquid Eye of Ender and releases it to cover bursty bridge/port/
  travel demand. Reuses the fluid-fuel economy, directly supports the tentpole.
  No kinetic-flywheel capacitor this cycle (revisit in a later design session if
  a rotational-power gadget is still wanted).
- **D2 (7A) — Bridge shows the partner network as a SEPARATE labeled section**
  in the terminal, visually distinct from local items. Clear provenance; degrades
  gracefully to a greyed/"offline" section when the partner is unloaded.
- **D3 (7A) — Bridge is BIDIRECTIONAL (pull and push) with per-side filters**
  (filters supplied by 7B), gated behind the fuel upkeep so it is not free
  infinite logistics. Idle bridge = no cost.
- **D4 (7D) — Ship Crushing + Washing modules in 1.1** (×3 tiers each).
  Mixing/Blasting/Haunting/Mechanical-Crafting queue for 1.2.

---

## 7E — Gateway Battery (build first)

**Goal:** a block that stores gateway fuel and releases it on demand, smoothing
the bursty consumption of bridges, ports, and travel so a network doesn't stall
mid-transfer when a pump can't keep up.

**Design (candidate a, Flux Battery):**
- Large multi-tank fluid buffer (e.g. 16 000 mB per fuel, both essence and eye)
  that Create pipes/pumps fill, and that any bound Core/Port/Bridge on the same
  ring can draw from before touching its own small tank.
- Exposes `IFluidHandler` like `GatewayFuelHandler`; registers via
  `CESGCapabilities`. Ring-attached (routes to Core via the same `findCore`
  BFS the frames use) OR standalone-on-network — confirm during D1.
- Goggle overlay shows both fuel levels + fill trend (reuse the Port's goggle
  pattern).

**New classes** (`com.cesg.gateways` or a new `com.cesg.power`):
- `GatewayBatteryBlock` (directional, EntityBlock)
- `GatewayBatteryBlockEntity` (two `FluidTank`s, capability, goggle info, client
  sync like the Port's `SYNC_INTERVAL`)

**Files to touch:**
- `init/CESGRegistration.java` — register block + item
- `init/CESGBlockEntities.java` — register BE type
- `init/CESGCapabilities.java` — attach the fluid handler
- `init/CESGCreativeTabContents.java` — tab placement
- `CESGConfig.java` — `push("battery")`: capacity, max draw/tick
- `datagen/*` — blockstate, model, item model, loot table, lang, recipe, tags
- Textures: battery block (brass casing + twin fluid windows, teal/green)

**Recipe** (signed off 2026-07-27): `BDB / DTD / BDB` — 4 Brass Ingot, 4 Ender
Pearl Dust, 1 Create Fluid Tank → **2** batteries. Yields 2 like Create's own
fluid tank because this is a bulk multiblock (a 3×3×3 array is 27 blocks), and
the dust ties it to the renewable 7G crushing loop. Revised from the pre-sign-off
version (4 brass + 4 whole ender pearls → 1), which made a full array cost 108
ender pearls.

**Risks:** ring-routing vs. standalone-network placement (D1); make sure draw
order (battery before local tank) can't create a fill/drain loop with a pump.

**Test:** fill via pump; run a port transfer that would exceed the Core's local
tank and confirm the battery covers it; verify goggle readout; verify config
capacity knob.

**PROPOSED ENHANCEMENT (user, 2026-07-22) — fuel governor / protected reserve.**
The battery's unique value beyond storage: a **two-tier fuel priority**. Player /
entity gateway *travel* is top priority (may draw fuel to empty); automated
Gateway Port / Bridge transfers are lower priority, permitted only while fuel
stays above a configurable **reserve floor** the battery protects. When the
battery runs dry / dips below the reserve, it throttles or pauses Port item-flow
but leaves enough charge for actual gateway travel — a safety so automation never
strands the player. Makes the battery a *policy* block, not just a tank; a natural
governor for the 7A Bridge / 7B routing consumers. Config: reserve floor (mB) +
enable toggle. **Prereq:** confirm how/whether Gateway Ports currently consume
fuel (they gate on `core.canTravel()`) to know exactly where the reserve check
hooks in.

---

## 7C — Crafting Terminal — ALREADY SHIPPED in 1.0.0

Superseded 2026-07-23: the Storage Terminal built in Phase 6D **is** the crafting
terminal. `StorageTerminalMenu` already has a 3×3 crafting grid + result slot with
live recipe preview; `TerminalBatchCrafting.shiftCraftAll` batch-crafts while
restocking the grid from the network (`stockToFullStacks`); shift-click deposits
into the network; a clear-grid button returns grid items to the network; and the
searchable network list auto-refreshes. Nothing to rebuild.

**JEI/EMI "+" transfer** (folded into 7F) is **code-complete** as of 2026-07-25
(`TerminalRecipeTransferHandler` + `TerminalEmiRecipeHandler` +
`TerminalFillRecipePacket`). Still needs in-game verification. R/U "show
recipe/uses" on list items was never in scope and remains omitted.

---

## 7A — Cross-Dimensional Storage Bridge (build third — the tentpole)

**Status (2026-07-26, `1.1.0-dev`): feature-complete pending in-game QA + crafting
recipe. Art landed (uncommitted) under 7H.**
- ✅ `StorageBridgeBlock` / `StorageBridgeBlockEntity`: `StorageNetwork` member +
  ring-attached endpoint; ring-BFS partner resolution with the Port's 3-state
  liveness (OFFLINE/LIVE/FAULT); bidirectional passive flush; fuel-gated via
  `tryConsumeAutomationFuel`; break drops the in-transit buffer.
- ✅ Registration, BE type, `bridge` config (transfer cost / max items / snapshot
  TTL), `StorageNetwork.isMember`, lang, datagen, status-driven models.
- ✅ **Terminal remote section (D2)** — Local / Partner tab strip, liveness dot,
  remote withdraw/deposit with fuel gates, primary-Bridge selection,
  `TerminalContentPacket` / `TerminalActionPacket` REMOTE_* modes.
- ✅ **Passive-transfer config GUI (D3)** — `StorageBridgeMenu` / `Screen` with
  push/pull ghost filters + enable + WL/BL.
- ✅ **Art (7H)** — hand-authored `storage_bridge/body` (+ `_lit`), OFFLINE/LIVE/
  FAULT side+top textures, glass gauge panes (working tree; not yet committed).
- ✅ **Crafting recipe** (signed off 2026-07-27) — `ERE / DCD / EYE`: 4 End Stone
  Bricks, 1 **Processed Shulker Shell**, 1 Brass Casing, 2 Ender Pearl Dust,
  1 Ender Eye → 1 Bridge. Gateway masonry around an ender-attuned storage core, so
  the recipe reads as both subsystems; the Processed Shell gates the Bridge behind
  the Liquid Ender Pearl economy it runs on.
- ⬜ **In-game verification** — see [PHASE7-QA.md](PHASE7-QA.md) 7A steps.

### Behavior as built (wiki reference)

**What the Bridge does.** The Cross-Dimensional Storage Bridge links two Storage
Networks through a bound gateway. Place it next to a Gateway Frame or Core that is
also touching a Storage Network; put a matching Bridge on the partner ring's
network. Each Bridge joins its **local** network (its items show on local
terminals) and mirrors the **partner** network across the gateway.

**On the terminal.** When a Bridge is on a terminal's network, the terminal grows
a **Local / Partner** tab strip (bridge-less terminals look unchanged). The
Partner tab shows the partner network's items as a separate section with a
liveness dot: **green** linked, **grey** offline (partner unbound or its chunk
unloaded — silent, not an error), **red** fault (bound + loaded but no partner
Bridge / no controller). Clicks on the Partner tab move items across the gateway:
click withdraws to cursor, shift-click to inventory, right-click one; a
carried-stack click deposits into the partner network.

**Which partner a terminal shows.** A terminal drives its Partner tab from **one
primary Bridge** (the first live one on the network), so several Bridges sharing a
partner never double-count. The reverse of the hub-and-spoke case matters: many
spoke networks can each see one shared hub, but a terminal only ever shows the
**one** partner its Bridge's active channel points at.

**Cross-dimension safety.** Every move is modelled extract-then-insert with an
in-transit buffer: nothing commits until the source extract succeeds, so an
unloaded partner or a broken Bridge can never dupe or void items (breaking a
Bridge drops its buffered items). Manual withdraw charges fuel only on a
successful pull (and bounces items back if the gateway can't pay); deposit charges
up front and is refused outright when unfuelled.

**Passive auto-transfer (config GUI).** Right-click the Bridge (empty hand) to
open its config: a **push** (local→partner) and a **pull** (partner→local) row of
nine filter slots each, with a per-direction enable and whitelist/blacklist
toggle. Same filter rules as everywhere (empty whitelist = nothing, empty
blacklist = everything). Enabled directions move filtered items automatically,
fuel-gated by any Gateway Flux Battery's reserve; idle = no cost. **In gateway
route mode (7B) the push side instead obeys the per-channel filters and the
config-GUI push filter is ignored** — see 7B.

**Goal:** link two Storage Networks through a bound gateway pair so a Terminal on
one side can see and move items to/from the other side's network. Makes gateways
permanent infrastructure, not just a travel novelty.

**Design (built on the Port model, L1/L2):**
- New block `Storage Bridge` that is BOTH a `StorageNetwork` member (so the local
  terminal/BFS includes it) AND a ring-attached gateway endpoint (finds its Core
  via ring BFS, routes over the active channel's `GatewayPartner`).
- The Bridge advertises the **local** network's aggregate handler across the
  gateway to the partner Bridge, which surfaces it to its terminal as a separate
  section (D2). Requests flow back as buffered pull/push operations (D3),
  committed only when the partner chunk is loaded (cross-dim safety) and while
  fuel upkeep is paid (drawn from the Core tank / 7E battery).
- Upkeep: a small per-operation or per-tick fuel cost (config, L5) so infinite
  free cross-dim logistics isn't automatic. Idle bridge = no cost.
- Offline partner = graceful: terminal shows the remote section greyed/"offline"
  (reuse the Port's 3-state liveness: unloaded = silent, red only for verified
  faults).

**Data flow:** local terminal → local network scan (existing) + remote section
(Bridge request → gateway → partner Bridge → partner network scan → serialized
snapshot back). Item moves are buffered operations, never a synchronous
cross-dimension inventory write.

**New classes** (`com.cesg.storage.network` + `com.cesg.gateways`):
- `StorageBridgeBlock` + `StorageBridgeBlockEntity` (send/receive request +
  transfer buffers, flush loop, ring scan, fuel draw — clone the Port's skeleton)
- A small serialization type for "remote network snapshot" (list of stacks +
  counts + liveness), sent over `CESGNetwork` or resolved server-side on the
  partner and cached with a short TTL (mirror `StorageNetwork`'s 20-tick member
  cache).
- Terminal-side: remote-section model in `StorageTerminalMenu` +
  `StorageTerminalScreen`

**Files to touch:**
- `storage/network/StorageNetwork.java` — add Bridge to `isMember`; expose a
  serializable snapshot builder
- `storage/network/StorageTerminalBlock/Menu/Screen` — render + interact with the
  remote section
- `gateways/CrossDimensionalGatewayCoreBlockEntity.java` — expose channel binding
  resolution for the Bridge (mostly already public)
- `gateways/GatewayFuelHandler.java` — Bridge fuel draw (reuse ring routing)
- `init/CESGRegistration.java`, `CESGBlockEntities.java`, `CESGCapabilities.java`
- `network/CESGNetwork.java` + new packet(s) for remote-snapshot sync
- `CESGConfig.java` — `push("bridge")`: upkeep cost, max stacks/tick, snapshot TTL
- `datagen/*`, textures (bridge block: brass casing + storage-teal conduit +
  gateway collar, echoing the Port)

**Risks (highest of the phase):**
- **Cross-dim write safety (L4):** all remote item moves MUST buffer + retry and
  only commit when `partnerLevel.isLoaded(pos)`; never hold a handler across the
  gateway. Force-loading via getBlockEntity synchronously loads chunks — scan per
  step and guard.
- **Snapshot staleness vs. spam:** cache the remote snapshot (short TTL) and
  push deltas on transfer, don't re-serialize a 256-member network every tick.
- **Dupe/void hazards:** the classic remote-inventory bug is committing a pull
  before confirming the remote extract succeeded. Model every move as
  extract-then-insert with rollback on partner-unloaded, exactly like the Port's
  send buffer holding when the partner is down.
- **GUI provenance (D2):** keep remote items visually distinct so players don't
  think their local base has items it doesn't.

**Test:** two networks in two dimensions, bridged; open local terminal, see
remote section; pull a stack across; unload the partner chunk mid-transfer and
confirm the buffer holds (no dupe, no loss); drain fuel and confirm upkeep gates
transfer; verify viewed boxes skipped on both sides.

---

## 7B — Gateway Routing: filters + fan-out (build fourth)

**Status (2026-07-26, `1.1.0-dev`): built, pending in-game verification.** No new
block/item art (UI + Core NBT only).
- ✅ Core: per-channel `ChannelFilter` map (9 ghost slots + whitelist/blacklist)
  + `routeMode` flag + `routeChannel(stack)` helper (first bound channel whose
  filter accepts it — deterministic, no flip-flop). NBT-safe: 1.0 worlds load
  with no filters + route off. Config `gateway.allowFanOut` gates the whole thing.
- ✅ Gateway Port: `flushRouted` fans each send item to its routed channel's
  partner ring (partners resolved once/flush + cached); fluid follows the active
  channel. Default path (`flushActive`) unchanged.
- ✅ Storage Bridge: route-mode passive push extracts whatever *some* channel
  accepts (channel filters are the only gate in route mode — the per-direction
  send filter is bypassed) and fans it out via `resolvePartner` per channel. Pull
  stays on the active channel (fan-out is a send concern).
- ✅ UI: channel picker grows a **Route: ON/OFF** toggle and **right-click a
  channel** opens its filter editor (`GatewayFilterMenu`/`Screen`, ghost slots +
  WL/BL toggle). New `OpenGatewayFilterPacket`; `SetGatewayChannelPacket` carries
  `routeMode`.
- ⬜ In-game verification — see [PHASE7-QA.md](PHASE7-QA.md) 7B steps.

### Behavior as built (wiki reference)

Authoritative description of what shipped — use this to write the wiki page.

**What routing does.** A Gateway Core is bound to up to 16 destinations
("channels"). Normally a Gateway Port or Storage Bridge only talks to the
**active** channel. Turn on **route mode** and it instead fans each item out to
whichever channel's **filter** accepts it — one Core can sort iron to base A,
gold to base B, everything else to base C, all at once.

**Enabling it.** Right-click the Core to open the destination picker. It shows a
**Route: ON/OFF** button (hidden if the server sets `gateway.allowFanOut = false`).
Route mode is a property of the Core and applies to every Port and Bridge on that
ring.

**Per-channel filters.** In the picker, **right-click a channel** to open its
filter editor: nine ghost slots plus a **Whitelist/Blacklist** toggle. Click a
slot with an item to add it as a filter (the item is a copy — nothing is
consumed); click a filled slot empty-handed to clear it. Filter rules match the
rest of the mod:
- **Whitelist** — only the listed items may route here. An **empty whitelist
  accepts nothing** (an unconfigured channel therefore receives nothing).
- **Blacklist** — everything routes here *except* the listed items. An **empty
  blacklist accepts everything**, making that channel a **catch-all**.

**How an item picks a channel.** For each item, the Core walks channels in order
(1→16) and sends it to the **first bound channel whose filter accepts it**. This
is deterministic — a given item type always lands on the same channel, so items
never bounce between destinations. An item that **no** channel accepts is not
moved (it stays in the Port's send buffer / the local storage network).

**Gateway Port in route mode.** Items funneled into the Port are sorted to the
matching channel's partner ring and delivered to a Port there. **Fluid is not
item-filterable, so it always follows the active channel.** With route off, the
Port behaves exactly as in 1.0 (everything to the active channel).

**Storage Bridge in route mode.** The passive **push** pulls from the local
network only the items some channel accepts, and delivers each to that channel's
partner network. In route mode the channel filters are the **only** gate — the
Bridge's own per-direction *send* filter is ignored (so you configure routing in
one place, on the channels). The passive **pull** and the terminal's **Partner
tab still use the active channel** — fan-out is a distribution/send concern only.

**What the active channel is still used for** (unchanged by route mode): player
**travel**, **fluid** transfer, **manual/terminal** item moves, and the Bridge's
**pull** direction. Route mode only changes where *pushed/sent items* go.

**Fuel & safety.** Routed transfers pay the same per-flush fuel as normal (Port:
`gateway.portTransferCostMb`, Bridge: `bridge.transferCostMb`; default 0) and are
gated by a Gateway Flux Battery's reserve floor the same way. A channel whose
partner chunk is unloaded simply holds its share (buffered, retried) while other
channels keep flowing — no dupe, no loss.

**Compatibility.** Route mode defaults **off** and channels start with **no
filters**, so 1.0 worlds and existing gateways behave exactly as before until a
player opts in. `gateway.allowFanOut = false` removes the feature server-wide.

**Goal:** per-channel filtering and one-to-many distribution, so a single Core
can route different item/fluid classes to different bound partners. Extends the
16-channel `bindings` infra already on the Core.

**Design:**
- Per-channel **filter** (whitelist/blacklist ghost slots) stored alongside each
  channel's `GatewayPartner`, consulted by Ports and the Bridge before a flush.
  Reuse `ConfiguredFilterStack` / the existing filter-slot UI from upgrades.
- **Fan-out mode:** when multiple channels are bound, a Port/Bridge in "route"
  mode distributes each item to the first bound channel whose filter accepts it.
  **As built:** deterministic first-match by channel index (no round-robin — that
  was considered and dropped to guarantee no item flip-flop). Default stays
  single-active-channel for compatibility.
- Destination-picker UI (already exists for channel selection) grows a
  filter-edit affordance per channel.

**New / changed classes:**
- `gateways/teleport/GatewayPartner.java` — either add an optional filter field
  or store filters in a parallel channel→filter map on the Core BE (prefer the
  latter to keep the `GatewayPartner` record small; watch NBT compat, L-note)
- `gateways/CrossDimensionalGatewayCoreBlockEntity.java` — channel→filter storage,
  routing decision helper
- `gateways/GatewayPortBlockEntity.java` + `StorageBridgeBlockEntity` — consult
  filter before flush; route mode
- `network/SetGatewayChannelPacket.java` — extend to carry filter edits (or new
  `SetGatewayFilterPacket`)
- `client/*` picker screen — filter slots per channel

**Files to touch:** the picker screen, datagen lang for new tooltips,
`CESGConfig` (allow-fan-out toggle for packs).

**Risks:** NBT back-compat — adding fields to `GatewayPartner.save/load` must
default cleanly for 1.0 worlds (parallel map avoids touching the record's
format). Routing must be deterministic to avoid item flip-flop between channels.

**Test:** bind two channels with disjoint filters; insert mixed items into a
Port; confirm each routes to the correct partner; confirm 1.0-world gateways load
with empty filters and behave as before.

---

## 7D — Create-synergy upgrade modules (orthogonal)

**Status (2026-07-26, `1.1.0-dev`): built, pending in-game verification.** Recipes
signed off by the user (chain-to-terminal, crushing+milling scope, recipe designs).
- ✅ Two tiered modules (Mk I/II/III): `CrushingUpgradeItem` (Create
  crushing + milling), `WashingUpgradeItem` (Create splashing). 6 items,
  registered like the Magnet tiers; auto-join the creative tab and inherit the
  global `ItemDescription` tooltip modifier.
- ✅ `ShulkerProcessingUpgrades`: per-op recipe lookup via
  `AllRecipeTypes.{CRUSHING,MILLING,SPLASHING}.getType()` + `SingleRecipeInput`,
  rolls each `ProcessingOutput` (chance/multi-output), and commits a conversion
  only after a **scratch-mirror simulate** confirms the outputs fit (full box
  holds; a rare commit overflow drops at the box rather than vanishing). Chains to
  a terminal form over successive passes, like Smelting.
- ✅ Hooked into `EnhancedShulkerBoxBlockEntity.serverTick` (PLACED boxes only),
  throttled to 1 pass/`PROCESS_INTERVAL` (20t); ops/pass = **1 / 2 / 4** by tier
  (`operationsForTier`). Skipped while the box is viewed (like Magnet). Only the
  highest installed tier of each module applies.
- ✅ Crafting recipes (approved): Mk I shaped around the themed Create machine
  (millstone / encased fan) + iron sheet + shulker shell; Mk II/III shapeless
  upgrades (+ electron tube / precision mechanism + brass + shell). Lang + datagen.
- ✅ **Art (7H)** — dedicated `crushing_upgrade_t{1,2,3}.png` /
  `washing_upgrade_t{1,2,3}.png` icons present (distinct from magnet; working
  tree). Pending in-game / user sign-off that they read as final.
- ⬜ In-game verification — see [PHASE7-QA.md](PHASE7-QA.md) 7D steps.

### Behavior as built (wiki reference)

**Crushing / Washing modules** are tiered Enhanced-Shulker sidebar modules that
**process the box's own contents in place**, like the Smelting module — but using
Create's multi-output, chance-based recipes.
- **Crushing Upgrade** runs Create **crushing + milling** recipes (ores → crushed
  ore, cobblestone → gravel → sand). **Washing Upgrade** runs **splashing** (fan /
  bulk washing) recipes (gravel → flint/iron nugget, sand → clay, crushed ore →
  extra nuggets).
- The box must be **placed** (a pocket box doesn't run a crusher). Every interval
  it performs a few conversions — **Mk I = 1/sec, Mk II = 2/sec, Mk III = 4/sec**;
  only the highest installed tier of a given module counts.
- Outputs go **back into the box** and are re-processed on later passes, so a
  Crushing box takes cobblestone all the way to sand (**chain to terminal**). A
  box with both a Crushing and a Washing module runs both; modules stack with
  Smelting (e.g. crushed ore → smelted to ingot).
- It never loses items: if the outputs don't fit, the box simply **holds** the
  input until there's room. Processing pauses while the box's GUI is open.

**Design (mirrors Smelting module):**
- On the upgrade tick, scan box contents; for each stack with a matching Create
  **crushing** (or **splashing/washing**) recipe, convert at a throttled rate,
  writing results back into the box (chaining allowed, like Smelting's
  cobblestone→stone→smooth-stone convergence).
- Recipe lookup against Create's `RecipeType`s (crushing wheel recipe type;
  bulk-splashing / fan-washing recipe type). Respect stack limits and filter
  modules already present.
- Tier scales throughput (Mk I/II/III), matching the other modules.

**New classes** (`com.cesg.upgrades`):
- `CrushingUpgradeItem`, `WashingUpgradeItem` (extend `ShulkerUpgradeItem`)
- Processing logic in `EnhancedShulkerBoxBlockEntity`'s upgrade handling (new
  branch alongside smelting), or small helper classes per module

**Files to touch:**
- `upgrades/ShulkerUpgradeItems.java` / `ShulkerStorageUpgrades.java` /
  `EnhancedShulkerUpgrades.java` — register the two new module families (×3 tiers)
- `upgrades/EnhancedShulkerBoxBlockEntity.java` — invoke the new processors
- `init/CESGRegistration.java` — register items (6 new items: 2 modules × 3 tiers)
- `datagen/*` — item models, textures, recipes (Mk I/II/III crafting), lang,
  tooltips (Create `ItemDescription` pattern)
- `compat/jei|emi` — ensure the modules show their processed output relationship
  if applicable

**Risks:** Create recipe-type access across versions (pin to Create 6.0.10 API);
throttle so a box full of cobble doesn't lag; make sure washing's multi-output +
chance outputs are handled (roll per operation, not per tick-batch).

**Test:** cobblestone→gravel→sand via crushing in-box; wash gravel→flint /
soul-sand outputs; confirm tier throughput scales; confirm filter/void modules
interact sanely; confirm no dupe on chained conversions.

---

## 7F — Polish & UX pass (build last)

Partial as of 2026-07-26. Feature tracks exist; this is the finishing pass.

### Done
- ✅ **Storage Terminal JEI/EMI "+" transfer** (from 7C) — 2026-07-25.
  `TerminalRecipeTransferHandler` + `TerminalEmiRecipeHandler` +
  `TerminalFillRecipePacket` → `StorageTerminalMenu.fillFromRecipe` (L4-safe).
  Pending in-game verify. R/U "show uses" not in scope.
- ✅ **Sounds (core set)** — `CESGSounds` + `sounds.json` + subtitle lang; wired
  `PORTAL_OPEN` / `PORTAL_CLOSE`, `TELEPORT`, `LINK_LIVE` / `LINK_FAULT`,
  `TRANSFER` (Port + Bridge, 20t throttle), `MACHINE_PROCESS`.
- ✅ **Particles (core set)** — portal ambiance; open/close; teleport;
  bridge LIVE/FAULT edges; Port/Bridge transfer bursts.
- ✅ **Storage Bridge ponder scene** authored + lang keys; registered on
  `storage_bridge` (and currently also reused for controller / terminal W-key).
- ✅ **Config knobs** exist with comments/defaults in `CESGConfig`
  (`gateway.portTransferCostMb`, `gateway.allowFanOut`, `battery.*`, `bridge.*`).
- ✅ **Lang** for new blocks/items/goggles/GUI/route/filter/sound subtitles/
  bridge ponder keys present in `CESGLangProvider` (regenerate `en_us` after
  commit).

### Still open (ship)
- ⬜ **Ponders — W-key verify** Phase 6 scenes (loader / unloader / belt ×2 /
  gateway core / ender infuser) + the new Bridge scene.
- ⬜ **Ponders — dedicated 1.1 scenes** for **Gateway Flux Battery** and
  **Crafting / Storage Terminal** (today controller + terminal reuse the Bridge
  scene; Battery has no storyboard).
- ⬜ **Recipe-viewer in-game sweep** — confirm battery / bridge (once recipe
  exists) / crushing / washing / 7G recipes show in JEI+EMI; confirm terminal "+".
- ⬜ **README / LISTING / CHANGELOG** — still 1.0-era; Unreleased only has 7G.
  Need 1.1 blurbs (Bridge, routing, battery, modules, polish).
- ⬜ **`docs/dev/TESTING.md` Section 5** — Phase 7 in-game checklist not added yet.

### Deferred / accepted as-is (not ship blockers)
- ❌ **Phase 6 cosmetics — WONTFIX** (2026-07-26): enhanced-shulker item icon,
  station side faces.
- ◯ No looping **bridge-active hum**; no **battery pipe-fill / Core top-up** sound
  (bucket fill still vanilla `BUCKET_*`; station dock/eject still vanilla).
- ◯ No **battery-discharge** particles on Core top-up; no continuous LIVE-idle
  bridge ambience (edge + transfer FX only).
- ◯ No dedicated JEI/EMI **category** explaining in-box Crushing/Washing (vanilla
  Create recipe lookup is enough unless players need a CESG explainer).
- ◯ Terminal R/U "show recipe/uses" omitted.

---

## 7G — End Cultivation (native-Create farming recipes)

**Goal:** make End-specific materials renewable through the player's *own* Create
factory, not new CESG blocks. CESG registers recipes into Create's native
processing types (crushing / haunting / compacting / filling); the "farm" is
whatever fan/wheel/press setup the player builds. Fits the mod's "Create: End"
identity perfectly. **Freely renewable by design** (build cost = the factory);
gate via chance values, not hard locks.

**Signature framing:** Haunting (encased fan through *soul fire*) is reflavored
as the **void transformation** — the verb that turns ordinary matter into End
matter. It is the one "magic" step; crushing/compacting are mundane prep.

**Recipe graph (all root in infinite cobblestone):**

```text
Cobble ─Crush→ Gravel ─Crush→ Sand ─(4× craft)→ Sandstone ─HAUNTING★→ END STONE
END STONE ─Crushing★→ Sand + Ender Pearl Dust (~15% chance)
3× Ender Pearl Dust ─Compacting★→ Ender Pearl
(optional) Stone + Liquid Ender Pearl ─Spout/Filling★→ END STONE  [premium accelerant]
```

**Recipes to author** (static JSON in `src/main/resources/data/cesg/recipe/`,
following the existing `*_from_crushing.json` / `*_from_mixing.json` pattern —
these are hand-written, NOT datagen'd; `CESGRecipeProvider` only emits vanilla
crafting):

| File | `type` | In → Out | Numbers |
|------|--------|----------|---------|
| `end_stone_from_haunting.json` | `create:haunting` | Sandstone → End Stone | 1:1 (keystone) |
| `ender_pearl_dust_from_end_stone_crushing.json` | `create:crushing` | End Stone → Sand + Ender Pearl Dust | dust ~15% chance |
| `ender_pearl_from_compacting.json` | `create:compacting` | 3× Ender Pearl Dust → Ender Pearl | 3:1 |
| `end_stone_from_filling.json` *(optional)* | `create:filling` | Stone + Liquid Ender Pearl (~50 mB) → End Stone | premium accelerant |

**Decisions locked 2026-07-22:**
- **Crushing only** for pearl-dust extraction — no parallel washing recipe (a
  same-input/higher-% washing recipe is strictly dominant = dead crushing
  content). If washing is ever wanted, it must be a *refinement second step*
  (crush → intermediate → wash), not an alternative.
- **No CESG purpur recipe.** Lean on vanilla: chorus fruit → (blast/smelt) →
  popped chorus → (craft) → purpur. Chorus is obtained by **regular harvesting**.
- Renewable end stone makes this self-closing: chorus flowers plant on end stone
  and grow in **any dimension**, so players build an **Overworld chorus farm** on
  farmed end stone → chorus fruit → purpur *and* Teleport Essence fuel. Emergent,
  zero extra content. One initial chorus flower (one End trip) seeds it.
- Byproduct = plain `minecraft:sand` (no new "End Stone Dust" item) unless a
  flavored byproduct is wanted later — cosmetic only.

**New content surface:** ~3–4 JSON recipes, **zero new blocks/BEs**, reuses the
existing `cesg:ender_pearl_dust` item. The leanest track in the phase.

**Files to touch:**
- `src/main/resources/data/cesg/recipe/*.json` — the recipes above
- `CESGRecipeProvider.java` — add reference comments (mirrors how the existing
  Create recipes are documented there)
- `datagen/CESGAdvancementProvider.java` — optional "first farmed End Stone"
  advancement
- README / wiki — document the purpur/chorus player strategy and the farm loops

**Non-circularity rule (critical):** the *base* end-stone path (haunting) MUST
stay essence-free. The optional filling recipe (#5, spends Liquid Ender Pearl) is
safe only because haunting exists beside it — otherwise the pearl farm
(crush → compact) becomes a pearl *sink* and the whole loop inverts.

**Balance knobs:** the crushing chance (~15%) and dust-per-pearl (3×) are the two
master dials. NOTE: Create bakes result chance into the recipe JSON — a *runtime*
`CESGConfig` knob would require recipe manipulation at load and is not native;
pack authors retune via **datapack override** instead. Flag a config knob as an
optional stretch, not a given.

**Risks:** verify Create 6.0.10 exposes `create:haunting` / `create:compacting`
JSON schemas as expected (the mod already uses `create:crushing`, `create:mixing`,
`create:filling`, so the authoring pattern is proven). Keep pearl economics
net-negative through the dust loop (existing `ender_pearl → 1 dust` crush means
3 dust > 1 pearl, so no duplication).

**Test:** haunt sandstone → end stone; crush end stone repeatedly and confirm
~15% dust; compact 3 dust → 1 pearl; confirm the cobblestone-only bootstrap works
with zero pearl input; plant chorus on farmed end stone in the Overworld and
confirm it grows/harvests.

---

## 7H — Art Pass (collects every deferred model/texture)

**Goal:** the single home for all modelling/texture work in Phase 7. Feature
tracks may ship a functional **placeholder** model to get under test, but each
such placeholder is logged here as an explicit requirement, and **1.1.0 does not
ship until 7H is complete** (or the user explicitly defers a specific item).

Follow the palette / PIL recolor workflow in the texture art-direction notes;
match Create's brass/andesite grain where a block reads as a machine.

**Art checklist (audit 2026-07-26):**
- ✅ **7E — Gateway Flux Battery** — Create fluid-tank visual stack (brass /
  portal-accent CT + teal windows + controller BER fill), terminal/bus top,
  fill-reactive side gauges, conduit sockets on ring-touching faces. _Shipped
  2026-07-24._
- ✅ **7A — Storage Bridge** — hand-authored `body` / `body_lit`, OFFLINE/LIVE/
  FAULT textures + glass gauge (working tree). Pending commit + in-game sign-off.
- ✅ **7D — Crushing / Washing icons** — six dedicated item PNGs present (not
  magnet copies). Pending in-game / user sign-off as final.
- N/A **7B** — UI-only (no new block/item art).
- N/A **7C / 7G** — no new art surface.

**Done when:** user signs off Bridge + module icons in-game (or explicitly
defers), and no placeholder cubes remain in the creative tab for 1.1 content.

---

## Cross-cutting risks & invariants (do not violate)

- **Enhanced-shulker snapshot handlers** (L4): fresh handler per op, skip
  `isViewed()` boxes, never cache handlers. `StorageNetwork` caches only member
  *positions* (20-tick TTL).
- **Cross-dimension chunk loading:** `ServerLevel.getBlockState/getBlockEntity`
  force-load synchronously; scan per step and guard `level.isLoaded(next)`.
  Buffer + retry all cross-dim moves.
- **NBT back-compat:** additions to persisted records/BEs must default cleanly so
  1.0 worlds load unchanged (this keeps 1.1 a MINOR release).
- **Config defaults = current behaviour** so untouched servers are unaffected.
- **Ring membership single source of truth:** `GatewayFuelHandler.isRingBlock`.
- **Game-bus vs mod-bus:** brewing/registration events differ (Phase 6 note);
  sounds register on the mod bus.
- **Build gotcha:** repo is under OneDrive — gradle intermittently fails with
  "Failed to clean up stale outputs"/"Unable to delete build/classes". Kill
  daemons + `--rerun-tasks`, or retry runData 2–3×. Build output is redirected to
  `%LOCALAPPDATA%\gradle-builds\...\libs`.

---

## Testing checklist (extend docs/dev/TESTING.md)

Add a **Section 5 (Phase 7)** to `docs/dev/TESTING.md` (not present yet) with
per-track in-game checks:
- 5E Battery: fill/draw, burst-smoothing, goggle, config.
- 5C Crafting Terminal: craft/shift-craft/JEI-fill, remainder return, viewed-box
  skip.
- 5A Storage Bridge: remote section visible, pull across, partner-unload hold (no
  dupe/loss), fuel-gated upkeep, both-side viewed-box skip.
- 5B Routing: per-channel filters, fan-out determinism, 1.0-world load.
- 5D Modules: crushing/washing chains, tier throughput, module interaction, no
  dupe.
- 5F Polish: all ponders render, sounds/particles fire, JEI+EMI complete, lang
  complete.

---

## Release checklist (per release-1-0-0 workflow)

1. All Section 5 checks pass in-game.
2. `CHANGELOG.md` — move items from `[Unreleased]` to `[1.1.0]`; MINOR bump.
3. `gradle.properties` `mod_version` → `1.1.0`.
4. `gradlew build` (green; datagen regenerated).
5. Update README + `docs/publishing/LISTING.md`.
6. Commit, tag `v1.1.0`, push (repo + tag).
7. Upload `cesg-1.21.1-1.1.0.jar` to CurseForge with the listing update.

---

## Decision log

All four gating decisions locked 2026-07-22 (see "Locked" under Design decisions):

- **D1** — Battery = Gateway Flux Battery (fuel buffer). No kinetic flywheel this cycle.
- **D2** — Bridge remote view = separate labeled section.
- **D3** — Bridge direction = bidirectional pull/push + per-side filters, fuel-gated.
- **D4** — 1.1 modules = Crushing + Washing (×3 tiers); rest → 1.2.
- **7G (End Cultivation)** — native-Create farming via recipes only, no new blocks.
  End stone via Haunting (essence-free base path); ender pearls via Crushing end
  stone → dust (~15%) → Compacting 3 dust → pearl (crushing only, no washing);
  purpur/chorus lean on vanilla (regular harvesting on now-renewable end stone);
  optional Liquid-Ender-Pearl Spout as a premium end-stone accelerant.

**Recipe sign-offs 2026-07-27** (content-workflow rule — both were the last open
balance decisions):
- **Storage Bridge** — `ERE / DCD / EYE`: 4 End Stone Bricks, 1 Processed Shulker
  Shell, 1 Brass Casing, 2 Ender Pearl Dust, 1 Ender Eye → 1. Chosen so the recipe
  reads as gateway masonry + ender-attuned storage core, and so the Bridge is
  gated behind the same Liquid Ender Pearl economy it consumes at runtime.
- **Gateway Flux Battery** — `BDB / DTD / BDB`: 4 Brass Ingot, 4 Ender Pearl Dust,
  1 Create Fluid Tank → **2**. Whole pearls → dust and yield 1 → 2, because the
  battery is a bulk multiblock: the old recipe put a 3×3×3 array at 108 ender
  pearls. Dust also routes the block through the renewable 7G crushing loop.

7E / 7A / 7B / 7D / 7G feature code is built and every recipe is signed off.
Remaining path to 1.1.0: in-game QA sweep, finish 7F ponders/docs, confirm 7H art,
then release checklist.
