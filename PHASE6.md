# Phase 6 — Plan & Implementation Notes

> Status: **code-complete (2026-07-01)** — all six tracks implemented; in-game verification pending
> (see TESTING.md §4A/§4B/§4C and the EMI/JEI rows in §5). Phase 4 (Gateways) was the last shipped
> roadmap phase; the gateway hardening + UX work before Phase 6 is treated as **Phase 5**.
>
> **Progress (2026-07-01):** everything below is built:
> - **6A DONE** — 16-channel multi-binding (`bindings` map on the Core, crystal carries its imprint
>   channel, legacy saves migrate to channel 1), destination-picker screen (empty-hand use;
>   `GatewayChannelScreen` + `SetGatewayChannelPacket`), partner-liveness goggle line (polled in
>   `lazyTick` without force-loading chunks), and the **Gateway Port** block
>   (`gateways/GatewayPort{Block,BlockEntity}`): BE-backed item+fluid caps with separate SEND/RECEIVE
>   buffers (no ping-pong loops), flushed every 10 ticks through a powered+bound+fueled gateway to
>   ports on the partner ring; buffers + retries while the partner chunk is unloaded. Transported
>   fluid is fully separate from fuel.
> - **6B DONE** — Ender Infuser (visual + recipe-driven `cesg:ender_infusing`, multi-catalyst).
> - **6C DONE (integration)** — EMI **and JEI** plugins for `ender_infusing` (JEI 19.27.0.344,
>   blamejared maven, API-only dep); **advancements** (`CESGAdvancementProvider`: root, first station,
>   enhanced shulker, tier upgrade, gateway built, gateway travel — travel is impossible-trigger,
>   awarded in `TeleportResolver.teleportBound`); **server config** (`CESGConfig`: travel cost,
>   teleport cooldown, cage harvest cooldown + fire interval). Ponder scenes were done earlier.
> - **6D DONE** — storage network: `storage/network/StorageNetwork` (BFS block-adjacency cluster, cap
>   256, on-demand aggregation), `StorageNetworkControllerBlock` (anchor + status message),
>   `StorageTerminalBlock/Menu` + `client/StorageTerminalScreen` (searchable 9×6 virtual grid,
>   click-withdraw / right-click-one, shift-click deposit, live resync via `TerminalContentPacket`).
>   Placed enhanced shulkers are indexed by wrapping their BE stack (`ShulkerInventoryAccess.wrap`).
> - **6E DONE** — decorative families + custom smooth/polished textures.
> - **6F DONE** — Teleport / Teleport Resistance potions; resistance blocks gateway travel.
>
> **Remaining (6C-polish, cosmetic only):** enhanced-shulker item icon, station side faces,
> teleport/station sounds & particles. New-block textures (port/controller/terminal) are PIL-generated
> per the art direction and could get a hand-polish pass later.

## Tracks at a glance

| Track | Theme | Size | Depends on |
|-------|-------|------|------------|
| **6A** | Gateway logistics / network | L | stable gateway (done) |
| **6B** | End automation depth | M | Phase 3 farming, fluids |
| **6C** | Create-native integration & polish | M (split) | features stable |
| **6D** | Storage network layer | XL | 6A net patterns reusable |
| **6E** | Decorative variants (end stone + purpur) | S–M | none |
| **6F** | Teleport / Teleport Resistance potions | M | `ender_pearl_dust` (exists) |

**Recommended sequencing:** 6C-polish (quick wins) → 6B → 6A → 6D → 6C-ponder/viewer/advancements
(authored last, against finished behaviour). Rationale: cheap polish first; automation is mostly
recipes/caps; logistics establishes cross-dimension transfer + UI patterns that 6D reuses; tutorials and
recipe-viewer support document the *final* behaviour so they aren't rewritten.

---

## 6A — Gateway Logistics / Network

**Goal:** move items & fluids through bound gateways, with multi-destination routing.

**Current state**
- `GatewayPartner` = single bound `(dimension, pos)` — a **1:1** link.
- `CrossDimensionalGatewayCoreBlockEntity` holds two fluid tanks; `tick()` teleports any entity in the
  portal interior (`TeleportResolver.teleportThroughPortal`). Dropped items already pass through
  physically, but that isn't pipe-driven logistics.

**Key decision (resolve first):** physical vs. capability bridge.
- *Physical:* funnels drop items into the portal plane → they teleport. Already works, zero new code,
  but clunky and rate-limited by entity ticking.
- *Capability bridge (recommended):* a port exposes `IItemHandler`/`IFluidHandler`; inserts are forwarded
  to a buffer at the **partner** core, pulled out there by Create automation. Clean, fast, pipe-native.

**Milestones**
1. **Item transfer endpoint.** Give the Core (or a dedicated `GatewayPort` block) an `IItemHandler`
   capability (`CESGCapabilities`) whose `insert` routes to the partner core's output buffer. Mirror the
   existing fluid `GatewayFuelHandler` pattern (BE-backed cap — BE-less caps are *not* picked up by Create
   pipes, see Phase 5 frame-routing fix).
2. **Fluid transfer endpoint.** Same for fluids, distinct from the *fuel* tanks (don't let transported
   fluid get eaten as fuel).
3. **Multi-destination.** Extend `GatewayPartner` → a keyed set of bindings (channel/address). Store an
   address on the **Gateway Binding Crystal** via a data component (`CESGDataComponents`); Core selects the
   destination by channel, with a filter/round-robin fallback.
4. **UI & sync.** Destination picker on the Core (reuse `network/CESGNetwork` packet plumbing +
   `ShulkerStationConfigScreen` patterns). Goggle tooltip shows active channel + partner liveness.

**Files:** `gateways/teleport/GatewayPartner` (→ multi-binding), `CrossDimensionalGatewayCoreBlockEntity`,
`init/CESGCapabilities` (item+fluid transport caps), `gateways/GatewayBindingItem` (channel), new
`gateways/GatewayPort{Block,BlockEntity}` (optional), `network/*` packets, `TeleportResolver` (item/fluid
forwarding helpers).

**Risks / cross-cutting**
- **Partner chunk unloaded** → cap query/getBlockEntity returns null. Need buffering at source +
  retry, or temporary force-load of the partner chunk during transfer.
- **Loop/dupe safety:** reject self-binding; guard against A→B→A cycles; transactional insert (simulate
  before commit).

---

## 6B — End Automation Depth

**Goal:** close the automation gaps so the End fuel chain & shell farm run hands-off, plus a couple of
new End machines. Builds on Phase 3 (`farming/ShulkerCage*`) and the real fluids (`init/CESGFluids`).

**Milestones**
1. **Automatable shulker-shell farm.** Confirm `ShulkerCageBlockEntity` drops are extractable by belts/
   funnels; if not, expose its output as an `IItemHandler` so shells pull automatically. Add a compact
   "shell collector" recipe/flow doc.
2. **Full ender→fuel line.** Verify the chain is end-to-end automatable with Create: ender pearl →
   `ENDER_PEARL_DUST` (crushing wheels) → Liquid Ender Pearl (heated basin) → Teleport Essence (+chorus)
   → Liquid Eye of Ender (+blaze powder). Fix any non-automatable step (e.g. item vs fluid mismatch).
3. **Chorus integration.** Recipe path for chorus fruit into the fuel chain (already a mixing input);
   consider a chorus-farm helper block only if there's a real gap.
4. **New machine(s) (scope-gated):** e.g. an "Ender Infuser" that converts essence↔eye, or End-stone
   decorative casing to match the texture direction. Add only if they serve the fuel/farm loop.

**Files:** `farming/ShulkerCageBlockEntity` (output cap), `recipe/*` + `datagen/CESGRecipeProvider`,
new blocks in `init/CESGRegistration` + `CESGBlockEntities`, textures.

**Risks:** balance (fuel cost vs. farm rate); keep shell drops End-only.

### Ender Infuser — DONE (rework + recipe-driven, 2026-06-30)

**Visual rework — done.** Solid copper machine with a copper-framed **front window** into a dark chamber;
fluid drawn by `EnderInfuserRenderer` (no-cull cutout, full-bright) and **rotated to match the block's
FACING** (the long-standing "fluid only shows from the side" bug — a directional BER must apply the
blockstate rotation; see memory `feedback-directional-block-renderers`). Solid copper-gearbox back; the
decorative `SHAFT_HALF` was dropped (it z-fought the fluid; the gearbox face is the kinetic-input cue).
Bucket fill/drain works.

**Recipe-driven rework — done.** New `cesg:ender_infusing` recipe type (`com.cesg.recipe.EnderInfusing*`):
`input fluid (+amount)` + `0..N item catalysts (each +count)` → `output fluid (+amount)` + `processing_time`
(tick interval at speed 64, scales with rotation). BE looks recipes up each cycle; accepts **any** fluid
(CESG/Create/vanilla) and **any** catalyst. Catalyst inventory = **3 slots**, one distinct item per slot
(no spilling), synced to client via `sendData()` so the goggle ticks in real time.

Seeded recipes (`EnderInfusingRecipeGen`): full ender chain — **Water + Ender Pearl Dust → Liquid Ender
Pearl**, **Liquid Ender Pearl + Chorus Fruit → Teleport Essence**, **Teleport Essence + Blaze Powder →
Liquid Eye of Ender**, **Liquid Eye of Ender → Teleport Essence** (reclaim); plus **Milk + Sugar + Cocoa
Beans → Chocolate** (two-catalyst, mirrors Create's mixing). Append more conversions in that one file.

**Loose end:** no EMI/JEI display for `ender_infusing` recipes yet → part of 6C integration.

---

## 6C — Create-native Integration & Polish

Split into **quick polish** (do early) and **integration** (do last, documents final behaviour).

**6C-polish (early, parallelizable)**
- Enhanced-shulker **item icon** still vanilla (only the placed BER is custom) — author item model/texture.
- Station **side faces** (noted as unfinished).
- Teleport / station **sounds & particles**.

**6C-integration (last)**
1. **Ponder scenes** (dep present, `ponder` pkg empty): author scenes for stations (loader/unloader/belt),
   enhanced-shulker upgrades, and gateway assemble→fuel→bind→travel. Register via Ponder API from a client
   setup hook.
2. **Recipe viewer support.** Add **EMI** (simplest) and/or **JEI** dep to `build.gradle`; register
   categories for heated-basin mixing, shulker tier upgrades, and the shulker cage. (Decision: EMI-only vs
   both.)
3. **Advancements.** New `datagen/CESGAdvancementProvider`: first station, first enhanced shulker, first
   tier upgrade, build a gateway, first cross-dimensional travel.
4. **Config.** NeoForge config for balance knobs (travel fuel cost, cooldowns, stress, farm rates).

**Files:** `ponder/*` (new scenes + registration), `compat/emi` and/or `compat/jei` (new),
`datagen/CESGAdvancementProvider` (new), a `CESGConfig` class, item models/textures.

**Risks:** Ponder/recipe-viewer API churn; keep compat behind soft deps.

---

## 6D — Storage Network Layer

**Goal:** link enhanced shulkers + stations into a queryable network with a terminal. Largest track —
do last; reuse 6A's cross-block capability + packet/UI patterns. The empty `storage/dock` package is a
likely home/seed.

**Milestones**
1. **Connection model.** Network controller block + membership (adjacency graph or Create-style cabling).
2. **Indexing.** Aggregate item counts across member enhanced shulkers/stations (reuse their
   `IItemHandler`s); incremental updates, not full rescans per tick.
3. **Terminal.** Block + `Menu` + `Screen` to view/search/sort/withdraw across the network (model on
   `EnhancedShulkerMenu`).
4. **Station hookup.** Networked stations route into the index automatically.

**Files:** new `storage/network/*` (controller BE, terminal block/BE/menu/screen), reuse
`storage/station/StationCapabilities` + `upgrades/EnhancedShulker*`, `network/CESGNetwork` packets.

**Risks:** performance with many members (cache + dirty-flagging); significant UI work; save/load scale.

---

## 6E — Decorative variants (end stone & purpur) — BUILT + NAMED (2026-06-30)

**Status:** the four families (smooth/polished end stone & purpur), each base + stairs + slab + wall, are
registered in `decoration/CESGDecoratives` with blockstates/models/loot and stonecutting/crafting recipes.
**Display names added** in `CESGLangProvider.decorativeNames()` — Registrate's auto-lang was being clobbered
by the manual provider (names showed in `en_ud` but not `en_us`), so they're now set explicitly. Remaining
polish: custom smooth/polished textures (currently reuse the base via `cube_all`).

**Goal:** smooth & polished building variants of end stone and purpur, matching the texture direction.

**Notes**
- Vanilla already has `end_stone_bricks` (+stairs/slab/wall) and `purpur_block`/`pillar`/`stairs`/`slab`.
  New blocks fill the *smooth* and *polished* gaps: **smooth end stone, polished end stone, smooth purpur,
  polished purpur** (base full blocks), with shape families (stairs/slab/wall) per the answer below.
- Register as plain Registrate blocks (`init/CESGRegistration`), `MINEABLE_WITH_PICKAXE` + `needs_*_tool`
  tags as appropriate; generate models (`cube_all`/`*_stairs`/`*_slab`/`*_wall`) and loot.
- Recipes: vanilla-style **stonecutting** (base → variant) + crafting (2×3 / 4-square polished). Add to
  `datagen/CESGRecipeProvider`.

**Files:** `init/CESGRegistration`, `datagen/CESGRecipeProvider`, blockstate/model + loot datagen, lang,
textures (smooth = blurred base; polished = tighter grain — see texture-art-direction).

**Risk:** purely additive; main cost is datagen volume if full shape families are chosen.

## 6F — Teleport & Teleport Resistance potions

**Goal:** two brewable potions built on the existing `ender_pearl_dust` item.

**Design (defaults; behaviour confirmed via questions)**
- New registries needed (no existing potion/effect code): `MobEffect` (`Registries.MOB_EFFECT`) and
  `Potion` (`Registries.POTION`) DeferredRegisters, plus potion items via the vanilla `potion` item with
  our `Potion` holders. Brewing via NeoForge `RegisterBrewingRecipesEvent` (`builder.addMix(...)`).
- **Teleport potion:** effect behaviour per the answer (random blink / spawn return / etc.). Brew:
  awkward potion **+ ender pearl dust** → Teleport potion (then redstone/glowstone for duration/amplifier).
- **Teleport Resistance potion:** the inverse — a `MobEffect` whose handler cancels NeoForge
  `EntityTeleportEvent` (scope per answer). Brew vanilla-style: Teleport potion **+ fermented spider eye**
  → Resistance (corruption pattern).
- Effects need an event handler (`EntityTeleportEvent` for resistance; tick logic for the teleport effect).

**Files:** new `init/CESGEffects` + `CESGPotions`, a `potion/` package (effect classes + event handler),
brewing registration in `CESG` mod-bus setup, `datagen` lang + (optional) advancement, textures for the
potion effect icons.

**Risk:** brewing API specifics; making resistance cancel the right teleport sub-causes without breaking
the mod's own gateway travel (decide whether gateways bypass resistance).

## 6C ponder implementation note

Ponder lib API (confirmed in `ponder-neoforge 1.0.82`): implement
`net.createmod.ponder.api.registration.PonderPlugin` (`getModId`, `registerScenes`, `registerTags`,
`registerSharedText`); in `registerScenes` call
`helper.forComponents(blocks...).addStoryBoard(id, storyBoard, tags...)` (or `helper.addStoryBoard(...)`).
Register the plugin in **client setup** (`PonderIndex.addPlugin(...)`). Scenes live in a new `ponder/`
package with `PonderStoryBoard` builders using `SceneBuilder`/`WorldInstructions`. Seed scenes: a station
(loader), enhanced-shulker upgrades, gateway assemble→fuel→bind→travel.

## Cross-cutting checklist (all tracks)

- **BE-backed capabilities only** for anything Create pipes touch (Phase 5 lesson).
- **Cross-dimension safety:** chunk-load handling, simulate-before-commit, no dupes/loops.
- **Data-component versioning** for new stored data (bindings, channels, network IDs).
- **Datagen:** lang, models, recipes, advancements, tags all regenerated (`runData`).
- **TESTING.md:** add a manual regression section per track.

## Locked decisions (2026-06-28)

1. **6A transfer: Both** — capability bridge (BE-backed item/fluid handler → partner buffer) for
   automation, *and* keep the physical portal pass-through for players/loose items.
2. **6A addressing: Channels/addresses** — Binding Crystal stores a channel/address (data component);
   Core routes by channel. Needs a destination-picker UI + multi-binding `GatewayPartner`.
3. **6C recipe viewer: Both EMI + JEI** — register categories in both (heated-basin mixing, shulker tier
   upgrades, shulker cage). Behind soft deps; share a common category-data layer to limit duplication.
4. **6D topology: Block adjacency** — members join when touching a controller cluster; no cable blocks.
5. **6B scope: Add 1–2 new machines** — close automation gaps *and* add a focused machine, e.g. an
   **Ender Infuser** converting Teleport Essence ↔ Liquid Eye of Ender, reusing the existing fuel chain.
6. **6E variant scope: Full families** — smooth + polished base blocks AND stairs/slabs/walls for both
   end stone and purpur.
7. **6F Teleport potion: periodic random blink** — effect with duration that randomly teleports the
   drinker every few seconds (chorus-style, longer range).
8. **6F Teleport Resistance: blocks ALL teleportation** — cancels every teleport while active, including
   the drinker's own ender pearls AND gateway travel (gateway code must check the effect explicitly, since
   its custom teleport doesn't fire `EntityTeleportEvent`).
