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
- Initial build/unit tests pass. Final in-game visual review and toggle/filter
  interaction checks remain pending before rolling the style out further.

https://github.com/lotlethtroll/cesg/issues/7
