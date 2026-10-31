# Packaged world upgrade check — 2026-10-30

Passed the packaged-server upgrade/load/save/reopen check using the candidate jar with SHA-256 311410a7652bb929008fe4f1f06413d9eeb872feb5f952c5ef04c500e3e60561.

## Method

Installed a clean production NeoForge 21.1.233 dedicated server under ignored run/packaged-upgrade, with Create 6.0.10 and the released CESG 1.1.1 jar. No development classes or JEI/EMI were loaded. Original worlds were not modified.

The untouched October world contained no CESG block entities, so the populated July fixture world was copied instead. Loaded all 30 chunks containing its 1,093 CESG block entities across Overworld, Nether and End, saved and shut down with released 1.1.1. That establishes a genuine released-1.1.1 saved baseline, though the fixture itself originally came from a development world.

Replaced only the CESG jar with the packaged 1.2.0 candidate. Started, loaded the previously forced fixture chunks, saved and shut down normally. Started 1.2.0 a second time and repeated save/shutdown. Compared region-file NBT before/after.

## Results

- All 1,093 CESG block-entity positions and identities retained after upgrade and second startup; no missing or added CESG block entities.
- All existing item channel filters retained. Cores gained FluidRouteMode=false and existing filters gained empty nine-slot fluid selections with blacklist off.
- No Port/Bridge buffer or core-fuel field changes in the first upgrade comparison.
- Three battery tank amounts increased while the world ticked. The fixture has nearby active Create creative-fluid supplies; this is consistent with continued filling, not proof of exact global fuel conservation.
- One station's held shulker changed from vanilla container representation to an enhanced shulker, preserving its 63 cobblestone. Cooldowns, activity timestamps and portal/frame cache fields also changed while ticking.
- No CESG startup, missing-mapping, block-entity load or save errors. Both versions emitted the same nonfatal optional JetBrains annotation Mixin warning and vanilla asset-URL warnings; omitted JEI/EMI generated expected world mod-list warnings.
- Both 1.2.0 runs reached Done and shut down with all dimensions saved.

Evidence retained locally in run/packaged-upgrade: baseline-1.1.1.json/.log, upgraded-1.2.0.json/.log, reopened-1.2.0.json/.log and comparison.json.

This is a clean packaged dedicated-server compatibility check, not multiplayer interaction coverage or a graphical normal-client test. It does not prove every machine recipe or third-party integration. Existing feature GameTests and single-player GUI checks remain the functional coverage.
