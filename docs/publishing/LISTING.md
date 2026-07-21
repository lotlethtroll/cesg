# Publishing kit — CurseForge & Modrinth

Everything paste-ready for creating the project pages. Keep this file updated when
the feature set changes; the description below is the canonical listing copy.

## Project identity

| Field | Value |
|---|---|
| Name | Create: End Storage & Gateways |
| Slug | `create-end-storage-gateways` |
| Summary | Industrial End storage, shulker logistics, and fueled dimensional gateways for Create. |
| Icon | `docs/publishing/project-icon-400.png` (400×400; same art as the in-jar `logo.png`) |
| License | MIT |
| Source / issues | https://codeberg.org/lotleth/cesg (issues: `/issues`) |
| Environment | Required on **both** client and server |

## Versions & dependencies (per file upload)

| Field | Value |
|---|---|
| Game version | 1.21.1 |
| Loader | NeoForge |
| Release channel | Release |
| File | `cesg-1.21.1-1.0.0.jar` (from `%LOCALAPPDATA%\gradle-builds\Create-End-Storage-and-Gateways\libs`) |
| Required dependency | Create (Modrinth project `create`; CurseForge project "Create") |
| Changelog | Paste the matching section from `CHANGELOG.md` |

Categories — Modrinth: `storage`, `technology`, `transportation`.
CurseForge: Storage, Technology → Automation, Technology → Player Transport.

## Description (paste into both sites)

**Create: End Storage & Gateways** brings the End into your Create factory:
shulker-box logistics, twin-linked ender storage, and kinetic-powered gateways
between dimensions.

### Shulker logistics
Dock shulker boxes at kinetic **Loader / Unloader stations** or belt-fed
**Belt Loaders / Unloaders** and treat them as swappable cargo containers.
Enhanced Shulker Boxes take tiered upgrade modules (andesite Mk I / brass Mk II /
diamond Mk III):

- **Smelting** — auto-smelts contents, chaining multi-step recipes live
  (cobblestone → stone → smooth stone)
- **Void** — trims overflow, with its own filter
- **Magnet** — vacuums up dropped items
- **Filter** — whitelist control over what gets in

### End storage
- **Ender Barrels** — crafted in twin pairs that share one inventory across any
  distance or dimension; network them by pair ID
- **Storage Network** — a controller clusters adjacent storage into one
  searchable terminal with a crafting-grid console

### Gateways
Build a frame ring around a **Cross-Dimensional Gateway Core**, power it with
rotation, and fuel it: lilac **Teleport Essence** for same-dimension links,
green **Liquid Eye of Ender** to cross dimensions. Fuel flows visibly through
the frame conduits; the core's porthole shows what's in the tank. **Gateway
Ports** move items and fluids between bound gateways, with an optional
destination chunk loader.

### Create integration
Stress-aware kinetics, goggle overlays, JEI recipe support, advancements, and a
server config for travel costs and chunk-loading permissions.

**Requires Create 6.0.10+ on NeoForge 1.21.1.**

## First-upload checklist

1. Create the project on each site with the identity table above (icon, summary,
   description, categories, license, source link).
2. Modrinth: set environment to required client + required server; add Create as
   a required dependency on the version you upload.
3. CurseForge: add Create under Related Projects → Required Dependency.
4. Upload `cesg-1.21.1-1.0.0.jar`, game version 1.21.1, NeoForge, Release,
   changelog from `CHANGELOG.md`.
5. Add 3–5 gallery screenshots: a gateway ring mid-flow (conduits showing fuel),
   a belt loader line, the storage terminal GUI, an upgraded shulker GUI.
6. After both pages exist, consider pointing `displayURL` in
   `src/main/templates/META-INF/neoforge.mods.toml` at the Modrinth page.
