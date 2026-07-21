# Create: End Storage & Gateways

Industrial End storage, shulker logistics, and fueled dimensional gateways for
[Create](https://modrinth.com/mod/create). Dock shulker boxes as swappable cargo
containers, link twin Ender Barrels across dimensions, and build kinetic-powered
gateways fueled by liquid ender essences.

- **Minecraft:** 1.21.1 (NeoForge 21.1.x)
- **Requires:** Create 6.0.10+
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

### End storage

- **Ender Barrels** — crafted in twin pairs that share one inventory across any
  distance or dimension; network them by pair ID
- **Storage Network** — a controller clusters adjacent storage into one
  searchable terminal with a crafting-grid console

### End farming

Trap live shulkers in **Shulker Cages** to farm Processed Shulker Shells, then
refine them through the Ender Infusion chain into **Teleport Essence** and
**Liquid Eye of Ender** — the gateway fuels.

### Gateways

Build a frame ring around a **Cross-Dimensional Gateway Core**, power it with
rotation, and fuel it: lilac Teleport Essence for same-dimension links, green
Liquid Eye of Ender to cross dimensions. Fuel flows visibly through the frame
conduits, and the core's porthole shows the tank contents. **Gateway Ports**
move items and fluids between bound gateways, with an optional destination
chunk loader.

### Create integration

Stress-aware kinetics, goggle overlays, JEI recipe support, advancements, and a
server config for travel costs and chunk-loading permissions.

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1.
2. Install [Create](https://modrinth.com/mod/create) 6.0.10 or newer.
3. Drop `cesg-1.21.1-<version>.jar` into your `mods` folder (client **and**
   server).

## Building from source

```sh
./gradlew build
```

The jar is written to the Gradle `libs` output directory as
`cesg-<minecraft version>-<mod version>.jar`.

## Links

- **Source & issues:** [codeberg.org/lotleth/cesg](https://codeberg.org/lotleth/cesg)
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)

## License

[MIT](LICENSE)
