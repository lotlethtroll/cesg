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

## [1.2.0] — 2026-10-30

### Added
- Independent per-channel fluid routing, with filled-container ghost filters and separate whitelist/blacklist settings. Fluid routing is opt-in; existing worlds keep active-channel fluid transfers.
- JEI/EMI recipe and uses shortcuts over terminal network items, respecting configured keys and search focus.
- Bridge push/pull status indicators and tooltips for unavailable transfers, plus terminal network scan-limit warnings.
- Fuel-preserving sneak-wrench pickup for isolated batteries. Charged placement consumes the item in creative too; fueled array relocation remains deferred.

### Changed
- Custom mechanical GUI textures and consistent panels, slots, headers and controls across terminals, bridges, gateways, stations and enhanced shulkers.
- Compact layouts keep inventory slots clear of recipe-viewer search at tight GUI scales.

### Fixed
- Removed obsolete out-of-bounds belt tube models.

No recipes or fuel costs changed. Install the same version on client and server.

## [1.1.1] — 2026-10-28

### Fixed
- Terminal batch refill leaves crafting remainders such as cake buckets in
  place rather than replacing them with ingredients pulled from the network.
- JEI/EMI terminal recipe transfer preserves crafting-grid leftovers when the
  network cannot accept them, instead of overwriting them with new ingredients.
- Terminal shift-crafting checks room for the entire output before moving it,
  preventing partial output duplication when the player inventory is nearly full.
- Closing a Storage Terminal returns its crafting-grid items to the player,
  dropping overflow safely instead of abandoning the temporary inventory.
- Storage Bridge terminal withdrawals reserve fuel before extracting items. An
  unfuelled withdrawal no longer needs to return items to a source that may
  reject them through its storage filter.
- Passive Storage Bridge transfers reserve buffer space before extracting, so
  a full or nearly full buffer cannot discard items during overflow recovery.
- Ports and Bridges only spend automation fuel when cargo actually crosses the
  gateway. Empty filters, full destinations, and failed terminal deposits do
  not consume fuel.
- Routed transfers use the destination's fuel and travel reserve, even when the
  active channel points to another dimension or lacks its own fuel. A flush pays
  the configured cost once per fuel type used; same-dimension traffic uses
  Teleport Essence and cross-dimension traffic uses Liquid Eye of Ender.
- Bridge incoming buffers can finish delivering locally after their gateway
  loses power or its partner goes offline.
- Bridge goggles read the synchronized block status instead of an unsynchronized
  client field. A missing remote controller consistently reports a fault instead
  of periodically reverting to linked.
- Storage-network and gateway fuel scans avoid unloaded chunks. Cached network
  membership is isolated by world, removed controllers stop authorizing transfers
  immediately, and branching scans process up to the full 256-member limit.

### Development
- Added fuel-accounting unit tests and headless server GameTests covering
  transfer safety, routing, travel reserves, persistence, and network scans.
- Repaired the Ponder verifier for the captured battery-array scenes.
- Clarified the existing Create 6.0.x and NeoForge 21.1.233+ requirements.

## [1.1.0] — 2026-07-31

### Added
- **Cross-Dimensional Storage Bridge** — the headline. Links two Storage Networks
  through a bound gateway. Place one beside a Gateway Frame or Core that also
  touches a Storage Network, put a matching Bridge on the partner ring, and each
  side's Storage Terminal grows a **Local / Partner** tab strip showing the other
  network as a clearly separate section:
  - Click to withdraw across the gateway, shift-click to your inventory,
    right-click for half a stack, or click with a carried stack to deposit
  - A liveness dot tracks the link: green linked, grey offline (unbound or the
    partner chunk is unloaded — silent, not an error), red fault
  - **Passive auto-transfer**: right-click the Bridge for push and pull rows, each
    with nine filter slots, an enable toggle and whitelist/blacklist
  - **Lock** any Bridge to stop other gateways pulling items out of that network,
    while still allowing it to push out and still accepting deposits
  - Every move is extract-then-insert with an in-transit buffer, so an unloaded
    partner or a broken Bridge can never dupe or void items; breaking a Bridge
    drops whatever it was holding
- **Gateway Routing** — a Core can now fan items out instead of only talking to
  its active channel. Turn on **Route** in the destination picker, right-click any
  channel to give it a nine-slot filter, and each item goes to the first bound
  channel that accepts it. One Core can sort iron to one base, gold to another and
  everything else to a third. Deterministic by design, so items never bounce
  between destinations. Fluids still follow the active channel, since filters are
  item-only. Off by default, and `gateway.allowFanOut` disables it server-wide.
- **Gateway Flux Battery** — a Create-style fluid multiblock that buffers gateway
  fuel so bursty travel and Port transfers never run a gateway dry:
  - Locks to the first fuel piped in, like a fluid tank; assembles into a square
    array up to 3×3×3 with capacity scaling per block
  - Tops up the connected ring's Core automatically
  - Acts as a **fuel governor**: automated Port and Bridge transfers pause once
    combined fuel would fall below a protected reserve, while **player travel is
    never gated** — automation can't strand you
  - One-click layer placement when building an array
- **Crushing and Washing modules** for Enhanced Shulker Boxes, in three tiers each.
  A placed box processes its own contents using Create's crushing, milling and
  splashing recipes, chaining to a terminal form — cobblestone all the way to sand
  — and stacking with the Smelting module. Never loses items: a full box simply
  holds its input until there is room.
- **End Cultivation** — End-specific materials are now renewable through native
  Create processing, no new blocks required:
  - Haunting (soul-fire fan) turns **sandstone into End Stone** — the renewable,
    essence-free base path, rooted in infinitely-farmable cobblestone → sand
  - Crushing **End Stone** yields sand plus a 15% chance of ender pearl dust
  - Compacting **3 ender pearl dust → 1 ender pearl**, closing a renewable
    ender-pearl loop
  - Filling plain **stone with Liquid Ender Pearl → End Stone** as an optional
    faster path once you have essence to spare
  - Renewable End Stone also makes chorus farms (and therefore purpur and
    Teleport Essence fuel) self-sustaining in any dimension
- **Sounds and particles** for gateway open/close, teleport, bridge link and fault,
  Port and Bridge transfers, and machine processing
- **Ponder scenes** for the Storage Bridge, Gateway Flux Battery (including how
  arrays form) and the Storage Terminal, alongside rebuilt scenes for the gateway
  and the shulker stations
- **JEI and EMI "+" recipe transfer** into the Storage Terminal's crafting grid,
  filling from network stock

### Changed
- Storage Terminal right-click now withdraws **half a stack** rather than a single
  item, matching vanilla slot behaviour
- Gateway Cores explain themselves when something is wrong: goggles now report a
  broken frame ring and name the specific rule that failed, show when route mode is
  redirecting pushed items, and a fuel-gated transfer says so instead of silently
  doing nothing

### Fixed
- Renaming a gateway now updates that name everywhere it appears. Bindings stored a
  copy taken at bind time, so every gateway pointing at a renamed one kept showing
  the old label
- Gateway Flux Battery goggles show capacity while empty, so an array's size is
  readable while you are building it

## [1.0.0] — 2026-07-21

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
- Enhanced / Reinforced / Ultimate Shulker Boxes show tier-colored corner-bracket
  lid trim (ender teal / andesite / brass) in world and in the item preview,
  across all 17 colors
- Tier 3 (Reinforced) and tier 4 (Ultimate) upgrades use full 3×3 recipes: the
  lower-tier box surrounded by 5 processed shells + a tier casing (andesite /
  brass) and 2 ender pearls / eyes of ender

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
