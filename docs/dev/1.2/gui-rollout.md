## Scope

- [x] Depends on the terminal prototype and Andrew's visual review (PR #6 merged).
- [ ] Apply approved assets to Bridge, gateway channel/filter, station configuration, and Enhanced Shulker screens.
- [ ] Keep lock, push/pull, filters, and inventory interactions recognizable; use labels/tooltips alongside status colors.
- [ ] Check all affected screens in-game, including JEI/EMI overlap and GUI scaling.

Targets `1.2-dev`. Storage Bridge is the first rollout prototype; other screens remain pending.

## Storage Bridge first pass

- Reuse the approved recessed slot texture and metal/brass perimeter, with purple
  push/pull section plates and matching beveled controls.
- Preserve filter, lock, inventory, and toggle hit targets and server behavior.
- Keep title, destination, and button text within their available widths; full
  title and destination text remain available on hover.
- Generator: `python tools/gui/generate_bridge.py` (also regenerates terminal assets).
- Build/unit tests and the Bridge interaction/visual pass below are complete.

## Bridge visual QA — 2026-10-28

Tested in the separate creative QA copy with JEI and EMI installed (EMI's visible sidebar).

- Approved corner-only fasteners, rails, section plates, and recessed slots render correctly.
- Lock changes Open/Locked and its highlight; push/pull enable buttons and both
  WL/BL buttons update correctly. Their tooltips are readable.
- One original pickaxe configures both ghost rows without being consumed; it
  returns to the hotbar. Empty-cursor clicking clears the pull ghost filter.
- Route mode veils the send row and grays its mode button. Send ghost clearing
  and WL/BL changes are refused; the existing ghost item/mode remain intact.
- A long destination label stays within the header; its full text is readable
  on hover. The routed-row explanation is readable on ghost-slot hover.
- Found and fixed Auto-scale EMI search/hotbar overlap: reserve bottom margin
  and remove the four-pixel inventory/hotbar gap only when vertical room is tight.
  Texture is stitched without stretching; filter and main inventory positions do not move.
- Retested compact fullscreen, a small window, and maximized normal layout with
  the menu open: no search overlap, and hotbar-to-inventory clicks map correctly.
- Added a regression test for repeated compact/normal hotbar placement, retaining
  all slot IDs, inventory indices, and filter/main inventory slot objects.
  Build, unit tests, and all 19 server GameTests pass.
- Also corrected the disabled send-mode button's misleading WL/BL tooltip to use
  the route-mode explanation. This final text-path change is build-checked; that
  same explanation was visually checked on the ghost row before the change.
- Restored the fixture to route off, push/pull off, unlocked, WL, and empty filters.
  Restored fullscreen. No production world was modified.

Not covered by this Bridge pass: isolated JEI/EMI runs, every explicit GUI scale,
other languages/resource-pack fonts, and multiplayer. The remaining rollout
screens still need implementation and their own in-game checks.

https://github.com/lotlethtroll/cesg/issues/7

## Gateway channel/filter first pass — 2026-10-28

- Added matching metal/brass panels with corner-only fasteners, purple status
  plates, recessed filter/inventory slots, and beveled channel/footer controls.
- Channel panel reserves space for all 16 channels and config-dependent footer
  buttons. Pending name text survives screen resize. Native button focus,
  narration, tooltips, and existing packet/menu behavior remain in place.
- Fixed a live rendering issue: draw the channel panel after Minecraft's
  background blur, keeping the frame crisp.
- Build and unit tests pass. Live checks with JEI/EMI installed cover fullscreen
  and the small Auto-scale window: both panels fit; filter inventory/hotbar and
  EMI search remain separate. Channel 2 selection persists when reopened;
  channel 1 was restored. Bound/active/unbound colors render correctly.
- Right-click opens the correct channel filter. WL/BL toggles and tooltips work.
  A pickaxe copies into the ghost row without consumption, returns to inventory,
  and empty-cursor clicking clears the ghost. Restored empty WL filter.
- Still pending: name-save/resize interaction, footer toggle persistence,
  keyboard focus, config-disabled footer variants, other languages, and multiplayer.
- Generator: `python tools/gui/generate_gateway.py`.

Remaining implementation: station configuration and Enhanced Shulker panels.
