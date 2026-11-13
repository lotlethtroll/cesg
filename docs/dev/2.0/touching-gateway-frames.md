## Touching gateway frames

Before 2.0, two gateway rings whose frames touched in the same vertical plane both failed to open.
`GatewayPortalShape.tryAxis` flood-fills every Gateway Frame connected to the Core and requires that set
to be exactly one rectangle outline, so a neighbouring ring's frames were absorbed and the check failed
with `extra`. Building a ring against an active gateway closed it within a second, because
`refreshPortalState` re-runs detection every 20 ticks. This was accepted as a known limitation in 1.1
(`docs/dev/PHASE7.md`).

### Rule

`detect` now runs in this order and returns the first shape found:

1. The original flood-fill on the X plane, then on the Z plane. This code is unchanged.
2. A fallback on the X plane, then on the Z plane. It starts from the Core's eight in-plane neighbours
   in a fixed order: up, down, +width, -width, then the four diagonals (so corner Cores work). From each
   air/portal neighbour it floods the connected air/portal region and gives up once the region is wider
   or taller than 8. It accepts the first region that is a full rectangle of a valid size (1-8 wide,
   2-8 tall) whose whole outline, corners included, is Gateway Frames plus this Core, with this Core on
   it. Another gateway's Core never counts as outline.

When a Core borders two valid openings, the fixed order decides (X plane first, then neighbour order).
`describeFailure` returns null whenever either step finds a shape. Otherwise it reports the original
reason. The `extra` message now reads "No complete Frame rectangle around a clear opening here". The
Gateway Frame tooltip no longer tells players to leave space between gateways.

### Why existing gateways are unchanged

The fallback runs only when the original detection fails on both planes. Any layout valid before 2.0
gets the same axis, interior list and frame list as before, including a corner Core with a touching X
ring and a clean Z ring, which still picks Z. There is no save-format change: `Portal` and `FrameLit`
on the Core are a cache that is rebuilt every 20 ticks.

### Left as-is

- The Port, Bridge and Battery searches in `GatewayFuelHandler` walk connected frames. They could
  already reach a touching neighbour's frames before 2.0.
- Frames shared by two gateways may briefly change lighting or colour when one gateway switches off.
  The other gateway relights them on its next refresh. This is cosmetic.

### Validation (2026-11-13)

`GatewayShapeTest` (5 GameTests) builds layouts in an in-memory `BlockGetter` and compares results
with the original detection (called by reflection):

- Same result as before: every interior size from 1x2 to 8x8 on both planes, with the Core on an edge
  and on each corner (672 layouts); Core-to-Core contact; diagonal contact; back-to-back rings; a Core
  on valid X and Z rings (picks X); a touching X ring with a clean Z ring (picks Z); 8x8 rings.
- Now opens: rings sharing a column, rings with adjacent columns, stacked rings sharing a row, a stray
  frame against a ring, three rings in a row, and a perpendicular ring touching in-plane.
- Still fails with the same diagnostic as before: a gap, a Port in the outline, a blocked opening, a
  size out of range, and a foreign Core in the outline. The same faults in a ring that shares a column
  with a good ring keep the faulty side closed and let the good side open.
- A real, fuelled Core stays open after a touching ring with its own Core is built beside it.

Results:

- `./gradlew clean build runGameTestServer --console=plain`: all 38 required tests passed (33 existing
  plus the 5 new ones).
- `python tools/ponder/verify.py`: all checks passed.
- Disabling the fallback fails 3 of the new tests. Running the fallback before the original detection
  fails the equivalence tests. Both changes were reverted.

Not verified: in a running client (goggle text, tooltip, frame lighting on shared frames), and in
multiplayer.
