# 1.1.1 upload package

Status: GitHub release approved for 2026-10-28; CurseForge upload is handled by Andrew.

- GitHub release: https://github.com/lotlethtroll/cesg/releases/tag/v1.1.1
- Release jar: https://github.com/lotlethtroll/cesg/releases/download/v1.1.1/cesg-1.21.1-1.1.1.jar

## CurseForge file settings

- File: `cesg-1.21.1-1.1.1.jar`
- Display name: Create: End Storage & Gateways 1.1.1
- Game version: Minecraft 1.21.1
- Loader: NeoForge
- Release type: Release
- Required dependency: Create 6.0.10 or a later 6.0.x release
- Minimum NeoForge: 21.1.233

## Short release notes

Maintenance update for storage and gateway reliability:

- Fix terminal crafting item loss and partial-output duplication.
- Preserve crafting remainders and rejected JEI/EMI recipe-transfer leftovers.
- Prevent Bridge rollback/buffer overflow from losing items.
- Only charge automation fuel for successful transfers; select fuel by destination.
- Correct Bridge status reporting and avoid loading chunks during network/fuel scans.

No recipes, item IDs, save keys, or configuration defaults changed. Install the
same version on both client and server. Back up existing worlds before updating.

## Validation and outstanding checks

Build, 2 unit tests, 17 headless server GameTests, and Ponder checks pass.
Live single-player terminal/EMI checks passed, including Andrew's shift-crafting
and cake-refill tests. The client subsequently exited cleanly.

Single-player full client restart and confirmed chunk unload/reload preserve
Port/Bridge item and fluid buffers and stored fuel. Bridge goggles correctly
render linked, offline, and fault states. A copied `1.1.0-dev` world loads and
retains all 1,093 legacy CESG block entities by type and position. A short idle
load test with 254 network members and three battery arrays passes at 100 TPS;
the final 100-tick sample averages 2.2 ms per tick.

Multiplayer, an exact released 1.1.0 upgrade, full-inventory close overflow,
JEI separately, and an extended production-load soak remain unverified. The
single-player persistence test holds cargo rather than transferring it in flight.
See [the complete QA record](../dev/RELEASE-1.1.1.md).

## Final publication steps

1. Review the outstanding checks above before publishing.
2. Finalize the changelog date and listing release status when publication is approved.
3. Merge the tested `1.1-dev` release into main and create tag `v1.1.1`.
4. Upload the jar using the settings and notes above.
5. Confirm the published file and download link, then record the release URLs.
