# Issues

## Functionality

1. ~~Verify that stack upgrades do not "Stack".~~ **RESOLVED 2026-07-02** — effect never stacked
   (highest installed tier already wins), but a whole stack could sit in one upgrade slot wasting
   items. Upgrade slots now hold exactly 1 (`EnhancedShulkerMenu.UpgradeSlot` max stack = 1).
   Pre-existing over-filled slots still extract normally.
2. ~~Items are not moving from Tiered shulkers using create native items like funnels and chutes.~~
   **RESOLVED 2026-07-02** — placed enhanced shulkers never exposed an item-handler capability at
   all (nothing to do with the loader guard, which is station-side and unchanged). New
   `EnhancedShulkerBlockItemHandler` registered for the placed block: funnels/chutes/hoppers/pipes
   can insert and extract; nesting shulkers is rejected (vanilla rule); access goes inert while a
   player has that box's GUI open (snapshot-clobber guard). Loader-held shulkers behave exactly as
   before. Requires client restart to test.

3. ~~Unloader won't eject the finished shulker into a smart chute below~~ **RESOLVED 2026-07-02** —
   eject only exposed the box toward Create funnels in extract orientation; chutes/hoppers/pipes never
   qualified. Eject is now extractor-agnostic: once the eject condition is met, the finished box is
   extractable from ANY side by any puller. Goggles reworded ("Ready to eject — extract from any side");
   the misleading "Eject funnel: south" listing is gone. Requires client restart.

4. Belt Unloader design tightened **2026-07-02** (from chat): docked contents now leave EXCLUSIVELY
   through the tube onto the belt below — side funnels/hoppers can no longer drain items past the
   tube. Docking (empty station) and finished-box eject from any side still work. Stationary
   Loader/Unloader unchanged (side access is their purpose). **2026-07-04: Belt Loader is now
   belt-intake-only too** (side inserts closed; dock + eject unchanged) — both belt stations are
   tube/belt-exclusive for contents.

5. ~~Unpowered unloader + side funnel dupes infinite items~~ **RESOLVED 2026-07-02 (CRITICAL)** —
   simulate/execute contract violation: the speed-scaled transfer budget (0 when unpowered) was only
   applied to REAL extracts, so simulation promised a full stack while execution delivered nothing.
   Create's funnels trust the simulated stack and spawn it anyway → infinite dupe. Same asymmetry on
   the loader's insert path silently VOIDED items (simulate accepted all, execute accepted only the
   budget). Both handlers now apply the budget to simulation too — unpowered stations promise
   nothing, and powered stations only promise what this tick's budget allows. Requires restart.
6. ~~Too much information in station goggle tooltips~~ **RESOLVED 2026-07-02** — compact/detailed
   split, Create-style. Plain goggles: docked box + fill count, unpowered warning, green
   "Ready to eject" when actionable, and a "[Sneak] for settings & filters" hint. Sneaking adds:
   retention/eject-when/threshold, eject + placement + config hints, station name, installed
   upgrades, and the filter lines. Also consolidated the four stations' duplicated tooltip code
   into one shared StationGoggleTooltip.appendStationTooltip. Requires restart.
7. ~~Manually placing raw items into a smelting-upgraded shulker does not cook them~~ **RESOLVED
   2026-07-02** — GUI clicks and shift-clicks bypassed the item handler where the module hooks live
   (they wrote to slots directly). Cursor placement now routes through the handler
   (EnhancedShulkerBoxSlot.safeInsert) and shift-clicks use a handler-backed move — so GUI inserts
   smelt/void identically to automation. Hotbar-swap (number keys / offhand F) is covered too via
   a setByPlayer transform — all manual paths now apply modules.
8. ~~Void doesn't work when manually placing or shift placing~~ **RESOLVED 2026-07-02** — same root
   cause and fix as #7. Voided overflow leaves the cursor/stack (that's the accept-and-destroy
   contract); the box must be TRULY full for the type before anything voids.
9. ~~Void should be configurable (which items to void)~~ **RESOLVED 2026-07-04 (independent)** —
   the Void module now has its OWN crimson ghost config slot (below the aqua filter slot), fully
   independent of the storage filter: configured = only matching overflow voids; empty = void any
   stored type. The void-list is stored ON the module item itself, so it travels with the module
   when moved between boxes.

10. ~~GLITCH: filter item destroyed when removing the Filter Upgrade with an item configured~~
   **RESOLVED 2026-07-02 (item loss)** — the filter slot stored a REAL item and the upgrade-removal
   cleanup wiped it. The slot is now a Create-style GHOST slot: clicking with an item sets a COPY as
   configuration (nothing consumed), empty-hand click clears, shift-click no longer hijacks items
   into it, and removing the upgrade discards only configuration — never a real item. Configured
   filters render translucent. (Items already consumed by the old slot before this fix are not
   recoverable.)

11. ~~RED ALERT: hotbar-swap on an oversized stack (512) moves it into the player inventory and
   CRASHES the game~~ **RESOLVED 2026-07-02 (crash)** — vanilla's SWAP click moves the slot stack
   RAW with no clamping (every other path — cursor pickup, shift-click, throw — clamps to vanilla
   max). Guarded in the menu: swapping an oversized slot now moves ONE vanilla-sized bite into an
   empty hotbar/offhand slot and leaves the rest; swap into an occupied hotbar slot is a no-op for
   oversized stacks. Creative middle-click CLONE clamped the same way.

12. ~~Installing a Smelting Upgrade leaves EXISTING contents raw~~ **RESOLVED 2026-07-02** —
   the transform only ran at insert time. Installing the module (or opening a box that has one) now
   runs a full-inventory smelt pass, mirroring how the compacting module compacts on install.
   Compaction runs after conversion so newly-identical stacks merge. Safety: a slot is skipped if
   the smelted result would exceed its slot limit (rare shrink-stack recipes), staying raw instead
   of clamp-destroying items. **Chained recipes (2026-07-02):** multi-step smelts (cobblestone ->
   stone -> smooth stone) now convert to their FIXED POINT in one pass — inserts and the bulk pass
   agree, and reopening a box never advances the stored form another step. Cycle-guarded against
   datapack recipe loops. **Live convergence (2026-07-02):** while the GUI is open, a once-a-second
   pass converts any raw contents visibly (no close/reopen), kept near-free by a terminal-item
   cache once everything is converged.

13. ~~3.4.4 unclear: does a shulker bullet hitting a cage do anything?~~ **CLARIFIED 2026-07-02**
   — it works and is intentional: any REAL shulker bullet striking an occupied cage in the End
   knocks a shell loose (same cooldown/difficulty rules). The powered paired-cage farm never spawns
   real bullets (its "shot" is abstract), so this only triggers from wild shulker crossfire — a
   flavor bonus, now documented in the cage's tooltip. Repro steps added to TESTING 3.4.4.

14. ~~4.7.2 needed a defined behavior for riding through gateways~~ **DEFINED 2026-07-04** —
   everything in the portal plane goes through, and every ENTITY pays the travel cost individually
   (player in a boat = 2x; each dropped item = 1x). Riders dismount on dimension change (vanilla
   engine rule); if fuel runs out mid-group, the remainder stays behind. This pricing is deliberate:
   the portal is the premium path, Gateway Ports are the free bulk-logistics path. Goggle line now
   reads "per entity". No code change beyond the label.

15. Port transfers stall when the far dimension is unloaded (by design: buffer + retry) —
   **OPTION ADDED 2026-07-04**: per-gateway "Keep destination loaded" toggle in the destination
   picker (OFF by default). While ON, the core keeps its OWN chunk and the ACTIVE channel's
   destination chunk loaded and ticking (block-owned NeoForge tickets, persist across restarts), so
   port transfers flow with nobody on either side. Tickets follow the active channel: switching
   destination releases the old pair and loads the new one; toggle-off/unbind/core removal releases
   everything. Server config gateway.allowChunkLoading=false removes the option entirely.
   Destination-switch mid-transfer verified safe: ports resolve the partner LIVE on every flush, so
   the next flush serves the new destination; items already delivered stay at the old one.

## Art Pass

1. ~~Fabricated End Gateway as creative tab icon~~ **DONE 2026-07-04** (one-liner in CESGCreativeTabs).
2. ~~Terminal/Controller 3D item models~~ **ALREADY IN 2026-07-04** — item models parent the 3D block models (came free with the model rework). Verify in-game; if they render flat, flag again.
3.Add "placeholder" icon indications in the upgrades slots for all tiers of shulkers
4. ~~Controller connective textures~~ **DONE 2026-07-04 (Wave 2)** — plate tops/bottoms use the seamless casing tile, so side-by-side controllers read as one bank (true CT doesn't apply to an open 3D frame).
5. ~~Gateway Frame diagonal white line~~ **DONE 2026-07-04 (Wave 1)** — interior rebuilt as clean translucent teal glass; lit strip keeps its animation pulse.
6. ~~Gateway Frame connected textures~~ **DONE 2026-07-04 (Wave 2)** — frames merge into one large brass-outlined window via Create's CT system; lit and unlit connect; sheets generated algorithmically from Create's own casing CT layout.
7.Front faces for Shulker Loader, Unloader, Belt Loader, Belt Unloader need redesign/refinement. I dont hate the concept but it doesn't seem very create themed. Right now they are just regular minecraft square blocks and they probably could benefit from being more create meachine designed so that they look natural in a factory.
8. ~~Dedicated tier-themed upgrade textures~~ **DONE 2026-07-04 (Wave 1)** — full card family regenerated from one template: andesite/brass/diamond tier borders (magnets + stack depth), glyph identities (coil/bars/flame/void-ring/funnel/press); filter & compacting reskinned to match.
9. ~~Emergency Eye Charge artwork~~ **DONE 2026-07-04 (Wave 1)** — recomposited from its recipe: ender-eye core, blaze-powder flare rays, ender-pearl orbit dots.
10. ~~Notch border overlapping main panel~~ **DONE 2026-07-04** — the notch is drawn open on the right (no right border) and its bottom shadow stops where the main panel body begins; seam is now a smooth body-color transition.
11. ~~Ghost placeholders should render solid~~ **DONE 2026-07-04** — translucent overlay removed; the placeholder and configured filter render at full opacity (the aqua accent frame still marks the slot as configuration).

## Additions

1. ~~New upgrades for shulker boxes (Cooking, smelting, void, etc)~~ **IMPLEMENTED 2026-07-02** —
   two new modules (test rows 2.2.5b-f): **Smelting Upgrade** (inserted items stored as their furnace
   result — covers ore smelting AND food cooking; no-recipe items store unchanged; leftover stays
   raw; 1:1 recipes only) and **Void Upgrade** (overflow of types the box already stores is
   destroyed once truly full; other types still rejected — not a universal trash can). Both work in
   GUI + all automation paths (same handler). More module ideas (e.g. washing/crushing) can reuse
   the same applyX-on-insert hook. **2026-07-02 also added: Magnet Upgrades Mk I-III** (rows
   2.2.5g-l) — placed boxes pull in dropped items they can accept (4/7/10 block radius, increasing
   pull speed; highest tier wins). Won't attract items the box can't take (no orbiting), ignores
   shulker items, pauses while the GUI is open; magnet+void = intentional vacuum-trash combo.
2. Add End Specific power source — gravity/void battery over the void
   ([Gravity battery](https://en.wikipedia.org/wiki/Gravity_battery)). **QUEUED after art pass**;
   design details to be ironed out first (working concept: "Void Winch" — charge by winding a
   weight up on rotation, discharge by letting it fall; capacity = clear drop below).
3. **QUEUED: Crafting Terminal** (from Wave 2 art review) — a 3x3 crafting grid inside the
   Storage Terminal GUI that pulls ingredients from the network (search + craft in one screen).
   Needs design: grid placement in the layout, ingredient auto-pull semantics, shift-craft
   batching. The terminal SCREEN texture already previews a crafting-grid motif.
4. **QUEUED module additions** (reuse the transformForStorage/insert hooks): washing, crushing,
   mixing, mechanical crafting, blasting, haunting. Blasting/haunting = same chained-transform
   pattern as smelting with a different recipe type (cheap); washing/crushing/mixing produce
   multi-output results — need an output-overflow rule before implementing.

