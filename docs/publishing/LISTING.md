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
| Source / issues | https://github.com/lotlethtroll/cesg (issues: `/issues`) |
| Environment | Required on **both** client and server |

## Versions & dependencies (per file upload)

| Field | Value |
|---|---|
| Game version | 1.21.1 |
| Loader | NeoForge |
| Release channel | Release |
| File | `cesg-1.21.1-1.1.0.jar` (from `%LOCALAPPDATA%\gradle-builds\Create-End-Storage-and-Gateways\libs`) |
| Required dependency | Create (Modrinth project `create`; CurseForge project "Create") |
| Changelog | Paste the matching section from `CHANGELOG.md` |

Categories — Modrinth: `storage`, `technology`, `transportation`.
CurseForge: Storage, Technology → Automation, Technology → Player Transport.

## Description (paste into both sites)

**Create: End Storage & Gateways** brings the End into your Create factory:
shulker-box logistics, twin-linked ender storage, kinetic-powered gateways
between dimensions, and storage networks that reach through them.

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
- **Crushing / Washing** — run Create's crushing, milling and splashing recipes
  on the box's own contents

### End storage
- **Ender Barrels** — crafted in twin pairs that share one inventory across any
  distance or dimension; network them by pair ID
- **Storage Network** — a controller clusters adjacent storage into one
  searchable terminal with a crafting-grid console
- **Storage Bridge** — links two networks through a bound gateway: your terminal
  shows the far side's items on their own tab and moves them either way, with
  filters and a per-bridge lock deciding what travels and who may take from you

### Gateways
Build a frame ring around a **Cross-Dimensional Gateway Core**, power it with
rotation, and fuel it: lilac **Teleport Essence** for same-dimension links,
green **Liquid Eye of Ender** to cross dimensions. Fuel flows visibly through
the frame conduits; the core's porthole shows what's in the tank. **Gateway
Ports** move items and fluids between bound gateways, with an optional
destination chunk loader.

A core holds up to sixteen destinations. Switch between them, or turn on **route
mode** and give each channel a filter so one gateway sorts iron to one base and
gold to another. **Gateway Flux Batteries** buffer fuel beside the ring and merge
into arrays up to 3×3×3, keeping a reserve back for player travel so automation
can never strand you.

**End Stone and ender pearls are renewable** through your own factory — haunt
sandstone into End Stone, crush it for ender pearl dust, compact the dust back
into pearls.

### Create integration
Stress-aware kinetics, goggle overlays, JEI and EMI support (including "+"
recipe transfer into the storage terminal), Ponder scenes for every machine,
advancements, and a server config for fuel costs, transfer rates and
chunk-loading permissions.

**Requires Create 6.0.10+ on NeoForge 1.21.1.**

## First-upload checklist

1. Create the project on each site with the identity table above (icon, summary,
   description, categories, license, source link).
2. Modrinth: set environment to required client + required server; add Create as
   a required dependency on the version you upload.
3. CurseForge: add Create under Related Projects → Required Dependency.
4. Upload `cesg-1.21.1-1.1.0.jar`, game version 1.21.1, NeoForge, Release,
   changelog from `CHANGELOG.md`.
5. Add 3–5 gallery screenshots: a gateway ring mid-flow (conduits showing fuel),
   a belt loader line, the storage terminal showing a Partner tab, a Flux Battery
   array, an upgraded shulker GUI.
6. After both pages exist, consider pointing `displayURL` in
   `src/main/templates/META-INF/neoforge.mods.toml` at the Modrinth page.
