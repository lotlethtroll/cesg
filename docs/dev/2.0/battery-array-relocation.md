# Fueled battery-array relocation

Sneak-right-clicking any block of a Gateway Flux Battery array with a Create wrench now picks up the whole array as one item. The item is unstackable and records the array's width W, height H and total fuel F; its tooltip shows, for example, "3×3×3 array" and the fuel and mB amount. Both Teleport Essence and Liquid Eye of Ender are supported. An empty array gives W·W·H plain, stackable batteries instead. Creative players also receive the item. When the inventory is full, the item is dropped at the player (NeoForge `giveItemToPlayer`, which, unlike `Inventory.add`, does not delete it in creative).

Placing a packed item builds the array away from the player from the placement position: W forward, W to the player's right, and H up, or down when placing against a block's underside. The whole space is checked first. Every cell must be loaded, replaceable, unobstructed, inside build height, allowed by `mayInteract` and `mayUseItemAt`, and no battery may touch any cell. The empty blocks are then placed bottom layer first so Create forms the array. Next, an `EntityMultiPlaceEvent` is fired, the controller is checked for W, H and an intact prism, and exactly F is filled. The item is consumed only when that fill returns exactly F, in creative as well as survival. Any failure removes the placed empty blocks and keeps the item. The client places nothing. If `capacityMbPerBlock` has been lowered so that F no longer fits in W·W·H blocks, placement is refused and the item is kept. Windows and gauge are not carried over: they use the 1.2 defaults, with the gauge facing the placer.

An isolated 1x1x1 battery still uses the 1.2 path and 1.2 item format. That path now also fires `BreakEvent` and respects cancellation, so protection mods apply to it.

## Ownership and duplication

The controller tank owns all of an array's fuel. Pickup is all-or-nothing. It refuses, changing nothing, if:

- the controller can't be resolved,
- any member chunk is unloaded (an injectable predicate, so this is tested),
- the prism isn't intact,
- any member has a connectivity update pending, or
- tanks hold mixed fuels.

It also refuses if `mayInteract` fails or if any member's `BreakEvent` is cancelled. Only then does it sum every member's tank into F and set every tank empty. It removes the members with the controller last and gives one item holding F. Because every tank is already empty when Create's `splitMulti` runs, there is nothing to distribute or discard. The fuel exists only in the item.

Placement fills the tank only after the array has formed and been verified. If the fill or any earlier step fails, the placed blocks are removed. Fuel that was partly filled leaves with them, and the item still holds all of F. World fuel plus item fuel therefore never exceeds F.

NeoForge captures block snapshots during item use and defers `onPlace` until `useOn` returns, so an array could not form inside the placement call. If the place event is then cancelled, it restores the removed blocks. Both multi-block paths therefore run with snapshot capture switched off and fire their own Break/Place events instead (verified against NeoForge 21.1.233 sources and the patched `LevelChunk`/`Level` bytecode). Vanilla `ServerPlayerGameMode` restores a creative player's held count after `useOn`. To consume the item, packed placement detaches the spent stack from the hand.

Normal mining, explosions, and Create's split and resize behaviour are unchanged. Mined drops carry no fuel, and splitting can still discard fuel.

## Validation (2026-11-13)

`gradlew.bat clean build runGameTestServer --console=plain` passed: "All 40 required tests passed" (33 existing tests plus 7 new ones in `BatteryArrayRelocationTest`). `python tools/ponder/verify.py` reported "all checks passed". Every scenario asserts conservation: world tanks plus inventory items plus dropped items equals F.

- Pickup of 2x2x1, 2x2x2 and 3x3x3 arrays × both fuels × {1 mB, partial, full} × {controller, far member} × {survival, creative} gives exactly one item holding F, and the space is left empty.
- Round trip, with 3 cycles per size and mode: item serialization, placement, shape, intactness and F, controller save/reload, then pickup from a member.
- Pickup refusals (cancelled `BreakEvent`, unloaded member, pending connectivity) and the 1.2 single path under a cancelled `BreakEvent` change nothing.
- Placement refusals keep the item and leave no battery in the target space: obstructed space, adjacent battery, unloaded chunk, lowered capacity, and a cancelled place event.
- With a full inventory, exactly one dropped item is produced. Empty arrays give W·W·H plain batteries. Mining a full 2x2x2 array still drops 8 fuel-free batteries.
- Mutation check: ignoring `BreakEvent` cancellation makes `refusedArrayPickupChangesNothing` fail.

## Not verified

- Live client: tooltip rendering, placement aim and direction, and the server-refusal message after the client's arm swing.
- Multiplayer, real protection or claim mods (only test listeners), and third-party block-moving tools.
- Placement against a block's underside (the downward build) has no GameTest.
- The 1.2 charged single-battery placement still uses `stack.shrink(1)` for creative. In live play, vanilla's creative count restore probably undoes that, so creative keeps the item. That path was left unchanged.
