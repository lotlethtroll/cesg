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
- This pass does not change menus, network packets, transfer logic, or crafting.

https://github.com/lotlethtroll/cesg/issues/5
