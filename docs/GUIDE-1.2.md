# CESG 1.2 player guide

## Fluid routing

Open the empty-handed Gateway Core. Items and Fluids have independent Active/Route controls. Fluid routing defaults to Active, preserving existing worlds. Right-click a channel to edit its filters; switch Items to Fluids and click a ghost slot with a filled fluid container. The container is not consumed. Empty cursor clears; invalid/empty containers leave a fluid selection unchanged.

Each channel has independent fluid whitelist/blacklist settings. Empty whitelist accepts nothing; empty blacklist is a catch-all. The first matching bound channel in numeric order wins. Place catch-alls after specific routes. Matching compares fluid and components, not container or amount. Full, incompatible or unloaded destinations leave fluid buffered; routing does not fall through or load all destination chunks. Fuel is selected by the actual destination and charged once per used fuel type per flush that moves cargo.

## Terminal shortcuts and layout

Hover a stored network item and use your recipe viewer's configured recipe or uses key (normally R/U). Typing in terminal search does not open recipes. JEI and EMI work independently; neither is required. Compact textured layouts keep item counts within cells and preserve accessible inventory slots.

## Transfer diagnostics

Hover Bridge push/pull headers or controls for the server's current explanation. Green indicates available transfer, grey disabled/idle, amber a blocker or scan limit. Messages distinguish destination refusal, extraction locks, missing routes, protected travel reserve, insufficient fuel, inactive gateway and unavailable networks. These are periodic snapshots. A terminal title prefixed with ! means its local network scan was truncated; hover for details. Exactly 256 members does not trigger a warning unless more members were discovered.

## Battery pickup

Sneak-right-click an isolated battery with a Create wrench to retain its fuel in one item. Charged items are unstackable and display fuel/amount. Place them away from other batteries; charged placement consumes the item even in creative. Empty batteries retain ordinary placement behavior. Fueled array relocation is not supported: drain arrays before dismantling. Normal breaking does not preserve fuel. Recipes are unchanged.

## Compatibility

Minecraft 1.21.1, NeoForge 21.1.233+, Create 6.0.10 or later 6.0.x. Install the same CESG version on client and server. Back up worlds before updating. Multiplayer and modded non-bucket fluid containers have not been verified for this release candidate.
