# Phase 7 QA & Verification Log

Living checklist for CESG 1.1.0 ("Logistics & Power"). Companion to
[PHASE7.md](PHASE7.md) — that doc is the *plan*, this is the *proof*. Add
verification steps here as each track is built, and check them off as they pass
in-game.

**How to use:** each track gets a section with concrete in-game steps. Mark each
`[ ]` → `[x]` when verified, and note the build (`mod_version` + date) it was
verified on. Record failures inline with a `⚠️` and a follow-up. Nothing ships in
1.1.0 until its track here is fully green.

**Process (per [[feedback-content-workflow]]):** every track that adds a
block/item carries an **Art pass** item. A feature track may be marked
functionally green on a *placeholder* model, but the art requirement is then
logged under **7H — Art Pass**, and 1.1.0 does not ship until 7H is complete. Any
new crafting/processing recipe must be signed off by the user before it is
authored; unsigned recipes are flagged here for review.

Status legend: `[ ]` untested · `[x]` verified pass · `⚠️` failed / needs fix

---

## 7G — End Cultivation (recipes)

Built: 2026-07-22 (`1.1.0-dev`). Four static datapack recipes; no Java/blocks.
Reminder: static recipes are parsed at **world load**, so `gradlew build` does NOT
validate them — only launching + loading a world does. `create:haunting` and
`create:compacting` are new recipe types for this repo, so watch those two most.

### Load & recipe-viewer

- [x] World loads with **no recipe parse errors** in the log for
  `create:haunting` / `create:crushing` / `create:compacting` / `create:filling`
- [x] JEI/EMI shows **Haunting: Sandstone → End Stone**
- [x] JEI/EMI shows **Crushing: End Stone → Sand + Ender Pearl Dust (15%)**
- [x] JEI/EMI shows **Compacting: 3× Ender Pearl Dust → Ender Pearl**
- [x] JEI/EMI shows **Filling: Stone + Liquid Ender Pearl (100 mB) → End Stone**
- [x] No duplicate/conflicting Create recipe already exists for the same inputs
  (sandstone haunting, end-stone crushing) causing nondeterministic output

### Functional — the farm loop

- [x] Encased fan over **soul fire** haunts sandstone → end stone
- [x] Crushing wheel / millstone on end stone yields sand reliably + ender pearl
  dust at roughly the 15% rate over a large sample
- [x] Mechanical press compacts 3 ender pearl dust → 1 ender pearl in a basin
- [x] Spout applies Liquid Ender Pearl to stone → end stone (accelerant path)
- [x] **Bootstrap check:** the whole chain runs from a cobble generator with
  **zero ender pearls input** (cobble → sand → sandstone → end stone → pearls)

### Balance / economy sanity

- [x] Pearl loop is net-negative through dust (crushing an end stone made from
  essence does NOT return more essence-equivalent than it cost — no dupe)
- [x] Chorus flower planted on **farmed end stone in the Overworld** grows and is
  harvestable (confirms the emergent purpur/chorus closure)
- [x] Yield feels like a "freely renewable but factory-gated" farm, not a grind —
  retune `chance` (0.15) / dust-per-pearl (3) in the JSON if off

### Notes / follow-ups

- 2026-07-22: Tried `gradlew runGameTestServer` for a headless recipe-load check —
  **does not work**: `GameTestServer` aborts with "No test functions were given!"
  before datapack load, because the cesg namespace has no `@GameTest` methods. It
  never validates recipes. Headless recipe validation needs `runServer` (a real
  dedicated server; requires accepting the Minecraft EULA in `run/`) or the client.
- 2026-07-22: Static schema review done. `filling` + `crushing` recipes match
  existing working repo files near-certainly; `haunting` + `compacting` are standard
  Create schemas (high confidence) but have no in-repo example to diff — these two
  are the ones a live load should confirm first.
- **2026-07-22: 7G fully verified in-game on `1.1.0-dev`** — all load/recipe-viewer,
  functional-loop, and balance boxes pass. Haunting + compacting confirmed live.
  Track is green. ✅

---

## 7E — Gateway Flux Battery

Built: 2026-07-23 (`1.1.0-dev`), reworked same day into a **single-fuel Create-style
multiblock**. New block + BE implementing `IMultiBlockEntityContainer.Fluid` via
Create's `ConnectivityHandler`; FluidHandler cap delegates to the controller;
`CESGConfig` `battery` section (per-block capacity); crafting recipe. Design: one
tank that locks to the first fuel piped in (Essence OR Eye); batteries assemble into
a square-base array (cap **2×2×2** / **3×3×3**) whose capacity = `blocks × perBlock`;
the controller **tops up the connected ring Core** every 5 ticks (push-only).
Art: Create fluid-tank visual stack (brass/portal CT + teal windows + fill BER).

### Load & recipe-viewer

- [x] World loads with no errors; block appears in the CESG creative tab
  _(2026-07-27, `1.1.0-dev`)_
- [x] JEI/EMI shows the crafting recipe (brass + ender pearl dust + Create fluid
  tank → 2) _(2026-07-27)_
- [x] Block places and renders as a windowed brass tank (not a solid cube); fill
  shows through windows when fueled _(2026-07-27 — level behind the glass rises
  and tracks the side gauge)_
- [x] Goggles show title + stored fuel `x/y mB`; on a multi, also `Array: WxWxH`
  _(2026-07-27. ⚠️ found + fixed: an **empty** array printed a bare "Empty" with
  no capacity, so you could not read an array's size while building it — exactly
  when it is empty. `cesg.goggles.battery.empty` now reads `Empty: 0/N mB`.
  Fix re-verified in-game 2026-07-27.)_

### Functional — single-fuel + top-up

- [x] Pumping **Teleport Essence** fills the tank; it then **rejects Liquid Eye of
  Ender** until emptied (single-fuel lock), and vice-versa; junk fluids rejected
  _(2026-07-27 — TE fills, rejects LEE while held; drained → LEE fills and the
  gauge/visual recolours; water rejected)_
- [x] Battery adjacent to a gateway ring is found (from **any** array member) and
  tops up the Core's matching tank; does not overfill past the 4000 mB Core cap
  _(2026-07-27 — stops dead at 4000; battery drop matches the Core's gain; found
  via a plain Gateway Frame as well as the Core, and via a single corner block of
  a 2×2×2 array. A battery holding the **other** fuel correctly does not top up,
  and neither tank is corrupted.)_
- [x] Rapid gateway travel that would drain the Core mid-burst stays fueled while
  the array holds reserve (the core smoothing goal) _(2026-07-27 — travel stalls
  with no battery once the Core empties; with a charged array it keeps running and
  the array visibly drains into the Core)_
- [x] Draining the array with a pump reclaims stored fuel; no reachable Core = holds
  _(2026-07-27 — drained to swap fuel types; an orphan array with no ring holds its
  fuel indefinitely, no leak or void)_

### Functional — MULTIBLOCK (the rework)

- [x] Four batteries in a **2×2** square merge into one array (goggle shows 2×2×1)
  _(2026-07-27)_
- [x] Stacking a second 2×2 layer forms **2×2×2**; a 3×3 base stacks to **3×3×3**
  _(2026-07-27)_
- [x] A lone 1×1 does **not** stack vertically (max height = base width)
  _(2026-07-27 — the two blocks stay separate BEs)_
- [x] Combined capacity = `block count × per-block` (goggle max scales with size)
  _(2026-07-27 — 1×1×1 = 8000, 2×2×1 = 32000 confirmed on the goggle readout)_
- [x] Piping fuel into **any** member block fills the shared array tank
  _(2026-07-27 — filled via a corner block)_
- [x] Breaking a member **splits** the array and redistributes fluid — **no dupe,
  no loss** across form/split cycles (the multiblock dupe-bug check)
  _(2026-07-27 — **clean, and better than the deferred note predicted.** 2×2×2
  holding 32 000: broke a top block → bottom 2×2×1 kept all 32 000, top tanks
  empty. Repeated at 33 000 → 32 000 bottom + 1 000 in one top tank = 33 000
  exactly. Re-merging preserves the level. No dupe **and** no loss.)_
- [x] Fuel already in a small array is preserved when it grows (form absorbs it)
  _(2026-07-27 — 1×1 holding 8 000 grown to 2×2 still reports 8 000)_
- [x] **One-click layer placer**: clicking the top (or bottom) face of a formed 2×2
  (or 3×3) array with batteries in hand auto-places the whole W×W layer, but only
  if the stack holds enough for the full layer; sneak-place still places one; an
  obstructed footprint places nothing extra _(2026-07-27 — 2×2, 3×3, top and
  bottom faces all pass)_
  - **Wording note for future testers:** "places nothing extra" means nothing
    beyond the single block vanilla placement already put down. `place()` runs
    `super.place` first and only then tries the layer fill, so a short stack or an
    obstructed footprint still leaves **one** normally-placed battery. That is
    intended (it mirrors Create's `FluidTankItem`), and that lone block correctly
    does **not** merge — a 2×2×1 plus one block is not a rectangular prism.

### Functional — fuel governor (port cost + reserve)

- [ ] With `gateway.portTransferCostMb = 0` (default), Gateway Ports transfer for
  free exactly as in 1.0 (governor inert)
- [x] With a port cost set and **no battery** on the ring, Ports spend that fuel
  from the Core per flush, and may drain it to empty (no gating)
  _(2026-07-27, `portTransferCostMb = 200`)_
  - Asked during QA and confirmed in code: a fuel-gated Port **buffers** the items
    it could not send, and that buffer is both **capped** and **dropped on break**.
    `SLOTS = 9` per handler with no slot-limit override, so send+receive is 18
    slots (≤1152 items worst case, not unbounded), and
    `GatewayPortBlock.onRemove` drops every non-empty slot. No hoard, no void.
- [x] With a battery on the ring, automated Port transfers **pause** once combined
  Core+battery fuel would drop below `battery.reserveFloorMb`…
  _(2026-07-27 — settles at exactly 1000 mB combined: the gate refuses once
  `(core + battery) - cost < floor`, i.e. below 1200 at cost 200)_
- [x] …while **player travel still works**, drawing into that reserve (travel is
  never gated) _(2026-07-27)_
- [x] A **dry** battery on the ring pauses automation (the safety), even though it
  holds nothing _(2026-07-27 — pauses once the **Core alone** reaches the floor,
  rather than draining to 0 as it does with no battery present)_
- [x] A battery holding the *other* fuel does not gate the current fuel's transfers
  _(2026-07-27 — same-dimension/Essence gateway ignores an Eye-filled battery)_
- [x] Goggles show `Travel reserve: X mB` on the battery when a port cost is set
  _(2026-07-27 — shown alongside the corrected `Empty: 0/N mB` capacity line)_

### Config

- [ ] `battery.capacityMbPerBlock` scales per-block capacity (goggle max reflects it)
- [ ] `battery.maxDrainMbPerTick` throttles how fast the Core is topped up
- [ ] `gateway.portTransferCostMb` sets the per-flush Port fuel cost (0 = free)
- [ ] `battery.reserveFloorMb` sets the protected travel reserve

### Art pass — DONE (Create-tank stack)

- Battery art shipped 2026-07-24: brass/portal CT sheets, teal glass windows,
  TOP/BOTTOM/SHAPE models, wrench window toggle, controller BER fill. Re-verify
  visuals in-game on a fresh client load (1×1 / 2×2×2 / 3×3×3 + wrench).
- ✅ **Recipe signed off 2026-07-27** and revised: `BDB / DTD / BDB` — 4 Brass
  Ingot, 4 **Ender Pearl Dust**, 1 Create Fluid Tank → **2** batteries. The
  pre-sign-off version (whole pearls, yield 1) made a 3×3×3 array cost 108 ender
  pearls; dust + yield-2 drops that to 54 dust (~18 pearls) and 14 tanks, and ties
  the bulk block to the renewable 7G crushing loop.

### Deferred follow-ups (accepted for 1.1 unless re-scoped)

- Breaking a lone battery loses its stored fluid (no NBT-on-item preservation).

### Notes / follow-ups

- 2026-07-23: Single-block battery built, then **reworked into the single-fuel
  Create-style multiblock** (`IMultiBlockEntityContainer.Fluid` + `ConnectivityHandler`,
  fetched from Create's source). `compileJava` + `runData` green. In-game
  verification pending — the multiblock form/split checks are the priority.
- 2026-07-26: Docs refreshed; feature code still treated as done, **all functional
  QA boxes above remain unchecked**.

## 7C — Crafting Terminal — ✅ SHIPPED IN 1.0.0

The Storage Terminal already is the crafting terminal (3×3 grid + result, batch
shift-craft with network restock, deposit-on-shift, clear-grid, searchable list).
Nothing to build. JEI/EMI "+" recipe transfer is **code-complete** under **7F**
(2026-07-25); QA it there.

## 7A — Cross-Dimensional Storage Bridge

Engine + terminal remote section + passive-transfer config GUI all built
2026-07-24 (`1.1.0-dev`); art (status-driven body + LIVE/FAULT textures) present
in the working tree 2026-07-26; crafting recipe signed off + authored 2026-07-27.
Track is feature-complete pending in-game verification. Verify in a two-dimension
setup with a bound gateway.

### Placement & structure
- [x] Bridge placed beside a Gateway Frame/Core that is adjacent to a Storage
  Network joins the **local** network (terminal count includes it) _(2026-07-27)_
- [x] Goggles show the 3-state partner status: gray OFFLINE (unbound/unloaded),
  green LIVE (partner ring has a Bridge + controller), red FAULT (bound+loaded
  but no partner Bridge / no partner controller) _(2026-07-27 — all three states
  forced and confirmed)_

### Remote view (D2)
- [x] **Fixed 2026-07-27:** the "Partner" label overflowed its tab. `TAB_W` was a
  fixed 42 px, too narrow for the word plus its 6 px liveness-dot inset. Tabs now
  size to their own label (`localTabW()` / `partnerTabW()`), so a longer word or
  any translation can no longer overflow; the search box and hover hit-testing
  follow the measured widths. _Confirmed in-game 2026-07-27._
- [x] **Grid placeholder reworked 2026-07-27** (three issues found by review, all
  confirmed fixed in-game):
  1. "Partner network is empty" was drawn whenever the *filtered* list came back
     empty, so a search matching nothing claimed the partner network was empty.
  2. The message was bare text over the slot grid and was unreadable — it now
     sits on a panel plate with the usual highlight/shadow border.
  3. The cell hover overlay is drawn after the placeholder, so it painted on top
     of the plate. Suppressed while a placeholder is up (the grid is empty by
     definition, and `hoveredEntry` already returned -1 there).
  - Both tabs now share one `placeholderKey()` with deliberate precedence:
    partner **OFFLINE/FAULT wins even mid-search** (real status, not a query
    result) → an active query that matched nothing says **"No results"** on
    *either* tab → only a genuinely empty partner says "Partner network is
    empty". A Local tab with an empty network and no query stays blank.
- [x] Terminal shows a **Local / Partner** tab strip only when a Bridge is on the
  network; a bridge-less terminal looks exactly as before _(2026-07-27)_
- [x] Partner tab's liveness dot matches the goggle status (green/grey/red)
  _(2026-07-27)_
- [x] Search box filters the active tab; switching tabs keeps the query
  _(2026-07-27)_
- [ ] Partner chunk unloaded → Partner tab greys, grid shows "Partner network
  offline" (silent, not an error); FAULT shows "No partner network found"
  — _FAULT text confirmed 2026-07-27; **the unloaded-chunk OFFLINE case belongs
  to the cross-dimension pass (3E) and is still untested**_

### Transfer via terminal
- [ ] On the Partner tab: click withdraws remote→cursor, shift-click→inventory,
  right-click→**half a stack**; carried-stack click deposits player→remote
  - ⚠️ **Changed 2026-07-27:** right-click withdrew exactly **1** item, which
    fights vanilla slot muscle memory (right-click takes half). Now takes half a
    *stack* — "half" of an entry total that can run to thousands is meaningless,
    and the cursor caps at a stack anyway. The **deposit** side deliberately stays
    at 1, because vanilla right-click *places* a single item.
- [ ] ⚠️ **Fixed 2026-07-27 — silent fuel gating.** A transfer refused by the
  battery reserve did nothing at all: no message, while the Core read healthy and
  the liveness dot stayed green. Found live at Core 1160 mB with cost 200 and
  floor 1000 — correct behaviour (`(1160 + 0) - 200 < 1000` with a battery on the
  ring, so it needs ≥1200), but indistinguishable from a broken terminal. Both
  `terminalWithdrawRemote` and `terminalDepositRemote` now take the `ServerPlayer`
  and show `cesg.network.remote.unfuelled` at the refusal site.
- [ ] Withdraw that finds nothing (or an unfuelled gateway) costs no fuel; deposit
  to an unfuelled gateway is rejected (no free transfer)
- [ ] Two Bridges on one ring (same partner) do **not** double-count the partner's
  items in the section
- [ ] Breaking the Bridge drops the in-transit buffer (no dupe, no loss)
  - **How to actually test this.** "Break it mid-transfer" is not testable — extract
    and deliver both happen inside one `flushPassive()`, so a successful transfer
    leaves the buffer empty and there is no window to race. Force the hold instead:
    make the partner side **Bridge + Controller only, with no storage boxes**. It
    still resolves LIVE (operational controller, so not FAULT) but `insert` has
    nowhere to go, so push extracts into `outBuffer` and the remainder stays there,
    self-limiting at `BUFFER_SLOTS = 9`. Then break the Bridge and check
    `dropped + local + partner` against the starting total.
  - [ ] Variant: with items held in the buffer, unbind the gateway / cut its fuel
    and confirm they hold indefinitely rather than draining, then restore and
    confirm they deliver once the partner can accept.
- [ ] Drain fuel below the battery reserve → transfer gated (idle = free)
- [x] Viewed (open-GUI) shulker boxes skipped on **both** sides — _**not reachable
  the obvious way, by design.** `EnhancedShulkerBoxBlock.useWithoutItem` refuses to
  open any box whose network `scan(...).operational()`, showing
  `cesg.network.box_locked`; boxes on a live network are terminal-only. So the L4
  hazard is prevented structurally rather than mitigated by the skip, which is a
  stronger guarantee than the checkbox assumed. `isViewed()` remains as
  defence-in-depth: `EnhancedShulkerBlockItemHandler.locked()` reports 0 slots
  while `openCount > 0`, so any writer that does reach a viewed box no-ops._
  - [ ] **Residual path still worth exercising:** open a box while its network is
    NOT operational (break the controller), then restore the controller while the
    GUI is still open, and confirm Bridge/terminal operations skip that box
    instead of clobbering the open snapshot.

### Passive auto-transfer (config GUI)
- [ ] Right-click the Bridge (empty hand) opens the config menu; push + pull rows
  each show 9 filter slots and enable + WL/BL toggles
- [ ] Click a filter slot with an item → sets a ghost copy (item not consumed);
  click again empty / shift-click clears it
- [ ] Toggles persist across close/reopen and a world reload
- [ ] Enable push + whitelist a couple items → only those flow local→partner;
  enable pull + blacklist an item → everything but it flows partner→local
- [ ] Empty whitelist moves nothing; empty blacklist moves everything
- [ ] Idle (both disabled) costs no fuel; enabled flush is fuel-gated by the
  battery reserve

### Recipe & art
- [x] Crafting recipe visible in JEI/EMI and craftable at a bench (authored after
  sign-off 2026-07-27: `ERE / DCD / EYE` — 4 End Stone Bricks, 1 Processed Shulker
  Shell, 1 Brass Casing, 2 Ender Pearl Dust, 1 Ender Eye → 1 Bridge)
  _(verified in-game 2026-07-27)_
- [x] In-game: OFFLINE / LIVE / FAULT models + glass gauge read correctly
  _(2026-07-27 — all three status models confirmed in-game)_

## 7B — Gateway Routing

Built 2026-07-24 (`1.1.0-dev`); compiles + full build clean, datagen regenerated.
Verify with one Core bound to two+ partners on different channels.

### Picker + filter editor
- [ ] Channel picker shows a **Route: ON/OFF** toggle (hidden when
  `gateway.allowFanOut = false`)
- [ ] Right-clicking a channel opens its filter editor (9 ghost slots + WL/BL);
  clicking a slot with an item sets a ghost copy (not consumed), empty/shift
  clears; the WL/BL toggle flips
- [ ] Route flag + per-channel filters persist across close/reopen and reload

### Port fan-out
- [ ] Route OFF: a Port sends everything to the active channel (1.0 behaviour)
- [ ] Route ON, two channels with disjoint whitelists: mixed items piped into one
  Port split to the correct partner rings; an item no channel accepts stays
  buffered
- [ ] An empty-blacklist channel acts as a catch-all; deterministic (same item
  always lands on the same channel, no flip-flop)
- [ ] Fluid follows the active channel in route mode

### Bridge fan-out
- [ ] Route ON + push enabled: items are pulled from the local network only when
  some channel accepts them, and delivered to that channel's partner network
- [ ] Pull still draws from the active channel only
- [ ] A channel whose partner chunk is unloaded holds its items (no dupe/loss);
  other channels keep flowing

### Back-compat
- [ ] A 1.0 world's gateways load with no filters and route OFF, behaving exactly
  as before

## 7D — Create-synergy Modules (Crushing + Washing)

Built 2026-07-24 (`1.1.0-dev`); compiles + full build clean, datagen regenerated.
Recipes signed off. Verify with a **placed** Enhanced Shulker (processing is
placed-only, and pauses while the GUI is open — check contents by breaking/peeking
or piping out, not by watching the open GUI).

### Craft & install
- [x] All 6 items appear in the CESG creative tab with names + tooltips
  _(2026-07-27 — names + dedicated icons confirmed, no placeholders)_
- [ ] Crushing Mk I crafts (millstone + iron sheets + shell); Washing Mk I crafts
  (encased fan + water bucket + iron sheets + shell), bucket returned
- [ ] Mk II/III craft as shapeless upgrades of the prior tier
- [ ] Modules install in the upgrade sidebar; only the highest tier of each applies

### Processing
- [ ] A placed box with **Crushing** turns cobblestone → gravel → sand (chains to
  terminal); crushed-ore/dust outputs appear from ores
- [ ] A placed box with **Washing** runs splashing (gravel → flint/nugget, etc.)
- [ ] Chance/multi-output rolls look right over a large sample (not always max)
- [ ] Throughput scales Mk I=1/s, Mk II=2/s, Mk III=4/s
- [ ] Crushing + Washing in one box both run; stacking with **Smelting** chains
  (e.g. crushed ore → ingot)

### Safety / no-dupe
- [ ] A **full** box holds the input (no processing, no loss) until space frees
- [ ] No item dupe across chained conversions or on chunk reload mid-process
- [ ] Overflow (rare) drops at the box rather than vanishing
- [ ] No lag with a box full of cobble (throttle holds); no infinite oscillation

### Art pass
- [ ] Six Crushing/Washing item icons look final in-game (textures present;
  distinct from magnet as of 2026-07-26 — user sign-off)

## 7F — Polish & UX

Partial (audit 2026-07-26). Terminal JEI/EMI "+" code shipped; sounds/particles
core set wired; Bridge ponder authored. Phase 6 cosmetics **WONTFIX**.

### Done (code) — still need in-game ticks where noted
- [x] `CESGSounds` + `sounds.json` + subtitle lang registered
- [x] Gateway open/close, teleport, bridge LIVE/FAULT, Port/Bridge transfer,
  Infuser process sounds wired
- [x] Portal ambiance + open/close + teleport + bridge edge + transfer particles
- [x] Storage Terminal JEI/EMI "+" transfer handlers (`TerminalRecipeTransferHandler`,
  `TerminalEmiRecipeHandler`)
- [x] Storage Bridge ponder scene + lang keys (registered)
- [x] Config knobs present with comments/defaults (`battery` / `bridge` /
  `gateway.portTransferCostMb` / `gateway.allowFanOut`)
- [x] Lang keys for new 1.1 blocks/items/GUIs/route/filter/subtitles/bridge ponder
- [x] **Goggle diagnostics gap fixed (found during 2C, 2026-07-27).** A gateway
  that is powered, bound and fully fuelled but whose ring fails
  `GatewayPortalShape.detect()` showed an entirely healthy tooltip and simply
  never opened — `refreshPortalState()` gates on the shape, but the tooltip had
  lines only for unpowered / unbound / fuel. `hasValidFrame()` existed and was
  unused. The Core goggle now names the specific broken rule via
  `GatewayPortalShape.describeFailure()` → `cesg.goggles.gateway.frame.*`
  (`no_ring` / `not_rect` / `blocked` / `size` / `too_big`), and `not_rect`
  reports the span and the found-vs-needed frame counts so the player can tell a
  gap from a stray block by arithmetic.
  - **This immediately diagnosed a live confusion:** a dead gateway reported
    `spans 4x5 with 13 frames, a clean ring needs 14`. Cause: a **Gateway Port had
    been built into the ring perimeter**. A Port is not a ring block in either
    `GatewayPortalShape.isRingBlock` or the canonical
    `GatewayFuelHandler.isRingBlock` (frames + Core only), so it reads as a hole.
    Ports attach **beside** the ring; only the Core may occupy a perimeter slot.
  - Other silent failure modes the diagnostic now surfaces: a **stray Gateway
    Frame touching the ring** in the same vertical plane (the flood fill absorbs
    it — this also bites two gateways built in a shared plane), any **non-air
    block in the interior**, and an interior outside 1–8 wide / 2–8 tall.
  - The gap/extra split matters because the fixes are opposites: any ring block
    strictly **inside** the bounding box means frames were absorbed beyond one
    perimeter (`extra`), while a shortfall with everything **on** the perimeter is
    an ordinary missing block (`gap`). Counting alone is not enough — a single
    protruding frame stretches the bounding box and reads as a shortfall.
  - [ ] **Re-verify in-game:** build two gateways with touching frames and confirm
    the goggle says "Extra Frames touching the ring", and that a genuinely missing
    frame still says "Ring has a gap".

### Ponder polish pass (done — static verification only, see below)
- [x] All 10 storyboards wrapped in Create's `CreateSceneBuilder`; every scene now
  calls `setKineticSpeed` (belts/shafts/cogwheels were rendering frozen — Ponder
  never runs the kinetic network) and the gateway scenes call `propagatePipeChange`
- [x] Belt transfers use `createItemOnBelt` / `createItemOnBeltLike` instead of
  thrown `createItemEntity` guesses; belt direction now follows the speed sign
  (`getMovementFacing()` reads only the belt axis + speed sign, never `facing`)
- [x] Terminal + Controller moved from `(2,1,0)`/`(3,1,0)` to the free `x=1`
  column. The gateway schematic is only 2 wide, and `layersFrom` builds its
  selection from `sceneBounds`, so anything at `x>1` was set in the world but
  could never be shown
- [x] Terminal + Controller have their own scene ids, titles and lang keys
- [x] Dedicated **Gateway Flux Battery** scene (2-tall merged tank beside the ring)
- [x] `layersFrom(0)` → `layersFrom(1)` in the three scenes that double-showed
  the base-plate layer
- [x] `cesg:end_storage` ponder tag added to the index with all 10 components
- [x] **Gateway schematic re-authored** (`cross_dimensional_gateway_core.nbt`). The old one was
  captured from a live world, so it shipped a fully fuelled/lit/bound/portal-open gateway: the Core
  BE held `Eye:4000` plus stale `FrameLit`/`Portal` packed-position lists and a bound overworld
  `Partner`, and the tank held 3750 mB. The storyboard only flipped the `lit` blockstate, so the
  glass eye — which reads the BE's fuel — showed full from the first frame and never filled. The
  frames also predated the `link_north/…/link_down` conduit properties, so every link defaulted to
  `none` and **no conduit rendered at all**. Rebuilt cold: y=0 checkerboard base plate, ring plane
  moved to x=1 (y=1..5) with conduit links computed from ring adjacency (`core` on the two frames
  touching the Core), `lit=false`, `fuel=none`, empty Core tanks, no portal blocks, full source tank
- [x] Gateway Core scene now drives the real fuel path: conduit `FUEL=EYE` walked around the ring
  from the pump's inlet frame, then `addEye` in steps to fill the Core (which drives its own
  `LIT`+`FUEL` via `updateCoreFuelVisual`), then frames light, then the portal opens. 4 text steps
- [x] Bridge / Controller / Terminal moved to the x=2 column at y=1..3, resting on the base plate;
  Terminal turned to `FACING=NORTH` so the console face points at the camera
- [x] Flux Battery scene: single battery on the plate beside the ring, charged for real (fluid set
  as a fraction of the *configured* capacity, so no hardcoded numbers) and then drained as the Core
  fills. **Note:** the old scene showed a 1×1×2 stack, which is not a legal array — height is capped
  to the base width, so a 1-wide battery never stacks
- [x] **New second battery scene** (`gateway_flux_battery_array.nbt`, 5×5 plate): grows 1×1×1 →
  2×2×2 → 3×3×3 from one corner, applying the SHAPE/TOP/BOTTOM lid rule and the
  controller/width/height/tank-size wiring by hand, since Ponder runs neither `ConnectivityHandler`
  nor `setWindows`. Narrates 1 / 8 / 27 blocks of fuel. Both battery scenes register against the
  `gateway_flux_battery` component, so W-key cycles them

### Ponder polish pass — round 2 (from in-game screenshots)
- [x] Gateway Core: stray Ender pearl removed; conduit `FUEL=EYE` and the frame glow (`LIT`) are now
  set in the **same** ring-walk step, so frames light as the fluid reaches them
- [x] Storage Bridge: demonstrates the real status gauge — starts `OFFLINE`, goes green `LIVE` when a
  partner answers (items then cross), then amber `FAULT` when the partner is missing, then back to
  `LIVE`. 4 text steps; Controller/Terminal stay at 3 and just prime the Bridge to `LIVE`
- [x] Flux Battery: thrown fuel buckets removed. The schematic now stacks a fluid tank + downward
  mechanical pump in the x=2 column above the battery slot, so the scene pumps fuel in for real and
  the window + gauge climb in eight steps; the top-up step then drains the battery while the Core's
  Eye tank fills, so both gauges move. The Core is primed to only 1/8 so the top-up is visible
- [x] **Connected textures on the 2×2×2 / 3×3×3 arrays fixed.** `GatewayFluxBatteryCT.connectsTo`
  resolves neighbours via `ConnectivityHandler.isConnected`, which compares each block entity's
  controller — but the section mesh is baked when `setBlocks` runs, *before* the controllers are
  wired, so every cell baked its own unconnected lid. `formBatteryArray` now ends with a no-op
  `modifyBlocks` over the prism; `ReplaceBlocksInstruction.needsRedraw()` is unconditionally true, so
  that re-queues the redraw with the controllers in place
- [x] Base plates added to all four station schematics (`shulker_loader` 4×8 → 8×8,
  `shulker_unloader` 2×7 → 7×7, `shulker_belt_loader` 3×7 → 7×7, `shulker_belt_unloader` 3×5 → 5×5).
  Each structure is shifted in x to centre it on its square plate and the storyboard constants moved
  by the same dx. All original block entity data preserved (verified by byte-exact round trip)

### Ponder polish pass — round 3: real belt transport into the machines
Ponder cannot do machine pickup at all: every transfer path in this mod and in Create sits behind
`if (level.isClientSide) return;` and `PonderLevel` is client-side. Confirmed at
`ShulkerBeltLoaderBlockEntity:89-91` and `AbstractShulkerStationBlockEntity:300-302`. That is why
Create ships `flapFunnel` / `stallBeltItem` / `changeBeltItemTo` / `removeItemsFromBelt` and scripts
every hand-off in its own scenes.

What *is* real is the travel — `createItemOnBelt` hands a genuine `TransportedItemStack` to the belt's
own `DirectBeltInputBehaviour`, and block entities in a shown section do tick
(`WorldSectionElementImpl.tickableBlockEntities`). So items ride belts at the real speed and direction
and only the hand-off is scripted.

- [x] `BELT_SPEED` raised 16 → 32. `getBeltMovementSpeed()` is `getSpeed() / 480`, so travel is now
  15 ticks per block instead of 30. `TICKS_PER_BLOCK` is derived from the speed, not hardcoded
- [x] New `beltItemConsumedAt` helper: insert on the belt → ride the real distance → `stallBeltItem`
  where it arrives → flap the funnel → `removeItemsFromBelt`. One item at a time, because
  `removeItemsFromBelt` clears every item on that belt's handler
- [x] **Shulker Loader** re-choreographed to match how the schematic is actually built (symmetric about
  the loader): empty box drops in from the barrel/chute above and docks, items ride in on the z=5-6
  belt and the funnel loads them, filled box ejects onto the z=2-3 belt and rides away. text_1/text_2
  rewritten
- [x] **Shulker Belt Loader**: items now ride the belt from z=1, stall under the hose tip at z=3 and are
  removed as the hose draws them up, instead of sliding past an unrelated animation
- [x] Unloader and Belt Unloader needed no change — they push items *out* onto belts, which
  `createItemOnBelt` already does for real
- [x] `verify.py` gained a travel-distance check: the `blocks` argument to `beltItemConsumedAt` must
  equal the real distance between its insert and arrive positions, so a mistimed stall fails the build

**Assumption to confirm in-game:** the Loader scene reads the barrel/chute above the machine as the
*empty shulker box* feed and the z=5-6 belt as the *item* feed. If the workshop was built the other way
round, swap text_1 and text_2 and the two feeds.

### Ponder tooling (`tools/ponder/`, currently gitignored — see note)
`nbtio.py` is a type-preserving NBT reader/writer (round trips all four station schematics
byte-for-byte). `gen_gateway.py` and `gen_stations.py` author/transform the schematics and are
idempotent and byte-deterministic (`mtime=0`), so re-running them yields no git diff. `verify.py`
runs the whole static check suite; `show.py` dumps a schematic.

**Note:** `.gitignore:42` ignores `tools/`, so these generators are not committed even though the
`.nbt` files they produce are. That makes the committed schematics unreproducible — worth either
un-ignoring `tools/ponder/` or moving the scripts somewhere tracked.

### Still open — ship
- [ ] **W-key verify** all 11 ponder scenes in a dev client. Static checks pass
  (every position in-bounds, every belt/funnel API target is the right block
  type, no missing lang keys) but nothing has been seen rendering yet. Confirm
  in particular the belt travel *sign* per scene and that the hose BER updates
  after `modifyBlockEntity`
- [ ] Confirm the Ponder level does not re-run `GatewayFluxBatteryBlockEntity.serverTick` →
  `updateConnectivity()`. If it does, `ConnectivityHandler` may overwrite the width/height the
  array scene sets by hand and the arrays will render as separate 1×1×1 tanks
- [ ] The station schematics still carry stale `Controller`/`Source` world positions in their belt
  block entity NBT (e.g. `Controller: [7,64,16]` in `shulker_loader`). Create normally re-resolves
  these on placement; confirm the belts render as one run and not as separate segments
- [ ] In-game: JEI **and** EMI "+" fill the terminal grid from network stock
- [x] In-game: all new 1.1 recipes/modules visible in JEI+EMI (battery, modules,
  7G, bridge) _(2026-07-27. The corner sprite on Washing Mk I's water bucket is
  JEI drawing the vanilla crafting remainder — the bucket is returned, expected.)_
- [ ] README feature blurbs updated for 1.1
- [ ] `docs/publishing/LISTING.md` updated for 1.1 (still cites `1.0.0` jar)
- [ ] `CHANGELOG.md` — move Unreleased + add Bridge / routing / battery /
  Crushing+Washing / polish into `[1.1.0]` when releasing
- [ ] `docs/dev/TESTING.md` Section 5 (Phase 7) added

### Deferred / accepted as-is (not blocking)
- ❌ Phase 6 cosmetics (enhanced-shulker item icon, station side faces) — WONTFIX
- ◯ No looping bridge-active hum; station dock/eject + battery bucket stay vanilla
- ◯ No battery-discharge / continuous LIVE-idle particles
- ◯ No CESG JEI/EMI category for in-box Crushing/Washing
- ◯ No terminal R/U "show uses"

## 7H — Art Pass (collects deferred models/textures)

The home for every placeholder deferred by a feature track. **1.1.0 does not ship
until this list is complete** (or an item is explicitly dropped by the user).
Follow the palette / PIL recolor workflow in the texture art-direction notes.

- [x] **7E — Gateway Flux Battery**: Create fluid-tank visual stack (brass/portal
  CT + teal windows + BER fill). Fill colour = stored fuel (lilac Essence /
  green Eye). _Shipped 2026-07-24._
- [X] **7A — Storage Bridge**: body / body_lit + OFFLINE/LIVE/FAULT textures +
  glass gauge present in working tree (2026-07-26) — confirm in-game / commit
- [X] **7D — Crushing + Washing Mk I/II/III icons**: six PNGs present (not magnet
  copies) — confirm in-game as final
- N/A 7B / 7C / 7G (no new block/item art surface)

Phase 6 cosmetic carryovers (enhanced-shulker item icon, station side faces) are
**out of scope / WONTFIX** for 1.1.0 — not tracked here.
