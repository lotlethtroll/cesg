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
Placeholder brass-cube texture pending art.

### Load & recipe-viewer

- [ ] World loads with no errors; block appears in the CESG creative tab
- [ ] JEI/EMI shows the crafting recipe (brass + ender pearls + Create fluid tank)
- [ ] Block places and renders (brass placeholder cube — art is a later polish)
- [ ] Goggles show title + stored fuel `x/y mB`; on a multi, also `Array: WxWxH`

### Functional — single-fuel + top-up

- [ ] Pumping **Teleport Essence** fills the tank; it then **rejects Liquid Eye of
  Ender** until emptied (single-fuel lock), and vice-versa; junk fluids rejected
- [ ] Battery adjacent to a gateway ring is found (from **any** array member) and
  tops up the Core's matching tank; does not overfill past the 4000 mB Core cap
- [ ] Rapid gateway travel that would drain the Core mid-burst stays fueled while
  the array holds reserve (the core smoothing goal)
- [ ] Draining the array with a pump reclaims stored fuel; no reachable Core = holds

### Functional — MULTIBLOCK (the rework)

- [ ] Four batteries in a **2×2** square merge into one array (goggle shows 2×2×1)
- [ ] Stacking a second 2×2 layer forms **2×2×2**; a 3×3 base stacks to **3×3×3**
- [ ] A lone 1×1 does **not** stack vertically (max height = base width)
- [ ] Combined capacity = `block count × per-block` (goggle max scales with size)
- [ ] Piping fuel into **any** member block fills the shared array tank
- [ ] Breaking a member **splits** the array and redistributes fluid — **no dupe,
  no loss** across form/split cycles (the multiblock dupe-bug check)
- [ ] Fuel already in a small array is preserved when it grows (form absorbs it)
- [ ] **One-click layer placer**: clicking the top (or bottom) face of a formed 2×2
  (or 3×3) array with batteries in hand auto-places the whole W×W layer, but only
  if the stack holds enough for the full layer; sneak-place still places one; an
  obstructed footprint places nothing extra

### Config

- [ ] `battery.capacityMbPerBlock` scales per-block capacity (goggle max reflects it)
- [ ] `battery.maxDrainMbPerTick` throttles how fast the Core is topped up

### Art pass — DEFERRED to 7H

- Battery art (placeholder brass cube → bespoke model + fluid windows) is deferred
  by the user 2026-07-23 and tracked as an explicit requirement under **7H — Art
  Pass** below. 7E is functionally green on the placeholder; final art gates 1.1.0.
- ⚠️ **Recipe not yet signed off** — brass + ender pearls + Create fluid tank was
  authored before the sign-off rule; still awaiting user review/adjust.

### Deferred follow-ups (7E not fully closed until these land)

- **Fuel governor + port cost** — `gateway.portTransferCostMb` (default 0) + a
  reserve floor so automated Port/Bridge transfers only draw while array fuel stays
  above the reserve, leaving charge for player travel. (The battery's unique value;
  see PHASE7.md 7E note.)
- Breaking a lone battery loses its stored fluid (no NBT-on-item preservation).

### Notes / follow-ups

- 2026-07-23: Single-block battery built, then **reworked into the single-fuel
  Create-style multiblock** (`IMultiBlockEntityContainer.Fluid` + `ConnectivityHandler`,
  fetched from Create's source). `compileJava` + `runData` green. In-game
  verification pending — the multiblock form/split checks are the priority.

## 7C — Crafting Terminal — ✅ SHIPPED IN 1.0.0

The Storage Terminal already is the crafting terminal (3×3 grid + result, batch
shift-craft with network restock, deposit-on-shift, clear-grid, searchable list).
Nothing to build. The only remaining piece — JEI/EMI "+" recipe transfer to
auto-fill the grid — is folded into **7F** and will be QA'd there.

## 7A — Cross-Dimensional Storage Bridge

_Not built yet. (remote section visible, pull across, partner-unload hold with no
dupe/loss, fuel-gated upkeep, both-side viewed-box skip.)_

## 7B — Gateway Routing

_Not built yet. (per-channel filters, fan-out determinism, 1.0-world load.)_

## 7D — Create-synergy Modules (Crushing + Washing)

_Not built yet. (in-box processing chains, tier throughput, no dupe.)_

## 7F — Polish & UX

_Not built yet. (ponders render, sounds/particles, JEI+EMI complete, lang audit,
**Storage Terminal JEI/EMI "+" recipe transfer** folded in from 7C.)_

## 7H — Art Pass (collects deferred models/textures)

The home for every placeholder deferred by a feature track. **1.1.0 does not ship
until this list is complete** (or an item is explicitly dropped by the user).
Follow the palette / PIL recolor workflow in the texture art-direction notes.

- [ ] **7E — Gateway Flux Battery**: replace placeholder brass cube with a bespoke
  model + texture — brass-cased body, two fluid windows (teal Teleport Essence /
  green Liquid Eye of Ender) showing fill level; directional optional. _Deferred
  2026-07-23._

_(Append 7A/7B/7C/7D blocks and any new item icons here as they are built on
placeholders.)_
