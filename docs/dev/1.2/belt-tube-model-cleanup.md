## Belt tube model cleanup — issue #18

The registered `belt_loader_tube` model contained a Y coordinate of -32, outside
Minecraft's permitted element bounds. Neither belt renderer uses this model:
both render Create's `SPOUT_MIDDLE` segments and CESG's `EXTRACTOR_NOZZLE`.

Removed the obsolete loader and unloader tube JSON assets, their partial-model
constants, eager initialization, and additional-model registrations. The active
renderers, nozzle asset, tube dimensions, animation, and transfer logic are unchanged.

Validation (2026-10-28):

- Gradle build and unit tests pass.
- Fresh client startup with JEI/EMI installed reports no failed model load,
  tube-model reference, or out-of-bounds model error.
- In the separate creative QA world, forced the loader and unloader to their
  extended and retracted states using fixture NBT. Both render the segmented
  hose and brass tip correctly; the retracted tip parks at the block bottom.
- Repository search finds no remaining reference to either removed asset.

This pass verifies rendering states, not an additional belt-transfer gameplay
cycle; gameplay and renderer logic were not modified.
