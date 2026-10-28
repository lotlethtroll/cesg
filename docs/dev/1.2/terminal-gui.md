## Scope

- [x] Create a coherent CESG panel, slot, tab, and button style with Create-like mechanical materials and restrained End accents.
- [x] Prototype only the Storage Terminal first; preserve packet, crafting, and transfer behavior (client slot coordinates adapt to height).
- [x] Review tab/control states, offline/fault messages, tooltips, supported GUI scales, narrow windows, and large counts in-game.
- [x] Obtain Andrew's visual feedback before rolling the style out.

Targets `1.2-dev`. Visual direction approved by Andrew; terminal merge checks complete.

## Prototype pass

- Original pixel textures for the terminal body and reusable recessed inventory slot.
- Warm metal/brass palette, restrained purple crafting header, textured panel grain,
  and selected-tab accent. Existing slot positions and click targets are preserved.
- Texture source: `tools/gui/generate_terminal.py` (Python + Pillow); rerun from
  the repo root to regenerate the PNG assets.
- Validation: Gradle build and unit tests pass. Background texture inspected.
- Completed live checks are recorded below; initial findings and their fixes are retained.
- The initial texture pass does not change menu behavior, network packets,
  transfer logic, or crafting. Responsive layout below changes client slot positions only.

## In-game QA — 2026-10-28

Tested in the separate `QA legacy 1.1 upgrade` creative world using the real
client, with both JEI and EMI loaded (EMI provides the visible recipe sidebar).

- GUI scale 2 and 3: complete terminal, crafting panel, and hotbar fit; recipe
  sidebar and bottom search stay outside the terminal. Headers align and the
  shared divider and perimeter render without the earlier gaps/dashed strip.
- Scale 2: Local/Partner switching and the empty partner message work. Long
  search text stays within its field and shows the no-results message.
- Synthetic storage counts 1,234,567, 12,345, and 999 render abbreviated inside
  their slots at scales 2 and 3. These command-injected counts are display stress
  fixtures, not normal legal stack sizes or an inventory-capacity test.
- Stored-item tooltip reports the full 1,234,567 total. Clear-grid tooltip is
  readable at scale 3.
- **Original finding (fixed below):** Auto scale leaves too little vertical margin: EMI's centered
  bottom search overlays the hotbar. The frame itself fits, but recipe-viewer
  integration initially failed at this scale.
- At this stage, scale 1, narrow windows, partner failure states, and isolated
  recipe viewers were pending; these are covered by the final pass below.

Client left at scale 3 for inspection. No production world was modified.

## Auto-scale overlap fix — issue #17

- Reserve 24 GUI pixels below the terminal for recipe-viewer controls. Display
  fewer storage rows when necessary (five in the tested Auto-scale setup), with
  scrolling through the same complete item list. Roomier screens retain six rows.
- Move player inventory and hotbar upward with the shorter body. Slot IDs,
  inventory indices, crafting slots, and server transfer behavior remain unchanged.
- Stitch the existing texture at the storage/inventory seam; no stretched art.
- In-game Auto-scale retest passes: EMI centered search clears the hotbar;
  inventory-to-hotbar movement selects the correct slots. With 48 stored item
  types, scrolling reaches the final yellow entries; withdrawing 64 yellow wool
  from the last row and placing it in the hotbar works.
- Build, unit tests, and all 18 GameTests pass. New coverage checks slot identities
  and coordinates across repeated compact/normal layout changes.
- No global scale or EMI preference is changed by the mod. The QA client uses
  its original Auto scale again. Isolated-viewer follow-up is recorded below.

## Final merge checks — 2026-10-28

- JEI alone: launched with EMI removed only from the development runtime via a
  temporary Gradle init script. Startup log confirms seven mods and JEI's runtime.
  The terminal, hotbar, JEI search, and item sidebar remain separate in the small
  window (approximately 854×480 client pixels; captured at desktop scaling).
- Repeatedly maximize/restore the window with the terminal open: five storage
  rows become six and return to five. Scrolling to the final compact row then
  expanding clamps the scroll position and restores the beginning of the list.
- Following those resizes, move 64 yellow wool from the hotbar into crafting,
  then clear the grid: all 64 return to storage and both source slots are empty.
- Fault and offline fixtures display the red/gray Partner indicators and centered
  message plates within the storage panel. Restoring the controller and binding
  recovers the linked/empty Partner view.
- EMI alone: launched with JEI removed only from the development runtime. The
  terminal, item sidebar, search, and six-row layout fit at GUI scale 1. Partner
  tab selection and empty-network message work. Together with prior scale 2/3
  and compact Auto checks, the targeted scale/viewer checks pass.
- Both clients save and exit cleanly. Restore the pre-QA display settings afterward.
- No terminal code changes were needed in this final pass. Build/unit tests and
  18 GameTests passed on this implementation in the preceding overlap-fix pass.
- Separate pre-existing startup model error: belt-loader tube has out-of-bounds
  geometry, unchanged from `v1.1.1`; tracked in issue #18, outside this PR.

Merge assessment: no known terminal blocker remains. Other languages and
third-party resource-pack fonts have not been exhaustively checked.

https://github.com/lotlethtroll/cesg/issues/5
