# 2.0.0 QA plan

Full QA for the 2.0.0 release candidate: everything new in 2.0, a world upgrade from 1.2.0, and a
regression pass over every existing feature. Record each result inline (`[x]` + date, or `FAIL` +
short note and issue link). Nothing ships while a Blocker or Major is open.

**Owners:** **[A]** headless/automated, run by Claude · **[M]** Andrew in a game client ·
**[MP]** needs a dedicated server and a second player.

**Severity:** **Blocker** — crash, world corruption, item/fuel loss or duplication, save data lost.
**Major** — a feature does not work as documented. **Minor** — cosmetic, wording, ordering.

**Test setups**

| ID | Setup | Used for |
|---|---|---|
| S1 | Dev client from `2.0-dev`, JEI + EMI, new creative world | most [M] checks |
| S2 | Production client (normal install), candidate jar, NeoForge 21.1.233, Create 6.0.10, no recipe viewer | release-artifact smoke, survival checks |
| S3 | Production dedicated server + S2 client, two players | [MP] checks |
| S4 | S2 plus Create Aeronautics 1.3.2 (+ Sable), Create Sifting, Create Ultimate Factory, Quark | addon behaviour in a real pack |
| S5 | Packaged dedicated server with the July fixture world (`run/packaged-upgrade` in the 1.2 worktree) | upgrade from 1.2.0 |

---

## 1. Release candidate [A]

- [ ] Build the jar from the `2.0-dev` tip; record commit, size and SHA-256.
- [ ] Jar metadata: version `2.0.0`, NeoForge `[21.1.233,)`, Create `[6.0.10,6.1.0)`, Minecraft `1.21.1`.
- [ ] Jar contents: no GameTest classes or `structure/empty.nbt`; contains
  `data/minecraft/recipe/shulker_box_coloring.json`, `ender_pearl_dust_from_end_stone_washing.json`,
  the six Haunting/Blasting textures, models and recipes; no `*_from_dye.json` recipes.
- [ ] `./gradlew clean build runGameTestServer`: zero compiler warnings, all 53 GameTests pass.
- [ ] `python tools/ponder/verify.py` passes; `git diff --check` clean.

## 2. Compatibility audit [A]

- [ ] 59-addon pack (`-Pneo_version=21.1.266`): all GameTests pass, `cesgRecipesHaveNoSameInputTwin`
  reports no conflicts, End Stone crushing/milling contain no `cesg` recipe, washing resolves to CESG.
- [ ] Production dedicated server, candidate jar, **NeoForge 21.1.233** (our minimum): reaches Done,
  no CESG errors or missing registries, clean shutdown.
- [ ] Same on the **latest NeoForge 21.1.x** (21.1.266 at time of writing).
- [ ] Server log on both: `CESGNetwork` subscribed to the mod bus; no mixin or registry warnings
  that 1.2.0 did not already emit.

## 3. World upgrade 1.2.0 → 2.0.0 [A] (S5)

Repeat the method in `docs/dev/RELEASE-1.2.0-UPGRADE.md`, this time keeping the NBT dump/compare
script in `tools/qa/` so the next release can reuse it.

- [ ] Before upgrading, scan the 1.2.0 baseline for gateway cores still carrying a `Partner` tag
  (the removed pre-1.0 format). Expected: none. If any exist, stop and decide before shipping.
- [ ] Baseline: load all fixture chunks with released 1.2.0, save, stop. Dump all CESG block entities.
- [ ] Swap in the 2.0.0 candidate, load the same chunks, save, stop; start again, save, stop.
- [ ] Compare: every CESG block entity keeps position and identity; no missing/added entities.
- [ ] Every gateway core keeps its bindings, channels, filters, route modes and name.
- [ ] Every core's `Portal`/`FrameLit` cache is identical before and after (touching-frame
  detection must not change any existing gateway).
- [ ] Battery fuel: controller tanks unchanged except where the fixture is actively filling them.
- [ ] Enhanced shulkers (placed and in stations): tiers, contents, upgrades and filters unchanged.
- [ ] No CESG load/save errors in either 2.0.0 run.
- [ ] Record the evidence files alongside this plan's results.

## 4. New in 2.0 [M] (S1 unless noted)

### 4.1 End Stone → pearl dust moves to washing
- [ ] Encased fan through water over 200 End Stone gives 200 Sand and roughly 30 Ender Pearl Dust
  (accept 18–42).
- [ ] A placed Enhanced Shulker Box with a Washing upgrade does the same.
- [ ] Crushing wheels and a Millstone given End Stone (no addons): record what happens. Expected:
  no pearl dust; the End Stone is not destroyed.
- [ ] JEI and EMI show End Stone under fan washing, not under crushing or milling.
- [ ] Full farm loop from cobblestone with zero pearls still works (haunt sandstone → wash End
  Stone → compact 3 dust → pearl).
- [ ] S4: crushing wheels on End Stone give Aeronautics End Stone Powder; a heated mixer makes
  Levitite Blend from it; the fan-washing path still gives pearl dust.

### 4.2 Enhanced shulker dyeing
- [ ] Uncolored box (tier 3–4, contents, upgrades, filter, custom name) + dye → enhanced box of
  that color with everything kept. Open it and check contents and upgrade slots.
- [ ] Already-colored box + a different dye → recolored, everything kept (was turned into a vanilla
  box in 1.2).
- [ ] Vanilla shulker boxes still dye normally.
- [ ] Dyeing works in the Storage Terminal crafting grid and a Create mechanical crafter.
- [ ] JEI and EMI each show 16 enhanced dye entries in crafting; vanilla shulker dyeing still shown.

### 4.3 Polished End Stone
- [ ] 2×2 Smooth End Stone → 4 Polished End Stone; 2×2 End Stone → 4 End Stone Bricks (vanilla).
- [ ] Stonecutter: End Stone → Polished End Stone still works.

### 4.4 Battery array relocation
- [ ] Sneak-wrench a member of a full 3×3×3 array: one item; tooltip shows 3×3×3 and the full fuel.
- [ ] Same for a 2×2×1 partly filled array and a 2×2×2 with Liquid Eye of Ender.
- [ ] Place each facing north, east, south and west: builds forward and to your right, correct size,
  same fuel (check with goggles), gateway draws from it.
- [ ] Place against the underside of a block: builds downward. (Not covered by GameTests.)
- [ ] Refusals keep the item and change nothing: obstructed space, battery touching the space,
  space partly in an unloaded area (if reachable).
- [ ] Lower the per-block capacity in the server config below the stored fuel: placement refused,
  item kept; restore config.
- [ ] Empty array → plain stackable batteries, count = W×W×H.
- [ ] Full inventory: the item drops at the player, exactly one.
- [ ] Creative: picking up gives the item; placing consumes it.
- [ ] Packed item survives save/reload in inventory and in a chest.
- [ ] Single isolated battery pickup and placement still work as in 1.2.
- [ ] Mining an array normally is unchanged (drops carry no fuel).
- [ ] [MP] With a claim mod (e.g. Open Parties and Claims), a non-member cannot pick up an array or a
  single battery inside the claim, and cannot place a packed array into it.

### 4.5 Touching gateway frames
- [ ] Two rings sharing a frame column: both open; each teleports to its own destination.
- [ ] Two rings with adjacent columns, stacked rings sharing a row, and a stray frame touching a
  ring: all open.
- [ ] With gateway A active and travelling, build ring B against it: A never closes.
- [ ] Break a shared frame: both close; replace it: both reopen.
- [ ] Goggles on a broken ring show the new wording; the frame tooltip no longer asks for spacing.
- [ ] Shared frames: lighting/colour flicker when one gateway turns off is brief and cosmetic.
- [ ] Ports, Bridges and Batteries on a shared frame: record which gateway they serve (documented
  as unchanged).
- [ ] Existing single gateways in an older test world behave exactly as before.

### 4.6 Haunting and Blasting upgrades
- [ ] All six craft with the documented recipes; icons, names and tooltips correct in inventory,
  upgrade slots, JEI and EMI.
- [ ] Haunting (placed box): sandstone → End Stone; cobblestone → blackstone; sand → soul sand.
- [ ] Blasting (placed box): raw iron/gold/copper and Create crushed ores → ingots; an iron sword,
  sand, cobblestone and beef are left alone.
- [ ] Speed: count conversions over 60 s for Mk I/II/III (expect about 60/120/240).
- [ ] Only the highest tier counts with two installed.
- [ ] Priority: Crushing + Haunting mills sandstone to sand (expected); Haunting + Washing farms
  pearl dust; Crushing + Blasting crushes ore, then blasts the crushed ore.
- [ ] Full box holds; nothing lost. Processing pauses with the box screen open and does not run in
  an unplaced box or a station.

### 4.7 Smelting fixes
- [ ] Smelting alone: raw iron → ingot; iron/gold tools and armour are not melted.
- [ ] Smelting + Crushing: raw iron and iron ore stay raw for Crushing; sand → glass; cobblestone →
  smooth stone (as in 1.2).

### 4.8 Cleanup
- [ ] All four registered CESG screens open (Enhanced Shulker, Storage Terminal, Storage Bridge,
  Gateway Filter) — confirms client menu registration after the event-bus change.
- [ ] Station configuration and gateway channel changes made in a GUI apply on the server (S3).

## 5. Regression of existing features [M]

Condensed from `docs/dev/PHASE7-QA.md` and the 1.2 records. Run in S2 survival unless noted.

- [ ] **Stations:** Loader/Unloader and Belt Loader/Unloader dock, fill, empty and eject on their
  conditions; retention modes, thresholds, name stamping, filters, goggles.
- [ ] **Enhanced shulkers:** tier 2→3→4 upgrade keeps contents/upgrades/name; Stack Depth, Void,
  Magnet, Filter, Compacting, Crushing, Washing each work; box keeps everything when broken/placed.
- [ ] **Ender Barrels:** twin pair shares inventory across dimensions; pair IDs.
- [ ] **Storage network:** controller clusters storage; terminal search, insert/extract, crafting
  grid (incl. cake bucket remainders), JEI/EMI recipe transfer and R/U shortcuts.
- [ ] **Storage Bridge:** remote tab, push/pull, filters, lock, passive transfer, cross-dimension,
  diagnostics colours/tooltips.
- [ ] **Gateways:** player teleport same-dimension (Essence) and cross-dimension (Eye); 16 channels;
  route mode with item and fluid filters; Ports move items and fluids; chunk loader option.
- [ ] **Fuel:** batteries top up the Core; automation reserve keeps travel fuel; costs as configured.
- [ ] **End farming:** Shulker Cage farms shells; Ender Infuser recipes; filling/compacting/mixing
  recipes from the 1.1 farm loop.
- [ ] **Decoratives:** smooth/polished End Stone and Purpur families, stairs/slabs/walls, stonecutter.
- [ ] **Ponder:** every CESG Ponder scene opens and plays.
- [ ] [MP] Two players on S3: terminals, bridges and gateways sync; teleporting players; no
  disconnects or log errors.

## 6. Exit criteria

- [ ] All [A] items pass; all [M]/[MP] items pass or are logged Minor with Andrew's sign-off.
- [ ] Changelog matches what was tested; the crushing-wheel pearl-farm upgrade note is prominent.
- [ ] Known limits recorded for the release notes (anything not tested, e.g. third-party block
  movers, resource packs, non-bucket modded fluid containers).
- [ ] Then write `docs/publishing/RELEASE-2.0.0.md`, open the release PR into `main`, tag `v2.0.0`.

---

## Results log (2026-11-13)

**Release candidate:** `2.0-dev` @ 2a998e4, jar SHA-256
`75a287e8d32aac972a963597666b46d1e35ceb09e1ef05da9c88394dd102e50c`. Earlier candidates 4b566ff and 9ab8b79
were superseded by the QA fixes in PRs #27 and #28.

**Headless [A]: all pass on this candidate**
- §1: metadata 2.0.0 / NeoForge [21.1.233,) / Create [6.0.10,6.1.0); no test classes in the jar; zero
  warnings; 58/58 GameTests; Ponder verify; diff --check.
- §2: 59-addon pack on NeoForge 21.1.266: 58/58, no CESG recipe conflicts. Production dedicated servers on
  21.1.233 and 21.1.266 start, save and stop cleanly.
- §3: 1.2.0 → 2.0.0 upgrade of the July fixture (1,093 CESG block entities): none missing or added, no
  `Partner` tags. Only fields that a plain 1.2.0 restart also changes differ (Cooldown, LastItemActivity,
  HeldShulker, NeedsSpeedUpdate). Scripts: `tools/qa/cesg_nbt.py`, `tools/qa/run_server.py`.

**In-game [M] (dev client): pass**
- 4.1 Fan washing: 256 End Stone → 256 sand + 41 dust (16%). EMI lists End Stone under Bulk Washing only.
  **Aeronautics 1.3.2 loaded:** crushing wheels give End Stone Powder (no CESG output), and EMI shows the
  heated Levitite Blend recipe (4 powder + 2 zinc nuggets + water).
- 4.2 Dyeing keeps contents, upgrades and name for colored and uncolored enhanced boxes; vanilla boxes dye
  as before; EMI entry shown.
- 4.3 EMI: 2×2 Smooth End Stone → 4 Polished End Stone.
- 4.4 2×2×2 and 2×2×1 arrays: pickup and placement keep exact fuel; placing under a ceiling builds
  downward; obstructed space and lowered capacity are refused and the item is kept; an empty array gives 4
  plain batteries; creative uses up the item.
- 4.5 Touching rings bind, power up and both open; the goggle text for a broken corner is correct. Travel
  after the fix: standing in a portal crosses once and lands in front of the partner (one 250 mB charge).
  Walking through works. A ring battery tops the core up to 4000 mB.
- 4.6 Haunting/Blasting icons, tooltips, recipes and in-box processing. 4.7 Smelting fixes.
- 4.8 Enhanced Shulker, Storage Terminal, Storage Bridge and Gateway Destination screens open, and a channel
  change reaches the server.
- §5: the terminal shows networked storage; a shift-click batch craft gives 64 sticks from 32 planks after
  the fix. An Ender Barrel pair shares items between the Overworld and the Nether. A Shulker Loader fills a
  docked box from a hopper. The Gateway Core Ponder plays. Bridge and port routing are covered by 25
  GatewayRegressionTest GameTests (unchanged code in 2.0).

**Bugs found and fixed**
- PR #27 (Major, since 1.0.0): a traveller standing in a portal bounced between gateways every cooldown,
  paying each time.
- PR #27 (Major, 2.0): items converted by the Smelting upgrade started a new slot per insert.
- PR #28 (Major, since batch crafting shipped): a terminal shift-click batch craft switched recipe and
  used up planks as Oak Buttons.

**Not tested (release notes):** multiplayer, real claim/protection mods (only test listeners), third-party
block movers, resource packs, modded non-bucket fluid containers, a graphical normal-install client.
Belt stations, the Bridge remote tab and Ports were not exercised by hand in 2.0 (their code is unchanged).
