# Phase 7 — "Logistics & Power" (ships as CESG 1.1.0)

Big themed content drop that **deepens the systems shipped in 1.0** rather than
opening new frontiers (deliberately no worldgen / combat this cycle). Three
pillars, chosen 2026-07-22:

1. **Deepen logistics** — the Cross-Dimensional Storage Bridge (headline),
   gateway routing, a crafting terminal, Create-synergy upgrade modules.
2. **Power & economy** — a fuel/energy battery to buffer bursty gateway demand,
   plus config-exposed economics.
3. **Polish & UX** — finish the Phase 6 ponder scaffold, close the cosmetic
   TODOs, add sounds/particles, complete recipe-viewer coverage.

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
| 7C | Crafting Terminal — ✅ already shipped in 1.0.0; only JEI/EMI "+" transfer remains → 7F | | — |
| 7D | Create-synergy upgrade modules (Crushing, Washing) | | 5 |
| 7E | Gateway Battery (fuel/energy buffer) | | 1 |
| 7G | End Cultivation (native-Create farming recipes) | | anytime (orthogonal) |
| 7F | Polish & UX pass | | 6 (last) |
| 7H | Art Pass (models/textures for every new block/item) | | last — collects deferred art |

Build order rationale: **7G done**, **7E done** (both verified/committed on
`1.1-dev`). **7C's crafting terminal already shipped in 1.0.0** — only its JEI/EMI
"+" transfer remains, folded into 7F. Remaining: 7A the big lift; 7B extends 7A's
routing; 7D is orthogonal and can slot anywhere; 7F last so ponders / particles /
recipe-viewer (incl. the terminal transfer handler) cover the final feature set;
7H last collects deferred art.

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

**Recipe:** brass casing + fluid tank framing + ender pearl block (mid-tier).

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

**The only remaining gap** is recipe-viewer transfer: JEI/EMI "+" to auto-fill the
grid from network stock (and optionally R/U "show recipe/uses" on list items). The
JEI/EMI plugins currently register only the Ender Infusing category — no terminal
transfer handler. **This work is folded into 7F** (see "Recipe-viewer
completeness" there): add a JEI `IRecipeTransferHandler` + the EMI equivalent for
`StorageTerminalMenu`, respecting the L4 snapshot rules on any network pull.

---

## 7A — Cross-Dimensional Storage Bridge (build third — the tentpole)

**Status (2026-07-24, `1.1.0-dev`): server engine + assets landed; terminal UX pending.**
- ✅ `StorageBridgeBlock` / `StorageBridgeBlockEntity`: `StorageNetwork` member +
  ring-attached endpoint; ring-BFS partner resolution with the Port's 3-state
  liveness (OFFLINE/LIVE/FAULT); bidirectional passive flush + manual pull/push,
  all modelled extract-then-insert with buffer/rollback (L4 dupe/void safety);
  fuel-gated via `tryConsumeAutomationFuel`; break drops the in-transit buffer.
- ✅ Registration, BE type, `bridge` config (transfer cost / max items / snapshot
  TTL), `StorageNetwork.isMember`, brass placeholder model, lang (name + tooltip +
  goggles), regenerated datagen (blockstate/models/loot/pickaxe tag).
- ⬜ **Terminal remote section (D2)** — `remoteSnapshot()` / `manualPull` /
  `manualPush` exist but have no caller; `StorageTerminalMenu`/`Screen`/
  `TerminalContentPacket` don't yet surface the partner network.
- ⬜ **Filter + direction GUI** — `sendFilter`/`pullFilter`/blacklist flags and
  `setPush/PullEnabled` persist but have no in-game control (no menu/packet), so
  passive transfer can't be configured yet.
- ⬜ **In-game verification** — see [PHASE7-QA.md](PHASE7-QA.md) 7A steps.
- ⬜ Bespoke art deferred to 7H.

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

**Goal:** per-channel filtering and one-to-many distribution, so a single Core
can route different item/fluid classes to different bound partners. Extends the
16-channel `bindings` infra already on the Core.

**Design:**
- Per-channel **filter** (whitelist/blacklist ghost slots) stored alongside each
  channel's `GatewayPartner`, consulted by Ports and the Bridge before a flush.
  Reuse `ConfiguredFilterStack` / the existing filter-slot UI from upgrades.
- **Fan-out mode:** when multiple channels are bound, a Port/Bridge in "route"
  mode distributes each item to the first channel whose filter accepts it
  (round-robin among ties). Default stays single-active-channel for compatibility.
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

**Goal:** turn an Enhanced Shulker into a portable Create processing node.
**Ship Crushing + Washing in 1.1** (D4); others queue for 1.2.

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

Do this after every feature above exists, so it covers the final surface.

- **Ponder scenes** — finish the Phase 6 scaffold (never W-key verified). Verify
  the 5 seeded scenes render, fix schematics/lang, and **add scenes** for the new
  1.1 features: Storage Bridge, Crafting Terminal, Gateway Battery.
  - Files: `ponder/CESGPonderScenes.java`, `ponder/CESGPonderPlugin.java`,
    `assets/cesg/ponder/*.nbt`, lang `cesg.ponder.*` keys.
- **Cosmetic TODOs** (carried from Phase 6): enhanced-shulker **item icon** (only
  the placed BER is custom today), **station side faces**.
- **Sounds** — gateway activate/deactivate, bridge-active hum, station dock/eject,
  battery fill. Register a `SoundEvent` set; wire to the relevant BEs.
- **Particles** — gateway portal ambiance already exists; add bridge-active and
  battery-discharge cues.
- **Recipe-viewer completeness** — every new block/module/recipe shows correctly
  in both JEI and EMI; add category entries for the in-box processing modules if
  they need explaining.
- **Storage Terminal recipe transfer (from 7C)** — add a JEI `IRecipeTransferHandler`
  + EMI equivalent for `StorageTerminalMenu` so the "+" auto-fills the 3×3 grid from
  network stock (optionally R/U "show recipe/uses" on list items). Pulls must
  respect the L4 snapshot rules (fresh handlers, skip viewed boxes). The crafting
  terminal itself already shipped in 1.0.0 — this is the only remaining piece.
- **Config surface** — confirm all new knobs are documented and defaulted (L5).
- **Lang audit** — every new block/item/tooltip/ponder key localized (ponder
  text does NOT auto-localize; raw keys show if missing).
- **README / LISTING / CHANGELOG** — add the new feature blurbs; update the
  CurseForge listing.

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

**Outstanding art requirements (grow this list as tracks defer art):**
- **7E — Gateway Flux Battery** — replace the placeholder brass cube with a
  bespoke model + texture: brass-cased body with two fluid windows (teal Teleport
  Essence / green Liquid Eye of Ender) reading fill level; directional optional.

(Add 7A/7B/7C/7D blocks + any new items here as they are built with placeholders.)

**Done when:** every new 1.1.0 block/item has final art, item icons included, and
no placeholder models remain in the creative tab.

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

Add a **Section 5 (Phase 7)** with per-track in-game checks:
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

7E / 7A / 7B / 7D / 7G are now fully specified. Next: start 7E (Gateway Flux
Battery), or 7G first if you want a fast, self-contained win (recipes only).
