# After 1.1.1

Agreed sequence: finish the reliability patch first, then build 1.2.0 around
fluid routing and terminal usability. No 1.2 feature is implemented by the patch.

## Proposed 1.2.0 scope

1. Per-channel fluid filters. Extend existing gateway routing; retain the current
   active-channel behavior for old worlds unless players enable fluid routing.
   Decide filter UI, catch-all behavior, and per-fuel accounting before coding.
2. Terminal JEI/EMI recipe and uses shortcuts (R/U).
3. Transfer diagnostics: destination full, partner locked, no matching route,
   protected reserve, and network scan limit reached.
4. Consider fuel-preserving battery pickup, starting with lone batteries.
   Specify multiblock ownership before supporting array relocation.

## Back burner

- Blasting / Haunting modules: assess distinct uses alongside existing processing.
- Mixing / Mechanical Crafting modules: require multi-input and fluid design.
- Touching-frame gateway detection: separate compatibility-sensitive project;
  preserve every previously valid gateway when replacing flood-fill detection.
- Kinetic capacitor, worldgen, combat, ambient sounds/particles, and previously
  deferred cosmetic work.

New recipes remain Andrew's balance decisions and need sign-off before authoring.
