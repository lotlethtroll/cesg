# After 1.1.1

Agreed sequence: finish the reliability patch first, then build 1.2.0 around
fluid routing, terminal usability, and custom GUI polish. No 1.2 feature is implemented by the patch.

## Proposed 1.2.0 scope

GUI polish is an explicit release priority; establish the visual direction before
expanding the routing and diagnostic interfaces described below.

1. Per-channel fluid filters. Extend existing gateway routing; retain the current
   active-channel behavior for old worlds unless players enable fluid routing.
   Decide filter UI, catch-all behavior, and per-fuel accounting before coding.
2. Terminal JEI/EMI recipe and uses shortcuts (R/U).
3. Transfer diagnostics: destination full, partner locked, no matching route,
   protected reserve, and network scan limit reached.
4. Consider fuel-preserving battery pickup, starting with lone batteries.
   Specify multiblock ownership before supporting array relocation.

## GUI polish direction

Andrew requested more custom, textured GUIs that feel at home beside Create's
interfaces. The final look is still open; start with a reviewable prototype.

- Establish a shared CESG palette, textured panel edges, recessed slots, buttons,
  tabs, and status indicators. Explore Create-like mechanical materials with
  restrained End/purpur accents; keep text and item counts easy to read.
- Prototype the Storage Terminal first: search, Local/Partner tabs, crafting area,
  and transfer feedback. Review it in-game before applying the style elsewhere.
- Follow with Storage Bridge push/pull and lock controls, gateway channel/filter
  screens, station configuration, and Enhanced Shulker inventory/module panels.
- Make enabled, disabled, selected, hover, locked, and unavailable states clear;
  pair status colors with labels/tooltips. Integrate the planned transfer diagnostics.
- Preserve familiar inventory interactions. Check supported GUI scales, narrow
  windows, long labels, large counts, and JEI/EMI overlap in-game.
- Keep the first visual pass separate from menu/container and transfer behavior
  changes so the 1.1.1 safety fixes remain easy to verify.

First deliverable: one textured terminal prototype and a small shared set of UI
assets, followed by Andrew's visual review. No final art direction is chosen yet.

## Back burner

- Blasting / Haunting modules: assess distinct uses alongside existing processing.
- Mixing / Mechanical Crafting modules: require multi-input and fluid design.
- Touching-frame gateway detection: separate compatibility-sensitive project;
  preserve every previously valid gateway when replacing flood-fill detection.
- Kinetic capacitor, worldgen, combat, ambient sounds/particles, and previously
  deferred cosmetic work.

New recipes remain Andrew's balance decisions and need sign-off before authoring.
