# After 1.2.0

These are candidates for the next feature release (provisionally 1.3.0), not release commitments. The 1.2 implementation records live in docs/dev/1.2.

## First design task: fueled battery-array relocation

1.2 supports isolated single-battery pickup only. An array has one shared controller tank; individual members do not each own a full tank. Before relocating fueled arrays, define how much fuel belongs to a removed block, how splits distribute fuel and overflow, and how unloaded members are handled. Specify controller/member removal, partial arrays, survival/creative, save/reload and canceled placement. Prove that dropped items plus remaining world tanks never exceed the original fuel. Prefer an atomic whole-array relocation design if per-block ownership becomes ambiguous. Drain arrays before dismantling under 1.2.

## Processing-module candidates

- Blasting and Haunting: establish distinct uses alongside existing Smelting, Crushing and Washing.
- Mixing and Mechanical Crafting: design multi-input recipes, fluid handling, buffering and outputs before implementation.
- New recipes and balance require Andrew's sign-off.

## Separate compatibility project

Touching-frame gateway detection: investigate replacing flood-fill detection without invalidating any previously valid gateway. Requires old-world fixtures and migration checks.

## Longer-term exploration

Kinetic capacitor, world generation, combat features, ambient sound/particle work, and further cosmetics. These need concrete use cases and scope before scheduling. They are not prerequisites for 1.2.0.

## Carry-forward validation

Multiplayer, third-party fonts/resource packs, modded non-bucket fluid containers, and third-party block-moving tools need broader compatibility coverage. Keep these as explicit validation limits rather than claiming they were tested.
