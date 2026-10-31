# 1.2.0 release candidate

Status: prepared, not published. Main and the published 1.1.1 release remain unchanged until the release PR is approved.

## Upload settings

- File: cesg-1.21.1-1.2.0.jar
- Display name: Create: End Storage & Gateways 1.2.0
- Minecraft 1.21.1; NeoForge 21.1.233+; Create 6.0.10 or later 6.0.x required
- Environment: same version on client and server
- Release type: Release

## Short CurseForge / GitHub notes

Custom mechanical GUIs across CESG, independent per-channel fluid routing filters, terminal JEI/EMI recipe and uses shortcuts, clearer transfer diagnostics, and fuel-preserving sneak-wrench pickup for isolated batteries. Includes compact GUI layouts and belt-model cleanup. Fluid routing defaults off for existing worlds. Drain battery arrays before dismantling; fueled array relocation remains deferred. No recipe or fuel-cost changes.

## Validation

On the integrated 1.2.0 candidate: Gradle build and all 30 required GameTests passed; Ponder verification passed. Release jar metadata confirms 1.2.0, expected GUI textures are present, and development GameTest classes/templates are excluded. SHA256SUMS accompanies the local candidate jar.

Earlier single-player GUI/viewer checks and feature-specific coverage are recorded in docs/dev/1.2. Live battery pickup/tooltip passed; battery placement passed GameTests, while the live placement attempt was inconclusive. Multiplayer, third-party fonts/resource packs, non-bucket modded fluid containers, third-party moving tools, a fresh normal-install packaged-jar smoke test, and an exact released-1.1.1 world upgrade remain unverified. Do not describe those as passed.

## Publication steps

1. Review candidate validation and accepted limits; optionally perform the normal-install/1.1.1-world smoke test on a copy.
2. Approve and merge the release PR into main. Finalize the changelog date, README published-release link, and this status before tagging.
3. Tag the tested release commit v1.2.0 and publish the GitHub release with the jar and checksum.
4. Andrew uploads the same jar to CurseForge with the notes/settings above.
5. Record published URLs and sync the final release back into 1.2-dev.

## Documentation and reconciliation

GUI issues #5, #7 and #17 were closed against completed PRs and their QA records. The post-1.2 roadmap covers fueled array ownership/relocation, processing modules, touching-frame detection and longer-term ideas. Player-facing updates are in docs/GUIDE-1.2.md. The GitHub wiki git endpoint returned Repository not found; no wiki publication is claimed.
