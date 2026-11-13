# 2.0.0 release

Status: approved by Andrew on 2026-11-13. Andrew uploads to CurseForge.

## Upload settings

- File: cesg-1.21.1-2.0.0.jar (SHA-256 75a287e8d32aac972a963597666b46d1e35ceb09e1ef05da9c88394dd102e50c)
- Display name: Create: End Storage & Gateways 2.0.0
- Minecraft 1.21.1; NeoForge 21.1.233+; Create 6.0.10 or later 6.0.x required
- Environment: same version on client and server
- Release type: Release

## Short CurseForge / GitHub notes

**Upgrading: crushing-wheel pearl farms no longer make ender pearl dust from End Stone. Wash End Stone with
an encased fan through water (or the Washing upgrade) instead.** This frees End Stone crushing for other
addons, so Create Aeronautics' End Stone Powder and Levitite work again.

New: move a fuelled battery array with the wrench as one item; gateway rings may touch or share Frames;
Haunting and Blasting upgrades for Enhanced Shulker Boxes. Fixes: dyeing an Enhanced Shulker Box no longer
destroys it; standing in a gateway no longer bounces you back and forth draining fuel; Storage Terminal
shift-crafting no longer switches recipe; smelted items stack; Polished End Stone no longer clashes with
End Stone Bricks. Full list in CHANGELOG.md.

## Validation

See docs/dev/2.0/QA.md: build and 58 GameTests, 59-addon compatibility run, production servers on NeoForge
21.1.233 and 21.1.266, a 1.2.0 → 2.0.0 world upgrade (1,093 CESG blocks unchanged), and an in-game pass
including Create Aeronautics. Untested: multiplayer, real claim/protection mods, third-party block movers,
resource packs, modded non-bucket fluid containers, a graphical normal-install client. Do not describe
those as passed.

## Publication steps

1. Andrew approves. Open the release PR `2.0-dev` → `main`; set the changelog date and README release
   link before tagging.
2. Tag the tested commit `v2.0.0`; publish the GitHub release with the jar and checksum.
3. Andrew uploads the same jar to CurseForge with the notes above.
4. Reply to the Aeronautics report: fixed in 2.0.0. Crushing wheels give Aeronautics' powder; wash End
   Stone for pearl dust.
