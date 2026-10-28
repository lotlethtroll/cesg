# CESG 1.1.1 — reliability patch

Status: approved for the 2026-10-28 GitHub release, `v1.1.1`.
The unverified checks below remain documented limitations accepted for this release.
The 1.2 feature plan is in [NEXT-RELEASE.md](NEXT-RELEASE.md).

## Scope

- Eliminate Bridge item rollback into potentially rejecting inventories.
- Reserve passive buffer space before extraction.
- Refund automation fuel when nothing crosses the gateway.
- Select fuel and protected reserves by actual destination, for both Ports and
  Bridges. A flush pays once per fuel type used, not once per stack or channel.
- Finish local delivery of incoming Bridge buffers even when the link is down.
- Read Bridge goggle status from its synchronized block state and keep missing
  remote controllers faulted during periodic status checks.
- Avoid unloaded chunks in network/fuel scans, isolate membership caches by
  world, recheck controllers, and fix prematurely truncated branching scans.
- Repair Ponder verification and clarify supported dependency versions.
- Return terminal grid contents on close, prevent partial-output crafting
  duplication, preserve rejected recipe-transfer leftovers, and keep crafting
  remainders intact during network refill.

No recipes, item IDs, existing NBT keys, or configuration defaults changed.
Automation still defaults to free transfers. Existing buffers load normally.

## Automated checks

Run from the repo root:

```sh
./gradlew build runGameTestServer
python tools/ponder/verify.py
```

The headless GameTest server uses `run/gametest`, separate from the normal client
and server world. Test classes/resources live in `src/gameTest` and do not ship
in the jar. JUnit fuel-accounting tests live in `src/test`.

- [x] Build and 2 JUnit tests pass.
- [x] All 17 server GameTests pass (2026-10-28).
- [x] Ponder registration, geometry, battery footprints, belts, and language checks pass.
- [x] Release jar reports version 1.1.1 and excludes test classes/template.
- [x] `git diff --check` passes.

GameTest coverage:
- Terminal close returns items exactly once; partial output cannot duplicate;
  recipe transfer preserves refused grid contents; cake crafting retains all
  three buckets even when the network has spare milk buckets.
- Failed fuel payment leaves a filtered source untouched; a funded withdrawal
  succeeds and pays the configured cost.
- A nearly full Bridge buffer only extracts what it can retain.
- Idle Bridge and full terminal deposit spend no fuel.
- Port and Bridge routing use Eye fuel for Nether destinations even when the
  active Overworld channel has no Essence.
- Blocked Port item/fluid transfers spend no fuel; buffers serialize intact.
- Dry-battery travel reserves gate paid automation; Bridge buffers serialize intact.
- Missing remote controllers stay faulted through periodic Bridge status updates.
- Zero-cost Port transfers retain their existing free behavior at the reserve floor.
- An unloaded partner retains cargo without charging fuel or loading its chunk.
- Branching network scans process 256 members; removed controllers immediately
  disable cached-network transfers; unloaded anchors are not force-loaded.

## Live client spot checks (2026-10-28)

Tested through the Minecraft UI in Andrew's new creative single-player world.
A terminal at (-140, 68, 420) touches a controller at (-139, 68, 420), with no
storage inventories attached. This intentionally makes grid returns to storage
fail, while still allowing the terminal menu to open.

- [x] Put 17 diamonds in the crafting grid, close, and reopen: all 17 returned
  to inventory and the grid was empty.
- [x] With those 17 diamonds in the grid and two oak logs in inventory, use EMI
  recipe transfer for oak planks: diamonds stay in the grid and both logs remain.
- [x] Move diamonds back into inventory and repeat EMI transfer: one log enters
  the grid, one stays in inventory, and the result shows four oak planks.
- [x] Click clear-grid with no storage capacity: the log remains in the grid.
  Closing the menu returns it, restoring the inventory total to two logs.

### Andrew's manual checks (2026-10-28)

Andrew reported both of the following live-client tests passed:

- [x] Shift-craft four oak planks with only one plank of inventory capacity:
  no partial output moves and the log remains. After freeing an inventory slot,
  crafting succeeds with 67 planks total and consumes the grid's log.
- [x] Shift-craft one cake with spare milk in network storage: receive one cake,
  retain all three empty buckets in the grid, and leave the spare milk bucket in
  storage. Closing the terminal returns all three empty buckets to inventory.

No item loss, duplication, or crash was observed in the computer-use spot checks.
The subsequent shift-crafting and cake-refill passes were reported by Andrew.
JEI separately, full-inventory close overflow, and multiplayer remain unverified.
Multiplayer cannot be exercised with the current one-player setup and remains
an explicit testing limitation, not a passed check.

### Additional single-player checks (2026-10-28)

Tested in a separate copy, `QA legacy 1.1 upgrade`, of the July QA world
`New World (2)`. The original was left untouched. Its saved mod metadata says
`1.1.0-dev`; this is legacy development-world coverage, not an exact released
1.1.0 upgrade test.

- [x] Existing End gateway machinery loads and renders. All 1,093 original CESG
  block entities retain their type and position after loading and saving.
- [x] Bridge goggles and indicator agree: linked/green, missing remote controller
  fault/red, unbound offline/gray, then restored linked/green.
- [x] Fully exit and restart the client/integrated server with nonempty buffers.
  Bridge retains 17 diamonds outgoing and 11 emeralds incoming. Port retains
  23 iron outgoing, 13 gold incoming, 500 mB water outgoing, and 700 mB lava
  incoming. Core retains 4,000 of each fuel and the battery retains 2,000 mB.
- [x] Teleport away, confirm both source and partner chunks are actually unloaded
  using `execute unless loaded`, then return. The same counts survive; Bridge
  renders linked/green again and Port goggles display the expected totals.
  Saved NBT checks after unloading and after the final save confirm these values.
- [x] Short load test: 252 enhanced shulkers plus controller and terminal
  (254 network members), with three formed 3×3×3 battery arrays nearby.
  Run at 100 TPS for approximately four minutes, keeping the terminal open for
  over three minutes. Final tick query: 2.2 ms average, P95 2.9 ms, P99 3.7 ms
  across 100 sampled ticks. Terminal remains responsive, with no runtime errors
  or can't-keep-up warnings during this interval. Restore normal 20 TPS afterward.

These persistence fixtures deliberately hold cargo without active transfers;
they do not exercise cross-dimension transactions in flight or paid travel.
Fuel values persist, but live travel-reserve behavior remains covered by GameTests.
The load test uses idle storage and empty arrays, not sustained production traffic
or a multi-hour soak. Retained block-entity identities do not prove every legacy
machine's behavior. The world saves cleanly after the checks.

## Remaining release gates — not covered by completed checks

- [ ] Connect two real clients to a dedicated server. Verify route controls,
  destination labels, locks, and Core/Bridge/Battery goggles on both clients.
- [ ] Use the same terminal from both clients; concurrently craft, deposit,
  withdraw, and run passive Bridge traffic. Count items before and afterward.
- [ ] Exercise viewed enhanced-shulker exclusion while another player operates
  the network; close/reopen menus and reconnect both clients.
- [x] Single-player restart and chunk unload/reload with nonempty Port and Bridge
  buffers; retained item, fluid, and fuel totals checked above. Dedicated-server
  and active-transfer variants remain unverified.
- [ ] Run a near-limit network and several battery arrays for an extended session;
  inspect tick times and logs. Automated limit tests are not a performance soak.
- [x] Visually confirm Bridge goggles agree with LIVE/OFFLINE/FAULT block visuals,
  including reconnects and loss of the remote controller (single-player).
- [ ] Exact released 1.1.0 world upgrade. A copied `1.1.0-dev` world passes the
  legacy loading checks above.

Do not mark multiplayer or rendered UI QA complete based on the headless pass.
The legacy 256-member network limit remains; a clear limit diagnostic is queued
for 1.2. Battery fuel-on-break and touching-frame gateways remain deferred.

## Release preparation after QA

1. Date the 1.1.1 changelog section and finalize the publishing listing.
2. Build `cesg-1.21.1-1.1.1.jar`; confirm matching versions on client/server.
3. Commit, tag, and upload only when Andrew requests those actions.
