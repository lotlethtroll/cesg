## Scope

- [ ] Create a coherent CESG panel, slot, tab, and button style with Create-like mechanical materials and restrained End accents.
- [ ] Prototype only the Storage Terminal first; preserve menu, packet, crafting, and transfer behavior.
- [ ] Review enabled, disabled, hover, selected, and offline states in-game at supported GUI scales, narrow windows, long labels, and large counts.
- [ ] Obtain Andrew's visual feedback before rolling the style out.

Targets `1.2-dev`. First visual prototype implemented; in-game review is pending.

## Prototype pass

- Original pixel textures for the terminal body and reusable recessed inventory slot.
- Warm metal/brass palette, restrained purple crafting header, textured panel grain,
  and selected-tab accent. Existing slot positions and click targets are preserved.
- Texture source: `tools/gui/generate_terminal.py` (Python + Pillow); rerun from
  the repo root to regenerate the PNG assets.
- Validation: Gradle build and unit tests pass. Background texture inspected.
- Pending: live client review of populated/empty grids, tooltips, search focus,
  Local/Partner tabs and offline states, GUI scales, and JEI/EMI overlap.
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
- Not yet checked: scale 1 terminal, narrow window, other languages/resource
  fonts, disconnected/faulted partner states, JEI alone, and EMI alone.

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
  its original Auto scale again. JEI-only and other-language checks remain pending.

https://github.com/lotlethtroll/cesg/issues/5
