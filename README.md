# Create: End Storage & Gateways

Industrial End storage, shulker logistics, and fueled dimensional gateways for
[Create](https://modrinth.com/mod/create). Dock shulker boxes as swappable cargo
containers, link twin Ender Barrels across dimensions, build kinetic-powered
gateways fueled by liquid ender essences, and bridge entire storage networks
between worlds.

- **Minecraft:** 1.21.1 (NeoForge 21.1.233+)
- **Requires:** Create 6.0.10 or newer, below 6.1.0
- **Environment:** required on both client and server
- **Mod ID:** `cesg`

## Features

### Shulker logistics

Treat shulker boxes as cargo containers. Kinetic **Loader / Unloader** stations
and belt-fed **Belt Loader / Belt Unloader** stations dock a box, fill or empty
it through funnels, belts, and pipes, and auto-eject it when your configured
fullness condition is met. Stations support retention modes, slot thresholds,
name stamping, filters, and full goggle overlays.

### Enhanced Shulker Boxes

Tiered shulkers (54 / 81 / 108 slots) that keep their inventory, upgrades, and
name through every tier upgrade. Each tier adds module slots for upgrade cards
in three grades (andesite Mk I / brass Mk II / diamond Mk III):

- **Stack Depth** — raises per-slot stack limits (up to 512)
- **Smelting** — auto-smelts contents, chaining multi-step recipes live
  (cobblestone → stone → smooth stone)
- **Void** — trims overflow, with its own independent filter
- **Magnet** — vacuums up dropped items, range scaling by tier
- **Filter** — whitelist control over what gets in
- **Compacting** — merges partial stacks on insert
- **Crushing / Washing** — run Create's crushing, milling and splashing recipes
  on the box's own contents, chaining cobblestone all the way down to sand

### End storage

- **Ender Barrels** — crafted in twin pairs that share one inventory across any
  distance or dimension; network them by pair ID
- **Storage Network** — a controller clusters adjacent storage into one
  searchable terminal with a crafting-grid console
- **Storage Bridge** — links two networks through a bound gateway, so a terminal
  shows the far side's items as a separate tab and moves them either way. Filters
  and a per-bridge lock decide what travels and who may take from you

### End farming

Trap live shulkers in **Shulker Cages** to farm Processed Shulker Shells, then
refine them through the Ender Infusion chain into **Teleport Essence** and
**Liquid Eye of Ender** — the gateway fuels.

End Stone and ender pearls are renewable through your own Create factory rather
than new machines: haunt sandstone into End Stone, crush it for ender pearl dust,
and compact the dust back into pearls.

### Gateways

Build a frame ring around a **Cross-Dimensional Gateway Core**, power it with
rotation, and fuel it: lilac Teleport Essence for same-dimension links, green
Liquid Eye of Ender to cross dimensions. Fuel flows visibly through the frame
conduits, and the core's porthole shows the tank contents. **Gateway Ports**
move items and fluids between bound gateways, with an optional destination
chunk loader.

A core can hold up to sixteen destinations. Switch between them, or turn on
**route mode** and give each channel a filter so one gateway sorts iron to one
base and gold to another. **Gateway Flux Batteries** buffer fuel beside the ring
and merge into arrays up to 3×3×3; they keep a reserve back for player travel, so
automation can never strand you on the wrong side of a portal.

### Create integration

Stress-aware kinetics, goggle overlays, JEI recipe support, advancements, and a
server config for travel costs and chunk-loading permissions.

## 1.2.0 update

Custom mechanical GUIs, independent fluid routing filters, terminal recipe/uses
shortcuts, transfer diagnostics, and fuel-preserving pickup for isolated batteries.
See [CHANGELOG.md](CHANGELOG.md) and the [1.2 guide](docs/GUIDE-1.2.md).

[Download 1.2.0](https://github.com/lotlethtroll/cesg/releases/tag/v1.2.0).
Back up your world before updating, and install the same version on client and server.

## Installation

1. Install [NeoForge](https://neoforged.net/) 21.1.233 or newer for Minecraft 1.21.1.
2. Install [Create](https://modrinth.com/mod/create) 6.0.10 or a later 6.0.x release.
3. Drop `cesg-1.21.1-<version>.jar` into your `mods` folder (client **and**
   server).

## Building from source

```sh
./gradlew build
```

Run the checks before preparing a release:

```sh
./gradlew build runGameTestServer
python tools/ponder/verify.py
```

GameTests run in an isolated `run/gametest` world. Test classes and their empty
structure template are development-only and are excluded from the release jar.
See the [1.2 release checklist](docs/publishing/RELEASE-1.2.0.md) for validation
and remaining coverage limits, and the [post-1.2 roadmap](docs/dev/NEXT-RELEASE.md).

The jar is written to the Gradle `libs` output directory as
`cesg-<minecraft version>-<mod version>.jar`.

## Links

- **Source & issues:** [github.com/lotlethtroll/cesg](https://github.com/lotlethtroll/cesg)
- **Wiki:** [www.cesg.dev](https://www.cesg.dev)
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)

## License

[MIT](LICENSE)
